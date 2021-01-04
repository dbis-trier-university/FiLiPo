package AlignmentProcessor;

import Utils.Loader.ConfigurationLoader;
import Utils.Loader.DatabaseLoader;
import javafx.util.Pair;

import java.util.*;

class JointFeatureProcessor {
    static Pair<Map<String,Double>,Map<String,Double>> determineJointFeatures(String apiName, List<Set<Pair<String,String>>> matchingRecords, List<Set<Pair<String,String>>> nonMatchingRecords){
        // Calculate support for matching records
        Map<String,Map<String,Integer>> implicitSupportMap = computeImplicitSupport(matchingRecords);
        Map<String,Integer> simpleSupportMap = computeSimpleSupport(matchingRecords);
        Map<String,Double> relativeSupportMap = computeRelativeSupport(implicitSupportMap,simpleSupportMap,matchingRecords.size(),ConfigurationLoader.getMinSupport());

        // Calculate support for non matching records (in order to filter predicate-object-pairs that are not selective)
        Map<String,Map<String,Integer>> implicitSupportMapNM =  computeImplicitSupport(nonMatchingRecords);
        Map<String,Integer> simpleSupportMapNM = computeSimpleSupport(nonMatchingRecords);
        Map<String,Double> relativeSupportMapNM = computeRelativeSupport(implicitSupportMapNM,simpleSupportMapNM,nonMatchingRecords.size(),ConfigurationLoader.getMinSupport());

        // Calculate the real support and confidence values
        filterSelectiveValues(apiName,relativeSupportMap,relativeSupportMapNM);
        Map<String,Double> confidenceMap = computeConfidence(relativeSupportMap,implicitSupportMap,simpleSupportMap);
        relativeSupportMap.entrySet().removeIf(entry -> !confidenceMap.containsKey(entry.getKey()));

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


    private static Map<String,Double> computeRelativeSupport(Map<String,Map<String,Integer>> implicit, Map<String,Integer> simple, int validMatches, double minSupport){
        Map<String,Double> relativeSupportMap = new HashMap<>();

        for(Map.Entry<String,Map<String,Integer>> entry : implicit.entrySet()){
            for(Map.Entry<String,Integer> innerEntry : entry.getValue().entrySet()){
                double supportValue = ((double) simple.get(entry.getKey()))/validMatches;

                if(supportValue >= minSupport){
                    String key = entry.getKey() + ", " + innerEntry.getKey();
                    relativeSupportMap.put(key,supportValue);
                }
            }
        }

        return relativeSupportMap;
    }

    private static void filterSelectiveValues(String apiName, Map<String,Double> matchingSupport, Map<String,Double> nonMatchingSupport){
            matchingSupport.entrySet().removeIf(
                    innerEntry -> nonMatchingSupport.containsKey(innerEntry.getKey())
            );
    }

    private static Map<String,Double> computeConfidence(Map<String,Double> relativeSupportMap, Map<String,Map<String,Integer>> implicitSupport, Map<String,Integer> simpleSupport){
        Map<String,Double> confidenceMap = new HashMap<>();

        for(Map.Entry<String,Double> relativeEntry : relativeSupportMap.entrySet()){
            String rel = relativeEntry.getKey().substring(0,relativeEntry.getKey().lastIndexOf(","));
            String last = relativeEntry.getKey().substring(relativeEntry.getKey().lastIndexOf(",")+2);

            double erg;
            try{
                 erg = ((double) implicitSupport.get(rel).get(last))/simpleSupport.get(rel);
            } catch (Exception e){
                erg = 0;
            }

            if(erg >= ConfigurationLoader.getMinConfidence())
                confidenceMap.put(relativeEntry.getKey(),erg);
        }

        return confidenceMap;
    }
}

