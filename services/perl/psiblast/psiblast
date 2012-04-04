#!/usr/bin/perl

package psiblast;

use strict;
use warnings;

#-----------------------------------------------------------------
# CONFIGURATION
#-----------------------------------------------------------------

my $service_config = {
    ServiceName => 'psiblast',
    Description => 'Runs PSI-BLAST against SwissProt',
    InputClass => 'http://semanticscience.org/resource/SIO_010015',
    OutputClass => 'http://sadiframework.org/ontologies/blast.owl#BLASTedSequence',
    Authority => 'sadiframework.org',
    Provider => 'ben dot vvalk at gmail',
};

my $blast_config = {
    Database => '/home/ben/temp/swissprot',
    DatabaseName => 'swissprot',
    BinaryDir => '/home/ben/temp/ncbi-blast-2.2.26+/bin/',
    BlastVersion => 'BLAST+ 2.2.26',
    NumIterations => 1,
};

#-----------------------------------------------------------------
# CGI HANDLER
#-----------------------------------------------------------------

use Log::Log4perl qw(:easy);
use base 'SADI::Simple::SyncService';

Log::Log4perl->easy_init($WARN);

my $service = psiblast->new(%$service_config);
$service->handle_cgi_request;

#-----------------------------------------------------------------
# RDF TEMPLATES
#-----------------------------------------------------------------

use constant ALIGNMENT_N3_TEMPLATE => <<'HEREDOC';

@prefix blast:   <http://sadiframework.org/ontologies/blast.owl#> .
@prefix sio:     <http://semanticscience.org/resource/> .

<[% blast_hit_uri %]> 
   sio:SIO_000028
     [  a     blast:SequenceAlignment ;
        sio:SIO_000008
            [ a     blast:bits ;
              sio:SIO_000300 "[% bitscore %]"^^<http://www.w3.org/2001/XMLSchema#float>
            ] ;
        sio:SIO_000008
            [ a     blast:expectation ;
              sio:SIO_000300 "[% evalue %]"^^<http://www.w3.org/2001/XMLSchema#decimal>
            ] ;
        sio:SIO_000008
            [ a     blast:score ;
              sio:SIO_000300 "[% score %]"^^<http://www.w3.org/2001/XMLSchema#int>
            ] ;
        sio:SIO_000008
            [ a     blast:Consensus ;
              sio:SIO_000300 "[% consensus_seq %]"^^<http://www.w3.org/2001/XMLSchema#string>
            ] ;
        sio:SIO_000008
            [ a     blast:identity ;
              sio:SIO_000300 "[% percent_identity %]"^^<http://www.w3.org/2001/XMLSchema#float>
            ] ;

        sio:SIO_000028 _:querySubseq , _:hitSubseq;

        # a BLAST hit is the output of a particular BLAST invocation...
        # "is output of"
        sio:SIO_000232 <[% blast_invocation_uri %]>

     ] . 

# this is a sequence that is part of an HSP
# in this case, it is a sub-sequence of the query sequence

_:querySubseq

  # "protein sequence"
  a       sio:SIO_010015 ;

  # "has attribute"
  sio:SIO_000008

          # "sequence start position"
          [ a       sio:SIO_000791 ;
            sio:SIO_000300 "[% query_start_pos %]"^^<http://www.w3.org/2001/XMLSchema#int>
          ] ;

  # "has attribute"
  sio:SIO_000008

          # "sequence stop position"
          [ a       sio:SIO_000792 ;
            sio:SIO_000300 "[% query_stop_pos %]"^^<http://www.w3.org/2001/XMLSchema#int>
          ] ;

  # "is part of"
  sio:SIO_000068 <[% query_seq_uri %]> ;

  # "has value"
  sio:SIO_000300 "[% query_subseq %]" .

# this is a sequence that is part of an HSP
# in this case, it is a sub-sequence of the match sequence
# it parallels the previous sub-sequence

