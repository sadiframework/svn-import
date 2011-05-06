package Utils::Chado::Feature;

use strict;
use warnings;

#------------------------------------------------------------
# imports
#------------------------------------------------------------

use Log::Log4perl;
use DBI;
use Utils::Chado::Cvterm qw(
    is_a
    get_term_namespace_and_id
);
use Utils::Chado::Dbxref qw(
    get_primary_dbxref
    get_uri_by_db_id
);
use Vocab::SO;

#------------------------------------------------------------
# exports
#------------------------------------------------------------

require Exporter;

use vars qw(@ISA @EXPORT_OK);

@ISA = qw(Exporter);

@EXPORT_OK = qw(
    is_double_stranded
    is_top_level_feature
    get_feature_name
    get_feature_type
    get_feature_loc
    get_feature_uri
    get_feature_type_uri
);

#------------------------------------------------------------
# logging
#------------------------------------------------------------

my $LOG = Log::Log4perl->get_logger('services');

#------------------------------------------------------------
# subroutines
#------------------------------------------------------------

sub is_double_stranded
{
    my ($dbh, $feature_id) = @_;

    my $type = get_feature_type($dbh, $feature_id);

    return (is_a($dbh, $type, $Vocab::SO::CHROMOSOME) ||
            is_a($dbh, $type, $Vocab::SO::CHROMOSOME_PART));
}

sub get_feature_name
{
    my ($dbh, $feature_id) = @_;

    my $hashref = $dbh->selectrow_hashref(
    
        join("\n",
             'SELECT name, uniquename',
             'FROM feature',
             'WHERE feature_id = ?'
        ),

        undef,
        $feature_id,

    );

    return undef unless $hashref;
    return ($hashref->{name}, $hashref->{uniquename});
}

sub is_top_level_feature
{
    my ($dbh, $feature_id) = @_;

    my $sth = $dbh->prepare(

        join("\n",
             'SELECT *',
             'FROM featureloc',
             'WHERE feature_id = ?'
         )

     );
             
    $sth->execute($feature_id) or die sprintf("error executing SQL: %s", $sth->errstr);
    
    return !defined($sth->fetchrow_hashref);
}

sub get_feature_type
{
    my ($dbh, $feature_id) = @_;
    
    my $sth = $dbh->prepare(

        join("\n",
            'SELECT db.name, x.accession',
            'FROM feature f',
            '     INNER JOIN cvterm t ON (f.type_id = t.cvterm_id)',
            '     INNER JOIN dbxref x ON (t.dbxref_id = x.dbxref_id)',
            '     INNER JOIN db ON (x.db_id = db.db_id)',
            'WHERE f.feature_id = ?'
        )

    );

    $sth->execute($feature_id) or die sprintf("error executing SQL: %s", $sth->errstr);
    
    my $hashref = $sth->fetchrow_hashref;

    $LOG->warn("no cvterm found for feature type of feature_id $feature_id") if !$hashref;
    $LOG->warn("multiple cvterms found for feature type of feature_id $feature_id") if $sth->fetchrow_hashref;

    # returns an ID for an ontology term (e.g. "SO:123456")
    return sprintf('%s:%s', $hashref->{name}, $hashref->{accession});
}

sub get_feature_loc
{
    my ($dbh, $feature_id) = @_;

    # Top level features (e.g. chromosomes) don't have entries in the featureloc table
    # In order to have a consistent RDF representation for features as for all
    # other features, we say that the reference feature for a top level feature is 
    # itself.

    if (is_top_level_feature($dbh, $feature_id)) {
        
        my $hashref = $dbh->selectrow_hashref('SELECT seqlen FROM feature WHERE feature_id = ?', undef, $feature_id);

        die "unable able to obtain seqlen for top-level feature (feature_id = $feature_id)" unless $hashref;

        my $featureloc = {};

        $featureloc->{ srcfeature_id } = $feature_id;
        $featureloc->{ strand } = 1;
        $featureloc->{ phase } = undef;
        $featureloc->{ start } = 1;
        $featureloc->{ end } = $hashref->{ seqlen }; 

        return $featureloc;

    }

    my $sth = $dbh->prepare(

        join("\n",
            'SELECT srcfeature_id, fmin, fmax, strand, phase',
            'FROM featureloc',
            'WHERE rank = 0 AND locgroup = 0 AND feature_id = ?',
        )

    );

    $sth->execute($feature_id) or die sprintf("error executing SQL: %s", $sth->errstr);
    
    my $hashref = $sth->fetchrow_hashref;

    $hashref or die "no featureloc row found for feature_id $feature_id!";
    $LOG->error("multiple featureloc rows found for feature_id $feature_id!") if $sth->fetchrow_hashref;

    # convert from interbase coords to standard base coords

    $hashref->{ start } = $hashref->{ fmin } + 1;
    $hashref->{ end } = $hashref->{ fmax };

    return $hashref;
}

sub get_feature_uri
{
    my ($dbh, $feature_id) = @_;
    
    my ($db, $db_id) = get_primary_dbxref($dbh, $feature_id);
    return undef unless ($db && $db_id);

    return get_uri_by_db_id($db, $db_id);
}

sub get_feature_id_type_uri
{
    my ($dbh, $feature_id) = @_;

    my ($db, $db_id) = get_primary_dbxref($dbh, $feature_id);
    return undef unless ($db && $db_id);

    return get_idtype_by_dbname($db);
}

sub get_feature_type_uri
{
    my ($dbh, $feature_id) = @_;

    my $feature_type = get_feature_type($dbh, $feature_id);
    my ($ontology, $id) = get_term_namespace_and_id($feature_type);
    
    return get_uri_by_db_id($ontology, $id);
}

1;

