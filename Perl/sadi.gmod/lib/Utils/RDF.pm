package Utils::RDF;

use strict;
use RDF::Core::Model;

sub create_memory_model 
{
    my ($file, $baseURI) = @_;

    # RDF::Core::Model requires a baseURI to be set,
    # even if there aren't any relative URIs in the
    # source document.
    $baseURI |= "unusedBaseURI#";
    
    my $storage = new RDF::Core::Storage::Memory;
    my $model = new RDF::Core::Model( Storage => $storage );

    my %options = (
        Model      => $model,
        Source     => $file,
        SourceType => 'file',
        BaseURI    => $baseURI,
    );

    my $parser = new RDF::Core::Model::Parser(%options);
    eval { $parser->parse };
    
    if ($@) {
        warn "error creating RDF::Core::Model for $file: $@\n";
        return undef;   
    }

    return $model;
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
    my ($model, $subject, $predicate) = @_;
    
    my @resourceValues = ();
    
    my $objects = $model->getObjects($subject, $predicate);
    foreach my $object (@$objects) {
        push @resourceValues, $object if is_resource($object);  
    }
    
    return @resourceValues;
}

sub get_literal_values
{
    my ($model, $subject, $predicate) = @_;
    
    my @resourceValues = ();
    
    my $objects = $model->getObjects($subject, $predicate);
    foreach my $object (@$objects) {
        push @resourceValues, $object if is_literal($object);   
    }
    
    return @resourceValues;
}

1;
