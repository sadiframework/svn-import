package Bio::DB::SeqFeature::Store::Extended::TypeFilteringIterator;

use strict;
use warnings;

use lib 'lib';

use Bio::SeqFeature::Extended;
use Utils::SO qw(is_a);

use base 'Bio::DB::SeqFeature::Store::Extended::Iterator';

sub new {

    my $class = shift;
    my $wrapped_iterator = shift;
    my $gff_id_to_dbxref = shift;

    my $self = $class->SUPER::new($wrapped_iterator, $gff_id_to_dbxref);
    $self->{types} = \@_;

    bless($self, $class);
    return $self;

}

sub next_seq {
    
    my $self = shift;

    while (my $feature = $self->SUPER::next_seq) {
        foreach my $type (@{$self->{types}}) {
            return $feature if is_a($feature->primary_tag, $type);
        }
    }

    return undef;
}

1;
