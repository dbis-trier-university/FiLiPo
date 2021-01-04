package SchemaExtractor;

import SchemaExtractor.Filter.TripleAnalyzer;
import SchemaExtractor.Filter.TypeFilter;
import Utils.Loader.ConfigurationLoader;
import Utils.Logging.LogHelper;
import javafx.util.Pair;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFLib;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public class SchemaExtractor {
    private static Logger logger = Logger.getLogger("Log");
    private String name, endpointUrl;
    private String typePath, factPath, outPath;
    private double idThreshold;
    private int memory;

    public SchemaExtractor(String label, String path, double idThreshold) {
        LogHelper.initLog(logger, "Extractor", label);
        this.name = label;
        this.endpointUrl = path;
        String output = ConfigurationLoader.getOutputPath() + "data/" + label + "/";
        this. memory = ConfigurationLoader.getMemory() * 1048576 ;

        this.typePath = output + "schema/" + this.name + "_types.nt";
        this.factPath = output + "schema/" + this.name + "_facts.nt";
        this.outPath = output + "schema/" + "index.json";
        this.idThreshold = idThreshold;
    }

    public void extractSchema() {
        logger.info("[OfflineSchemaExtractor] Start extracting in background..");

        // Init index files (delete if existing and create new files)
        initFiles();
        logger.info("[OfflineSchemaExtractor] Start dividing into types and facts..");

        // Prepare rdf stream and filter handler
        // The filter handler is used to divide between types and facts
        // Triples like (subject a type) are types and all other triples are facts
        StreamRDF noWhere = StreamRDFLib.sinkNull() ;
        TypeFilter typeFilter = new TypeFilter(noWhere, this.name, this.typePath, this.factPath, this.memory, logger);
        RDFParser.source(this.endpointUrl).parse(typeFilter);

        logger.info("[OfflineSchemaExtractor] Start replacing and reducing..");

        // Prepare rdf stream and filter handler
        // The filter is used to replace entities by their types and group them together to a schema
        TripleAnalyzer tripleAnalyzer = new TripleAnalyzer(noWhere, this.name, typeFilter.getTypes(), logger);
        RDFParser.source(this.factPath).parse(tripleAnalyzer);
        tripleAnalyzer.types = null;

        // Count class occurrences
        JSONArray classArray = new JSONArray();
        Map<String,Integer> classOccurrences = countClasses(typeFilter.getTypes());
        for(Map.Entry<String,Integer> entry : classOccurrences.entrySet()){
            JSONObject tmp = new JSONObject();
            tmp.put("class",entry.getKey());
            tmp.put("occurrence", entry.getValue());
            classArray.put(tmp);
        }
        typeFilter.close();

        // Build JSON array to store the structure of the knowledge base
        JSONArray array = new JSONArray();
        for(Map.Entry entry : tripleAnalyzer.replacement.entrySet()){
            Triple t = ((Triple) entry.getKey());

            JSONObject tmp = new JSONObject();
            tmp.put("subject", t.getSubject());
            tmp.put("predicate", t.getPredicate());
            tmp.put("occurrence",entry.getValue());

            if(t.getObject().isURI()){
                tmp.put("object", t.getObject());
            }

            array.put(tmp);
        }
        tripleAnalyzer.replacement = null;

        // Build JSON array to store possible identifier
        JSONArray functionality = new JSONArray();
        JSONArray identifier = new JSONArray();
        for(Map.Entry<String,Double> entry : tripleAnalyzer.functionality.entrySet()){
            JSONObject idObject = new JSONObject();
            idObject.put("predicate",entry.getKey());
            idObject.put("functionality",entry.getValue());

            // In case of identifier relation
            if(entry.getValue() >= this.idThreshold) {
                idObject.put("type","none");
                identifier.put(idObject);
            }

            functionality.put(idObject);
        }
        tripleAnalyzer.functionality = null;

        // Build final JSON object
        JSONObject json = new JSONObject();
        json.put("identifier",identifier);
        json.put("functionality",functionality);
        json.put("classes",classArray);
        json.put("structure",array);

        // Writing reduced schema to disk
        try {
            FileWriter fileWriter = new FileWriter(this.outPath);
            BufferedWriter writer = new BufferedWriter(fileWriter, this.memory);

            writer.write(json.toString());
            writer.flush();
            writer.close();

            delTempFiles();
        } catch (IOException e) {
            e.printStackTrace();
        }

        logger.info("[OfflineSchemaExtractor] Done..");
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void initFiles(){
        try {
            File yourFile = new File(factPath.substring(0,factPath.lastIndexOf("/")));
            yourFile.mkdirs();
            yourFile = new File(factPath);
            yourFile.delete();
            yourFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void delTempFiles(){
        File yourFile = new File(factPath);
        yourFile.delete();
        yourFile = new File(typePath);
        yourFile.delete();
    }

    private Map<String,Integer> countClasses(Map<String, Set<String>> classes){
            Map<String,Integer> map = new HashMap<>();

            for(Map.Entry<String,Set<String>> entry : classes.entrySet()){
                for(String type : entry.getValue()){
                    if(!map.containsKey(type)){
                        map.put(type,1);
                    } else {
                        int counter = map.get(type) + 1;
                        map.put(type,counter);
                    }
                }
            }

            return map;
    }
}
