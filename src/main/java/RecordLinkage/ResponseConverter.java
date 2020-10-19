package RecordLinkage;

import Utils.Loader.ConfigurationLoader;
import WebApi.HttpResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wnameless.json.flattener.JsonFlattener;
import org.json.XML;

import java.io.IOException;
import java.util.HashMap;

class ResponseConverter {

    // This method is used to convert an HTTP response in form of a json tree into a flatted json (dictionary/map)
    // The path to the value is afterwards a predicate string and the value itself is an object (spoken in RDF notation)
    static HashMap<String,Object> convertResponse(HttpResponse response){
        HashMap<String,Object> result = new HashMap<>();
        String jsonString;

        // Check the response type (XML, JSON and other)
        // All formats will be transformed into an flattened JSON version
        if(response.getApplicationType().contains("xml")){
            jsonString = XML.toJSONObject(response.getContent()).toString();
            jsonString = JsonFlattener.flatten(jsonString);
        } else if(response.getApplicationType().contains("json")){
           jsonString = JsonFlattener.flatten(response.getContent());
        } else {
            if(ConfigurationLoader.getLogLevel() >= 1) {
                System.out.println("[ResponseConverter.convertResponse]: Unknown result format. " +
                        "Behave like it would be a JSON format.");
            }

            // Using an external library to flatten JSON (link: https://github.com/wnameless/json-flattener)
            jsonString = JsonFlattener.flatten(response.getContent());
        }

        try {
            //noinspection unchecked
            result = new ObjectMapper().readValue(jsonString, HashMap.class);
        } catch (IOException e) {
            if(ConfigurationLoader.getLogLevel() >= 2){
                System.out.println("[ResponseConverter.convertResponse]: " + e.getMessage());
            }

            return result;
        }

        return result;
    }
}
