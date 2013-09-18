package SADI::Simple::OutputModel;

use SADI::Simple::Utils;
use RDF::Trine::Model 0.135;
use RDF::Trine::Parser 0.135;
use RDF::Trine::Node::Resource;
use Log::Log4perl;
use Template;
use Encode;
use Digest::MD5 qw(md5 md5_hex md5_base64);

use constant RDF_TYPE_URI => 'http://www.w3.org/1999/02/22-rdf-syntax-ns#type';

use base 'RDF::Trine::Model';

sub _init {
	my $self = shift;
	my $service_base = shift;   # this is awful!  Need to refactor the code properly at some point
	$self->{'service_base'} = $service_base;
	$self->{'invocation_timestamp'} = time;
	my $root_uri = $self->{'service_base'}->{Signature}->URL;
	$root_uri =~ s/\/$//;  # REMOVE TRAILING SLASH IF IT EXISTS
	
	my $named_graph = $root_uri."/provenance/".$self->{'invocation_timestamp'};
	$self->{'provenance_named_graph'} = $named_graph;   # http://my.service.org/servicename/provenance/1238758
	
	my $assertion_root = $root_uri."/assertion/";
	$self->{'assertion_statement_root'} = $assertion_root;   # http://my.service.org/servicename/assertion/
	
	my $nanopublication_root = $root_uri."/nanopublication/";  
	$self->{'nanopublication_root'} = $nanopublication_root;  # http://my.service.org/servicename/nanopublication/
	
	
}

sub add_statement {

    my ($self, $statement, $context_info) = @_;  # context info is the $input  RDF::Trine::Resource currently being analyzed.  Provides the basis of a unique context URI for the quads
    #print STDERR "in add statement with self of type ", ref$self, "  statement $statement  stmpred ", $statement->predicate, "  input $context_info  \n";

    																							# but the same for all assertions of that input
	if (($context_info) && (ref($context_info) =~ /trine/i)){
		#print STDERR "recognized that it is a quad\n\n";
		my $inputURIstring = $context_info->as_string();
		# unless ($self->{'invocation_timestamp'}){$self->{'invocation_timestamp'}=time}   # do your best here!  This isn't what we want, though...
		my $AssertionContextID = md5_base64($inputURIstring . $self->{'invocation_timestamp'});   # a hash of the input and timestamp of service invocation
    	my $assertion_context = $self->{'assertion_statement_root'} . $AssertionContextID;          # this will be different for every input
		
		my $sub = $statement->subject; 
		my $pred = $statement->predicate; 
		my $obj = $statement->object;
		my $context = RDF::Trine::Node::Resource->new($assertion_context);
		my $named_statement = RDF::Trine::Statement::Quad->new($sub, $pred, $obj, $context);	# subj  pred  obj   stm_in_assert:AHhfj847hKHJRF	
		RDF::Trine::Model::add_statement($self, $named_statement)
	} else {
		RDF::Trine::Model::add_statement($self, $statement);
	}
	#print STDERR "STATEMENT ADDED\n\n";

}

sub nanopublish_result_for {
	my ($self, $input) = @_;  # self is the output model RDF::Trine::Model
	return unless ($self->{'service_base'}->get_response_content_type =~ /quads/i);  # only do this if requesting quads
	return unless ($self->{'service_base'}->{'Signature'}->NanoPublisher);
    # nanopub:AHhfj847hKHJRF  np:hasAassertion assert:AHhfj847hKHJRF
    # nanopub:AHhfj847hKHJRF  np:hasProvenance $self->{'provenance_named_graph'}   (this is the same for all different nanopubs in a multiplexed invocation of the service)																						  
	# nanopub:AHhfj847hKHJRF  rdf:type  np:Nanopublication
	# assert:AHhfj847hKHJRF   rdf:type  np:Assertion

	my $np = "http://www.nanopub.org/nschema#";
	my $NanopublicationType =  RDF::Trine::Node::Resource->new($np."Nanopublication");
	my $AssertionType =  RDF::Trine::Node::Resource->new($np."Assertion");
	my $ProvenanceType =  RDF::Trine::Node::Resource->new($np."Provenance");
	my $rdfType = RDF::Trine::Node::Resource->new(RDF_TYPE_URI);


	my $inputURIstring = $input->as_string();
	my $AssertionContextID = md5_base64($inputURIstring . $self->{'invocation_timestamp'});   # a hash of the input and timestamp of service invocation
    my $AssertionContextURI = $self->{'assertion_statement_root'} . $AssertionContextID;      # this will be different for every input
    																						  # but the same for all assertions of that input
    																				# http://my.service.org/servicename/assertion/AHhfj847hKHJRF
								  
	my $NanopublicationID = md5_base64($inputURIstring . $self->{'invocation_timestamp'});   
    my $NanopublicationURI = $self->{'nanopublication_root'} . $NanopublicationID;   # http://my.service.org/servicename/nanopublication/AHhfj847hKHJRF   																						 
	
	my $Nanopub = RDF::Trine::Node::Resource->new($NanopublicationURI);
	my $hasAssertion = RDF::Trine::Node::Resource->new($np."hasAssertion");
	my $Assertion =  RDF::Trine::Node::Resource->new($AssertionContextURI);
	my $np_hasAssertion_Assertion = RDF::Trine::Statement->new($Nanopub, $hasAssertion, $Assertion);
	$self->add_statement($np_hasAssertion_Assertion); 
	
	my $provenance_named_graph = RDF::Trine::Node::Resource->new($self->{'provenance_named_graph'});	
	my $hasProvenance = RDF::Trine::Node::Resource->new($np."hasProvenance");
	my $np_hasProvenance_Provenance = RDF::Trine::Statement->new($Nanopub, $hasProvenance, $provenance_named_graph);
	$self->add_statement($np_hasProvenance_Provenance); 

	$self->add_statement(RDF::Trine::Statement->new($Nanopub, $rdfType, $NanopublicationType));
	$self->add_statement(RDF::Trine::Statement->new($Assertion, $rdfType, $AssertionType));
	$self->add_statement(RDF::Trine::Statement->new($provenance_named_graph, $rdfType, $ProvenanceType));
	
    $self->_add_provenance($Nanopub, $provenance_named_graph);
    
}