_:hitSubseq

  # "protein sequence"
  a       sio:SIO_010015 ;

  # "has attribute"
  sio:SIO_000008

          # "sequence start position"
          [ a       sio:SIO_000791 ;
            sio:SIO_000300 "[% hit_start_pos %]"^^<http://www.w3.org/2001/XMLSchema#int>
          ] ;

  # "has attribute"
  sio:SIO_000008

          # "sequence stop position"
          [ a       sio:SIO_000792 ;
            sio:SIO_000300 "[% hit_stop_pos %]"^^<http://www.w3.org/2001/XMLSchema#int>
          ] ;

  # "is part of"
  sio:SIO_000068 _:hitSeq ;

  # "has value"
  sio:SIO_000300 "[% hit_subseq %]" .

# this is the query sequence
<[% query_seq_uri %]>
     a       blast:BLASTedSequence ;
     
     # "has part"
     sio:SIO_000028 _:querySubseq .

# this is a match sequence
_:hitSeq

     # "protein sequence"
     a       sio:SIO_010015 ;
   
     # "is attribute of"
     sio:SIO_000011 <[% hit_protein_uri %]> ;
   
     # "has part"
     sio:SIO_000028 _:hitSubseq .

# this is the entity that the match sequence is from
<[% hit_protein_uri %]>

     # "has attribute"
     sio:SIO_000008 _:hitSeq .    
      
HEREDOC

use constant BLAST_INVOCATION_N3_TEMPLATE => <<'HEREDOC';

@prefix rdfs:    <http://www.w3.org/2000/01/rdf-schema#> .
@prefix blast:   <http://sadiframework.org/ontologies/blast.owl#> .
@prefix sio:     <http://semanticscience.org/resource/> .
@prefix rdf:     <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .

# this node represents the particular invocation of BLAST that resulted
# in the output

<[% blast_invocation_uri %]>

  # SIO_000667 is "software execution"
  a       sio:SIO_000667 ;

  # "has attribute"
  sio:SIO_000008

          # "start time"
          [ a       sio:SIO_000669 ;
            sio:SIO_000300 "[% start_time %]"^^<http://www.w3.org/2001/XMLSchema#dateTime>
          ] ;

  # "has attribute"
  sio:SIO_000008

          # "end time"
          [ a       sio:SIO_000670 ;
            sio:SIO_000300 "[% end_time %]"^^<http://www.w3.org/2001/XMLSchema#dateTime>
          ] ;

  # "has agent"
  sio:SIO_000139

          # "software application"
          # this node should have a URI, if we are able to map the
          # string label to a known application...
          [ a       sio:SIO_000101 ;
            
            # this label is all we know from the BLAST report
            rdfs:label "PSI-BLAST"^^<http://www.w3.org/2001/XMLSchema#string> ;

            # "has attribute"
            sio:SIO_000008

                    # "version identifier"
                    [ a       sio:SIO_000653 ;
                      sio:SIO_000300 "[% blast_version %]"^^<http://www.w3.org/2001/XMLSchema#string>
                    ]
          ] ;

  # "has input"
  sio:SIO_000230
          [ rdfs:label "[% database_name %]"^^<http://www.w3.org/2001/XMLSchema#string>
          ] .

HEREDOC

#-----------------------------------------------------------------
# SERVICE IMPLEMENTATION
#-----------------------------------------------------------------

use RDF::Trine::Node::Resource;
use RDF::Trine::Node::Literal;
use RDF::Trine::Statement;
use RDF::Trine::Namespace qw(rdf);
use RDF::Trine::Parser;
use Log::Log4perl;
use Data::UUID;
use Template;
use Bio::Tools::Run::StandAloneBlastPlus;
use Bio::Seq;
use Bio::Seq;
use DateTime;
use DateTime::Format::XSD;

=head2 process_it

 Function: implements the business logic of a SADI service
 Args    : $inputs - ref to an array of RDF::Trine::Node::Resource
           $input_model - an RDF::Trine::Model containing the input RDF data
           $output_model - an RDF::Trine::Model containing the output RDF data
 Returns : nothing (service output is stored in $output_model)

=cut

