#! /usr/bin/perl -w

use strict;
use File::Basename;
use Getopt::Long;
use Text::ParseWords;
use Switch;

#--------------------------------------------------
# Constants
#--------------------------------------------------

my $EXIT_CODE_SUCCESS = 0;
my $EXIT_CODE_NO_RESULTS = 1;
my $EXIT_CODE_FAILURE = 2;
my $EXIT_CODE_TIMEOUT = 3;

my %STATUS_STRING = (
	$EXIT_CODE_SUCCESS => "SUCCESS",
	$EXIT_CODE_NO_RESULTS => "NO RESULTS",
	$EXIT_CODE_FAILURE => "ERROR",
	$EXIT_CODE_TIMEOUT => "TIMEOUT",
	);

# in seconds
my $QUERY_TIMEOUT = 60 * 60;

my $USAGE = "USAGE: ./benchmark.queries.pl [--training-query training.query1.file --training-query training.query2.file ...] [--test-query test.query1.file --test-query test.query2.file ...] [--training-runs <INT> ] [--test-runs <INT>] [--trials-per-test-run <INT>] <share-standalone-jar> <statsdb-jar> <stats endpoint URL> <stats endpoint username> <stats endpoint password> <samples graph URI prefix> <summary stats graph URI prefix>";

my $RESUME_ARGS_FILE = "resume.args";

my @trainingQueries = ();
my @testQueries = ();
my $numTestRuns = 5;
my $numTrainingRuns = 3;
my $generatedFilePrefix = "";
my $clearStats = 1;
my $numTrialsPerTestRun = 3;
my $shareJar;
my $statsdbJar;
my $samplesGraphPrefix;
my $summaryStatsGraphPrefix;
my $statsEndpointURL;
my $statsUsername;
my $statsPassword;
my $resume = 0;

#--------------------------------------------------
# Parse commandline
#--------------------------------------------------

# remember original command line args for --resume
my @origARGV = @ARGV;

my %options = (
	"training-query=s" => \@trainingQueries,
	"test-query=s" => \@testQueries,
	"test-runs=i" => \$numTestRuns,
	"training-runs=i" => \$numTrainingRuns,
	"trials-per-test-run=i" => \$numTrialsPerTestRun,
	"prefix=s" => $generatedFilePrefix,
	"clear-stats!" => \$clearStats,
	"resume" => \$resume,
);

my $getoptSuccess = GetOptions(%options);

die "error parsing command line options:\n$USAGE\n" unless ($getoptSuccess);

if($resume) {

	# restore ARGV to reflect previous invocation of script
	@ARGV = shellwords(readStringFromFile($RESUME_ARGS_FILE));

	my $getoptSuccess = GetOptions(%options);
	die "error parsing command line options from $RESUME_ARGS_FILE:\n$USAGE\n" unless ($getoptSuccess);

	msg("resuming execution of previous benchmarking run");

}

if(@ARGV != 7) {
	die "Incorrect number of arguments. Did you provide a path to the SHARE jar, Stats DB jar, and a stats endpoint/username/password/graph prefixes?:\n$USAGE\n";	
}

($shareJar, $statsdbJar, $statsEndpointURL, $statsUsername, $statsPassword, $samplesGraphPrefix, $summaryStatsGraphPrefix) = @ARGV;

# it is legal to run the script without training queries,
# or without test queries, but not both.

if(@trainingQueries == 0 && @testQueries == 0) {
	die "at least one training query file or test query file must be specified:\n$USAGE\n";
}

if(length($generatedFilePrefix) > 0) {
	$generatedFilePrefix .= ".";
}

# Options we want to use every time we use the SHARE jar.

my @commonShareOptions = (
	"--no-reasoning",
	"--stats-endpoint", $statsEndpointURL,
	"--stats-username", $statsUsername,
	"--stats-password", $statsPassword,
);

# Options we want to use every time we use the Stats DB jar.

my @commonStatsOptions = (
	"--username", $statsUsername,
	"--password", $statsPassword,
);

