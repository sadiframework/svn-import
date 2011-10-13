package Utils::Array;

require Exporter;
our @ISA = qw(Exporter);
our @EXPORT_OK = qw(intersection);

sub intersection
{
    my ($array1, $array2) = @_;
    my %hash2 = ();
    my %hash_both;
    
    $hash2{$_} = 1 foreach @$array2; 
     
    foreach my $element (@$array1) {
         if ($hash2{$element}) {
             $hash_both{$element} = 1;
         }
    }

    return (keys %hash_both);
}

1;

