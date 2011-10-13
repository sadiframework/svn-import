package Vocab::SO;

#------------------------------------------------------------
# imports
#------------------------------------------------------------

use strict;
use warnings;

#------------------------------------------------------------
# exports
#------------------------------------------------------------

require Exporter;

use vars qw(@ISA @EXPORT_OK);

@ISA = qw(Exporter);

@EXPORT_OK = qw(
    SO_ONTOLOGY_PREFIX
    SO_ONTOLOGY_URL
);

#------------------------------------------------------------
# constants
#------------------------------------------------------------

use constant SO_PREFIX => 'SO:';
use constant SO_ONTOLOGY_URL => 'http://purl.org/obo/owl/SO';
use constant SO_ONTOLOGY_PREFIX => SO_ONTOLOGY_URL . '#';

1;
