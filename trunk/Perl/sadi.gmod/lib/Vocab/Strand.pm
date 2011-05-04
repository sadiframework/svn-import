package Vocab::Strand;

use strict;

use constant ONTOLOGY_PREFIX => "http://sadiframework.org/ontologies/GMOD/Strand.owl#";

#//////////////////////////////////////////////////////
# properties
#//////////////////////////////////////////////////////

#//////////////////////////////////////////////////////
# classes
#//////////////////////////////////////////////////////

our $STRAND = new RDF::Core::Resource(ONTOLOGY_PREFIX . "Strand");
our $PLUS_STRAND = new RDF::Core::Resource(ONTOLOGY_PREFIX . "PlusStrand");
our $MINUS_STRAND = new RDF::Core::Resource(ONTOLOGY_PREFIX . "MinusStrand");

1;
