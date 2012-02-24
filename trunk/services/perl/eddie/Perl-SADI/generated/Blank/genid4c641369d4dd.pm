#-----------------------------------------------------------------
# Blank::genid4c641369d4dd
# Generated: 12-Aug-2010 09:34:47 PDT
# Contact: Edward Kawas <edward.kawas+owl2perl@gmail.com>
#-----------------------------------------------------------------
package Blank::genid4c641369d4dd;

use OWL::Data::OWL::Class;


no strict;
use vars qw( @ISA );
@ISA = qw(
 OWL::Data::OWL::Class 
);
use strict;

# imports 
use OWL::Data::String;
use OWL::Data::Integer;
use OWL::Data::Float;
use OWL::Data::Boolean;
use OWL::Data::DateTime;

use OWL::RDF::Predicates::DC_PROTEGE;
use OWL::RDF::Predicates::OMG_LSID;
use OWL::RDF::Predicates::OWL;
use OWL::RDF::Predicates::RDF;
use OWL::RDF::Predicates::RDFS;



use semanticscience::org::resource::SIO_000300;


{
    my %_allowed = (


        SIO_000300 => {
            type => 'semanticscience::org::resource::SIO_000300',
            is_array => 1, },

    );

    sub _accessible {
        my ( $self, $attr ) = @_;
        exists $_allowed{$attr} 
          or $self->OWL::Data::OWL::Class::_accessible($attr)
    }

    sub _attr_prop {
        my ( $self, $attr_name, $prop_name ) = @_;
        my $attr = $_allowed{$attr_name};
        return ref($attr) ? $attr->{$prop_name} : $attr if $attr;
        return $self->OWL::Data::OWL::Class::_attr_prop( $attr_name, $prop_name ) 
            if $self->OWL::Data::OWL::Class::_accessible($attr_name);

    }
}

#-----------------------------------------------------------------
# init
#-----------------------------------------------------------------
sub init {
    my ($self) = shift;
    $self->OWL::Data::OWL::Class::init();
    $self->type('_:genid4c641369d4dd');

}

# returns an RDF::Core::Enumerator object or undef;
sub _get_statements {
	# use_type is undef initially and 1 for super() calls
	my ($self) = @_;

    # create a named resource or bnode for this object ....
    my $subject = new RDF::Core::Resource( $self->value ) if defined $self->value and $self->value ne '';
    $subject = 
        new RDF::Core::Resource( "_:a" . sprintf( "%08x%04x", time(), int(rand(0xFFFF))) )
      unless defined $self->value and $self->value ne '';
    # set the subject so that this sub graph can be linked to other graphs
    $self->subject($subject);
    
    my $add_type_statement = defined $self->value and $self->value ne '' ? 1 : undef;

    # in case we have a bnode
    $self->value($subject->getURI) unless defined $self->value and $self->value ne '';

    # add parent statements too
    eval {
    	# add parent statements
        $self->OWL::Data::OWL::Class::_get_statements();
    };

    if (defined $self->SIO_000300){
	    foreach (@{$self->SIO_000300}) {
	        $self->model->addStmt(
	          new RDF::Core::Statement(
	              $subject,
	              $subject->new('http://semanticscience.org/resource/SIO_000300'), 
	              new RDF::Core::Literal( 
	                $_->value, undef, (defined $_->range ? $_->range : undef) 
	              ),
	          )
	        );
	        $add_type_statement = 1;
	    }
    }

    # add a label if one is specified
    if (defined $self->label()) {
        $self->model->addStmt(
           new RDF::Core::Statement(
            $subject,
            $subject->new( OWL::RDF::Predicates::RDFS->label ),
            new RDF::Core::Literal( $self->label )
           )
        );
    }
    
    # add the type
    $self->model->addStmt(
        new RDF::Core::Statement(
            $subject,
            $subject->new( OWL::RDF::Predicates::RDF->type ),
            new RDF::Core::Resource( $self->type )
        )
    ) if $add_type_statement;
    return $self->model()->getStmts();
}

1;

__END__

=head1 NAME

Blank::genid4c641369d4dd - an automatically generated owl class!

=cut

=head1 SYNOPSIS

  use Blank::genid4c641369d4dd;
  my $class = Blank::genid4c641369d4dd->new();

  # get the uri for this class
  my $uri = $class->uri;
  # add datatype properties 
  use semanticscience::org::resource::SIO_000300;
  my $SIO_000300 = new semanticscience::org::resource::SIO_000300('some value');
  $class->add_SIO_000300($SIO_000300);
  # get the SIO_000300 datatype properties
  my $SIO_000300_property = $class->SIO_000300;

=cut

=head1 DESCRIPTION

I<Inherits from>:
L<OWL::Data::OWL::Class>




=cut

