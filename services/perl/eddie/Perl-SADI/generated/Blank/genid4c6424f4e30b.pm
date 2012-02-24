#-----------------------------------------------------------------
# Blank::genid4c6424f4e30b
# Generated: 12-Aug-2010 09:44:37 PDT
# Contact: Edward Kawas <edward.kawas+owl2perl@gmail.com>
#-----------------------------------------------------------------
package Blank::genid4c6424f4e30b;

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


use semanticscience::org::resource::SIO_000061;
use semanticscience::org::resource::SIO_000062;
use semanticscience::org::resource::SIO_000219;
use semanticscience::org::resource::SIO_000226;



{
    my %_allowed = (

        SIO_000061 => {
        	type => 'OWL::Data::OWL::Class',
        	# range checking of classes added 
        	post => sub {
        		my ($self) = shift;
        		return unless $self->strict;
                my $type = @{$self->SIO_000061}[-1] if defined $self->SIO_000061;
                return unless defined $type and $type->type;
                my $range = new semanticscience::org::resource::SIO_000061->range();
                return unless $range;
                $range = $self->uri2package($range);
                eval {
                	$range = $range->new();
                };
                return if $@;
        		$self->throw("\n" . $type->type() . "\nis not related to\n" . $range->type()) unless $type->isa(ref($range));
        	},
        	is_array => 1, },

        SIO_000062 => {
        	type => 'OWL::Data::OWL::Class',
        	# range checking of classes added 
        	post => sub {
        		my ($self) = shift;
        		return unless $self->strict;
                my $type = @{$self->SIO_000062}[-1] if defined $self->SIO_000062;
                return unless defined $type and $type->type;
                my $range = new semanticscience::org::resource::SIO_000062->range();
                return unless $range;
                $range = $self->uri2package($range);
                eval {
                	$range = $range->new();
                };
                return if $@;
        		$self->throw("\n" . $type->type() . "\nis not related to\n" . $range->type()) unless $type->isa(ref($range));
        	},
        	is_array => 1, },

        SIO_000219 => {
        	type => 'OWL::Data::OWL::Class',
        	# range checking of classes added 
        	post => sub {
        		my ($self) = shift;
        		return unless $self->strict;
                my $type = @{$self->SIO_000219}[-1] if defined $self->SIO_000219;
                return unless defined $type and $type->type;
                my $range = new semanticscience::org::resource::SIO_000219->range();
                return unless $range;
                $range = $self->uri2package($range);
                eval {
                	$range = $range->new();
                };
                return if $@;
        		$self->throw("\n" . $type->type() . "\nis not related to\n" . $range->type()) unless $type->isa(ref($range));
        	},
        	is_array => 1, },

        SIO_000226 => {
        	type => 'OWL::Data::OWL::Class',
        	# range checking of classes added 
        	post => sub {
        		my ($self) = shift;
        		return unless $self->strict;
                my $type = @{$self->SIO_000226}[-1] if defined $self->SIO_000226;
                return unless defined $type and $type->type;
                my $range = new semanticscience::org::resource::SIO_000226->range();
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
    $self->type('_:genid4c6424f4e30b');

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

    # for each attribute, if is array do:
    if (defined $self->SIO_000061){
	    foreach (@{$self->SIO_000061}) {
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
	              $subject->new('http://semanticscience.org/resource/SIO_000061'), 
	              (defined $_->subject ? $_->subject : RDF::Core::Resource->new($_->value))
	          )
	        );
	        $add_type_statement = 1;
	    }
    }
    # for each attribute, if is array do:
    if (defined $self->SIO_000062){
	    foreach (@{$self->SIO_000062}) {
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
	              $subject->new('http://semanticscience.org/resource/SIO_000062'), 
	              (defined $_->subject ? $_->subject : RDF::Core::Resource->new($_->value))
	          )
	        );
	        $add_type_statement = 1;
	    }
    }
    # for each attribute, if is array do:
    if (defined $self->SIO_000219){
	    foreach (@{$self->SIO_000219}) {
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
	              $subject->new('http://semanticscience.org/resource/SIO_000219'), 
	              (defined $_->subject ? $_->subject : RDF::Core::Resource->new($_->value))
	          )
	        );
	        $add_type_statement = 1;
	    }
    }
    # for each attribute, if is array do:
    if (defined $self->SIO_000226){
	    foreach (@{$self->SIO_000226}) {
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
	              $subject->new('http://semanticscience.org/resource/SIO_000226'), 
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

Blank::genid4c6424f4e30b - an automatically generated owl class!

=cut

=head1 SYNOPSIS

  use Blank::genid4c6424f4e30b;
  my $class = Blank::genid4c6424f4e30b->new();

  # get the uri for this class
  my $uri = $class->uri;
  # add object properties 
  # Make sure to use the appropriate OWL class! 
  # OWL::Data::OWL::Class used as an example
  $class->add_SIO_000061(new OWL::Data::OWL::Class('#someURI'));

  # get the SIO_000061 object properties
  my $SIO_000061_objects = $class->SIO_000061;
  # Make sure to use the appropriate OWL class! 
  # OWL::Data::OWL::Class used as an example
  $class->add_SIO_000062(new OWL::Data::OWL::Class('#someURI'));

  # get the SIO_000062 object properties
  my $SIO_000062_objects = $class->SIO_000062;
  # Make sure to use the appropriate OWL class! 
  # OWL::Data::OWL::Class used as an example
  $class->add_SIO_000219(new OWL::Data::OWL::Class('#someURI'));

  # get the SIO_000219 object properties
  my $SIO_000219_objects = $class->SIO_000219;
  # Make sure to use the appropriate OWL class! 
  # OWL::Data::OWL::Class used as an example
  $class->add_SIO_000226(new OWL::Data::OWL::Class('#someURI'));

  # get the SIO_000226 object properties
  my $SIO_000226_objects = $class->SIO_000226;

=cut

=head1 DESCRIPTION

I<Inherits from>:
L<OWL::Data::OWL::Class>




=cut

