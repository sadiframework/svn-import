package SADI::Service::Core;
use strict;
use Carp;
use vars qw($AUTOLOAD @ISA);
use RDF::Simple::Parser;
use RDF::Core::Resource;
use RDF::Trine;
use RDF::Core;
use RDF::Core::Model;
use RDF::Core::Storage::Memory;
use RDF::Core::Model::Parser;
use RDF::Core::Model::Serializer;

{

	# Encapsulated:
	# DATA
	#___________________________________________________________
	#ATTRIBUTES
	my %_attr_data =    #     				DEFAULT    	ACCESSIBILITY
	  (
                ServiceName     => [ undef, 'read/write' ],
                ServiceURI     => [ undef, 'read/write' ],
                ServiceType     => [ undef, 'read/write' ],
                InputClass     => [ undef, 'read/write' ],
                OutputClass     => [ undef, 'read/write' ],
                Description     => [ undef, 'read/write' ],
                UniqueIdentifier     => [ undef, 'read/write' ],
                ContentType    => ["text/plain", 'red/write'],
                Provider        => ["anonymous\@sadiframework.org", 'read/write'],
                Format          => ["sadi", 'read/write'],
                URL             => [undef, 'read/write'],
                Authoritative   => ['false', 'read/write'],
                Authority       => [undef, 'read/write'],
                ServicePredicate    => [undef, 'read/write'],
                _model          => [undef, 'read/write'],
                _output_model  => [undef, 'read/write'],
                _default_request_method => ["GET", 'read/write'],
                
		
	  );

	#_____________________________________________________________
	# METHODS, to operate on encapsulated class data
	# Is a specified object attribute accessible in a given mode
	sub _accessible {
		my ( $self, $attr, $mode ) = @_;
		$_attr_data{$attr}[1] =~ /$mode/;
	}

	# Classwide default value for a specified object attribute
	sub _default_for {
		my ( $self, $attr ) = @_;
		$_attr_data{$attr}[0];
	}

	# List of names of all specified object attributes
	sub _standard_keys {
		keys %_attr_data;
	}
}

sub new {
	my ( $caller, %args ) = @_;
	my $caller_is_obj = ref( $caller );
	return $caller if $caller_is_obj;
	my $class = $caller_is_obj || $caller;
	my $self = bless {}, $class;
	foreach my $attrname ( $self->_standard_keys ) {
		if ( exists $args{$attrname} ) {
			$self->{$attrname} = $args{$attrname};
		} elsif ( $caller_is_obj ) {
			$self->{$attrname} = $caller->{$attrname};
		} else {
			$self->{$attrname} = $self->_default_for( $attrname );
		}
	}
        $self->_prepareOutputModel();
	return $self;
}

sub Prepare {
    my ($self) = @_;
    my $rdf = join "",<>;
    die unless ($rdf =~ /RDF/);
    my $storage = new RDF::Core::Storage::Memory;
    my $model = new RDF::Core::Model (Storage => $storage);
    my %options = (Model => $model,
                 Source => $rdf,
                 SourceType => 'string',
                 #parserOptions
                 BaseURI => "http://www.foo.com/",
                 BNodePrefix => "genid"
                );
    my $parser = new RDF::Core::Model::Parser(%options);
    $parser->parse;
    $self->_model($model);
    return 1;
}

sub addOutputData {
    my ($self, %args) = @_;
    my $outputmodel = $self->_output_model;
    my $nodename = $args{node};
    my $node = RDF::Core::Resource->new($nodename);
    my $value = $args{value};
    my $predicate = RDF::Core::Resource->new($self->ServicePredicate);
    my $type = RDF::Core::Resource->new($self->OutputClass);
    my $typepredicate = RDF::Core::Resource->new("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");

    my $object = RDF::Core::Literal->new($value);
    my $statement = RDF::Core::Statement->new($node, $predicate, $object);
    my $typestatement = RDF::Core::Statement->new($node, $typepredicate, $type);
    $self->_addToModel(statement => $statement);
    $self->_addToModel(statement => $typestatement);
}


