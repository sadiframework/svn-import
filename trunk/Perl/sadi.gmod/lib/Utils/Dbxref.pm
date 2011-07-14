package Utils::Dbxref;

use strict;
use warnings;

#------------------------------------------------------------
# imports
#------------------------------------------------------------

use lib 'lib';

use SADI::GMOD::Config qw(
    $DBXREF_TO_LSRN
    $LSRN_TO_ENTITY_TYPE
);


use Vocab::LSRN qw(
    LSRN_ONTOLOGY_PREFIX
    LSRN_ENTITY_PREFIX
);

use Log::Log4perl;

#------------------------------------------------------------
# exports
#------------------------------------------------------------

require Exporter;

our @ISA = qw(Exporter);

our @EXPORT_OK = qw(
    dbxref_to_id_type
    dbxref_to_entity_type
    dbxref_to_uri
    id_type_to_entity_type
    id_type_to_dbxref
    get_feature_by_primary_dbxref
);

#------------------------------------------------------------
# logging
#------------------------------------------------------------

my $LOG = Log::Log4perl->get_logger(__PACKAGE__);

#------------------------------------------------------------
# subroutines
#------------------------------------------------------------

sub dbxref_to_entity_type
{
    my $dbxref = shift;
    my $lsrn = dbxref_to_lsrn($dbxref);
    return $lsrn ? lsrn_to_entity_type($lsrn) : undef;    
}

sub dbxref_to_lsrn
{
    my $dbxref = shift;
    my ($dbname, $id) = split(/:/, $dbxref, 2);
    return undef unless $DBXREF_TO_LSRN;
    $LOG->warn("no LSRN mapping for Dbxref prefix $dbname") unless $DBXREF_TO_LSRN->{$dbname};
    return $DBXREF_TO_LSRN->{$dbname};
}

sub lsrn_to_entity_type
{
    my $lsrn = shift;
    return undef unless $LSRN_TO_ENTITY_TYPE;
    return $LSRN_TO_ENTITY_TYPE->{$lsrn};
}

sub dbxref_to_uri
{
    my $dbxref = shift;
    my $lsrn = dbxref_to_lsrn($dbxref);
    my $id = (split(/:/, $dbxref, 2))[1];
    return $lsrn ? sprintf('%s%s:%s', LSRN_ENTITY_PREFIX, $lsrn, $id) : undef;
}

sub dbxref_to_id_type
{
    my $dbxref = shift;
    my $lsrn = dbxref_to_lsrn($dbxref);
    return $lsrn ? sprintf('%s%s_Identifier', LSRN_ONTOLOGY_PREFIX, $lsrn) : undef;
}

sub id_type_to_lsrn
{
    my $id_type = shift;
    $id_type =~ /^@{[LSRN_ONTOLOGY_PREFIX]}(.*)_Identifier$/;
    return $1;
}

sub id_type_to_dbxref
{
    my $id_type = shift;
    my $lsrn = id_type_to_lsrn($id_type);
    return $lsrn ? lsrn_to_dbxref($lsrn) : undef;
}

sub lsrn_to_dbxref
{
    my $lsrn = shift;
    return undef unless $DBXREF_TO_LSRN;
    return grep($DBXREF_TO_LSRN->{$_} eq $lsrn, keys %$DBXREF_TO_LSRN)
}

sub id_type_to_entity_type
{
    my $id_type = shift;
    my $lsrn = id_type_to_lsrn($id_type);
    return undef unless $LSRN_TO_ENTITY_TYPE;
    return $lsrn ? $LSRN_TO_ENTITY_TYPE->{$lsrn} : undef;
}

sub get_feature_by_primary_dbxref
{
    my ($feature_store, $dbxref) = @_;

    my @candidates = $feature_store->features(-attributes => { Dbxref => $dbxref });
    my @matches;

    foreach my $feature (@candidates) {
        if (grep($dbxref, $feature->primary_dbxrefs)) {
            push(@matches, $feature);
        }
    }

    if (@matches > 1) {
        $LOG->warn("multiple features have $dbxref as a primary dbxref! Returning only the first match");
    }

    return $matches[0];
}
1;

