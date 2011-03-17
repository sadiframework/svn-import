use Test::More qw/no_plan/;

BEGIN {
    use FindBin qw ($Bin);
    use lib "$Bin/../lib";
    use_ok('SADI::RDF::Core');
};

END {
    # destroy persistent data here
};
my $rdf = '@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .';
is($rdf =~ m|http://www\.w3\.org/1999/02/22-rdf-syntax-ns|g, 1, "test n3 regular expression");
$rdf =<<EOF;
<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
  xmlns:dc="http://purl.org/dc/elements/1.1/">
  <rdf:Description rdf:about="http://www.w3.org/">
    <dc:title>World Wide Web Consortium</dc:title> 
  </rdf:Description>
</rdf:RDF>
EOF
is($rdf =~ m|http://www\.w3\.org/1999/02/22-rdf-syntax-ns|g, 1, "test rdf/xml regular expression");
$rdf = '@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.';
isnt($rdf =~ m|http://www\.w3\.org/1999/02/22-rdf-syntax-ns|g, 1, "test n3 regular expression with invalid input");

