# Before `make install' is performed this script should be runnable with
# `make test'. After `make install' it should work as `perl SADI-Config.t'
#########################
# change 'tests => 1' to 'tests => last_test_to_print';
#use Test::More tests => 7;
use Test::More qw/no_plan/;
use strict;
use vars qw/$path/;
BEGIN {
	use FindBin qw ($Bin);
	use lib "$Bin/../lib";
	$path = $Bin;
	$path .= "/t" unless $path =~ /t$/;
	$ENV{SADI_CFG_DIR} = $path;
}

END {
	delete $ENV{SADI_CFG_DIR};
	# destroy persistent data here (if any)
}
#########################
# Insert your test code below, the Test::More module is use()ed here so read
# its man page ( perldoc Test::More ) for help writing this test script.
#use_ok('SADI::Config');
use SADI::Config;
my $config = SADI::Config->new();
is($config->param('generators.outdir'), 'foo', 'check for generators.outdir parameter');
is($config->param('a'), 'b', 'check for "a" parameter');
$config->delete('a');
is($config->param('a'), undef, 'check for recently deleted "a" parameter');
is($config->param('generators.outdir2'), 'foo/bar', 'check for generators.outdir2 parameter (uses ${} syntax)');
is($config->param('generators.outdir3'), 'foo/foo/bar/${generators.notHere}', 'check for generators.outdir3 parameter (uses ${} syntax)');
$config->import_names('SADICFG');
is($SADICFG::GENERATORS_OUTDIR3, 'foo/foo/bar/${generators.notHere}', 'imported names - check for imported parameter ($SADICFG::GENERATORS_OUTDIR3)');
is($SADICFG::GENERATORS_OUTDIR, 'foo', 'imported names - check for imported parameter ($SADICFG::GENERATORS_OUTDIR)');
is($config->param('t_unfold', '${generators.outdir}/unfolded'), 'foo/unfolded', 'check the unfolding of an key we add ourselves via code (t_unfold = ${generators.outdir}/unfolded');
is($config->param('generators.outdir4'), '${generators.outdir4}', 'check the unfolding of an key that is is recursive with ${xxx} syntax');
is($SADICFG::DONT_EXIST, undef, 'query for a parameter that does not exist ($SADICFG::DONT_EXIST)');