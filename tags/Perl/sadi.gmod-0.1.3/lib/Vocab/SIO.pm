package Vocab::SIO;

use strict;
use warnings;

require Exporter;

our @ISA = qw(Exporter);
our @EXPORT_OK = qw(
    SIO_ONTOLOGY_PREFIX
    HAS_ATTRIBUTE
    HAS_VALUE
);

use constant SIO_ONTOLOGY_PREFIX => 'http://semanticscience.org/resource/';

use constant HAS_ATTRIBUTE => new RDF::Trine::Node::Resource(SIO_ONTOLOGY_PREFIX . 'SIO_000008');
use constant HAS_VALUE => new RDF::Trine::Node::Resource(SIO_ONTOLOGY_PREFIX . 'SIO_000300');

1;
