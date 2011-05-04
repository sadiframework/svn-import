package Utils::SIO;

use strict;
use RDF::Core::Resource;
use RDF::Core::Model;
use RDF::Core::Constants qw(:rdf);
use Vocab::SIO;
use Utils::RDF::Core;

sub get_attribute_values
{
    my ($model, $root, $attributeType, $predicate) = @_;
    
    $predicate = $Vocab::SIO::HAS_ATTRIBUTE unless $predicate;
    
    my @values = ();
    
    foreach my $attributeNode (@{$model->getObjects($root, $predicate)}) {
        next unless Utils::RDF::Core::is_resource($attributeNode);
        foreach my $value (@{$model->getObjects(Utils::RDF::Core::as_resource($attributeNode), $Vocab::SIO::HAS_VALUE)}) {
            push @values, $value->getLabel if $value->isLiteral;
        }
    }

    if(wantarray) {
        return @values;
    } else {
        if(@values > 1) {
            warn sprintf(
                "returning only the first of multiple values for subject %s, property %s, and attribute type %s", 
                $root->getURI, 
                $predicate->getURI, 
                $attributeType->getURI);    
        }
        return $values[0];   
    }
}

sub add_attribute_values
{
    my ($model, $root, $attributeType, $predicate, $bnodePrefix, @values) = @_;

    my $bnodeCount = 0;
    
    foreach my $value (@values) {

        my $attributeNode = new RDF::Core::Resource("_:$bnodePrefix$bnodeCount");
        $bnodeCount++;

        my $valueAsLiteral = new RDF::Core::Literal($value);  
    
        $model->addStmt(new RDF::Core::Statement($root, $predicate, $attributeNode));
        $model->addStmt(new RDF::Core::Statement($attributeNode, new RDF::Core::Resource(RDF_TYPE), $attributeType));
        $model->addStmt(new RDF::Core::Statement($attributeNode, $Vocab::SIO::HAS_VALUE, $valueAsLiteral));
        
    }
}

1;
