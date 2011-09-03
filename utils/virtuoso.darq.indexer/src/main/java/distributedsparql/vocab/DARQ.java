package distributedsparql.vocab;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

public class DARQ 
{
	private static final Model model = ModelFactory.createDefaultModel();
	
	public static final String URI_PREFIX = "http://darq.sf.net/dose/0.1#";
	
	public static final Property capability = model.createProperty(String.format("%scapability", URI_PREFIX));
	public static final Property predicate = model.createProperty(String.format("%spredicate", URI_PREFIX));
	public static final Property triples = model.createProperty(String.format("%striples", URI_PREFIX));
	public static final Property totalTriples = model.createProperty(String.format("%stotalTriples", URI_PREFIX));

	public static final Resource service = model.createResource(String.format("%sService", URI_PREFIX));
	
	/** The named graph within the endpoint that contains the DARQ index. */
	public static final Resource serviceDescriptionGraph = service;
}
