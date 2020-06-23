package QueryManagement;

import Utils.Loader.ConfigurationLoader;
import Utils.Loader.DatabaseLoader;
import Utils.Loader.SchemaLoader;
import javafx.util.Pair;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetFormatter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

public class KnowledgeBaseManagement {
    private static final int DEPTH = ConfigurationLoader.getTraversalDepth();

    public static String getPredicateValue(String name, String p, int offset){
        String queryString = "select ?value where { ?s <" + p + "> ?value . } limit 1 offset " + offset;
        if(ConfigurationLoader.getLogLevel() > 1) System.out.println("[getPredicateValue]: " + queryString);

        QueryProcessor qp = new QueryProcessor(queryString, Objects.requireNonNull(DatabaseLoader.getDatabaseUrl(name)));
        String value;
        try{
            value = qp.query().next().get("value").toString();
        } catch (Exception e) {
            value = null;
        }
        qp.close();

        return value;
    }

    public static List<QuerySolution> getCompleteEntry(String databaseName, String p, int offset){
        String queryString = "select ?s where { ?s <" + p + "> ?value . } limit 1 offset " + offset;
        QueryProcessor qp = new QueryProcessor(queryString, Objects.requireNonNull(DatabaseLoader.getDatabaseUrl(databaseName)));
        String subject = qp.query().next().get("s").toString();
        qp.close();

        queryString = "select ?relation ?value where { <" + subject + "> ?relation ?value . }";
        qp = new QueryProcessor(queryString, Objects.requireNonNull(DatabaseLoader.getDatabaseUrl(databaseName)));
        List<QuerySolution> solutions = ResultSetFormatter.toList(qp.query());
        qp.close();

        return solutions;
    }

    public static Set<Pair<String,String>> getFullKnowledge(String dbName, String inputType, String subject, List<QuerySolution> localKnowledge){
        Set<Pair<String,String>> fullKnowledge = getKnowledge(dbName,localKnowledge,null);
        Set<String> inverseRelations = getInverseRelations(dbName, inputType);

        // Also travel the inverse direction to get all related information
        for(String relation : inverseRelations){
            String inverseQuery = "select ?relation ?value where { <" + subject + "> ^<" + relation + "> ?e . ?e ?relation ?value . }";
            QueryProcessor qp = new QueryProcessor(inverseQuery,Objects.requireNonNull(DatabaseLoader.getDatabaseUrl(dbName)));
            List<QuerySolution> solutions = ResultSetFormatter.toList(qp.query());
            qp.close();

            Set<Pair<String,String>> inverseFullKnowledge = new HashSet<>();
            for(Pair<String,String> pair : getKnowledge(dbName,solutions,relation)){
                Pair<String,String> newPair = new Pair<>("^" + relation + ", " + pair.getKey(), pair.getValue());
                inverseFullKnowledge.add(newPair);
            }
            fullKnowledge.addAll(inverseFullKnowledge);
        }

        return fullKnowledge;
    }

    public static Set<Pair<String,String>> getFullKnowledge(String dbName, String inputType, String inputPredicate, int offset, List<QuerySolution> localKnowledge){
        Set<Pair<String,String>> fullKnowledge = getKnowledge(dbName,localKnowledge,null);
        Set<String> inverseRelations = getInverseRelations(dbName, inputType);

        // Also travel the inverse direction to get all related information
        for(String relation : inverseRelations){
            String queryString = "select ?s where { ?s <" + inputPredicate + "> ?value . } limit 1 offset " + offset;
            QueryProcessor qp = new QueryProcessor(queryString, Objects.requireNonNull(DatabaseLoader.getDatabaseUrl(dbName)));
            String subject = qp.query().next().get("s").toString();
            qp.close();

            String inverseQuery = "select ?relation ?value where { <" + subject + "> ^<" + relation + "> ?e . ?e ?relation ?value . }";
            qp = new QueryProcessor(inverseQuery,Objects.requireNonNull(DatabaseLoader.getDatabaseUrl(dbName)));
            List<QuerySolution> solutions = ResultSetFormatter.toList(qp.query());
            qp.close();

            Set<Pair<String,String>> inverseFullKnowledge = new HashSet<>();
            for(Pair<String,String> pair : getKnowledge(dbName,solutions,relation)){
                Pair<String,String> newPair = new Pair<>("^" + relation + ", " + pair.getKey(), pair.getValue());
                inverseFullKnowledge.add(newPair);
            }
            fullKnowledge.addAll(inverseFullKnowledge);
        }

        return fullKnowledge;
    }

