#!/usr/bin/perl -w

use strict;
use warnings;

use lib 'lib';
use lib 'testlib';

use Utils::Text qw(wrap_text);
use LWP::Simple;
use File::Spec::Functions qw(rel2abs catdir catfile splitpath);
use IO::Uncompress::Gunzip qw(gunzip $GunzipError);
use URI;
use DBI;
use Term::UI;
use Term::ReadLine;
use Utils::Postgres;
use Utils::GMOD;
use constant::boolean;
use Text::Trim;

#--------------------------------------------------
# constants
#--------------------------------------------------

use constant DEFAULT_TESTDB_HOST => 'localhost';
use constant DEFAULT_TESTDB_PORT => 5432;
use constant DEFAULT_TESTDB_NAME => 'sadi_gmod_test';

# GMOD stores database connection profiles in files 
# named: $ENV{GMOD_ROOT}/conf/$PROFILE_NAME.conf.

use constant TESTDB_GMOD_PROFILE => 'sadi_gmod_test';

use constant TESTDB_DUMP_URL => 'http://sadi.googlecode.com/files/sadi.gmod.testdb-1.0.sql.gz';
use constant DOWNLOAD_DIR => 't';

#--------------------------------------------------
# prompt stuff
#--------------------------------------------------

my $term = Term::ReadLine->new($0);

#--------------------------------------------------
# query user for database connection params
#--------------------------------------------------

unless ($ENV{GMOD_ROOT}) {

    die wrap_text(
        
        "Please set the GMOD_ROOT environment variable. (Typically, GMOD_ROOT=/usr/local/gmod.) " .
        "If you are running this script under sudo, you can do something like:\n\n" .
        "> sudo GMOD_ROOT=/usr/local/gmod ./setup_test_db.pl\n\n"

    );

}

my $gmod_conf = Bio::GMOD::Config->new();
my $conf_file = catfile($gmod_conf->confdir, TESTDB_GMOD_PROFILE . '.conf');

unless (-w $gmod_conf->confdir) {

    die wrap_text(
        "This script needs permission to write to " . $gmod_conf->confdir . ", " .
        "you will probably need to run it as root.\n"
    );

}


my $dbconf = {};

prompt_for_db_connection_params($dbconf);

#--------------------------------------------------
# Write the database configuration to file,
# and set permissions appropriately
#--------------------------------------------------

write_conf_file($conf_file, $dbconf);

my $username = prompt(

    print_me => wrap_text(
        "\n" .
        "For security reasons, we will now change the permissions on " .
        "the GMOD database configuration file, so that it is readable only " .
        "by the user who will run SADI GMOD unit tests."
    ),

    prompt => 'Name of user that will be running the SADI GMOD unit tests: ', 
    allow => sub { trim($_); return getpwnam($_); }   # checks if the given OS user exists 

);

my ($login, $pass, $uid, $gid) = getpwnam($username);

chown($uid, $gid, $conf_file) or die "couldn't do chown on $conf_file: $!\n";
chmod(0600, $conf_file) or die "couldn't do chmod on $conf_file: $!\n";

#------------------------------------------------------------
# create the test database
#------------------------------------------------------------

print "=> dropping database '$dbconf->{DBNAME}'...\n";

Utils::Postgres::drop_db(
    $dbconf->{DBHOST},
    $dbconf->{DBPORT},
    $dbconf->{DBUSER},
    $dbconf->{DBPASS},
    $dbconf->{DBNAME},
); 

print "=> creating database '$dbconf->{DBNAME}'...\n";

Utils::Postgres::create_db(
    $dbconf->{DBHOST},
    $dbconf->{DBPORT},
    $dbconf->{DBUSER},
    $dbconf->{DBPASS},
    $dbconf->{DBNAME},
);


#-----------------------------------------------------------
# Download and unzip the test database, if necessary
#-----------------------------------------------------------

my $gzip_url = new URI(TESTDB_DUMP_URL);
my $gzip_basename = (splitpath($gzip_url->path))[2]; 
my $gzip_downloaded_file = catfile(DOWNLOAD_DIR, $gzip_basename);

my $basename = $gzip_basename; 
$basename =~ s/\.gz$//;

my $unzipped_file = catfile(DOWNLOAD_DIR, $basename);

if (! -e $unzipped_file || ! -f $unzipped_file ) {

    print "=> downloading $gzip_basename...\n";

    my $http_status = LWP::Simple::getstore(TESTDB_DUMP_URL, $gzip_downloaded_file);

    if($http_status >= 400 && $http_status < 600) {
        die sprintf('download of %s failed with HTTP %d', TESTDB_DUMP_URL, $http_status);
    }

    print "=> unzipping $gzip_basename...\n";

    gunzip($gzip_downloaded_file, $unzipped_file)
        or die "gunzip failed on ${gzip_downloaded_file}: $GunzipError";

} 

#--------------------------------------------------
# load the test database
#--------------------------------------------------

print "\n";
print wrap_text(
    "We will now load $basename into database '$dbconf->{DBNAME}'. " .
    "If the database user '$dbconf->{DBUSER}' requires a password, " .
    "you will have to enter the password again (sorry!)\n"
);   
print "\n";

Utils::Postgres::run_sql_file(
    $dbconf->{DBHOST},
    $dbconf->{DBPORT},
    $dbconf->{DBUSER},
    $dbconf->{DBNAME},
    $unzipped_file
);

print "Successfully loaded the SADI GMOD test database!\n";

#--------------------------------------------------
# Subroutines
#--------------------------------------------------

sub write_conf_file
{
    my ($filename, $dbconf) = @_;

    open(my $fh, '>', $filename) or die "error writing $filename: $!\n";
    
    foreach my $key (keys %$dbconf) {
        my $value = $dbconf->{$key} || '';
        print $fh "$key=$value\n";
    }

    close($fh) or die "error closing $filename: $!\n";
}

# This is a wrapper for get_reply (from Term::UI), so that when
# the user just presses Enter and there is no default, undef will be
# returned without warnings.
#
# This method also provides a couple of additional options
#
# trim => [0|1]    # trim leading and trailing whitespace
# repeat => [0|1]  # repeat the prompt until we get a non-empty response

sub prompt
{
    my %args = @_;

    $args{default} = '' unless $args{default};

    my $response; 
    
    do {

        $response = $term->get_reply(%args);
        trim($response) if $args{trim};

    } until($response || !$args{repeat});

    $response = undef if $response eq '';

    return $response;
}

sub prompt_for_db_connection_params 
{
    my ($dbconf) = @_;

    $dbconf->{DBHOST} = prompt(
        prompt => "Host or IP for test database", 
        default => DEFAULT_TESTDB_HOST
    ); 

    $dbconf->{DBPORT} = prompt(
        prompt => "Port for test database", 
        default => DEFAULT_TESTDB_PORT,
        allow => qr/^\d+/,
    ); 

    $dbconf->{DBNAME} = prompt(
        prompt => "Name of test database (db will be created if necessary)", 
        default => DEFAULT_TESTDB_NAME
    ); 

    $dbconf->{DBUSER} = prompt(
        prompt => "Postgres username for creating/querying the test database: ",
        allow => sub { trim($_); return $_; }
    ); 

    $dbconf->{DBPASS} = prompt(
        prompt => "Enter password for user '$dbconf->{DBUSER}' (just press Enter if no password is required): ",
    );

    print "\n";
}
