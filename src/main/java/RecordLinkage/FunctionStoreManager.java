package RecordLinkage;

import Utils.Loader.ConfigurationLoader;
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

import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

class FunctionStoreManager {

    static int offset = 0;

    static void createFunctionStore(String apiName, String dbName, int similarityRequests,
                                    double stringSimilarity, double recordSimilarity, double responses,
                                    Map<String,Pair<Long,Integer>> responseTimeMap,
                                    Map<String,Map<String,Map<String,Pair<String,Integer>>>> linkagePoints,
                                    Pair<Map<String,Double>,Map<String,Double>> supportConfidence)
    {
        String functionStorePath = ConfigurationLoader.getOutputPath() + "functionstore/functionstore.ttl";
        initFunctionStore(functionStorePath);
        writeFunctionStore(apiName, dbName, similarityRequests,stringSimilarity,recordSimilarity,responses,responseTimeMap,linkagePoints,supportConfidence);
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
                                                 List<Triple<String,String,Integer>> remoteRelations,
                                                 Map<String,Pair<Long,Integer>> responseTimeMap, Dataset data,
                                                 Pair<Map<String,Double>,Map<String,Double>> supportConfidence)
    {
        Pair<Long,Integer> timePair = responseTimeMap.get(preCondition);
        long time = timePair.getKey()/timePair.getValue();

        StringBuilder insertQuery = new StringBuilder("prefix fs: <http://localhost/functionsstore#> " +
                "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> insert data { ");
        insertQuery.append(subject).append(" a fs:WebApiFunction . ");
        insertQuery.append(subject).append(" fs:knowledgeBase \"").append(dbName).append("\" . ");
        insertQuery.append(subject).append(" fs:api \"").append(apiName).append("\" . ");
        insertQuery.append(subject).append(" fs:preCondition <").append(preCondition).append("> . ");
        insertQuery.append(subject).append(" fs:postCondition <").append(postCondition).append("> . ");

        // Combined linkage point
       if(remoteRelations.size() > 1){
           // TODO Use entities for path (in case of multiple paths)
           for(Triple<String,String,Integer> triple : remoteRelations) {
               insertQuery.append(subject).append(" fs:jsonPath \"").append(triple.getLeft()).append("\" . ");
               insertQuery.append(subject).append(" fs:similarityMetric \"").append(triple.getMiddle()).append("\" . ");
               insertQuery.append(subject).append(" fs:dataAvailability \"").append(String.format("%.2f", (triple.getRight()/responses))).append("\" . ");
           }
       } else {
           insertQuery.append(subject).append(" fs:jsonPath \"").append(remoteRelations.get(0).getLeft()).append("\" . ");
           insertQuery.append(subject).append(" fs:similarityMetric \"").append(remoteRelations.get(0).getMiddle()).append("\" . ");
           insertQuery.append(subject).append(" fs:dataAvailability \"").append(String.format("%.2f", (remoteRelations.get(0).getRight()/responses))).append("\" . ");
       }

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
        }

        // Support and Confidence
        if(supportConfidence != null) {
            Map<String,Double> support = supportConfidence.getKey();
            Map<String,Double> confidence = supportConfidence.getValue();

            int id = 1;
            for(Map.Entry<String,Double> supportEntry : support.entrySet()){
                // Build link to entity
                String responseEnhancement = "<http://localhost/f/" + apiName + (id++) + ">";
                insertQuery.append(subject).append(" fs:responseEnhancement ").append(responseEnhancement).append(" .");

                // Build information for the entity
                insertQuery.append(responseEnhancement).append(" a ").append("fs:ResponseFeature .");
                insertQuery.append(responseEnhancement).append(" fs:apiName ").append("\"").append(apiName).append("\" .");
                insertQuery.append(responseEnhancement).append(" fs:value ").append("\"").append(supportEntry.getKey()).append("\"").append(" .");
                insertQuery.append(responseEnhancement).append(" fs:support ").append("\"").append(supportEntry.getValue()).append("\"").append(" .");
                insertQuery.append(responseEnhancement).append(" fs:confidence ").append("\"").append(confidence.get(supportEntry.getKey())).append("\"").append(" .");

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

    private static void writeFunctionStore(String apiName, String dbName, int similarityRequests,
                                           double stringSimilarity, double recordSimilarity, double responses,
                                           Map<String,Pair<Long,Integer>> responseTimeMap,
                                           Map<String, Map<String,Map<String,Pair<String,Integer>>>> linkagePoints,
                                           Pair<Map<String,Double>,Map<String,Double>> supportConfidence)
    {
        String functionStorePath = ConfigurationLoader.getOutputPath() + "functionstore/functionstore.ttl";
        Dataset data = RDFDataMgr.loadDataset(functionStorePath);

        for(Map.Entry<String, Map<String,Map<String,Pair<String,Integer>>>> preConditionMap : linkagePoints.entrySet()){
            String preCondition = preConditionMap.getKey();

            for(Map.Entry<String,Map<String,Pair<String,Integer>>> localRelationMap : preConditionMap.getValue().entrySet()){
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

                List<Triple<String,String,Integer>> remoteRelations = new LinkedList<>();
                for(Map.Entry<String,Pair<String,Integer>> remoteRelation : localRelationMap.getValue().entrySet()){
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
                            traversePath,preCondition,postCondition,remoteRelations,responseTimeMap,data,supportConfidence);
                }

                // Write to disk
                try {
                    OutputStream out = new FileOutputStream(functionStorePath);
                    RDFDataMgr.write(out,data.getDefaultModel(), RDFFormat.TURTLE_PRETTY);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }

        data.end();
        data.close();
    }
}
