package Utils::Chado::Dbxref;

use strict;
use warnings;

#------------------------------------------------------------
# imports
#------------------------------------------------------------

use Log::Log4perl;
use DBI;
#use Config::Simple;
use File::Spec::Functions qw(catfile);

# There are several modules which support
# the same YAML API. YAML::Any will automatically
# select the best module available. 

use YAML::Any qw(LoadFile);   

#------------------------------------------------------------
# exports
#------------------------------------------------------------

require Exporter;

use vars qw(@ISA @EXPORT_OK);

@ISA = qw(Exporter);

@EXPORT_OK = qw(
    get_db_id
    get_primary_dbxref
    get_feature_by_primary_dbxref
    get_dbname_by_idtype
    get_idtype_by_dbname
    get_uriprefix_by_dbname
    get_uri_by_db_id
);

#------------------------------------------------------------
# constants / package variables
#------------------------------------------------------------

my $SADI_GMOD_ROOT = $ENV{ SADI_GMOD_ROOT } || '.';
my $DBNAME_TO_IDTYPE_MAPPING_FILE = catfile($SADI_GMOD_ROOT, 'resources', 'mappings', 'dbname.to.idtype.mapping');
my $DBNAME_TO_URIPREFIX_MAPPING_FILE = catfile($SADI_GMOD_ROOT, 'resources', 'mappings', 'dbname.to.uriprefix.mapping');

#------------------------------------------------------------
# initialize mappings between database identifiers and URIs
#------------------------------------------------------------

#my %DBNAME_TO_IDTYPE = ();
#my %DBNAME_TO_URIPREFIX = ();
#
#Config::Simple->import_from($DBNAME_TO_IDTYPE_MAPPING_FILE, \%DBNAME_TO_IDTYPE);
#Config::Simple->import_from($DBNAME_TO_URIPREFIX_MAPPING_FILE, \%DBNAME_TO_URIPREFIX);

my $DBNAME_TO_IDTYPE = LoadFile($DBNAME_TO_IDTYPE_MAPPING_FILE);
my $DBNAME_TO_URIPREFIX = LoadFile($DBNAME_TO_URIPREFIX_MAPPING_FILE);

#------------------------------------------------------------
# logging
#------------------------------------------------------------

my $LOG = Log::Log4perl->get_logger('services');

#------------------------------------------------------------
# subroutines
#------------------------------------------------------------

sub get_db_id
{
    my ($dbh, $db_name) = @_;

    my $sth = $dbh->prepare(

        join("\n",
             'SELECT db_id',
             'FROM db',
             'WHERE name = ?'
         )

     );
             
    $sth->execute($db_name) or die sprintf("error executing SQL: %s", $sth->errstr);
    
    my $hashref = $sth->fetchrow_hashref;
    return undef unless $hashref;
        
    $LOG->warn("db name '$db_name' matches more than one db_id, returning only first match") if $sth->fetchrow_hashref;
    
    return $$hashref{ db_id };
}

sub get_feature_by_primary_dbxref
{
    my ($dbh, $db_name, $db_id) = @_;

    my $sth = $dbh->prepare(

        join("\n",
             'SELECT feature_id, uniquename, f.name',
             'FROM feature f',
             '     INNER JOIN dbxref x ON (f.dbxref_id = x.dbxref_id)',
             '     INNER JOIN db ON (x.db_id = db.db_id)',
             'WHERE db.name = ? AND x.accession = ? AND f.is_obsolete = false'
         )

     );
             
    $sth->execute($db_name, $db_id) or die sprintf("error executing SQL: %s", $sth->errstr);
    
    my $hashref = $sth->fetchrow_hashref;

#    $LOG->error(sprintf("%s:%s is a primary dbxref for multiple features", $db_name, $db_id)) if $sth->fetchrow_hashref;
    
    return $hashref;
}

sub get_primary_dbxref
{
    my ($dbh, $feature_id) = @_;

    my $sth = $dbh->prepare(

        join("\n",
             'SELECT db.name, x.accession',
             'FROM feature f',
             '     INNER JOIN dbxref x ON (f.dbxref_id = x.dbxref_id)',
             '     INNER JOIN db ON (x.db_id = db.db_id)',
             'WHERE f.feature_id = ?'
         )

     );
             
    $sth->execute($feature_id) or die sprintf("error executing SQL: %s", $sth->errstr);
    
    my $hashref = $sth->fetchrow_hashref;
    return undef unless $hashref;
        
    $LOG->error("feature_id $feature_id has more than one primary dbxref") if $sth->fetchrow_hashref;
    
    return ($$hashref{name}, $$hashref{accession});
}

sub get_dbname_by_idtype
{
    my ($idtype) = @_;
    return grep($DBNAME_TO_IDTYPE->{$_} eq $idtype, keys %$DBNAME_TO_IDTYPE);
}

sub get_idtype_by_dbname
{
    my ($dbname) = @_;

    return undef unless $dbname;
    return $DBNAME_TO_IDTYPE->{$dbname};
}

sub get_uriprefix_by_dbname
{
    my ($dbname) = @_;
    return $DBNAME_TO_URIPREFIX->{$dbname};
}

sub get_uri_by_db_id
{
    my ($db, $id) = @_;

    my $uri_prefix = get_uriprefix_by_dbname($db);
    
    if (!$uri_prefix) {
        $LOG->warn("no URI mapping in $DBNAME_TO_URIPREFIX_MAPPING_FILE for database '$db'");
        return undef;
    }

    return ($uri_prefix . $id);
}

1;

