package Utils.Loader;

import Utils.ReaderWriter.DiskReader;
import javafx.util.Pair;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ConfigurationLoader {
    private static final String configPath = "config.json";

    /*******************************************************************************************************************
     *  Global Configuration
     ******************************************************************************************************************/
    private static JSONObject loadGlobals(){
        String configString = DiskReader.readFile(configPath);
        JSONObject json = new JSONObject(configString);

        return json.getJSONObject("globals");
    }

    public static String getLogPath() {
        return loadGlobals().getString("logpath");
    }

    public static String getIpcUrl() {
        return loadGlobals().getString("ipc");
    }

    public static String getDatabasePath() {
        return loadGlobals().getString("dbpath");
    }

    public static String getOutputPath() {
        return loadGlobals().getString("outpath");
    }

    public static int getMemory() {
        return Integer.parseInt(loadGlobals().getString("memory"));
    }

    public static String getSecretPath(){
        return loadGlobals().getString("secretpath");
    }

    public static String getSupportAndConfidencePath(){
        return loadGlobals().getString("scpath");
    }

    public static String getSecret(String key){
        String path = getSecretPath();
        JSONObject json = new JSONObject(DiskReader.readFile(path));

        JSONArray array = json.getJSONArray("secrets");
        for (int i = 0; i < array.length(); i++) {
            if(array.getJSONObject(i).getString("name").equalsIgnoreCase(key)){
                return array.getJSONObject(i).getString("secret");
            }
        }

        return null;
    }

    public static int getTimeout(){
        JSONObject obj = loadGlobals();
        return Integer.parseInt(obj.getString("timeout"));
    }

    public static int getLogLevel(){
        JSONObject obj = loadGlobals();
        return Integer.parseInt(obj.getString("loglevel"));
    }

    public static int getMode(){
        JSONObject linkageConf = loadGlobals();
        return Integer.parseInt(linkageConf.getString("mode"));
    }

    /*******************************************************************************************************************
     *  Linkage Point Configuration
     ******************************************************************************************************************/
    private static JSONObject loadLinkageConfig(){
        String configString = DiskReader.readFile(configPath);
        JSONObject json = new JSONObject(configString);

        return json.getJSONObject("linkage_config");
    }

    public static boolean useRegex(){
        JSONObject linkageConf = loadLinkageConfig();

        return linkageConf.getString("classifier").equals("regex");
    }

    public static String[] getSimilarityMetrics(){
        JSONArray jsonArray = loadLinkageConfig().getJSONArray("similarity_metrics");

        String[] array = new String[jsonArray.length()];
        int i = 0;
        for(Object object : jsonArray){
            array[i++] = object.toString();
        }

        return array;
    }

    public static int getCandidateRequests(){
        JSONObject linkageConf = loadLinkageConfig();
        return Integer.parseInt(linkageConf.getString("candidate_requests"));
    }

    public static double getFunctionalityThreshold(){
        JSONObject linkageConf = loadLinkageConfig();
        return Double.parseDouble(linkageConf.getString("functionality_threshold"));
    }

    public static double getDistributionVariance(){
        JSONObject linkageConf = loadLinkageConfig();
        return Double.parseDouble(linkageConf.getString("distribution_variance"));
    }

    public static double getErrorThreshold(){
        JSONObject linkageConf = loadLinkageConfig();
        return Double.parseDouble(linkageConf.getString("error_threshold"));
    }

    public static int getSimilarityRequests(){
        JSONObject linkageConf = loadLinkageConfig();
        return Integer.parseInt(linkageConf.getString("similarity_requests"));
    }

    public static double getCandidateResponses(){
        JSONObject linkageConf = loadLinkageConfig();
        return Double.parseDouble(linkageConf.getString("candidate_responses"));
    }


    public static double getStringSimilarity(){
        JSONObject linkageConf = loadLinkageConfig();
        return Double.parseDouble(linkageConf.getString("string_similarity"));
    }

    public static double getRecordSimilarity(){
        JSONObject linkageConf = loadLinkageConfig();
        return Double.parseDouble(linkageConf.getString("record_similarity"));
    }

    public static int getTraversalDepth(){
        JSONObject linkageConf = loadLinkageConfig();
        return Integer.parseInt(linkageConf.getString("traversal_depth"));
    }

    public static boolean isInSupportMode(){
        JSONObject linkageConf = loadLinkageConfig();
        return Integer.parseInt(linkageConf.getString("support_mode")) != 0;
    }

    public static double getMinSupport(){
        JSONObject linkageConf = loadLinkageConfig();
        return Double.parseDouble(linkageConf.getString("min_support"));
    }

    public static double getMinConfidence(){
        JSONObject linkageConf = loadLinkageConfig();
        return Double.parseDouble(linkageConf.getString("min_confidence"));
    }

    // Rule Set
    public static String getRegex(String type){
        String configString = DiskReader.readFile(configPath);
        JSONObject json = new JSONObject(configString);

        JSONArray ruleSet = json.getJSONArray("ruleset");

        for (int i = 0; i < ruleSet.length(); i++) {
            JSONObject obj = ruleSet.getJSONObject(i);
            if(obj.getString("name").equals(type)) return obj.getString("filter");
        }

        return null;
    }

}
