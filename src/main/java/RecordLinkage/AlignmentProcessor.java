package RecordLinkage;

import QueryManagement.KnowledgeBaseManagement;
import QueryManagement.QueryProcessor;
import SchemaExtractor.SchemaExtractor;
import Similarity.Classifier.GbClassifier;
import Similarity.Classifier.RegExer;
import Similarity.StringSimilarityProcessor;
import Utils.Loader.ConfigurationLoader;
import Utils.Loader.DatabaseLoader;
import Utils.Loader.SchemaLoader;
import Utils.ReaderWriter.DiskReader;
import Utils.ReaderWriter.DiskWriter;
import Utils.Utils;
import WebApi.ArgumentException;
import WebApi.GeneralWebApi;
import WebApi.HttpResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.util.Pair;
import me.tongfei.progressbar.ProgressBar;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class AlignmentProcessor {
    // Constants
    private static final String interPath = ConfigurationLoader.getOutputPath() + "log/";
    private static final int MILLIS = ConfigurationLoader.getTimeout();
    private static final int DEBUG = ConfigurationLoader.getLogLevel();

    // Values
    private String apiName, dbName, inputType;
    private int candidateRequests, similarityRequests;
    private double stringSimThreshold, recordSimThreshold;

    // Needed Objects
    private GbClassifier classifier;

    // Data Structures
    private Map<String,Integer> numberOfEntitiesMap = new HashMap<>();              // Map<Predicate, Number of Entities using that predicate>
    private Map<String, List<Integer>> candidateOffsetMap = new HashMap<>();        // Map<Predicate, List<Offsets Values>>
    private Map<String,List<HttpResponse>> candidateResponseMap = new HashMap<>();  // Map<Predicate, List of API Responses>
    private Map<String,Integer> candidateMap = new HashMap<>();                     // Map<Predicate, Number of Responses>
    private List<Set<Pair<String,String>>> matchingRecords = new LinkedList<>();
    private List<Set<Pair<String,String>>> nonMatchingRecords = new LinkedList<>();
    private Map<String, Map<String,LinkedList<String[]>>> links = new HashMap<>();  // Map<Input Relation, Map<Local Relation, List<String[Remote Relation, Metric, Similarity]>>>
    private Map<String,Pair<Long,Integer>> responseTimeMap = new HashMap<>();

    public AlignmentProcessor(String apiName, String dbName, int candidateRequests, int similarityRequests,
                              double stringSimThreshold, double recordSimThreshold)
    {
        this.apiName = apiName;
        this.dbName = dbName;
        this.candidateRequests = candidateRequests;
        this.similarityRequests = similarityRequests;
        this.stringSimThreshold = stringSimThreshold;
        this.recordSimThreshold = recordSimThreshold;

        // Use AI
        if(!ConfigurationLoader.useRegex()) classifier = new GbClassifier(ConfigurationLoader.getIpcUrl());

        // Load input type if specified
        this.inputType = DatabaseLoader.getSingleInputType(apiName);
    }

    public void startRelationAligning(boolean calcSupportAndConfidence){
        // Determine identifier of the local data base
        String path = ConfigurationLoader.getOutputPath() + "data/" + this.dbName + "/schema/index.json";
        File file = new File(path);
        if(!file.exists()){
            String source = DatabaseLoader.getDatabaseSource(this.dbName);

            // In the CIKM2020 paper this component is called Identifier Extractor
            // Additionally it does also a schema inference in order to do a later optimized querying (future work)
            SchemaExtractor extractor = new SchemaExtractor(this.dbName,source,ConfigurationLoader.getFunctionalityThreshold());
            extractor.extractSchema();
        }

        System.out.println("[detectLinkagePoints]: Collecting all predicates..");
        getPredicates();

        System.out.println("[detectLinkagePoints]: Start to request Web API..");
        doProbingPhase();

        // Only try to find error responses if we got more than one valid input type.
        // And dont find error responses if the user itself configured the input
        if(!isFilterSet() && this.numberOfEntitiesMap.size() > 1 && this.candidateMap.size() > 1){
            removeErrorResponses();
        }

        doAligningPhase(this.matchingRecords,this.nonMatchingRecords);
        System.out.println("[detectLinkagePoints]: Valid Responses: " + this.matchingRecords.size());

        System.out.println("[detectLinkagePoints]: Determine Alignment..");
        Map<String, Map<String,Map<String,Pair<String,Integer>>>> alignments = determineLinkagePoints();
        if(DEBUG >= 1 || ConfigurationLoader.getMode() == 3){
            for (Map.Entry<String, Map<String, Map<String, Pair<String, Integer>>>> entry : alignments.entrySet()) {
                System.out.println("[detectLinkagePoints]: Found Alignments for " + entry.getKey() + ": " + entry.getValue().size());
                if(ConfigurationLoader.getMode() == 3){
                    for(Map.Entry<String, Map<String, Pair<String, Integer>>> alignment : entry.getValue().entrySet()){
                        System.out.println("- " + alignment.getKey());
                    }
                }
            }
        }

        // Compute support and confidence values
        Pair<Map<String,Double>,Map<String,Double>> supportConfidence = null;
        if(calcSupportAndConfidence){
            supportConfidence = JointFeatureProcessor.determineJointFeatures(matchingRecords, nonMatchingRecords);

            if(supportConfidence.getKey() == null || (supportConfidence.getKey() != null && supportConfidence.getKey().size() > 0))
                System.out.println("[detectLinkagePoints]: No support and confidence values found..");
        }

        // Create function store and write matching records to hard disk
        System.out.println("[detectLinkagePoints]: Write alignments to disk..");
        FunctionStoreManager.createFunctionStore(this.apiName,this.dbName,this.similarityRequests,
                this.stringSimThreshold,this.recordSimThreshold,this.matchingRecords.size(),this.responseTimeMap,alignments,supportConfidence);
    }

    /*******************************************************************************************************************
     * Helper Functions
     ******************************************************************************************************************/
    // Iterate over properties that might work as input type (e.g., doi, isbn, title, name)
    private void doAligningPhase(List<Set<Pair<String,String>>> matchingRecords, List<Set<Pair<String,String>>> nonMatchingRecords){
        // Iterate over input predicates
        try (ProgressBar pb = new ProgressBar("Aligning Phase", candidateMap.size() * ((this.similarityRequests - this.candidateRequests) + this.similarityRequests))) {
            for (Map.Entry<String, Integer> candidate : candidateMap.entrySet()) {
                int counter = 0;

                // If we had too less responses for the property we (can) ignore that property
                if (candidate.getValue() >= (candidateRequests * ConfigurationLoader.getCandidateResponses())) {
                    List<HttpResponse> responses = candidateResponseMap.get(candidate.getKey()); // Put responses from candidate phase into the list
                    List<Integer> requested = candidateOffsetMap.get(candidate.getKey());        // Do the same with their offset values

                     //Progress Bar and debugging
                    if(DEBUG > 0) {
                        if(candidate.getKey().contains("#")) pb.setExtraMessage(candidate.getKey().substring(candidate.getKey().indexOf("#")+1));
                        else  pb.setExtraMessage(candidate.getKey().substring(candidate.getKey().lastIndexOf("/")+1));
                    }

                    // Collect more random API records in order to determine the linkage points in a more precise way
                    if (DEBUG > 1) System.out.println("[collectAdditionalData]: Request additional data..");
                    getAdditional(candidate, requested, responses, nonMatchingRecords, pb);

                    // Not all requests result in a valid response.
                    for (int i = 0; i < this.similarityRequests - responses.size();i++){pb.step();}

                    // Iterate over all responses that got collected during the candidate set phase
                    for (int i = 0; i < responses.size(); i++) {
                        List<QuerySolution> localKnowledge = KnowledgeBaseManagement.getCompleteEntry(dbName, candidate.getKey(), requested.get(i));

                        // Filter application type and check if response is json. If not: convert the XML string to a JSON
                        // string and flatten afterwards. If yes: just flatten the JSON tree (output is a dictionary/map)
                        // Map<Remote Path, Remote Value>
                        Map<String, Object> apiResponse = ResponseConverter.convertResponse(responses.get(i));

                        // Request all information stored about the entity (also traverse over the graph)
                        Set<Pair<String, String>> fullKnowledge = KnowledgeBaseManagement.getFullKnowledge(this.dbName, inputType, candidate.getKey(), requested.get(i), localKnowledge);

                        Map<String, LinkedList<String[]>> relationMetrics = calculatePotentialLinkagePoints(KnowledgeBaseManagement.getPredicateValue(dbName, candidate.getKey(), requested.get(i)), apiResponse, fullKnowledge);

                        // Calculate overall record similarity, if high enough add to list of meaningful results
                        boolean enoughOverlapping = (apiResponse.size() > localKnowledge.size()
                                && relationMetrics.size() > (localKnowledge.size() * recordSimThreshold))
                                || (localKnowledge.size() > apiResponse.size() &&
                                relationMetrics.size() > (apiResponse.size() * recordSimThreshold));

                        if (enoughOverlapping) {
                            if (links.containsKey(candidate.getKey())) {
                                Map<String, LinkedList<String[]>> oldRelationMetrics = links.get(candidate.getKey());

                                for (Map.Entry<String, LinkedList<String[]>> entry : relationMetrics.entrySet()) {
                                    if (oldRelationMetrics.containsKey(entry.getKey())) {
                                        LinkedList<String[]> old = oldRelationMetrics.get(entry.getKey());
                                        old.addAll(entry.getValue());
                                        oldRelationMetrics.put(entry.getKey(), old);
                                    } else {
                                        oldRelationMetrics.put(entry.getKey(), entry.getValue());
                                    }
                                }

                                links.put(candidate.getKey(), oldRelationMetrics);
                            } else {
                                links.put(candidate.getKey(), relationMetrics);
                            }

                            matchingRecords.add(fullKnowledge);

                            // Evaluation Mode: Print API results in order to determine the recall
                            if (ConfigurationLoader.getMode() == 2 && counter < 5) {
                                List<String[]> csv = new LinkedList<>();
                                for (Map.Entry<String, Object> entry : apiResponse.entrySet()) {
                                    String value = "";
                                    try {
                                        value = entry.getValue().toString();
                                    } catch (Exception ignore) {
                                    }
                                    String[] line = {entry.getKey(), value};
                                    csv.add(line);
                                }
                                String name;
                                if (candidate.getKey().contains("#"))
                                    name = "api_" + candidate.getKey().substring(candidate.getKey().indexOf("#")) + requested.get(i);
                                else
                                    name = "api_" + candidate.getKey().substring(candidate.getKey().lastIndexOf("/") + 1) + requested.get(i);
                                DiskWriter.writeCsv("res/evaluation/data/" + name + ".csv", csv);

                                csv = new LinkedList<>();
                                for (Pair<String, String> entry : fullKnowledge) {
                                    String[] line = {entry.getKey(), entry.getValue()};
                                    csv.add(line);
                                }
                                if (candidate.getKey().contains("#"))
                                    name = "kb_" + candidate.getKey().substring(candidate.getKey().indexOf("#")) + requested.get(i);
                                else
                                    name = "kb_" + candidate.getKey().substring(candidate.getKey().lastIndexOf("/") + 1) + requested.get(i);
                                DiskWriter.writeCsv("res/evaluation/data/" + name + ".csv", csv);
                                counter++;
                            }
                        } else {
                            nonMatchingRecords.add(fullKnowledge);
                        }

                        pb.step();
                    }
                }
            }
        }

        if(classifier != null) classifier.close();
    }

    private void removeErrorResponses(){
        double threshold = ConfigurationLoader.getErrorThreshold();

        List<HttpResponse> responseList = new LinkedList<>();
        for(Map.Entry<String,List<HttpResponse>> tmp : candidateResponseMap.entrySet()){
            responseList.addAll(tmp.getValue());
        }

        try (ProgressBar pb = new ProgressBar("Remove Error Responses", 2)) {
            if(ConfigurationLoader.getLogLevel() >= 2) System.out.println("[removeErrorResponses] Determine error response..");
            Map<String,Integer> counterMap = new HashMap<>();
            for (int i = 0; i < responseList.size(); i = i+8) {
                int counter = 0;

                for (int j = 0; j < responseList.size(); j = j+8) {
                    // Use Levenshtein to compare JSON responses
                    if(i != j && org.sotorrent.stringsimilarity.edit.Variants.levenshtein(responseList.get(i).getContent(),responseList.get(j).getContent()) > threshold){
                        counter++;
                    }
                }

                counterMap.put(responseList.get(i).getContent(),counter);
            }
            pb.step();

            // Find potential error response
            try{
                Map.Entry<String,Integer> errorResponse = Collections.max(counterMap.entrySet(), Comparator.comparing(Map.Entry::getValue));
                if(errorResponse.getValue() >= this.candidateRequests * 0.3){
                    if(ConfigurationLoader.getLogLevel() >= 1) System.out.println("Error Response: " + errorResponse.getKey());

                    if(ConfigurationLoader.getLogLevel() >= 2) System.out.println("[removeErrorResponses] Clear map for error responses..");
                    Map<String,List<HttpResponse>> newCandidateResponseMap = new HashMap<>();
                    for(Map.Entry<String,List<HttpResponse>> tmp : candidateResponseMap.entrySet()){
                        List<HttpResponse> newResponseList = new LinkedList<>();

                        for(HttpResponse response : tmp.getValue()){
                            if(org.sotorrent.stringsimilarity.edit.Variants.levenshtein(response.getContent(),errorResponse.getKey()) < threshold){
                                newResponseList.add(response);
                            }
                        }

                        if(newResponseList.size() > 0) newCandidateResponseMap.put(tmp.getKey(),newResponseList);
                    }

                    candidateResponseMap.entrySet().removeIf(entry -> !newCandidateResponseMap.containsKey(entry.getKey()));
                    candidateMap.entrySet().removeIf(entry -> !newCandidateResponseMap.containsKey(entry.getKey()) || newCandidateResponseMap.get(entry.getKey()).size() <= candidateRequests * 0.1);
                }
            } catch (Exception ignored){}

            pb.step();
        }
    }

    private Map<String, Map<String,Map<String,Pair<String,Integer>>>> determineLinkagePoints(){
        Set<String> identifierPredicates = loadIdentifierPredicates();

        Map<String, Map<String,Map<String,Pair<String,Integer>>>> finalLinkagePoints = new HashMap<>();
        for(Map.Entry<String, Map<String,LinkedList<String[]>>> inputEntry : links.entrySet()){
            ObjectMapper mapper = new ObjectMapper();

            String predicateName;
            if(inputEntry.getKey().contains("#")) predicateName = inputEntry.getKey().substring(inputEntry.getKey().lastIndexOf("#")+1);
            else predicateName = inputEntry.getKey().substring(inputEntry.getKey().lastIndexOf("/")+1);

            String dirName = interPath + this.dbName + "_" + this.apiName + "/" + predicateName + "/";
            File directory = new File(dirName);
            if(!directory.exists()) directory.mkdirs();

            Map<String, Map<String, Pair<String,Integer>>> summedMetrics = createSummedMaxMetrics(inputEntry);
            try {
                mapper.writeValue(new File(dirName + "summedMetrics.json"),summedMetrics);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // After adding wildcards check if paths are equals (with the same metric used) and if yes,
            // then merge their count together. In this way we wanna find linkage points that belong together
            // such as first name and last name.
            Map<String, Map<String, Map<String,Integer>>> wildcardMap = createWildCardMap(summedMetrics);
            try {
                mapper.writeValue(new File(dirName + "wildcardMap.json"),wildcardMap);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Filter best metrics in wild cards
            Map<String, Map<String, Pair<String,Integer>>> bestWildCardMetrics = findBestWildCardMetrics(wildcardMap);
            try {
                mapper.writeValue(new File(dirName + "bestWildcardMap.json"),wildcardMap);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Determine if combined linkage points or not
            Map<String,Map<String,Pair<String,Integer>>> temporaryLinkagePoints = determineCombinedLinkagePoints(bestWildCardMetrics,identifierPredicates);

            // Search for the rest of the linkage points (wild card linkage points and fix linkage points)
            for(Map.Entry<String, Map<String,Pair<String,Integer>>> localRelationMap : summedMetrics.entrySet()){
                String relation = localRelationMap.getKey();

                if(!temporaryLinkagePoints.containsKey(relation)){
                    // Determine maximum value
                    String maxRemoteRelation = "";
                    int all = 0;
                    Pair<String,Integer> maxPair = new Pair<>("",-1);
                    for(Map.Entry<String,Pair<String,Integer>> remoteRelationMap : localRelationMap.getValue().entrySet()){
                        if(remoteRelationMap.getValue().getValue() > maxPair.getValue()) {
                            maxRemoteRelation = remoteRelationMap.getKey();
                            maxPair = remoteRelationMap.getValue();
                        }
                        all += remoteRelationMap.getValue().getValue();
                    }

                    // Only add to final linkage points if we had enough responses (second confidence metric)
                    if(maxPair.getValue() > ((this.matchingRecords.size()/this.links.size()) * ConfigurationLoader.getCandidateResponses())){
                        // Search matching wild card relations for the fix relation
                        Map<String,Integer> wildCardCandidates = createWildCardCandidates(localRelationMap, maxRemoteRelation);

                        // In case wildcard linkage point where found
                        if(wildCardCandidates.size() > 0){
                            String maxPrefix = "";
                            int maxPrefixCount = -1;

                            // Search the best wild card linkage point
                            for(Map.Entry<String,Integer> entry : wildCardCandidates.entrySet()) {
                                if(entry.getValue() > maxPrefixCount){
                                    maxPrefixCount = entry.getValue();
                                    maxPrefix = entry.getKey();
                                }
                            }

                            double wildCardConfidence = getWildCardConfidence(maxPrefix,localRelationMap);

                            // Only if we have enough confidence add the linkage point to the map
                            if((all > this.similarityRequests * this.recordSimThreshold) && (wildCardConfidence >= this.recordSimThreshold)){
                                String string = maxRemoteRelation.substring(maxPrefix.length());
                                string = string.substring(string.indexOf("]"));
                                string = maxPrefix + "*" + string;

                                Map<String,Pair<String,Integer>> tmp = new HashMap<>();
                                tmp.put(string,maxPair);
                                temporaryLinkagePoints.put(relation,tmp);
                            }
                        }

                        // In case of a fix linkage point
                        else {
                            //if((all > this.similarityRequests * ConfigurationLoader.getCandidateResponses()) && (maxPair.getValue() > all * ConfigurationLoader.getCandidateResponses())){
                            if(maxPair.getValue() >= (this.matchingRecords.size()/this.candidateMap.size()) * ConfigurationLoader.getCandidateResponses()){
                                Map<String,Pair<String,Integer>> tmp = new HashMap<>();
                                tmp.put(maxRemoteRelation,maxPair);
                                temporaryLinkagePoints.put(relation,tmp);
                            }
                        }
                    }
                }
            }

            finalLinkagePoints.put(inputEntry.getKey(),temporaryLinkagePoints);
        }

        return finalLinkagePoints;
    }

    private Map<String, Map<String, Pair<String,Integer>>> createSummedMaxMetrics(Map.Entry<String, Map<String,LinkedList<String[]>>> inputEntry){
        Map<String, Map<String, Pair<String,Integer>>> summedMetrics = new HashMap<>();

        // Iterate over potential linkage relations (predicates, paths)
        for(Map.Entry<String,LinkedList<String[]>> candidateEntry : inputEntry.getValue().entrySet()){
            List<String[]> candidateList = candidateEntry.getValue();

            // Count how often metrics are used
            Map<String, Map<String,Integer>> linkageMap = countMetrics(candidateList);

            // Search for maximum metric for each potential linkage point
            // (e.g., message.reference[1].title, message.reference[2].title)
            Map<String, Pair<String,Integer>> cleanedLinkageMap = findMaximumMetrics(linkageMap, candidateEntry.getKey());
            summedMetrics.put(candidateEntry.getKey(), cleanedLinkageMap);
        }

        return summedMetrics;
    }

    // Count how often metrics are used for all potential linkage points
    private Map<String, Map<String,Integer>> countMetrics(List<String[]> candidateList){
        Map<String, Map<String,Integer>> linkageMap = new HashMap<>();

        for(String[] linkage : candidateList){
            if(linkageMap.containsKey(linkage[0])){
                Map<String,Integer> metricCounter = linkageMap.get(linkage[0]);

                if(metricCounter.containsKey(linkage[1])){
                    int counter = metricCounter.get(linkage[1]) + 1;
                    metricCounter.put(linkage[1],counter);
                } else {
                    metricCounter.put(linkage[1],1);
                }

                linkageMap.put(linkage[0], metricCounter);
            } else {
                Map<String,Integer> tmp = new HashMap<>();
                tmp.put(linkage[1],1);
                linkageMap.put(linkage[0],tmp);
            }
        }

        return linkageMap;
    }

    // Search for maximum metric for each potential linkage point
    private Map<String, Pair<String,Integer>> findMaximumMetrics(Map<String, Map<String,Integer>> linkageMap, String candidateEntryKey){
        Map<String, Pair<String,Integer>> cleanedLinkageMap = new HashMap<>();

        for (Map.Entry<String, Map<String,Integer>> linkageEntry : linkageMap.entrySet()){
            Map<String,Integer> metricCounts = linkageEntry.getValue();

            String metric = "";
            int count = -1;
            for(Map.Entry<String,Integer> metricEntry : metricCounts.entrySet()){
                if(metricEntry.getValue() > count){
                    metric = metricEntry.getKey();
                    count = metricEntry.getValue();
                } else if(metricEntry.getValue() == count){
                    // If more than one maximum: Prefer the fuzzy metric (not equals)
                    // This is because equals is usually too strict for titles and so on
                    if(metric.equals("Equals") || metric.equals("Equals Normalized")){
                        metric = metricEntry.getKey();
                        count = metricEntry.getValue();
                    } else {
                        // The other maximum values will be ignored. Just take the first one
                        // With good numbers for probing and similarity requests this should not happen.
                        if(DEBUG > 1){
                            System.out.println("Problem: " + candidateEntryKey);
                            System.out.println(metric + " " + count);
                            System.out.println(metricEntry.getKey() + " " + metricEntry.getValue());
                        }
                    }
                }
            }

            cleanedLinkageMap.put(linkageEntry.getKey(), new Pair<>(metric,count));
        }

        return cleanedLinkageMap;
    }

    // Create wildcard linkage points e.g. message.author[*].name
    private Map<String, Map<String, Map<String,Integer>>> createWildCardMap(Map<String, Map<String, Pair<String,Integer>>> summedMetrics){
        Map<String, Map<String, Map<String,Integer>>> wildcardMap = new HashMap<>();

        for(Map.Entry<String, Map<String, Pair<String,Integer>>> localMap : summedMetrics.entrySet()){

            Map<String, Map<String,Integer>> newRemoteMap = new HashMap<>();
            for(Map.Entry<String, Pair<String,Integer>> remoteMap : localMap.getValue().entrySet()){
                // Replace the remote path with wildcard symbol (branching point)
                String remoteMapKey = remoteMap.getKey().replaceAll("\\[[0-9][0-9]*\\]","[*]");

                Map<String,Integer> metricsCountMap;
                if(newRemoteMap.containsKey(remoteMapKey)){
                    metricsCountMap = newRemoteMap.get(remoteMapKey);

                    if(metricsCountMap.containsKey(remoteMap.getValue().getKey())){
                        int counter = metricsCountMap.get(remoteMap.getValue().getKey()) + remoteMap.getValue().getValue();
                        metricsCountMap.put(remoteMap.getValue().getKey(),counter);
                    } else {
                        metricsCountMap.put(remoteMap.getValue().getKey(), remoteMap.getValue().getValue());
                    }

                } else {
                    metricsCountMap = new HashMap<>();
                    metricsCountMap.put(remoteMap.getValue().getKey(),remoteMap.getValue().getValue());
                }

                newRemoteMap.put(remoteMapKey,metricsCountMap);
            }

            // Remove metrics and linkage points that have to less confidence
            Iterator<Map.Entry<String, Map<String,Integer>>> it = newRemoteMap.entrySet().iterator();
            while(it.hasNext()){
                Map.Entry<String, Map<String,Integer>> entry = it.next();

                entry.getValue().entrySet().removeIf(
                        innerEntry -> innerEntry.getValue() < (this.matchingRecords.size() * ConfigurationLoader.getCandidateResponses())
                );

                if(entry.getValue().size() <= 0) it.remove();
            }

            if(newRemoteMap.size() > 0) wildcardMap.put(localMap.getKey(), newRemoteMap);
        }

        return wildcardMap;
    }

    private  Map<String, Map<String, Pair<String,Integer>>> findBestWildCardMetrics(Map<String, Map<String, Map<String,Integer>>> wildcardMap){
        Map<String, Map<String, Pair<String,Integer>>> bestWildCardMetrics = new HashMap<>();

        for(Map.Entry<String, Map<String, Map<String,Integer>>> localRelationMap : wildcardMap.entrySet()){
            Map<String,Pair<String,Integer>> tmp = new HashMap<>();
            for(Map.Entry<String,Map<String,Integer>> remoteRelationMap : localRelationMap.getValue().entrySet()){
                Pair<String,Integer> pair = new Pair<>("",-1);

                for(Map.Entry<String,Integer> entry : remoteRelationMap.getValue().entrySet()){
                    // If greater (search for max) or is a fuzzy metric (we want to use equals only if really needed)
                    // That makes more sense for strings like a title, name etc.
                    if(entry.getValue() > pair.getValue() || (entry.getValue() == pair.getValue() && (pair.getKey().equals("Equals")
                            || pair.getKey().equals("Equals Normalized"))))
                    {
                        int localLevel = Math.max(localRelationMap.getKey().split(", ").length,1);
                        int remoteLevel = Math.max((int) remoteRelationMap.getKey().chars().filter(ch -> ch == '.').count()+1,1);

                        int discount = 1;
                        int diff = localLevel - remoteLevel;
                        if(diff != 0){
                            if(diff > 0) discount += diff;
                            else discount += (diff * (-1));
                        }

                        pair = new Pair<>(entry.getKey(), entry.getValue()/discount);
                    }
                }

                tmp.put(remoteRelationMap.getKey(),pair);
            }

            bestWildCardMetrics.put(localRelationMap.getKey(),tmp);
        }

        return bestWildCardMetrics;
    }

    private Map<String,Pair<String,Integer>> determineCombinedRelation(Map<String,Pair<String,Integer>> maxMap,
                                                                       Map.Entry<String, Map<String,Pair<String,Integer>>> localRelationMap)
    {
        Map<String,Pair<String,Integer>> combinedMap = new HashMap<>();

        for(Map.Entry<String,Pair<String,Integer>> maxEntry : maxMap.entrySet()){
            String maxRelation = maxEntry.getKey();
            try{
                for(Map.Entry<String,Pair<String,Integer>> remoteRelationMap : localRelationMap.getValue().entrySet()) {
                    String relation = remoteRelationMap.getKey();

                    // Determine the longest common subsequence
                    int index = Utils.getLongestSubsequenceLength(maxRelation,relation);
                    String prefixMaxRelation = maxRelation.substring(0,index);
                    String prefixRelation = relation.substring(0,index);

                    // Only the last past of the part is allowed to change if its a combined linkage point
                    String maxRelationSuffix = maxRelation.substring(index);
                    String relationSuffix = relation.substring(index);

                    boolean notEmptyAndLeaf = (!maxRelationSuffix.equals("") && !maxRelationSuffix.contains("."))
                            && (!relationSuffix.equals("") && !relationSuffix.contains("."));

                    if(prefixMaxRelation.equals(prefixRelation) && !maxRelation.equals(relation) && notEmptyAndLeaf){
                        double threshold = (1 - ConfigurationLoader.getDistributionVariance());

                        // if the linkage points appear most of the time together we guess that it is a combined
                        // linkage point, e.g., first and last name. The threshold is calculated via the distribution
                        // variance. This score tells how much difference in the occurrence is allowed in order
                        // to yield as a combined linkage point
                        if(((double) remoteRelationMap.getValue().getValue() / maxEntry.getValue().getValue()) >= threshold){
                            combinedMap.put(maxRelation,maxEntry.getValue());
                            combinedMap.put(relation,remoteRelationMap.getValue());
                        }
                    }
                }
            } catch (Exception ignored){}
        }

        return combinedMap;
    }

    private Map<String,Map<String,Pair<String,Integer>>> determineCombinedLinkagePoints(Map<String, Map<String, Pair<String,Integer>>> bestWildCardMetrics,
                                                                                        Set<String> identifierPredicates)
    {
        Map<String,Map<String,Pair<String,Integer>>> temporaryLinkagePoints = new HashMap<>();

        for(Map.Entry<String, Map<String,Pair<String,Integer>>> localRelationMap : bestWildCardMetrics.entrySet()){
            String[] predicate = localRelationMap.getKey().split(", ");

            if(!identifierPredicates.contains(predicate[predicate.length-1]) && localRelationMap.getValue().size() > 1){
                // Determine maximum value
                int max = -1;
                for(Map.Entry<String,Pair<String,Integer>> remoteRelationMap : localRelationMap.getValue().entrySet()){
                    if(remoteRelationMap.getValue().getValue() > max){
                        max = remoteRelationMap.getValue().getValue();
                    }
                }

                // Collect all relations that are maximum
                Map<String,Pair<String,Integer>> maxMap = new HashMap<>();
                for(Map.Entry<String,Pair<String,Integer>> remoteRelationMap : localRelationMap.getValue().entrySet()) {
                    if(remoteRelationMap.getValue().getValue() == max){
                        maxMap.put(remoteRelationMap.getKey(),remoteRelationMap.getValue());
                    }
                }

                // collect linkage points that have to be combined
                Map<String,Pair<String,Integer>> combinedMap = determineCombinedRelation(maxMap,localRelationMap);

                if(combinedMap.size() > 0){
                    temporaryLinkagePoints.put(localRelationMap.getKey(),combinedMap);
                }
            }
        }

        return temporaryLinkagePoints;
    }

    private Map<String,Integer> createWildCardCandidates(Map.Entry<String, Map<String,Pair<String,Integer>>> localRelationMap,
                                                         String maxRemoteRelation)
    {
        Map<String,Integer> wildCardCandidates = new HashMap<>();

        for(Map.Entry<String,Pair<String,Integer>> remoteRelationMap : localRelationMap.getValue().entrySet()){
            String remoteRelation = remoteRelationMap.getKey();

            String wildMaxRemoteRelation = maxRemoteRelation.replaceAll("\\[[0-9][0-9]*\\]","[*]");
            String wildRemoteRelation = remoteRelation.replaceAll("\\[[0-9][0-9]*\\]","[*]");

            if(wildMaxRemoteRelation.equals(wildRemoteRelation) && !maxRemoteRelation.equals(remoteRelation)){
                int len = wildMaxRemoteRelation.length();
                int end = Utils.getLongestSubsequenceLength(maxRemoteRelation, remoteRelation);

                String subsequence;
                if(len != end) subsequence = maxRemoteRelation.substring(0,end);
                else subsequence = wildMaxRemoteRelation;

                if(wildCardCandidates.containsKey(subsequence)){
                    int counter = wildCardCandidates.get(subsequence) + 1;
                    wildCardCandidates.put(subsequence, counter);
                } else {
                    wildCardCandidates.put(subsequence, 2);
                }
            }
        }

        return wildCardCandidates;
    }

    private double getWildCardConfidence(String maxPrefix, Map.Entry<String, Map<String,Pair<String,Integer>>> localRelationMap) {
        double counter = 0;
        double all = 0;
        for(Map.Entry<String,Pair<String,Integer>> remoteRelationMap : localRelationMap.getValue().entrySet()){
            if(remoteRelationMap.getKey().startsWith(maxPrefix)){
                counter += remoteRelationMap.getValue().getValue();
            }

            all += remoteRelationMap.getValue().getValue();
        }

        return (counter/all);
    }

    private Map<String,LinkedList<String[]>> calculatePotentialLinkagePoints(String preCondition, Map<String, Object> apiResponse, Set<Pair<String,String>> fullKnowledge) {
        Map<String,LinkedList<String[]>> relationMetrics = new HashMap<>();

        for(Map.Entry<String,Object> apiEntry : apiResponse.entrySet()){
            String apiRelation = apiEntry.getKey().trim();

            // Values at API side can be empty and therefore result in a null pointer error*
            String apiValue;
            try{
                apiValue = apiEntry.getValue().toString().trim();

                if(!apiValue.equals(preCondition)){
                    for(Pair<String,String> localEntry : fullKnowledge){
                        String dbValue = localEntry.getValue();
                        String dbRelation = localEntry.getKey();
                        String[] relationArray = dbRelation.split(", "); // relation can be a path of multiple predicates (separated by comma)

                        // Comparision of URLs and numbers needs to be different than compared to strings (e.g. title vs year)
                        // URLs and numbers should therefore be equal and not similar (a year off by one is no match)
                        Pair<String,Double> originalComputation;
                        if(Utils.isNumeric(dbValue) || Utils.isNumeric(apiValue) || Utils.isUrl(dbValue) || Utils.isUrl(apiValue)){
                            double similarity = StringSimilarityProcessor.computeSimilarity(dbValue,apiValue,"Equal");
                            originalComputation = new Pair<>("Equal",similarity);
                        } else {
                            originalComputation = StringSimilarityProcessor.computeSimilarity(apiValue,dbValue);
                        }

                        // If similarity is high enough to be considered as a match we will add it to our map
                        if(originalComputation.getValue() >= stringSimThreshold){
                            Set<String> identifierPredicates = loadIdentifierPredicates();
                            String[] tmp = null;

                            // In case its an identifier use the pretrained classifier (gradient boosting)
                            if(identifierPredicates.contains(relationArray[relationArray.length-1])){
                                boolean equals = false;
                                String method = "";
                                if(ConfigurationLoader.useRegex()){
                                    String[] dbRelationArray = dbRelation.split(", ");
                                    String type = SchemaLoader.getIdentifierType(dbRelationArray[dbRelationArray.length-1],this.dbName);

                                    // If no filter is specified then search for the best matching one otherwise use the filter
                                    if(type != null && type.equals("none")){
                                        List<String> types = SchemaLoader.getIdentifierTypes();
                                        for(String t : types){
                                            if(RegExer.isEqual(apiValue,dbValue,ConfigurationLoader.getRegex(t))){
                                                equals = true;
                                                if(Objects.requireNonNull(ConfigurationLoader.getRegex(t)).contains("/f")){
                                                    method = "RegExer Fuzzy (" + originalComputation.getKey() + ")";
                                                } else {
                                                    method = "RegExer " + t;
                                                }
                                                break;
                                            }
                                        }
                                        if(apiValue.equals(dbValue)){
                                            method = "RegExer Equal";
                                        }
                                    } else {
                                        String filter = ConfigurationLoader.getRegex(type);
                                        equals = RegExer.isEqual(apiValue,dbValue,filter);
                                        if(Objects.requireNonNull(filter).contains("/f")){
                                            method = "RegExer Fuzzy (" + originalComputation.getKey() + ")";
                                        } else {
                                            method = "RegExer " + type;
                                        }
                                    }

                                } else {
                                    equals = apiValue.equals(dbValue) || classifier.isEquals(apiValue,dbValue);
                                    method = "Classifier";
                                }

                                if(equals) {
                                    tmp = new String[3];
                                    tmp[0] = apiRelation;                   // JSON Path
                                    tmp[1] = method;                        // Used Similarity Metric
                                    tmp[2] = "1";                           // Similarity (value)
                                }
                            } else {
                                tmp = new String[3];
                                tmp[0] = apiRelation;                                   // JSON Path
                                tmp[1] = originalComputation.getKey();                  // Used Similarity Metric
                                tmp[2] = originalComputation.getValue().toString();     // Similarity (value)

                            }

                            if (tmp != null) {
                                if(relationMetrics.containsKey(dbRelation)){
                                    LinkedList<String[]> metricList = relationMetrics.get(dbRelation);
                                    metricList.add(tmp);
                                    relationMetrics.put(dbRelation,metricList);
                                } else {
                                    LinkedList<String[]> metricList = new LinkedList<>();
                                    metricList.add(tmp);
                                    relationMetrics.put(dbRelation,metricList);
                                }
                            }
                        }
                    }
                }
            } catch (NullPointerException ignored){}
        }

        return relationMetrics;
    }

    // Collects all available predicates in the local knowledge base
    private void getPredicates(){
        String indexPath = ConfigurationLoader.getOutputPath() + "data/" + dbName + "/schema/index.json";
        String predicatePath = ConfigurationLoader.getOutputPath() + "data/" + dbName + "/predicates/predicates.csv";
        File predicateIndex = new File(predicatePath);
        boolean existPredicates = predicateIndex.exists();

        // In case predicates are already extracted/loaded in an external file (only used for speed boost)
        if (existPredicates) {
            try {
                Scanner sc = new Scanner(predicateIndex);
                while (sc.hasNext()){
                    String[] value = sc.nextLine().split(",");

                    if(this.inputType.equals(value[0].trim()))
                        numberOfEntitiesMap.put(value[1].trim(),Integer.parseInt(value[2].trim()));
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

        // Similar to the above case but now a schema of the local knowledge base exists. All used predicates will be
        // therefore extracted from the schema file
        } else {
            JSONArray array = new JSONObject(Objects.requireNonNull(DiskReader.readFile(indexPath))).getJSONArray("structure");
            for (int i = 0; i < array.length(); i++) {
                    if(array.getJSONObject(i).getString("subject").equals(this.inputType)){
                        String tmpPredicate = array.getJSONObject(i).getString("predicate");
                        int tmpUsage = array.getJSONObject(i).getInt("occurrence");
                        if(numberOfEntitiesMap.containsKey(tmpPredicate)){
                            numberOfEntitiesMap.put(tmpPredicate,numberOfEntitiesMap.get(tmpPredicate)+tmpUsage);
                        } else {
                            numberOfEntitiesMap.put(tmpPredicate,tmpUsage);
                        }
                    }
            }
        }

        // In case a technical user has specified an input relation for the used API remove all relations that do not match
        List<String> filters = DatabaseLoader.getSingleInputTypeFilter(this.apiName);
        if(filters != null) numberOfEntitiesMap.entrySet().removeIf(entry -> !filters.contains(entry.getKey()));

        if(DEBUG > 0) writePredicatesToDisk(numberOfEntitiesMap,predicatePath);
    }

    // Method to request all classes/types in the local knowledge base
    private List<QuerySolution> getAllClasses(){
        String queryClassesString = "select distinct ?class where { ?s a ?class . }";
        QueryProcessor qp = new QueryProcessor(queryClassesString, Objects.requireNonNull(DatabaseLoader.getDatabaseUrl(dbName)));
        List<QuerySolution> classList = ResultSetFormatter.toList(qp.query());
        qp.close();

        int offset = classList.size();
        int limit = classList.size();

        // Repeat with limit and offset to crawl all data even if server has a limit on the result size
        boolean repeat = classList.size() > 0;
        List<QuerySolution> tmp;
        while(repeat){
            String query = "select distinct ?class where { ?s a ?class . } offset " + offset + " limit " + limit;
            if(DEBUG >= 2) System.out.println("[getAllClasses]: " + query);
            qp = new QueryProcessor(query, Objects.requireNonNull(DatabaseLoader.getDatabaseUrl(dbName)));
            tmp = ResultSetFormatter.toList(qp.query());
            qp.close();

            if(tmp.size() > 0){
                classList.addAll(tmp);
                offset = classList.size();
                limit = tmp.size();
            } else {
                repeat = false;
            }
        }

        return classList;
    }


    // Method to request how often the predicates are instantiated
    private void getPredicateUsageBySparql(Map<String,Integer> predicates){

        int i = 1;
        for (Map.Entry<String,Integer> entry : predicates.entrySet()){
            if(DEBUG >= 2) System.out.println("[getPredicatesBySparql]: Request usage ("+(i++)+"/"+predicates.size()+")");
            if(entry.getValue() == -1){
                String queryClassesString = "select (count(?s) as ?no) where { ?s a <" + this.inputType + "> . ?s <" + entry.getKey() + "> ?o . }";
                QueryProcessor qp = new QueryProcessor(queryClassesString, Objects.requireNonNull(DatabaseLoader.getDatabaseUrl(dbName)));
                ResultSet result = qp.query();

                String tmp = result.next().get("no").toString();
                tmp = tmp.substring(0,tmp.indexOf("^^"));
                predicates.put(entry.getKey(),Integer.parseInt(tmp));

                qp.close();
            }
        }
    }

    // Method to collect a first sample of predicates which can be used as input type for a Web API
    // Results of Web APIs are stored to later determine the linkage points between knowledge base and Web API
    private void doProbingPhase(){
        // Iterate over all predicates
        try (ProgressBar pb = new ProgressBar("Probing Phase", numberOfEntitiesMap.size() * this.candidateRequests)) {
            for (Map.Entry<String,Integer> entry : numberOfEntitiesMap.entrySet()) {
                // If predicate is used to define types and classes don't use it as input parameter
                if(!StructuralPredicates.contains(entry.getKey())){
                    String predicate = entry.getKey();
                    int numberOfEntities = entry.getValue();

                    // Progress Bar
                    if(DEBUG > 0 || ConfigurationLoader.getMode() == 3) {
                        if(predicate.contains("#")) pb.setExtraMessage(predicate.substring(predicate.indexOf("#")+1));
                        else  pb.setExtraMessage(predicate.substring(predicate.lastIndexOf("/")+1));
                    }

                    // In case that for the predicate no number of Entities is available
                    // request the number of entities via SPARQL
                    int maxNumber;
                    if(numberOfEntities == -1) {
                        System.out.println("Request number of possible values of " + predicate);
                        maxNumber = KnowledgeBaseManagement.getNumberOfEntities(dbName, predicate);
                    }
                    else maxNumber = numberOfEntities;

                    // For the case that the number of entities is smaller than the number of initial requests
                    // store maxNumber in order to not make a second request
                    int loops = Math.min(maxNumber, candidateRequests);
                    numberOfEntitiesMap.put(predicate,maxNumber);

                    List<Integer> offsetList = new LinkedList<>();
                    List<HttpResponse> responseList = new LinkedList<>();
                    for (int i = 0; i < loops;) {
                        Random rand = new Random();
                        int offset = rand.nextInt(maxNumber);
                        while (offsetList.contains(offset)) { offset = rand.nextInt(maxNumber); }

                        // change that also more than one API parameter is possible (future task)
                        String value = KnowledgeBaseManagement.getPredicateValue(dbName,predicate,offset);
                        if(value == null){
                            if(DEBUG > 1) System.out.println("[getCandidateSet]: Null " + offsetList.size());
                            if(offsetList.size() >= loops) break;
                            else continue;
                        }

                        // Request Web API and add the response to responseList
                        doApiRequest(value,predicate,offset,offsetList,responseList);
                        i++;
                        pb.step();
                    }

                    // Only used for progress bar
                    for (int i = 0; i < this.candidateRequests-loops; i++) {pb.step();}

                    candidateOffsetMap.put(predicate,offsetList);
                    candidateResponseMap.put(predicate,responseList);
                } else {
                    pb.step();
                }
            }
        }

        if(ConfigurationLoader.getMode() == 3){
            System.out.println("Result of Probing Phase:");
            for (Map.Entry<String, Integer> candidate : candidateMap.entrySet()) {
                if (candidate.getValue() >= (candidateRequests * ConfigurationLoader.getCandidateResponses()))
                    System.out.println("- " + candidate.getKey() + " is a valid input parameter for " + this.apiName);
            }
        }

    }

    // Method to request additional responses from the Web API in order to determine linkage points
    private void getAdditional(Map.Entry<String,Integer> candidate, List<Integer> requested,
            List<HttpResponse> responses, List<Set<Pair<String,String>>> nonMatchingRecords, ProgressBar pb)
    {
        int size = candidateRequests > numberOfEntitiesMap.get(candidate.getKey()) ? numberOfEntitiesMap.get(candidate.getKey()) : similarityRequests;
        for (int i = this.candidateRequests; i < size; i++) {
            Random rand = new Random();
            int offset = rand.nextInt(numberOfEntitiesMap.get(candidate.getKey()));
            while (requested.contains(offset)) offset = rand.nextInt(numberOfEntitiesMap.get(candidate.getKey()));

            String value = KnowledgeBaseManagement.getPredicateValue(dbName, candidate.getKey(), offset);
            boolean sucessful = doApiRequest(value, candidate.getKey(), offset, requested, responses);

            if (DEBUG > 1) System.out.println("[getAdditional]: Request Status " + sucessful);

            if(!sucessful) {
                List<QuerySolution> localKnowledge = KnowledgeBaseManagement.getCompleteEntry(dbName, candidate.getKey(), offset);
                Set<Pair<String, String>> fullKnowledge = KnowledgeBaseManagement.getFullKnowledge(this.dbName, this.inputType, candidate.getKey(), offset, localKnowledge);
                nonMatchingRecords.add(fullKnowledge);
            }

            pb.step();
        }
    }

    private void writePredicatesToDisk(Map<String,Integer> predicates, String predicatePath){
        // Prepare data to write it on disk
        StringBuilder output = new StringBuilder();
        for (Map.Entry<String,Integer> entry : predicates.entrySet()) {
            if(!StructuralPredicates.contains(entry.getKey())){
                if(this.inputType != null) output.append(this.inputType).append(", ").append(entry.getKey()).append(", ").append(entry.getValue()).append("\n");
                else output.append(entry.getKey()).append(", ").append(entry.getValue()).append("\n");
            }
        }

        // Write data to disk
        try {
            DiskWriter.writeLineToFile(predicatePath, output.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Request the Web API and add the response to the list responseList
    private boolean doApiRequest(String value, String p, int offset, List<Integer> offsetList, List<HttpResponse> responseList) {
        boolean status;

        GeneralWebApi api = new GeneralWebApi(this.apiName);
        try {
            String url = api.buildCallUrl(true, value);
            long sTime = System.currentTimeMillis();
            HttpResponse response = api.doApiCall(url);
            long eTime = System.currentTimeMillis();
            long responseTime = (eTime - sTime);

            // Check if we got a response or just an empty response
            if(response != null && response.getContent() != null)
            {
                if(candidateMap.containsKey(p)){
                    candidateMap.put(p,candidateMap.get(p)+1);
                } else {
                    candidateMap.put(p,1);
                }
                offsetList.add(offset);
                responseList.add(response);

                // Time measurement for each input type
                Pair<Long,Integer> newIndex;
                if(responseTimeMap.containsKey(p)){
                    Pair<Long,Integer> index = responseTimeMap.get(p);
                    newIndex = new Pair<>(index.getKey() + responseTime,index.getValue()+1);
                } else {
                   newIndex = new Pair<>(responseTime,1);
                }
                responseTimeMap.put(p,newIndex);
                status = true;

            // In case we got no response
            } else {
                status = false;
            }

        // for the case of some general and server errors
        } catch (ArgumentException | UnsupportedEncodingException e) {
            e.printStackTrace();
            status = false;
        }

        // Wait some time so that the Web API won't be flooded
        try {
            TimeUnit.MILLISECONDS.sleep(MILLIS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return status;
    }

    // Method to load the predicates that are used as identifier in the local knowledge base. Clearly identifier should
    // be compared by using equals() instead of some similarity methods like Jaro-Winkler
    private Set<String> loadIdentifierPredicates(){
        Set<String> identifierPredicates = new HashSet<>();

        JSONArray array = SchemaLoader.getIdentifierPredicates(this.dbName);
        for (int i = 0; i < array.length(); i++) {
            identifierPredicates.add(array.getJSONObject(i).getString("predicate"));
        }

        return identifierPredicates;
    }

    private boolean isFilterSet(){
        return DatabaseLoader.getSingleInputTypeFilter(this.apiName) != null;
    }

}
