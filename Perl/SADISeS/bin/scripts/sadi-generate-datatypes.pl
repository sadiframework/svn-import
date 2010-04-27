#!/usr/bin/perl -w
#
# Generate perl modules from OWL files.
#
# $Id: sadi-generate-datatypes.pl,v 1.70 2010-02-11 18:16:44 ubuntu Exp $
# Contact: Edward Kawas <edward.kawas+SADI@gmail.com>
# -----------------------------------------------------------
# some command-line options
use Getopt::Std;
use vars qw/ $opt_h $opt_b $opt_u $opt_d $opt_v $opt_s $opt_i $opt_F $opt_o/;
getopts('hudvsFibo:');

# usage
if ( &check_odo() or $opt_h or @ARGV == 0 ) {
	print STDOUT <<'END_OF_USAGE';
Generate perl modules from OWL files.
Usage: [-vdsib] [-o outdir] owl-class-file
       [-vdsi] [-o outdir] -u owl-class-url

    -u ... owl is from url
    -s ... show generated code on STDOUT
           (no file is created, disabled when no data type name given)

    -b ... option to specify the base uri for the owl document (you will be prompted)
    
    -i ... follow owl import statements
    
    -v ... verbose
    -d ... debug
    -h ... help

Note: This script requires that the PERL module ODO, from IBM Semantic Layered
      Research Platform be installed on your workstation! ODO is available on CPAN
      as PLUTO.

END_OF_USAGE
	exit(0);
}

sub check_odo {
	eval "require PLUTO";
	if ($@) {
		print STDOUT
		  "Module PLUTO not installed and is required for this script.\n";
		print STDOUT "Should I proceed? [n] ";
		my $tmp = <STDIN>;
		$tmp =~ s/\s//g;
		exit() unless $tmp =~ /y/i;
	}
}

# -----------------------------------------------------------
use strict;
use warnings;
use SADI::Base;
use SADI::Utils;
use File::Spec;
use FindBin qw( $Bin );
use lib $Bin;
use OWL2Perl;
use Data::Dumper;
$LOG->level('INFO')  if $opt_v;
$LOG->level('DEBUG') if $opt_d;
sub say { print @_, "\n"; }
my %imports_added;
say "Using SAX parser $SADICFG::XML_PARSER"
  if defined $SADICFG::XML_PARSER and $opt_v;
my $owl2perl = OWL2Perl->new();

# set the output dir unless we are outputting to STDOUT
unless ($opt_s) {

	# set the outdir
	$owl2perl->outdir( $SADICFG::GENERATORS_OUTDIR
					   || SADI::Utils->find_file( $Bin, 'generated' ) )
	  unless $opt_o;
	$owl2perl->outdir($opt_o) if $opt_o;

	# tell people where the output is going
	say sprintf( "Output is going to %s\n", $owl2perl->outdir() );
}

# set whether or not we follow imports
$owl2perl->follow_imports( $opt_i ? 1 : 0 );

# set whether or not we overwrite files
$owl2perl->force( $opt_F ? 1 : 0 );

# owl_urls contain all owl files (as urls or file paths) to be parsed
# base_uris will hold the base_uris
my ( @owl_urls, @base_uris );
my $counter = -1;
if (@ARGV) {
	foreach my $arg (@ARGV) {
		say "Processing OWL file: $arg\n";
		my $base_uri = undef;
		if ($opt_b) {
			print STDOUT "Please specify the base uri for $arg: ";
			my $tmp = <STDIN>;
			$tmp =~ s/\s//g;
			chomp($tmp);
			# strip # from end if it exists
			$tmp =~ s/#*$//gi;
			$base_uri = $tmp;
		}
		unless ($opt_u) {
			# make a url out of the file path
			$arg = File::Spec->rel2abs($arg)
			  unless File::Spec->file_name_is_absolute( $arg );
		}
		# add the url and base_uri to our arrays
		$counter++;
        $owl_urls[$counter] = $arg;
		$base_uris[$counter] = $base_uri;
	}
	say('Aggregating ontologies ...') if $opt_v;
	my $ontology = $owl2perl->process_owl(\@owl_urls, \@base_uris);
	
	# generate the CODE
	say('Generating PERL modules from the OWL documents ...') if $opt_v;
	if ($opt_s) {
		my $code = '';
		$owl2perl->generate_datatypes($ontology, \$code);
		print STDOUT $code;
	} else {
		$owl2perl->generate_datatypes($ontology);
	}
}
say 'Done.';
__END__
