package ca.wilkinsonlab.sadi.vocab;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

public class PubChem 
{
	private static Model m_model = ModelFactory.createDefaultModel();
	
	public static final String OLD_SUBSTANCE_PREFIX = String.format("%sPubChem_Substance/", Deprecated.ENTITY_PREFIX);
	public static final String SUBSTANCE_PREFIX = String.format("%sPubChem_Substance:", LSRN.ENTITY_PREFIX);
	public static final Resource SUBSTANCE_TYPE = m_model.createResource(LSRN.ONTOLOGY_PREFIX + "PubChem_Substance_Record");
	public static final Resource SUBSTANCE_IDENTIFIER = m_model.createResource(LSRN.ONTOLOGY_PREFIX + "PubChem_Substance_Identifier");
}
