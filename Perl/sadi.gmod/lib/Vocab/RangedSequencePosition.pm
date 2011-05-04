package Vocab::RangedSequencePosition;

use strict;

use constant ONTOLOGY_PREFIX => "http://sadiframework.org/ontologies/GMOD/RangedSequencePosition.owl#";

#//////////////////////////////////////////////////////
# properties
#//////////////////////////////////////////////////////

our $IN_RELATION_TO = new RDF::Core::Resource(ONTOLOGY_PREFIX . "in_relation_to");

#//////////////////////////////////////////////////////
# classes
#//////////////////////////////////////////////////////

our $RANGED_SEQUENCE_POSITION = new RDF::Core::Resource(ONTOLOGY_PREFIX . "RangedSequencePosition");
our $START_POSITION = new RDF::Core::Resource(ONTOLOGY_PREFIX . "StartPosition");
our $END_POSITION = new RDF::Core::Resource(ONTOLOGY_PREFIX . "EndPosition");

1;

