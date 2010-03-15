# Before `make install' is performed this script should be runnable with
# `make test'. After `make install' it should work as `perl SADI-OWL-SADIServiceParser.t'

#########################

# change 'tests => 1' to 'tests => last_test_to_print';

#use Test::More tests => 7;
use Test::More qw/no_plan/;

BEGIN {
	use FindBin qw ($Bin);
	use lib "$Bin/../lib/";
	use_ok('SADI::Service::Instance');
	use_ok('SADI::OWL::SADIServiceParser');
}

END {

	# destroy persistent data here
}
#########################

# Insert your test code below, the Test::More module is use()ed here so read
# its man page ( perldoc Test::More ) for help writing this test script.

# test unprimed object
my $parser = SADI::OWL::SADIServiceParser->new();
is( ref $parser,
	"SADI::OWL::SADIServiceParser",
	'is object reference a SADI::OWL::SADIServiceParser?' );

my $array_ref = $parser->getServices( "file:/$Bin/getMolecularInteractions.rdf" );
my @services = @$array_ref;

is( scalar @services, 1, "Did we extract exactly 1 service from the RDF?" );

my $data = pop @services;

# are the fields all set in $data?
can_ok(
		$data,
		(
		   'ServiceName', 'ServiceType',      'InputClass',    'OutputClass',
		   'Description', 'UniqueIdentifier', 'Authority',     'Provider',
		   'ServiceURI',  'URL',              'Authoritative', 'Format',
		   'SignatureURL'
		)
);

# Check that the fields are correct
is( $data->ServiceName, 'getMolecularInteractions', "Check the service name" );
is( $data->ServiceType,
	'http://sadiframework.org/RESOURCES/_______whattoputhere______',
	"Check the service type" );
is( $data->InputClass,
	'http://purl.oclc.org/SADI/LSRN/UniProt_Record',
	"Check the input class URI" );
is(
	$data->OutputClass,
'http://sadiframework.org/ontologies/service_objects.owl#getMolecularInteractions_Output',
	"Check the output class URI"
);
is( $data->Description,
	'gets the BIND interaction ids for a given UniProt protein',
	"Check the service description" );
is(
	$data->UniqueIdentifier,
'urn:lsid:sadiframework.org:serviceinstances:sadiframework.net,getMolecularInteractions',
	"Check the unique id"
);
is( $data->Authority, 'illuminae.com', "Check the authority" );
is( $data->Provider, 'anonymous@sadiframework.org',
	"Check the provider email address" );
is( $data->ServiceURI,
	'http://sadiframework.org/services/getMolecularInteractions',
	"Check the service URI" );
is( $data->URL,
	'http://sadiframework.org/services/getMolecularInteractions',
	"Check the URL" );
is( $data->Authoritative, 0,      "Check the isAuthoritative field" );
is( $data->Format,        'sadi', "Check the format" );
is( $data->SignatureURL,
	'http://foo.bar/myServiceDescription/location.rdf',
	"Check the service description location" );

