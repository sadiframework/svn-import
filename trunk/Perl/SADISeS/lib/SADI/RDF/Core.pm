#-----------------------------------------------------------------
#  SADI::RDF::Core
# Author: Mark Wilkinson,
#         Edward Kawas
# For copyright and disclaimer see below.
#
# $Id: Core.pm,v 1.19 2010-03-09 17:36:14 ubuntu Exp $
#-----------------------------------------------------------------
package SADI::RDF::Core;
use strict;

use Carp;

use Template;
 
use FindBin qw( $Bin );
use lib $Bin;
use File::Spec;

use RDF::Core::Resource;
use RDF::Core;
use RDF::Core::Model;
use RDF::Core::Storage::Memory;
use RDF::Core::Model::Parser;
use RDF::Core::Model::Serializer;

use SADI::Utils;
use SADI::Service::Instance;
use SADI::Base;
use base ("SADI::Base");

# add versioning to this module
use vars qw /$VERSION/;
$VERSION = sprintf "%d.%02d", q$Revision: 1.19 $ =~ /: (\d+)\.(\d+)/;

=head1 NAME

SADI::RDF::Core - A Perl package for SADI services

=head1 DESCRIPTION

    This is a module that helps service providers for SADI services do most
    of the routine garbage that they need to do to parse and construct
    RDF messages for SADI


=head1 SYNOPSIS

 use SADI::RDF::Core;
 use SADI::Service::Instance;

 my $service = SADI::Service::Instance->new(
     ServiceName => "helloworld",
     ServiceType => "http://someontology.org/services/sometype",
     InputClass => "http://someontology.org/datatypes#Input1",
     OutputClass => "http://someontology.org/datatypes#Output1",
     Description => "the usual hello world service",
     UniqueIdentifier => "urn:lsid:myservices:helloworld",
     Authority => "helloworld.com",
     Provider => 'myaddress@organization.org',
     ServiceURI => "http://helloworld.com/cgi-bin/helloworld.pl",
     URL => "http://helloworld.com/cgi-bin/helloworld.pl",
 );

 # instantiate a new SADI::Service::Core object
 my $core = SADI::RDF::Core->new;

 # set the Instance for $core
 $core->Signature($service);

 # get the Instance for $core
 $service = $core->Signature();

 # get the service signature 
 my $signature = $core->getServiceInterface;

 # parse the incoming RDF
 $core->Prepare($rdf) || $core->throw( "somehow the input data was improperly formed\n" );

 # get the RDF nodes representing the input, based on input class (from 'new')
 my @inputs = $core->getInputNodes();

 # add output nodes
 $core->addOutputData(
		node  => $resource, # type RDF::Core::Resource
		value => "http://view.ncbi.nlm.nih.gov/protein/12408656",
		predicate =>
"http://sadiframework.org/ontologies/predicates.owl#hasInteractingParticipant"
 );

=cut

=head1 METHODS

=cut

=head2 new

 $service = SADI::RDF::Core->new(%args);
 args:
     Signature L<SADI::Service::Instance> - the SADI service instance we are using (can be set later),
     ServicePredicate(URI) - the predicate that the service will add B<requried>,
     ContentType(string)   - what content-type header should we respond with I<optional>

=cut

=head2  ServicePredicate

  $predURI = $service->ServicePredicate($URI)
  get/set the URI of the predicate the service will add to the input data

=cut

