package Utils::SIO;

use strict;
use warnings;

use Vocab::SIO qw(
    HAS_ATTRIBUTE
    HAS_VALUE
);

sub get_attributes
{
    my ($model, $node) = @_;
    
    my @values = ();
    
    foreach my $attribute_node ($model->objects($root, HAS_ATTRIBUTE)) {
        next unless $attribute_node->is_resource;
        foreach my $value ($model->objects($attribute_node, HAS_VALUE)) {
            push @values, $value->value if $value->is_literal;
        }
    }

    if(wantarray) {
        return @values;
    } else {
        if(@values > 1) {
            warn sprintf(
                "returning only the first of multiple values for subject %s, property %s", 
                $root->uri, 
                $predicate->uri, 
                $attribute_type->uri);    
        }
        return $values[0];   
    }
}

1;

