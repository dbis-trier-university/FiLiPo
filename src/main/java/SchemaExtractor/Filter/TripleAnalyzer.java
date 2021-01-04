package SchemaExtractor.Filter;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.system.StreamRDF;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class TripleAnalyzer extends FilterSinkRDF {
    public Map<Triple,Integer> replacement;                        //Map<Triple,Counter> Triple only contain types
    public Map<String, Set<String>> types;                         //Map<Entity,Type>
    private Map<String, Map<String,Integer>> functionalityHelper;   //Map<Predicate, Map<Object,Counter>>
    public Map<String, Double> functionality;
    private String name;
    private long time;
    private Logger logger;

    public TripleAnalyzer(StreamRDF dest, String name, Map<String, Set<String>> types, Logger logger) {
        super(dest);
        this.replacement = new HashMap<>();
        this.functionalityHelper = new HashMap<>();

        this.name = name;
        this.types = types;
        this.logger = logger;
    }

    private void calcFunctionality() {
        this.functionality = new HashMap<>();

        // for every predicate
        for(Map.Entry<String,Map<String,Integer>> predicateEntry : functionalityHelper.entrySet()){
            double occurences = 0;
            for(Map.Entry<String,Integer> valueEntry : predicateEntry.getValue().entrySet()){
                occurences += valueEntry.getValue();
            }
            this.functionality.put(predicateEntry.getKey(),predicateEntry.getValue().size()/occurences);
        }

        System.out.println("Test");
    }

    @Override
    public void start() {
        super.start();
        this.time = System.currentTimeMillis();
    }

    @Override
    public void finish() {
        super.finish();

        long timeEnd = System.currentTimeMillis();
        this.time = (timeEnd - this.time) / 1000;

        if(this.time > 60) logger.info("[ReplaceFilter] Finished replacing of " + this.name + " in " + (this.time / 60) + " Minutes.");
        else logger.info("[ReplaceFilter] Finished replacing of " + this.name + " in " + this.time + " Seconds.");

        calcFunctionality();
    }

    @Override
    public void triple(Triple triple) {
        countObjectValues(triple);
        replaceEntitiesWithTypes(triple);
    }

    private void countObjectValues(Triple triple){
        if(functionalityHelper.containsKey(triple.getPredicate().toString())){
            Map<String,Integer> values = functionalityHelper.get(triple.getPredicate().toString());

            if(values.containsKey(triple.getObject().toString())){
                int tmp = values.get(triple.getObject().toString()) + 1;
                values.put(triple.getObject().toString(),tmp);
                functionalityHelper.put(triple.getPredicate().toString(),values);
            } else {
                values.put(triple.getObject().toString(),1);
                functionalityHelper.put(triple.getPredicate().toString(),values);
            }
        } else {
            Map<String,Integer> values = new HashMap<>();
            values.put(triple.getObject().toString(),1);
            functionalityHelper.put(triple.getPredicate().toString(),values);
        }
    }

    private void replaceEntitiesWithTypes(Triple triple){
        // If subject is a type
        if(this.types.containsKey(triple.getSubject().toString())){
            for(String type : this.types.get(triple.getSubject().toString())){
                Node s = NodeFactory.createURI(type);

                // Replacing entities with its type or with generic type Thing
                // In case that object is a literal we just ignore the object
                replaceTriple(s,triple);
            }

        // Subject type is unknown
        } else {
            if(triple.getSubject().isURI()) {
                Node s = NodeFactory.createURI("http://www.w3.org/2002/07/owl#Thing");
                replaceTriple(s, triple);

            } else if(triple.getSubject().isBlank()){
                Node s = NodeFactory.createURI("http://www.w3.org/2002/07/owl#Blank");
                replaceTriple(s, triple);
            } else {
                Node s = NodeFactory.createURI("http://www.w3.org/2000/01/rdf-schema#Literal");
                replaceTriple(s,triple);
            }
        }
    }

    // This method is used to replace entities with its type or with generic type Thing
    private void replaceTriple(Node s, Triple triple){
        Node o;

        if(types.containsKey(triple.getObject().toString())){
            for(String type : this.types.get(triple.getObject().toString())){
                o = NodeFactory.createURI(type);
                Triple t = new Triple(s,triple.getPredicate(),o);
                update(t);
            }
        } else if (!types.containsKey(triple.getObject().toString()) && triple.getObject().isURI()) {
            o = NodeFactory.createURI("http://www.w3.org/2002/07/owl#Thing");
            Triple t = new Triple(s,triple.getPredicate(),o);
            update(t);
        } else {
            o = NodeFactory.createBlankNode("");
            Triple t = new Triple(s,triple.getPredicate(),o);
            update(t);
        }
    }

    private void update(Triple t){
        if(this.replacement.containsKey(t)){
            int tmp = this.replacement.get(t);
            this.replacement.put(t,tmp+1);
        } else {
            this.replacement.put(t,1);
        }
    }
}
