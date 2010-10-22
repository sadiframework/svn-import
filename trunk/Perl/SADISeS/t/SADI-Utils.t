# Before `make install' is performed this script should be runnable with
# `make test'. After `make install' it should work as `perl SADI.t'
#########################
# change 'tests => 1' to 'tests => last_test_to_print';
#use Test::More tests => 7;
use Test::More qw/no_plan/;
use strict;

BEGIN {
    use FindBin qw ($Bin);
    use lib "$Bin/../lib";
}

END {

    # destroy persistent data here (if any)
}
my $RDF = <<EOF;

<rdf:RDF
    xmlns:a="http://semanticscience.org/resource/"
    xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
        <rdf:Description rdf:about="http://lsrn.org/GeneID:468">
            <a:SIO_000008>
                <rdf:Description>
                    <rdf:type rdf:resource="http://purl.oclc.org/SADI/LSRN/GeneID_Identifier"/>
                    <a:SIO_000300>367</a:SIO_000300>
                </rdf:Description>
            </a:SIO_000008>
            <rdf:type rdf:resource="http://purl.oclc.org/SADI/LSRN/GeneID_Record"/>
        </rdf:Description>
</rdf:RDF>
EOF

use_ok('SADI::Service::Instance');
use_ok('SADI::Utils');
use_ok('SADI::RDF::Core');

# test the unLSRNize() method
# set up the SADI::RDF::Core object,
my $core = new SADI::RDF::Core;
$core->Prepare($RDF);
my $service = SADI::Service::Instance->new(
                          ServiceName => "helloworld",
                          ServiceType => "http://someontology.org/services/sometype",
                          InputClass  => "http://purl.oclc.org/SADI/LSRN/GeneID_Record",
                          OutputClass => "http://purl.oclc.org/SADI/LSRN/GeneID_Record",
                          Description => "the usual hello world service",
                          UniqueIdentifier => "urn:lsid:myservices:helloworld",
                          Authority        => "helloworld.com",
                          Provider         => 'myaddress@organization.org',
                          ServiceURI => "http://helloworld.com/cgi-bin/helloworld.pl",
                          URL        => "http://helloworld.com/cgi-bin/helloworld.pl",
);

# set the Instance for $core
$core->Signature($service);
is( $core->Prepare($RDF), 1, 'Check our test RDF' );

# should only be one value here ...
foreach my $input ( $core->getInputNodes() ) {
    is( SADI::Utils::unLSRNize( $input, $core ), 367, 'Check the parsed literal' );
    is( SADI::Utils->unLSRNize( $input, $core ), 367, 'Check the parsed literal with arrow operator' );
}

# test the unLSRNize() method using an object reference
my $utils = new SADI::Utils->new;

# should only be one value here ...
foreach my $input ( $core->getInputNodes() ) {
    is(
        $utils->unLSRNize( $input, $core ),
        367, 'Check the parsed literal using an object reference'
    );
}

# try to find_files
is( SADI::Utils::find_file( $Bin, 'SADI-Utils.t' ),
    "$Bin/SADI-Utils.t", 'Check find_file() for this test script' );
is( SADI::Utils::find_file( "$Bin/../", 'SADI', 'RDF', 'Core.pm' ),
    "$Bin/../lib/SADI/RDF/Core.pm",
    'Check find_file() for the SADI::RDF::Core module' );
is( SADI::Utils->find_file( $Bin, 'SADI-Utils.t' ),
    "$Bin/SADI-Utils.t", 'Check find_file() for this test script with arrow operator' );
is( SADI::Utils->find_file( "$Bin/../", 'SADI', 'RDF', 'Core.pm' ),
    "$Bin/../lib/SADI/RDF/Core.pm",
    'Check find_file() for the SADI::RDF::Core module with arrow operator' );

# find files using reference to a SADI::Utils object
is( $utils->find_file( $Bin, 'SADI-Utils.t' ),
    "$Bin/SADI-Utils.t", 'Check find_file() for this test script using a reference' );
is( $utils->find_file( "$Bin/../", 'SADI', 'RDF', 'Core.pm' ),
    "$Bin/../lib/SADI/RDF/Core.pm",
    'Check find_file() for the SADI::RDF::Core module using a reference' );

# test empty_rdf()
like( SADI::Utils::empty_rdf(), qr/RDF/, 'Check empty_rdf()' );
like( SADI::Utils->empty_rdf(), qr/RDF/, 'Check empty_rdf() with arrow operator' );

# test empty_rdf() using reference to a SADI::Utils object
like( $utils->empty_rdf(), qr/RDF/, 'Check empty_rdf() using a reference' );

# test trim()
is( SADI::Utils::trim('hello sadi world'),
    'hello sadi world',
    'Check trim() with no leading/trailing whitespace' );
is( SADI::Utils->trim('hello sadi world'),
    'hello sadi world',
    'Check trim() with no leading/trailing whitespace with arrow operator' );

for (16 .. 26) {
    is( SADI::Utils::trim(sprintf("%" . $_ . "s", "hello sadi world")),
    'hello sadi world',
    sprintf('Check trim() with %d leading whitespace', $_ - 16));
    is( SADI::Utils->trim(sprintf("%" . $_ . "s", "hello sadi world")),
    'hello sadi world',
    sprintf('Check trim() with %d leading whitespace with arrow operator', $_ - 16));
}

for (16 .. 26) {
    is( SADI::Utils::trim(sprintf("%-" . $_ . "s", "hello sadi world")),
    'hello sadi world',
    sprintf('Check trim() with %d trailing whitespace', $_ - 16 ));
    is( SADI::Utils->trim(sprintf("%-" . $_ . "s", "hello sadi world")),
    'hello sadi world',
    sprintf('Check trim() with %d trailing whitespace with arrow operator', $_ - 16 ));
}

# test trim() using reference to a SADI::Utils object
is( $utils->trim('hello sadi world'),
    'hello sadi world',
    'Check trim() with no leading/trailing whitespace using a reference' );
for (16 .. 26) {
    is( $utils->trim(sprintf("%" . $_ . "s", "hello sadi world")),
    'hello sadi world', # what it should be
    sprintf('Check trim() with %d leading whitespace using a reference', $_ - 16));
}

for (16 .. 26) {
    is( $utils->trim(sprintf("%-" . $_ . "s", "hello sadi world")),
    'hello sadi world', # what it should be
    sprintf('Check trim() with %d trailing whitespace using a reference', $_ - 16 ));
}