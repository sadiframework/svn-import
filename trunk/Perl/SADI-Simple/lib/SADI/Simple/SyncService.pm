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

__END__

=head1 NAME

SADI::Simple::SyncService - a superclass for all synchronous SADI services

=head1 SYNOPSIS

 use base qw( SADI::Simple::SyncService )

=head1 DESCRIPTION

A common superclass for all SADI::Simple services.

=head1 SUBROUTINES

=head2 process_it

A job-level processing: B<This is the main method to be overriden by a
service provider!>. Here all the business logic belongs to.

This method is called once for each service invocation request.

Note that here, in C<SADI::Simple::SyncService>, this method does
nothing. Which means it leaves the output job empty, as it was given
here. Consequence is that if you do not override this method in a 
sub-class, the client will get back an empty request. Which may be 
good just for testing but not really what a client expects (I guess).

You are free to throw an exception (TBD: example here). However, if
you do so the complete processing of the whole client request is
considered failed. After such exception the client will not get any
data back (only an error message).

=head1 AUTHORS, COPYRIGHT, DISCLAIMER

 Ben Vandervalk (ben.vvalk [at] gmail [dot] com)
 Edward Kawas  (edward.kawas [at] gmail [dot] com)

Copyright (c) 2009 Edward Kawas. All Rights Reserved.

This module is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.

This software is provided "as is" without warranty of any kind.

=cut


