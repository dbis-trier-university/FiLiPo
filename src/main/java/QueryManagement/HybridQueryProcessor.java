package QueryManagement;

import org.apache.jena.query.ResultSet;

public class HybridQueryProcessor {
    private QueryProcessor qp;

    public HybridQueryProcessor(String query, String url){
        this.qp = new QueryProcessor(doQueryTranslation(query),url);
    }

    public ResultSet query(){
         return qp.query();
    }

    public void close(){
        this.qp.close();
    }

    // TODO rewrite the query
    public String doQueryTranslation(String query){
        return query;
    }
}
