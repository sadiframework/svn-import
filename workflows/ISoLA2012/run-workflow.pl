#!/usr/bin/perl

use strict;
use warnings;

#----------------------------------------------------------------------
# imports
#----------------------------------------------------------------------

use autodie qw(:all);
use constant::boolean;
use Getopt::Long;

#----------------------------------------------------------------------
# usage
#----------------------------------------------------------------------

use constant USAGE => <<HEREDOC;
USAGE 1: $0 input.sequence.n3
USAGE 2: $0 --resume

The --resume function works by checking the existence of the particular output files 
generated at each step of the workflow.  
HEREDOC

#----------------------------------------------------------------------
# service URLs
#----------------------------------------------------------------------

use constant SERVICE_HOST => 'http://184.73.172.249/';

use constant BLAST_BASE_URL => SERVICE_HOST . 'cgi-bin/sadi/blastp/';
use constant BLAST_YEAST_URL => BLAST_BASE_URL . 'S_cerevisiae.pl';
use constant BLAST_HUMAN_URL => BLAST_BASE_URL . 'H_sapiens.pl';

use constant IDMAPPER_BASE_URL => SERVICE_HOST . 'sadi/idmapper/';
use constant UNIPROT_TO_SGD_URL => IDMAPPER_BASE_URL . 'UniProt-to-SGD';
use constant SGD_TO_UNIPROT_URL => IDMAPPER_BASE_URL . 'SGD-to-UniProt';

use constant INTERACTION_BASE_URL => SERVICE_HOST . 'sadi/interaction-services/';
use constant YEAST_INTERACTION_URL => INTERACTION_BASE_URL . 'RetrieveYeastInteractors';

use constant UNIPROT_INFO_URL => 'http://sadiframework.org/examples/uniprotInfo';

#----------------------------------------------------------------------
# workflow declarations
#----------------------------------------------------------------------

my @yeast_workflow = (
    "Yeast Workflow",
    { 
        service_url => BLAST_YEAST_URL,
        description => 'Find homologs of input sequence in yeast (BLAST)', 
        output_file => 'homolog-uniprot-ids.n3',
    },
    {
        service_url => UNIPROT_TO_SGD_URL,
        description => 'Map UniProt IDs of homologs to SGD IDs',
        output_file => 'homolog-sgd-ids.n3',
    },
    {
        service_url => YEAST_INTERACTION_URL,
        description => 'Retrieve interactors of homologs (SGD IDs)',
        output_file => 'interactor-sgd-ids.n3',
    },
    {
        service_url => SGD_TO_UNIPROT_URL,
        description => 'Map SGD IDs of interactors to UniProt IDs',
        output_file => 'interactor-uniprot-ids.n3',
    },
    {
        service_url => UNIPROT_INFO_URL,
        description => 'Retrieve sequences of interactors',
        output_file => 'interactor-sequences.n3',
    },
    {
        service_url => BLAST_HUMAN_URL,
        description => 'Retrieve homologs of interactors in query organism (BLAST)',
        output_file => 'interactor-homologs.n3',
    }
);

#----------------------------------------------------------------------
# process commandline args
#----------------------------------------------------------------------

my $opt_resume = FALSE;

my $getopt_success = GetOptions(
    'resume' => \$opt_resume,
);

die USAGE unless $getopt_success;
die USAGE unless (@ARGV == 1 || $opt_resume);

#----------------------------------------------------------------------
# main
#----------------------------------------------------------------------

if ($opt_resume) {
    resume_workflow(@yeast_workflow);
} else {
    run_workflow(1, $ARGV[0], @yeast_workflow);
}

#----------------------------------------------------------------------
# subroutines
#----------------------------------------------------------------------

sub resume_workflow 
{
    my ($workflow_name, @workflow) = @_;
    my $resume_step = 1;
    my $input_file;
    foreach my $step (@workflow) {
        last unless -e $step->{output_file};
        $input_file = $step->{output_file}; 
        $resume_step++;
    }
    if ($resume_step == 1) {
        die "No steps completed successfully on previous execution of workflow. " .
            "Please re-run without the --resume option.\n";
    }
    if ($resume_step > @workflow) {
        warn sprintf(
            "Skipping execution of %s. All steps completed successfully on previous execution.\n", 
            $workflow_name
        ); 
        return;
    }
    warn sprintf("Resuming %s at Step %d...\n", $workflow_name, $resume_step);
    run_workflow($resume_step, $input_file, $workflow_name, @workflow);
}

sub run_workflow
{
    my ($start_step, $input_file, $workflow_name, @workflow) = @_;
    my $step_count = 1;
    foreach my $step (@workflow) {
        if ($step_count >= $start_step) {
            log_workflow_step($workflow_name, $step_count, $input_file, $step);
            invoke_sadi_service(
                $step->{service_url},
                $input_file,
                $step->{output_file},
                'N3',
                'N3',
            );
        }
        $input_file = $step->{output_file};
        $step_count++;
    }
}

sub log_workflow_step
{
    my ($workflow_name, $step_count, $input_file, $step) = @_;
    warn '-'x70 . "\n";
    warn sprintf("%s, Step %d\n", $workflow_name, $step_count);
    warn sprintf("\tDescription: %s\n", $step->{description});
    warn sprintf("\tInput file: %s\n", $input_file);
    warn sprintf("\tService URL: %s\n", $step->{service_url});
    warn sprintf("\tOutput file: %s\n", $step->{output_file});
    warn '-'x70 . "\n";
}

sub invoke_sadi_service 
{
    my ($url, $input_file, $output_file, $input_format, $output_format) = @_;
    run (
        'java',
        '-jar', 'sadi-client-commandline-0.0.4.jar',
        '-I', $input_format,
        '-O', $output_format,
        '-i', $input_file,
        '-o', $output_file,
        $url
    ); 
}

sub run 
{
    warn join(' ', @_) . "\n";
    die if system (@_);
}
