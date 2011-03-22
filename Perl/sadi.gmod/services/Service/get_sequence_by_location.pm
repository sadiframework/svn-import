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
use Utils::SIO;
use Utils::RDF; 
use Utils::GMOD;
use Utils::Sequence;

use Vocab::GenomicCoordinates;
use Vocab::Properties;
use Vocab::SIO;


use constant GMOD_DB_PROFILE => 'default';

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
  
    # connect to the database 

    my $dbconf = Utils::GMOD::get_db_conf(GMOD_DB_PROFILE);
   
    my $db = Bio::DB::Das::Chado->new(
        -dsn => sprintf('dbi:Pg:dbname=%s;host=%s;port=%s', $dbconf->name, $dbconf->host, $dbconf->port),
        -user => $dbconf->user,
        -pass => $dbconf->password,
    );
    
    # These are supposed to be private methods, but I want my helper modules to
    # operate on instances of RDF::Core::Model (rather than SADI::RDF::Core) so 
    # that they are reusable outside the context of a SADI service.
     
    my $inputModel = $core->_model;
    my $outputModel = $core->_output_model;
    
    #-----------------------------------------------------------------------------
    # iterate over each input
    #-----------------------------------------------------------------------------
    
    my @inputs = @$values;
    
    # used to generate unique bnode identifiers
    my $inputCount = 0;
     
    foreach my $input (@inputs) {
    
        my $inputURI = $input->getURI;

        $LOG->info ('processing input: ' . $inputURI); 
        
        #------------------------------------------------------------------------------
        # Extract/check info from the input node (genomic coordinates)
        #------------------------------------------------------------------------------
        
        my @features = Utils::RDF::get_resource_values($inputModel, $input, $Vocab::GenomicCoordinates::IS_RELATIVE_TO);

        if(@features != 1) {
            $LOG->warn("skipping input $inputURI, a set of genomic coordinates must have exactly one reference feature (e.g. a chromosome)");
            next;   
        }

        my $feature = $features[0];
        my $featureID = Utils::SIO::get_attribute_values($inputModel, $feature, $Vocab::SIO::IDENTIFIER);

        if(!$featureID) {
            $LOG->warn("skipping input $inputURI, reference feature for genomic coordinates does not have an identifier");
            next;
        }
    
        my $startpos = Utils::SIO::get_attribute_values($inputModel, $input, $Vocab::SIO::ORDINAL_NUMBER, $Vocab::GenomicCoordinates::START_POSITION);
        my $endpos = Utils::SIO::get_attribute_values($inputModel, $input, $Vocab::SIO::ORDINAL_NUMBER, $Vocab::GenomicCoordinates::END_POSITION);
        my $strand = Utils::SIO::get_attribute_values($inputModel, $input, $Vocab::SIO::NUMBER, $Vocab::GenomicCoordinates::STRAND);
        
        #------------------------------------------------------------------------------
        # Retrieve the sequence
        #------------------------------------------------------------------------------
        
        my $segment = get_segment($db, $featureID, $startpos, $endpos);

        if(!$segment) {
            $LOG->warn("skipping input $inputURI, no feature found for ID (i.e. 'uniquename') '$featureID'");
            next;
        }

        if(!$segment->seq || !$segment->seq->seq) {
            $LOG->warn("skipping input $inputURI, database does not have any sequence data for the specified coordinates");
            next;
        }
    
        my $sequence = $segment->seq->seq;

        if($strand eq -1) {
            $sequence = Utils::Sequence::get_complementary_dna_sequence($sequence);
        }
        
        #------------------------------------------------------------------------------
        # Encode the output data
        #------------------------------------------------------------------------------

        my $bnodePrefix = "a$inputCount";
        
        Utils::SIO::add_attribute_values(
                $outputModel, 
                $input, 
                $Vocab::SIO::DNA_SEQUENCE, 
                $Vocab::Properties::HAS_SEQUENCE, 
                $bnodePrefix, 
                ($sequence)
                );
        
        $inputCount++;
    }

}

#-------------------------------------------------------------------------------------------
# SUBROUTINES
#-------------------------------------------------------------------------------------------

sub get_segment
{
    my ($db, $uniquename, $start, $end) = @_;
    
    # Bio::DB::Das::Chado quirk -- when retrieving segments/features, we must always supply 
    # the name of the feature (e.g. chromosome "IX") in addition to whatever information
    # we want to query by (e.g. uniquename).
    
    my $name = get_name_by_uniquename($db->dbh, $uniquename);
    return undef unless $name;
    
    my @segments = $db->segment(-name => $name, -db_id => $uniquename, -start => $start, -end => $end);

    $LOG->error("GMOD database is corrupt!, uniquename '$uniquename' matches multiple features (using the first match only)") if @segments > 1;

    return $segments[0];
}

sub get_name_by_uniquename
{
    my ($dbh, $uniquename) = @_;

# This code was inexplicably causing some error in DBD::Pg ('server unexpectedly closed connection').
#
#    my @features = Bio::Chado::CDBI::Feature->search(uniquename => $uniquename);
#
#    LOG->error("GMOD database is corrupt!, feature ID (i.e. 'uniquename') '$uniquename' matches multiple features (using the first match only)") if @features > 1;
#    return undef unless @features == 1;
#
#    return $features[0]->get('name');
    
    my $sth = $dbh->prepare('SELECT name FROM feature WHERE uniquename = ?');
    $sth->execute($uniquename) or die sprintf("error executing SQL: %s", $sth->errstr);

    my $hashref = $sth->fetchrow_hashref;
    return undef unless $hashref;
    
    $LOG->error("GMOD database is corrupt!, uniquename '$uniquename' matches multiple features (using the first match only)") if $sth->fetchrow_hashref;
    
    return $$hashref{'name'};
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