# At this point, we know the use hasn't made any errors 
# with command line switches/arguments.  Remember 
# the original commandline arguments for use by
# --resume.

if(!$resume) {
	msg("removing files generated from previous benchmarking run");
	unlink <*.complete>;
	unlink <*.last>;
	unlink <*.time>;
	unlink <*ordering*log>;
	unlink <*variant*log>;
	unlink <*.exit.status>;
	unlink <*.results>;
	writeStringToFile(join(" ", @origARGV), $RESUME_ARGS_FILE);
}

#--------------------------------------------------
# Clear stats (if requested)
#--------------------------------------------------

if($clearStats && !$resume) {

	msg("clearing stats");

	for(my $i = 1; $i <= $numTrainingRuns; $i++) {
		
		msg("clearing <" . samplesGraphURI($i) . "> and <" . summaryStatsGraphURI($i) . ">");

		clearStats(
			@commonStatsOptions,
			"--samples-graph", samplesGraphURI($i),
			"--summary-stats-graph", summaryStatsGraphURI($i),
			);
	}
}

#--------------------------------------------------
# Generate random input query orderings for 
# training runs and test runs
#--------------------------------------------------

msg("------------------------------------");
msg("GENERATING RANDOM QUERY ORDERINGS");
msg("------------------------------------");

my %trainingQueryOrderings = ();
my %testQueryOrderings = ();

my $orderingsCompleteFile = "orderings.complete";

if ($resume && (-e $orderingsCompleteFile)) {
	
	msg("resume: skipping generation of query orderings (completed on previous run)");

} else {

#	msg("generating random input query orderings for training runs");
#
#	foreach my $queryFile (@trainingQueries) {
#
#		my ($basename) = fileparse($queryFile);
#		my @generatedFiles = writeRandomQueryOrderingsToFile($queryFile, $numTrainingRuns, "${generatedFilePrefix}${basename}");
#
#	}

	msg("generating random input query orderings for test runs");

	foreach my $queryFile (@testQueries) {

		my ($basename) = fileparse($queryFile);
		my @generatedFiles = writeRandomQueryOrderingsToFile($queryFile, $numTestRuns, "${generatedFilePrefix}${basename}");

	}
	
}

touch($orderingsCompleteFile);

# NOTE: We run all of the training runs first, before running any of the
# test queries. We do this because it isn't possible to re-run a test query unless 
# all of the training queries that it depends on have completed successfully.
# (Failed queries are common due to services going down, etc.)

for(my $i = 1; $i <= $numTrainingRuns; $i++) {

	my @statsOptions = (
		"--samples-graph", samplesGraphURI($i),
		"--summary-stats-graph", summaryStatsGraphURI($i),
	);		

	msg("------------------------------------");
	msg("TRAINING RUN $i");
	msg("------------------------------------");

	my $trainingRunCompleteFile = "training.run.$i.complete";	

	# NOTE: If we are resuming and we didn't get all the way
	# through a training run, there's no way to pickup
	# at the exact query where we left off.  The problem
	# is that even a partially run query will record 
	# statistics, and so we must redo the whole training
	# run from scratch.  

	if($resume && (-e $trainingRunCompleteFile)) {

		msg("resume: skipping training run $i (completed on previous run)");

	} else {

		# see NOTE above
		if($resume) {
			msg("restarting training run $i from the beginning");
			msg("clearing <" . samplesGraphURI($i) . "> and <" . summaryStatsGraphURI($i) . ">");
			clearStats(@commonStatsOptions, @statsOptions);
		}

		#-------------------------------------------------
		# Copy samples from previous training run
		#------------------------------------------------

		# Samples from each training run are stored in separate graphs,
		# so that it is easy to re-run a benchmark query that occurred
		# at any point during the training.
		#
		# Samples should be cumulative across training runs though,
		# so we need to copy over the samples from the previous training
		# run.

		if($i > 1) {
			copy_graph(samplesGraphURI($i - 1), samplesGraphURI($i), $statsEndpointURL, $statsUsername, $statsPassword);
		}

		#--------------------------------------------------
		# Run training queries
		#--------------------------------------------------

		foreach my $queryFile (@trainingQueries) {

			my $queryOrderingFile = "$queryFile.variant$i";
			my ($basename) = fileparse($queryOrderingFile);
			my $filePrefix = "${generatedFilePrefix}${basename}";

			die "UNABLE TO FIND TRAINING QUERY FILE $queryOrderingFile" unless (-e $queryOrderingFile);

			msg("running training query $basename (training run $i)");

			# NOTE: We use the summary stats graph from the previous training run here, in 
			# order to provide the optimizer with stats that have been gathered in
			# previous training runs. 

			my $summaryStatsGraphURI = ($i > 1) ? summaryStatsGraphURI($i - 1) : summaryStatsGraphURI($i);

			query($queryOrderingFile, 
				$filePrefix, 
				"--record-stats", 
				"--optimize", 
				@commonShareOptions, 
				"--samples-graph", samplesGraphURI($i),
				"--summary-stats-graph", $summaryStatsGraphURI);

		}

		#--------------------------------------------------
		# Recompute statistics from samples
		#--------------------------------------------------

		msg("recomputing predicate stats");
		recomputeStats(@commonStatsOptions, @statsOptions);

		touch($trainingRunCompleteFile);

	} # if we aren't resuming, or can't resume after this training run

}

