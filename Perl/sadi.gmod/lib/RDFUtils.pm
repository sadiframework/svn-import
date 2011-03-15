package RDFUtils;

use strict;
use RDF::Core::Model;

sub create_memory_model 
{
	my ($file, $baseURI) = @_;

	# RDF::Core::Model requires a baseURI to be set,
	# even if there aren't any relative URIs in the
	# source document.
	$baseURI |= "unusedBaseURI#";
	
	my $storage = new RDF::Core::Storage::Memory;
	my $model = new RDF::Core::Model( Storage => $storage );

	my %options = (
		Model      => $model,
		Source     => './resources/get_attribute_values.rdf',
		SourceType => 'file',
		BaseURI    => $baseURI,
	);

	my $parser = new RDF::Core::Model::Parser(%options);
	eval { $parser->parse };
 	
 	if ($@) {
 		warn "error creating RDF::Core::Model for $file: $@\n";
 		return undef;	
 	}

	return $model;
}

sub is_resource
{
	my ($node) = @_;
	return !$node->isLiteral;	
}

sub is_literal
{
	my ($node) = @_;
	return $node->isLiteral;
}

sub as_resource
{
	my ($node) = @_;
	die "cannot cast a literal '" . $node->getLabel . "' to a resource\n" unless is_resource($node); 
	return new RDF::Core::Resource($node->getLabel);	
}


1;