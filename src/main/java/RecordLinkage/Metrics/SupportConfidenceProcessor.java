package RecordLinkage.Metrics;

import Utils.Loader.ConfigurationLoader;
import Utils.Loader.DatabaseLoader;
import javafx.util.Pair;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

public class SupportConfidenceProcessor {

    public static void computeSupportAndConfidence(SCConfiguration config) {
        double minSupport = DatabaseLoader.getMinSupport(config.apiName);
        double minConfidence = DatabaseLoader.getMinConfidence(config.apiName);

        Map<String,Double> matchingSimilarities = countProperties(config.matches);
        Map<String,Double> nonMatchingSimilarities = countProperties(config.nonMatches);

        Map<String,Double> predicateProbabilities = getAllPredicateProbabilities(matchingSimilarities,nonMatchingSimilarities,config.similarityRequests);
        Map<String,Pair<Double,Double>> supportConfidenceMap = new HashMap<>();

        for(Map.Entry<String,Double> match : matchingSimilarities.entrySet()){
            double prob = config.validResponses/config.similarityRequests;
            double sup = predicateProbabilities.get(match.getKey());
            double conditionalProb = 0.0;
            if(predicateProbabilities.containsKey(match.getKey()))
                conditionalProb = (match.getValue()/config.similarityRequests)/sup;

            if(conditionalProb > prob) {
                if(sup > minSupport && conditionalProb > minConfidence){
                    supportConfidenceMap.put(match.getKey(),new Pair<>(sup,conditionalProb));
                }
            }
        }

        // Sort map in decreasing order by confidence
        supportConfidenceMap = supportConfidenceMap.entrySet().stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue(Comparator.comparing(Pair::getKey))))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        writeSupportAndConfidenceAsJson(config.apiName,config.dbName,supportConfidenceMap);
    }

    private static Map<String,Double> countProperties(List<Set<Pair<String,String>>> recordList){
        Map<String,Double> map = new HashMap<>();
        for(Set<Pair<String,String>> set : recordList){
            for(Pair<String,String> pair : set){
                String key = pair.getKey() + ", " + pair.getValue();

                if(map.containsKey(key)){
                    map.put(key,map.get(key) + 1);
                } else {
                    map.put(key,1.0);
                }
            }
        }

        // Sort map in decreasing order
        return map.entrySet()
                .stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));
    }

    private static Map<String,Double> getAllPredicateProbabilities(Map<String,Double> matches, Map<String,Double> nonMatches, double similarityRequests){
        Map<String,Double> probability = new HashMap<>();

        for(Map.Entry<String,Double> match : matches.entrySet()){
            if(nonMatches.containsKey(match.getKey())){
                probability.put(match.getKey(),((match.getValue()+nonMatches.get(match.getKey()))/similarityRequests));
            } else {
                probability.put(match.getKey(),match.getValue()/similarityRequests);
            }
        }

        return probability;
    }


    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void writeSupportAndConfidenceAsJson(String apiName, String dbName, Map<String,Pair<Double,Double>> supportConfidenceMap){
        JSONArray array = new JSONArray();
        for(Map.Entry<String,Pair<Double,Double>> entry : supportConfidenceMap.entrySet()){
            JSONObject tmp = new JSONObject();
            tmp.put("path",entry.getKey());
            tmp.put("support",entry.getValue().getKey());
            tmp.put("confidence",entry.getValue().getValue());

            array.put(tmp);
        }

        JSONObject json = new JSONObject();
        json.put("metrics",array);

        String path = ConfigurationLoader.getOutputPath()+dbName+"/support_confidence/";
        File storeFile = new File(path);
        storeFile.mkdirs();

        // Write matches
        storeFile = new File(path + apiName + ".json");
        storeFile.delete();

        try{
            storeFile.createNewFile();
            BufferedWriter bw = new BufferedWriter(new FileWriter(storeFile));
            bw.write(json.toString());
            bw.flush();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void createSupportConfidenceEntry(String name){
        File file = new File(ConfigurationLoader.getSupportAndConfidencePath());
        BufferedReader br;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            JSONObject obj = new JSONObject(IOUtils.toString(br));

            JSONArray array = obj.getJSONArray("apis");
            JSONObject entry = new JSONObject();
            entry.put("label",name);
            entry.put("min_support","0.0");
            entry.put("min_confidence","0.0");
            array.put(entry);

            obj = new JSONObject();
            obj.put("apis",array);

            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(obj.toString());
            writer.flush();
            writer.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
