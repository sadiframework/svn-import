package Utils::Service;

use strict;
use warnings;

use File::Slurp;
use SADI::RDF::Core;
use Utils::RDF::Trine;
use RDF::Trine;

sub invoke_service
{
    my ($service, $input_file, $rdf_lang) = @_;

    # RDF::Core doesn't support TURTLE, so if the input is 
    # TURTLE we need to load it into an RDF::Trine model
    # first.

    my $input_rdf = read_file($input_file);

    if (uc($rdf_lang) eq 'TTL') { 
        $input_rdf = Utils::RDF::Trine::ttl_to_rdfxml($input_rdf);
    } 

    my $core = new SADI::RDF::Core;

    # load the service description to set the input class, output class, etc. 

    ref($service) =~ /(.*::)?(.*)/;
    my $service_name = $2;

    $core->Signature($service->get_service_signature($service_name));

    # build the input model

    $core->Prepare($input_rdf);

    my @input_nodes = $core->getInputNodes;

    # invoke the service

    $service->process_it(\@input_nodes, $core);

    # return the output model as an RDF::Trine model,
    # because it produces nicer looking string dumps, and
    # and has the option of outputting TURTLE
    
    my $trine_model = RDF::Trine::Model->temporary_model;
    Utils::RDF::Trine::load_rdf_core_model($trine_model, $core->_output_model);

    return $trine_model;
}

1;

