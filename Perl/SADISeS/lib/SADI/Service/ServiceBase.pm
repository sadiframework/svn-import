#-----------------------------------------------------------------
# SADI::Service::ServiceBase
# Author: Edward Kawas <edward.kawas@gmail.com>
# For copyright and disclaimer see below.
#
# $Id: ServiceBase.pm,v 1.8 2010-01-07 21:58:21 ubuntu Exp $
#-----------------------------------------------------------------

package SADI::Service::ServiceBase;

use SADI::Base;
use base qw( SADI::Base );

use strict;

# add versioning to this module
use vars qw /$VERSION/;
$VERSION = sprintf "%d.%02d", q$Revision: 1.9 $ =~ /: (\d+)\.(\d+)/;

#-----------------------------------------------------------------
# process_it
#-----------------------------------------------------------------
sub process_it {
	my ( $self, $values, $service ) = @_;

	# subclass over-rides this
}

#-----------------------------------------------------------------
# as_uni_string
#-----------------------------------------------------------------
use SADI::Data::String;
use Unicode::String;

sub as_uni_string {
	my ( $self, $value ) = @_;
	return new SADI::Data::String( Unicode::String::latin1($value) );
}

#-----------------------------------------------------------------
# log_request
#
# should be called when a request from a client comes; it returns
# information about the current call (request) that can be used in a
# log entry
#-----------------------------------------------------------------

my @ENV_TO_REPORT =
  ( 'REMOTE_ADDR', 'REQUEST_URI' ,'HTTP_USER_AGENT', 'CONTENT_LENGTH', 'CONTENT_TYPE', 'HTTP_ACCEPT' );

sub log_request {
	my ($self) = shift;

	my @buf;
	foreach my $elem (@ENV_TO_REPORT) {
		push( @buf, "$elem: $ENV{$elem}" ) if exists $ENV{$elem};
	}
	return join( ", ", @buf );
}

sub get_service_signature {
	my ( $self, $name ) = @_;
	my $sig = undef;
	eval {
		my $services = SADI::Generators::GenServices->new->read_services( $name, );
		# iterate over the services (should be only 1)
		foreach my $s (@$services) {
			$sig = $s;
			last;
		}
	};
	$LOG->error("Problems retrieving service signature!\n$@") if $@;
	return $sig if $sig;
	$self->throw("Couldn't find a signature for '$name'!.");
}

# returns the request content type
# defaults to application/rdf+xml
sub get_request_content_type {
	my ($self) = @_;
    my $CONTENT_TYPE = 'application/rdf+xml';
    if (defined $ENV{CONTENT_TYPE}) {
        $CONTENT_TYPE = 'text/rdf+n3' if $ENV{CONTENT_TYPE} =~ m|text/rdf\+n3|gi;
        $CONTENT_TYPE = 'text/rdf+n3' if $ENV{CONTENT_TYPE} =~ m|text/n3|gi;
    }
    return $CONTENT_TYPE;
}

# returns the response requested content type
# defaults to application/rdf+xml
sub get_response_content_type {
    my ($self) = @_;
    my $CONTENT_TYPE = 'application/rdf+xml';
    if (defined $ENV{HTTP_ACCEPT}) {
        $CONTENT_TYPE = 'text/rdf+n3' if $ENV{HTTP_ACCEPT} =~ m|text/rdf\+n3|gi;
        $CONTENT_TYPE = 'text/rdf+n3' if $ENV{HTTP_ACCEPT} =~ m|text/n3|gi;
    }
    return $CONTENT_TYPE;
}

1;
__END__

=head1 NAME

SADI::ServiceBase - a super-class for all SADI services

=head1 SYNOPSIS

 use base qw( SADI::ServiceBase )

=head1 DESCRIPTION

A super class for all SADI services implemented with SADISeS.

=head1 SUBROUTINES

=head2 process_it

A job-level processing: B<This is the main method to be overriden by a
service provider!>. Here all the business logic belongs to.

This method is called once for each service invocation request.

Note that here, in C<SADI::Service::ServiceBase>, this method does
nothing. Which means it leaves the output job empty, as it was given
here. Consequence is that if you do not override this method in a 
sub-class, the client will get back an empty request. Which may be 
good just for testing but not really what a client expects (I guess).

You are free to throw an exception (TBD: example here). However, if
you do so the complete processing of the whole client request is
considered failed. After such exception the client will not get any
data back (only an error message).

=head2 as_uni_string

Convert given $value (the only argument) into Unicode and wrap it as a
SADI string (type SADI::Data::String).

=head2 log_request

 # should be called when a request from a client comes; it returns
 # information about the current call (request) that can be used in a
 # log entry

=head2 get_request_content_type

 # Returns the content type of the incoming data, defaults to application/rdf+xml.
 #
 # Possible values: 'application/rdf+xml', 'text/rdf+n3'

=head2 get_response_content_type

 # Returns the requested content type of the outgoing data, defaults to application/rdf+xml.
 #
 # Possible values: 'application/rdf+xml', 'text/rdf+n3'

=head1 AUTHORS, COPYRIGHT, DISCLAIMER

 Edward Kawas  (edward.kawas [at] gmail [dot] com)
 Martin Senger (martin.senger [at] gmail [dot] com)

Copyright (c) 2009 Edward Kawas. All Rights Reserved.

This module is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.

This software is provided "as is" without warranty of any kind.

=cut

