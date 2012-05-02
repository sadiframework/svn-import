package org.sadiframework.vocab;

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
     * An RDF type for SADI services.
     */
    public static final Resource Service = m_model.createResource( NS + "Service" );
    
    /**
     * A property that connects a service with the restrictions it attaches to its input.
     */
    public static final Property decoratesWith = m_model.createProperty( NS + "decoratesWith" );
    
    /**
     * A property that indicates when a service was registered.
     */
    public static final Property registration = m_model.createProperty( NS + "registration" );
    
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
	public static final Property hasStackTrace = m_model.createProperty( NS + "hasStackTrace" );
    
	/**
	 * A property that indicates the status of a service (e.g. ok, dead) 
	 */
	public static final Property serviceStatus = m_model.createProperty( NS + "serviceStatus" );
	
	/**
	 * Status value that indicates a service is functioning correctly.  
	 */
	public static final Resource ok = m_model.createResource( NS + "ok" );

	/**
	 * Status value that indicates a service is responding more slowly than expected.
	 */
	public static final Resource slow = m_model.createResource( NS + "slow" );

	/**
	 * Status value that indicates a service is generating incorrect results for its test cases.
	 */
	public static final Resource incorrect = m_model.createResource( NS + "incorrect" );

	/**
	 * Status value that indicates a service is not responding.
	 */
	public static final Resource dead = m_model.createResource( NS + "dead" );
	
	public static final Property error = m_model.createProperty( NS + "error" );
	
    public static final String ASYNC_HEADER = "sadi-please-wait";
}
