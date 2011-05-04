use Test::Simple tests => 1;

use strict;
use warnings;

use lib 'lib';
use lib 'testlib';
use lib 'services';

use Service::get_features_overlapping_region;
use File::Spec::Functions qw(catfile);
use Utils::Service;
use Config::Simple;
use Vocab::URIPrefixes qw(%uri_prefixes);

# set SADI_GMOD_DB_PROFILE so that the service queries
# the test database instead of real one

my %config = ();
Config::Simple->import_from('sadi.gmod.conf', \%config);
$ENV{ SADI_GMOD_DB_PROFILE } = $config{ TEST_GMOD_DB_PROFILE };

my $input_file = catfile('test-resources', 'genomic.region.ttl');
my $service = new Service::get_features_overlapping_region;
my $output_model = Utils::Service::invoke_service($service, $input_file, 'ttl');

my $serializer = new RDF::Trine::Serializer::Turtle(namespaces => \%uri_prefixes);
print "service output:\n" . $serializer->serialize_model_to_string($output_model) . "\n";

