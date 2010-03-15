# Before `make install' is performed this script should be runnable with
# `make test'. After `make install' it should work as `perl SADI-Service-Instance.t'

#########################

# change 'tests => 1' to 'tests => last_test_to_print';

#use Test::More tests => 7;
use Test::More qw/no_plan/;

BEGIN {
	use FindBin qw ($Bin);
	use lib "$Bin/../lib";
	use_ok('SADI::Service::Instance');
}

END {

	# destroy persistent data here
}
#########################

# Insert your test code below, the Test::More module is use()ed here so read
# its man page ( perldoc Test::More ) for help writing this test script.

# test unprimed object
my $data = SADI::Service::Instance->new();
is( ref $data, "SADI::Service::Instance",
	'is object reference a SADI::Service::Instance?' );

# check that the fields are all empty (except the Format field)
is( $data->ServiceName,      undef, "check that for empty field for service name" );
is( $data->ServiceType,      undef, "check that for empty field for service type" );
is( $data->InputClass,       undef, "check that for empty field for input class" );
is( $data->OutputClass,      undef, "check that for empty field for output class" );
is( $data->Description,      undef, "check that for empty field for description" );
is( $data->UniqueIdentifier, undef, "check that for empty field for unique id" );
is( $data->Authority,        undef, "check that for empty field for authority" );
is( $data->Provider,         undef, "check that for empty field for provider address" );
is( $data->ServiceURI,       undef, "check that for empty field for service uri" );
is( $data->URL,              undef, "check that for empty field for url" );
is( $data->Authoritative,    1, "check that for default field for authoritative" );
is( $data->Format,       'sadi', "check that Format is set to default 'sadi'" );
is( $data->SignatureURL, undef,  "check that for empty field for signature url" );

# create a new primed SADI service instance object
$data = SADI::Service::Instance->new(
							ServiceName => "helloworld",
							ServiceType => "http://someontology.org/services/sometype",
							InputClass  => "http://someontology.org/datatypes#Input1",
							OutputClass => "http://someontology.org/datatypes#Output1",
							Description => "the usual hello world service",
							UniqueIdentifier => "urn:lsid:myservices:helloworld",
							Authority        => "helloworld.com",
							Provider         => 'myaddress@organization.org',
							ServiceURI => "http://helloworld.com/cgi-bin/helloworld.pl",
							URL        => "http://helloworld.com/cgi-bin/helloworld.pl",
							Authoritative => 1,
							SignatureURL  => "http://helloworld.com/myLocation"
);
can_ok(
		$data,
		(
		   'ServiceName', 'ServiceType',      'InputClass',    'OutputClass',
		   'Description', 'UniqueIdentifier', 'Authority',     'Provider',
		   'ServiceURI',  'URL',              'Authoritative', 'Format',
		   'SignatureURL'
		)
);

# check that the fields are as specified above
is( $data->ServiceName,      'helloworld' );
is( $data->ServiceType,      'http://someontology.org/services/sometype' );
is( $data->InputClass,       'http://someontology.org/datatypes#Input1' );
is( $data->OutputClass,      'http://someontology.org/datatypes#Output1' );
is( $data->Description,      'the usual hello world service' );
is( $data->UniqueIdentifier, 'urn:lsid:myservices:helloworld' );
is( $data->Authority,        'helloworld.com' );
is( $data->Provider,         'myaddress@organization.org' );
is( $data->ServiceURI,       'http://helloworld.com/cgi-bin/helloworld.pl' );
is( $data->URL,              'http://helloworld.com/cgi-bin/helloworld.pl' );
is( $data->Authoritative,    1 );
is( $data->Format,           'sadi' );
is( $data->SignatureURL,     'http://helloworld.com/myLocation' );

# check the post processing of the Provider field
eval { $data->Provider('foo&@bar.ubc.ca'); };
like( $@,
	  qr/Invalid email address specified/,
	  'Tried setting email address to "foo&@bar.ubc.ca"! (an invalid address)' );
eval { $data->Provider('foo'); };
like( $@,
	  qr/Invalid email address specified/,
	  'Tried setting email address to "foo"! (an invalid address)' );
eval { $data->Provider('foo@bar'); };
like( $@,
	  qr/Invalid email address specified/,
	  'Tried setting email address to "foo@bar"! (an invalid address)' );
eval { $data->Provider('foo@bar.ubc.ca'); };
unlike( $@,
	  qr/Invalid email address specified/,
	  'Tried setting email address to "foo@bar.ubc.ca"! (a valid address)' );
eval { $data->Provider('foo@bar.ca'); };
unlike( $@,
	  qr/Invalid email address specified/,
	  'Tried setting email address to "foo@bar.ca"! (a valid address)' );
