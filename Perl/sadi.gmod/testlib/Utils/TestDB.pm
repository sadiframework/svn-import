package Utils::TestDB;

use strict;
use warnings;

use DBI;
use YAML::Tiny qw(LoadFile);

# we assume that the tests are run from the root of the SADI GMOD project
use constant SADI_GMOD_CONF => 'sadi.gmod.conf';

sub get_test_db 
{
    my $dbconf = LoadFile(SADI_GMOD_CONF);

    my $dbh = DBI->connect(

        sprintf("dbi:Pg:dbname=%s;host=%s;port=%s", 
                $dbconf->{ TESTDB_NAME }, 
                $dbconf->{ TESTDB_HOST }, 
                $dbconf->{ TESTDB_PORT }),

        $dbconf->{ TESTDB_USERNAME },
        $dbconf->{ TESTDB_PASSWORD }

    );

    return $dbh;
}

1;

