
package ca.unbsj.cbakerlab.information_extraction_sadi;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

/* package */ class Vocab
	{
	    private static Model m_model = ModelFactory.createDefaultModel();



	    // rdf:value
	    public static final Property value = 
		m_model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#value");
	    
	    public static final Property sourceString = 
			m_model.createProperty("http://nlp2rdf.lod2.eu/schema/string/sourceString");

	    
	    // rss:link
	    public static final Property link = 
		m_model.createProperty("http://purl.org/rss/1.0/link");
 
	    // foaf:topic
	    public static final Property topic = 
		m_model.createProperty("http://xmlns.com/foaf/0.1/topic");

	    
	    // rdf:type
	    public static final Property type = 
		m_model.
		createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
	    
	    public static final String mutationOntologyNS = 
		"http://unbsj.biordf.net/ontologies/mutation-impact-ontology.owl#";
	    
	    public static final String mutationOntologyExtrasNS = 
		"http://unbsj.biordf.net/ontologies/mutation-impact-ontology-extras.owl#";
	    

	    public static final String biordfMobyUniprotOntologyNS = 
		"http://biordf.net/moby/UniProt/";

	    public static final String sioNS = 
		"http://semanticscience.org/resource/";

	    public static final String lsrnNS = 
		"http://purl.oclc.org/SADI/LSRN/";


	    public static final String biboNS = 
		"http://purl.org/ontology/bibo/";


	    public static final Resource PointMutation = 
		m_model.
		createResource(mutationOntologyNS + "PointMutation");

	    public static final Resource MutationSpecification = 
		m_model.
		createResource(mutationOntologyNS + "MutationSpecification");

	    public static final Resource CompoundMutation = 
		m_model.
		createResource(mutationOntologyNS + "CompoundMutation");

	    public static final Resource MutationSeries = 
		m_model.
		createResource(mutationOntologyNS + "MutationSeries");

	    public static final Resource Protein = 
		m_model.
		createResource(mutationOntologyNS + "Protein");

	    public static final Resource ProteinMutant = 
		m_model.
		createResource(mutationOntologyNS + "ProteinMutant");

	    public static final Resource ProteinProperty = 
		m_model.
		createResource(mutationOntologyNS + "ProteinProperty");

	    public static final Resource MutationImpact = 
		m_model.
		createResource(mutationOntologyNS + "MutationImpact");

	    public static final Resource ImpactDirection = 
		m_model.
		createResource(mutationOntologyNS + "ImpactDirection");
	    
	    public static final Resource ProteinPropertyType  = 
		m_model.
		createResource(mutationOntologyExtrasNS + "ProteinPropertyType");
	    

	    public static final Property hasWildtypeResidue = 
		m_model.createProperty(mutationOntologyNS + "hasWildtypeResidue");

	    public static final Property hasMutantResidue = 
		m_model.createProperty(mutationOntologyNS + "hasMutantResidue");
	    
	    public static final Property hasPosition = 
		m_model.createProperty(mutationOntologyNS + "hasPosition");
	    
	    public static final Property hasNormalizedForm = 
		m_model.createProperty(mutationOntologyNS + "hasNormalizedForm");
	    
	    public static final Property hasSequence = 
		m_model.createProperty(mutationOntologyNS + "hasSequence");

	    public static final Property containsElementaryMutation = 
		m_model.createProperty(mutationOntologyNS + "containsElementaryMutation");
	    public static final Property specifiesMutations = 
		m_model.createProperty(mutationOntologyNS + "specifiesMutations");

	    public static final Property groundMutationsTo = 
		m_model.createProperty(mutationOntologyNS + "groundMutationsTo");

	    public static final Property resultsIn = 
		m_model.createProperty(mutationOntologyNS + "resultsIn");

	    public static final Property hasSwissProtId = 
		m_model.createProperty(mutationOntologyNS + "hasSwissProtId");

	    public static final Property specifiesImpact = 
		m_model.createProperty(mutationOntologyNS + "specifiesImpact");

	    public static final Property impactIsSpecifiedBy = 
		m_model.createProperty(mutationOntologyNS + "impactIsSpecifiedBy");

	    public static final Property hasDirection = 
		m_model.createProperty(mutationOntologyNS + "hasDirection");

	    public static final Property affectProperty = 
		m_model.createProperty(mutationOntologyNS + "affectProperty");

	    public static final Property hasSubseries = 
		m_model.createProperty(mutationOntologyNS + "hasSubseries");
	    
	    public static final Property mutationSeriesIsSpecifiedBy =
		m_model.createProperty(mutationOntologyNS + "mutationSeriesIsSpecifiedBy");
	    public static final Property proteinIsSpecifiedAsWildtypeBy =
		m_model.createProperty(mutationOntologyNS + "proteinIsSpecifiedAsWildtypeBy");
	    public static final Property proteinIsSpecifiedAsMutantBy =
		m_model.createProperty(mutationOntologyNS + "proteinIsSpecifiedAsMutantBy");
	    public static final Property proteinPropertyHasType =
		m_model.createProperty(mutationOntologyExtrasNS + "proteinPropertyHasType");

	    public static final Property isPropertyOfProtein =
		m_model.createProperty(mutationOntologyNS + "isPropertyOfProtein");

	    public static final Property hasProperty =
		m_model.createProperty(mutationOntologyNS + "hasProperty");

	    public static final Property propertyIsAffectedByImpact =
		m_model.createProperty(mutationOntologyNS + "propertyIsAffectedByImpact");

	    
	    public static final Property proteinPropertyTypeHasMember =
		m_model.createProperty(mutationOntologyExtrasNS + "proteinPropertyTypeHasMember");

	    
	    public static final Property biologicalEntityTypeHasMember =
		m_model.createProperty(mutationOntologyExtrasNS + "biologicalEntityTypeHasMember");
	    

		public static final Property SIO_000628 = 
			m_model.createProperty(sioNS + "SIO_000628");
		    public static final Property refersTo = SIO_000628;
		



	    public static final Property SIO_000212 = 
		m_model.createProperty(sioNS + "SIO_000212");
	    public static final Property isReferredToBy = SIO_000212;
	    
	    
	    public static final Property SIO_000008 = 
		m_model.createProperty(sioNS + "SIO_000008");
	    public static final Property hasAttribute = SIO_000008;
		
	    public static final Property SIO_000300 = 
		m_model.createProperty(sioNS + "SIO_000300");
	    public static final Property hasValue = SIO_000300;


	    public static final Property SIO_000146 = 
		m_model.createProperty(sioNS + "SIO_000146");
	    public static final Property isInformationAbout = SIO_000146;

	    public static final Property SIO_010302 = 
		m_model.createProperty(sioNS + "SIO_010302");
	    public static final Property isHomologousTo = SIO_010302;

	    public static final Resource UniProt_Identifier = 
		m_model.
		createResource(lsrnNS + "UniProt_Identifier");
	    

	    
		public static final Resource PDB_Identifier = 
		    m_model.
		    createResource(lsrnNS + "PDB_Identifier");

		public static final Resource PDB_Record = 
		    m_model.
		    createResource(lsrnNS + "PDB_Record");

		public static final Resource SIO_010015 = 
		    m_model.
		    createResource(sioNS + "SIO_010015");
		public static final Resource AminoAcidSequence = SIO_010015;


     
	    public static final Property hasJmol3DStructureVisualization = 
		m_model.createProperty("http://sadiframework.org/ontologies/service_objects.owl#hasJmol3DStructureVisualization");
	    
	    public static final Property content = 
		m_model.createProperty(biboNS + "content");


		//public static final Resource Drug = null;
	    public static final Resource SIO_010038 = 
		m_model.createProperty(sioNS + "SIO_010038");
		public static final Resource Drug = SIO_010038;
	    
	    public static final Resource SIO_000116 = 
			m_model.createResource(sioNS + "SIO_000116");
		    public static final Resource Name = SIO_000116;
		    
		    
		    public static final Resource DRUG_BANK_Record = 
				m_model.
				createResource("http://purl.oclc.org/SADI/LSRN/DRUG_BANK_Record");
			    
			    
			    public static final Resource DRUG_BANK_Identifier = 
				m_model.
				createResource("http://purl.oclc.org/SADI/LSRN/DRUG_BANK_Identifier");


					
				    public static final Property SIO_000011 = 
					m_model.createProperty(sioNS + "SIO_000011");
				    public static final Property isAttributeOf = SIO_000011;
					
				    
				    public static final Property SIO_000629 = 
						m_model.createProperty(sioNS + "SIO_000629");
					    public static final Property isSubjectOf = SIO_000629;
					    
					    public static final Resource Class = 
							m_model.createResource("http://www.w3.org/2002/07/owl#Class");
					    
					    // rdfs:subClassOf
					    public static final Property subClassOf = 
						m_model.
						createProperty("http://www.w3.org/2000/01/rdf-schema#subClassOf");

					    
					    // rdfs:label
					    public static final Property label = 
						m_model.
						createProperty("http://www.w3.org/2000/01/rdf-schema#label");

		public static final Resource SIO_010004 = 
		m_model.createProperty(sioNS + "SIO_010004");
		public static final Resource ChemicalEntity = SIO_010004;
							
		public static final Resource SIO_000728 = 
		m_model.createProperty(sioNS + "SIO_000728");
		public static final Resource ChemicalIdentifier = SIO_000728;

	} // class Vocab