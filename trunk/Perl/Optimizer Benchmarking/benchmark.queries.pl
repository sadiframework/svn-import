#! /usr/bin/perl -w

use strict;
use File::Basename;
use Getopt::Long;
use Switch;

#--------------------------------------------------
# Constants
#--------------------------------------------------

my $EXIT_CODE_SUCCESS = 0;
my $EXIT_CODE_NO_RESULTS = 1;
my $EXIT_CODE_FAILURE = 2;

my $USAGE = "USAGE: ./benchmark.queries.pl [--training-query training.query1.file --training-query training.query2.file ...] [--test-query test.query1.file --test-query test.query2.file ...] [--training-runs <INT> ] [--test-runs <INT>] [--trials-per-test-run <INT>] [--samples-graph <URI>] [--summary-stats-graph <URI>] <share-standalone-jar> <stats endpoint URL> <stats endpoint username> <stats endpoint password>";

my @trainingQueries = ();
my @testQueries = ();
my $numTestRuns = 5;
my $numTrainingRuns = 3;
my $generatedFilePrefix = "";
my $clearStats = 0;
my $numTrialsPerTestRun = 3;
my $shareJar;
my $samplesGraph;
my $summaryStatsGraph;
my $statsEndpointURL;
my $statsUsername;
my $statsPassword;

#--------------------------------------------------
# Parse commandline
#--------------------------------------------------

my $getoptSuccess = 
	GetOptions(
		"training-query=s" => \@trainingQueries,
		"test-query=s" => \@testQueries,
		"test-runs=i" => \$numTestRuns,
		"training-runs=i" => \$numTrainingRuns,
		"trials-per-test-run=i" => \$numTrialsPerTestRun,
		"prefix=s" => $generatedFilePrefix,
		"clear-stats" => \$clearStats,
		"samples-graph=s" => \$samplesGraph,
		"summary-stats-graph=s" => \$summaryStatsGraph,
    );

die "error parsing command line options:\n$USAGE\n" unless ($getoptSuccess);

if(@ARGV != 4) {
	die "Incorrect number of arguments. Did you provide a path to the SHARE jar and a stats endpoint/username/password?:\n$USAGE\n";	
}

($shareJar, $statsEndpointURL, $statsUsername, $statsPassword) = @ARGV;

# it is legal to run the script without training queries,
# or without test queries, but not both.

if(@trainingQueries == 0 || @testQueries == 0) {
    die "at least one training query file or test query file must be specified:\n$USAGE\n";
}

if(!defined($shareJar)) {
	die "you must specify the path to SHARE standalone jar with the --share-jar option:\n$USAGE\n";
}

if(length($generatedFilePrefix) > 0) {
	$generatedFilePrefix .= ".";
}

my @shareStatsOptions = (
	"--stats-endpoint", $statsEndpointURL,
	"--stats-username", $statsUsername,
	"--stats-password", $statsPassword
);

push(@shareStatsOptions, "--samples-graph", $samplesGraph) if defined($samplesGraph);
push(@shareStatsOptions, "--summary-stats-graph", $summaryStatsGraph) if defined($summaryStatsGraph);

#--------------------------------------------------
# Clear stats (if requested)
#--------------------------------------------------

if($clearStats) {
		
    msg("clearing stats");
    clearStats(@shareStatsOptions);

}

#--------------------------------------------------
# Generate random input query orderings for 
# training runs and test runs
#--------------------------------------------------

my %trainingQueryOrderings = ();
my %testQueryOrderings = ();

msg("generating random input query orderings for training runs");

foreach my $queryFile (@trainingQueries) {
	
	my ($basename) = fileparse($queryFile);
	my @generatedFiles = writeRandomQueryOrderingsToFile($queryFile, $numTrainingRuns, "${generatedFilePrefix}${basename}", @shareStatsOptions);
	$trainingQueryOrderings{ $queryFile } = \@generatedFiles;

}

msg("generating random input query orderings for test runs");

foreach my $queryFile (@testQueries) {

	my ($basename) = fileparse($queryFile);
	my @generatedFiles = writeRandomQueryOrderingsToFile($queryFile, $numTestRuns, "${generatedFilePrefix}${basename}", @shareStatsOptions);
	$testQueryOrderings{ $queryFile } = \@generatedFiles;

}
		
