package Utils::Sequence;

use strict;

require Exporter;

our @ISA = qw(Exporter);
our @EXPORT_OK = qw(get_complementary_sequence);

sub get_complementary_sequence
{
    my ($sequence) = @_;

    my $complement = $sequence;
    #$complement = reverse $sequence;
    $complement =~ tr/gatucGATUC/ctaagCTAAG/;

    return $complement;
}

1;
