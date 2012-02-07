package ca.wilkinsonlab.sadi.vocab;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

public class LSRN 
{
	public static final String ONTOLOGY_PREFIX = "http://purl.oclc.org/SADI/LSRN/";
	
	public static Pattern[] ADDITIONAL_UNIPROT_URI_PATTERNS = new Pattern[] {
		Pattern.compile("http://purl.uniprot.org/uniprot/([^\\s\\.\\?]*)"),
		Pattern.compile("http://www.uniprot.org/uniprot/([^\\s\\.\\?]*)"),
	};

	public static final LSRNRecordType DDB = new LSRNRecordType("DDB"); // DictyBase
	public static final LSRNRecordType EcoGene = new LSRNRecordType("ECOGENE"); 
	public static final LSRNRecordType Ensembl = new LSRNRecordType("ENSEMBL");
	public static final LSRNRecordType FlyBase = new LSRNRecordType("FLYBASE");
	public static final LSRNRecordType HGNC = new LSRNRecordType("HGNC");
	public static final LSRNRecordType MGI = new LSRNRecordType("MGI");
	public static final LSRNRecordType OMIM = new LSRNRecordType("OMIM");
	public static final LSRNRecordType PDB = new LSRNRecordType("PDB", "PDB");
	public static final LSRNRecordType PMID = new LSRNRecordType("PMID");
	public static final LSRNRecordType RGD = new LSRNRecordType("RGD");
	public static final LSRNRecordType SGD = new LSRNRecordType("SGD");
	public static final LSRNRecordType UniProt = new LSRNRecordType("UniProt", "UniProt", ADDITIONAL_UNIPROT_URI_PATTERNS);
	public static final LSRNRecordType WormBase = new LSRNRecordType("WormBase");
	public static final LSRNRecordType ZFIN = new LSRNRecordType("ZFIN");
	
	static public class Entrez
	{
		public static final LSRNRecordType Gene = new LSRNRecordType("GeneID", "EntrezGene_ID");
	}
	
	static public class PubChem 
	{
		public static final LSRNRecordType Substance = new LSRNRecordType("PubChem_Substance", "PubChem_Substance");
	}
	
	static public class KEGG 
	{
		static public final LSRNRecordType Gene = new LSRNRecordType("KEGG", "KEGG", Pattern.compile(".*[/:#](\\S{3}:\\S+)"));
		static public final LSRNRecordType Pathway = new LSRNRecordType("KEGG_PATHWAY", "KEGG_PATHWAY");
		static public final LSRNRecordType Compound = new LSRNRecordType("KEGG_COMPOUND", "KEGG_COMPOUND", Pattern.compile(".*[/:#](cpd:\\S+)", Pattern.CASE_INSENSITIVE));
	}
	
	static public final class LSRNRecordType
	{
		private final static Model m_model = ModelFactory.createDefaultModel();
		
		private final static String URI_PREFIX = "http://lsrn.org/";
		private final static String OLD_URI_PREFIX = "http://biordf.net/moby/";

		public final static String URI_PREFIX_REGEX = "http://lsrn\\.org/";
		public final static String OLD_URI_PREFIX_REGEX = "http://biordf\\.net/moby/";
		
		public final static Pattern DEFAULT_FAILSAFE_URI_PATTERN = Pattern.compile(".*[/:#]([^\\s\\.]+)");
		
		private final String namespace;
		private final String mobyNamespace;

		private final String uriPrefix;
		private final String oldUriPrefix;
		
		private final Resource recordType;
		private final Resource identifierType;
		
		private final List<Pattern> uriPatterns;
		
		/**
		 * @param namespace The LSRN namespace for the record type (e.g. "UniProt").  
		 */
		public LSRNRecordType(String namespace) {
			this(namespace, null, null, null);
		}

