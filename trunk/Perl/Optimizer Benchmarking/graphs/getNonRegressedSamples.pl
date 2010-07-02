#!/usr/bin/perl -w
use strict;

my $USAGE = "USAGE: sampleAverages.pl <graph URI>\n";

my $GET_ALL_TIME_SAMPLES_QUERY = <<HEREDOC;

PREFIX stats: <http://sadiframework.org/ontologies/predicatestats.owl#>

SELECT ?predicate ?directionIsForward ?numInputs ?responseTime
FROM <$ARGV[0]> 
WHERE {  
	?sample stats:predicate ?predicate .
	?sample stats:numInputs ?numInputs .
	?sample stats:directionIsForward ?directionIsForward .
	?sample stats:responseTime ?responseTime .
}

HEREDOC

if(@ARGV != 1) {
	die $USAGE;
}

my @sampleLines = qx"bash -c 'rsparql --service http://dev.biordf.net/sparql --query <(echo \"$GET_ALL_TIME_SAMPLES_QUERY\") | jena_table_to_plain_text'";

# Put the samples in nested hash. 
# Structure: predicate => directionIsForward (0/1) => numInputs => list of response times

my $samples = {};

foreach my $sampleLine (@sampleLines) {

	my @fields = split(/\s+/, $sampleLine);

	my $predicate = $fields[0];
	my $direction = ($fields[1] eq '"true"') ? "forward" : "reverse";
	my $numInputs = $fields[2];
	my $responseTime = $fields[3];

	$samples->{ $predicate } = {} unless defined ($samples->{ $predicate });
	$samples->{ $predicate }->{ $direction } = {} unless defined ($samples->{ $predicate }->{ $direction });
	$samples->{ $predicate }->{ $direction }->{ $numInputs } = [] unless defined ($samples->{ $predicate }->{ $direction }->{ $numInputs });

	push(@{$samples->{ $predicate }->{ $direction }->{ $numInputs }}, $responseTime);

}

foreach my $predicate (keys %$samples) {

	foreach my $direction (keys %{$samples->{ $predicate }} ) {
		
		my %numInputsMap = %{$samples->{ $predicate }->{ $direction }};

		if( (keys %numInputsMap) == 1 ) {
			my @numInputKeys = keys %numInputsMap;
			my $numInputs = $numInputKeys[0]; 
			print join(" ", ($predicate, $direction, $numInputs, @{ $numInputsMap{ $numInputs } })) . "\n";
		}		

	}

}

