import QueryManagement.QueryProcessor;
import Utils.Loader.DatabaseLoader;
import javafx.scene.chart.XYChart;
import org.apache.jena.graph.BlankNodeId;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.RDFNode;

import java.util.List;
import java.util.Objects;
import java.util.Scanner;

public class SparqlConsole {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.print("Database: ");
        String dbName = sc.nextLine();

        System.out.print("Query: ");
        String queryStr = sc.nextLine();

        long startTime = System.currentTimeMillis();

        QueryProcessor qp = new QueryProcessor(queryStr, Objects.requireNonNull(DatabaseLoader.getDatabaseUrl(dbName)));
        List<QuerySolution> result = ResultSetFormatter.toList(qp.query());
        qp.close();

        long endTime = System.currentTimeMillis();

        List<String> resultVars = qp.getResultVars();
        for (String var : resultVars)
            System.out.print(var + " ");
        System.out.println();

        for(QuerySolution solution : result){
            for(String var : resultVars){
                System.out.print(solution.get(var).toString() + " ");
            }
            System.out.println();
        }

        long timeElapsed = endTime - startTime;
        System.out.println("Time: " + timeElapsed/1000);

        System.out.println("Done..");
    }
}
