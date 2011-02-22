package ca.wilkinsonlab.sadi.vocab;

import java.util.regex.Pattern;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

public class LSRN 
{
	private static Model m_model = ModelFactory.createDefaultModel();

	public static final String ONTOLOGY_PREFIX = "http://purl.oclc.org/SADI/LSRN/";
	public static final String ENTITY_PREFIX = "http://lsrn.org/";
	public static final String OLD_ENTITY_PREFIX = "http://biordf.net/moby/";

	public static final String ENTITY_PREFIX_REGEX = "http://lsrn\\.org/";
	public static final String OLD_ENTITY_PREFIX_REGEX = "http://biordf\\.net/moby/";

	static public class UniProt
	{
		public static final String OLD_UNIPROT_PREFIX = String.format("%sUniProt/", OLD_ENTITY_PREFIX);
		public static final String UNIPROT_PREFIX = String.format("%sUniProt:", ENTITY_PREFIX);
		public static final Resource UNIPROT_TYPE = m_model.createResource(ONTOLOGY_PREFIX + "UniProt_Record");
		public static final Resource UNIPROT_IDENTIFIER = m_model.createResource(ONTOLOGY_PREFIX + "UniProt_Identifier");
	}

	static public class PubChem 
	{
		public static final String OLD_SUBSTANCE_PREFIX = String.format("%sPubChem_Substance/", OLD_ENTITY_PREFIX);
		public static final String SUBSTANCE_PREFIX = String.format("%sPubChem_Substance:", ENTITY_PREFIX);
		public static final Resource SUBSTANCE_TYPE = m_model.createResource(ONTOLOGY_PREFIX + "PubChem_Substance_Record");
		public static final Resource SUBSTANCE_IDENTIFIER = m_model.createResource(ONTOLOGY_PREFIX + "PubChem_Substance_Identifier");
	}
	
	static public class KEGG 
	{
		static public final Pattern[] GENE_URI_PATTERNS = {
			Pattern.compile(String.format("%sKEGG/(\\S+)", OLD_ENTITY_PREFIX_REGEX)), 
			Pattern.compile(String.format("%sKEGG:(\\S+)", ENTITY_PREFIX_REGEX)),
			Pattern.compile(".*[/:#](\\S{3}:\\S+)") // failsafe best-guess pattern
		};

		public static final String OLD_GENE_PREFIX = String.format("%sKEGG/", OLD_ENTITY_PREFIX);
		public static final String GENE_PREFIX = String.format("%sKEGG:", ENTITY_PREFIX);
		public static final Resource GENE_TYPE = m_model.createResource(ONTOLOGY_PREFIX + "KEGG_Record");
		public static final Resource GENE_IDENTIFIER = m_model.createResource(ONTOLOGY_PREFIX + "KEGG_Identifier");

		static public final Pattern[] PATHWAY_URI_PATTERNS = {
			Pattern.compile(String.format("%sKEGG_PATHWAY/(\\S+)", OLD_ENTITY_PREFIX_REGEX)),
			Pattern.compile(String.format("%sKEGG_PATHWAY:(\\S+)", ENTITY_PREFIX_REGEX)),
			Pattern.compile(".*[/:#]([^\\s\\.]+)") // failsafe best-guess pattern
		};

		public static final String OLD_PATHWAY_PREFIX = String.format("%sKEGG_PATHWAY/", OLD_ENTITY_PREFIX);
		public static final String PATHWAY_PREFIX = String.format("%sKEGG_PATHWAY:", ENTITY_PREFIX);
		public static final Resource PATHWAY_TYPE = m_model.createResource(ONTOLOGY_PREFIX + "KEGG_PATHWAY_Record");
		public static final Resource PATHWAY_IDENTIFIER = m_model.createResource(ONTOLOGY_PREFIX + "KEGG_PATHWAY_Identifier");

		static public final Pattern[] COMPOUND_URI_PATTERNS = {
			Pattern.compile(String.format("%sKEGG_COMPOUND/(\\S+)", OLD_ENTITY_PREFIX_REGEX)),
			Pattern.compile(String.format("%sKEGG_COMPOUND:(\\S+)", ENTITY_PREFIX_REGEX)),
			Pattern.compile(".*[/:#](cpd:\\S+)", Pattern.CASE_INSENSITIVE) // failsafe best-guess pattern
		};
		
		public static final String OLD_COMPOUND_PREFIX = String.format("%sKEGG_COMPOUND/", OLD_ENTITY_PREFIX);
		public static final String COMPOUND_PREFIX = String.format("%sKEGG_COMPOUND:", ENTITY_PREFIX);
		public static final Resource COMPOUND_TYPE = m_model.createResource(ONTOLOGY_PREFIX + "KEGG_COMPOUND_Record");
		public static final Resource COMPOUND_IDENTIFIER = m_model.createResource(ONTOLOGY_PREFIX + "KEGG_COMPOUND_Identifier");
	}
	
	static public class PDB 
	{
		public static final String OLD_PDB_PREFIX = String.format("%sPDB/", OLD_ENTITY_PREFIX);
		public static final String PDB_PREFIX = String.format("%sPDB:", ENTITY_PREFIX);
		public static final Resource PDB_TYPE = m_model.createResource(ONTOLOGY_PREFIX + "PDB_Record");
		public static final Resource PDB_IDENTIFIER = m_model.createResource(ONTOLOGY_PREFIX + "PDB_Identifier");
	}
	
	static public class EntrezGene
	{
		static public final Pattern[] GENE_URI_PATTERNS = {
			Pattern.compile(String.format("%sEntrezGene_ID/(\\S+)", OLD_ENTITY_PREFIX_REGEX)),
			Pattern.compile(String.format("%sGeneID:(\\S+)", ENTITY_PREFIX_REGEX)),
			Pattern.compile(".*[/:#]([^\\s\\.]+)") // failsafe best-guess pattern
		};
		
		public static final String OLD_ENTREZ_GENE_PREFIX = String.format("%sEntrezGene_ID/", OLD_ENTITY_PREFIX);
		public static final String ENTREZ_GENE_PREFIX = String.format("%sGeneID:", ENTITY_PREFIX);
		public static final Resource ENTREZ_GENE_TYPE = m_model.createResource(ONTOLOGY_PREFIX + "GeneID_Record");
		public static final Resource ENTREZ_GENE_IDENTIFIER = m_model.createResource(ONTOLOGY_PREFIX + "GeneID_Identifier");
	}
	
}
