#-----------------------------------------------------------------
# SADI::Service::Instance
# Author: Edward Kawas <edward.kawas@gmail.com>,
#
# For copyright and disclaimer see below.
#
# $Id: Instance.pm,v 1.6 2010-03-23 16:30:16 ubuntu Exp $
#-----------------------------------------------------------------
package SADI::Simple::ServiceDescription;

use SADI::Simple::Utils;
use base ("SADI::Simple::Base");
use strict;

# add versioning to this module
use vars qw /$VERSION/;
$VERSION = sprintf "%d.%02d", q$Revision: 1.7 $ =~ /: (\d+)\.(\d+)/;

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
#		ServiceName      => { type => SADI::Base->STRING },
#		ServiceType      => { type => SADI::Base->STRING },
#		InputClass       => { type => SADI::Base->STRING },
#		OutputClass      => { type => SADI::Base->STRING },
#		Description      => { type => SADI::Base->STRING },
#		UniqueIdentifier => { type => SADI::Base->STRING },
		ServiceName      => { type => SADI::Simple::Base->STRING },
		ServiceType      => { type => SADI::Simple::Base->STRING },
		InputClass       => { type => SADI::Simple::Base->STRING },
		OutputClass      => { type => SADI::Simple::Base->STRING },
		Description      => { type => SADI::Simple::Base->STRING },
		UniqueIdentifier => { type => SADI::Simple::Base->STRING },
		Authority        => {
#			type => SADI::Base->STRING,
			type => SADI::Simple::Base->STRING,
			post => sub {
				my $i      = shift;
				my $domain = $i->Authority; 
				if ($domain =~ /[\@\&\%\#\(\)\=]/gi) {
					$i->{Authority} = ""; 
                    $i->throw(
    "Invalid authority specified! '$domain' contains invalid characters ."
                    );
				}
				  unless ($domain =~ /.+\.+.+/gi) {
				  	$i->{Authority} = "";
                    $i->throw(
                       "Invalid authority specified! '$domain' must take the form NNN.NNN.NNN." );
				  }
				my $uri = new URI($domain);
                $i->{Authority} = $uri->authority if defined $uri->authority;
			  }
		},
		Provider => {
#			type => SADI::Base->STRING,
			type => SADI::Simple::Base->STRING,
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
#		ServiceURI => { type => SADI::Base->STRING },
		ServiceURI => { type => SADI::Simple::Base->STRING },
		URL        => {
#			type => SADI::Base->STRING,
			type => SADI::Simple::Base->STRING,
			post => sub {
				my $i = shift;

				# set the signature url to be the URL address unless defined
				$i->SignatureURL( $i->URL ) unless $i->SignatureURL;
				# set the service uri to be the URL address unless defined
                $i->ServiceURI( $i->URL ) unless $i->ServiceURI;
                # set the unique id to be the URL address unless defined
                $i->UniqueIdentifier( $i->URL ) unless $i->UniqueIdentifier;
			  }
		},
#		Authoritative => { type => SADI::Base->BOOLEAN },
		Authoritative => { type => SADI::Simple::Base->BOOLEAN },
#		Format        => { type => SADI::Base->STRING },
		Format        => { type => SADI::Simple::Base->STRING },
#		SignatureURL  => { type => SADI::Base->STRING },
		SignatureURL  => { type => SADI::Simple::Base->STRING },
#		UnitTest     => { type => 'SADI::Service::UnitTest', is_array => 1 },
		UnitTest     => { type => 'SADI::Simple::UnitTest', is_array => 1 },
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

use constant SERVICE_DESCRIPTION_TEMPLATE => <<TEMPLATE;
[%# A template for a sadi service signature
    ===========================

    Expected/recognized parameters:
      name - the name of the SADI service
      uri  - the service uri
      type - the service type
      input - the input class URI
      output - the output class URI
      desc - a description for this service
      id - a unique identifier (LSID, etc)
      email - the providers email address
      format - the category of service (sadi)
      url - the service url
      authoritative - whether or not the service is authoritative
      authority - the service authority URI
      sigURL - the url to the service signature
      tests - an array SADI::Service::UnitTest
-%]
[% SET COUNTER = 0 %]
[% SET TEST_COUNTER = 1 %]
<?xml version="1.0" encoding="UTF-8"?>
<rdf:RDF
 xmlns="http://www.w3.org/2002/07/owl#"
 xmlns:a="http://www.mygrid.org.uk/mygrid-moby-service#"
 xmlns:b="http://purl.org/dc/elements/1.1/"
 xml:base="http://bioinfo.icapture.ubc.ca/SADI"
 xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
 xmlns:databases="http://sadiframework.org/ontologies/Databases.owl#"
 xmlns:misc="http://sadiframework.org/ontologies/miscellaneousObjects.owl#"
 xmlns:owl="http://www.w3.org/2002/07/owl#"
 xmlns:xsd="http://www.w3.org/2001/XMLSchema#"
 xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">    
  <rdf:Description rdf:about="[% uri %]">
    <rdf:type rdf:resource="http://www.mygrid.org.uk/mygrid-moby-service#serviceDescription"/>
    <b:format>[% format %]</b:format>
    <b:identifier>[% id %]</b:identifier>
    <a:locationURI>[% url %]</a:locationURI>
    <a:hasServiceDescriptionText>[% FILTER xml %][% desc %][% END %]</a:hasServiceDescriptionText>
    [%- IF sigURL == '' %]
    <a:hasServiceDescriptionLocation/>[% ELSE %]
    <a:hasServiceDescriptionLocation>[% sigURL %]</a:hasServiceDescriptionLocation>[%END%]
    <a:hasServiceNameText>[% name %]</a:hasServiceNameText>
    <a:providedBy>
        <rdf:Description rdf:about="[% name %]_[% authority %]_[% GET COUNTER %]">[% SET COUNTER = COUNTER+1 %]
            <a:authoritative rdf:datatype="http://www.w3.org/2001/XMLSchema#boolean">[% authoritative %]</a:authoritative>
            <b:creator>[% email %]</b:creator>
            <b:publisher>[% authority %]</b:publisher>
            <rdf:type rdf:resource="http://www.mygrid.org.uk/mygrid-moby-service#organisation"/>
        </rdf:Description>
    </a:providedBy>
    <a:hasOperation>
        <rdf:Description rdf:about="[% name %]_[% authority %]_[% GET COUNTER %]">[% SET COUNTER = COUNTER+1 %]
            <a:hasOperationNameText>[% name %]</a:hasOperationNameText>
            <rdf:type rdf:resource="http://www.mygrid.org.uk/mygrid-moby-service#operation"/>
            <a:performsTask>
                <rdf:Description rdf:about="[% name %]_[% authority %]_[% GET COUNTER %]">[% SET COUNTER = COUNTER+1 %]
                    <rdf:type rdf:resource="http://www.mygrid.org.uk/mygrid-moby-service#operationTask"/>
                    <rdf:type rdf:resource="[% type %]"/>
                </rdf:Description>
            </a:performsTask>
            <a:inputParameter>
                <rdf:Description rdf:about="[% name %]_[% authority %]_[% GET COUNTER %]">[% SET COUNTER = COUNTER+1 %]
                    <rdf:type rdf:resource="http://www.mygrid.org.uk/mygrid-moby-service#parameter"/>
                    <a:objectType>
                        <rdf:Description rdf:about="[% input %]"/>
                    </a:objectType>
                </rdf:Description>
            </a:inputParameter>
            <a:outputParameter>
                <rdf:Description rdf:about="[% name %]_[% authority %]_[% GET COUNTER %]">[% SET COUNTER = COUNTER+1 %]
                    <rdf:type rdf:resource="http://www.mygrid.org.uk/mygrid-moby-service#parameter"/>
                    <a:objectType>
                        <rdf:Description rdf:about="[% output %]"/>
                    </a:objectType>
                </rdf:Description>
            </a:outputParameter>[% FOREACH test IN tests %]
            <a:hasUnitTest>
                    <rdf:Description rdf:about="[% name %]_[% authority %]_TEST_[% GET TEST_COUNTER %]">[% SET TEST_COUNTER = TEST_COUNTER + 1 %]
                        <rdf:type rdf:resource="http://www.mygrid.org.uk/mygrid-moby-service#unitTest"/>
[%- IF test.regex != '' %]
                        <a:validREGEX>[% FILTER xml %][%- test.regex -%][% END %]</a:validREGEX>[% END %]
[%- IF test.xpath != '' %]
                        <a:validXPath>[% FILTER xml %][%- test.xpath -%][% END %]</a:validXPath>[% END %]
[%- IF test.input != '' %]
    [%- SET file = test.input %]
                        <a:exampleInput>[%- FILTER xml -%][%- TRY -%][%- INSERT \$file -%]
    [%- CATCH -%][%-  test.input  -%][%- END -%][%- END -%]</a:exampleInput>[% END %]
[%- IF test.output != '' %]
    [%- SET file = test.output %]
                        <a:validOutputXML>[%- FILTER xml -%][%- TRY -%][%- INSERT \$file -%]
    [%- CATCH -%][%-  test.output  -%][% END %][%- END -%]</a:validOutputXML>[% END %]
                    </rdf:Description>
            </a:hasUnitTest>[% END %]
        </rdf:Description>
    </a:hasOperation>
  </rdf:Description>
</rdf:RDF>
TEMPLATE

#-----------------------------------------------------------------
# init
#-----------------------------------------------------------------
sub init {
	my ($self) = shift;
	$self->SUPER::init();

	# set the default format for this signature
	$self->Format('sadi');
	$self->Authoritative(0);
}

sub getServiceInterface {

	my ($self, $content_type) = @_;

    my $LOG = Log::Log4perl->get_logger(__PACKAGE__);

    $content_type ||= 'application/rdf+xml';

	# generate from template

	my $tt = Template->new(ABSOLUTE => 1, TRIM => 1);

    my $input = SERVICE_DESCRIPTION_TEMPLATE;
	my $sadi_interface_signature;

	$tt->process(

				  \$input,
				  {
					 name          => $self->ServiceName,
					 uri           => $self->ServiceURI,
					 type          => $self->ServiceType,
					 input         => $self->InputClass,
					 output        => $self->OutputClass,
					 desc          => $self->Description,
					 id            => $self->UniqueIdentifier || $self->ServiceURI,
					 email         => $self->Provider,
					 format        => $self->Format,
					 url           => $self->URL,
					 authoritative => $self->Authoritative,
					 authority     => $self->Authority,
					 sigURL        => $self->SignatureURL,
					 tests         => $self->UnitTest || (),
				  },
				  \$sadi_interface_signature

	) || $LOG->logdie( $tt->error() );

    if ($content_type eq 'text/rdf+n3') {
        return SADI::Simple::Utils->rdfxml_to_n3($sadi_interface_signature);
    }

	return $sadi_interface_signature;
}
1;

__END__
