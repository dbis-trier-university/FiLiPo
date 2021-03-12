package Utils.Loader;

import AlignmentProcessor.Metrics.SupportConfidenceProcessor;
import Utils.ReaderWriter.DiskReader;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class DatabaseLoader {


    /*******************************************************************************************************************
     *  Database Configuration
     ******************************************************************************************************************/
    public static String getDatabaseUrl(String name){
        JSONObject object = new JSONObject(DiskReader.readFile(ConfigurationLoader.getDatabasePath()));
        JSONArray array = object.getJSONArray("endpoints");

        for (int i = 0; i < array.length(); i++) {
            if(array.getJSONObject(i).getString("label").equalsIgnoreCase(name)){
                return array.getJSONObject(i).getString("url");
            }
        }

        return null;
    }

    public static String getDatabaseSource(String name){
        JSONObject object = new JSONObject(DiskReader.readFile(ConfigurationLoader.getDatabasePath()));
        JSONArray array = object.getJSONArray("endpoints");

        for (int i = 0; i < array.length(); i++) {
            if(array.getJSONObject(i).getString("label").equalsIgnoreCase(name)){
                return array.getJSONObject(i).getString("source");
            }
        }

        return null;
    }

    public static List<String> getDatabaseNames(){
        JSONObject object = new JSONObject(DiskReader.readFile(ConfigurationLoader.getDatabasePath()));
        JSONArray array = object.getJSONArray("endpoints");

        List<String> list = new LinkedList<>();
        for (int i = 0; i < array.length(); i++) {
            list.add(array.getJSONObject(i).getString("label"));
        }

        return list;
    }

    public static JSONArray getDatabases() {
        JSONObject object = new JSONObject(DiskReader.readFile(ConfigurationLoader.getDatabasePath()));
        return object.getJSONArray("endpoints");
    }

    /*******************************************************************************************************************
     *  Api Configuration
     ******************************************************************************************************************/
    public static JSONObject getApiConfiguration(String apiName){
        JSONArray apiArray = new JSONObject(DiskReader.readFile(ConfigurationLoader.getDatabasePath())).getJSONArray("apis");
        for (int i = 0; i < apiArray.length(); i++) {
            JSONObject tmp = apiArray.getJSONObject(i);
            if(tmp.getString("label").equalsIgnoreCase(apiName)){
                return tmp;
            }
        }

        return null;
    }

    public static String getSingleInputType(String apiName){
        JSONObject tmp = getApiConfiguration(apiName);
        JSONArray array = Objects.requireNonNull(tmp).getJSONArray("parameters");

        try{
            return array.getJSONObject(0).getString("type");
        } catch (Exception e){
            return null;
        }
    }

    public static List<String> getSingleInputTypeFilter(String apiName){
        JSONObject tmp = getApiConfiguration(apiName);
        JSONArray array = Objects.requireNonNull(tmp).getJSONArray("parameters");

        try{
            List<String> filterList = new LinkedList<>();

            JSONArray filters = array.getJSONObject(0).getJSONArray("filter");
            for (int i = 0; i < filters.length(); i++) {
                filterList.add(filters.getString(i));
            }

            return filterList;
        } catch (Exception e){
            return null;
        }
    }

    public static List<String[]> getSelection(String apiName){
        JSONObject apiConfig = getApiConfiguration(apiName);
        JSONArray array = apiConfig.getJSONArray("selection");

        List<String[]> list = new LinkedList<>();
        for (int i = 0; i < array.length(); i++) {
            String[] tmp = array.getJSONObject(i).getString("type").split(", ");
            list.add(tmp);
        }

        return list;
    }

    public static boolean existsSelection(String apiName){
       try{
           JSONObject apiConfig = getApiConfiguration(apiName);
           JSONArray array = apiConfig.getJSONArray("selection");

           return true;
       } catch (Exception e){
           return false;
       }
    }

    public static List<String> getApiList(){
        JSONArray apiArray = new JSONObject(DiskReader.readFile(ConfigurationLoader.getDatabasePath())).getJSONArray("apis");

        List<String> list = new LinkedList<>();
        for (int i = 0; i < apiArray.length(); i++) {
            list.add(apiArray.getJSONObject(i).getString("name"));
        }

        return list;
    }

    public static JSONArray getApis(){
        return new JSONObject(DiskReader.readFile(ConfigurationLoader.getDatabasePath())).getJSONArray("apis");
    }

    /*******************************************************************************************************************
     *  Support and Confidence Database
     ******************************************************************************************************************/
    private static JSONObject getSupportAndConfidenceConfiguration(String name){
        JSONObject object = new JSONObject(DiskReader.readFile(ConfigurationLoader.getSupportAndConfidencePath()));
        JSONArray array = object.getJSONArray("apis");

        for (int i = 0; i < array.length(); i++) {
            if(array.getJSONObject(i).getString("label").equalsIgnoreCase(name)){
                return array.getJSONObject(i);
            }
        }

        return null;
    }

    public static Double getMinSupport(String name){
        JSONObject obj = getSupportAndConfidenceConfiguration(name);

        double value;
        try{
            value = Double.parseDouble(Objects.requireNonNull(obj).getString("min_support"));
        } catch (NullPointerException e){
            SupportConfidenceProcessor.createSupportConfidenceEntry(name);
            value = 0.0;
        }

        return value;
    }

    public static Double getMinConfidence(String name){
        JSONObject obj = getSupportAndConfidenceConfiguration(name);

        double confidence;
        try{
            confidence = Double.parseDouble(Objects.requireNonNull(obj).getString("min_confidence"));
        } catch (NullPointerException e){
            SupportConfidenceProcessor.createSupportConfidenceEntry(name);
            confidence = 0.0;
        }

        return confidence;
    }


}
