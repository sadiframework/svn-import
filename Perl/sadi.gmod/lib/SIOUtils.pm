package SIOUtils;

use strict;
use RDF::Core::Resource;
use RDF::Core::Model;

use constant ONTOLOGY_PREFIX => "http://semanticscience.org/resource/";
my $HAS_ATTRIBUTE_PROPERTY = new RDF::Core::Resource(ONTOLOGY_PREFIX . "SIO_000008");
my $HAS_VALUE_PROPERTY = new RDF::Core::Resource(ONTOLOGY_PREFIX . "SIO_000300");

sub get_attribute_values
{
	my ($model, $root, $predicate, $attributeType) = @_;
	
	$predicate = $HAS_ATTRIBUTE_PROPERTY unless $predicate;
	
	my @values = ();
	
	foreach my $attributeNode (@{$model->getObjects($root, $predicate)}) {
		next unless RDFUtils::is_resource($attributeNode);
		foreach my $value (@{$model->getObjects(RDFUtils::as_resource($attributeNode), $HAS_VALUE_PROPERTY)}) {
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

1;