    private static Set<String> getInverseRelations(String dbName, String inputType){
        JSONArray schema = SchemaLoader.getStructure(dbName);

        Set<String> inverseRelations = new HashSet<>();
        for (int i = 0; i < schema.length(); i++) {
            JSONObject obj = schema.getJSONObject(i);

            try{
                String object  = obj.getString("object");

                if(object.equals(inputType)){
                    inverseRelations.add(obj.getString("predicate"));
                }
            } catch (JSONException ignored){}
        }

        return inverseRelations;
    }

    private static Set<Pair<String,String>> getKnowledge(String dbName, List<QuerySolution> localKnowledge, String predecessor){
        Set<Pair<String,String>> fullKnowledge = new HashSet<>();
        for(QuerySolution solution : localKnowledge){
            fullKnowledge.add(new Pair<>(solution.get("relation").toString(),solution.get("value").toString()));
        }

        for(QuerySolution dbEntry : localKnowledge){
            String dbRelation = dbEntry.get("relation").toString();
            String dbValue = dbEntry.get("value").toString();

            // If the value is an URI query the entity and get the facts of that entity (path traversal)
            // Dont go traverse backwards over the predecessor edge (prevent circles)
            if((predecessor == null && dbEntry.get("value").isResource())
                    || (predecessor != null && !predecessor.equals(dbEntry.get("relation").toString()) && dbEntry.get("value").isResource()))
            {
                List<Pair<String,String>> innerRecord = traverse(dbName,dbRelation,dbValue);
                fullKnowledge.addAll(innerRecord);
            }
        }

        return fullKnowledge;
    }

    private static List<Pair<String,String>> traverse(String dbName, String relation, String entityUrl){
        int currentDepth = relation.split(",").length;
        List<QuerySolution> innerRecord = traverseEntity(dbName,entityUrl); // Query properties and values
        List<Pair<String,String>> intermediateResult = new LinkedList<>();

        if(currentDepth < (DEPTH)){
            for (QuerySolution solution : innerRecord) {
                if (solution.get("value").isResource()) {
                    intermediateResult.add(new Pair<>(relation + ", " + solution.get("relation").toString(), solution.get("value").toString()));
                    intermediateResult.addAll(traverse(dbName,relation + ", " + solution.get("relation").toString(), solution.get("value").toString()));
                } else {
                    intermediateResult.add(new Pair<>(relation + ", " + solution.get("relation").toString(), solution.get("value").toString()));
                }
            }
        }

        return intermediateResult;
    }

    private static List<QuerySolution> traverseEntity(String dbName, String entityUrl){
        String traversalQuery = "select ?relation ?value where { <"+entityUrl+"> ?relation ?value .}";
        QueryProcessor qp = new QueryProcessor(traversalQuery, Objects.requireNonNull(DatabaseLoader.getDatabaseUrl(dbName)));
        List<QuerySolution> innerRecord = ResultSetFormatter.toList(qp.query());
        qp.close();

        return innerRecord;
    }

    public static int getNumberOfEntities(String dbName, String property){
        String queryNumberString = "select (count(?value) as ?no) where { ?s <" + property + "> ?value . }";
        QueryProcessor qp = new QueryProcessor(queryNumberString, Objects.requireNonNull(DatabaseLoader.getDatabaseUrl(dbName)));
        String tmp = qp.query().next().get("no").toString();
        qp.close();

        return Integer.parseInt(tmp.substring(0,tmp.indexOf("^^")));
    }
}
