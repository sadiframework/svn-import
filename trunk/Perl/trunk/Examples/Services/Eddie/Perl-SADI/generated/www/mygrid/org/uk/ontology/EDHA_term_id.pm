#-----------------------------------------------------------------
# www::mygrid::org::uk::ontology::EDHA_term_id
# Generated: 12-Aug-2010 09:34:21 PDT
# Contact: Edward Kawas <edward.kawas+owl2perl@gmail.com>
#-----------------------------------------------------------------
package www::mygrid::org::uk::ontology::EDHA_term_id;

use OWL::Data::OWL::Class;
use www::mygrid::org::uk::ontology::human_anatomy_term_id;


no strict;
use vars qw( @ISA );
@ISA = qw(
 OWL::Data::OWL::Class
 www::mygrid::org::uk::ontology::human_anatomy_term_id 
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


use www::mygrid::org::uk::ontology::is_identifier_of;



{
    my %_allowed = (

        is_identifier_of => {
        	type => 'OWL::Data::OWL::Class',
        	# range checking of classes added 
        	post => sub {
        		my ($self) = shift;
        		return unless $self->strict;
                my $type = @{$self->is_identifier_of}[-1] if defined $self->is_identifier_of;
                return unless defined $type and $type->type;
                my $range = new www::mygrid::org::uk::ontology::is_identifier_of->range();
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
          or $self->www::mygrid::org::uk::ontology::human_anatomy_term_id::_accessible($attr)
    }

    sub _attr_prop {
        my ( $self, $attr_name, $prop_name ) = @_;
        my $attr = $_allowed{$attr_name};
        return ref($attr) ? $attr->{$prop_name} : $attr if $attr;
        return $self->OWL::Data::OWL::Class::_attr_prop( $attr_name, $prop_name ) 
            if $self->OWL::Data::OWL::Class::_accessible($attr_name);
        return $self->www::mygrid::org::uk::ontology::human_anatomy_term_id::_attr_prop( $attr_name, $prop_name ) 
            if $self->www::mygrid::org::uk::ontology::human_anatomy_term_id::_accessible($attr_name);

    }
}

#-----------------------------------------------------------------
# init
#-----------------------------------------------------------------
sub init {
    my ($self) = shift;
    $self->OWL::Data::OWL::Class::init();
    $self->www::mygrid::org::uk::ontology::human_anatomy_term_id::init();
    $self->type('http://www.mygrid.org.uk/ontology#EDHA_term_id');

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
        $self->www::mygrid::org::uk::ontology::human_anatomy_term_id::_get_statements();
    };

    # for each attribute, if is array do:
    if (defined $self->is_identifier_of){
	    foreach (@{$self->is_identifier_of}) {
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
	              $subject->new('http://www.mygrid.org.uk/ontology#is_identifier_of'), 
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

www::mygrid::org::uk::ontology::EDHA_term_id - an automatically generated owl class!

=cut

=head1 SYNOPSIS

  use www::mygrid::org::uk::ontology::EDHA_term_id;
  my $class = www::mygrid::org::uk::ontology::EDHA_term_id->new();

  # get the uri for this class
  my $uri = $class->uri;
  # add object properties 
  # Make sure to use the appropriate OWL class! 
  # OWL::Data::OWL::Class used as an example
  $class->add_is_identifier_of(new OWL::Data::OWL::Class('#someURI'));

  # get the is_identifier_of object properties
  my $is_identifier_of_objects = $class->is_identifier_of;

=cut

=head1 DESCRIPTION

I<Inherits from>:
L<OWL::Data::OWL::Class>
L<www::mygrid::org::uk::ontology::human_anatomy_term_id>




=cut

