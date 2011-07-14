package Utils::Directories;

#------------------------------------------------------------
# imports
#------------------------------------------------------------

use strict;
use warnings;

use File::Spec::Functions qw(catdir);

#------------------------------------------------------------
# exports
#------------------------------------------------------------

require Exporter;

our @ISA = qw(Exporter);

our @EXPORT_OK = qw(
    SADI_GMOD_ROOT
    MAPPINGS_DIR
    ONTOLOGIES_DIR
    TEMPLATES_DIR
);


#------------------------------------------------------------
# constants
#------------------------------------------------------------

use constant MAPPINGS_DIR => catdir('resources', 'mappings');
use constant ONTOLOGIES_DIR => catdir('resources', 'ontologies');
use constant TEMPLATES_DIR => catdir('resources', 'templates');

1;

