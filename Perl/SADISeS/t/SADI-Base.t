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

$data = SADI::Base->new( );
my @packages = qw(
  http://dev.biordf.net/~kawas/owl/getUniprotByKeggGene.owl#UniprotByKeggGeneOutputClass
  http://dev.biordf.net/~kawas/owl/getUniprotByKeggGene.owl#~UniprotByKeggGeneOutputClass
  http://dev.biordf.net/~kawas/owl/getUniprotByKeggGene.owl#UniprotByKeggGeneOutputClass#
  http://dev.biordf.net/~kawas/owl/getUniprotByKeggGene.owl#UniprotByKeggGeneOutputClass##///
  http://dev.biordf.net/~kawas/owl/getUniprotByKeggGene.owl#UniprotByKeggGeneOutputClass/
  http://dev.biordf.net/~kawas/owl/getUniprotByKeggGene.owl#UniprotByKeggGeneOutputClass//
  http://dev.biordf.net/~kawas/owl/getUniprotByKeggGene.owl##UniprotByKeggGeneOutputClass
  http://dev.biordf.net/~kawas/owl/getUniprotByKeggGene.owl/UniprotByKeggGeneOutputClass
  http://dev.biordf.net/~kawas/owl/getUniprotByKeggGene.owl//UniprotByKeggGeneOutputClass
  http://dev.biordf.net/~kawas/owl/getUniprotByKeggGene.owl/#UniprotByKeggGeneOutputClass
  http://dev.biordf.net/~kawas/owl/getUniprotByKeggGene.owl#/UniprotByKeggGeneOutputClass
);

my $pac = undef;
$pac = $data->uri2package($_) and ok( $pac
     eq
'dev::biordf::net::kawas::owl::getUniprotByKeggGene::UniprotByKeggGeneOutputClass',
    "check uri2package($_) = $pac"
) foreach (@packages);

