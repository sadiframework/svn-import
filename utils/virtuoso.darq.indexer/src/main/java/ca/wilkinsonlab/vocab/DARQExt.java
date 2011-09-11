package ca.wilkinsonlab.vocab;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Extensions to the original DARQ service description ontology. 
 */
public class DARQExt 
{
	private static final Model model = ModelFactory.createDefaultModel();
	
	public static final String URI_PREFIX = "http://sadiframework.org/ontologies/DARQ/darq-extensions.owl#";
	
	public static final Property graph = model.createProperty(String.format("%sgraph", URI_PREFIX));
	public static final Property URI = model.createProperty(String.format("%sURI", URI_PREFIX));
	public static final Property resultsLimit = model.createProperty(String.format("%sresultsLimit", URI_PREFIX));
	public static final Property subjectRegex = model.createProperty(String.format("%ssubjectRegex", URI_PREFIX));
	public static final Property objectRegex = model.createProperty(String.format("%sobjectRegex", URI_PREFIX));
	public static final Property subjectType = model.createProperty(String.format("%ssubjectType", URI_PREFIX));
	public static final Property objectType = model.createProperty(String.format("%sobjectType", URI_PREFIX));
	
	public static final Resource defaultGraph = model.createResource(String.format("%sdefaultGraph", URI_PREFIX));
}
