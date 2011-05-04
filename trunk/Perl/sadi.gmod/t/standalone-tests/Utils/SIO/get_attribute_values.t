use Test::Simple tests => 1;

use strict;
use warnings;

use lib 'lib';

use RDF::Core::Model;
use RDF::Core::Resource;
use Utils::SIO;
use Utils::RDF::Core;
use Vocab::GenomicCoordinates;
use Vocab::SIO;
use File::Spec::Functions qw(catfile);

use constant GENOMIC_COORDS_PREFIX => "http://sadiframework.org/ontologies/GMOD/genomic-coordinates.owl#";
use constant RESOURCES_DIR => 'xml';

my $GENOMIC_COORDS_NODE = new RDF::Core::Resource("http://unresolvable/inputURI");
my $model = Utils::RDF::Core::create_memory_model(catfile(RESOURCES_DIR, 'get_attribute_values.rdf'));
my $endPosition = Utils::SIO::get_attribute_values($model, $GENOMIC_COORDS_NODE, $Vocab::SIO::ORDINAL_NUMBER, $Vocab::GenomicCoordinates::END_POSITION); 

ok($endPosition eq 100, 'extract SIO attribute value');