sub getInputNodes {
    my ($self, %args) = @_;
    my $predicate = $args{type};
    my $model = $self->_model();
    my $type = RDF::Core::Resource->new("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
    my $inputtype = RDF::Core::Resource->new($predicate);
    #  we need the inpout types to be "Input" because a client can honestly send us a more complex type that inherits and it wpont be understood without a reasoner
    
    my $yesno = $model->existsStmt(undef, $type, $inputtype);
    return () unless $yesno;
    my $enumerator = $model->getStmts(undef, $type, $inputtype);
    my @subjects;

    my $statement = $enumerator->getFirst;
    while (defined $statement) {
        push @subjects, $statement->getSubject;
        $statement = $enumerator->getNext
    }
    $enumerator->close;
    return @subjects;
}

sub getScalarValues {
    my ($self, %args) = @_;
    my $model = $self->_model;
    my $property = $args{property};
    my $nodes = $args{nodes};
    my @nodes = @$nodes;
    my %valuehash;  # the output  {$node, \@scalars}
    my $desired_property = RDF::Core::Resource->new($property);

    foreach my $subject (@nodes){
        my $iterator = $model->getStmts($subject, $desired_property, undef);
        my $statement = $iterator->getFirst;
        my @values;
        while (defined $statement) {
            my $input_object = $statement->getObject;
            my $value; 
            if (ref($input_object) eq "RDF::Core::Literal"){
                $value = $input_object->getValue;        
                push @values, $value;
            }
            $statement = $iterator->getNext
        }
        $iterator->close;
        $valuehash{$subject->getURI} = \@values;
    }
    return %valuehash;
}



sub SerializeInputModel {
    my ($self) = @_;
    my $model = $self->_model;
    my $output;
    my $serializer = new RDF::Core::Model::Serializer(
                                                        Model=>$model,
                                                        Output=>\$output,
#                                                        BaseURI => 'URI://BASE/',
                                                       );
    $serializer->serialize;
    return $output;
}

sub SerializeOutputModel {
    my ($self) = @_;
    my $model = $self->_output_model;
    my $output;
    my $serializer = new RDF::Core::Model::Serializer(
                                                        Model=>$model,
                                                        Output=>\$output,
#                                                        BaseURI => 'URI://BASE/',
                                                       );
    $serializer->serialize;
    return $output;
}

sub Respond {
    my ($self) = @_;
    print "Content-Type: ",$self->ContentType,"; charset=ISO-8859-1;\n\n\n"; 
    print "\n\n";
    print $self->SerializeOutputModel();
}


sub handleGET {
    my ($self) = @_;
    
    my $name = $self->ServiceName();
    my $uri = $self->ServiceURI();
    my $type = $self->ServiceType();
    my $input = $self->InputClass();
    my $output = $self->OutputClass();
    my $desc = $self->Description();
    my $id = $self->UniqueIdentifier() || $self->ServiceURI();
    my $contenttype= $self->ContentType();
    my $email = $self->Provider();
    my $format = $self->Format();
    my $URL = $self->URL();
    my $authoritative = $self->Authoritative();
    my $authority = $self->Authority();
    my $nodeid1 = $authority.$name."aaa";
    my $nodeid2 = $authority.$name."bbb";
    my $nodeid3 = $authority.$name."ccc";
    my $nodeid4 = $authority.$name."ddd";
    my $nodeid5 = $authority.$name."eee";
    my $nodeid6 = $authority.$name."fff";
    my $nodeid7 = $authority.$name."ggg";

    
    my $sadi_interface_signature = qq{<?xml version="1.0" encoding="UTF-8"?>
    <rdf:RDF
     xmlns="http://www.w3.org/2002/07/owl#"
     xmlns:a="http://www.mygrid.org.uk/mygrid-moby-service#"
     xmlns:b="http://protege.stanford.edu/plugins/owl/dc/protege-dc.owl#" 
     xml:base="http://bioinfo.icapture.ubc.ca/SADI"
     xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
     xmlns:databases="http://sadiframework.org/ontologies/Databases.owl#"
     xmlns:misc="http://sadiframework.org/ontologies/miscellaneousObjects.owl#"
     xmlns:owl="http://www.w3.org/2002/07/owl#"
     xmlns:xsd="http://www.w3.org/2001/XMLSchema#"
     xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">    
<rdf:Description rdf:about="$uri">
    <rdf:type rdf:resource="http://www.mygrid.org.uk/mygrid-moby-service#serviceDescription"/>
    <b:format>$format</b:format>
    <b:identifier>$id</b:identifier>
    <a:locationURI>$URL</a:locationURI>
    <a:hasServiceDescriptionText>$desc</a:hasServiceDescriptionText>
    <a:hasServiceDescriptionLocation/>
    <a:hasServiceNameText>$name</a:hasServiceNameText>
    <a:providedBy>
        <rdf:Description rdf:about="$nodeid1">
            <a:authoritative>$authoritative</a:authoritative>
            <b:creator>$email</b:creator>
            <b:publisher>$authority</b:publisher>
            <rdf:type rdf:resource="http://www.mygrid.org.uk/mygrid-moby-service#organisation"/>
        </rdf:Description>
    </a:providedBy>
    <a:hasOperation>
        <rdf:Description rdf:about="$nodeid2">
            <a:hasOperationNameText>$name</a:hasOperationNameText>
            <rdf:type rdf:resource="http://www.mygrid.org.uk/mygrid-moby-service#operation"/>
            <a:performsTask>
                <rdf:Description rdf:about="$nodeid3">
                    <rdf:type rdf:resource="http://www.mygrid.org.uk/mygrid-moby-service#operationTask"/>

                    <rdf:type rdf:resource="http://sadiframework.org/RESOURCES/_______whattoputhere______"/>

                </rdf:Description>
            </a:performsTask>

            <a:inputParameter>
                <rdf:Description rdf:about="$nodeid4">
                    <rdf:type rdf:resource="http://www.mygrid.org.uk/mygrid-moby-service#parameter"/>

                    <a:objectType>
                        <rdf:Description rdf:about="$nodeid5">
                            <rdf:type rdf:resource="$input"/>
                        </rdf:Description>
                    </a:objectType>
                </rdf:Description>
            </a:inputParameter>

            <a:outputParameter>
                <rdf:Description rdf:about="$nodeid6">
                    <rdf:type rdf:resource="http://www.mygrid.org.uk/mygrid-moby-service#parameter"/>

                    <a:objectType>
                        <rdf:Description rdf:about="$nodeid7">
                            <rdf:type rdf:resource="$output"/>
                        </rdf:Description>
                    </a:objectType>
                </rdf:Description>
            </a:outputParameter>
        </rdf:Description>
    </a:hasOperation>
</rdf:Description>
</rdf:RDF>
};

    my $method = $ENV{REQUEST_METHOD} || $self->_default_request_method();
    if ($method eq "GET"){
        print "Content-Type: $contenttype; charset=ISO-8859-1;\n\n";
        print $sadi_interface_signature;
        exit;
    }
    #die unless $ENV{CONTENT_LENGTH} < 20;
}


sub _prepareOutputModel {
    my ($self) = @_;
    my $storage = new RDF::Core::Storage::Memory;
    my $model = new RDF::Core::Model (Storage => $storage);
    my %options = (Model => $model,
                 BNodePrefix => "genid"
                );
    $self->_output_model($model);
}

sub _addToModel {
    my ($self, %args) = @_;
    my $statement = $args{statement};
    my $model = $self->_output_model();
    $model->addStmt($statement);
}

sub AUTOLOAD {
	no strict "refs";
	my ( $self, $newval ) = @_;
	$AUTOLOAD =~ /.*::(\w+)/;
	my $attr = $1;
	if ( $self->_accessible( $attr, 'write' ) ) {
		*{$AUTOLOAD} = sub {
			if ( defined $_[1] ) { $_[0]->{$attr} = $_[1] }
			return $_[0]->{$attr};
		};    ### end of created subroutine
###  this is called first time only
		if ( defined $newval ) {
			$self->{$attr} = $newval;
		}
		return $self->{$attr};
	} elsif ( $self->_accessible( $attr, 'read' ) ) {
		*{$AUTOLOAD} = sub {
			return $_[0]->{$attr};
		};    ### end of created subroutine
		return $self->{$attr};
	}

	# Must have been a mistake then...
	croak "No such method: $AUTOLOAD";
}
sub DESTROY { }
1;