for(my $i = 0; $i <= $numTrainingRuns; $i++) {

	my @statsOptions = (
		"--samples-graph", samplesGraphURI($i),
		"--summary-stats-graph", summaryStatsGraphURI($i),
	);		

	#--------------------------------------------------
	# Run test queries
	#--------------------------------------------------

	msg("------------------------------------");
	msg("RUNNING TEST QUERIES ($i TRAINING RUNS)");
	msg("------------------------------------");

	foreach my $queryFile (@testQueries) {
	
		for(my $j = 0; $j < $numTestRuns; $j++) {

			my $queryOrderingFile = "$queryFile.ordering.$j";
			my ($basename) = fileparse($queryOrderingFile);
			my $filePrefix = "${generatedFilePrefix}${basename}";
			
			die "UNABLE TO FIND TEST QUERY FILE $queryOrderingFile" unless (-e $queryOrderingFile);

			# On the first test run, time each trial with optimization off (for comparison).

			if($i == 0) {

				msg("running non-optimized trials for $basename ($i training runs)");

				run_test_query_trials(
					$queryOrderingFile,
				   	$numTrialsPerTestRun,
				   	"${filePrefix}.no.opt",
				   	$resume,
				   	"--no-reordering",
					@commonShareOptions,
				   	@statsOptions);

			}

			# Time each trial with optimation on.

			msg("running optimized trials for $basename ($i training runs)");

			run_test_query_trials(
				$queryOrderingFile,
			   	$numTrialsPerTestRun,
			   	"${filePrefix}.test.run.${i}.opt",
			   	$resume,
				"--optimize",
				@commonShareOptions,
			   	@statsOptions);

		} # for each test run

	} # for each test query

} # for each training run

#--------------------------------------------------
# Subroutines
#--------------------------------------------------

sub copy_graph
{
	my ($sourceGraph, $targetGraph, $endpointURL, $username, $password) = @_;

	msg("copying contents of <$sourceGraph> to <$targetGraph>, in $endpointURL");

	my @copyCommand = (
		"curl",
		"-o", "/dev/null", # Unix specific
		"--silent",
		"--write-out", "'%{http_code}'",
		"-u", "'$username:$password'",
		"--anyauth",
		"--data-urlencode", "query='insert into graph <$targetGraph> { ?s ?p ?o } from <$sourceGraph> where { ?s ?p ?o }'",
		$endpointURL,
	);

	my $copyCommand = join(" ", @copyCommand);
	my $httpStatus = qx($copyCommand);

	die "error copying samples from $sourceGraph to $targetGraph: HTTP $httpStatus" unless ($httpStatus == 200);
}

