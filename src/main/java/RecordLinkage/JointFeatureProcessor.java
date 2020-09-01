package RecordLinkage;

import QueryManagement.KnowledgeBaseManagement;
import Utils.Loader.ConfigurationLoader;
import Utils.Loader.DatabaseLoader;
import javafx.util.Pair;

import java.util.*;

class JointFeatureProcessor {
    static Pair<Map<String,Double>,Map<String,Double>> determineJointFeatures(String apiName, List<Set<Pair<String,String>>> matchingRecords, List<Set<Pair<String,String>>> nonMatchingRecords){
        // Calculate support for matching records
        Map<String,Map<String,Integer>> implicitSupportMap = computeImplicitSupport(matchingRecords);
        Map<String,Integer> simpleSupportMap = computeSimpleSupport(matchingRecords);
        Map<String,Double> relativeSupportMap = computeRelativeSupport(implicitSupportMap,matchingRecords.size(),ConfigurationLoader.getMinSupportMatch());

        // Calculate support for non matching records (in order to filter predicate-object-pairs that are not selective)
        Map<String,Map<String,Integer>> implicitSupportMapNM =  computeImplicitSupport(nonMatchingRecords);
        Map<String,Double> relativeSupportMapNM = computeRelativeSupport(implicitSupportMapNM,nonMatchingRecords.size(),ConfigurationLoader.getMinSupportNonMatch());

        // Calculate the real support and confidence values
        filterSelectiveValues(apiName,relativeSupportMap,relativeSupportMapNM);
        Map<String,Double> confidenceMap = computeConfidence(relativeSupportMap, implicitSupportMap,simpleSupportMap);

        return new Pair<>(relativeSupportMap,confidenceMap);
    }

    private static Map<String,Map<String,Integer>> computeImplicitSupport(List<Set<Pair<String,String>>> list){
        Map<String,Map<String,Integer>> map = new HashMap<>();

        for(Set<Pair<String,String>> record : list){
            for(Pair<String,String> fact : record){
                String key = fact.getKey();

                if(map.containsKey(key)){
                    Map<String,Integer> innerMap = map.get(key);

                    if(innerMap.containsKey(fact.getValue())){
                        int counter = innerMap.get(fact.getValue());
                        innerMap.put(fact.getValue(),counter+1);
                    } else {
                        innerMap.put(fact.getValue(),1);
                    }

                    map.put(key,innerMap);
                } else {
                    Map<String,Integer> innerMap = new HashMap<>();
                    innerMap.put(fact.getValue(),1);

                    if(innerMap.size() > 0) map.put(key,innerMap);
                }
            }
        }

        return map;
    }

    private static Map<String,Integer> computeSimpleSupport(List<Set<Pair<String,String>>> matchingRecords){
        Map<String,Integer> simpleSupportMap = new HashMap<>();

        for(Set<Pair<String,String>> record : matchingRecords){
            Set<String> tmp = new HashSet<>();

            for(Pair<String,String> pair : record){
                tmp.add(pair.getKey());
            }

            for(String key : tmp){
                if(simpleSupportMap.containsKey(key)){
                    int counter = simpleSupportMap.get(key);
                    simpleSupportMap.put(key,counter+1);
                } else {
                    simpleSupportMap.put(key,1);
                }
            }

        }

        return simpleSupportMap;
    }

    private static Map<String,Double> computeRelativeSupport(Map<String,Map<String,Integer>> map, double dataSize, double minSupport){
        Map<String,Double> relativeSupportMap = new HashMap<>();

        for(Map.Entry<String,Map<String,Integer>> relation : map.entrySet()){

            for(Map.Entry<String,Integer> value : relation.getValue().entrySet()){
                double supportValue = value.getValue()/dataSize;

                if(supportValue >= ConfigurationLoader.getCandidateResponses()){
                    String key = relation.getKey() + ", " + value.getKey();
                    relativeSupportMap.put(key,supportValue);
                }
            }
        }

        // Remove metrics and linkage points that have to less confidence
        relativeSupportMap.entrySet().removeIf(
                innerEntry -> innerEntry.getValue() < minSupport
        );

        return relativeSupportMap;
    }

    private static void filterSelectiveValues(String apiName, Map<String,Double> matchingSupport, Map<String,Double> nonMatchingSupport){
        if(DatabaseLoader.existsSelection(apiName)){
            matchingSupport.entrySet().removeIf(
                    innerEntry -> innerEntry.getValue() < 1.0
            );
        } else {
            matchingSupport.entrySet().removeIf(
                    innerEntry -> nonMatchingSupport.containsKey(innerEntry.getKey())
            );
        }
    }

    private static Map<String,Double> computeConfidence(Map<String,Double> relativeSupportMap, Map<String,Map<String,Integer>> implicitSupport, Map<String,Integer> simpleSupport){
        Map<String,Double> confidenceMap = new HashMap<>();

        for(Map.Entry<String,Map<String,Integer>> relation : implicitSupport.entrySet()){
            String simple = relation.getKey();

            for(Map.Entry<String,Integer> value : relation.getValue().entrySet()){
                double implicitSupportValue = value.getValue();
                double simpleSupportValue = simpleSupport.get(simple);

                double confidence = implicitSupportValue/simpleSupportValue;
                confidenceMap.put(simple + ", " + value.getKey(), confidence);
            }
        }

        // Only for entries with a support that is high enough are of interest
        confidenceMap.entrySet().removeIf(
                entry -> !relativeSupportMap.containsKey(entry.getKey())
        );

        return confidenceMap;
    }
}

