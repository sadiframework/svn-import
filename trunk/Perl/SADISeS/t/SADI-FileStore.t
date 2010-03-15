# Before `make install' is performed this script should be runnable with
# `make test'. After `make install' it should work as `perl SADI-FileStore.t'

#########################

# change 'tests => 1' to 'tests => last_test_to_print';

#use Test::More tests => 7;
use Test::More qw/no_plan/;

BEGIN {
	use FindBin qw ($Bin);
	use lib "$Bin/../lib";
	use_ok('SADI::FileStore');
}

END {

	# destroy persistent data here
}
#########################

# Insert your test code below, the Test::More module is use()ed here so read
# its man page ( perldoc Test::More ) for help writing this test script.

# test unprimed object
my $data = SADI::FileStore->new();
is( ref $data, "SADI::FileStore",
	'is object reference a SADI::FileStore?' );


# check that the fields are all empty
is( $data->ServiceName,      undef, "check that for empty field for service name" );

# check methods
can_ok(
		$data,
		(
		   'add', 'get', 'remove'
		)
);

# set the servicename
$data->ServiceName('myTestService');

is ($data->ServiceName, 'myTestService', "check the service name setter");
is (defined $data->_escaped_name, 1, "did the escaped name get set?");
# set a value
$data->add("foobar","my value");
is ($data->get("foobar"), "my value", "check the value set for foobar");
# remove a value
is ($data->remove("foobar"), 1, "remove the value for foobar");
is ($data->remove("foobar"), 1, "remove the value for foobar ... again!");
is ($data->get("foobar"), undef, "get foobar / confirm it doesnt exist!");
is ($data->get("bar"), undef, "get bar / confirm it doesnt exist!");
is ($data->remove("bar"), 1, "remove the value for bar (doesn't exist)!");

# test the unique id generator
is (length($data->generate_uid(undef)), 24, "test id generator with undef" );
is (length($data->generate_uid(10)), 10, "test id generator with 10" );
is (length($data->generate_uid(100)), 100, "test id generator with 100" );
is (length($data->generate_uid(-1)), 24, "test id generator with -1" );
is (length($data->generate_uid(0)), 0, "test id generator with 0" );
