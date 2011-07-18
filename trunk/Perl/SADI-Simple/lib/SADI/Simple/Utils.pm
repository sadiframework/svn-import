package SADI::Simple::Utils;

use strict;
use warnings;

use File::Spec 3.33;
use RDF::Trine::Model 0.135;
use RDF::Trine::Parser 0.135;
use RDF::Trine::Serializer 0.135;
use File::Spec::Functions 3.33;

=head1 NAME

SADI::Simple::Utils - internal utils for manipulating RDF

=cut

=head1 DESCRIPTION

There are no public methods in this module.

=cut

sub rdfxml_to_n3
{
    my ($self, $rdfxml) = @_;

    $rdfxml = $self
      unless ref($self) =~ m/^SADI::Simple::Utils/ or $self =~ /^SADI::Simple::Utils/;

    my $model = RDF::Trine::Model->temporary_model;
    my $parser = RDF::Trine::Parser->new('rdfxml');

    eval { $parser->parse_into_model(undef, $rdfxml, $model) };

    die "failed to convert RDF/XML to TTL, error parsing RDF/XML: $@" if $@;
    
    my $serializer = RDF::Trine::Serializer->new('turtle');
    
    return $self->serialize_model($model, 'text/rdf+n3');
}

sub n3_to_rdfxml
{
    my ($self, $n3) = @_;

    $n3 = $self
      unless ref($self) =~ m/^SADI::Simple::Utils/ or $self =~ /^SADI::Simple::Utils/;

    my $model = RDF::Trine::Model->temporary_model;
    my $parser = RDF::Trine::Parser->new('turtle');

    eval { $parser->parse_into_model(undef, $n3, $model) };

    die "failed to convert N3 to RDF/XML, error parsing N3: $@" if $@;
    
    my $serializer = RDF::Trine::Serializer->new('rdfxml');
    
    return $self->serialize_model($model, 'text/rdf+n3');
}

my @N3_MIME_TYPES = (
    'text/rdf+n3',
    'text/n3',
    'application/x-turtle',
);

sub serialize_model
{
    my ($self, $model, $mime_type) = @_;

    unless(ref($self) =~ m/^SADI::Simple::Utils/ or $self =~ /^SADI::Simple::Utils/) {
        ($model, $mime_type) = @_;
    }

    my $serializer;

    if (grep($_ eq $mime_type, @N3_MIME_TYPES)) {
        $serializer = RDF::Trine::Serializer->new('turtle');
    } else {
        $serializer = RDF::Trine::Serializer->new('rdfxml');
    }
    
    return $serializer->serialize_model_to_string($model);
}

1;
