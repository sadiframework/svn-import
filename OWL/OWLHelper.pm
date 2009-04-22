#!/usr/bin/perl -w
use strict;

package OWLHelper;

use XML::LibXML;

sub openOWLFile
{
    my ($baseNamespace, %namespaces) = @_;
    my $owlfile = XML::LibXML::Document->new();
    my $rdf_tag = $owlfile->createElement('rdf:RDF');
    
    $rdf_tag->setAttribute('xmlns', $baseNamespace);
    foreach my $ns (keys %namespaces) {
	$rdf_tag->setAttribute('xmlns:' . $ns, $namespaces{$ns}); 
    }
    
    $owlfile->setDocumentElement($rdf_tag);
    return $owlfile;
}

sub printOWLFile
{
    my $owlfile = shift(@_);
    print $owlfile->toString(1);
}


sub addAnnotationProperty
{
    my ($owlfile, $propertyURI) = @_;
    my $rdf_tag = $owlfile->getDocumentElement();
    my $prop_tag = $owlfile->createElement('owl:AnnotationProperty');
    $prop_tag->setAttribute('rdf:about', $propertyURI);
    $rdf_tag->appendChild($prop_tag);
}

sub mobyArticleNameToPropertyName
{
    my $propname = shift(@_);

    # Replace " " in article name with "_", because
    # " " is not allowed in an OWL property name. - B.V.
    $propname =~ s/\s/_/g;

    $propname = capitalizeFirstLetter($propname);
    $propname = "has" . $propname;

    return $propname;
}
 
sub capitalizeFirstLetter
{
    my $str = shift(@_);
    
    if(length($str) == 0) {
	return $str;
    }

    my $capitalized = substr($str, 0, 1);
    $capitalized =~ tr/a-z/A-Z/;
    $capitalized .= substr($str, 1);

    return $capitalized;
}

sub newNecessaryAndSufficientConditions
{
    my ($owlfile, @restrictions) = @_;
    my $equivalentClass_tag = $owlfile->createElement('owl:equivalentClass');
    my $class_tag = $owlfile->createElement('owl:Class');
    my $intersection_tag = $owlfile->createElement('owl:intersectionOf');
    $intersection_tag->setAttribute('rdf:parseType', 'Collection');
    
    foreach my $r (@restrictions) {
	$intersection_tag->appendChild($r);
    }
    
    $class_tag->appendChild($intersection_tag);
    $equivalentClass_tag->appendChild($class_tag);
    return $equivalentClass_tag;
}

sub newCardinalityRestriction
{
    my ($owlfile, $property, $isDatatypeProperty, $cardinality) = @_;
    my $restriction_tag = newPropertyRestriction($owlfile, $property, $isDatatypeProperty);
    my $cardinality_tag = $owlfile->createElement('owl:cardinality');
    $cardinality_tag->setAttribute('rdf:datatype', 'http://www.w3.org/2001/XMLSchema#nonNegativeInteger');
    $cardinality_tag->appendChild(XML::LibXML::Text->new($cardinality));
    $restriction_tag->appendChild($cardinality_tag);
    return $restriction_tag;
}

sub newMinCardinalityRestriction
{
    my ($owlfile, $property, $isDatatypeProperty, $cardinality) = @_;
    my $restriction_tag = newPropertyRestriction($owlfile, $property, $isDatatypeProperty);
    my $cardinality_tag = $owlfile->createElement('owl:minCardinality');
    $cardinality_tag->setAttribute('rdf:datatype', 'http://www.w3.org/2001/XMLSchema#nonNegativeInteger');
    $cardinality_tag->appendChild(XML::LibXML::Text->new($cardinality));
    $restriction_tag->appendChild($cardinality_tag);
    return $restriction_tag;
}

sub newPropertyRestriction
{
    my ($owlfile, $property, $isDatatypeProperty) = @_;
    my $restriction_tag = $owlfile->createElement('owl:Restriction');
    my $onProperty_tag = $owlfile->createElement('owl:onProperty');
    my $property_tag;
    if($isDatatypeProperty) {
	$property_tag = newDatatypeProperty($owlfile, $property);
    }
    else {
	$property_tag = newObjectProperty($owlfile, $property);
    }
    $onProperty_tag->appendChild($property_tag);
    $restriction_tag->appendChild($onProperty_tag);
    return $restriction_tag;
}

