use Test::Simple tests => 2;

use strict;

use lib 'lib';

use RDF::Core::Model;
use RDF::Core::Storage::Memory;
use RDF::Core::Resource;
use RDF::Core::Statement;
use RDF::Core::Model::Serializer;

use RDF::Trine::Model;
use RDF::Trine::Serializer::Turtle;
use RDF::Trine::Node::Resource;

use Utils::RDF::Core;
use Utils::RDF::Trine;

# Setup

my $trine_model = RDF::Trine::Model->temporary_model; 
my $rdf_core_model = new RDF::Core::Model(Storage => new RDF::Core::Storage::Memory);

# Note: RDF::Core exhibits strange behaviours if you
# give it URIs that aren't HTTP URLs.  For example
# it changes the URI <a> to <uri:a>. 

use constant URI_PREFIX => "#";

my $rdfxml = <<RDFXML;

<rdf:RDF
    xmlns:a="#"
    xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">

    <rdf:Description rdf:about="#a">
        <a:b rdf:resource="#c"/>
    </rdf:Description>

</rdf:RDF>

RDFXML

my $core_a = new RDF::Core::Resource(URI_PREFIX . "a");
my $core_b = new RDF::Core::Resource(URI_PREFIX . "b");
my $core_c = new RDF::Core::Resource(URI_PREFIX . "c");

my $trine_a = new RDF::Trine::Node::Resource(URI_PREFIX . "a");
my $trine_b = new RDF::Trine::Node::Resource(URI_PREFIX . "b");
my $trine_c = new RDF::Trine::Node::Resource(URI_PREFIX . "c");

$rdf_core_model->addStmt(new RDF::Core::Statement($core_a,$core_b,$core_c));

Utils::RDF::Trine::load_rdf_core_model($trine_model, $rdf_core_model);

# uncomment to see dump of $trine_model

#my $serializer = RDF::Trine::Serializer::Turtle->new();
#print "trine_model:\n" . $serializer->serialize_model_to_string($trine_model);

# Tests

ok($trine_model->size == 1, 'model has correct number of statements after load_rdf_core_model');
ok($trine_model->get_statements($trine_a, $trine_b, $trine_c)->next, 'model contains correct statements after load_rdf_core_model');

