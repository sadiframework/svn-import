#!/usr/bin/perl -w
use strict;

my $USAGE = "USAGE: summaryStats.pl <graph URI>\n";
my $NO_VALUE_AVAILABLE = "NA";

my $QUERY_FORWARD = <<HEREDOC;
PREFIX stats: <http://sadiframework.org/ontologies/predicatestats.owl#>

SELECT ?predicate ?baseTimeForward ?timePerInputForward
FROM <$ARGV[0]> 
WHERE {  
	?predicate stats:baseTimeForward ?baseTimeForward .
	?predicate stats:timePerInputForward ?timePerInputForward .
}
HEREDOC

my $QUERY_REVERSE = <<HEREDOC;
PREFIX stats: <http://sadiframework.org/ontologies/predicatestats.owl#>

SELECT ?predicate ?baseTimeReverse ?timePerInputReverse
FROM <$ARGV[0]> 
WHERE {  
	?predicate stats:baseTimeReverse ?baseTimeReverse .
	?predicate stats:timePerInputReverse ?timePerInputReverse .
}
HEREDOC

if(@ARGV != 1) {
	die $USAGE;
}

my %baseTimeForward = ();
my %timePerInputForward = ();
my %baseTimeReverse = ();
my %timePerInputReverse = ();

my @forwardResults = qx"bash -c 'rsparql --service http://dev.biordf.net/sparql --query <(echo \"$QUERY_FORWARD\") | jena_table_to_plain_text'";

foreach my $line (@forwardResults) {

	my @fields = split(/\s+/, $line);
	$baseTimeForward{ $fields[0] } = $fields[1];
	$timePerInputForward{ $fields[0] } = $fields[2];

}

my @reverseResults = qx"bash -c 'rsparql --service http://dev.biordf.net/sparql --query <(echo \"$QUERY_REVERSE\") | jena_table_to_plain_text'";

foreach my $line (@reverseResults) {
	
	my @fields = split(/\s+/, $line);
	$baseTimeReverse{ $fields[0] } = $fields[1];
	$timePerInputReverse{ $fields[0] } = $fields[2];

}

my %allKeys = ();
map { $allKeys{$_} = 1 } keys(%baseTimeForward);
map { $allKeys{$_} = 1 } keys(%timePerInputForward);
map { $allKeys{$_} = 1 } keys(%baseTimeReverse);
map { $allKeys{$_} = 1 } keys(%timePerInputReverse);

foreach my $key (keys %allKeys) {

	print $key;
	print " ";
	print (defined($baseTimeForward{$key}) ? $baseTimeForward{$key} : $NO_VALUE_AVAILABLE);
	print " ";
	print (defined($timePerInputForward{$key}) ? $timePerInputForward{$key} : $NO_VALUE_AVAILABLE);
	print " ";
	print (defined($baseTimeReverse{$key}) ? $baseTimeReverse{$key} : $NO_VALUE_AVAILABLE);
	print " ";
	print (defined($timePerInputReverse{$key}) ? $timePerInputReverse{$key} : $NO_VALUE_AVAILABLE);
	print "\n";

}

