#!/usr/bin/perl -w
use strict;
use MOBY::Client::Central;
use OWLHelper;

#-------------------------------------------------------------
# SETUP DOCUMENT (XML namespaces and entities)
#-------------------------------------------------------------

my $baseNamespace = 'http://moby.ucalgary.ca/RESOURCES/MOBY-S/Objects#';

my %namespaces = ( 
    'protege-dc' => 'http://purl.org/dc/elements/1.1/',
    'rdf' => 'http://www.w3.org/1999/02/22-rdf-syntax-ns#',
    'rdfs' => 'http://www.w3.org/2000/01/rdf-schema#',
    'owl' => 'http://www.w3.org/2002/07/owl#',
    ); 

my $owlfile = OWLHelper::openOWLFile($baseNamespace, %namespaces);

#------------------------------------------------------------
# DEFINE OWL ANNOTATION PROPERTIES
#------------------------------------------------------------ 

OWLHelper::addAnnotationProperty($owlfile, 'http://www.w3.org/2000/01/rdf-schema#comment');
OWLHelper::addAnnotationProperty($owlfile, 'http://www.w3.org/2000/01/rdf-schema#label');
OWLHelper::addAnnotationProperty($owlfile, 'http://purl.org/dc/elements/1.1/creator');
OWLHelper::addAnnotationProperty($owlfile, 'http://purl.org/dc/elements/1.1/identifier');
OWLHelper::addAnnotationProperty($owlfile, 'http://purl.org/dc/elements/1.1/publisher');
OWLHelper::addAnnotationProperty($owlfile, $baseNamespace . 'mobyOrigin');

#-------------------------------------------------------------
# ATOMIC MOBY DATATYPES (PRIMITIVES)
#-------------------------------------------------------------

my @primitives = qw{String Integer Float Boolean DateTime};
my %isPrimitive;
for(@primitives) { $isPrimitive{$_} = 1; }

#-------------------------------------------------------------
# CONNECT TO THE MOBY REGISTRY, RETRIEVE FULL LIST OF MOBY DATATYPES
#------------------------------------------------------------

# Connect to the registry and get the full list of Moby datatypes.

my $m = MOBY::Client::Central->new(); #(Registries => $reglist);
warn "Retrieving Moby datatypes...\n";
my $n = $m->retrieveObjectNames();

#------------------------------------------------------------
# BUILD XML FOR EACH MOBY DATATYPE
#------------------------------------------------------------

foreach my $name (keys %$n){

    next if $isPrimitive{$name};

    my $def = $m->retrieveObjectDefinition(objectType => $name);
    my $desc = $def->{description};
    my $email = $def->{contactEmail};
    my $authURI = $def->{authURI};
    my $rels = $def->{Relationships};
    my $lsid = $def->{objectLSID};
    my $isa = $rels->{'urn:lsid:biomoby.org:objectrelation:isa'};
    my $hasa = $rels->{'urn:lsid:biomoby.org:objectrelation:hasa'};
    my $has = $rels->{'urn:lsid:biomoby.org:objectrelation:has'};
    my @isas;
    (@isas = @$isa) if $isa;

    # Skip the Moby root datatype 'Object'
    next unless @isas;

    warn "Generating OWL for Moby datatype $name...\n";

    my @classMembers;
    my @restrictions;

    #--------------------------------------------------------
    # Class annotations.
    #--------------------------------------------------------

    push (@classMembers, OWLHelper::newComment($owlfile, $desc));
    push (@classMembers, OWLHelper::newPublisher($owlfile, $authURI));
    push (@classMembers, OWLHelper::newCreator($owlfile, $email));
    push (@classMembers, OWLHelper::newLabel($owlfile, $name));
    
    #--------------------------------------------------------
    # Assert parent classes.
    #--------------------------------------------------------
    
    foreach my $parentClass (@isas) {

	my $parentName = $parentClass->{object};

	next if $parentName eq 'Object';

	push (@classMembers, OWLHelper::newParentClass($owlfile, $parentName));
    }
	
    #---------------------------------------------------------
    # Generate OWL for HASA members. 
    #
    # A datatype instance has exactly one member corresponding 
    # to a HASA entry. 
    #---------------------------------------------------------

    my @hasas;
    (@hasas = @$hasa) if $hasa;
    foreach my $hasa (@hasas){

        my $obj = $hasa->{object};
        my $an = $hasa->{articleName};
        my $thislsid = $hasa->{lsid};

        my $propertyName = OWLHelper::mobyArticleNameToPropertyName($an);
	my $isDatatypeProperty = $isPrimitive{$obj};

	if(!$isDatatypeProperty) {
	    push(@restrictions, OWLHelper::newAllValuesFromRestriction($owlfile, $propertyName, $obj));
	}
	    
	push(@restrictions, OWLHelper::newCardinalityRestriction($owlfile, $propertyName, $isDatatypeProperty, 1));
	OWLHelper::addProperty($owlfile, $propertyName, $isDatatypeProperty, $name, $an);
    }
    
    #---------------------------------------------------------
    # Generate OWL for HAS members.
    #
    # A datatype instance has one or more members corresponding
    # to a HAS entry.
    #---------------------------------------------------------

    my @hass;
    (@hass = @$has) if $has;
    
    foreach my $has (@hass) {

        my $obj = $has->{object};
        my $an = $has->{articleName};
        my $thislsid = $has->{lsid};

	my $isDatatypeProperty = $isPrimitive{$obj};
	
        my $propertyName = OWLHelper::mobyArticleNameToPropertyName($an);
 
	if(!$isDatatypeProperty) {
	    push (@restrictions, OWLHelper::newAllValuesFromRestriction($owlfile, $propertyName, $obj));
	}
	push (@restrictions, OWLHelper::newMinCardinalityRestriction($owlfile, $propertyName, $isDatatypeProperty, 1));
	OWLHelper::addProperty($owlfile, $propertyName, $isDatatypeProperty, $name, $an);
    }

    if(@restrictions > 0) {
	push (@classMembers, OWLHelper::newNecessaryAndSufficientConditions($owlfile, @restrictions));
    }

    OWLHelper::addClass($owlfile, $name, @classMembers);
}

OWLHelper::printOWLFile($owlfile);




