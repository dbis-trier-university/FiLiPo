package QueryManagement;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.tdb2.TDB2Factory;

import java.util.List;

public class QueryProcessor {
    private Query query;
    private QueryExecution qexec;
    private Dataset dataset;
    private Model model;

    public QueryProcessor(String queryStr, String url){
        boolean remoteEndpoint = url.startsWith("http");
        if(remoteEndpoint){
            this.query = QueryFactory.create(queryStr);
            this.qexec = QueryExecutionFactory.sparqlService(url, this.query);
        } else {
            boolean ttlFile = url.endsWith(".ttl") || url.endsWith(".nt") || url.endsWith(".n3");

            if(ttlFile){
                this.model = ModelFactory.createDefaultModel();
                this.model.read(url);
            } else {
                this.query = QueryFactory.create(queryStr);
                this.dataset = TDB2Factory.connectDataset(url);
                this.model = this.dataset.getDefaultModel();
            }

            this.query = QueryFactory.create(queryStr);
            this.qexec = QueryExecutionFactory.create(query,model);
        }
    }

    public void close(){
        if(this.qexec != null) this.qexec.close();
        if(this.dataset != null) {
            this.dataset.end();
            this.dataset.close();
        }
    }

    public ResultSet query() {
        if(dataset != null) dataset.begin(ReadWrite.READ);
        return this.qexec.execSelect();
    }

    public List<String> getResultVars(){
        return this.query.getResultVars();
    }
}