sub run_test_query_trials
{

	my ($queryFile, $numTrials, $filePrefix, $resume, @shareOptions) = @_;

	my ($basename) = fileparse($queryFile);	

	for(my $i = 0; $i < $numTrials; $i++) {

		my $trialPrefix = "$filePrefix.trial.$i";
		my $completionFile = "$trialPrefix.complete";
		# created to indicate that the current trials were cut short due to failure/timeout
		my $lastFile = "$trialPrefix.last";

		if($resume && (-e $completionFile)) {
		
			if(-e $lastFile) {
				msg("resume: trial $i of $basename failed or timed out on previous run, skipping all remaining trials");
				last;
			}

			msg("resume: skipping trial $i of $basename (completed on previous run)");
			next;

		}

		msg("running $basename (trial $i)");
		my ($exitCode) = query($queryFile, $trialPrefix, @shareOptions);
		touch($completionFile);

		# don't repeat queries that fail or timeout

		if ($exitCode == $EXIT_CODE_FAILURE || $exitCode == $EXIT_CODE_TIMEOUT) {
			touch($lastFile);
			last;
		}

	}

}

sub touch
{
	my $filename = shift @_;
	open(my $fh, ">>", $filename) or die "unable to open $filename for writing: $!";
}

sub samplesGraphURI
{
	my $trainingRuns = shift @_;
	return "$samplesGraphPrefix/$trainingRuns.training.runs";
}

sub summaryStatsGraphURI
{
	my $trainingRuns = shift @_;
	return "$summaryStatsGraphPrefix/$trainingRuns.training.runs";
}

sub stats
{
	my ($endpointURL, @statsOptions) = @_;
	system("java", "-Xmx2000M", "-jar", $statsdbJar, @statsOptions, $endpointURL);
}

sub share
{
	my ($timeout, @shareOptions) = @_;

	my @command = ("java", "-Xmx3000M", "-jar", $shareJar, @shareOptions);	
	my $commandString = join(" ", @command);

	msg("running SHARE: $commandString");

	my $exitCode;
	my $elapsedTime;

	if($timeout > 0) {

		($exitCode, $elapsedTime) = runTimedCommand($timeout, @command);

	} else {

		my $startTime = time;
		system { $command[0] } @command;
		$exitCode = ($? >> 8);
		$elapsedTime = time - $startTime;

	}

	return ($exitCode, $elapsedTime);
}

sub clearStats
{
	my @statsOptions = @_;

#	my ($exitCode) = share(
#		0,
#		"--clear-stats",
#		"--log-file", "${generatedFilePrefix}clear.stats.log",
#		@shareOptions
#	);

	my $exitCode = stats($statsEndpointURL, @statsOptions, "--clear-all");	
	die "ERROR OCCURRED DURING CLEARING OF STATS\n" unless ($exitCode == $EXIT_CODE_SUCCESS);
}

sub recomputeStats
{
	my @statsOptions =  @_;

#	my ($exitCode) = share(
#		0,
#		"--recompute-stats",
#		"--log-file", "${generatedFilePrefix}recomputestats.log",
#		@shareOptions
#	);

	my $exitCode = stats($statsEndpointURL, @statsOptions, "--recompute-stats");
	die "ERROR OCCURRED DURING COMPUTATION OF STATS\n" unless ($exitCode == $EXIT_CODE_SUCCESS);
}

