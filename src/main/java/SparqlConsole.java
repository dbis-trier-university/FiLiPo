import QueryManagement.QueryProcessor;
import Utils.Loader.DatabaseLoader;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetFormatter;

import java.util.List;
import java.util.Objects;
import java.util.Scanner;

public class SparqlConsole {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.print("Database: ");
        String dbName = sc.nextLine();

        boolean runnig = true;
        while (runnig){
            System.out.print("Query: ");
            String queryStr = sc.nextLine();

            if(queryStr.equals("exit")){
                runnig = false;
                break;
            }

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
                    try{
                        System.out.print(solution.get(var).toString() + " ");
                    } catch (NullPointerException e){
                        System.out.print("none ");
                    }
                }
                System.out.println();
            }

            long timeElapsed = endTime - startTime;
            System.out.println("Time: " + timeElapsed/1000);
        }

        System.out.println("Done..");
    }
}
