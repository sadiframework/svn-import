package Vocab::URIPrefixes;

use SADI::GMOD::Config qw(get_base_url);

require Exporter;
our @ISA = qw(Exporter);
our @EXPORT_OK = qw(%uri_prefixes);

our %uri_prefixes = (
    rdf => 'http://www.w3.org/1999/02/22-rdf-syntax-ns#',
    rdfs => 'http://www.w3.org/2000/01/rdf-schema#',
    xsd => 'http://www.w3.org/2001/XMLSchema#',
    sio => 'http://semanticscience.org/resource/',
    range => 'http://sadiframework.org/ontologies/GMOD/RangedSequencePosition.owl#',
    strand => 'http://sadiframework.org/ontologies/GMOD/Strand.owl#',
    feature => 'http://sadiframework.org/ontologies/GMOD/Feature.owl#',
    so => 'http://purl.org/obo/owl/SO#',
    lsrn => 'http://purl.oclc.org/SADI/LSRN/',
    db => get_base_url,
);

1;
