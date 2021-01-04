package Utils.Loader;

import Utils.ReaderWriter.DiskReader;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class SchemaLoader {

    public static JSONObject getSchema(String dbName){
        String path = ConfigurationLoader.getOutputPath() + "data/" + dbName + "/schema/index.json";
        return new JSONObject(DiskReader.readFile(path));
    }

    public static JSONArray getStructure(String dbName){
        JSONObject schema = getSchema(dbName);

        return schema.getJSONArray("structure");
    }

    public static JSONArray getIdentifierPredicates(String dbName){
        JSONObject json = getSchema(dbName);

        return json.getJSONArray("identifier");
    }

    public static String getIdentifierType(String relation, String dbName){
        JSONArray identifier = getIdentifierPredicates(dbName);

        for (int i = 0; i < identifier.length(); i++) {
            JSONObject obj = identifier.getJSONObject(i);
            if(obj.getString("predicate").equals(relation)) return obj.getString("type");
        }

        return null;
    }

    public static List<String> getIdentifierTypes(){
        String configString = DiskReader.readFile("config.json");
        JSONObject json = new JSONObject(configString);

        JSONArray identifier = json.getJSONArray("ruleset");

        List<String> types = new LinkedList<>();
        for (int i = 0; i < identifier.length(); i++) {
            types.add(identifier.getJSONObject(i).getString("name"));
        }

        return types;
    }

    // Method to load the predicates that are used as identifier in the local knowledge base. Clearly identifier should
    // be compared by using equals() instead of some similarity methods like Jaro-Winkler
    public static Set<String> loadIdentifierPredicates(String dbName) {
        Set<String> identifierPredicates = new HashSet<>();

        JSONArray array = SchemaLoader.getIdentifierPredicates(dbName);
        for (int i = 0; i < array.length(); i++) {
            identifierPredicates.add(array.getJSONObject(i).getString("predicate"));
        }

        return identifierPredicates;
    }

}