sub writeRandomQueryOrderingsToFile
{
	my ($queryFile, $numOrderings, $filePrefix, @shareOptions) = @_;

	# This command generates a list of SPARQL queries on STDOUT

	my $orderingsFile = "$queryFile.orderings";

	my ($exitCode) = share(
		0,
		"--output-file", $orderingsFile,
		"--enumerate-orderings",
		"--query-file", $queryFile,
		@shareOptions
	);

	if($exitCode != $EXIT_CODE_SUCCESS) {
		warn "ERROR: QUERY ENUMERATION FAILED FOR $queryFile"; 
		return ();
	}

	my $queryList = readStringFromFile($orderingsFile);
	# Note: this is Unix-dependent
	my @queries = split(/\n\n\n/, $queryList);

	my @generatedFiles = ();

	if(@queries == 0) {
		warn "ERROR: ZERO RESOLVABLE QUERY ORDERINGS FOR $queryFile";
		return ();
	}

	for(my $i = 0; $i < $numOrderings; $i++) {

		my $filename = "$filePrefix.ordering.$i";
		
		my $queryIndex = int(rand(@queries));
		my $query = $queries[ $queryIndex ];

		writeStringToFile($query, $filename);

		push(@generatedFiles, $filename);

	}

	return @generatedFiles;
}

sub query
{
	my ($queryFile, $generatedFilePrefix, @shareOptions) = @_;

	my ($basename) = fileparse($queryFile);

	my $query = readStringFromFile($queryFile);
	msg("running $basename:\n" . $query . "\n");

	my ($exitCode, $elapsedTime) = share(
		$QUERY_TIMEOUT,
		"--query-file", $queryFile,
		"--log-file", "$generatedFilePrefix.log",
		"--output-file", "$generatedFilePrefix.results",
		@shareOptions
	);

	msg("query $basename finished in $elapsedTime seconds");

	my $statusFile = "$generatedFilePrefix.exit.status";
	
	switch($exitCode) {
		case ( $EXIT_CODE_SUCCESS ) { msg("query completed successfully"); }
		case ( $EXIT_CODE_NO_RESULTS ) { msg("QUERY RETURNED NO RESULTS"); }
		case ( $EXIT_CODE_TIMEOUT ) { msg("QUERY TIMED OUT AFTER $QUERY_TIMEOUT SECONDS"); }
		case ( $EXIT_CODE_FAILURE ) { msg("QUERY FAILED DURING EXECUTION"); }
		else { msg("unrecognized return code from $shareJar"); }
	}

	if(defined($STATUS_STRING{$exitCode})) {
		writeStringToFile($STATUS_STRING{$exitCode}, $statusFile);
	} else {
		writeStringToFile("ERROR", $statusFile);
	}	
	
	writeStringToFile($elapsedTime, "$generatedFilePrefix.time");

	return ($exitCode, $elapsedTime);
}

sub msg
{
	warn "=> " . shift(@_) . "\n";
}

sub readStringFromFile
{
	my $filename = shift(@_);

	open(my $filehandle, "<", $filename) or die "unable to open $filename for reading: $!";
	my $string = join("", <$filehandle>);
	close($filehandle) or warn "error closing $filename: $!";

	return $string;
}

sub writeStringToFile 
{
	my ($string, $filename) = @_;

	open(my $filehandle, ">", $filename) or die "unable to open $filename for writing: $!";
	print $filehandle $string;
	close($filehandle) or warn "error closing $filename: $!";
}

sub runTimedCommand
{
	my ($timeout, @cmd) = @_;

	my $startTime = time;

	my $pid = fork();
	die "fork failed" unless defined($pid);

	if($pid == 0) {
		# child process
		exec {$cmd[0]} @cmd;
		die "exec failed!";
	}

	# the parent process waits until either query finishes or
	# the timeout elapses.

	eval {
		local $SIG{ALRM} = sub { die "alarm\n" }; # NB: \n required
		alarm($timeout);
		waitpid($pid, 0);
		alarm 0;
	};

	my $elapsedTime = time - $startTime;  
	my $exitCode = $EXIT_CODE_SUCCESS;

	if ($@) {

		# query timed out
		die unless $@ eq "alarm\n"; # propagate unexpected errors
		kill(15, $pid);
		waitpid($pid, 0);
		$exitCode = $EXIT_CODE_TIMEOUT;

	}
	else {
		# query finished
		$exitCode = ($? >> 8);
	}

	return ($exitCode, $elapsedTime);
}

