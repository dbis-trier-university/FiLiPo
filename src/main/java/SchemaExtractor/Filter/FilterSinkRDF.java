package SchemaExtractor.Filter;

import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFWrapper;

class FilterSinkRDF extends StreamRDFWrapper {
    final Node[] properties;

    FilterSinkRDF(StreamRDF dest, Property...properties) {
        super(dest);
        this.properties = new Node[properties.length];
        for ( int i = 0 ; i < properties.length ; i++ )
            this.properties[i] = properties[i].asNode();
    }
}
