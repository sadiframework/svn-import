package Vocab::Properties;

use strict;

use constant ONTOLOGY_PREFIX => "http://sadiframework.org/ontologies/properties.owl#";

our $HAS_SEQUENCE = new RDF::Core::Resource(ONTOLOGY_PREFIX . "hasSequence");

1;
