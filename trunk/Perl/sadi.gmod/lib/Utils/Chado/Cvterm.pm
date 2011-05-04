package Utils::Chado::Cvterm;

use strict;
use warnings;

#------------------------------------------------------------
# imports
#------------------------------------------------------------

use Algorithm::Search;
use Utils::TypeQuerySearchNode;
use DBI;

#------------------------------------------------------------
# exports
#------------------------------------------------------------

require Exporter;

use vars qw(@ISA @EXPORT_OK);

@ISA = qw(Exporter);

@EXPORT_OK = qw(
    is_a
    get_cvterm_id
    get_term_namespace_and_id
);

#------------------------------------------------------------
# logging
#------------------------------------------------------------

my $LOG = Log::Log4perl->get_logger(__PACKAGE__);

#------------------------------------------------------------
# subroutines
#------------------------------------------------------------

sub is_a
{
    my ($dbh, $term, $type) = @_;

    my $search = new Algorithm::Search();

    $search->search({
        search_this => Utils::TypeQuerySearchNode->new($dbh, $term, $type),
        search_type => 'bfs',
        solutions_to_find => 1,
        do_not_repeat_values => 1, # visit each ontology term at most once
    });

    return $search->solution_found;
}

sub get_cvterm_id
{
    my ($dbh, $term) = @_;

    my ($ontology, $id) = get_term_namespace_and_id($term);
        
    if (!$id) {
        $LOG->error("unable to parse identifier for ontology term '$term'");
        return undef;
    }

    my $sth = $dbh->prepare(

        join("\n",
             'SELECT cvterm_id',
             'FROM cvterm t',
             '     INNER JOIN dbxref x ON (t.dbxref_id = x.dbxref_id)',
             '     INNER JOIN db ON (x.db_id = db.db_id)',
             'WHERE db.name = ? AND x.accession = ?'
         )

    ); 

    $sth->execute($ontology, $id) or die sprintf("error executing SQL: %s", $sth->errstr);

    my $hashref = $sth->fetchrow_hashref;

    if (!$hashref) {
        $LOG->warn("no entry was found in db for ontology term '$term'");
        return undef;
    }

    $LOG->warn("'$term' matches more than one cvterm_id, returning only first match") if $sth->fetchrow_hashref;
    
    return $$hashref{ cvterm_id };
}

sub get_term_namespace_and_id
{
    my ($term) = @_;

    $term =~ /(.*?):(.*)/;
    return ($1, $2);
}

1;

