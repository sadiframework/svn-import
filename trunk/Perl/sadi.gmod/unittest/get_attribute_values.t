use Test::Simple tests => 1;

use strict;

use FindBin qw( $Bin );
use lib "$Bin/../lib"; # shared helper modules for all GMOD SADI services
use lib $Bin;

use RDF::Core::Model;
use RDF::Core::Resource;
use RDF::Core::Storage::Memory;
use RDF::Core::Model::Parser;
use Utils::SIO;
use Utils::RDF;
use Vocab::GenomicCoordinates;
use Vocab::SIO;

use constant GENOMIC_COORDS_PREFIX => "http://sadiframework.org/ontologies/GMOD/genomic-coordinates.owl#";

my $RESOURCES_DIR = "$Bin/../xml";
my $GENOMIC_COORDS_NODE = new RDF::Core::Resource("http://unresolvable/inputURI");
my $model = Utils::RDF::create_memory_model("$RESOURCES_DIR/get_attribute_values.rdf");
my $endPosition = Utils::SIO::get_attribute_values($model, $GENOMIC_COORDS_NODE, $Vocab::SIO::ORDINAL_NUMBER, $Vocab::GenomicCoordinates::END_POSITION); 

ok($endPosition eq 100, 'extract SIO attribute value');




