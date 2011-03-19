package Vocab::GenomicCoordinates;

use constant ONTOLOGY_PREFIX => "http://sadiframework.org/ontologies/GMOD/genomic-coordinates.owl#";

our $IS_RELATIVE_TO = new RDF::Core::Resource(ONTOLOGY_PREFIX . "is-relative-to");
our $START_POSITION = new RDF::Core::Resource(ONTOLOGY_PREFIX . "start-position");
our $END_POSITION = new RDF::Core::Resource(ONTOLOGY_PREFIX . "end-position");
our $STRAND = new RDF::Core::Resource(ONTOLOGY_PREFIX . "strand");

1;
