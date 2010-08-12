#!/usr/bin/perl -w
#
# Generate services.
#
# $Id: sadi-generate-services.pl,v 1.3 2010-03-09 16:39:14 ubuntu Exp $
# Contact: edward kawas <edward.kawas+sadi@gmail.com>
# -----------------------------------------------------------

# some command-line options
use Getopt::Std;
use vars qw/ $opt_h $opt_A $opt_B $opt_d $opt_v  $opt_s $opt_b $opt_F $opt_S $opt_D $opt_T $opt_g/;
getopts('hdvsbFSADBT');

# usage
if ( $opt_h or @ARGV == 0 ) {
	print STDOUT <<'END_OF_USAGE';
Generate Services.
Usage: [-vds] [-b|S|A|D] service-name [service-name...]

    All parameters for generating services are taken from the 
   'sadi-service.cfg' configuration file.

    -b ... generate base[s] of given service[s]
    -B ... generate asynchronous base[s] of given service[s]

    -S ... generate implementation and the base of service[s], the
           implementation module has enabled option to read the base
           statically (that is why it is also generated here)

    -A ... generate an asynchronous based implementation of the given service

    -D ... generate a definition file for the service that you can fill in

    -T ... generate a unit test for your service that you can fill in

    -g ... generate OWL2Perl modules for your services output OWL class 
           if you havae already created a service definition.

    If none of {-b,-S} given, it generates/show implementation (not a base) of 
    service[s].

    -s ... show generated code on STDOUT
           (no file is created)

    -v ... verbose
    -d ... debug
    -h ... help
END_OF_USAGE
	exit(0);
}

# Undocumented options
# (because it is dangerous, you can loose your code):
#   -F ... force to overwrite existing implementtaion
# -----------------------------------------------------------

use strict;

use SADI::Base;
use SADI::Generators::GenServices;

$LOG->level('INFO')  if $opt_v;
$LOG->level('DEBUG') if $opt_d;

sub say { print @_, "\n"; }

my $generator = new SADI::Generators::GenServices;

say "Generating services for [" . join( ", ", @ARGV ) . "]";

# show code on STDOUT
if ($opt_s) {
	say "\tGenerated code on STDOUT\n";
	my $code = '';
	if ($opt_b) {

		# generate just the base
		$generator->generate_base( service_names => [@ARGV],
								   outcode       => \$code );
	} elsif ($opt_B) {

        # generate just the base
        $generator->generate_async_base( service_names => [@ARGV],
                                   outcode       => \$code );
    } elsif ($opt_S) {

        # generate impl/cgi/base
        $generator->generate_impl(
                                   service_names => [@ARGV],
                                   outcode       => \$code,
                                   force_over    => $opt_F,
                                   static_impl   => 1
        );
        $generator->generate_base( service_names => [@ARGV],
                                    outcode       => \$code );
        $generator->generate_cgi( service_names => [@ARGV],
                                  outcode       => \$code,
                                  force_over    => $opt_F );
    } elsif ($opt_A) {

		# generate async impl/cgi
        $generator->generate_impl( service_names => [@ARGV],
                                   is_async      => 1,
                                   outcode       => \$code );
        $generator->generate_async_cgi(
                                  service_names => [@ARGV],
                                  force_over    => $opt_F,
                                  outcode       => \$code
        );
	} elsif ($opt_D) {

		# generate a definition file to fill in
		$generator->generate_definition( service_names => [@ARGV],
										 outcode       => \$code );
	} elsif ($opt_T) {

        # generate a unit test file to fill in
        $generator->generate_unit_test( service_names => [@ARGV],
                                        outcode       => \$code );
    }else {

		# generate impl/cgi
		$generator->generate_impl( service_names => [@ARGV],
								   outcode       => \$code );
		$generator->generate_cgi(
								  service_names => [@ARGV],
								  force_over    => $opt_F,
								  outcode       => \$code
		);
	}
	say $code;
} else {
	# generate code for a file
	if ($opt_b) {

		# generate just the base
		$generator->generate_base( service_names => [@ARGV] );
	} elsif ($opt_B) {

        # generate just the base
        $generator->generate_async_base( service_names => [@ARGV] );
    } elsif ($opt_S) {

		# generate impl/cgi/base
		$generator->generate_impl(
								   service_names => [@ARGV],
								   force_over    => $opt_F,
								   static_impl   => 1
		);
		$generator->generate_base( service_names => [@ARGV] );
		$generator->generate_cgi( service_names => [@ARGV],
								  force_over    => $opt_F );
	} elsif ($opt_A) {

		# generate impl/cgi
        $generator->generate_impl( service_names => [@ARGV],
                                   is_async      => 1,
                                   force_over    => $opt_F );
        $generator->generate_async_cgi( service_names => [@ARGV],
                                  force_over    => $opt_F );
	} elsif ($opt_D) {

		# generate a definition file to fill in
		$generator->generate_definition( service_names => [@ARGV],
										 force_over    => $opt_F );
	} elsif ($opt_T) {

        # generate a unit test file to fill in
        $generator->generate_unit_test( service_names => [@ARGV],
                                         force_over    => $opt_F );
    } else {

		# generate impl/cgi
		$generator->generate_impl( service_names => [@ARGV],
								   force_over    => $opt_F );
		$generator->generate_cgi( service_names => [@ARGV],
								  force_over    => $opt_F );
	}
}
say 'Done.';

__END__
