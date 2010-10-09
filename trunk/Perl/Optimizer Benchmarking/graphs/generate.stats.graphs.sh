#!/bin/bash

R_COMMAND="R --vanilla --args"

if (( $# < 2 )); then
	echo "USAGE: ./generate.stats.graphs.sh <numTrainingRuns> <samples graph prefix> <summary stats graph prefix>" 1>&2
	exit 1;
fi

trainingRuns=$1
samplesGraphPrefix=$2
summaryGraphPrefix=$3

for i in $(seq 1 $trainingRuns); do

	samplesGraphURI="$samplesGraphPrefix/$i.training.runs"
	summaryStatsGraphURI="$summaryGraphPrefix/$i.training.runs"

	echo "plotting graph for $samplesGraphURI..." 1>&2

	samplesFilename="non.regressed.samples.$i.training.runs.txt"
	# newline is intentional here
	samplesGraphTitle="Average Response Times For Predicates
Without Regression Lines
($i Training Runs)" 

	./getNonRegressedSamples.pl $samplesGraphURI | apply.sparql.prefixes.pl sparql.prefixes > $samplesFilename
	$R_COMMAND $samplesFilename "$samplesGraphTitle" "non.regressed.averages.$i.svg" < non.regressed.samples.scatter.plot.R
	rm $samplesFilename

	echo "plotting graph for $summaryStatsGraphURI..." 1>&2

	summaryStatsFilename="summary.stats.$i.training.runs.txt"
	./summaryStats.pl $summaryStatsGraphURI | apply.sparql.prefixes.pl sparql.prefixes | sort > $summaryStatsFilename
	$R_COMMAND $summaryStatsFilename "Base Time and Time-per-Input After $i Training Runs" "summaryStats.$i.svg" < summary.stats.bar.plot.R 
	rm $summaryStatsFilename

done

