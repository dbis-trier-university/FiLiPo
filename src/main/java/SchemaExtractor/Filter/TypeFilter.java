package SchemaExtractor.Filter;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.vocabulary.RDF;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class TypeFilter extends FilterSinkRDF {
    private BufferedWriter typeWriter = null;
    private BufferedWriter factWriter = null;
    private Map<String, Set<String>> types;
    private String name;
    private long time;
    Logger logger;

    public TypeFilter(StreamRDF dest, String name, String typePath, String factPath, int memory, Logger logger) {
        super(dest, RDF.type);

        this.types = new HashMap<>();
        this.name = name;
        this.logger = logger;

        try {
            FileWriter factWriter = new FileWriter(factPath);
            this.factWriter = new BufferedWriter(factWriter, memory);

            FileWriter typeWriter = new FileWriter(typePath);
            this.typeWriter = new BufferedWriter(typeWriter, memory);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void start() {
        super.start();
        this.time = System.currentTimeMillis();
    }

    @Override
    public void finish() {
        super.finish();

        try {
            factWriter.flush();
            factWriter.close();

            typeWriter.flush();
            typeWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        long timeEnd = System.currentTimeMillis();
        this.time = (timeEnd - this.time) / 1000;

        if(this.time > 60) logger.info("[TypeFilter] Finished dividing of " + this.name + " in " + (this.time / 60) + " Minutes.");
        else logger.info("[TypeFilter] Finished dividing of " + this.name + " in " + this.time + " Seconds.");
    }

    @Override
    public void triple(Triple triple) {
        for ( Node p : this.properties ) {
            String subj;
            if(triple.getSubject().isURI()) subj = "<" + triple.getSubject().getURI() + "> ";
            else subj = "<" + triple.getSubject().getBlankNodeLabel() + "> ";

            String pred = "<" + triple.getPredicate().getURI() + "> ";
            String obj;
            if(triple.getObject().isURI()) obj = "<" + triple.getObject().getURI() + "> .";
            else if(triple.getObject().isBlank()) obj = "<" + triple.getObject().getBlankNodeId().getLabelString() + "> . ";
            else obj = "\"" + StringEscapeUtils.escapeJava(triple.getObject().getLiteralLexicalForm()) + "\" .";

            // Types
            if ( triple.getPredicate().equals(p) ){
                Set<String> set;
                if(types.containsKey(triple.getSubject().toString())){
                    set = types.get(triple.getSubject().toString());
                    set.add(triple.getObject().toString());
                } else {
                    set = new HashSet<>();
                    set.add(triple.getObject().toString());
                }
                types.put(triple.getSubject().toString(),set);

                try {
                    typeWriter.append(subj).append(pred).append(obj).append("\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }

            // Facts
            } else {
                try {
                    factWriter.append(subj).append(pred).append(obj).append("\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public Map<String, Set<String>> getTypes() {
        return this.types;
    }

}
