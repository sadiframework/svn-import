#!/usr/bin/perl -w

use strict;
use warnings;

use lib 'lib';
use lib 'testlib';

use TAP::Harness;
use File::Find::Rule;
use Getopt::Long;
use File::Spec::Functions qw(rel2abs catdir catfile splitpath);
use DBI;
use Utils::Postgres;
use Utils::GMOD;
use constant::boolean;
use Utils::Text qw(wrap_text);

#--------------------------------------------------
# constants
#--------------------------------------------------

# GMOD stores database connection profiles in files 
# named $ENV{GMOD_ROOT}/conf/$DB_PROFILE_NAME.conf.

use constant TESTDB_GMOD_PROFILE => 'sadi_gmod_test';

my $DATABASE_DEPENDENT_TESTS_DIR = catdir('t', 'database-dependent-tests');
my $STANDALONE_TESTS_DIR = catdir('t', 'standalone-tests');

#--------------------------------------------------
# parse command line options
#--------------------------------------------------

my $skip_database_tests = FALSE;

my $getoptSuccess = GetOptions(
    'skip-database-tests' => \$skip_database_tests,
);

#--------------------------------------------------
# check that the test database is setup, if we're 
# using it
#--------------------------------------------------

if (!$skip_database_tests) {  

    my $TEST_DB_NOT_SETUP_MSG = wrap_text(

        "Many of the unit tests depend on the installation of a small GMOD test database. " .  
        "It appears that this database has not been set up yet.  To setup the test database, run " .  
        "./setup_test_db.pl as root.  Alternatively, you may skip all tests that depend " .
        "on the test database by specifying the --skip-database-tests option.\n" 

    );
                  
    die wrap_text("Please set the GMOD_ROOT environment variable. (Typically, GMOD_ROOT=/usr/local/gmod)\n")
        unless $ENV{GMOD_ROOT};

    my $gmod_conf = Bio::GMOD::Config->new();
    my $conf_file = catfile($gmod_conf->confdir, TESTDB_GMOD_PROFILE . '.conf');

    #------------------------------------------------------------
    # 1) does sadi_gmod_test.conf exist? (GMOD database profile)
    #------------------------------------------------------------

    die $TEST_DB_NOT_SETUP_MSG unless (-e $conf_file && -f $conf_file);

    my $dbconf = Bio::GMOD::DB::Config->new($gmod_conf, TESTDB_GMOD_PROFILE);

    #------------------------------------------------------------
    # 2) have all required values been set in sadi_gmod_test.conf?
    #------------------------------------------------------------

    # note: the accessor methods for GMOD::Config (e.g. host(), port())
    # aren't that useful, because they return unpredictable values
    # if the corresponding fields aren't defined in the .conf
    # file.

    my $host = $dbconf->{conf}->{DBHOST};
    my $port = $dbconf->{conf}->{DBPORT};
    my $dbname = $dbconf->{conf}->{DBNAME};
    my $username = $dbconf->{conf}->{DBUSER};
    my $password = $dbconf->{conf}->{DBPASS};

    # note: password is omitted here because the database may not require one

    die $TEST_DB_NOT_SETUP_MSG unless (
            $host &&
            $port &&
            $dbname &&
            $username
    );

    #------------------------------------------------------------
    # 3) has the 'sadi_gmod_test' database been created in postgres?
    #------------------------------------------------------------

    die $TEST_DB_NOT_SETUP_MSG unless Utils::Postgres::db_exists(
            $host,
            $port,
            $username,
            $password,
            $dbname,
    ); 

    #------------------------------------------------------------
    # 4) has the 'sadi_gmod_test' database been loaded?
    #------------------------------------------------------------

    my $dbh = DBI->connect(

        sprintf('dbi:Pg:dbname=%s;host=%s;port=%s', $dbname, $host, $port),
        $username,
        $password,
        { RaiseError => 1, AutoCommit => 1 },

    );

    # Notes: 
    # 
    # 1) '_' is a wildcard character is SQL LIKE strings, and so it must be escaped
    # 2) postgres will not interpret backslashes as escapes unless you prefix the 
    #    string with 'E' 

    my $db_is_empty = ! $dbh->selectrow_hashref(

        join("\n",
            'SELECT tablename',
            'FROM pg_tables',
            'WHERE',
            '    tablename NOT LIKE E\'pg\\_%\' AND',
            '    tablename NOT LIKE E\'sql\\_%\''
        )

    );

    die $TEST_DB_NOT_SETUP_MSG if $db_is_empty;

}

#--------------------------------------------------
# run the tests
#--------------------------------------------------

my $harness = new TAP::Harness({
                        verbosity => 1,
                        lib => [ 'lib' ],
                        color => 1,
                    });

my @tests = File::Find::Rule->file()->name('*.t')->in($STANDALONE_TESTS_DIR);

if (!$skip_database_tests) {
    push(@tests, File::Find::Rule->file()->name('*.t')->in($DATABASE_DEPENDENT_TESTS_DIR));
}

$harness->runtests(@tests);

