package org.sadiframework.vocab;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;

/**
 * New properties vocabulary.
 * Modeled on the vocabulary classes included with the ARQ distribution.
 * 
 * @author Luke McCarthy
 */
public class Properties
{
private static Model m_model = ModelFactory.createDefaultModel();
	
	/**
	 * The namespace of the vocabulary as a string.
	 */
	public static final String NS = "http://sadiframework.org/ontologies/properties.owl#";
	
	/** 
	 * Returns the namespace of the vocabulary as a string.
	 * @return the namespace of the vocabulary as a string.
	 */
    public static String getURI() { return NS; }
    
    public static Property hasName = m_model.createProperty(NS, "hasName");
    public static Property hasDescription = m_model.createProperty(NS, "hasDescription");
    public static Property hasSequence = m_model.createProperty(NS, "hasSequence");
    public static Property fromOrganism = m_model.createProperty(NS, "fromOrganism");
    public static Property has3DStructure = m_model.createProperty(NS, "has3DStructure");
    public static Property is3DStructureOf = m_model.createProperty(NS, "is3DStructureOf");
    public static Property hasMotif = m_model.createProperty(NS, "hasMotif");
    public static Property isSubstance = m_model.createProperty(NS, "isSubstance");
}
