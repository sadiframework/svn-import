package Vocab::LSRN;

use strict;
use warnings;

#------------------------------------------------------------
# exports
#------------------------------------------------------------

require Exporter;

our @ISA = qw(Exporter);

our @EXPORT_OK = qw(
    LSRN_ONTOLOGY_PREFIX
    LSRN_ENTITY_PREFIX
);


#------------------------------------------------------------
# constants
#------------------------------------------------------------

use constant LSRN_ONTOLOGY_PREFIX => 'http://purl.oclc.org/SADI/LSRN/';
use constant LSRN_ENTITY_PREFIX => 'http://lsrn.org/';

1;
