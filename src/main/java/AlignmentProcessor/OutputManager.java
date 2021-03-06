package AlignmentProcessor;

import Utils.Loader.ConfigurationLoader;
import Utils.ReaderWriter.DiskReader;
import javafx.util.Pair;
import org.apache.commons.lang3.tuple.MutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

class OutputManager {

    static int offset = 0;
    static JSONObject database;

    static void createFiles(String apiName, String dbName, int candidateRequests, int similarityRequests,
                            double stringSimilarity, double recordSimilarity, double responses,
                            Map<String,Pair<Long,Integer>> responseTimeMap,
                            Map<String,Map<String,Map<String,Pair<String,Double>>>> alignments,
                            Pair<Map<String,Double>,Map<String,Double>> supportConfidence)
    {
        String functionStorePath = ConfigurationLoader.getOutputPath() + "functionstore/functionstore.ttl";
        database = new JSONObject(DiskReader.readFile(ConfigurationLoader.getDatabasePath()));

        initFunctionStore(functionStorePath);
        writeFunctionStore(apiName, dbName, candidateRequests, similarityRequests,stringSimilarity,recordSimilarity,responses,responseTimeMap,alignments,supportConfidence);
    }

    private static void initFunctionStore(String functionStorePath){
        File storeFile = new File(functionStorePath);
        BufferedWriter bw = null;
        try {
            if((!storeFile.exists()) || storeFile.length() == 0) {
                storeFile = new File(functionStorePath.substring(0,functionStorePath.lastIndexOf("/")));
                storeFile.mkdirs();

                storeFile = new File(functionStorePath);
                storeFile.createNewFile();

                // Create a default model with prefixes
                bw = new BufferedWriter(new FileWriter(storeFile,false));
                bw.write("@prefix fs:  <http://localhost/functionsstore#> .\n\n");
                bw.flush();
                bw.close();

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void deleteFunctionStoreEntry(String subject, Dataset data){
        String deleteQuery = "delete where { " + subject + " ?p ?o . }";
        UpdateRequest deleteRequest = UpdateFactory.create(deleteQuery);
        UpdateProcessor deleteProcessor = UpdateExecutionFactory.create(deleteRequest,data);
        deleteProcessor.execute();
    }

    private static void insertFunctionStoreEntry(String apiName, String dbName, String subject, int similarityRequests,
                                                 double stringSimilarity, double recordSimilarity, double responses,
                                                 String traversalPath, String preCondition, String postCondition,
                                                 List<Triple<String,String,Double>> remoteRelations,
                                                 Map<String,Pair<Long,Integer>> responseTimeMap, Dataset data,
                                                 Pair<Map<String,Double>,Map<String,Double>> supportConfidence,
                                                 JSONObject inputEntry)
    {
        JSONObject alignment = new JSONObject();
        Pair<Long,Integer> timePair = responseTimeMap.get(preCondition);
        long time = timePair.getKey()/timePair.getValue();

        StringBuilder insertQuery = new StringBuilder("prefix fs: <http://localhost/functionsstore#> " +
                "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> insert data { ");
        insertQuery.append(subject).append(" a fs:WebApiFunction . ");
        insertQuery.append(subject).append(" fs:knowledgeBase \"").append(dbName).append("\" . ");
        insertQuery.append(subject).append(" fs:api \"").append(apiName).append("\" . ");
        insertQuery.append(subject).append(" fs:preCondition <").append(preCondition).append("> . ");
        insertQuery.append(subject).append(" fs:postCondition <").append(postCondition).append("> . ");
        alignment.put("relation",postCondition);

        try{
            inputEntry.get("general");
        } catch (Exception e){
            JSONObject general = new JSONObject();
            general.put("inputRelation", preCondition);
            general.put("responseTime", time);
            general.put("responseProbability", (responses/similarityRequests));
            inputEntry.put("general",general);
        }

        // Combined linkage point
        JSONArray paths = new JSONArray();
        if(remoteRelations.size() > 1){
           for(Triple<String,String,Double> triple : remoteRelations) {
               insertQuery.append(subject).append(" fs:jsonPath \"").append(triple.getLeft()).append("\" . ");
               insertQuery.append(subject).append(" fs:similarityMetric \"").append(triple.getMiddle()).append("\" . ");
               insertQuery.append(subject).append(" fs:dataAvailability \"").append(String.format("%.2f", (triple.getRight()/responses) )).append("\" . ");

               JSONObject tmp = new JSONObject();
               tmp.put("path",triple.getLeft());
               tmp.put("dataAvailability",triple.getRight()/similarityRequests);
               tmp.put("confidence",triple.getRight()/responses);
               tmp.put("similarityMethod",triple.getMiddle());
               paths.put(tmp);
           }
       } else {
            insertQuery.append(subject).append(" fs:jsonPath \"").append(remoteRelations.get(0).getLeft()).append("\" . ");
            insertQuery.append(subject).append(" fs:similarityMetric \"").append(remoteRelations.get(0).getMiddle()).append("\" . ");
            insertQuery.append(subject).append(" fs:dataAvailability \"").append(String.format("%.2f", (remoteRelations.get(0).getRight()))).append("\" . ");

            JSONObject tmp = new JSONObject();
            tmp.put("path", remoteRelations.get(0).getLeft());
            tmp.put("dataAvailability", remoteRelations.get(0).getRight());
            tmp.put("confidence",remoteRelations.get(0).getRight());
            tmp.put("similarityMethod",remoteRelations.get(0).getMiddle());
            paths.put(tmp);
        }
        alignment.put("path",paths);

        // If a traverse path was used add the information to the function store
        if(!traversalPath.equals("")) {
            String[] traverseArray = traversalPath.split(", ");

            if(traverseArray.length > 1){
                insertQuery.append(subject).append(" fs:traversePath ").append("_:b").append(offset).append(" .");

                for (int i = 0; i < traverseArray.length; i++) {
                    insertQuery.append("_:b").append(offset + i).append(" rdf:first <").append(traverseArray[i]).append("> . ");
                    if(i+1 < traverseArray.length) insertQuery.append("_:b").append(offset + i).append(" rdf:rest ").append("_:b").append(offset + (i+1)).append(" .");
                }

                offset = traverseArray.length-1;
            } else {
                insertQuery.append(subject).append(" fs:traversePath <").append(traversalPath).append("> . ");
            }
            alignment.put("traversePath",traversalPath);
        }

        JSONArray array;
        try{
            array = inputEntry.getJSONArray("alignments");
            array.put(alignment);
        } catch (Exception e){
            array = new JSONArray();
            array.put(alignment);
        }
        inputEntry.put("alignments",array);

        // Support and Confidence
        if(supportConfidence != null && supportConfidence.getKey().size() > 0 && supportConfidence.getValue().size() > 0) {
            Map<String,Double> support = supportConfidence.getKey();
            Map<String,Double> confidence = supportConfidence.getValue();

            JSONArray apiArray = database.getJSONArray("apis");
            for (int i = 0; i < apiArray.length(); i++) {
                JSONObject obj = apiArray.getJSONObject(i);
                String tmp = obj.getString("label");

                if(tmp.equalsIgnoreCase(apiName)){
                    insertSupportConfidence(supportConfidence, obj);
                }
            }
        }

        // Linkage options
        insertQuery.append(subject).append(" fs:similarityRequests \"").append(similarityRequests).append("\" . ");
        insertQuery.append(subject).append(" fs:stringSimilarity \"").append(stringSimilarity).append("\" . ");
        insertQuery.append(subject).append(" fs:recordSimilarity \"").append(recordSimilarity).append("\" . ");

        // Quality metrics
        insertQuery.append(subject).append(" fs:averageResponseTime \"").append(time).append("\" . ");
        insertQuery.append(subject).append(" fs:responseProbability \"").append(String.format("%.2f", (responses/similarityRequests))).append("\" . ");

        insertQuery.append("}");

        UpdateRequest insertRequest = UpdateFactory.create(insertQuery.toString().replace("^",""));
        UpdateProcessor insertProcessor = UpdateExecutionFactory.create(insertRequest,data);
        insertProcessor.execute();
    }

    private static void insertSupportConfidence(Pair<Map<String,Double>,Map<String,Double>> supportConfidence, JSONObject obj){
        JSONArray array = new JSONArray();
        Map<String,Double> support = supportConfidence.getKey();
        Map<String,Double> confidence = supportConfidence.getValue();

        for(Map.Entry<String,Double> supportEntry : support.entrySet()) {
            JSONObject selectionEntry = new JSONObject();
            selectionEntry.put("type",supportEntry.getKey());
            selectionEntry.put("support",supportEntry.getValue());
            selectionEntry.put("confidence",confidence.get(supportEntry.getKey()));
            array.put(selectionEntry);
        }

        obj.put("selection", array);
    }

    private static void writeFunctionStore(String apiName, String dbName, int candidateRequests, int similarityRequests,
                                           double stringSimilarity, double recordSimilarity, double responses,
                                           Map<String,Pair<Long,Integer>> responseTimeMap,
                                           Map<String, Map<String,Map<String,Pair<String,Double>>>> alignments,
                                           Pair<Map<String,Double>,Map<String,Double>> supportConfidence)
    {
        String functionStorePath = ConfigurationLoader.getOutputPath() + "functionstore/functionstore.ttl";
        String jsonPath = ConfigurationLoader.getOutputPath() + "functionstore/" + dbName + "_" + apiName + ".json";
        Dataset data = RDFDataMgr.loadDataset(functionStorePath);

        // Globals
        JSONObject json = new JSONObject();
        JSONObject globals = new JSONObject();
        globals.put("apiName", apiName);
        globals.put("dbName", dbName);
        globals.put("probingSize", candidateRequests);
        globals.put("sampleSize", similarityRequests);
        globals.put("stringSimilarity", stringSimilarity);
        globals.put("recordSimilarity", recordSimilarity);
        json.put("globals",globals);

        JSONArray preconditionArray = new JSONArray();
        for(Map.Entry<String, Map<String,Map<String,Pair<String,Double>>>> preConditionMap : alignments.entrySet()){
            String preCondition = preConditionMap.getKey();
            JSONObject inputEntry = new JSONObject();

            for(Map.Entry<String,Map<String,Pair<String,Double>>> localRelationMap : preConditionMap.getValue().entrySet()){
                String[] localRelationPath = localRelationMap.getKey().split(", ");

                String postCondition;
                String traversePath = "";
                if(localRelationPath.length > 1){
                    postCondition = localRelationPath[localRelationPath.length-1];

                    for (int i = 0; i < localRelationPath.length - 1; i++) {
                        if(i+1 < localRelationPath.length-1){
                            traversePath += localRelationPath[i] + ", ";
                        } else {
                            traversePath += localRelationPath[i];
                        }
                    }

                } else {
                    postCondition = localRelationMap.getKey();
                }

                List<Triple<String,String,Double>> remoteRelations = new LinkedList<>();
                for(Map.Entry<String,Pair<String,Double>> remoteRelation : localRelationMap.getValue().entrySet()){
                    remoteRelations.add(new MutableTriple<>(remoteRelation.getKey(), remoteRelation.getValue().getKey(), remoteRelation.getValue().getValue()));
                }

                String postConditionShort;
                if(postCondition.contains("#")) postConditionShort = postCondition.substring(postCondition.indexOf("#")+1) + ">";
                else postConditionShort = postCondition.substring(postCondition.lastIndexOf("/")+1) + ">";

                String subject;
                if(preCondition.contains("#")){
                    subject = "<http://localhost/f/" + apiName + "_" + dbName + "_" + preCondition.substring(preCondition.indexOf("#")+1) + "_";
                } else {
                    subject = "<http://localhost/f/" + apiName + "_" + dbName + "_" + preCondition.substring(preCondition.lastIndexOf("/")+1) + "_";
                }

                if(traversePath.equals("")){
                    subject += postConditionShort;
                } else {
                    String[] pathArray = traversePath.split(", ");

                    StringBuilder tmp = new StringBuilder();
                    for (int i = 0; i < pathArray.length; i++) {
                        if(pathArray[i].contains("#")){
                            if(i+1 < pathArray.length){
                                tmp.append(pathArray[i].substring(pathArray[i].indexOf("#") + 1)).append("_");
                            } else {
                                tmp.append(pathArray[i].substring(pathArray[i].indexOf("#") + 1));
                            }
                        } else {
                            if(i+1 < pathArray.length){
                                tmp.append(pathArray[i].substring(pathArray[i].lastIndexOf("/") + 1)).append("_");
                            } else {
                                tmp.append(pathArray[i].substring(pathArray[i].lastIndexOf("/") + 1));
                            }
                        }
                    }

                    subject += tmp + "_" + postConditionShort;
                }

                // Delete old entry
                deleteFunctionStoreEntry(subject, data);

                // Insert new entry
                if(!preCondition.equals(postCondition)){
                    insertFunctionStoreEntry(apiName,dbName,subject,similarityRequests,stringSimilarity,recordSimilarity, responses,
                            traversePath,preCondition,postCondition,remoteRelations,responseTimeMap,data,supportConfidence,inputEntry);
                }

                // Write to disk
                try {
                    OutputStream out = new FileOutputStream(functionStorePath);
                    RDFDataMgr.write(out,data.getDefaultModel(), RDFFormat.TURTLE_PRETTY);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
            preconditionArray.put(inputEntry);
        }

        json.put("inputRelations",preconditionArray);

        JSONArray array = database.getJSONArray("apis");
        for (int i = 0; i < array.length(); i++) {
            JSONObject tmp = array.getJSONObject(i);
            if(tmp.getString("label").equals(apiName)){
                JSONObject obj = tmp.getJSONArray("parameters").getJSONObject(0);

                JSONArray filterArray = new JSONArray();
                for(Map.Entry<String, Map<String,Map<String,Pair<String,Double>>>> preCondition : alignments.entrySet()){
                    filterArray.put(preCondition.getKey());
                }

                obj.put("filter",filterArray);
            }
        }

        // Write Config
        File f = new File(jsonPath);
        f.delete();
        String databasePath = ConfigurationLoader.getDatabasePath();

        // Write support and confidence in database.json
        FileWriter file;
        try {
            file = new FileWriter(databasePath);
            file.write(database.toString());
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        data.end();
        data.close();

        writeJsonFile(jsonPath, json);
    }

    private static void writeJsonFile(String jsonPath, JSONObject obj){
        File storeFile = new File(jsonPath);
        BufferedWriter bw = null;
        try {
            if((!storeFile.exists()) || storeFile.length() == 0) {
                storeFile = new File(jsonPath.substring(0,jsonPath.lastIndexOf("/")));
                storeFile.mkdirs();

                storeFile = new File(jsonPath);
                storeFile.createNewFile();

                // Create a default model with prefixes
                bw = new BufferedWriter(new FileWriter(storeFile,false));
                bw.write(obj.toString());
                bw.flush();
                bw.close();

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
