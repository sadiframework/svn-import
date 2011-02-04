# Before `make install' is performed this script should be runnable with
# `make test'. After `make install' it should work as `perl SADI.t'
#########################
# change 'tests => 1' to 'tests => last_test_to_print';
#use Test::More tests => 7;
use Test::More qw/no_plan/;
use File::Spec;
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
	is( SADI::Utils->unLSRNize( $input, $core ),
		367, 'Check the parsed literal with arrow operator' );
}

# test the unLSRNize() method using an object reference
my $utils = new SADI::Utils->new;

# should only be one value here ...
foreach my $input ( $core->getInputNodes() ) {
	is( $utils->unLSRNize( $input, $core ),
		367, 'Check the parsed literal using an object reference' );
}

# try to find_files
is(
	comparePaths( SADI::Utils::find_file( $Bin, 'SADI-Utils.t' ), "$Bin/SADI-Utils.t" ),
	0,
	'Check find_file() for this test script'
);
is(
	comparePaths(
				  SADI::Utils::find_file( "$Bin/../", 'SADI', 'RDF', 'Core.pm' ),
				  "$Bin/../lib/SADI/RDF/Core.pm"
	),
	0,
	'Check find_file() for the SADI::RDF::Core module'
);
is(
	comparePaths( SADI::Utils->find_file( $Bin, 'SADI-Utils.t' ), "$Bin/SADI-Utils.t" ),
	0,
	'Check find_file() for this test script with arrow operator'
);
is(
	comparePaths(
				  SADI::Utils->find_file( "$Bin/../", 'SADI', 'RDF', 'Core.pm' ),
				  "$Bin/../lib/SADI/RDF/Core.pm"
	),
	0,
	'Check find_file() for the SADI::RDF::Core module with arrow operator'
);

# find files using reference to a SADI::Utils object
is( comparePaths( $utils->find_file( $Bin, 'SADI-Utils.t' ), "$Bin/SADI-Utils.t" ),
	0, 'Check find_file() for this test script using a reference' );
is(
	comparePaths(
				  $utils->find_file( "$Bin/../", 'SADI', 'RDF', 'Core.pm' ),
				  "$Bin/../lib/SADI/RDF/Core.pm"
	),
	0,
	'Check find_file() for the SADI::RDF::Core module using a reference'
);

# test empty_rdf()
like( SADI::Utils::empty_rdf(), qr/RDF/, 'Check empty_rdf()' );
like( SADI::Utils->empty_rdf(), qr/RDF/, 'Check empty_rdf() with arrow operator' );

# test empty_rdf() using reference to a SADI::Utils object
like( $utils->empty_rdf(), qr/RDF/, 'Check empty_rdf() using a reference' );

# test trim()
is( SADI::Utils::trim('hello sadi world'),
	'hello sadi world',
	'Check trim() with no leading/trailing whitespace' );
is(
	SADI::Utils->trim('hello sadi world'),
	'hello sadi world',
	'Check trim() with no leading/trailing whitespace with arrow operator'
);
for ( 16 .. 26 ) {
	is( SADI::Utils::trim( sprintf( "%" . $_ . "s", "hello sadi world" ) ),
		'hello sadi world',
		sprintf( 'Check trim() with %d leading whitespace', $_ - 16 ) );
	is(
		SADI::Utils->trim( sprintf( "%" . $_ . "s", "hello sadi world" ) ),
		'hello sadi world',
		sprintf( 'Check trim() with %d leading whitespace with arrow operator',
				 $_ - 16 )
	);
}
for ( 16 .. 26 ) {
	is( SADI::Utils::trim( sprintf( "%-" . $_ . "s", "hello sadi world" ) ),
		'hello sadi world',
		sprintf( 'Check trim() with %d trailing whitespace', $_ - 16 ) );
	is(
		SADI::Utils->trim( sprintf( "%-" . $_ . "s", "hello sadi world" ) ),
		'hello sadi world',
		sprintf( 'Check trim() with %d trailing whitespace with arrow operator',
				 $_ - 16 )
	);
}

