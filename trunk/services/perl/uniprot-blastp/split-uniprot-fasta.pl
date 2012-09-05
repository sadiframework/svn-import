#!/usr/bin/perl

use strict;
use warnings;

#----------------------------------------------------------------------
# imports
#----------------------------------------------------------------------

use Getopt::Long;
use constant::boolean;
use File::Spec::Functions qw(catfile);

#----------------------------------------------------------------------
# constants
#----------------------------------------------------------------------

use constant USAGE => <<HEREDOC;
USAGE: $0 [options] <uniprot-fasta-file-1> [uniprot-fasta-file-2] ...

OPTIONS:

--list-organisms            don't generate any files, just list the organism names in the input UniProt FASTA file(s).
--organism <ORGANISM-NAME>  generate an output FASTA file for the specified organism only. The organism name must match a prefix of an organism name that is used in the input UniProt FASTA file(s). This option may be used multiple times to generate FASTA files for any subset of the available organisms. If the user does not specify any --organism switches, the script will generate a separate FASTA file for every organism in the input UniProt FASTA file(s).
--target-dir                the output directory for the generated FASTA files. By default, the files will be created in the current working directory.
HEREDOC

#----------------------------------------------------------------------
# parse command line args
#----------------------------------------------------------------------

my $opt_help = FALSE;
my $opt_list_organisms = FALSE;
my @opt_organism;
my $opt_target_dir = '.';

my $getopt_success = GetOptions(
    'help', \$opt_help,
    'list-organisms', \$opt_list_organisms,
    'organism=s', \@opt_organism,
    'target-dir=s', \$opt_target_dir,
);

die USAGE unless $getopt_success;
if ($opt_help) { print USAGE; exit 0; }

#----------------------------------------------------------------------
# check input file(s)
#----------------------------------------------------------------------

foreach my $file (@ARGV) {
    die "$file doesn't exist or isn't readable\n" unless -f -r $file;
}

#----------------------------------------------------------------------
# traverse input file(s)
#----------------------------------------------------------------------

my $output_on = FALSE;
my %organisms = ();
my %output_fasta_files = ();
my $fh;
my $match_count = 0;
while(<>) {
    if (/^>.*OS=(.+?)\s+GN/) {
        my $organism = $1;
#warn "organism: $organism\n";
        my $organism_abbrev = get_organism_abbrev($organism);
#warn "organism_abbrev: $organism_abbrev\n";
        $organisms{$organism} = 1;
        unless ($opt_list_organisms) {
            if (@opt_organism == 0 || grep($organism =~ m/^\Q$_\E/, @opt_organism)) {
#warn "match: $organism\n";
#warn "match_count: " . $match_count++ . "\n";
                unless ($output_fasta_files{$organism_abbrev}) {
                    my $output_file_path = catfile($opt_target_dir, $organism_abbrev . '.fasta');
warn "opening $output_file_path for writing\n";
                    open(my $fh, '>', $output_file_path) or die "unable to open $output_file_path for writing: $?\n";
                    $output_fasta_files{$organism_abbrev} = $fh;
                }
                $fh = $output_fasta_files{$organism_abbrev};
                $output_on = TRUE;
            } else {
                $output_on = FALSE;
            }
        }
    }
    print $fh $_ if $output_on;
}

if ($opt_list_organisms) {
    print join("\n", sort(keys %organisms)) . "\n";
}

#----------------------------------------------------------------------
# cleanup
#----------------------------------------------------------------------

foreach my $fh (keys %output_fasta_files) {
    close($fh);
}

#----------------------------------------------------------------------
# subroutines
#----------------------------------------------------------------------

sub get_organism_abbrev
{
    my $organism_name = shift @_;
    if ($organism_name =~ /^(\S+)\s+(\S+)/) {
        my $major = $1;
        my $minor = $2;
        # Bio::Tools::Run::StandAloneBlastPlus can't handle '.'s in BLAST database filenames, so we use '_' everywhere instead
        return substr($major, 0, 1) . '_' . $minor;
    } else {
        return $organism_name;
    }
}
