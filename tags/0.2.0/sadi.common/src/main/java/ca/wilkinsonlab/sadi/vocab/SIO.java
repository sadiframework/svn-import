package ca.wilkinsonlab.sadi.vocab;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * SIO vocabulary.
 * Modeled on the vocabulary classes included with the ARQ distribution.
 * 
 * @author Luke McCarthy
 */
public class SIO
{
	private static Model m_model = ModelFactory.createDefaultModel();
	
	/**
	 * The namespace of the vocabulary as a string.
	 */
	public static final String NS = "http://semanticscience.org/resource/";
	
	/** 
	 * Returns the namespace of the vocabulary as a string.
	 * @return the namespace of the vocabulary as a string.
	 */
    public static String getURI() { return NS; }
    
    /**
     * The namespace of the vocabulary as a resource.
     */
    public static final Resource NAMESPACE = m_model.createResource( NS );
    
    public static final Resource name = m_model.createResource( NS + "SIO_000116" );
    public static final Resource preferred_name = m_model.createResource( NS + "SIO_000117" );
    public static final Resource scientific_name = m_model.createResource( NS + "SIO_000120" );
    public static final Resource biopolymer_sequence = m_model.createResource( NS + "SIO_000030" );
    public static final Resource amino_acid_sequence = m_model.createResource( NS + "SIO_010015" );
    public static final Resource nucleic_acid_sequence = m_model.createResource( NS + "SIO_010016" );
    public static final Resource protein_sequence = amino_acid_sequence;
    public static final Resource _3d_structure_model = m_model.createResource( NS + "SIO_010530" );
    public static final Resource probability = m_model.createResource( NS + "SIO_000638" );
    public static final Resource software_application = m_model.createResource( NS + "SIO_000101" );
    public static final Resource software_execution = m_model.createResource( NS + "SIO_000667" );
    public static final Resource start_time = m_model.createResource( NS + "SIO_000669" );
    public static final Resource end_time = m_model.createResource( NS + "SIO_000670" );
    public static final Resource version_identifier = m_model.createResource( NS + "SIO_000653" );
    public static final Resource sequence_start_position = m_model.createResource( NS + "SIO_000791" );
    public static final Resource sequence_stop_position = m_model.createResource( NS + "SIO_000792" );
    
    public static final Property has_attribute = m_model.createProperty( NS, "SIO_000008" );
    public static final Property is_attribute_of = m_model.createProperty( NS, "SIO_000011" );
	public static final Property has_value = m_model.createProperty( NS, "SIO_000300" );
	public static final Property has_unit = m_model.createProperty( NS, "SIO_000221" );
	public static final Property has_participant = m_model.createProperty( NS, "SIO_000132" );
	public static final Property is_participant_in = m_model.createProperty( NS, "SIO_000062" );
	public static final Property encodes = m_model.createProperty( NS, "SIO_010078" );
	public static final Property is_encoded_by = m_model.createProperty( NS, "SIO_010079" );
	public static final Property is_related_to = m_model.createProperty( NS, "SIO_000219" );
	public static final Property is_located_in = m_model.createProperty( NS,"SIO_000061" );
	public static final Property is_variant_of = m_model.createProperty( NS, "SIO_000272" );
	public static final Property has_function = m_model.createProperty( NS, "SIO_000225" );
	public static final Property has_reference = m_model.createProperty( NS, "SIO_000253" );
	public static final Property is_homologous_to = m_model.createProperty( NS, "SIO_010302");
	public static final Property is_causally_related_with = m_model.createProperty( NS, "SIO_000243");
	public static final Property has_part = m_model.createProperty( NS, "SIO_000028" );
	public static final Property is_part_of = m_model.createProperty( NS, "SIO_000068" );
	public static final Property is_output_of = m_model.createProperty( NS, "SIO_000232" );
	public static final Property has_input = m_model.createProperty( NS, "SIO_000230" );
	public static final Property has_agent = m_model.createProperty( NS, "SIO_000139" );
}