# test trim() using reference to a SADI::Utils object
is( $utils->trim('hello sadi world'),
	'hello sadi world',
	'Check trim() with no leading/trailing whitespace using a reference' );
for ( 16 .. 26 ) {
	is(
		$utils->trim( sprintf( "%" . $_ . "s", "hello sadi world" ) ),
		'hello sadi world',    # what it should be
		sprintf( 'Check trim() with %d leading whitespace using a reference', $_ - 16 )
	);
}
for ( 16 .. 26 ) {
	is(
		$utils->trim( sprintf( "%-" . $_ . "s", "hello sadi world" ) ),
		'hello sadi world',    # what it should be
		sprintf(
				 'Check trim() with %d trailing whitespace using a reference', $_ - 16
		)
	);
}

#===============================================================
# comparePaths() and supporting functions
#===============================================================
# returns
# 1  if $sPath1 owns/contains $sPath2
# 0  if $sPath1 equals $sPath2
# -1 if $sPath1 is owned *by* $sPath2
# -2 if $sPath1 is along side of $sPath2
sub comparePaths {
	my ( $sPath1, $sPath2 ) = @_;
	my ( $sVol1, $aDirs1, $sFile1 ) = parsePath($sPath1);
	my ( $sVol2, $aDirs2, $sFile2 ) = parsePath($sPath2);

	# paths on two different volumes can't own one another
	return -2 if ( $sVol1 ne $sVol2 );

	# assume the most deeply nested path components are at the
	# end of the directory array.
	# files are "inside" directories, so just push them onto the
	# directory path
	push @$aDirs1, $sFile1 if $sFile1;
	push @$aDirs2, $sFile2 if $sFile2;

	# $"='|'; #to make leading and trailing '' more visible
	# print STDERR "dirs1=<@$aDirs1> <@$aDirs2>\n";
	# decide if we are inside or outside by comparing directory
	# components
	my $iSegments1 = scalar @$aDirs1;
	my $iSegments2 = scalar @$aDirs2;
	if ( $iSegments1 <= $iSegments2 ) {
		for ( my $i = 0 ; $i < $iSegments1 ; $i++ ) {
			return -2 if $aDirs1->[$i] ne $aDirs2->[$i];
		}
		return $iSegments1 == $iSegments2 ? 0 : 1;
	} else {
		for ( my $i = 0 ; $i < $iSegments2 ; $i++ ) {
			return -2 if $aDirs1->[$i] ne $aDirs2->[$i];
		}
		return -1;
	}
}

sub parsePath {
	my $sPath = shift;

	# parse the canonical path
	$sPath = File::Spec->canonpath($sPath);

	# parse the canonical path
	$sPath = File::Spec->canonpath($sPath);

	# split the path into components
	my ( $sVolume, $sDirPart, $sFilePart ) = File::Spec->splitpath( $sPath, 0 );

	# maybe the nesting order of directories in $sDirPart
	# is right to left instead of left to right
	# (as in Unix,MsWin)?
	# If so, further split the directory portion into
	# components in the hope that splitdir produces
	# an array with most nested directory components at
	# the end... BUT this is an assumption. There is no
	# documentation guarenteeing it.
	# Also, canonize the directory part before splitting
	# it.  File::Spec::Unix sets the directory part to '.../'
	# but splitdir doesn't strip empty directories from UNIX.
	# this is explained in File::Spec's documentation for splitdir:
	#
	#   Unlike just splitting the directories on the separator,
	#   empty directory names ('') can be returned, because these
	#   are significant on some OSes.
	$sDirPart = File::Spec->canonpath($sDirPart);
	my @aDirs = File::Spec->splitdir($sDirPart);

	# return parsed path
	return ( $sVolume, \@aDirs, $sFilePart );
}
