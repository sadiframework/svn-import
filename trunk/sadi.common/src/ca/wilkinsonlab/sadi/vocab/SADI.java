package ca.wilkinsonlab.sadi.vocab;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * SADI vocabulary definitions.
 * Modeled on the vocabulary classes included with the ARQ distribution.
 * 
 * @author Luke McCarthy
 */
public class SADI
{
	private static Model m_model = ModelFactory.createDefaultModel();
	
	/**
	 * The namespace of the vocabulary as a string.
	 */
	public static final String NS = "http://sadiframework.org/ontologies/sadi.owl#";
	
	/** 
	 * Returns the namespace of the vocabulary as a string.
	 * @return the namespace of the vocabulary as a string.
	 */
    public static String getURI() { return NS; }
    
    /**
     * The namespace of the vocabulary as a resource.
     */
    public static final Resource NAMESPACE = m_model.createResource( NS );
    

    /**
     * An RDF type for SADI service exceptions.
     */
	public static final Resource Exception = m_model.createResource( NS + "Exception" );

	/**
	 * An RDF type for SADI exception stack trace elements.
	 * The stack trace itself is just an RDF list.
	 */
	public static final Resource StackTraceElement = m_model.createResource( NS + "StackTraceElement" );

	/**
	 * A property that connects exceptions and stack traces.
	 */
	public static final Property hasStackTrace = m_model.createProperty( NS + "hasStackTrace ");
    
    public static final String ASYNC_HEADER = "sadi-please-wait";

}
