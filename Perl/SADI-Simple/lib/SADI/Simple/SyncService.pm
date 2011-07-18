package SADI::Simple::SyncService;

use SADI::Simple::Utils;
use RDF::Trine::Model;
use RDF::Trine::Parser;
use Log::Log4perl;
use Template;

use parent 'SADI::Simple::ServiceBase';

sub handle_cgi_request {

    my $self = shift;

    # if this is a GET, send the service interface
    if ($ENV{REQUEST_METHOD} eq 'GET' or $ENV{REQUEST_METHOD} eq 'HEAD') {

        my $q = new CGI;
        print $q->header(-type=>$self->get_response_content_type());
        print $self->{Signature}->getServiceInterface($self->get_response_content_type());

    } else {

        # get the posted data
        my $data = join "",<STDIN>;

        # call the service
        my $output =  $self->invoke($data);

        # print the results
        my $q = new CGI;
        print $q->header(-type=>$self->get_response_content_type());
        print $output;

    }

}

sub invoke {

    my ($self, $data) = @_;
   
    my $LOG = Log::Log4perl->get_logger(__PACKAGE__);

    Log::Log4perl::NDC->push ($$);
    $LOG->info ('*** REQUEST START *** ' . "\n" . $self->log_request);
    $LOG->debug ("Input raw data (first 1000 characters):\n" . substr ($data, 0, 1000)) if $LOG->is_debug;

    $self->default_throw_with_stack (0);

    my $input_model;
    my $output_model = RDF::Trine::Model->temporary_model;

    # get/parse the incoming RDF
    eval {
#        # transform content type to what RDF::Trine expects
#        my $content_type = $self->get_request_content_type eq 'text/rdf+n3' ? 'application/turtle' : $self->get_request_content_type;
#        my $parser = RDF::Trine::Parser->parser_by_media_type($content_type);
#        $parser->parse_into_model(undef, $data, $input_model);
        $input_model = $self->_build_model($data, $self->get_request_content_type);
    };

    # error in creating parser, or parsing input
    if ($@) {
		# construct an outgoing message
		my $stack = $self->format_stack ($@);
        $self->_add_error_to_model($output_model, $@, 'Error parsing input message for sadi service!', $stack);
        $LOG->error ($stack);
		$LOG->info ('*** FATAL ERROR RESPONSE BACK ***');
		Log::Log4perl::NDC->pop();
        SADI::Simple::Utils->serialize_model($output_model, $self->get_response_content_type);
    }
	
    # do something (this service main task)
    eval { 
    	my @inputs = $self->_get_inputs_from_model($input_model);
    	$self->process_it(\@inputs, $input_model, $output_model);
    };
    # error thrown by the implementation class
    if ($@) {
		my $stack = $self->format_stack ($@);
		$self->_add_error_to_model($output_model, $@, 'Error running sadi service!', $stack);
		$LOG->error ($stack);
		$LOG->info ('*** REQUEST TERMINATED RESPONSE BACK ***');
		Log::Log4perl::NDC->pop();
        SADI::Simple::Utils->serialize_model($output_model, $self->get_response_content_type);
    }

    # return result
    $LOG->info ('*** RESPONSE READY *** ');

    Log::Log4perl::NDC->pop();
    SADI::Simple::Utils->serialize_model($output_model, $self->get_response_content_type);
   
}

1;
