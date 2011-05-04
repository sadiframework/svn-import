package Utils::RDF::Trine;

use strict;

use RDF::Trine::Parser;
use RDF::Core::Model::Serializer;

sub load_rdf_core_model
{
    my ($trine_model, $rdf_core_model) = @_;
  
    my $rdfxml;
    my $serializer = new RDF::Core::Model::Serializer(Model => $rdf_core_model, Output => \$rdfxml);
    $serializer->serialize;

    my $parser = new RDF::Trine::Parser('rdfxml');

    # There are no relative URIs in $rdfxml, since no BaseURI was given to
    # RDF::Core::Model::Serializer.  Thus it is safe to use 'undef' for
    # the base URI here.
    
    $parser->parse_into_model(undef, $rdfxml, $trine_model);             
}

sub ttl_to_rdfxml
{
    my ($ttl_string) = @_;

    my $trine_model = RDF::Trine::Model->temporary_model;

    my $parser = RDF::Trine::Parser->new('turtle');
    $parser->parse_into_model(undef, $ttl_string, $trine_model);
    
    my $serializer = RDF::Trine::Serializer::RDFXML->new();
    my $rdfxml_string = $serializer->serialize_model_to_string($trine_model);

    return $rdfxml_string;
}

1;

