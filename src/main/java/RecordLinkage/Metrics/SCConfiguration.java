package RecordLinkage.Metrics;

import javafx.util.Pair;

import java.util.List;
import java.util.Set;

public class SCConfiguration {
    public String apiName, dbName;
    public List<Set<Pair<String,String>>> matches, nonMatches;
    public double validResponses, similarityRequests;

    public SCConfiguration(String apiName, String dbName, List<Set<Pair<String,String>>> matches,
        List<Set<Pair<String,String>>> nonMatches, double validResponses, double similarityRequests)
    {
        this.apiName = apiName;
        this.dbName = dbName;
        this.matches = matches;
        this.nonMatches = nonMatches;
        this.validResponses = validResponses;
        this.similarityRequests = similarityRequests;
    }

}
