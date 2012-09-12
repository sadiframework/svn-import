package org.sadiframework.service.nlp.vocab;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

public class NLP2RDF
{
	private static Model m_model = ModelFactory.createDefaultModel();
	
	/**
	 * The namespace of the vocabulary as a string.
	 */
	public static final String str = "http://nlp2rdf.lod2.eu/schema/string/";
	
	public static final String scms = "http://ns.aksw.org/scms/";
    
    /**
     * The namespace of the vocabulary as a resource.
     */
    public static final Resource STR_R = m_model.createResource(str );
    
	public static final Property sourceUrl = m_model.createProperty(str + "sourceUrl");
    
	public static final Property sourceString = m_model.createProperty(str + "sourceString");
	
	public static final Property subString = m_model.createProperty(str + "subString");

	public static final Property anchorOf = m_model.createProperty(str + "anchorOf");
	
	public static final Property means = m_model.createProperty(scms + "means");
}
