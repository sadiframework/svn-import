# for Curry(), Compose() 
library(roxygen)
# for errbar()
library(Hmisc)
# for barplot2()
library(gplots)
# for std.error()
library(plotrix)

#standard.error = function(x) {  sd(x, na.rm=TRUE) / length(x) }

# Command line arg 1 is a file containing sample values,
# where each line has the form:
#
# <predicate> <direction> <numInputs> <time1> [time2] [time3] ...

args = commandArgs(trailingOnly=TRUE)

# The read.table command only looks at the
# first 5 lines of the file to determine
# the maximum number of columns per row.
# We need to be more certain, so we use a 
# Perl one-liner to figure it out.
# 
# intern=TRUE here causes the output of the 
# command to captured as the return 
# value for system(). 

maxcols <- as.real(system(paste("perl -e 'BEGIN { $maxcols = 0 }' -lane '$maxcols = @F if @F > $maxcols;' -e 'END { print $maxcols }'", args[1]), intern=TRUE))

# The only way to indicate the number
# of columns to read.table is to 
# provide a col.names vector

colNames = rep("", maxcols)
data = read.table(args[1], col.names=colNames, fill=TRUE)

# Sort the rows by (numInputs, predicate) so that the ordering
# of the bars is consistent across plots for different training
# runs

#data = data[ order(data[,3], data[,1]), ]

meanTimes = cbind( data[,1:3], rowMeans( data[,4:ncol(data)], na.rm=TRUE ) )
colnames(meanTimes) = c("predicate", "direction", "numInputs", "meanTime")

standardErrs = cbind( data[,1:3], apply( data[,4:ncol(data)], c(1), std.error ) )
colnames(standardErrs) = c("predicate", "direction", "numInputs", "standardErr")

barGroups = split(meanTimes, meanTimes$numInputs)
errGroups = split(standardErrs, standardErrs$numInputs)
maxGroupSize = max(sapply(barGroups, nrow))

# Right-pad each barGroup vector with NAs so that they are the same length,
# then concatenate the vectors into a matrix for plotting. 

getColumn = function(x,col) { x[,col] } 
padVector = function(x) { c(x, rep(NA, maxGroupSize - length(x))) } 
dirToAngle = function(x) { if(is.null(x) || is.na(x) || x == "forward") { 10 } else { -10 } } 
dirToAngle("forward")

plotMatrix = sapply(barGroups, Compose(Curry(getColumn,col=4), padVector))
plotErrs = sapply(errGroups, Compose(Curry(getColumn,col=4), padVector))
shadingAngles = as.numeric(sapply(barGroups, Compose(Curry(getColumn,col=1), as.character, padVector, Curry(Map, dirToAngle))))
plotLabels = sapply(barGroups, Compose(Curry(getColumn,col=1), as.character, padVector))
barWidths = as.numeric(Map(function(x) { if(is.na(x) || is.null(x)) { 0 } else { 1 } }, plotMatrix))

# las = 3 means draw all axis labels vertically
par(las=3)
# mar(bottom, left, top, right) sets margins (measured in lines of text)
par(mar=c(10, 4, 4, 2))

barplot2(
	plotMatrix, 
	main=args[2],
	ylab='Average Response Time (ms)',
	beside=TRUE,
	names.arg=plotLabels,
	ci.l=(plotMatrix - plotErrs),
	ci.u=(plotMatrix + plotErrs),
	angle=shadingAngles,
	density=10,
	col=rgb(0,0,0),
	)

#forwardMeanTimes = meanTimes[ meanTimes$direction == "forward", ]
#forwardStandardErrs = standardErrs[ standardErrs$direction == "forward", ]
#reverseMeanTimes = meanTimes[ meanTimes$direction == "reverse", ]
#reverseStandardErrs = standardErrs[ standardErrs$direction == "reverse", ]
#
## The plot() call just draws the axes and title, the errbar() calls 
## plot the points
#
#png(args[3])
#
#plot(
#	c(),
#	c(),
#	xlab="Number of Inputs",
#	ylab="Average Response Time (ms)",
#	main=args[2],
#	xlim=c(1,5),
#	ylim=c(250,50000),
#	log="y"
#)
#
#errbar(
#	forwardMeanTimes[,3], 
#	forwardMeanTimes[,4], 
#	forwardMeanTimes[,4] + forwardStandardErrs[,4], 
#	forwardMeanTimes[,4] - forwardStandardErrs[,4], 
#	pch=24, 
#	add=TRUE, 
#	ann=FALSE
#)
#
#errbar(
#	reverseMeanTimes[,3], 
#	reverseMeanTimes[,4], 
#	reverseMeanTimes[,4] + reverseStandardErrs[,4], 
#	reverseMeanTimes[,4] - reverseStandardErrs[,4], 
#	pch=25, 
#	add=TRUE, 
#	ann=FALSE
#)
#
# label each point with the (abbreviated) predicate URI
#text(x=meanTimes[,3], y=meanTimes[,4], labels=meanTimes[,1], pos=4)
#
#dev.off()
