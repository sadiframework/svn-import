package ca.wilkinsonlab.sadi.vocab;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Fake patient data vocabulary.
 * Modeled on the vocabulary classes included with the ARQ distribution.
 * 
 * @author Luke McCarthy
 */
public class Patients
{
private static Model m_model = ModelFactory.createDefaultModel();
	
	/**
	 * The namespace of the vocabulary as a string.
	 */
public static final String NS = "http://sadiframework.org/ontologies/patients.owl#";
	
	/** 
	 * Returns the namespace of the vocabulary as a string.
	 * @return the namespace of the vocabulary as a string.
	 */
    public static String getURI() { return NS; }
    

	public static final Resource Patient = m_model.createResource( NS + "Patient" );
	public static final Resource MeasurementEvent = m_model.createResource( NS + "MeasurementEvent" );
	public static final Resource Measurement = m_model.createResource( NS + "Measurement" );
	public static final Resource Offset = m_model.createResource( NS + "Offset" );
	
	public static final Property creatinineLevels = m_model.createProperty( NS, "creatinineLevels" );
	public static final Property bunLevels = m_model.createProperty( NS, "BUNLevels" );
}
