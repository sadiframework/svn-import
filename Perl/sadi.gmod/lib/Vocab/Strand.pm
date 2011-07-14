package Vocab::Strand;

use strict;
use warnings;

#------------------------------------------------------------
# exports
#------------------------------------------------------------

require Exporter;

our @ISA = qw(Exporter);

our @EXPORT_OK = qw(
    STRAND_TYPE_URI
    PLUS_STRAND_TYPE_URI
    MINUS_STRAND_TYPE_URI
);


#------------------------------------------------------------
# constants
#------------------------------------------------------------

use constant ONTOLOGY_PREFIX => "http://sadiframework.org/ontologies/GMOD/Strand.owl#";

use constant STRAND_TYPE_URI => ONTOLOGY_PREFIX . 'Strand';
use constant PLUS_STRAND_TYPE_URI => ONTOLOGY_PREFIX . 'PlusStrand';
use constant MINUS_STRAND_TYPE_URI => ONTOLOGY_PREFIX . 'MinusStrand';

1;
