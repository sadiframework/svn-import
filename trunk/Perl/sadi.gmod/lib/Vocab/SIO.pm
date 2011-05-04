package Vocab::SIO;

use strict;

use constant ONTOLOGY_PREFIX => "http://semanticscience.org/resource/";

#///////////////////////////////////////////////////////////
#// properties
#///////////////////////////////////////////////////////////

our $HAS_ATTRIBUTE = new RDF::Core::Resource(ONTOLOGY_PREFIX . "SIO_000008");
our $IS_ATTRIBUTE_OF = new RDF::Core::Resource(ONTOLOGY_PREFIX . "SIO_000011");
our $HAS_VALUE = new RDF::Core::Resource(ONTOLOGY_PREFIX . "SIO_000300");

our $HAS_PROPER_PART = new RDF::Core::Resource(ONTOLOGY_PREFIX . "SIO_000053");
our $IS_PROPER_PART_OF = new RDF::Core::Resource(ONTOLOGY_PREFIX . "SIO_000093");

#///////////////////////////////////////////////////////////
#// classes
#///////////////////////////////////////////////////////////

our $IDENTIFIER = new RDF::Core::Resource(ONTOLOGY_PREFIX . "SIO_000115");
our $ORDINAL_NUMBER = new RDF::Core::Resource(ONTOLOGY_PREFIX . "SIO_000613");
our $NUMBER = new RDF::Core::Resource(ONTOLOGY_PREFIX . "SIO_000366");

our $SEQUENCE = new RDF::Core::Resource(ONTOLOGY_PREFIX . "SIO_000030");
our $DNA_SEQUENCE = new RDF::Core::Resource(ONTOLOGY_PREFIX . "SIO_010018");

1;
