package Bio::DB::SeqFeature::Store::Extended::Iterator;

use strict;
use warnings;

use Bio::SeqFeature::Extended;
use Utils::SO qw(is_a);

sub new {

    my $class = shift;

    my $self = {};
    $self->{wrapped_iterator} = shift;
    $self->{feature_store} = shift;
    $self->{gff_id_to_dbxref} = shift;

    bless($self, $class);
    return $self;

}

sub next_seq {
    
    my $self = shift;

    while (my $feature = $self->{wrapped_iterator}->next_seq) {
        return new Bio::SeqFeature::Extended($feature, $self->{feature_store}, $self->{gff_id_to_dbxref});
    }

    return undef;
}

1;

