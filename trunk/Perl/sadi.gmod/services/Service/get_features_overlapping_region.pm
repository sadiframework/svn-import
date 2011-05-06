#-----------------------------------------------------------------
# Service name: get_features_overlapping_region
#    An asynchronous service
# Authority:    wilkinsonlab.ca
# Created:      30-Mar-2011 15:26:11 PDT
# Contact:      ben.vvalk@gmail.com
# Description:  
#               Retrieve the features overlapping the given genomic region
#-----------------------------------------------------------------

package Service::get_features_overlapping_region;

#-----------------------------------------------------------------
# This is a mandatory section - but you can still choose one of
# the two options (keep one and commented out the other):
#-----------------------------------------------------------------
use SADI::Base;
# --- (1) this option loads dynamically everything
BEGIN {
    use SADI::Generators::GenServices;
    new SADI::Generators::GenServices->async_load( 
	 service_names => ['get_features_overlapping_region']);
}

# --- (2) this option uses pre-generated module
#  You can generate the module by calling a script:
 #    sadi-generate-services -B get_features_overlapping_region
#  then comment out the whole option above, and uncomment
#  the following line (and make sure that Perl can find it):
#use SADI::Generators::GenServices;
#use ca::wilkinsonlab::get_features_overlapping_regionBase;

# (this to stay here with any of the options above)
use vars qw( @ISA );
@ISA = qw( ca::wilkinsonlab::get_features_overlapping_regionBase );

use strict;
use warnings;

# add vocabulary use statements
use SADI::RDF::Predicates::DC_PROTEGE;
use SADI::RDF::Predicates::FETA;
use SADI::RDF::Predicates::OMG_LSID;
use SADI::RDF::Predicates::OWL;
use SADI::RDF::Predicates::RDF;
use SADI::RDF::Predicates::RDFS;
use SADI::Utils;

use Bio::DB::Das::Chado;
use Template;
use File::Spec::Functions qw(catfile);
#use Config::Simple;
use YAML::Any qw(LoadFile);
use RDF::Core::Constants qw(:rdf);
use RDF::Query;
use RDF::Trine::Parser;

use Vocab::SIO;
use Vocab::LSRN;
use Vocab::Strand;

use Utils::GMOD;
use Utils::Chado::Feature qw(
    is_double_stranded 
    get_feature_name
    get_feature_type
    get_feature_loc
    get_feature_uri
    get_feature_type_uri
);

use Utils::Chado::Dbxref qw(
    get_feature_by_primary_dbxref
    get_primary_dbxref
    get_dbname_by_idtype
    get_idtype_by_dbname
    get_uri_by_db_id
);

use Utils::RDF::Trine;
use Utils::RDF::Query qw(build_query execute_query);
use Utils::RDF::Core;

use constant::boolean;

use constant GMOD_CONF_FILE => 'sadi.gmod.conf';

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
#                  THAT YOU WANT IT TO BE AN OBJECT, NOT A SCALAR VALUE.  To$query = RDF::Query->new($ over-ride this,
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

    # the CGI wrapper script sets $ENV{ SADI_GMOD_ROOT } so that the
    # services are able to load resource files from the SADI GMOD 
    # directory tree.

    my $SADI_GMOD_ROOT = $ENV{SADI_GMOD_ROOT} || '.';

    # load SADI GMOD config

