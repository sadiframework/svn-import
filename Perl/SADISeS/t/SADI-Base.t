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
};


END {
	# destroy persistent data here
};
#########################

# Insert your test code below, the Test::More module is use()ed here so read
# its man page ( perldoc Test::More ) for help writing this test script.



