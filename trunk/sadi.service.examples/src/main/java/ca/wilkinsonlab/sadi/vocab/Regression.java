package ca.wilkinsonlab.sadi.vocab;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Linear regression vocabulary.
 * Modeled on the vocabulary classes included with the ARQ distribution.
 * 
 * @author Luke McCarthy
 */
public class Regression
{
private static Model m_model = ModelFactory.createDefaultModel();
	
	/**
	 * The namespace of the vocabulary as a string.
	 */
	public static final String NS = "http://sadiframework.org/examples/regression.owl#";
	
	/** 
	 * Returns the namespace of the vocabulary as a string.
	 * @return the namespace of the vocabulary as a string.
	 */
    public static String getURI() { return NS; }
    
	public static final Resource X = m_model.createResource( NS + "X" );
	public static final Resource Y = m_model.createResource( NS + "Y" );
	public static final Resource PairedValue = m_model.createResource( NS + "PairedValue" );
	public static final Resource LinearRegressionModel = m_model.createResource( NS + "LinearRegressionModel" );
	
	public static final Property slope = m_model.createProperty( NS, "slope" );
	public static final Property intercept = m_model.createProperty( NS, "intercept" );
	public static final Property hasRegressionModel = m_model.createProperty( NS, "hasRegressionModel" );
	public static final Property yForLargestX = m_model.createProperty( NS, "yForLargestX" );
}