#    my %sadi_gmod_config = ();
#    Config::Simple->import_from(catfile($SADI_GMOD_ROOT, GMOD_CONF_FILE), \%sadi_gmod_config);
    my $sadi_gmod_config = LoadFile(catfile($SADI_GMOD_ROOT, GMOD_CONF_FILE));

    $ENV{GMOD_ROOT} = $sadi_gmod_config->{GMOD_ROOT} if $sadi_gmod_config->{GMOD_ROOT};

    # for building TURTLE and SPARQL from templates.

    my $templater = Template->new({
            ABSOLUTE => 1   # allow absolute paths to template files 
        });

    #------------------------------------------------------------
    # connect to the database 
    #------------------------------------------------------------

    # note: environment variable facilitates unit testing against a separate database
    my $profile = $ENV{ SADI_GMOD_DB_PROFILE } || $sadi_gmod_config->{ GMOD_DB_PROFILE };

    if (!defined($profile)) {

        $LOG->warn(sprintf('no value for GMOD_DB_PROFILE has been set in %s,' .
                           'service will use the default GMOD DB profile',
                           catfile($SADI_GMOD_ROOT, GMOD_CONF_FILE),
                       ));

    }

    my $dbconf = Utils::GMOD::get_db_conf($profile)->{conf};

    # It is safer not to use the GMOD::Config accessor functions 
    # (e.g. $dbconf->host), because those functions return
    # unpredictable values when the corresponding 
    # fields are blank/missing in the GMOD .conf file.

    my $host = $dbconf->{DBHOST};
    my $port = $dbconf->{DBPORT};
    my $dbname = $dbconf->{DBNAME};
    my $user = $dbconf->{DBUSER};
    my $password = $dbconf->{DBPASS};
  
    # note: the new method will die if it can't connect to the db

    my $chado = Bio::DB::Das::Chado->new(
        -dsn => sprintf('dbi:Pg:dbname=%s;host=%s;port=%s', $dbname, $host, $port),
        -user => $user,
        -pass => $password,
    );

    my $dbh = $chado->dbh;

    #-----------------------------------------------------------------------------
    # get handles to input/output models
    #-----------------------------------------------------------------------------
    
    # These are supposed to be private methods, but I want my helper modules to
    # operate on instances of RDF::Core::Model (rather than SADI::RDF::Core) so 
    # that they are reusable outside the context of a SADI service.
     
    my $input_model = $core->_model;
    my $output_model = $core->_output_model;
    
    # Load a copy of the input model into an RDF::Trine model, so we can 
    # do SPARQL queries on it.

    my $trine_input_model = RDF::Trine::Model->temporary_model;
    Utils::RDF::Trine::load_rdf_core_model($trine_input_model, $input_model);

    my $trine_output_model = RDF::Trine::Model->temporary_model;

    #-----------------------------------------------------------------------------
    # iterate over each input
    #-----------------------------------------------------------------------------
   
    # maps Chado feature_id => URI, for those features that have already
    # been added to the output RDF

    my %visited_feature_uris = ();

    # maps Chado "$feature_id:$strand" => reference sequence URI,
    # for those reference sequences that have already been added 
    # to the output RDF

    my %visited_ref_sequence_uris = ();

    my $blank_node_counter = 0;
    my @output_ttl = ();

    my @inputs = @$values;

    my $feature_count = 0;

    foreach my $input (@inputs) {
    
        my $input_uri = $input->getURI;

        $LOG->info ("skipping input $input_uri because it is a blank node") if ($input_uri =~ /^_:/);
        $LOG->info ("processing input: $input_uri"); 

        #------------------------------------------------------------------------------
        # extract info from the input node (BiopolymerRegion)
        #------------------------------------------------------------------------------

        my ($query, @results);

        $query = build_query(
                    catfile($SADI_GMOD_ROOT, 'resources', 'templates', 'sparql', 'parse.biopolymer.region.sparql.tt2'), 
                    { biopolymer_region_uri => $input_uri }
                );

        @results = execute_query($query, $trine_input_model);
    
        if (@results == 0) {
            $LOG->warn("skipping input $input_uri, input graph is malformed or is missing required data");
            next;
        }

        # The position of the BiopolymerRegion may be defined relative to multiple
        # reference features. Regardless of which reference feature is used, the real
        # physical position of the region should be the same, so we just use the 
        # first reference feature that we can find in database.

        my $result;

        my $ref_feature_db;
        my $ref_feature_id;
        my $ref_feature;
        my $start_pos;
        my $end_pos;
        my $strand;

        foreach $result (@results) {

            # check that the bindings are of the expected type (resource/literal)

            next unless $result->{start_pos}->is_literal;
            next unless $result->{end_pos}->is_literal;
            next unless $result->{strand_type}->is_resource || $result->{strand_type}->is_nil;
            next unless $result->{ref_feature_id_type}->is_resource; 
            next unless $result->{ref_feature_id}->is_literal;

            # filter out unwanted rdf:types for strand

            my $strand_type_uri = $result->{strand_type}->uri;
            next unless ($strand_type_uri eq $Vocab::Strand::PLUS_STRAND->getURI ||
                         $strand_type_uri eq $Vocab::Strand::MINUS_STRAND->getURI); 

            # try to find the reference feature in the database

            my @dbnames = get_dbname_by_idtype($result->{ ref_feature_id_type }->uri);
            next unless @dbnames;

            foreach $ref_feature_db (@dbnames) {

                $ref_feature_id = $result->{ ref_feature_id }->value;
                $ref_feature = get_feature_by_primary_dbxref($dbh, $ref_feature_db, $ref_feature_id);
                next unless $ref_feature;

                $start_pos = $result->{ start_pos }->value;
                $end_pos = $result->{ end_pos }->value;

                if ($result->{ strand_type }->is_resource) {
                    $strand = ($result->{ strand_type }->uri eq $Vocab::Strand::PLUS_STRAND) ? +1 : -1;
                }

                last;
            }
            
            last if $ref_feature;
        }
      
        if (!$ref_feature) {
            $LOG->warn("skipping input $input_uri, the DB accession for the reference feature(s) could not be found in database");
            next;
        }  
      
        #------------------------------------------------------------------------------
        # retrieve the features overlapping the input GenomicRegion
        #------------------------------------------------------------------------------

        # Bio::DB::Das::Chado quirk -- a feature name must be supplied with 
        # all calls to segment()

        #print "building segment...\n";
        
        my $segment = $chado->segment(
                                -feature_id => $ref_feature->{feature_id}, 
                                -name => $ref_feature->{name},
                                -start => $start_pos,
                                -end => $end_pos
                                );

        if(!$segment) {

            $LOG->warn(
                sprintf(
                    "skipping input $input_uri, could not build DAS segment for feature %s:%s, range (%d .. %d)",
                    $ref_feature_db,
                    $ref_feature_id,
                    $start_pos,
                    $end_pos
                ));

            next;

        }

        #print "finding features...\n";

        my $it = $segment->features(-rangetype => 'overlaps', -iterator => 1);

        #------------------------------------------------------------------------------
        # encode each matching feature and its position in RDF
        #------------------------------------------------------------------------------
        
        while (my $feature = $it->next_seq) {

            # omit analysis-based features for now (e.g. alignments), as there are a lot of them
            next if $feature->is_analysis;

            my $feature_id = $feature->feature_id;
            
            while(!$visited_feature_uris{$feature_id}) {

                #print 'feature_count: ' . $feature_count++ . "\n";
                #print 'organism ' . $feature->organism . "\n";

                #------------------------------------------------------------
                # add RDF for feature 
                #------------------------------------------------------------

                my $feature_uri = 
                        get_feature_uri($dbh, $feature_id) ||
                        ('_:feature'.$blank_node_counter++);

                # mark feature as visited

                $visited_feature_uris{$feature_id} = $feature_uri;

                my $feature_type_uri = get_feature_type_uri($dbh, $feature_id);
                my ($feature_db, $feature_db_id) = get_primary_dbxref($dbh, $feature_id);
                my $feature_id_type_uri = get_idtype_by_dbname($feature_db);                         
                my $feature_loc = get_feature_loc($dbh, $feature_id);

                # Some features aren't specific to a strand (e.g. a chromosome band)
                # and in these cases $feature_loc->{strand} will either be 0 or undef.
                #
                # In these cases we use the plus strand as the reference point for 
                # defining the position of the feature.

                my $feature_strand = $feature_loc->{strand} || 1;
             
                my $ref_sequence_key = sprintf("%s:%s", $feature->srcfeature_id, $feature_strand); 
                my $ref_sequence_uri = 
                        $visited_ref_sequence_uris{$ref_sequence_key} ||
                        ('_:sequence'.$blank_node_counter++);

                # mark the reference sequence as visited

                my $ref_sequence_visited = $visited_ref_sequence_uris{$ref_sequence_key};
                $visited_ref_sequence_uris{$ref_sequence_key} = $ref_sequence_uri;

                # all uri variables may also be blank node labels, so before we put
                # them in the template, we may need to surround them with "<..>"
                
                bracket_if_uri(
                    \$feature_uri, 
                    \$feature_type_uri, 
                    \$feature_id_type_uri, 
                    \$ref_sequence_uri 
                );

                my $feature_ttl;

                $templater->process(

                        catfile($SADI_GMOD_ROOT, 'resources', 'templates', 'ttl', 'feature.ttl.tt2'),

                        {
                            feature_uri => $feature_uri,
                            feature_type_uri => $feature_type_uri,
                            feature_db_id => $feature_db_id,
                            feature_id_type_uri => $feature_id_type_uri,
                            start_pos => $feature_loc->{start},
                            end_pos => $feature_loc->{end},
                            ref_sequence_uri => $ref_sequence_uri,
                        },
                    
                        \$feature_ttl,

                );
                      
                push(@output_ttl, $feature_ttl); 

                #------------------------------------------------------------
                # link input region to feature with SIO 'overlaps with' predicate
                #------------------------------------------------------------

                push(@output_ttl, 
                    join("\n",
                        "",
                        "\@prefix sio: <http://semanticscience.org/resource/> .",
                        "<$input_uri> sio:SIO_000325 $feature_uri .",
                        "",
                    )
                ); 
                
                #------------------------------------------------------------
                # add RDF that connects ref sequence to ref feature 
                #------------------------------------------------------------

                if (!$ref_sequence_visited) {

                    my $ref_feature_id = $feature_loc->{srcfeature_id};

                    my $ref_feature_uri = 
                            $visited_feature_uris{ $ref_feature_id } ||
                            get_feature_uri($dbh, $ref_feature_id) ||
                            ('_:feature'.$blank_node_counter++);

                    # temporarily forced to TRUE due to internal inconsistencies in FlyBase --
                    # some features have type dxrefs of the form "SO:0000123" and some have "SO:SO:0000123"
                    
                    my $ref_feature_is_double_stranded = TRUE; # is_double_stranded($dbh, $feature->srcfeature_id);
                    my $strand_type_uri = ($feature_strand == -1) ? $Vocab::Strand::MINUS_STRAND->getURI : $Vocab::Strand::PLUS_STRAND->getURI;

                    # check for unexpected case

                    if (!$ref_feature_is_double_stranded && $feature_strand == -1) {

                        $LOG->error(sprintf("Unhandled case: feature is located on the opposite strand of a " .
                                "single stranded feature. Omitting feature_id %s", 
                                $feature_id ));
                        last;

                    }

                    # all uri variables may also be blank node labels, so before we put
                    # them in the template, we may need to surround them with "<..>"

                    bracket_if_uri(
                        \$ref_feature_uri, 
                        \$strand_type_uri, 
                    );

                    my $ref_sequence_ttl;

                    $templater->process(

                        catfile($SADI_GMOD_ROOT, 'resources', 'templates', 'ttl', 'ref.sequence.ttl.tt2'),

                        {
                            ref_sequence_uri => $ref_sequence_uri,
                            ref_feature_uri => $ref_feature_uri,
                            ref_feature_is_double_stranded => $ref_feature_is_double_stranded,
                            strand_type_uri => $strand_type_uri, 
                        },
                    
                        \$ref_sequence_ttl,

                    );

                    push(@output_ttl, $ref_sequence_ttl);
                    
                    # mark reference sequence as visited 
                    $visited_ref_sequence_uris{$ref_sequence_key} = $ref_sequence_uri;
                }

                # repeat the above current feature's reference feature, and its reference feature, and so on.
                $feature_id = $feature_loc->{srcfeature_id}; 

            }
            
        } # for each matching feature
        
    } # for each input (genomic region)

    my $ttl = join("\n", @output_ttl);

    #print "writing ttl to temp file...\n";

#    open(my $fh, '>', '/home/ben/Temp/test.ttl') or die $!;
#    print $fh $ttl;
#    close($fh);


    #print "loading ttl into output model...\n";

    Utils::RDF::Core::load_ttl_from_string($output_model, undef, join("\n", @output_ttl)) if @output_ttl;

#    $self->store($core, 1);
    
}

sub bracket_if_uri
{
    map($$_ = "<$$_>", grep(ref($_) && defined($$_) && $$_ !~ /^_:/, @_));
}

1;
__END__

=head1 NAME

Service::get_features_overlapping_region - a SADI service

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

Retrieve the features overlapping the given genomic region

=head1 CONTACT

B<Authority>: wilkinsonlab.ca

B<Email>: ben.vvalk@gmail.com

=cut
