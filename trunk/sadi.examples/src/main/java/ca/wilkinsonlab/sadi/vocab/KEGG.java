package ca.wilkinsonlab.sadi.vocab;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

public class KEGG 
{
	private static Model m_model = ModelFactory.createDefaultModel();
	
	public static final String OLD_GENE_PREFIX = String.format("%sKEGG/", Deprecated.ENTITY_PREFIX);
	public static final String GENE_PREFIX = String.format("%sKEGG:", LSRN.ENTITY_PREFIX);
	public static final Resource GENE_TYPE = m_model.createResource(LSRN.ONTOLOGY_PREFIX + "KEGG_Record");
	public static final Resource GENE_IDENTIFIER = m_model.createResource(LSRN.ONTOLOGY_PREFIX + "KEGG_Identifier");

	public static final String OLD_PATHWAY_PREFIX = String.format("%sKEGG_PATHWAY/", Deprecated.ENTITY_PREFIX);
	public static final String PATHWAY_PREFIX = String.format("%sKEGG_PATHWAY:", LSRN.ENTITY_PREFIX);
	public static final Resource PATHWAY_TYPE = m_model.createResource(LSRN.ONTOLOGY_PREFIX + "KEGG_PATHWAY_Record");
	public static final Resource PATHWAY_IDENTIFIER = m_model.createResource(LSRN.ONTOLOGY_PREFIX + "KEGG_PATHWAY_Identifier");

	public static final String OLD_COMPOUND_PREFIX = String.format("%sKEGG_COMPOUND/", Deprecated.ENTITY_PREFIX);
	public static final String COMPOUND_PREFIX = String.format("%sKEGG_COMPOUND:", LSRN.ENTITY_PREFIX);
	public static final Resource COMPOUND_TYPE = m_model.createResource(LSRN.ONTOLOGY_PREFIX + "KEGG_COMPOUND_Record");
	public static final Resource COMPOUND_IDENTIFIER = m_model.createResource(LSRN.ONTOLOGY_PREFIX + "KEGG_COMPOUND_Identifier");
	
}
