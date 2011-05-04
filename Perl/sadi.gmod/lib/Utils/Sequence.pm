package Utils::Sequence;

use strict;

sub get_complementary_dna_sequence
{
    my ($sequence) = @_;

    my $complement = reverse $sequence;
    $complement =~ tr/gatcGATC/ctagCTAG/;

    return $complement;
}

sub build_sequence_node
{
}

1;