{
	my %_allowed = (
		_model                  => { type => 'RDF::Core::Model' },
		_output_model           => { type => 'RDF::Core::Model' },
		_default_request_method => { type => SADI::Base->STRING },
		ContentType             => { type => SADI::Base->STRING },
		Signature               => {
			type => 'SADI::Service::Instance',
			post => sub {
				my $s = shift;
				$s->Signature->ServiceURI = $s->Signature->URL
				  unless $s->Signature->ServiceURI;
				$s->Signature->throw("Needs Input Class")
				  unless $s->Signature->InputClass();
				$s->Signature->throw("Needs Output Class")
				  unless $s->Signature->OutputClass();
				$s->Signature->throw("Needs provider email")
				  unless $s->Signature->Provider();
				$s->Signature->throw("Needs Authority URI")
				  unless $s->Signature->Authority();
				$s->Signature->throw("No Endpoint specified ('URL' init parameter)")
				  unless $s->Signature->URL();
				$s->Signature->throw("No service name specified")
				  unless $s->Signature->ServiceName();
				$s->Signature->throw("No ServiceType specified")
				  unless $s->Signature->InputClass();
				$s->Signature->throw("Needs Description")
				  unless $s->Signature->Description();
				$s->_prepareOutputModel();
			  }
		},
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

	# set the default format for this signature
	$self->ContentType('text/plain');
	$self->_default_request_method('GET');
}

=head2 Prepare

  $service->Prepare()

  Prepare the incoming data and make sure it is at least parsible;  
  Consumes a string of RDF and Returns true if
  the incoming message was parsable, though if it isnt then it'll likely
  crap-out at some point rather than returning false...

=cut

sub Prepare {
	my ($self, $rdf) = @_;
	$self->throw("Error in Prepare: No valid RDF/OWL found in\n$rdf\n!!!!")
	  unless ( $rdf =~ /RDF/ );
	my $storage = new RDF::Core::Storage::Memory;
	my $model = new RDF::Core::Model( Storage => $storage );
	my %options = (
		Model      => $model,
		Source     => $rdf,
		SourceType => 'string',

		#parserOptions
		BaseURI     => "http://www.foo.com/",
		BNodePrefix => "genid"
	);
	my $parser = new RDF::Core::Model::Parser(%options);
	eval {$parser->parse;};
	$self->throw("Error parsing input RDF: $@") if $@;
	$self->_model($model) if $model;
	return 1 if $model;
	return undef;
}

=head2 getInputNodes

 @nodes = $service->getInputNodes(%args)

 get the input passed to the service

 args:
      type => URI  ;  optional
 returns
      an array of RDF::Core::Resource objects

=cut

sub getInputNodes {
	my ( $self, %args ) = @_;
	my $predicate = $args{type} || $self->Signature->InputClass;
	my $model = $self->_model();
	my $type =
	  RDF::Core::Resource->new("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
	my $inputtype = RDF::Core::Resource->new($predicate);

#  we need the input types to be "Input" because a client can honestly send us a more complex type that inherits and it wont be understood without a reasoner

	my $yesno = $model->existsStmt( undef, $type, $inputtype );
	return () unless $yesno;
	my $enumerator = $model->getStmts( undef, $type, $inputtype );
	my @subjects;

	my $statement = $enumerator->getFirst;
	while ( defined $statement ) {
		push @subjects, $statement->getSubject;
		$statement = $enumerator->getNext;
	}
	$enumerator->close;
	return @subjects;
}

=head2 getLiteralPropertyValues

  %values = $service->getLiteralPropertyValues(%args)

  get the value for some property of interest (e.g. from input node(s))

  args
      property =>  $URI  :  the URI of the predicate for which you want a value
      nodes =>  @nodes   :  the list of nodes (e.g. from getInputNodes)
  returns
      hash of {$nodeURI => [$val, $val], ...}

=cut

sub getLiteralPropertyValues {
	my ( $self, %args ) = @_;
	my $model    = $self->_model;
	my $property = $args{property};
	my $nodes    = $args{nodes};
	my @nodes    = @$nodes;
	my %valuehash;    # the output  {$node, \@scalars}
	my $desired_property = RDF::Core::Resource->new($property);

	foreach my $subject (@nodes) {
		my $iterator = $model->getStmts( $subject, $desired_property, undef );
		my $statement = $iterator->getFirst;
		my @values;
		while ( defined $statement ) {
			my $input_object = $statement->getObject;
			my $value;
			if ( ref($input_object) eq "RDF::Core::Literal" ) {
				$value = $input_object->getValue;
				push @values, $value;
			}
			$statement = $iterator->getNext;
		}
		$iterator->close;
		$valuehash{ $subject->getURI } = \@values;
	}
	return %valuehash;
}

=head2 getStatements

  my @statements = $core->getStatements(%args);

  get an array of RDF::Core::Statements given a subject, object, and/or predicate from the input data

  %args
      subject   => the URI of the subject for which you want to retrieve statements for
      object    => the URI of the object for which you want to retrieve statements for
      predicate => the URI of the predicate for which you want to retrieve statements for

  B<subject, object and predicate are all optional.>

  returns
      a reference to an array of RDF::Core::Statements that match the given subject, object and predicate

=cut

sub getStatements {
	my ($self, %args) = @_;
    my $objects;
    my ($subject, $object, $predicate);
    # set up the subject if it is defined
    if (defined $args{subject}) {
    	unless ( UNIVERSAL::isa( $args{subject}, 'RDF::Core::Resource') ) {
            $subject = RDF::Core::Resource->new($args{subject});
        } else {
            $subject = $args{subject};
        }
    }
    # set up the object if it is defined
    if (defined $args{object}) {
        unless ( UNIVERSAL::isa( $args{object}, 'RDF::Core::Resource') ) {
            $object = RDF::Core::Resource->new($args{object});
        } else {
            $object = $args{object};
        }
    }
    # set up the predicate if it is defined
    if (defined $args{predicate}) {
        unless ( UNIVERSAL::isa( $args{predicate}, 'RDF::Core::Resource') ) {
            $predicate = RDF::Core::Resource->new($args{predicate});
        } else {
            $predicate = $args{predicate};
        }
    }
    
    eval {$objects = $self->_model->getStmts($subject, $predicate, $object);};
    if ($@) {
        $self->throw("Error in getStatements: $@");
    }
    my $statements;
    my $e = $objects->getFirst;
    while (defined $e) {
        push @$statements, $e;
        $e = $objects->getNext;
    }
    $objects->close if $objects;
    return $statements;
}

=head2 getObjects

  my @objects = $core->getObjects(%args);

  get an array of RDF::Core::Resource nodes given a subject and predicate from the input data

  %args
      subject   => the URI of the subject for which you want to retrieve objects for
      predicate => the URI of the predicate for which you want to retrieve objects for

  B<subject, object and predicate are all optional.>

  returns
      a reference to an array of RDF::Core::Resource that match the given subject and predicate

=cut

sub getObjects {
	my ($self, %args) = @_;
	my ($subject, $predicate);
    # set up the subject if it is defined
    if (defined $args{subject}) {
        unless ( UNIVERSAL::isa( $args{subject}, 'RDF::Core::Resource') ) {
            $subject = RDF::Core::Resource->new($args{subject});
        } else {
            $subject = $args{subject};
        }
    }
    # set up the predicate if it is defined
    if (defined $args{predicate}) {
        unless ( UNIVERSAL::isa( $args{predicate}, 'RDF::Core::Resource') ) {
            $predicate = RDF::Core::Resource->new($args{predicate});
        } else {
            $predicate = $args{predicate};
        }
    }    
	my $objects;
	eval {$objects = $self->_model->getObjects($subject, $predicate);};
	if ($@) {
		$self->throw("Error in getObjects: $@");
	}
	return $objects;
}

=head2 addOutputData

  $service->addOutputData(%args);

  add an output triple to the model; the predicate of the triple
  is automatically extracted from the ServicePredicate.

  You can pass a URI or an RDF::Core::Resource as the "value" argument.  
  The node is automatically rdf:typed as the OutputClass if you include
  the "typed_as_output" argument as true.

  If you pass a "value" that looks like a URI, then this routine WILL ASSUME
  THAT YOU WANT IT TO BE AN OBJECT, NOT A SCALAR VALUE.  To over-ride this,
  set the boolean "force_literal" argument.  If you pass an RDF::Core::Resource
  together with the force_literal argument, the URI of the RDF::Core::Resource
  will be extracted and added as a literal value rather than as an object.

  args

     node => $URI  (the URI string, RDF::Core::Resource of the subject node or 
             a OWL::Data::OWL::Class (object generated using sadi-generate-datatypes)).
             In the event of an OWL class, all other args are ignored.

     value => $val  (a string value)

     predicate => $URI (required unless node isa OWL::Data::OWL::Class- the predicate to put between them.)

     typed_as_output => boolean (if present output is rdf:typed as output class)

     force_literal => boolean

     label => $label (string); label for value node, only if value is a URI

=cut

sub addOutputData {
	my ( $self, %args ) = @_;
	my $outputmodel = $self->_output_model;
	my $subject     = $args{node};
	if ( ref($subject) =~ /RDF::Core::Resource/ ) {
		$subject = RDF::Core::Resource->new( $subject->getURI );
	} elsif ( UNIVERSAL::isa($subject, 'OWL::Data::OWL::Class') or UNIVERSAL::isa($subject, 'SADI::Data::OWL::Class') ) {
		# using generated modules, so get their statements and return
		foreach ( @{ $subject->_get_statements } ) {
            $self->_addToModel( statement => $_ );
        }
        return;
    } else {
		$subject = RDF::Core::Resource->new($subject);
	}
	my $object         = $args{value};
	my $predicate_sent = $args{predicate};
	my $label          = $args{label};

	if ($predicate_sent) {
		if ( ref($predicate_sent) =~ /RDF::Core/ ) {
			$predicate_sent = $predicate_sent->getURI;
		}    # need to stringify it before proceeding
	}
	my $add_type_data = $args{typed_as_output};
	my $force_literal = $args{force_literal};

	my $predicate =
	  $predicate_sent
	  ? RDF::Core::Resource->new($predicate_sent)
	  : undef;
	  #: RDF::Core::Resource->new( $self->Signature->ServicePredicate );
	$LOG->warn("Cannot completely addOutputData() without a predicate!\nPlease check how you are calling addOutputData() and include a predicate!")
	  unless defined $predicate;
	if (defined $predicate) {
		if ( ref($object) && ( ref($object) =~ /RDF::Core/ ) )
		{        # did they send us an objectt of the right type?
			if ($force_literal)
			{ # did they want the URI of that object as a literal value (very rare, but why not)
				my $URI = $object->getURI;
				$object = RDF::Core::Literal->new($URI);
	
				my $statement = RDF::Core::Statement->new( $subject, $predicate, $object );
				$self->_addToModel( statement => $statement );
			} else {    # they sent an RDF::Core node that we should simply add to the graph
				my $statement = RDF::Core::Statement->new( $subject, $predicate, $object );
				$self->_addToModel( statement => $statement );
				if ($label) {
					$label = RDF::Core::Literal->new($label);
					my $lab = RDF::Core::Resource->new(
											  'http://www.w3.org/2000/01/rdf-schema#label');
					$statement = RDF::Core::Statement->new( $object, $lab, $label );
					$self->_addToModel( statement => $statement );
				}
			}
		} else {    # they sent a literal value... is it a URI-type thing?
			if ( $object =~ /\S+\:\S+\.\S+/ && !$force_literal )
			{ # a terrible regexp for a URI... should find the one that is sanctioned by the W3C URI RFC... look for it later...
				$object = RDF::Core::Resource->new($object);
				my $statement = RDF::Core::Statement->new( $subject, $predicate, $object );
				$self->_addToModel( statement => $statement );
				if ($label) {
					$label = RDF::Core::Literal->new($label);
					my $lab = RDF::Core::Resource->new(
											  'http://www.w3.org/2000/01/rdf-schema#label');
					$statement = RDF::Core::Statement->new( $object, $lab, $label );
					$self->_addToModel( statement => $statement );
				}
			} else {
				$object = RDF::Core::Literal->new($object);
				my $statement = RDF::Core::Statement->new( $subject, $predicate, $object );
				$self->_addToModel( statement => $statement );
			}
		}
	}
	if ($add_type_data) {
		my $output_type = RDF::Core::Resource->new( $self->Signature->OutputClass );
		my $typepredicate =
		  RDF::Core::Resource->new("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		my $typestatement =
		  RDF::Core::Statement->new( $subject, $typepredicate, $output_type );
		$self->_addToModel( statement => $typestatement );
	}
}

=head2 serializeInputModel

  $xml = $service->serializeInputModel()

  if you want access to the raw RDF-XML for the input data, use this method.
  Returns you a string with the raw XML

=cut

sub serializeInputModel {
	my ($self) = @_;
	my $model = $self->_model;
	my $output;
	my $serializer = new RDF::Core::Model::Serializer(
		Model  => $model,
		Output => \$output,

	  #                                                        BaseURI => 'URI://BASE/',
	);
	$serializer->serialize;
	return $output;
}

=head2 serializeOutputModel

  $xml = $service->serializeOutputModel()

  if you want access to the raw RDF-XML for the output data (at any point
  during the construction of the output), use this method.
  Returns you a string with the raw XML

=cut

sub serializeOutputModel {
	my ($self) = @_;
	my $model = $self->_output_model;
	my $output;
	my $serializer = new RDF::Core::Model::Serializer(
		Model  => $model,
		Output => \$output,

	  #                                                        BaseURI => 'URI://BASE/',
	);
	$serializer->serialize;
	return $output;
}

=head2 getServiceInterface

  according to the SADI best-practices, the service URL should return the
  interface document if you call it with GET.  Here we auto-generate that
  document.

  $service->getServiceInterface()

=cut

sub getServiceInterface {
	my ($self) = @_;

	my $name   = $self->Signature->ServiceName();
	my $uri    = $self->Signature->ServiceURI();
	my $type   = $self->Signature->ServiceType();
	my $in     = $self->Signature->InputClass();
	my $output = $self->Signature->OutputClass();
	my $desc   = $self->Signature->Description();
	my $id     = $self->Signature->UniqueIdentifier() || $self->Signature->ServiceURI();
	my $email         = $self->Signature->Provider();
	my $format        = $self->Signature->Format() ;
	my $url           = $self->Signature->URL() ;
	my $authoritative = $self->Signature->Authoritative();
	my $authority     = $self->Signature->Authority() ;
	my $sigURL        = $self->Signature->SignatureURL() || "";
	my @tests         = $self->Signature->UnitTest || ();

	# generate from template
	my $sadi_interface_signature= '';
	my $tt = Template->new( 
	   ABSOLUTE => 1, 
	   TRIM => 1, 
	);
	my $input = File::Spec->rel2abs(
					  SADI::Utils->find_file(
						  $Bin, 'SADI', 'Generators', 'templates', 'service-signature.tt'
					  )
	);

	$tt->process(
				  $input,
				  {
					 name          => $name,
					 uri           => $uri,
					 type          => $type,
					 input         => $in,
					 output        => $output,
					 desc          => $desc,
					 id            => $id,
					 email         => $email,
					 format        => $format,
					 url           => $url,
					 authoritative => $authoritative,
					 authority     => $authority,
					 sigURL        => $sigURL,
					 tests         => @tests,
				  },
				  \$sadi_interface_signature
	) || $LOG->logdie( $tt->error() );

	return $sadi_interface_signature;
}


sub _add_error {
    my ($self, $msg, $comment, $stack) = @_;

    # generate from template
    my $error_rdf = '';
    my $tt = Template->new( ABSOLUTE => 1, TRIM => 1 );
    my $input = File::Spec->rel2abs(
                      SADI::Utils->find_file(
                          $Bin, 'SADI', 'Generators', 'templates', 'service-error.tt'
                      )
    );
    $msg ||= '';
    $comment ||= '';
    $stack ||= '';
    
    use CGI;
    $tt->process(
                  $input,
                  {
                     message  => CGI::escapeHTML($msg),
                     comment  => CGI::escapeHTML($comment),
                     stack    => CGI::escapeHTML($stack),
                  },
                  \$error_rdf
    ) || $LOG->logdie( $tt->error() );
    # if problem generating error doc, return
    return unless defined ($error_rdf);
    return if $error_rdf eq '';

    # parse the error doc now
	my $storage = new RDF::Core::Storage::Memory;
	my $model = new RDF::Core::Model( Storage => $storage );
	my %options = (
	                Model      => $model,
	                Source     => $error_rdf,
	                SourceType => 'string',
	);
	my $parser = new RDF::Core::Model::Parser(%options);
	$parser->parse;
	my $enumerator = $model->getStmts;
	my $statement  = $enumerator->getFirst;
	# add statement to our output model
	while ( defined $statement ) {
	    $self->_addToModel(statement=>$statement);
	    $statement = $enumerator->getNext;
	}
	$enumerator->close;
	# done;
	return;
}


sub _prepareOutputModel {
	my ($self) = @_;
	my $storage = new RDF::Core::Storage::Memory;
	my $model = new RDF::Core::Model( Storage => $storage );
	my %options = (
					Model       => $model,
					BNodePrefix => "genid"
	);
	$self->_output_model($model);
}

sub _addToModel {
	my ( $self, %args ) = @_;
	my $statement = $args{statement};
	my $model     = $self->_output_model();
	$model->addStmt($statement);
}

1;

