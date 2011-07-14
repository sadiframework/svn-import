#-----------------------------------------------------------------
# semanticscience::org::resource::SIO_000324
# Generated: 12-Aug-2010 09:33:25 PDT
# Contact: Edward Kawas <edward.kawas+owl2perl@gmail.com>
#-----------------------------------------------------------------
package semanticscience::org::resource::SIO_000324;

use semanticscience::org::resource::SIO_000202;

no strict;
use vars qw( @ISA );
@ISA = qw( semanticscience::org::resource::SIO_000202 );
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
    $self->uri('http://semanticscience.org/resource/SIO_000324');
}

1;
__END__

=head1 NAME

semanticscience::org::resource::SIO_000324 - an object propery

=head1 SYNOPSIS

  use semanticscience::org::resource::SIO_000324;
  my $property = semanticscience::org::resource::SIO_000324->new();

  # get the domain of this property
  my $domain = $property->domain;

  # get the range of this property
  my $range = $property->range;

  # get the uri for this property
  my $uri = $property->uri;

=head1 DESCRIPTION

I<Inherits from>: L<http://semanticscience.org/resource/SIO_000202|semanticscience::org::resource::SIO_000202>

=cut
