#-----------------------------------------------------------------
# www::mygrid::org::uk::ontology::is_identifier_of
# Generated: 12-Aug-2010 09:33:25 PDT
# Contact: Edward Kawas <edward.kawas+owl2perl@gmail.com>
#-----------------------------------------------------------------
package www::mygrid::org::uk::ontology::is_identifier_of;

use www::mygrid::org::uk::ontology::is_part_of;

no strict;
use vars qw( @ISA );
@ISA = qw( www::mygrid::org::uk::ontology::is_part_of );
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
    $self->range('http://www.mygrid.org.uk/ontology#bioinformatics_record');    
    # set the domain for this object property
    $self->domain('http://www.mygrid.org.uk/ontology#bioinformatics_record_id');    
    # set the uri for this object property
    $self->uri('http://www.mygrid.org.uk/ontology#is_identifier_of');
}

1;
__END__

=head1 NAME

www::mygrid::org::uk::ontology::is_identifier_of - an object propery

=head1 SYNOPSIS

  use www::mygrid::org::uk::ontology::is_identifier_of;
  my $property = www::mygrid::org::uk::ontology::is_identifier_of->new();

  # get the domain of this property
  my $domain = $property->domain;

  # get the range of this property
  my $range = $property->range;

  # get the uri for this property
  my $uri = $property->uri;

=head1 DESCRIPTION

I<Inherits from>: L<http://www.mygrid.org.uk/ontology#is_part_of|www::mygrid::org::uk::ontology::is_part_of>

=cut
