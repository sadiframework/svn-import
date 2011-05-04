package Utils::Postgres;

use strict;
use warnings;

use DBI;
use Data::Dumper;

sub db_exists
{
    my ($host, $port, $username, $password, $dbname) = @_;

    my $postgres_dbh = DBI->connect(

        sprintf('dbi:Pg:dbname=postgres;host=%s;port=%s', $host, $port),
        $username,
        $password,
        { RaiseError => 1, AutoCommit => 1 },

    );

    return $postgres_dbh->selectrow_hashref('SELECT datname FROM pg_database WHERE datname = ?', undef, $dbname);  
}

sub create_db
{
    my ($host, $port, $username, $password, $dbname) = @_;

    my $postgres_dbh = DBI->connect(

        sprintf('dbi:Pg:dbname=postgres;host=%s;port=%s', $host, $port),
        $username,
        $password,
        { RaiseError => 1, AutoCommit => 1 },

    );

    # note, prepare doesn't work here because it quotes the database name
    $postgres_dbh->do(sprintf('CREATE DATABASE %s', $dbname));
}

sub drop_db
{
    my ($host, $port, $username, $password, $dbname) = @_;

    my $postgres_dbh = DBI->connect(

        sprintf('dbi:Pg:dbname=postgres;host=%s;port=%s', $host, $port),
        $username,
        $password,
        { RaiseError => 1, AutoCommit => 1 },

    );

    # note, prepare doesn't work here because it quotes the database name
    $postgres_dbh->do(sprintf('DROP DATABASE IF EXISTS %s', $dbname));
}

sub run_sql_file
{
    my ($host, $port, $username, $dbname, $sql_file) = @_;

    # Unfortunately, we must do this via the 'psql' utility, 
    # as there seems to be no way to do it easily/cleanly through DBI.

    system(

        'psql',
        '--host', $host,
        '--port', $port,
        '--dbname', $dbname,
        '--username', $username,
        '--file', $sql_file,

    ) == 0 or die;
}

1;
