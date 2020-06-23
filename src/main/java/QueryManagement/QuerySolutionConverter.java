package QueryManagement;

import javafx.util.Pair;
import org.apache.jena.query.QuerySolution;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class QuerySolutionConverter {

    public static List<Pair<String,String>> convertToPair(List<QuerySolution> solutionList){
        List<Pair<String,String>> newSolution = new LinkedList<>();

        List<String> vars = getCollumns(solutionList);
        for(QuerySolution solution : solutionList){
            newSolution.add(new Pair<String,String>(solution.get(vars.get(0)).toString(), solution.get(vars.get(1)).toString()));
        }

        return newSolution;
    }

    public static List<String> convertToList(List<QuerySolution> solutionList, String cName){
        List<String> newSolution = new LinkedList<>();

        for(QuerySolution solution : solutionList){
            newSolution.add(solution.get(cName).toString());
        }

        return newSolution;
    }

    private static List<String> getCollumns(List<QuerySolution> solutionList){
        Iterator<String> it = solutionList.get(0).varNames();
        List<String> vars = new LinkedList<>();
        while (it.hasNext()) vars.add(it.next());

        return vars;
    }

}
