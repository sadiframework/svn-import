#!/bin/bash

R_COMMAND="R --vanilla --args"
ALPHABET="abcdefghijklmnopqrstuvwxyz"

FIGURE_NUMBER=27
FIGURE="Figure $FIGURE_NUMBER"

PNG_WIDTH_IN_PIXELS=1000

if (( $# < 2 )); then
	echo "USAGE: ./generate.stats.graphs.sh <numTrainingRuns> <samples graph prefix> <summary stats graph prefix> <target dir>" 1>&2
	exit 1;
fi

trainingRuns=$1
samplesGraphPrefix=$2
summaryGraphPrefix=$3
targetDir=$4

for i in $(seq 1 $trainingRuns); do

	summaryStatsGraphURI="$summaryGraphPrefix/$i.training.runs"

	echo "plotting graph for $summaryStatsGraphURI..." 1>&2

	summaryStatsFilename="summary.stats.$i.training.runs.txt"
	figurePart=$(($i - 1))
	summaryStatsSVGFile="$targetDir/$FIGURE${ALPHABET:$figurePart:1} - Base Time and Time-per-Input - $i Training Runs.svg"
	summaryStatsPNGFile=$(echo $summaryStatsSVGFile | perl -ple 's/svg/png/g')

	./summaryStats.pl $summaryStatsGraphURI | apply.sparql.prefixes.pl sparql.prefixes | sort > $summaryStatsFilename
	$R_COMMAND $summaryStatsFilename "Base Time and Time-per-Input After $i Training Runs" "$summaryStatsSVGFile" < summary.stats.bar.plot.R 
	rm $summaryStatsFilename

	inkscape -e "$summaryStatsPNGFile" -w $PNG_WIDTH_IN_PIXELS "$summaryStatsSVGFile"

done

for i in $(seq 1 $trainingRuns); do

	samplesGraphURI="$samplesGraphPrefix/$i.training.runs"

	echo "plotting graph for $samplesGraphURI..." 1>&2

	samplesFilename="non.regressed.samples.$i.training.runs.txt"
	# newline is intentional here
	samplesGraphTitle="Average Response Times For Predicates
Without Regression Lines
($i Training Runs)" 
	figurePart=$(($trainingRuns + $i - 1))
	samplesSVGFile="$targetDir/$FIGURE${ALPHABET:$figurePart:1} - Average Response Times for Predicates Without Regression Lines - $i Training Runs.svg"
	samplesPNGFile=$(echo $samplesSVGFile | perl -ple 's/svg/png/g')

	./getNonRegressedSamples.pl $samplesGraphURI | apply.sparql.prefixes.pl sparql.prefixes > $samplesFilename
	$R_COMMAND $samplesFilename "$samplesGraphTitle" "$samplesSVGFile" < non.regressed.samples.scatter.plot.R
	rm $samplesFilename
	
	inkscape -e "$samplesPNGFile" -w $PNG_WIDTH_IN_PIXELS "$samplesSVGFile"

done


