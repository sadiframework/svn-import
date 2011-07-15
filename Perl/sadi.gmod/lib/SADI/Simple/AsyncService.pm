package SADI::Simple::AsyncService;

use SADI::Simple::ServiceDescription;
use SADI::Simple::Utils;

use Log::Log4perl;

use POSIX qw(setsid);
use Data::Dumper;

use RDF::Trine::Model;
use RDF::Trine::Serializer;
use RDF::Trine::Parser;
use RDF::Trine::Node::Resource;
use Template;
use File::Spec;
use File::Spec::Functions qw(catfile splitpath);
use File::Temp qw(tempfile);
use Storable;

use base 'SADI::Simple::ServiceBase';

my $LOG = Log::Log4perl->get_logger(__PACKAGE__);

# in seconds
use constant POLL_INTERVAL => 30;

use constant POLLING_RDF_TEMPLATE => <<TEMPLATE;
<rdf:RDF
     xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
     xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#">
[% FOREACH uri IN inputURIs %]
   <rdf:Description rdf:about="[% uri %]">
     <rdf:type rdf:resource="[% outClass %]"/>
     <rdfs:isDefinedBy rdf:resource="[% url %]"/>
  </rdf:Description>
[% END %]
</rdf:RDF>
TEMPLATE

sub handle_cgi_request
{
    my $self = shift;

    if ($ENV{REQUEST_METHOD} eq 'GET' or $ENV{REQUEST_METHOD} eq 'HEAD') {

        my $q = new CGI;
        
        # print the interface unless we are polling
        do {
            print $q->header(-type=>$self->get_response_content_type());
            print $self->{Signature}->getServiceInterface($self->get_response_content_type());
            exit();
        } unless $q->param('poll');

        # we are polling ... 
        # $poll is the id for our file store
        my $poll = $q->param('poll');
        my $completed;
        eval {$completed = $self->completed($poll);};
        # do something if $@
        print $q->header(-status=>"404 nothing found for the given polling parameter" ) if $@;
        exit if $@;
        if ($completed) {
            # we are done
            eval {$completed = $self->retrieve($poll);};
            unless ($@) {
                print $q->header(-type=>$self->get_response_content_type());
                print $completed;
                exit;
            }
        } else {
            # still waiting
            my $signature = $self->{Signature};
            print $q->redirect(-uri=>$signature->URL . "?poll=$poll", -status=>302, -Retry_After=>POLL_INTERVAL);
            exit;
        }
    } else {
        # call the service

        # get the posted data
        my $data = join "",<STDIN>;

        # call the service
        my ($poll_id, @input_uris) = $self->invoke($data);

        my $q = new CGI;
        print $q->header(
            -type=>$self->get_response_content_type(),
            -status=>202,
            -Retry_After=>POLL_INTERVAL
        );
        print $self->get_polling_rdf($poll_id, @input_uris);
    }

}

#-----------------------------------------------------------------
# store
#   saves the state of our service invocation given a $uid.
# throws exception if there are any problems saving data to disk.
#----------------------------------------------------------------- 

sub store {

    my ($self, $output_model, $is_finished, $poll_id) = @_;

    my %hash;
    $hash{rdfxml} = SADI::Simple::Utils->serialize_model($output_model, 'application/rdf+xml');
    $hash{done} = $is_finished ? 1 : 0;

    my $filename = $self->_poll_id_to_filename($poll_id);
    Storable::store(\%hash, $filename) or $self->throw("unable to store state to $filename");

}

sub _poll_id_to_filename
{
    my ($self, $poll_id) = @_;
    return catfile(File::Spec->tmpdir(), $poll_id);
}

#-----------------------------------------------------------------
# retrieve
#   given a $uid, retrieves the current saved state for our service
#   invocation
# NOTE: if a value is retrieved, then it removed from the cache
#----------------------------------------------------------------- 
sub retrieve {

    my ($self, $poll_id) = @_;

    my $filename = $self->_poll_id_to_filename($poll_id);

    my $hashref = Storable::retrieve($filename) or $self->throw("no data stored for poll_id $poll_id");
    my $rdfxml = $hashref->{rdfxml};

    unlink($filename) or $log->warn("failed to removed tempfile $filename: $!");

    if ($self->get_response_content_type eq 'text/rdf+n3') {
        return SADI::Simple::Utils->rdfxml_to_n3($value);
    }

    return $rdfxml;
}

