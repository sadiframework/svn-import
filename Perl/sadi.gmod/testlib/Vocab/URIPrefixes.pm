package Vocab::URIPrefixes;

#------------------------------------------------------------
# exports
#------------------------------------------------------------

require Exporter;

use vars qw(@ISA @EXPORT_OK);
@ISA = qw(Exporter);
@EXPORT_OK = qw(%uri_prefixes);

#------------------------------------------------------------
# global vars
#------------------------------------------------------------

our %uri_prefixes = (
    sio => 'http://semanticscience.org/resource/',
    range => 'http://sadiframework.org/ontologies/GMOD/RangedSequencePosition.owl#',
    feature => 'http://sadiframework.org/ontologies/GMOD/Feature.owl#',
    strand => 'http://sadiframework.org/ontologies/GMOD/Strand.owl#',
    so => 'http://purl.org/obo/owl/SO#',
    xsd => 'http://www.w3.org/2001/XMLSchema#',
);

1;

