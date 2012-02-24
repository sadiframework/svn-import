#-----------------------------------------------------------------
# dev::biordf::net::kawas::owl::DragonServices::getDragonAlleleDescription_Output
# Generated: 12-Aug-2010 09:44:52 PDT
# Contact: Edward Kawas <edward.kawas+owl2perl@gmail.com>
#-----------------------------------------------------------------
package dev::biordf::net::kawas::owl::DragonServices::getDragonAlleleDescription_Output;

use OWL::Data::OWL::Class;
use purl::oclc::org::SADI::LSRN::DragonDB_Allele_Record;


no strict;
use vars qw( @ISA );
@ISA = qw(
 OWL::Data::OWL::Class
 purl::oclc::org::SADI::LSRN::DragonDB_Allele_Record 
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


use sadiframework::org::ontologies::properties::hasDescription;



{
    my %_allowed = (

        hasDescription => {
        	type => 'OWL::Data::OWL::Class',
        	# range checking of classes added 
        	post => sub {
        		my ($self) = shift;
        		return unless $self->strict;
                my $type = @{$self->hasDescription}[-1] if defined $self->hasDescription;
                return unless defined $type and $type->type;
                my $range = new sadiframework::org::ontologies::properties::hasDescription->range();
                return unless $range;
                $range = $self->uri2package($range);
                eval {
                	$range = $range->new();
                };
                return if $@;
        		$self->throw("\n" . $type->type() . "\nis not related to\n" . $range->type()) unless $type->isa(ref($range));
        	},
        	is_array => 1, },


    );

    sub _accessible {
        my ( $self, $attr ) = @_;
        exists $_allowed{$attr} 
          or $self->OWL::Data::OWL::Class::_accessible($attr)
          or $self->purl::oclc::org::SADI::LSRN::DragonDB_Allele_Record::_accessible($attr)
    }

    sub _attr_prop {
        my ( $self, $attr_name, $prop_name ) = @_;
        my $attr = $_allowed{$attr_name};
        return ref($attr) ? $attr->{$prop_name} : $attr if $attr;
        return $self->OWL::Data::OWL::Class::_attr_prop( $attr_name, $prop_name ) 
            if $self->OWL::Data::OWL::Class::_accessible($attr_name);
        return $self->purl::oclc::org::SADI::LSRN::DragonDB_Allele_Record::_attr_prop( $attr_name, $prop_name ) 
            if $self->purl::oclc::org::SADI::LSRN::DragonDB_Allele_Record::_accessible($attr_name);

    }
}

#-----------------------------------------------------------------
# init
#-----------------------------------------------------------------
sub init {
    my ($self) = shift;
    $self->OWL::Data::OWL::Class::init();
    $self->purl::oclc::org::SADI::LSRN::DragonDB_Allele_Record::init();
    $self->type('http://dev.biordf.net/~kawas/owl/DragonServices.owl#getDragonAlleleDescription_Output');

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
        $self->purl::oclc::org::SADI::LSRN::DragonDB_Allele_Record::_get_statements();
    };

    # for each attribute, if is array do:
    if (defined $self->hasDescription){
	    foreach (@{$self->hasDescription}) {
	        # add all statements from $_
	        my $enumerator = $_->_get_statements if defined $_->_get_statements;
	        next unless defined $enumerator;
            my $statement = $enumerator->getFirst;
            while (defined $statement) {
              $self->model->addStmt($statement);
              $statement = $enumerator->getNext
            }
            $enumerator->close;
	        # add a statement linking the graphs
	        $self->model->addStmt(
	          new RDF::Core::Statement(
	              $subject,
	              $subject->new('http://sadiframework.org/ontologies/properties.owl#hasDescription'), 
	              (defined $_->subject ? $_->subject : RDF::Core::Resource->new($_->value))
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

dev::biordf::net::kawas::owl::DragonServices::getDragonAlleleDescription_Output - an automatically generated owl class!

=cut

=head1 SYNOPSIS

  use dev::biordf::net::kawas::owl::DragonServices::getDragonAlleleDescription_Output;
  my $class = dev::biordf::net::kawas::owl::DragonServices::getDragonAlleleDescription_Output->new();

  # get the uri for this class
  my $uri = $class->uri;
  # add object properties 
  # Make sure to use the appropriate OWL class! 
  # OWL::Data::OWL::Class used as an example
  $class->add_hasDescription(new OWL::Data::OWL::Class('#someURI'));

  # get the hasDescription object properties
  my $hasDescription_objects = $class->hasDescription;

=cut

=head1 DESCRIPTION

I<Inherits from>:
L<OWL::Data::OWL::Class>
L<purl::oclc::org::SADI::LSRN::DragonDB_Allele_Record>




=cut