#-----------------------------------------------------------------
# completed
#   given a $uid, retrieves the current state our service
#   invocation. a Perl true value if completed, 0 | undef otherwise.
# Throws exception if there is nothing to retrieve for the given $uid
#----------------------------------------------------------------- 
sub completed {

    my ($self, $poll_id) = @_;

    my $filename = $self->_poll_id_to_filename($poll_id);
    my $hashref = Storable::retrieve($filename) or $self->throw("no data stored for poll_id $poll_id");
    my $rdfxml = $hashref->{done};

}

#-----------------------------------------------------------------
# [% obj.ServiceName %]
#   the main method; corresponds to the name of this SADI web service
#----------------------------------------------------------------- 

sub invoke {

    my ($self, $data) = @_;

    my $log = Log::Log4perl->get_logger(__PACKAGE__);

    my ($fh, $filename) = tempfile(
                            TEMPLATE => 'sadi-XXXXXXXX', 
                            TMPDIR => 1,
                            UNLINK => 0,  # we do this in retrieve()
                        );

    close($fh) or $self->throw($!);

    my $poll_id = (splitpath($filename))[2];

    Log::Log4perl::NDC->push ($$);

    $log->info ('*** REQUEST START *** ' . "\n" . $self->log_request);
    $log->debug ("Input raw data (first 1000 characters):\n" . substr ($data, 0, 1000)) if ($log->is_debug);
    
    $self->default_throw_with_stack (0);

    my $input_model = RDF::Trine::Model->temporary_model;
    my $output_model = RDF::Trine::Model->temporary_model;

    my @inputs; 
    my @input_uris = ();
    
    # save the input URIs for polling RDF
    {   
        my $parser = RDF::Trine::Parser->parser_by_media_type($self->get_request_content_type);
        $parser->parse_into_model(undef, $data, $input_model);

        @inputs = $self->_get_inputs_from_model($input_model);

        push @input_uris, $_->uri foreach @inputs;
    }
    # error in creating parser, or parsing input
    if ($@) {
		my $stack = $self->format_stack ($@);
        $self->_add_error_to_model($output_model, $@, 'Error parsing input message for sadi service!', $stack);
        $log->error ($stack);
		$log->info ('*** FATAL ERROR RESPONSE BACK ***');
		Log::Log4perl::NDC->pop();
		$self->store($output_model, 1, $poll_id);
		return;
    }

    unless (defined( my $pid = fork() )) {
    } elsif ($pid == 0) {

        # Daemonize 
        open STDIN, "/dev/null";
        open STDOUT, ">/dev/null";
        open STDERR, ">/dev/null";
        setsid;
    
        # child process
        # do something (this service main task)
        eval {
            # save empty output model before we begin
            $self->store($output_model, 0, $poll_id);
            $self->process_it(\@inputs, $input_model, $output_model);
        };
        # error thrown by the implementation class
        if ($@) {
    		my $stack = $self->format_stack ($@);
            $self->_add_error_to_model($output_model, $@, 'Error running sadi service!', $stack);
            $log->error ($stack);
    		$log->info ('*** REQUEST TERMINATED RESPONSE BACK ***');
    		Log::Log4perl::NDC->pop();
    		# signal that we are done
            $self->store($output_model, 1, $poll_id);
    		return ;
        }
        
        # return result
        $log->info ('*** RESPONSE READY *** ');

        Log::Log4perl::NDC->pop();
        $self->store($output_model, 1, $poll_id);

        exit 0;
    } 

    return ($poll_id, @input_uris);
}

sub get_polling_rdf 
{
    my ($self, $poll_id, @input_uris) = @_;

    my $log = Log::Log4perl->get_logger(__PACKAGE__);

    my $tt = Template->new( ABSOLUTE => 1, TRIM => 1 );
    my $input = POLLING_RDF_TEMPLATE;
    my $signature = $self->{Signature};
    my $polling_rdf;

    $tt->process(
                  \$input,
                  {
                     inputURIs     => \@input_uris,
                     outClass      => $signature->OutputClass, 
                     url           => $signature->URL . '?poll=' . $poll_id,
                  },
                  \$polling_rdf
    ) || $log->logdie( $tt->error() );

    if ($self->get_response_content_type() eq 'text/rdf+n3') {
        return SADI::Simple::Utils->rdfxml_to_n3($polling_rdf);
    }

    return $polling_rdf;
}

1;
