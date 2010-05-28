#!/bin/bash

if (( $# != 1 )); then
	echo "USAGE: $0 <directory containing result files>"
	exit 1;
fi

#-------------------------------------------------------
# Result file naming scheme:
#
# The timing results for non-optimized queries are
# stored in files named as:
#
# <query file>.ordering.<N>.no.opt.trial.<N>.time
#
# while the time results for optimized queries are
# stored in files named as:
#
# <query file>.ordering.<N>.test.run.<N>.opt.trial.<N>.time
#
# The test run number indicates how many training
# runs have occurred prior to running the test query. 
#------------------------------------------------------

R_FILE="plot.graphs.R"
RESULT_FILES_PATH=$1;

function add_line
{
	echo $* >> $R_FILE;
}

#-----------------------------------------------
# Generate an R script that creates the graphs
#-----------------------------------------------

# remove the previously generated file if it exists
rm $R_FILE 2> /dev/null

# required for the errbar() method
add_line "library(Hmisc)"

# In some cases (timeouts or queries that die), we will only have one trial time
# and this makes the standard error for that set of trials undefined.  For our
# purposes, we just want standard error to be zero in this case.

add_line "safe.standard.error <- function(row.vector) { if (length(row.vector) > 1) { sd(t(row.vector)) / sqrt(length(row.vector)) } else { 0 } }"
add_line "safe.mean <- function(vector) { if (length(vector) > 0) { mean(vector) } else { 0 } }"

test_query_prefixes=$(ls $RESULT_FILES_PATH/*.test.run.*.time | perl -ple 's/(.*)\.ordering.*/\1/g' | sort | uniq) 

echo -e "test_query_prefixes:\n${test_query_prefixes}\n"

for test_query_prefix in $test_query_prefixes; do

	#-------------------------------------------------------------
	# Put the mean times for each test query ordering 
	# into a matrix. (Each mean represents the query time
	# for one test run.)
	#
	# Likewise, put the standard error for each query time into
	# a matrix so that we can calculate the error bars.
	#-------------------------------------------------------------

	add_line "mean.matrix <- cbind()"
	add_line "stderr.matrix <- cbind()"
	# indicates the exit status of queries if not normal (e.g. TIMEOUT)
	add_line "annotation.matrix <- cbind()"

	ordering_prefixes=$(ls ${test_query_prefix}.ordering.*.time | perl -ple 's/(.*\.ordering\.[0-9]+).*/\1/g' | sort | uniq)

	echo -e "ordering_prefixes:\n${ordering_prefixes}\n"

	for ordering_prefix in $ordering_prefixes; do

		# vector of mean execution times for each test run
		add_line "means <- c()";
		# vector of standard errors for each test run 
		add_line "stderrs <- c()";
		# vector of labels for each bar in a group
		add_line "bar.labels <- c()";
		# indicate exit status of query if not normal (e.g. TIMEOUT)
		add_line "bar.annotations <- c()";

		#--------------------------------------------------------------
		# Compute mean and standard error of non-optimized trials
		#--------------------------------------------------------------

		no_opt_vector=$(basename ${ordering_prefix})".no.opt"
		add_line "$no_opt_vector <- c()" 

		no_opt_trials=$(ls ${ordering_prefix}.no.opt.*.time | sort | uniq)

		echo -e "no_opt_trials:\n${no_opt_trials}\n"

		annotation=""

		for no_opt_trial in $no_opt_trials; do

			add_line "$no_opt_vector <- rbind($no_opt_vector, read.table('$no_opt_trial', header=FALSE))"

			status_file=$(echo ${no_opt_trial} | perl -ple 's/\.time$/.exit.status/')
			exit_status=$(cat ${status_file})
			if [ "${exit_status}" != "SUCCESS" ]; then
				annotation="${exit_status}"
			fi	

		done

		add_line "means <- cbind(means, safe.mean($no_opt_vector))"
		add_line "stderrs <- cbind(stderrs, safe.standard.error(t($no_opt_vector)))"
		add_line "bar.labels <- rbind(bar.labels, 'no optimization')"
		add_line "bar.annotations <- rbind(bar.annotations, '$annotation')"

		#-------------------------------------------------------------
		# Compute mean and standard error of optimized test runs
		#-------------------------------------------------------------

		test_run_prefixes=$(ls ${ordering_prefix}.test.run*.time | perl -ple 's/(.*)\.trial.*/\1/g' | sort | uniq)

		echo -e "test_run_prefixes:\n${test_run_prefixes}\n"

		test_run_count=0

		for test_run_prefix in $test_run_prefixes; do

			opt_vector=$(basename ${test_run_prefix})
			add_line "$opt_vector <- c()"

			trials=$(ls ${test_run_prefix}*.time | sort | uniq)

			annotation=""

			for trial in $trials; do

				add_line "$opt_vector <- rbind($opt_vector, read.table('$trial', header=FALSE))"
				
				status_file=$(echo $trial | perl -ple 's/\.time$/.exit.status/')
				exit_status=$(cat ${status_file})
				if [ "${exit_status}" != "SUCCESS" ]; then
					annotation="${exit_status}"
				fi	

			done

			add_line "means <- cbind(means, safe.mean($opt_vector))"
			add_line "stderrs <- cbind(stderrs, safe.standard.error(t($opt_vector)))"
			add_line "bar.labels <- rbind(bar.labels, '${test_run_count} training runs')"
			add_line "bar.annotations <- rbind(bar.annotations, '$annotation')"

			test_run_count=$((${test_run_count} + 1))

		done # for each test run

		#------------------------------------------------------------
		# Add mean times to matrix for test run, likewise for
		# standard errors.
		#------------------------------------------------------------

		add_line "mean.matrix <- cbind(mean.matrix, t(means))"
		add_line "stderr.matrix <- cbind(stderr.matrix, t(stderrs))"
		add_line "annotation.matrix <- cbind(annotation.matrix, t(bar.annotations))"

	done # for each query ordering

	test_query=$(basename ${test_query_prefix})
	graph_file="${test_query}.png";

	add_line "png('${graph_file}')"
	add_line ""
	add_line "# generate color gradient for bars"
	add_line "shades.of.gray <- c()"
	add_line "inc <- (0.9 - 0.5) / nrow(mean.matrix)"
	add_line "for(i in 1:nrow(mean.matrix)) {"
	add_line "  intensity <- 0.5 + (i - 1)*inc"
	add_line "  shades.of.gray <- rbind(shades.of.gray, rgb(intensity, intensity, intensity))"   
	add_line "}"
	add_line ""
	add_line "# generate labels for bar groups"
	add_line "group.labels <- c()"
	add_line "for(i in 1:ncol(mean.matrix)) {"
	add_line "  group.labels <- rbind(group.labels, paste('Ordering ',i))"
	add_line "}"
	add_line
	add_line "xvals.matrix <- barplot(mean.matrix, main='Results for ${test_query}', legend.text=bar.labels, names.arg=group.labels, col=shades.of.gray, beside=TRUE)"	
	add_line "xvals <- xvals.matrix[ 1:length(xvals.matrix) ]"
	add_line "yvals <- mean.matrix[ 1:length(mean.matrix) ]"
	add_line "ydeltas <- stderr.matrix[ 1:length(stderr.matrix) ]"
	add_line "errbar(xvals, yvals, yvals + ydeltas, yvals - ydeltas, xlab='Random Input Query Ordering', ylab='Query Time (seconds)', add=TRUE)"
	add_line ""
	add_line "# apply text annotations indicating query exit status (e.g. TIMEOUT)"
	add_line "annotation.vector <- annotation.matrix[ 1:length(annotation.matrix) ]"
	add_line "text(xvals, rep(0, length(xvals)), annotation.vector, srt=90, adj=0)"
	add_line ""
	add_line "dev.off()"

done # for each test query

R --vanilla < plot.graphs.R > plot.graphs.R.out 2>&1
