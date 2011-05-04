#!/usr/bin/perl -w

use strict;

use lib 'lib';
use lib 'testlib';
use lib 'services';

use SADI::RDF::Core;
use Utils::Service;
use RDF::Core::Resource;
use RDF::Core::Model::Serializer;
use RDF::Trine::Model;
use Utils::RDF::Trine;
use XML::Twig;
use Getopt::Long;
use Vocab::URIPrefixes qw(%uri_prefixes);

use constant USAGE => <<USAGE;
Usage: ./test.service.pl [--input-rdf-format <'TTL'|'RDF/XML'>] [--output-rdf-format <'TTL'|'RDF/XML'>] <service module name> <input rdf file>
USAGE

#--------------------------------------------------
# parse command-line options/arguments
#--------------------------------------------------

my @allowed_rdf_formats = ('RDF/XML', 'TTL');

my $input_rdf_format = 'RDF/XML';
my $output_rdf_format = 'RDF/XML';

my $success = GetOptions(
    "input-rdf-format=s" => \$input_rdf_format,
    "output-rdf-format=s" => \$output_rdf_format,
);

die USAGE unless grep($input_rdf_format eq $_, @allowed_rdf_formats); 
die USAGE unless grep($output_rdf_format eq $_, @allowed_rdf_formats); 
die USAGE unless $success;
die USAGE unless @ARGV == 2;

my $service_package_name = $ARGV[0];
my $input_rdf_file = $ARGV[1];

#--------------------------------------------------
# create an instance of the service
#--------------------------------------------------

my $service_file = $service_package_name;
$service_file =~ s|::|/|;
$service_file .= ".pm";

require $service_file;

no strict 'refs';
my $service = $service_package_name->new;
use strict;

#--------------------------------------------------
# invoke the service
#--------------------------------------------------

# note: $output_model is an RDF::Trine model
my $output_model = Utils::Service::invoke_service($service, $input_rdf_file, $input_rdf_format);

#--------------------------------------------------
# display the service output on STDOUT
#--------------------------------------------------

my $serializer;

if ($output_rdf_format eq 'TTL') {
    $serializer = new RDF::Trine::Serializer::Turtle(namespaces => \%uri_prefixes);
} else {
    $serializer = new RDF::Trine::Serializer::RDFXML(namespaces => \%uri_prefixes);
}

print "serializing model on STDOUT...\n";
print "service output:\n" . $serializer->serialize_model_to_string($output_model) . "\n";