		/**
		 * @param namespace The LSRN namespace for the record type (e.g. "UniProt").  
		 * @param mobyNamespace The Moby namespace that is equivalent to the LSRN namespace.  This is useful
		 * for compatibility with older SADI services that use a legacy URI scheme 
		 * (http://biordf.net/moby/$MOBY_NAMESPACE/$ID). This argument may be null.
		 */
		public LSRNRecordType(String namespace, String mobyNamespace) {
			this(namespace, mobyNamespace, null, null);
		}
		
		/**
		 * @param namespace The LSRN namespace for the record type (e.g. "UniProt").  
		 * @param failsafeUriPattern This regex is used as a last ditch attempt to extract an ID from a given URI,
		 * if the URI doesn't match any of the other regexes (including those provided by additionalUriPatterns).
		 * The default value for failsafeUriPattern is ".*[/:#]([^\\s\\.]+)". This argument may be null.
		 */
		public LSRNRecordType(String namespace, Pattern failsafeUriPattern) {
			this(namespace, null, null, failsafeUriPattern);
		}

		/**
		 * @param namespace The LSRN namespace for the record type (e.g. "UniProt").  
		 * @param mobyNamespace The Moby namespace that is equivalent to the LSRN namespace.  This is useful
		 * for compatibility with older SADI services that use a legacy URI scheme 
		 * (http://biordf.net/moby/$MOBY_NAMESPACE/$ID). This argument may be null.
		 * @param failsafeUriPattern This regex is used as a last ditch attempt to extract an ID from a given URI,
		 * if the URI doesn't match any of the other regexes (including those provided by additionalUriPatterns).
		 * The default value for failsafeUriPattern is ".*[/:#]([^\\s\\.]+)". This argument may be null.
		 */
		public LSRNRecordType(String namespace, String mobyNamespace, Pattern failsafeUriPattern) {
			this(namespace, mobyNamespace, null, failsafeUriPattern);
		}

		/**
		 * @param namespace The LSRN namespace for the record type (e.g. "UniProt").  
		 * @param additionalUriPatterns Additional URI regexes that correspond to this LSRN record type.
		 * The default URI regexes are "http://lsrn\.org/$NAMESPACE/(\\S+)" and 
		 * "http://biordf\.net/moby/$MOBY_NAMESPACE/(\\S+)", where the bracketed group represents the 
		 * ID portion of the URI. The provided URI patterns should likewise contain a single capturing 
		 * group for the ID portion of the URI. This argument may be null.
		 */
		public LSRNRecordType(String namespace, Pattern[] additionalUriPatterns) {
			this(namespace, null, additionalUriPatterns, null);
		}
		
		/**
		 * @param namespace The LSRN namespace for the record type (e.g. "UniProt").  
		 * @param additionalUriPatterns Additional URI regexes that correspond to this LSRN record type.
		 * The default URI regexes are "http://lsrn\.org/$NAMESPACE/(\\S+)" and 
		 * "http://biordf\.net/moby/$MOBY_NAMESPACE/(\\S+)", where the bracketed group represents the 
		 * ID portion of the URI. The provided URI patterns should likewise contain a single capturing 
		 * group for the ID portion of the URI. This argument may be null.
		 * @param failsafeUriPattern This regex is used as a last ditch attempt to extract an ID from a given URI,
		 * if the URI doesn't match any of the other regexes (including those provided by additionalUriPatterns).
		 * The default value for failsafeUriPattern is ".*[/:#]([^\\s\\.]+)". This argument may be null.
		 */
		public LSRNRecordType(String namespace, Pattern[] additionalUriPatterns, Pattern failsafeUriPattern) {
			this(namespace, null, additionalUriPatterns, failsafeUriPattern);
		}

