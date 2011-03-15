#-----------------------------------------------------------------
# Service name: get_sequence_by_location
#    An asynchronous service
# Authority:    wilkinsonlab.ca
# Created:      11-Mar-2011 17:57:05 PST
# Contact:      ben.vvalk@gmail.com
# Description:  
#               Retrieve the sequence for a given set of genomic coordinates.
#-----------------------------------------------------------------

package Service::get_sequence_by_location;

use FindBin qw( $Bin );
use lib $Bin;

# shared helper modules for all services 
use lib "$Bin/../../lib";

use SADI::Base;
BEGIN {
    use SADI::Generators::GenServices;
    new SADI::Generators::GenServices->async_load( 
	 service_names => ['get_sequence_by_location']);
}

use vars qw( @ISA );
@ISA = qw( ca::wilkinsonlab::get_sequence_by_locationBase );
use strict;

# add vocabulary use statements
use SADI::RDF::Predicates::DC_PROTEGE;
use SADI::RDF::Predicates::FETA;
use SADI::RDF::Predicates::OMG_LSID;
use SADI::RDF::Predicates::OWL;
use SADI::RDF::Predicates::RDF;
use SADI::RDF::Predicates::RDFS;
use SADI::Utils;

use Bio::DB::Das::Chado;
use SIOUtils;

use constant GMOD_DB_NAME => "gmodrelease";
use constant GMOD_DB_HOST => "localhost";
use constant GMOD_DB_PORT => 5432;
use constant GMOD_DB_USER => "ben";
use constant GMOD_DB_PASSWORD => "ben";

use constant GMOD_ONTOLOGY_PREFIX => "http://sadiframework.org/ontologies/GMOD/";
use constant COORDINATES_ONTOLOGY_PREFIX => GMOD_ONTOLOGY_PREFIX . "genomic-coordinates.owl#";
use constant SERVICE_ONTOLOGY_PREFIX => GMOD_ONTOLOGY_PREFIX . "get-sequence-by-location.owl#";

use constant SIO_ONTOLOGY_PREFIX => "http://semanticscience.org/resource/";
use constant SIO_IDENTIFIER_TYPE => new RDF::Core::Resource(SIO_ONTOLOGY_PREFIX . "SIO_000115");

#-----------------------------------------------------------------
# process_it
#    This method is called for every client request.
#    Input data in the array reference, $values, are of type 
#      RDF::Core::Resource.
#
#      RDF::Core::Resource contains the following methods:
#         * new($URI)
#         * getURI - gets the URI for your Resource
#         * getNamespace - gets the namespace of the Resource
#         * getLocalValue - gets the value of the Resource
#         * equals($other) - checks equality of 2 Resources
#
#    $core is a reference to a SADI::RDF::Core object.
#
#    SADI::RDF::Core contains the following methods: 
#         * Signature() - gets the SADI::Service::Instance 
#               object backing this SADI::RDF::Core reference
#         * getInputNodes() - returns the RDF::Core::Resource 
#               nodes representing the input based on the 
#               input class for this service
#         * getStatements -
#            get an array of RDF::Core::Statements given a subject, object, and/or predicate from the input data
#            
#              %args
#                  subject   => the URI of the subject for which you want to retrieve statements for
#                  object    => the URI of the object for which you want to retrieve statements for
#                  predicate => the URI of the predicate for which you want to retrieve statements for
#             
#              B<subject, object and predicate are all optional.>
#              
#              returns
#                  a reference to an array of RDF::Core::Statements that match the given subject, object and predicate
#         * getObjects
#                get an array of RDF::Core::Resource nodes given a subject and predicate from the input data
#                
#                  %args
#                      subject   => the URI of the subject for which you want to retrieve objects for
#                      predicate => the URI of the predicate for which you want to retrieve objects for
#                  
#                  B<subject, object and predicate are all optional.>
#                  
#                  returns
#                      a reference to an array of RDF::Core::Resource that match the given subject and predicate
#         * addOutputData
#                add an output triple to the model; the predicate of the triple
#                  is automatically extracted from the ServicePredicate.
#                  
#                  You can pass a URI or an RDF::Core::Resource as the "value" argument.  
#                  The node is automatically rdf:typed as the OutputClass if you include
#                  the "typed_as_output" argument as true.
#                  
#                  If you pass a "value" that looks like a URI, then this routine WILL ASSUME
#                  THAT YOU WANT IT TO BE AN OBJECT, NOT A SCALAR VALUE.  To over-ride this,
#                  set the boolean "force_literal" argument.  If you pass an RDF::Core::Resource
#                  together with the force_literal argument, the URI of the RDF::Core::Resource
#                  will be extracted and added as a literal value rather than as an object.
#                
#                  args
#                     node => $URI  (the URI string or RDF::Core::Resource of the subject node
#                             or a subclass of OWL::Data::OWL::Class )
#                       NOTE:
#                          if $URI->isa(OWL::Data::OWL::Class) created via
#                          sadi-generate-datatypes script then all RDF statements
#                          for this resource are added to our model and all 
#                          other arguments are ignored!
#                     value => $val  (a string value)
#                     predicate => $URI (required - the predicate to put between them.  
#                                        unless your $URI->isa(OWL::Data::OWL::Class))
#                     typed_as_output => boolean (if present output is rdf:typed as output class)
#                     force_literal => boolean
#                     label => $label (string); label for value node, only if value is a URI
#
# Make sure to read the perldoc for up to date information on any module mentioned here!
#-----------------------------------------------------------------

sub process_it {
    
    my ($self, $values, $core) = @_;
    
    # empty data, then return
    return unless $values;

    my @inputs = @$values;
  
	# connect to the database 
	my $db = Bio::DB::Das::Chado->new(
		-dsn => 'dbi:Pg:dbname=' . GMOD_DB_NAME . ';host=' . GMOD_DB_HOST . ';port=' . GMOD_DB_PORT,
		-user => GMOD_DB_USER,
		-pass => GMOD_DB_PASSWORD,
	);
	  
    # iterate over each input
    foreach my $input (@inputs) {
    
    	$LOG->info ('processing input: ' . $input->getURI ? $input->getURI : 'no_uri'); 

        # fill in the output nodes - this is what you need to do!
        # for example ...
        foreach my $output (0..2) {
         	$core->addOutputData(
        		node => $input->getURI,
                value => "$output",
        	    predicate => "http://sadiframework.org/ontologies/predicates.owl#somePredicate$output"
        	);
            # mimic a long running service; sleep 15 seconds per addition of output
            sleep(15);  
        }
    }
}


1;
__END__

=head1 NAME

Service::get_sequence_by_location - a SADI service

=head1 SYNOPSIS

 # the only thing that you need to do is provide your
 # business logic in process_it()!
 #
 # This method consumes an array reference of input data
 # (RDF::Core::Resource),$values, and a reference to 
 # a SADI::RDF::Core object, $core.
 #
 # Basically, iterate over the the inputs, do your thing
 # and then $core->addOutputData().
 #
 # Since this is an asynchronous implementation of a SADI
 # web service, I am assuming that your task takes a while
 # to run. So to save what you have so far, do store($core).
 #  

=head1 DESCRIPTION

Retrieve the sequence for a given set of genomic coordinates.

=head1 CONTACT

B<Authority>: wilkinsonlab.ca

B<Email>: ben.vvalk@gmail.com

=cut
