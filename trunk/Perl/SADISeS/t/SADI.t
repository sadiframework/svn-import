# Before `make install' is performed this script should be runnable with
# `make test'. After `make install' it should work as `perl SADI.t'
#########################
# change 'tests => 1' to 'tests => last_test_to_print';
#use Test::More tests => 7;
use Test::More qw/no_plan/;
use strict;

BEGIN {
	
}

END {
	# destroy persistent data here (if any)
}
#########################
# Insert your test code below, the Test::More module is use()ed here so read
# its man page ( perldoc Test::More ) for help writing this test script.

use_ok('SADI::Utils');
use_ok('SADI::Service::ServiceBase');
use_ok('SADI::Service::Instance');

use_ok('SADI::RDF::Core');
use_ok('SADI::RDF::Predicates::DC_PROTEGE');
use_ok('SADI::RDF::Predicates::FETA');
use_ok('SADI::RDF::Predicates::OMG_LSID');
use_ok('SADI::RDF::Predicates::OWL');
use_ok('SADI::RDF::Predicates::RDF');
use_ok('SADI::RDF::Predicates::RDFS');