		/**
		 * @param namespace The LSRN namespace for the record type (e.g. "UniProt").  
		 * @param mobyNamespace The Moby namespace that is equivalent to the LSRN namespace.  This is useful
		 * for compatibility with older SADI services that use a legacy URI scheme 
		 * (http://biordf.net/moby/$MOBY_NAMESPACE/$ID). This argument may be null.
		 * @param additionalUriPatterns Additional URI regexes that correspond to this LSRN record type.
		 * The default URI regexes are "http://lsrn\.org/$NAMESPACE/(\\S+)" and 
		 * "http://biordf\.net/moby/$MOBY_NAMESPACE/(\\S+)", where the bracketed group represents the 
		 * ID portion of the URI. The provided URI patterns should likewise contain a single capturing 
		 * group for the ID portion of the URI. This argument may be null.
		 */
		public LSRNRecordType(String namespace, String mobyNamespace, Pattern[] additionalUriPatterns) {
			this(namespace, mobyNamespace, additionalUriPatterns, null);
		}
		
		/**
		 * @param namespace The LSRN namespace for the record type (e.g. "UniProt").  
		 * @param mobyNamespace The Moby namespace that is equivalent to the LSRN namespace.  This is useful
		 * for compatibility with older SADI services that use a legacy URI scheme 
		 * (http://biordf.net/moby/$MOBY_NAMESPACE/$ID). This argument may be null.
		 * @param additionalUriPatterns Additional URI regexes that correspond to this LSRN record type.
		 * The default URI regexes are "http://lsrn\.org/$NAMESPACE/(\\S+)" and 
		 * "http://biordf\.net/moby/$MOBY_NAMESPACE/(\\S+)", where the bracketed group represents the 
		 * ID portion of the URI. The provided URI patterns should likewise contain a single capturing 
		 * group for the ID portion of the URI. This argument may be null.
		 * @param failsafeUriPattern This regex is used as a last ditch attempt to extract an ID from a given URI,
		 * if the URI doesn't match any of the other regexes (including those provided by additionalUriPatterns).
		 * The default value for failsafeUriPattern is ".*[/:#]([^\\s\\.]+)". This argument may be null.
		 */
		public LSRNRecordType(String namespace, String mobyNamespace, Pattern[] additionalUriPatterns, Pattern failsafeUriPattern) 
		{
			
			this.namespace = namespace;
			this.mobyNamespace = mobyNamespace;
			
			this.uriPrefix = String.format("%s%s:", URI_PREFIX, this.namespace);
			this.oldUriPrefix = (mobyNamespace != null) ? String.format("%s%s/", OLD_URI_PREFIX, this.mobyNamespace) : null;
		
			this.recordType = m_model.createResource(String.format("%s%s_Record", ONTOLOGY_PREFIX, this.namespace));
			this.identifierType = m_model.createResource(String.format("%s%s_Identifier", ONTOLOGY_PREFIX, this.namespace));
			
			this.uriPatterns = new ArrayList<Pattern>();
			
			this.uriPatterns.add(Pattern.compile(String.format("%s%s:(\\S+)", URI_PREFIX_REGEX, this.namespace)));
			
			if(mobyNamespace != null) {
				this.uriPatterns.add(Pattern.compile(String.format("%s%s/(\\S+)", OLD_URI_PREFIX_REGEX, this.mobyNamespace)));
			}

			if(additionalUriPatterns != null) {
				for(Pattern pattern : additionalUriPatterns) {
					this.uriPatterns.add(pattern);
				}
			}
			
			if(failsafeUriPattern != null) {
				this.uriPatterns.add(failsafeUriPattern);
			} else {
				this.uriPatterns.add(DEFAULT_FAILSAFE_URI_PATTERN);
			}
			
		}
		
		public String getNamespace() {
			return namespace;
		}

		public String getMobyNamespace() {
			return mobyNamespace;
		}

		public String getUriPrefix() {
			return uriPrefix;
		}

		public String getOldUriPrefix() {
			return oldUriPrefix;
		}

		public Resource getRecordTypeURI() {
			return recordType;
		}

		public Resource getIdentifierTypeURI() {
			return identifierType;
		}

		public Pattern[] getURIPatterns() {
			return uriPatterns.toArray(new Pattern[0]);
		}
	}
	
}
