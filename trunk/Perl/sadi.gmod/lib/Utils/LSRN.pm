package Utils::LSRN;

use Vocab::SIO qw(
    HAS_ATTRIBUTE
    HAS_VALUE
);
use Vocab::RDF qw(
    RDF_TYPE
);
use Vocab::LSRN qw(
    LSRN_ENTITY_PREFIX
    LSRN_ONTOLOGY_PREFIX
);

require Exporter;

our @ISA = qw(Exporter);
our @EXPORT_OK = qw(get_id_from_lsrn_record_node);

sub get_id_from_lsrn_record_node
{
    my ($model, $record_node) = @_;
   
    return undef unless $record_node->is_resource;

    my @attribute_nodes = $model->objects($record_node, HAS_ATTRIBUTE);

    if (@attribute_nodes == 0) {
        return undef if $record_node->is_blank;
        if ($record_node->uri =~ /^@{[LSRN_ENTITY_PREFIX]}(.*?):(.*)$/) {
            return ($1 . '_Identifier', $2);
        } else {
            return undef;
        }       
    } 

    foreach my $attribute_node (@attribute_nodes) {
        foreach my $attribute_type ($model->objects($attribute_node, RDF_TYPE)) {
            next unless $attribute_type->is_resource and !$attribute_type->is_blank;
            if ($attribute_type->uri =~ /^@{[LSRN_ONTOLOGY_PREFIX]}.*_Identifier$/) {
                my ($id_literal) = $model->objects($attribute_node, HAS_VALUE);
                return ($attribute_type->uri, $id_literal->value);
            }
        }
    }

}

1;

