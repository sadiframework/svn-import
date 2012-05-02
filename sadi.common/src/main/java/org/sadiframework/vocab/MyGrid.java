package org.sadiframework.vocab;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * MyGrid service ontology vocabulary.
 * Modeled on the vocabulary classes included with the ARQ distribution.
 * 
 * @author Luke McCarthy
 */
public class MyGrid
{
	private static Model m_model = ModelFactory.createDefaultModel();
	
	/**
	 * The namespace of the vocabulary as a string.
	 */
	public static final String NS = "http://www.mygrid.org.uk/mygrid-moby-service#";
	
	/** 
	 * Returns the namespace of the vocabulary as a string.
	 * @return the namespace of the vocabulary as a string.
	 */
    public static String getURI() { return NS; }
    
    /**
     * The namespace of the vocabulary as a resource.
     */
    public static final Resource NAMESPACE = m_model.createResource( NS );
    
    public static final Resource serviceDescription = m_model.createResource(NS + "serviceDescription");
    public static final Resource organisation = m_model.createResource(NS + "organisation");
    public static final Resource operation = m_model.createResource(NS + "operation");
    public static final Resource parameter = m_model.createResource(NS + "parameter");
    public static final Property providedBy = m_model.createProperty(NS + "providedBy");
    public static final Property authoritative = m_model.createProperty(NS + "authoritative");
    public static final Property hasOperation = m_model.createProperty(NS + "hasOperation");
    public static final Property inputParameter = m_model.createProperty(NS + "inputParameter");
    public static final Property outputParamater = m_model.createProperty(NS + "outputParameter");
    public static final Property hasServiceNameText = m_model.createProperty(NS + "hasServiceNameText");
    public static final Property hasServiceDescriptionText = m_model.createProperty(NS + "hasServiceDescriptionText");
    public static final Property objectType = m_model.createProperty(NS + "objectType");
}