sub addProperty 
{
    my ($owlfile, $property, $isDatatypeProperty, $className, $articleName) = @_;
    my $rdf_tag = $owlfile->getDocumentElement();
    my $property_tag;

    if($isDatatypeProperty) {
	$property_tag = newDatatypeProperty($owlfile, $property);
    }
    else {
	$property_tag = newObjectProperty($owlfile, $property);
    }
    
    my $source_tag = newMobyOrigin($owlfile, $className . ":" . $articleName);
    $property_tag->appendChild($source_tag);
    $rdf_tag->appendChild($property_tag);
}

sub newDatatypeProperty
{
    my ($owlfile, $property) = @_;
    my $property_tag = $owlfile->createElement('owl:DatatypeProperty');
    $property_tag->setAttribute('rdf:about', '#'.$property);
    return $property_tag;
}

sub newObjectProperty
{
    my ($owlfile, $property) = @_;
    my $property_tag = $owlfile->createElement('owl:ObjectProperty');
    $property_tag->setAttribute('rdf:about', '#'.$property);
    return $property_tag;
}

sub newAllValuesFromRestriction
{
    my ($owlfile, $property, $class) = @_;
    my $restriction_tag = newPropertyRestriction($owlfile, $property, 0);
    my $allvalues_tag = $owlfile->createElement('owl:allValuesFrom');
    $allvalues_tag->setAttribute('rdf:resource', $class);
    $restriction_tag->appendChild($allvalues_tag);
    return $restriction_tag;
}

sub addClass
{
    my ($owlfile, $className,  @classMembers) = @_;
    my $rdf_tag = $owlfile->getDocumentElement();
    my $class_tag = $owlfile->createElement('owl:Class');
    $class_tag->setAttribute('rdf:about', '#'.$className);
    
    foreach my $member (@classMembers) {
	$class_tag->appendChild($member);
    }
    
    $rdf_tag->appendChild($class_tag);
}


sub newParentClass
{
    my ($owlfile, $parentID) = @_;
    my $subclass_tag = $owlfile->createElement('rdfs:subClassOf');
    my $parentclass_tag = $owlfile->createElement('owl:Class');
    $parentclass_tag->setAttribute('rdf:about', '#'.$parentID);
    $subclass_tag->appendChild($parentclass_tag);
    return $subclass_tag;
}


sub newComment
{
   my ($owlfile, $comment) = @_;
    
   my $comment_tag = $owlfile->createElement('rdfs:comment');
   $comment_tag->setAttribute('xml:lang', 'en');
   $comment_tag->appendChild($owlfile->createCDATASection($comment));
   return $comment_tag;
}

sub newPublisher
{
   my ($owlfile, $publisher) = @_;
   my $publisher_tag = $owlfile->createElement('protege-dc:publisher');
   $publisher_tag->setAttribute('rdf:datatype', 'http://www.w3.org/2001/XMLSchema#string');
   $publisher_tag->appendChild(XML::LibXML::Text->new($publisher));
   return $publisher_tag;
}

sub newCreator
{
   my ($owlfile, $creator) = @_;
   my $creator_tag = $owlfile->createElement('protege-dc:creator');
   $creator_tag->setAttribute('rdf:datatype', 'http://www.w3.org/2001/XMLSchema#string');
   $creator_tag->appendChild(XML::LibXML::Text->new($creator));
   return $creator_tag;
}

sub newIdentifier
{
   my ($owlfile, $id) = @_;
   my $id_tag = $owlfile->createElement('protege-dc:identifier');
   $id_tag->setAttribute('rdf:datatype', 'http://www.w3.org/2001/XMLSchema#string');
   $id_tag->appendChild(XML::LibXML::Text->new($id));
   return $id_tag;
}

sub newMobyOrigin
{
   my ($owlfile, $source) = @_;
   my $source_tag = $owlfile->createElement('mobyOrigin');
   $source_tag->setAttribute('rdf:datatype', 'http://www.w3.org/2001/XMLSchema#string');
   $source_tag->appendChild(XML::LibXML::Text->new($source));
   return $source_tag;
}

sub newLabel
{
   my ($owlfile, $label) = @_;
   my $label_tag = $owlfile->createElement('protege-dc:identifier');
   $label_tag->setAttribute('rdf:datatype', 'http://www.w3.org/2001/XMLSchema#string');
   $label_tag->appendChild(XML::LibXML::Text->new($label));
   return $label_tag;
}

return 1; # all packages must return true
