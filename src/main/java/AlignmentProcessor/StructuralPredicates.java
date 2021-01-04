package AlignmentProcessor;

import java.util.HashSet;
import java.util.Set;

class StructuralPredicates{
    private static Set<String> structuralPredicates = new HashSet<>();
    private static final String RDFS = "http://www.w3.org/2000/01/rdf-schema#";
    private static final String RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";

    private static void load(){
        structuralPredicates.add(RDF+"type");
        structuralPredicates.add(RDFS+"class");
        structuralPredicates.add(RDFS+"subClassOf");
        structuralPredicates.add(RDFS+"subPropertyOf");
        structuralPredicates.add(RDFS+"domain");
        structuralPredicates.add(RDFS+"range");
    }

    static boolean contains(String predicate){
        load();
        return structuralPredicates.contains(predicate.toLowerCase());
    }
}