sub _add_provenance {
	my ($self, $Nanopub, $provenance_named_graph) = @_;
	use DateTime;
#	   name - the name of the SADI service
#      uri  - the service uri
#      type - the service type
#      input - the input class URI
#      output - the output class URI
#      desc - a description for this service
#      id - a unique identifier (LSID, etc)
#      email - the providers email address
#      format - the category of service (sadi)
#      nanopublisher - can the service publish nquads for nanopubs?
#      url - the service url
#      authoritative - whether or not the service is authoritative
#      authority - the service authority URI
#      sigURL - the url to the service signature
	my $signature = $self->{'service_base'}->{Signature};

	# remember this is adding quads!
	my $Identifier = RDF::Trine::Node::Literal->new($signature->ServiceURI,"","http://www.w3.org/2001/XMLSchema#string" );  # this is a URI, so I need to cast it as a string before sending it into the statement, otherwise it will be cast as a resource
	$self->_add_statement($Nanopub, "http://purl.org/dc/terms/created", [DateTime->now,"", "http://www.w3.org/2001/XMLSchema#date",""], $provenance_named_graph);
	$self->_add_statement($Nanopub, "http://purl.org/dc/terms/creator", [$signature->Authority, "", "http://www.w3.org/2001/XMLSchema#string",], $provenance_named_graph) if $signature->Authority ;
	$self->_add_statement($Nanopub, "http://purl.org/dc/elements/1.1/coverage", ["Output from SADI Service", "lang:en"], $provenance_named_graph);
	$self->_add_statement($Nanopub, "http://purl.org/dc/elements/1.1/description", ["Service Description: ".$signature->Description, "lang:en"], $provenance_named_graph) if $signature->Description;
	$self->_add_statement($Nanopub, "http://purl.org/dc/elements/1.1/identifier", [$Identifier], $provenance_named_graph) if $signature->ServiceURI;
	$self->_add_statement($Nanopub, "http://purl.org/dc/elements/1.1/publisher", [$signature->Authority,"", "http://www.w3.org/2001/XMLSchema#string" ], $provenance_named_graph) if $signature->Authority;
	$self->_add_statement($Nanopub, "http://purl.org/dc/elements/1.1/source", [$signature->URL],  $provenance_named_graph) if $signature->URL;
	$self->_add_statement($Nanopub, "http://purl.org/dc/elements/1.1/title", [$signature->ServiceName, "","http://www.w3.org/2001/XMLSchema#string"], $provenance_named_graph) if $signature->ServiceName;
	
}

sub _add_statement {
	my ($self, $s, $p, $o, $c) = @_;
	my ($obj, $lang, $datatype, $canonicalflag) = @$o;
	unless (ref($s) =~ /trine/i){
		$s = RDF::Trine::Node::Resource->new($s);
	}
	unless (ref($p) =~ /trine/i){
		$p = RDF::Trine::Node::Resource->new($p);
	}
	if (ref($obj) =~ /trine/i){
		$o = $obj;
	} else {
		if ($obj =~ /http:\/\//i){
			$o = RDF::Trine::Node::Resource->new($obj);		
		} else {
			$o = RDF::Trine::Node::Literal->new($obj, $lang, $datatype, $canonicalflag);					
		}
	}
	if ($c){
		unless (ref($c) =~ /trine/i){
			$c = RDF::Trine::Node::Resource->new($c);
		}
	}
	if ($c){
		my $stm = RDF::Trine::Statement::Quad->new($s, $p, $o, $c);
		$self->add_statement($stm);
	}  else {
		my $stm = RDF::Trine::Statement->new($s, $p, $o);
		$self->add_statement($stm);
	}

}
1;

__END__

=head1 NAME

SADI::Simple::OutputModel - a light wrapper around RDF::Trine::Model that simplifies NanoPublications in SADI

=head1 SYNOPSIS

 my $output_model = SADI::Simple::OutputModel->new();
 $output_model->_init( $output_model->_init($implements_ServiceBase);))
 

=head1 DESCRIPTION

 There are various things that an output model can do to itself to 
 automatically generate NanoPublications.  This object wraps RDF::Trine::Model
 exposing its API, but adding these new functionalities.
 
 All of the functionalities for NanoPublishers will be ignored if present
 in a service that does not NanoPublish, so feel free to include them
 in your service to avoid having to update the service later when you
 decide that NanoPublishing is pretty cool...

=head1 SUBROUTINES
 
=head2 add_statement

 $output_model->add_statement( $statement, [$input]);
 
 Allows you to add the context for a NanoPubs Assertion named graph.
 Second parameter is optional, and can be present but undef in a service
 that does not publish nanopubs, but might be a nanopublisher one day.
 The $input is the RDF::Trine::Node::Resource that is currently 
 being analyzed in the service "process it" loop.


=head2 nanopublish_result_for 

 $output_model->nanopublish_result_for($input);
 
 in the context of the "process it" loop of a SADI service, this method
 should be invoked at the end of the loop to complete the
 NanoPublication of the output from the given $input.
 
 If it is present in a non-NanoPublishing service, it will be ignored.

=cut


