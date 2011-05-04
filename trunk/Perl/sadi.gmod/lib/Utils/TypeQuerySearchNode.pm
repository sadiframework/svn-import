package Utils::TypeQuerySearchNode;

use strict;
use warnings;

use lib 'lib';

use Utils::Chado::Cvterm;
use constant::boolean;
use DBI;

sub new 
{ 
    my $self = shift;
    my ($dbh, $term, $type) = @_;

    my $term_cvterm_id = Utils::Chado::Cvterm::get_cvterm_id($dbh, $term);
    my $type_cvterm_id = Utils::Chado::Cvterm::get_cvterm_id($dbh, $type);

    # we figure this out up front because we need to know it for
    # every call to next_moves

    my $is_a_cvterm_id = Utils::Chado::Cvterm::get_cvterm_id($dbh, 'OBO_REL:is_a');

    return bless { 
        dbh => $dbh, 
        term => $term,
        term_cvterm_id => $term_cvterm_id, 
        type => $type,
        type_cvterm_id => $type_cvterm_id,
        is_a_cvterm_id => $is_a_cvterm_id,
    };
}

sub move 
{
    my $self = shift;
    $self->{ term_cvterm_id } = shift;

    # move returns the relative cost of the move.
    # since this is not a cost-based search, we just 
    # return 0.

    return 0;
}

# note: the value method is used to avoid traversing the 
# same node twice

sub value 
{
    my $self = shift;
    return $self->{ term_cvterm_id };
}

# a clone method (required by Algorithm::Search)

sub copy
{
    my $self = shift;
    return $self->new($self->{ dbh }, $self->{ term }, $self->{ type });
}

sub is_solution
{
    my $self = shift;
    return ($self->{ term_cvterm_id } == $self->{ type_cvterm_id });
}

sub next_moves 
{
    my ($self) = @_;

    my $dbh = $self->{ dbh };
    my $term_cvterm_id = $self->{ term_cvterm_id };
    my $is_a_cvterm_id = $self->{ is_a_cvterm_id };

    my $sth = $dbh->prepare(

        join("\n",
             'SELECT object_id',
             'FROM cvterm_relationship',
             'WHERE',
             '  subject_id = ? AND type_id = ?',
         )

    );

    $sth->execute($term_cvterm_id, $is_a_cvterm_id) or die sprintf("error executing SQL: %s", $sth->errstr);

    my @next_moves = ();
    while (my $hashref = $sth->fetchrow_hashref) {
        push(@next_moves, $hashref->{ object_id });    
    }

    return @next_moves;
}

1;

