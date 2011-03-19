#!/usr/bin/perl -w
# This is a CGI-BIN script, invoked by a web server when an HTTP
# POST comes in, dispatching requests to the appropriate module
# (SADI web service).
#
# It includes some hard-coded paths - they were added during the
# generate service call.
#
# $Id: service-cgi-async.tt,v 1.24 2010-01-26 20:43:35 ubuntu Exp $
# Contact: Edward Kawas <edward.kawas@gmail.com>
# ---------------------------------------------------------------

#-----------------------------------------------------------------
# Authority:    wilkinsonlab.ca
# Service name: get_sequence_by_location
# Generated:    11-Mar-2011 17:57:05 PST
# Contact: Edward Kawas <edward.kawas@gmail.com>
#-----------------------------------------------------------------

use strict;


# --- during service generation --- 
# leave at the top!
use lib '/home/ben/Programming/perl.workspace.gmod/sadi.gmod/generated';
use lib '/home/ben/Programming/perl.workspace.gmod/sadi.gmod/services';
use lib '/home/ben/Programming/perl.workspace.gmod/sadi.gmod/lib';
use lib '/home/ben/Programming/perl.workspace.gmod';

use CGI;
use CGI::Carp qw(fatalsToBrowser);

use SADI::Service::Instance;
use SADI::RDF::Core;
use SADI::Generators::GenServices;
use SADI::FileStore;

# the suggested wait time for polling in milliseconds
my $SUGGESTED_WAIT_TIME = 0.5*60*1000;

# here we require the service module and add it to ISA hierarchy
use base 'Service::get_sequence_by_location';

# if this is a GET, send the service interface
if ($ENV{REQUEST_METHOD} eq 'GET' or $ENV{REQUEST_METHOD} eq 'HEAD') {
    my $q = new CGI;
    # print the interface unless we are polling
    do {
        # send the signature for this service
        # instantiate a new SADI::RDF::Core object
        my $core = SADI::RDF::Core->new;
        # set the Instance for $core
        $core->Signature(__PACKAGE__->get_service_signature('get_sequence_by_location'));
        print $q->header(-type=>'application/rdf+xml');
        print $core->getServiceInterface();
        exit();
    } unless $q->param('poll');
    
    # we are polling ... 
    # $poll is the id for our file store
    my $poll = $q->param('poll');
    $ENV{SADI_UID} = $poll;
    my $completed;
    eval {$completed = __PACKAGE__->completed($poll);};
    # do something if $@
    print $q->header(-status=>"404 nothing found for the given polling parameter" ) if $@;
    exit if $@;
    if ($completed) {
        # we are done
        eval {$completed = __PACKAGE__->retrieve($poll);};
        unless ($@) {
          print $q->header(-type=>'application/rdf+xml');
          print $completed;
          exit;
        }
    } else {
        # still waiting
        my $signature = __PACKAGE__->get_service_signature('get_sequence_by_location');
        print $q->redirect(-uri=>$signature->URL . "?poll=$poll", -status=>302, -pragma=>"sadi-please-wait = $SUGGESTED_WAIT_TIME");
        #print $q->header(-type=>'application/rdf+xml', -sadi_please_wait=>$SUGGESTED_WAIT_TIME);
        #print __PACKAGE__->get_polling_rdf();
        exit;
    }
} else {
    # call the service

    # get the posted data
    my $data = join "",<STDIN>;

    # set the UID
    my $uid = SADI::FileStore->new(ServiceName => "get_sequence_by_location")->generate_uid();
    $ENV{SADI_UID} = $uid;

    # call the service
    __PACKAGE__->get_sequence_by_location(
       $data
    );
   my $q = new CGI;
   print $q->header(
     -type=>'application/rdf+xml',
     -status=>202,
     -pragma=>"sadi-please-wait = $SUGGESTED_WAIT_TIME"
   );
   print __PACKAGE__->get_polling_rdf();
}

__END__