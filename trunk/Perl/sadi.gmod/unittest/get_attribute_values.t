use Test::Simple tests => 1;

use strict;

use FindBin qw( $Bin );
use lib "$Bin/../lib"; # shared helper modules for all GMOD SADI services
use lib $Bin;

use RDF::Core::Model;
use RDF::Core::Storage::Memory;
use RDF::Core::Model::Parser;
use SIOUtils;
use RDFUtils;

use constant GENOMIC_COORDS_PREFIX => "http://sadiframework.org/ontologies/GMOD/genomic-coordinates.owl#";

my $GENOMIC_COORDS_NODE = new RDF::Core::Resource("http://unresolvable/inputURI");
my $END_POSITION_PROPERTY = new RDF::Core::Resource(GENOMIC_COORDS_PREFIX . "end-position");
my $ORDINAL_TYPE = new RDF::Core::Resource(SIOUtils::ONTOLOGY_PREFIX . "SIO_000613");

my $model = RDFUtils::create_memory_model('./resources/get_attribute_values.rdf');
my $endPosition = SIOUtils::get_attribute_values($model, $GENOMIC_COORDS_NODE, $END_POSITION_PROPERTY, $ORDINAL_TYPE); 

ok($endPosition eq 100, 'extract SIO attribute value');




