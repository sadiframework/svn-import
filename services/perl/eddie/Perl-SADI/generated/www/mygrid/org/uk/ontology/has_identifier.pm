#-----------------------------------------------------------------
# www::mygrid::org::uk::ontology::has_identifier
# Generated: 12-Aug-2010 09:33:25 PDT
# Contact: Edward Kawas <edward.kawas+owl2perl@gmail.com>
#-----------------------------------------------------------------
package www::mygrid::org::uk::ontology::has_identifier;

use www::mygrid::org::uk::ontology::has_part;

no strict;
use vars qw( @ISA );
@ISA = qw( www::mygrid::org::uk::ontology::has_part );
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
    
    # set the range of this object property
    $self->range('http://www.mygrid.org.uk/ontology#bioinformatics_record_id');    
    # set the domain for this object property
    $self->domain('http://www.mygrid.org.uk/ontology#bioinformatics_record');    
    # set the uri for this object property
    $self->uri('http://www.mygrid.org.uk/ontology#has_identifier');
}

1;
__END__

=head1 NAME

www::mygrid::org::uk::ontology::has_identifier - an object propery

=head1 SYNOPSIS

  use www::mygrid::org::uk::ontology::has_identifier;
  my $property = www::mygrid::org::uk::ontology::has_identifier->new();

  # get the domain of this property
  my $domain = $property->domain;

  # get the range of this property
  my $range = $property->range;

  # get the uri for this property
  my $uri = $property->uri;

=head1 DESCRIPTION

I<Inherits from>: L<http://www.mygrid.org.uk/ontology#has_part|www::mygrid::org::uk::ontology::has_part>

=cut
