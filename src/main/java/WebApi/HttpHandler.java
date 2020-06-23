package WebApi;

import Utils.Loader.ConfigurationLoader;
import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

class HttpHandler {
    private final static String USER_AGENT = "Mozilla/5.0";

    private static HttpResponse sendGetRequest(String urlString){
        return sendGetRequest(urlString,0);
    }

    static HttpResponse sendGetRequest(String urlString, int debug){
        try {
            if(debug > 1) System.out.println("[sendGetRequest]: Sending 'GET' request to URL : " + urlString);

            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", USER_AGENT);

            BufferedReader response = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String content = IOUtils.toString(response);
            int responseCode = connection.getResponseCode();

            if(debug > 1) System.out.println("[sendGetRequest]: Response Code : " + responseCode);
            if(debug > 2){
                System.out.println("[sendGetRequest]: Response Message: \n" + content);
            }

            if(responseCode == HttpURLConnection.HTTP_OK) {
                return new HttpResponse(connection.getHeaderField("Content-Type"),content);
            } else if (responseCode == HttpURLConnection.HTTP_MOVED_PERM
                    || responseCode == HttpURLConnection.HTTP_MOVED_TEMP)
            {
                return handleRedirect(connection,debug);
            } else {
                return null;
            }
        } catch (IOException e) {
            if(ConfigurationLoader.getLogLevel() > 1) {
                System.out.println("[HttpHandler.sendGetRequest]: " + e + " ->");
            }
            if(ConfigurationLoader.getLogLevel() > 3){
                for(StackTraceElement elem : e.getStackTrace()){
                    System.out.println("\t" + elem);
                }
            }
        }

        return null;
    }

    private static HttpResponse handleRedirect(HttpURLConnection connection, int debug) {
        if(debug >= 1) {
            System.out.println("[handleRedirect]: Performing redirect: " + connection.getHeaderField("Location"));
        }

        String url = connection.getHeaderField("Location");
        try {
            URL obj = new URL(url);
            HttpURLConnection newCon = (HttpURLConnection) obj.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return sendGetRequest(url);
    }
}
