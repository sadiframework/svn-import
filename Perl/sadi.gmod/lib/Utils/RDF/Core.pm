package Utils::RDF::Core;

use strict;

use constant::boolean;

use RDF::Core::Model;
use RDF::Core::Model::Parser;
use RDF::Core::Storage::Memory;
use RDF::Core::Constants qw(:rdf);

# RDF::Core doesn't parse TURTLE (TTL), so we do it with Trine
use RDF::Trine::Model;
use RDF::Trine::Parser::Turtle;
use RDF::Trine::Serializer::RDFXML;
use File::Slurp;

sub create_memory_model 
{
    my ($file, $base_uri) = @_;

    my $model = new RDF::Core::Model(Storage => new RDF::Core::Storage::Memory);
    load_rdfxml_from_file($model, $base_uri, $file);

    return $model;
}

sub load_rdfxml_from_file
{
    my ($model, $base_uri, $file) = @_;
    _load_rdfxml($model, $base_uri, $file, 'file');
}

sub load_rdfxml_from_string
{
    my ($model, $base_uri, $string) = @_;
    _load_rdfxml($model, $base_uri, $string, 'string');
}

sub _load_rdfxml
{
    my ($model, $base_uri, $string_or_filename, $source_type) = @_;

    die "unrecognized RDF/XML source type: $source_type (expected 'file' or 'string')"
        unless ($source_type eq 'file' || $source_type eq 'string');

    # RDF::Core::Parser requires a baseURI to be set,
    # even if there aren't any relative URIs in the
    # source document.
    $base_uri ||= "default_base_uri#";

    my %options = (
        Model      => $model,
        Source     => $string_or_filename,
        SourceType => $source_type,
        BaseURI    => $base_uri,
    );

    my $parser = new RDF::Core::Model::Parser(%options);
    $parser->parse;
}

sub is_resource
{
    my ($node) = @_;
    return !$node->isLiteral;   
}

sub is_literal
{
    my ($node) = @_;
    return $node->isLiteral;
}

sub as_resource
{
    my ($node) = @_;
    die "cannot cast a literal '" . $node->getLabel . "' to a resource\n" unless is_resource($node); 
    return new RDF::Core::Resource($node->getLabel);    
}

sub get_resource_values
{
    my ($model, $subject, $predicate, $type) = @_;
    
    my @resourceValues = ();
    
    my $objects = $model->getObjects($subject, $predicate);

    foreach my $object (@$objects) {

        next unless is_resource($object);
        $object = as_resource($object);

        if ($type) {
            next unless has_type($model, $subject, $type);
        }
        
        push @resourceValues, $object;

    }
   
    return @resourceValues if wantarray;

    if(@resourceValues > 1) {

        warn sprintf(
            "returning only the first of multiple resource values for subject %s, predicate %s%s",
            $subject->getURI,
            $predicate->getURI,
            $type ? sprintf(", object type %s", $type->getURI) : "");

    }

    return $resourceValues[0];
}

sub get_literal_values
{
    my ($model, $subject, $predicate) = @_;
    
    my @literalValues = ();
    
    my $objects = $model->getObjects($subject, $predicate);

    foreach my $object (@$objects) {
        push @literalValues, $object if is_literal($object);   
    }
    
    return @literalValues if wantarray;

    if(@literalValues > 1) {

        warn sprintf(
            "returning only the first of multiple literal values for subject %s, predicate %s",
            $subject->getURI,
            $predicate->getURI);

    }

    return $literalValues[0];
}

sub has_type
{
    my ($model, $subject, $type) = @_;

    my @types = get_resource_values($model, $subject, new RDF::Core::Resource(RDF_TYPE));
    return grep($_->equals($type), @types);
}

sub load_ttl_from_string
{
    my ($model, $base_uri, $ttl_string) = @_;

    my $trine_model = RDF::Trine::Model->temporary_model;

    my $parser = RDF::Trine::Parser->new('turtle');
    $parser->parse_into_model( $base_uri, $ttl_string, $trine_model );

    load_trine_model($model, $trine_model);
}

sub load_trine_model
{
    my ($model, $trine_model) = @_;

    my $serializer = RDF::Trine::Serializer::RDFXML->new();
    my $rdf_string = $serializer->serialize_model_to_string($trine_model);
   
    load_rdfxml_from_string($model, undef, $rdf_string);    
}

1;
