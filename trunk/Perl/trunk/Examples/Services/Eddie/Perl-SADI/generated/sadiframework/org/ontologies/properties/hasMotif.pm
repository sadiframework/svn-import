#-----------------------------------------------------------------
# sadiframework::org::ontologies::properties::hasMotif
# Generated: 12-Aug-2010 09:33:24 PDT
# Contact: Edward Kawas <edward.kawas+owl2perl@gmail.com>
#-----------------------------------------------------------------
package sadiframework::org::ontologies::properties::hasMotif;

use semanticscience::org::resource::SIO_000008;

no strict;
use vars qw( @ISA );
@ISA = qw( semanticscience::org::resource::SIO_000008 );
use strict;

{
    my %_allowed = (

    );

    sub _accessible {
        my ( $self, $attr ) = @_;
        exists $_allowed{$attr} or $self->SUPER::_accessible($attr);
    }

    sub _attr_prop {
        my ( $self, $attr_name, $prop_name ) = @_;
        my $attr = $_allowed{$attr_name};
        return ref($attr) ? $attr->{$prop_name} : $attr if $attr;
        return $self->SUPER::_attr_prop( $attr_name, $prop_name );
    }
}

#-----------------------------------------------------------------
# init
#-----------------------------------------------------------------
sub init {
    my ($self) = shift;
    $self->SUPER::init();
            
    # set the uri for this object property
    $self->uri('http://sadiframework.org/ontologies/properties.owl#hasMotif');
}

1;
__END__

=head1 NAME

sadiframework::org::ontologies::properties::hasMotif - an object propery

=head1 SYNOPSIS

  use sadiframework::org::ontologies::properties::hasMotif;
  my $property = sadiframework::org::ontologies::properties::hasMotif->new();

  # get the domain of this property
  my $domain = $property->domain;

  # get the range of this property
  my $range = $property->range;

  # get the uri for this property
  my $uri = $property->uri;

=head1 DESCRIPTION

I<Inherits from>: L<http://semanticscience.org/resource/SIO_000008|semanticscience::org::resource::SIO_000008>

=cut
