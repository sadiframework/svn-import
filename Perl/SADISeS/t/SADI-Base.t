# Before `make install' is performed this script should be runnable with
# `make test'. After `make install' it should work as `perl SADI-Base.t'

#########################

# change 'tests => 1' to 'tests => last_test_to_print';

#use Test::More tests => 7;
use Test::More qw/no_plan/;

BEGIN {
	use FindBin qw ($Bin);
	use lib "$Bin/../lib";
	use_ok('SADI::Base');
	use_ok('SADI::Data::Boolean');
	use_ok('SADI::Data::DateTime');
	use_ok('SADI::Data::Float');
	use_ok('SADI::Data::Integer');
	use_ok('SADI::Data::String');

};


END {
	# destroy persistent data here
};
#########################

# Insert your test code below, the Test::More module is use()ed here so read
# its man page ( perldoc Test::More ) for help writing this test script.

# test primed object
my $data = SADI::Data::Object->new (namespace=>"NCBI_gi", id=>"545454");
is(ref $data, "SADI::Data::Object", 'is object reference a SADI::Data::Object?');
can_ok($data, ('toString','id', 'namespace', 'primitive','throw'));
ok($data->id eq '545454', 'Check id we primed this object with');
ok($data->namespace eq 'NCBI_gi', 'Check namespace we primed this object with');
ok(!$data->primitive, 'Check if we are a primitive');


# test unprimed object
$data = SADI::Data::Object->new ();
is(ref $data, "SADI::Data::Object", 'is object reference a SADI::Data::Object?');
can_ok($data, ('toString','id', 'namespace'));
ok($data->id eq '', 'Check id (should be empty)');
ok($data->namespace eq '', 'Check namespace (should be empty)');
ok(!$data->primitive, 'Check if we are a primitive');

# test primed boolean
$data = SADI::Data::Boolean->new (namespace=>"NCBI_gi", id=>"545454", value=>1);
is(ref $data, "SADI::Data::Boolean", 'check object reference (SADI::Data::Boolean)');
can_ok($data, ('toString','id', 'namespace', 'value'));
ok($data->id eq '545454', 'Check id we primed this object with');
ok($data->namespace eq 'NCBI_gi', 'Check namespace we primed this object with');
ok($data->primitive, 'Check if we are a primitive');
ok($data->value eq 1, 'check the value');

# test unprimed boolean
$data = SADI::Data::Boolean->new ();
is(ref $data, "SADI::Data::Boolean", 'check object reference (SADI::Data::Boolean)');
can_ok($data, ('toString','id', 'namespace', 'value'));
ok($data->id eq '', 'Check id (should be empty)');
ok($data->namespace eq '', 'Check namespace (should be empty)');
ok($data->primitive, 'Check if we are a primitive');
ok($data->value eq 1, 'check the value');

# test primed datetime
$data = SADI::Data::DateTime->new (namespace=>"NCBI_gi", id=>"545454", value=>'2009-07-15 19:44:55Z');
is(ref $data, "SADI::Data::DateTime", 'check object reference (SADI::Data::DateTime)');
can_ok($data, ('toString','id', 'namespace', 'value'));
ok($data->id eq '545454', 'Check id we primed this object with');
ok($data->namespace eq 'NCBI_gi', 'Check namespace we primed this object with');
ok($data->primitive, 'Check if we are a primitive');
ok($data->value eq '2009-07-15 19:44:55Z', 'check the value');

# test unprimed datetime
$data = SADI::Data::DateTime->new ();
is(ref $data, "SADI::Data::DateTime", 'check object reference (SADI::Data::DateTime)');
can_ok($data, ('toString','id', 'namespace', 'value'));
ok($data->id eq '', 'Check id (should be empty)');
ok($data->namespace eq '', 'Check namespace (should be empty)');
ok($data->primitive, 'Check if we are a primitive');
ok(defined $data->value, 'check the value');

# test incorrectly primed datetime
$data = SADI::Data::DateTime->new (value=>'a');
is(ref $data, "SADI::Data::DateTime", 'check object reference for incorrectly primed object (SADI::Data::DateTime)');
can_ok($data, ('toString','id', 'namespace', 'value'));
ok($data->id eq '', 'Check id (should be empty)');
ok($data->namespace eq '', 'Check namespace (should be empty)');
ok($data->primitive, 'Check if we are a primitive');
ok(defined $data->value, 'check the value');

