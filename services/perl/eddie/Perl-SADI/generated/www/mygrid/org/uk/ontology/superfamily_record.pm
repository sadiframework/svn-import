#-----------------------------------------------------------------
# www::mygrid::org::uk::ontology::superfamily_record
# Generated: 12-Aug-2010 09:35:23 PDT
# Contact: Edward Kawas <edward.kawas+owl2perl@gmail.com>
#-----------------------------------------------------------------
package www::mygrid::org::uk::ontology::superfamily_record;

use OWL::Data::OWL::Class;
use www::mygrid::org::uk::ontology::protein_family_record;


no strict;
use vars qw( @ISA );
@ISA = qw(
 OWL::Data::OWL::Class
 www::mygrid::org::uk::ontology::protein_family_record 
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


use www::mygrid::org::uk::ontology::has_identifier;
use www::mygrid::org::uk::ontology::produced_by;



{
    my %_allowed = (

        has_identifier => {
        	type => 'OWL::Data::OWL::Class',
        	# range checking of classes added 
        	post => sub {
        		my ($self) = shift;
        		return unless $self->strict;
                my $type = @{$self->has_identifier}[-1] if defined $self->has_identifier;
                return unless defined $type and $type->type;
                my $range = new www::mygrid::org::uk::ontology::has_identifier->range();
                return unless $range;
                $range = $self->uri2package($range);
                eval {
                	$range = $range->new();
                };
                return if $@;
        		$self->throw("\n" . $type->type() . "\nis not related to\n" . $range->type()) unless $type->isa(ref($range));
        	},
        	is_array => 1, },

        produced_by => {
        	type => 'OWL::Data::OWL::Class',
        	# range checking of classes added 
        	post => sub {
        		my ($self) = shift;
        		return unless $self->strict;
                my $type = @{$self->produced_by}[-1] if defined $self->produced_by;
                return unless defined $type and $type->type;
                my $range = new www::mygrid::org::uk::ontology::produced_by->range();
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
          or $self->www::mygrid::org::uk::ontology::protein_family_record::_accessible($attr)
    }

    sub _attr_prop {
        my ( $self, $attr_name, $prop_name ) = @_;
        my $attr = $_allowed{$attr_name};
        return ref($attr) ? $attr->{$prop_name} : $attr if $attr;
        return $self->OWL::Data::OWL::Class::_attr_prop( $attr_name, $prop_name ) 
            if $self->OWL::Data::OWL::Class::_accessible($attr_name);
        return $self->www::mygrid::org::uk::ontology::protein_family_record::_attr_prop( $attr_name, $prop_name ) 
            if $self->www::mygrid::org::uk::ontology::protein_family_record::_accessible($attr_name);

    }
}

#-----------------------------------------------------------------
# init
#-----------------------------------------------------------------
sub init {
    my ($self) = shift;
    $self->OWL::Data::OWL::Class::init();
    $self->www::mygrid::org::uk::ontology::protein_family_record::init();
    $self->type('http://www.mygrid.org.uk/ontology#superfamily_record');

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
        $self->www::mygrid::org::uk::ontology::protein_family_record::_get_statements();
    };

    # for each attribute, if is array do:
    if (defined $self->has_identifier){
	    foreach (@{$self->has_identifier}) {
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
	              $subject->new('http://www.mygrid.org.uk/ontology#has_identifier'), 
	              (defined $_->subject ? $_->subject : RDF::Core::Resource->new($_->value))
	          )
	        );
	        $add_type_statement = 1;
	    }
    }
    # for each attribute, if is array do:
    if (defined $self->produced_by){
	    foreach (@{$self->produced_by}) {
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
	              $subject->new('http://www.mygrid.org.uk/ontology#produced_by'), 
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

www::mygrid::org::uk::ontology::superfamily_record - an automatically generated owl class!

=cut

=head1 SYNOPSIS

  use www::mygrid::org::uk::ontology::superfamily_record;
  my $class = www::mygrid::org::uk::ontology::superfamily_record->new();

  # get the uri for this class
  my $uri = $class->uri;
  # add object properties 
  # Make sure to use the appropriate OWL class! 
  # OWL::Data::OWL::Class used as an example
  $class->add_has_identifier(new OWL::Data::OWL::Class('#someURI'));

  # get the has_identifier object properties
  my $has_identifier_objects = $class->has_identifier;
  # Make sure to use the appropriate OWL class! 
  # OWL::Data::OWL::Class used as an example
  $class->add_produced_by(new OWL::Data::OWL::Class('#someURI'));

  # get the produced_by object properties
  my $produced_by_objects = $class->produced_by;

=cut

=head1 DESCRIPTION

I<Inherits from>:
L<OWL::Data::OWL::Class>
L<www::mygrid::org::uk::ontology::protein_family_record>




=cut

