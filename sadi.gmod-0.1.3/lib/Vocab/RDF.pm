package Vocab::RDF;

#------------------------------------------------------------
# imports
#------------------------------------------------------------

use strict;
use warnings;

use RDF::Trine::Node::Resource;

#------------------------------------------------------------
# exports
#------------------------------------------------------------

require Exporter;

our @ISA = qw(Exporter);
our @EXPORT_OK = qw(
    RDF_ONTOLOGY_PREFIX
    RDF_TYPE
);

#------------------------------------------------------------
# constants
#------------------------------------------------------------

use constant RDF_ONTOLOGY_PREFIX => 'http://www.w3.org/1999/02/22-rdf-syntax-ns#';

use constant RDF_TYPE => new RDF::Trine::Node::Resource(RDF_ONTOLOGY_PREFIX . 'type');
