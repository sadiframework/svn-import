use Test::Simple tests => 3;

use strict;
use warnings;

use lib 'lib';

use RDF::Core::Model;
use RDF::Core::Storage::Memory;
use RDF::Core::Resource;
use RDF::Core::Statement;
use RDF::Core::Model::Serializer;

use Utils::RDF::Core;

# Setup

my $storage = new RDF::Core::Storage::Memory;
my $model = new RDF::Core::Model(Storage => $storage);

# Note: RDF::Core exhibits strange behaviours if you
# give it URIs that aren't HTTP URLs.  For example
# it changes the URI <a> to <uri:a>. 

use constant URI_PREFIX => "http://unresolvable/";

my $a = new RDF::Core::Resource(URI_PREFIX . "a");
my $b = new RDF::Core::Resource(URI_PREFIX . "b");
my $c = new RDF::Core::Resource(URI_PREFIX . "c");
my $d = new RDF::Core::Resource(URI_PREFIX . "d");
my $e = new RDF::Core::Resource(URI_PREFIX . "e");
my $f = new RDF::Core::Resource(URI_PREFIX . "f");

$model->addStmt(new RDF::Core::Statement($a,$b,$c));

my $ttl_string = <<END_TTL;
\@prefix : <http://unresolvable/> .
:d :e :f .
END_TTL

Utils::RDF::Core::load_ttl_from_string($model, undef, $ttl_string); 

# Uncomment to see a dump of the model

#my $xml;
#my $serializer = new RDF::Core::Model::Serializer(Model=>$model, Output=>\$xml, BaseURI=>'URI://BASE/');
#$serializer->serialize;
#print "Model:\n$xml\n";


# Tests

ok($model->countStmts == 2, 'correct number of statements after load_ttl_from_string');
ok($model->countStmts($a,$b,$c) == 1, 'existing statements in model unharmed by load_ttl_from_string');
ok($model->countStmts($d,$e,$f) == 1, 'correct statements loaded by load_ttl_from_string');

