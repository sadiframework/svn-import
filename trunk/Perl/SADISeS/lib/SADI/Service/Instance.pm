#-----------------------------------------------------------------
# SADI::Service::Instance
# Author: Edward Kawas <edward.kawas@gmail.com>,
#
# For copyright and disclaimer see below.
#
# $Id: Instance.pm,v 1.5 2010-03-08 19:29:02 ubuntu Exp $
#-----------------------------------------------------------------
package SADI::Service::Instance;

use SADI::Base;
use base ("SADI::Base");
use strict;

# add versioning to this module
use vars qw /$VERSION/;
$VERSION = sprintf "%d.%02d", q$Revision: 1.5 $ =~ /: (\d+)\.(\d+)/;

=head1 NAME

SADI::Service::Instance - A module that describes a SADI web service.

=head1 SYNOPSIS

 use SADI::Service::Instance;

 # create a new blank SADI service instance object
 my $data = SADI::Service::Instance->new ();

 # create a new primed SADI service instance object
 $data = SADI::Service::Instance->new (
     ServiceName => "helloworld",
     ServiceType => "http://someontology.org/services/sometype",
     InputClass => "http://someontology.org/datatypes#Input1",
     OutputClass => "http://someontology.org/datatypes#Output1",
     Description => "the usual hello world service",
     UniqueIdentifier => "urn:lsid:myservices:helloworld",
     Authority => "helloworld.com",
     Authoritative => 1,
     Provider => 'myaddress@organization.org',
     ServiceURI => "http://helloworld.com/cgi-bin/helloworld.pl",
     URL => "http://helloworld.com/cgi-bin/helloworld.pl",
     SignatureURL =>"http://foo.bar/myServiceDescription",
 );

 # get the service name
 my $name = $data->ServiceName;
 # set the service name
 $data->ServiceName($name);

 # get the service type
 my $type = $data->ServiceType;
 # set the service type
 $data->ServiceType($type);

 # get the input class URI
 my $input_class = $data->InputClass;
 # set the input class URI
 $data->InputClass($input_class);

 # get the output class URI
 my $output_class = $data->OutputClass;
 # set the output class URI
 $data->OutputClass($input_class);

 # get the description
 my $desc = $data->Description;
 # set the description
 $data->Description($desc);

 # get the unique id
 my $id = $data->UniqueIdentifier;
 # set the unique id
 $data->UniqueIdentifier($id);

 # get the authority
 my $auth = $data->Authority;
 # set the authority
 $data->Authority($auth);

 # get the service provider URI
 my $uri = $data->Provider;
 # set the service provider URI
 $data->Provider($uri);

 # get the service URI
 my $uri = $data->ServiceURI;
 # set the service URI
 $data->ServiceURI($uri);

 # get the service URL
 my $url = $data->URL;
 # set the service URL
 $data->URL($url);

 # get the signature url
 my $sig = $data->SignatureURL;
 # set the signature url
 $data->SignatureURL($sig);

=head1 DESCRIPTION

An object representing a SADI service signature.

=head1 AUTHORS

 Edward Kawas (edward.kawas [at] gmail [dot] com)

=cut

#-----------------------------------------------------------------
# A list of allowed attribute names. See SADI::Base for details.
#-----------------------------------------------------------------

=head1 ACCESSIBLE ATTRIBUTES

Details are in L<SADI::Base>. Here just a list of them (additionally
to the attributes from the parent classes)

=over

=item B<ServiceName>

A name for the service.

=item B<ServiceType>

Our SADI service type.

=item B<InputClass>

The URI to the input class for our SADI service.

=item B<OutputClass>

The URI to the output class for our SADI service.

=item B<Description>

A description for our SADI service.

=item B<UniqueIdentifier>

A unique identifier (like an LSID, etc) for our SADI service.

=item B<Authority>

The service provider URI for our SADI service.

=item B<ServiceURI>

The service URI for our SADI service.

=item B<URL>

The URL to our SADI service.

=item B<Provider>

The email address of the service provider. 
B<Note: This method throws an exception if the address is syntactically invalid!>.

=item B<Authoritative>

Whether or not the provider of the SADI service is an authority over the data. 
This value must be a boolean value. True values match =~ /true|\+|1|yes|ano/. 
All other values are false.

Defaults to 1;

=item B<Format>

The format of the service. More than likely, it will be 'sadi' if it is a SADI web service.

=item B<SignatureURL>

A url to the SADI service signature.

=back

=cut

{
	my %_allowed = (
		ServiceName      => { type => SADI::Base->STRING },
		ServiceType      => { type => SADI::Base->STRING },
		InputClass       => { type => SADI::Base->STRING },
		OutputClass      => { type => SADI::Base->STRING },
		Description      => { type => SADI::Base->STRING },
		UniqueIdentifier => { type => SADI::Base->STRING },
		Authority        => {
			type => SADI::Base->STRING,
			post => sub {
				my $i      = shift;
				my $domain = $i->Authority;
				$i->throw(
"Invalid authority specified! '$domain' contains invalid characters ."
				) if $domain =~ /[\@\&\%\#\(\)\=]/gi;
				$i->throw(
					   "Invalid authority specified! '$domain' must take the form NNN.NNN.NNN." )
				  unless $domain =~ /.+\.+.+/gi;
			  }
		},
		Provider => {
			type => SADI::Base->STRING,
			post => sub {
				my $i = shift;
				my ( $name, $domain ) = $i->Provider =~ /^(.*)@(.*)$/;
				$i->throw(   "Invalid email address specified! '"
						   . $i->Provider
						   . "' is not a valid address" )
				  unless $name and $domain;
				$i->throw(
"Invalid email address specified! Invalid characters found in username."
				) if $name =~ /[\@\&\%\#\(\)\=]/gi;
				$i->throw(
"Invalid email address specified! Invalid characters found in domain."
				) if $domain =~ /[\@\&\%\#\(\)\=]/gi;
				$i->throw(
"Invalid email address specified! Please check the domain of the address."
				) unless $domain =~ /.+\.+.+/gi;

			  }
		},
		ServiceURI => { type => SADI::Base->STRING },
		URL        => {
			type => SADI::Base->STRING,
			post => sub {
				my $i = shift;

				# set the signature url to be the URL address unless defined
				$i->SignatureURL( $i->URL ) unless $i->SignatureURL;
			  }
		},
		Authoritative => { type => SADI::Base->BOOLEAN },
		Format        => { type => SADI::Base->STRING },
		SignatureURL  => { type => SADI::Base->STRING },
		UnitTest     => { type => 'SADI::Service::UnitTest', is_array => 1 },
	);

	sub _accessible {
		my ( $self, $attr ) = @_;
		exists $_allowed{$attr} or $self->SUPER::_accessible($attr);
	}

	sub _attr_prop {
		my ( $self, $attr_name, $prop_name ) = @_;
		my $attr = $_allowed{$attr_name};
		return ref($attr) ? $attr->{$prop_name} : $attr if $attr;
		return $self->SUPER::_attr_prop( $attr_name, $prop_name );
	}
}

#-----------------------------------------------------------------
# init
#-----------------------------------------------------------------
sub init {
	my ($self) = shift;
	$self->SUPER::init();

	# set the default format for this signature
	$self->Format('sadi');
	$self->Authoritative(1);
}

1;

__END__
