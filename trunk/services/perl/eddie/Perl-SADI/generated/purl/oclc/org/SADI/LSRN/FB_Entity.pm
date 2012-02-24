#-----------------------------------------------------------------
# purl::oclc::org::SADI::LSRN::FB_Entity
# Generated: 12-Aug-2010 09:33:40 PDT
# Contact: Edward Kawas <edward.kawas+owl2perl@gmail.com>
#-----------------------------------------------------------------
package purl::oclc::org::SADI::LSRN::FB_Entity;

use OWL::Data::OWL::Class;
use purl::oclc::org::SADI::LSRN::LSRN_Entity;


no strict;
use vars qw( @ISA );
@ISA = qw(
 OWL::Data::OWL::Class
 purl::oclc::org::SADI::LSRN::LSRN_Entity 
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





{
    my %_allowed = (


    );

    sub _accessible {
        my ( $self, $attr ) = @_;
        exists $_allowed{$attr} 
          or $self->OWL::Data::OWL::Class::_accessible($attr)
          or $self->purl::oclc::org::SADI::LSRN::LSRN_Entity::_accessible($attr)
    }

    sub _attr_prop {
        my ( $self, $attr_name, $prop_name ) = @_;
        my $attr = $_allowed{$attr_name};
        return ref($attr) ? $attr->{$prop_name} : $attr if $attr;
        return $self->OWL::Data::OWL::Class::_attr_prop( $attr_name, $prop_name ) 
            if $self->OWL::Data::OWL::Class::_accessible($attr_name);
        return $self->purl::oclc::org::SADI::LSRN::LSRN_Entity::_attr_prop( $attr_name, $prop_name ) 
            if $self->purl::oclc::org::SADI::LSRN::LSRN_Entity::_accessible($attr_name);

    }
}

#-----------------------------------------------------------------
# init
#-----------------------------------------------------------------
sub init {
    my ($self) = shift;
    $self->OWL::Data::OWL::Class::init();
    $self->purl::oclc::org::SADI::LSRN::LSRN_Entity::init();
    $self->type('http://purl.oclc.org/SADI/LSRN/FB_Entity');

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
    eval {
    	# add parent statements
        $self->purl::oclc::org::SADI::LSRN::LSRN_Entity::_get_statements();
    };


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

purl::oclc::org::SADI::LSRN::FB_Entity - an automatically generated owl class!

=cut

=head1 SYNOPSIS

  use purl::oclc::org::SADI::LSRN::FB_Entity;
  my $class = purl::oclc::org::SADI::LSRN::FB_Entity->new();

  # get the uri for this class
  my $uri = $class->uri;


=cut

=head1 DESCRIPTION

I<Inherits from>:
L<OWL::Data::OWL::Class>
L<purl::oclc::org::SADI::LSRN::LSRN_Entity>




=cut

