#-----------------------------------------------------------------
# dev::biordf::net::kawas::owl::getGOTermAssociations::getGOTermAssociationsAGI_Output
# Generated: 19-Aug-2010 10:59:03 PDT
# Contact: Edward Kawas <edward.kawas+owl2perl@gmail.com>
#-----------------------------------------------------------------
package dev::biordf::net::kawas::owl::getGOTermAssociations::getGOTermAssociationsAGI_Output;

use OWL::Data::OWL::Class;
use purl::oclc::org::SADI::LSRN::GO_Record;
use Blank::genid4c6d70c44bfd;


no strict;
use vars qw( @ISA );
@ISA = qw(
 OWL::Data::OWL::Class
 purl::oclc::org::SADI::LSRN::GO_Record
 Blank::genid4c6d70c44bfd 
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
          or $self->purl::oclc::org::SADI::LSRN::GO_Record::_accessible($attr)
          or $self->Blank::genid4c6d70c44bfd::_accessible($attr)
    }

    sub _attr_prop {
        my ( $self, $attr_name, $prop_name ) = @_;
        my $attr = $_allowed{$attr_name};
        return ref($attr) ? $attr->{$prop_name} : $attr if $attr;
        return $self->OWL::Data::OWL::Class::_attr_prop( $attr_name, $prop_name ) 
            if $self->OWL::Data::OWL::Class::_accessible($attr_name);
        return $self->purl::oclc::org::SADI::LSRN::GO_Record::_attr_prop( $attr_name, $prop_name ) 
            if $self->purl::oclc::org::SADI::LSRN::GO_Record::_accessible($attr_name);
        return $self->Blank::genid4c6d70c44bfd::_attr_prop( $attr_name, $prop_name ) 
            if $self->Blank::genid4c6d70c44bfd::_accessible($attr_name);

    }
}

#-----------------------------------------------------------------
# init
#-----------------------------------------------------------------
sub init {
    my ($self) = shift;
    $self->OWL::Data::OWL::Class::init();
    $self->purl::oclc::org::SADI::LSRN::GO_Record::init();
    $self->Blank::genid4c6d70c44bfd::init();
    $self->type('http://dev.biordf.net/~kawas/owl/getGOTermAssociations.owl#getGOTermAssociationsAGI_Output');

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
        $self->purl::oclc::org::SADI::LSRN::GO_Record::_get_statements();
    };
    eval {
    	# add parent statements
        $self->Blank::genid4c6d70c44bfd::_get_statements();
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

dev::biordf::net::kawas::owl::getGOTermAssociations::getGOTermAssociationsAGI_Output - an automatically generated owl class!

=cut

=head1 SYNOPSIS

  use dev::biordf::net::kawas::owl::getGOTermAssociations::getGOTermAssociationsAGI_Output;
  my $class = dev::biordf::net::kawas::owl::getGOTermAssociations::getGOTermAssociationsAGI_Output->new();

  # get the uri for this class
  my $uri = $class->uri;


=cut

=head1 DESCRIPTION

I<Inherits from>:
L<OWL::Data::OWL::Class>
L<purl::oclc::org::SADI::LSRN::GO_Record>
L<Blank::genid4c6d70c44bfd>




=cut

