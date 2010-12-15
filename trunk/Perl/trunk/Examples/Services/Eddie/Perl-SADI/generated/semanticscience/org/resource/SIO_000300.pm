#-----------------------------------------------------------------
# semanticscience::org::resource::SIO_000300
# Generated: 12-Aug-2010 09:33:25 PDT
# Contact: Edward Kawas <edward.kawas+owl2perl@gmail.com>
#-----------------------------------------------------------------
package semanticscience::org::resource::SIO_000300;

use OWL::Data::OWL::DatatypeProperty;

no strict;
use vars qw( @ISA );
@ISA = qw( OWL::Data::OWL::DatatypeProperty );
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
            
    # set the uri for this datatype property
    $self->uri('http://semanticscience.org/resource/SIO_000300');
    # set the value of this datatype property
    $self->value('some value');
    
    
}

1;
__END__

=head1 NAME

semanticscience::org::resource::SIO_000300 - a datatype propery

=head1 SYNOPSIS

  use semanticscience::org::resource::SIO_000300;
  my $property = semanticscience::org::resource::SIO_000300->new();

  # get the domain of this property
  my $domain = $property->domain;

  # get the range of this property
  my $range = $property->range;

  # get the uri for this property
  my $uri = $property->uri;

  
  # set the value for a resource that 
  # http://semanticscience.org/resource/SIO_000300 is 
  # a predicate on 
  $property->value('some literal value');
  

=head1 DESCRIPTION

I<Inherits from>: L<OWL::Data::OWL::DatatypeProperty|OWL::Data::OWL::DatatypeProperty>

=cut
