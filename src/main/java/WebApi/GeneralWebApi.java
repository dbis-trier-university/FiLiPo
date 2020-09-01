package WebApi;

import Utils.Loader.ConfigurationLoader;
import Utils.Loader.DatabaseLoader;
import javafx.util.Pair;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class GeneralWebApi {

    private JSONObject apiConfig;

    public GeneralWebApi(String key){
        apiConfig = DatabaseLoader.getApiConfiguration(key);
    }

    private int getNumberOfReqParams(){
        int counter = 0;

        List<Pair<String,String>> list = this.getParameters();
        for(Pair p : list){
            if(p.getValue().toString().equalsIgnoreCase("required")) counter++;
        }

        return counter;
    }

    public String buildCallUrl(boolean optionals, String ... params) throws ArgumentException, UnsupportedEncodingException {
        String url = this.getUrl();
        Object[] paramArray = this.getParameters().toArray();

        if((optionals && paramArray.length != params.length) || (!optionals && this.getNumberOfReqParams() != params.length)){
            throw new ArgumentException("Too many or to less arguments in buildCallUrl");
        } else {
            for (int i = 0; i < paramArray.length; i++) {
                Pair<String,String> p = (Pair<String,String>) paramArray[i];

                if(p.getValue().equalsIgnoreCase("required") || optionals){
                    if(url.charAt(url.indexOf("{"+p.getKey()+"}")-1) != '/'){
                        url = url.replace("{"+p.getKey()+"}", URLEncoder.encode(params[i], StandardCharsets.UTF_8.toString()));
                    } else {
                        try{
                            url = url.replace("{"+p.getKey()+"}",params[i]);
                        } catch (Exception e){
                            System.out.println("ERROR: " + params[i]);
                        }
                    }
                } else {
                    url = url.replace("{"+p.getKey()+"}","");

                    if(url.charAt( url.indexOf(p.getKey()+"=")-1 ) == '&'){
                        url = url.replace("&" + p.getKey()+"=","");
                    } else {
                        url = url.replace(p.getKey()+"=","");
                    }

                }
            }

            return url;
        }
    }

    public String getUrl(){
        String url = this.getPlainUrl();
        if(url.contains("{format}")) url = url.replace("{format}", this.getFormat());
        try{
            String secret = ConfigurationLoader.getSecret(this.apiConfig.getString("label"));
            url = url.replace("{key}", Objects.requireNonNull(secret));
        } catch (NullPointerException ignored){}

        return url;
    }

    private String getPlainUrl(){
        return this.apiConfig.getString("url");
    }

    private String getFormat(){
        return this.apiConfig.getString("format");
    }

    private List<Pair<String,String>> getParameters(){
        List<Pair<String,String>> list = new LinkedList<>();
        JSONArray array = this.apiConfig.getJSONArray("parameters");

        for (int i = 0; i < array.length(); i++) {
            Pair pair = new Pair<>(array.getJSONObject(i).getString("name"),
                    array.getJSONObject(i).getString("status"));
            list.add(pair);
        }

        return list;
    }

    public HttpResponse doApiCall(String url){
        return HttpHandler.sendGetRequest(url, ConfigurationLoader.getLogLevel());
    }
}
