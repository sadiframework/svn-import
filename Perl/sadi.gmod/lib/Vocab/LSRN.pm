package Vocab::LSRN;

use strict;

use constant ONTOLOGY_PREFIX => "http://purl.oclc.org/SADI/LSRN/";

#///////////////////////////////////////////////////////////
#// classes
#///////////////////////////////////////////////////////////

our $FLYBASE_IDENTIFIER = new RDF::Core::Resource(ONTOLOGY_PREFIX . "FLYBASE_Identifier");
our $GB_IDENTIFIER = new RDF::Core::Resource(ONTOLOGY_PREFIX . "GB_Identifier");

1;