for(my $i = 0; $i <= $numTrainingRuns; $i++) {

	if($i > 0) {

	    #--------------------------------------------------
		# Run training queries
		#--------------------------------------------------

		foreach my $queryFile (@trainingQueries) {

			my $queryOrderingFile = $trainingQueryOrderings{ $queryFile }->[$i - 1];

			my ($basename) = fileparse($queryOrderingFile);
			my $filePrefix = "${generatedFilePrefix}${basename}";

			query($queryOrderingFile, $filePrefix, "--no-reordering", "--record-stats", @shareStatsOptions);

		}

		#--------------------------------------------------
		# Recompute statistics from samples
		#--------------------------------------------------

		msg("recomputing predicate stats");
		recomputeStats(@ARGV);
	
	}

	#--------------------------------------------------
	# Run test queries
	#--------------------------------------------------

    foreach my $queryFile (@testQueries) {

		for(my $j = 0; $j < $numTestRuns; $j++) {

			my $queryOrderingFile = $testQueryOrderings{ $queryFile }->[$j];

			if(!defined($queryOrderingFile)) {
				warn "NO QUERY ORDERINGS GENERATED FOR $queryFile";
				next;
			}

			my ($basename) = fileparse($queryOrderingFile);

			# On the first test run, time each trial with optimization off (for comparison).

			if($i == 0) {

				for(my $k = 0; $k < $numTrialsPerTestRun; $k++) {
					
					my $filePrefix = "${generatedFilePrefix}${basename}.no.opt.trial.${k}";

					msg("running $basename without optimization (trial $k)");
					my ($exitCode) = query($queryOrderingFile, $filePrefix, "--no-reordering", @shareStatsOptions);
					
					# don't repeat queries that fail, run out memory, etc.
					last if ($exitCode != $EXIT_CODE_SUCCESS);

				}

			}
			
			# Time each trial with optimation on.

			for(my $k = 0; $k < $numTrialsPerTestRun; $k++) {
				
				my $filePrefix = "${generatedFilePrefix}${basename}.opt.trial.${k}";
				
				msg("running $basename with optimization (trial $k)");
				my ($exitCode) = query($queryOrderingFile, $filePrefix, "--optimize", @shareStatsOptions);

				# don't repeat queries that fail, run out memory, etc.
				last if ($exitCode != $EXIT_CODE_SUCCESS);

			}
			
		} # for each test run

    } # for each test query
   
} # for each training run


#--------------------------------------------------
# Subroutines
#--------------------------------------------------

sub share
{
    my @shareOptions = @_;

    my $options = join(" ", @shareOptions);

	my $commandString = "java -Xmx2000M -jar $shareJar $options";	
	msg("running SHARE: $commandString");
    my $stdout = qx($commandString);

    return (($? >> 8), $stdout);
}

sub clearStats
{
    my @shareOptions = @_;

    my ($exitCode) = share(
	    "--clear-stats",
	    "--log-file", "${generatedFilePrefix}clear.stats.log",
	    @shareOptions
		);

    die "ERROR OCCURRED DURING CLEARING OF STATS\n" unless ($exitCode == $EXIT_CODE_SUCCESS);
}

sub recomputeStats
{
    my @shareOptions =  @_;

    my ($exitCode) = share(
	    "--recompute-stats",
	    "--log-file", "${generatedFilePrefix}recomputestats.log",
	    @shareOptions
		);

    die "ERROR OCCURRED DURING COMPUTATION OF STATS\n" unless ($exitCode == $EXIT_CODE_SUCCESS);
}

sub writeRandomQueryOrderingsToFile
{
	my ($queryFile, $numOrderings, $filePrefix, @shareOptions) = @_;
	
    # This command generates a list of SPARQL queries on STDOUT
    my ($exitCode, $queryList) = share(
	    "--enumerate-orderings",
	    "--query-file", $queryFile,
	    @shareOptions
		);

	if($exitCode != $EXIT_CODE_SUCCESS) {
    	warn "ERROR: QUERY ENUMERATION FAILED FOR $queryFile"; 
		return ();
	}

    # Note: this is Unix-dependent
    my @queries = split(/\n\n\n/, $queryList);
	my @generatedFiles = ();

	if(@queries == 0) {
		warn "ERROR: ZERO RESOLVABLE QUERY ORDERINGS FOR $queryFile";
		return ();
	}

	for(my $i = 0; $i < $numOrderings; $i++) {
    
		my $queryIndex = int(rand(@queries));

		my $query = $queries[ $queryIndex ];
		my $filename = "$filePrefix.ordering.$i";

		writeStringToFile($query, $filename);

		push(@generatedFiles, $filename);

	}

	return @generatedFiles;
}

sub query
{
    my ($queryFile, $generatedFilePrefix, @shareOptions) = @_;

	my ($basename) = fileparse($queryFile);

	my $fh;
	if(!open($fh, "<", $queryFile)) {
		warn "ABORTING QUERY EXECUTION, COULD NOT OPEN FILE $queryFile";
		return;
	}

    msg("running $basename:\n" . join("", <$fh>) . "\n");
    
    my $startTime = time;

    my ($exitCode) = share(
	    "--query-file", $queryFile,
	    "--log-file", "$generatedFilePrefix.log",
	    "--output-file", "$generatedFilePrefix.results",
	    @shareOptions
		);

    my $elapsedTime = time - $startTime;

    msg("query $basename finished in $elapsedTime seconds");

    switch($exitCode) {
		case { $EXIT_CODE_SUCCESS } { msg("query completed successfully"); }
		case { $EXIT_CODE_NO_RESULTS } { msg("QUERY RETURNED NO RESULTS"); $elapsedTime .= "(NO_RESULTS)"; }
		case { $EXIT_CODE_FAILURE } { msg("QUERY FAILED DURING EXECUTION"); $elapsedTime .= "(ERROR)"; }
		else { msg("unrecognized return code from $shareJar"); }
    }

	writeStringToFile($elapsedTime, "$generatedFilePrefix.time");

    return ($exitCode, $elapsedTime);
}

sub msg
{
    warn "=> " . shift(@_) . "\n";
}

sub writeStringToFile 
{
    my ($string, $filename) = @_;

    open(my $filehandle, ">", $filename) or die "unable to open $filename for writing: $!";
    print $filehandle $string;
    close($filehandle) or warn "error closing $filename: $!";
}