sub process_it {

    my ($self, $inputs, $input_model, $output_model) = @_;

    my $sio = RDF::Trine::Namespace->new('http://semanticscience.org/resource/');
    my $blast = RDF::Trine::Namespace->new('http://sadiframework.org/ontologies/blast.owl#');

    my $templater = Template->new();
    my $parser = RDF::Trine::Parser->new('turtle');
    my $blast_runner = Bio::Tools::Run::StandAloneBlastPlus->new(
        -db_name => $blast_config->{Database},
        -prog_dir => $blast_config->{BinaryDir},
    );
    

    foreach my $input (@$inputs) {
        
        INFO(sprintf('processing input %s', $input->uri));

        my ($seq) = $input_model->objects($input, $sio->SIO_000300);

        if (!$seq || !$seq->is_literal) {
            WARN('skipping input %s, doesn\'t have an attached amino acid sequence', $input->uri);
            next;
        }
        
        my $bioseq = Bio::Seq->new(-seq => $seq->value);

        my $start_time = DateTime->now;

        my $result = $blast_runner->psiblast(
            -query => $bioseq, 
            -method_args => [-num_iterations => $blast_config->{NumIterations}],
        );

        my $last_iteration = $result;
        while(my $iteration = $blast_runner->next_result) { $last_iteration = $iteration };

        my $end_time = DateTime->now;
       
        # add provenance info for BLAST invocation
        
        my $blast_invocation_uri = gen_uuid_uri();

        my $n3; 
        my $template = BLAST_INVOCATION_N3_TEMPLATE;
        $templater->process(
            \$template,
            {
                blast_invocation_uri => $blast_invocation_uri,
                start_time => DateTime::Format::XSD->format_datetime($start_time),
                end_time   => DateTime::Format::XSD->format_datetime($end_time),
                blast_version => $blast_config->{BlastVersion},
                database_name => $blast_config->{DatabaseName},
            },
            \$n3,
        );

        load_n3($n3, $output_model);

        while (my $hit = $last_iteration->next_hit) {
            
            my $hit_uri = gen_uuid_uri();
            my $hit_node = RDF::Trine::Node::Resource->new($hit_uri);
            $output_model->add_statement(RDF::Trine::Statement->new($hit_node, $rdf->type, $blast->BlastHit));

            while (my $hsp = $hit->next_hsp) {

                my $n3;
                my $template = ALIGNMENT_N3_TEMPLATE;

                $templater->process(
                    \$template, 
                    {
                        blast_invocation_uri => $blast_invocation_uri,
                        blast_hit_uri => $hit_uri,
                        bitscore => $hsp->bits,
                        evalue => $hsp->evalue,
                        score => $hsp->score,
                        consensus_seq => $hsp->homology_string,
                        percent_identity => $hsp->percent_identity,
                        query_start_pos => $hsp->start('query'),
                        query_stop_pos => $hsp->end('query'),
                        query_seq_uri => $input->uri,
                        query_subseq => $hsp->query_string,
                        hit_subseq => $hsp->hit_string,
                        hit_start_pos => $hsp->start('hit'),
                        hit_stop_pos => $hsp->end('hit'),
                        hit_protein_uri => sprintf("http://lsrn.org/UniProt:%s", $hit->accession)
                    },
                    \$n3,
                );

                load_n3($n3, $output_model);
                
            }
 
        }

        $blast_runner->cleanup; # remove temp files

    }

}

# Workaround for RDF::Trine bug -- if you load separate N3 strings into
# a model that use the same blank node labels, the blank 
# nodes will be merged. (Blank node labels should not have any significance
# across datasets.)

sub load_n3
{
    my ($n3, $model) = @_;

    my $n3_parser = RDF::Trine::Parser->new('turtle');
    my $rdfxml_parser = RDF::Trine::Parser->new('rdfxml');
    my $rdfxml_serializer = RDF::Trine::Serializer->new('rdfxml');

    my $temp_model = RDF::Trine::Model->temporary_model;
    $n3_parser->parse_into_model(undef, $n3, $temp_model);
    my $rdfxml = $rdfxml_serializer->serialize_model_to_string($temp_model);
    $rdfxml_parser->parse_into_model(undef, $rdfxml, $model);
}

sub gen_uuid_uri
{
    my $uuid = Data::UUID->new();
    return sprintf('urn:uuid:%s', lc($uuid->to_string($uuid->create())));
}
