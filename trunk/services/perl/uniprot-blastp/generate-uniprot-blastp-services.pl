#!/usr/bin/perl

use strict;
use warnings;

#----------------------------------------------------------------------
# imports
#----------------------------------------------------------------------

use Getopt::Long;
use File::Spec::Functions qw(catfile);
use File::Which qw(which);
use File::Basename;
use Template;
use Cwd qw(abs_path);
use constant::boolean;

#----------------------------------------------------------------------
# constants
#----------------------------------------------------------------------

use constant USAGE => <<HEREDOC;
USAGE: $0 [options] <target-directory-for-blast-databases> <target-directory-for-cgi-scripts> <uniprot-fasta-file-1> [uniprot-fasta-file-2] ...

OPTIONS:

--blast-binary-dir <DIR>      Location of BLAST binaries, which can be downloaded from NCBI. This option is necessary if the BLAST binaries aren't on the current PATH.  The 'makeblastdb' binary is required for building the BLAST databases and the 'blastp' binary is required to do the BLAST queries when the services are invoked.

--organism <ORGANISM-NAME>    Build a BLAST service for the specified organism only.  By default, the script will build a separate BLAST service for every organism in the input FASTA file(s).  The specified organism name must exactly match an organism name that appears in one or more input FASTA file(s).
HEREDOC

use constant CGI_SCRIPT_TEMPLATE_FILE => 'uniprot-blastp.pl.template';

#----------------------------------------------------------------------
# process command-line args
#----------------------------------------------------------------------

my $blast_binary_dir;
if (which('makeblastdb')) {
    $blast_binary_dir = abs_path((fileparse(which('makeblastdb')))[1]);
}

my $opt_help = FALSE;
my @opt_organism;

my $getopt_success = GetOptions(
    'help', \$opt_help,
    'blast-binary-dir=s' => \$blast_binary_dir,
    'organism=s' => \@opt_organism,
);

die USAGE unless $getopt_success;
if ($opt_help) { print USAGE; exit 0; }
die USAGE unless (@ARGV >= 3);

my $target_blastdb_dir = abs_path(shift @ARGV);
my $target_cgi_dir = abs_path(shift @ARGV);
my @input_fasta_files = @ARGV;

#----------------------------------------------------------------------
# check for required binaries
#----------------------------------------------------------------------

my $makeblastdb_path;
my $blastp_path;

$makeblastdb_path = catfile($blast_binary_dir, 'makeblastdb');
$blastp_path = catfile($blast_binary_dir, 'blastp');

unless (-x $makeblastdb_path && -x $blastp_path) {
    die "Error: The 'makeblastdb'/'blastp' binaries were either: not found,  not executable, or not in the same directory. If the binaries are not on the current PATH, you must specify their location using the --blast-binary-dir option.\n";
} 

#----------------------------------------------------------------------
# extract separate FASTA files for specified organisms, or all organisms
# if no --organism switches were used
#----------------------------------------------------------------------

run(
    './split-uniprot-fasta.pl', 
    '--target-dir', $target_blastdb_dir, 
    map(('--organism', $_), @opt_organism), 
    @ARGV,
);

#----------------------------------------------------------------------
# build BLAST databases
#----------------------------------------------------------------------

my @fasta_files = <$target_blastdb_dir/*.fasta>;

foreach my $fasta_file (@fasta_files) {
    my ($basename, $path) = fileparse($fasta_file, '.fasta');
    run(
        $makeblastdb_path,
        '-dbtype', 'prot',
        '-in', $fasta_file,
        '-input_type', 'fasta',
        '-title', $basename,
        '-out', catfile($path, $basename),
        '-parse_seqids',
    );
}

#----------------------------------------------------------------------
# generate SADI CGI scripts
#----------------------------------------------------------------------

my $templater = Template->new();
my @blastdb_files = <$target_blastdb_dir/*.psq>;

foreach my $blastdb_file (@blastdb_files) {
    my ($basename, $path) = fileparse($blastdb_file, '.psq');
    my $output_file = catfile($target_cgi_dir, "$basename.pl");
    warn "generating output file: $output_file\n";
    $templater->process(
        CGI_SCRIPT_TEMPLATE_FILE,
        {
            SPECIES_ABBREV => $basename,
            BLAST_DATABASE_DIR => $target_blastdb_dir,
            BLAST_DATABASE_NAME => $basename,
            BLAST_BINARY_DIR => $blast_binary_dir,
        },
        $output_file,
    ) or die $templater->error();
    chmod(0755, $output_file) or die "unable to set read/execute perms for $output_file: $?";
}



#----------------------------------------------------------------------
# subroutines
#----------------------------------------------------------------------

sub run 
{
    print join(' ', @_) . "\n";
    system(@_);
}

