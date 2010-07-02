# for the scatterplot() function
library(car)
# for errbar() function
library(Hmisc)
# for std.error() function
library(plotrix)
# for pointLabel() function
library(maptools)

# Command line arg 1 is a file containing sample values,
# where each line has the form:
#
# <predicate> <direction> <numInputs> <time1> [time2] [time3] ...

args = commandArgs(trailingOnly=TRUE)

#standard.error = function(x) {  sd(x, na.rm=TRUE) / length(x) }

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

meanTimes = cbind( data[,1:3], rowMeans( data[,4:ncol(data)], na.rm=TRUE ) )
colnames(meanTimes) = c("predicate", "direction", "numInputs", "meanTime")
standardErrs = cbind( data[,1:3], apply( data[ , 4:ncol(data) ], c(1), std.error ) )
colnames(standardErrs) = c("predicate", "direction", "numInputs", "standardErr")

xRange = range(meanTimes$numInputs)
standardErrsWithZeros = sapply(as.vector(standardErrs$standardErr), function(x) { if (is.na(x) ) { 0 } else { x } })
standardErrsWithZeros
yRange = range(meanTimes$meanTime + standardErrsWithZeros, meanTimes$meanTime - standardErrsWithZeros)
#yRange = range(meanTimes$meanTime)

forwardMeanTimes = meanTimes[ meanTimes$direction == "forward", ]
forwardStandardErrs = standardErrs[ standardErrs$direction == "forward", ]
reverseMeanTimes = meanTimes[ meanTimes$direction == "reverse", ]
reverseStandardErrs = standardErrs[ standardErrs$direction == "reverse", ]

# The plot() call just draws the axes and title, the errbar() calls 
# plot the points

png(args[3])

drawAxes = function() {

	plot(
		c(),
		c(),
		xlab="Number of Inputs",
		ylab="Average Response Time (ms)",
		main=args[2],
		xlim=xRange,
		ylim=yRange,
		log="y"
	)

}

drawAxes()

# Add a little extra space on the right side of the plot region, so that point labels
# don't get clipped (then redraw the axes)

charWidths = strwidth("a:label:that:is:pretty:long")
labelWidth = sum(charWidths)
xRange[2] = xRange[2] + labelWidth

drawAxes()

legend("topright", legend=c("forward response time", "reverse response time"), pch=c(24,25))

errbar(
	forwardMeanTimes[,3], 
	forwardMeanTimes[,4], 
	forwardMeanTimes[,4] + forwardStandardErrs[,4], 
	forwardMeanTimes[,4] - forwardStandardErrs[,4], 
	pch=24, 
	add=TRUE, 
	ann=FALSE
)

errbar(
	reverseMeanTimes[,3], 
	reverseMeanTimes[,4], 
	reverseMeanTimes[,4] + reverseStandardErrs[,4], 
	reverseMeanTimes[,4] - reverseStandardErrs[,4], 
	pch=25, 
	add=TRUE, 
	ann=FALSE
)

# label each point with the (abbreviated) predicate URI
text(x=meanTimes[,3], y=meanTimes[,4], labels=meanTimes[,1], pos=4)

dev.off()
