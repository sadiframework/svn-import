#-----------------------------------------------------------------
# sadiframework::org::ontologies::properties::hasScientificName
# Generated: 12-Aug-2010 09:33:24 PDT
# Contact: Edward Kawas <edward.kawas+owl2perl@gmail.com>
#-----------------------------------------------------------------
package sadiframework::org::ontologies::properties::hasScientificName;

use sadiframework::org::ontologies::properties::hasName;

no strict;
use vars qw( @ISA );
@ISA = qw( sadiframework::org::ontologies::properties::hasName );
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
    $self->uri('http://sadiframework.org/ontologies/properties.owl#hasScientificName');
}

1;
__END__

=head1 NAME

sadiframework::org::ontologies::properties::hasScientificName - an object propery

=head1 SYNOPSIS

  use sadiframework::org::ontologies::properties::hasScientificName;
  my $property = sadiframework::org::ontologies::properties::hasScientificName->new();

  # get the domain of this property
  my $domain = $property->domain;

  # get the range of this property
  my $range = $property->range;

  # get the uri for this property
  my $uri = $property->uri;

=head1 DESCRIPTION

I<Inherits from>: L<http://sadiframework.org/ontologies/properties.owl#hasName|sadiframework::org::ontologies::properties::hasName>

=cut
