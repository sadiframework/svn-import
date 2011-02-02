#-----------------------------------------------------------------
# SADI::OWL::SADIServiceParser
# Author: Edward Kawas <edward.kawas@gmail.com>,
# For copyright and disclaimer see below.
#
# $Id: SADIServiceParser.pm,v 1.4 2010-03-08 19:28:53 ubuntu Exp $
#-----------------------------------------------------------------
package SADI::OWL::SADIServiceParser;
use strict;

# imports
use RDF::Core::Model::Parser;
use RDF::Core::Model;
use RDF::Core::Storage::Memory;
use RDF::Core::Resource;

use SADI::Utils;
use SADI::Service::Instance;
use SADI::Service::UnitTest;

use SADI::RDF::Predicates::DC_PROTEGE;
use SADI::RDF::Predicates::OMG_LSID;
use SADI::RDF::Predicates::RDF;
use SADI::RDF::Predicates::FETA;
use SADI::RDF::Predicates::RDFS;

use OWL::LSID;

use vars qw /$VERSION/;
$VERSION = sprintf "%d.%02d", q$Revision: 1.4 $ =~ /: (\d+)\.(\d+)/;

=pod

=head1 NAME

ServiceParser - An module for parsing the RDF that describes a SADI service in the mygrid service ontology

=cut

=head1 SYNOPSIS

	use SADI::OWL::SADIServiceParser;
	use Data::Dumper;

	# construct a parser for service instances
	my $parser = SADI::OWL::SADIServiceParser->new();

	# get all services from a URL
	my $service_arref = $parser->getServices(
	   'http://sadiframework.org/services/getMolecularInteractions'
	);

	# print out details regarding 'getMolecularInteractions'
	print Dumper( $service_arrayref );

=cut

=head1 DESCRIPTION

This module contains the methods required to download and parse service instance RDF into individual services

=cut

=head1 WARNING

Do not attempt to parse service instance RDF containing more than a few hundred services because the RDF is
parsed and held in memory.

=cut

=head1 AUTHORS

 Edward Kawas (edward.kawas [at] gmail [dot] com)

=cut

=head1 SUBROUTINES

=cut

=head2 new

Contructs a new ServiceParser.

Input: none.

Example: 

	SADI::OWL::SADIServiceParser->new()

=cut

sub new {
	my ($class) = @_;

	# create an object
	my $self = bless {}, ref($class) || $class;

	# done
	return $self;
}

=pod

=head2 getServices 

Downloads RDF from $url, parses it and returns an arrayref of C<SADI::Service::Instance>.

Input: a scalar URL 

Example:

	my $parser = SADI::OWL::SADIServiceParser->new();
	my $service_arref = $parser->getServices(
	   'http://sadiframework.org/services/getMolecularInteractions'
	);

=cut

sub getServices {

	my ( $self, $url ) = @_;
	my @services;
	return \@services unless $url;

	# download string from url
	my $rdf = undef;

	# 'try/catch'
	eval { $rdf = SADI::Utils::getHttpRequestByURL($url); };
	print $@;
	return \@services unless $rdf;

	# create RDF model and populate
	my $storage = new RDF::Core::Storage::Memory;
	my $model = new RDF::Core::Model( Storage => $storage );
	my %options = (
					Model      => $model,
					Source     => $rdf,
					SourceType => 'string',
					BaseURI    => "$url",
	);
	my $parser = new RDF::Core::Model::Parser(%options);
	$parser->parse;

	# get information from the model
	my $enumerator = $model->getStmts(
									   undef, undef,
									   new RDF::Core::Resource(
															 SADI::RDF::Predicates::FETA
															   ->serviceDescription(
															   )
									   )
	);
	my $statement = $enumerator->getFirst;
	while ( defined $statement ) {
		my $instance = SADI::Service::Instance->new;

		# set the name for the service
		my $val = $model->getObjects(
									  $statement->getSubject,
									  new RDF::Core::Resource(
															 SADI::RDF::Predicates::FETA
															   ->hasServiceNameText
									  )
		);
		$val = "" unless $$val[0];
		$val = $$val[0]->getValue if ref($val) eq 'ARRAY' and $$val[0];
		$instance->ServiceName( SADI::Utils::trim($val) );

		# set the category
		$val = $model->getObjects(
								   $statement->getSubject,
								   new RDF::Core::Resource(
											   SADI::RDF::Predicates::DC_PROTEGE->format
								   )
		);
		$val = "" unless $$val[0];
		$val = $$val[0]->getValue if ref($val) eq 'ARRAY' and $$val[0];
		$instance->Format( SADI::Utils::trim($val) );

		# set the lsid
		$val = $model->getObjects(
								   $statement->getSubject,
								   new RDF::Core::Resource(
										   SADI::RDF::Predicates::DC_PROTEGE->identifier
								   )
		);
		$val = "" unless $$val[0];
		$val = $$val[0]->getValue if ref($val) eq 'ARRAY' and $$val[0];
		$instance->UniqueIdentifier( SADI::Utils::trim($val) );

		# set the url
		$val = $model->getObjects(
								   $statement->getSubject,
								   new RDF::Core::Resource(
												SADI::RDF::Predicates::FETA->locationURI
								   )
		);
		$val = "" unless $$val[0];
		$val = $$val[0]->getValue if ref($val) eq 'ARRAY' and $$val[0];
		$instance->ServiceURI( SADI::Utils::trim($val) );
		$instance->URL( SADI::Utils::trim($val) );

		# set the service description text
		$val = $model->getObjects(
								   $statement->getSubject,
								   new RDF::Core::Resource(
														   SADI::RDF::Predicates::FETA
															 ->hasServiceDescriptionText
								   )
		);
		$val = "" unless $$val[0];
		$val = $$val[0]->getValue if ref($val) eq 'ARRAY' and $$val[0];
		$instance->Description( SADI::Utils::trim($val) );
		
		# set the service description location url
		$val = $model->getObjects(
								   $statement->getSubject,
								   new RDF::Core::Resource(
														   SADI::RDF::Predicates::FETA
															 ->hasServiceDescriptionLocation
								   )
		);
		$val = "" unless $$val[0];
		$val = $$val[0]->getValue if ref($val) eq 'ARRAY' and $$val[0];
		$instance->SignatureURL( SADI::Utils::trim($val) );

		# get providedBy node
		my $providedBy =
		  $model->getObjects(
							  $statement->getSubject,
							  new RDF::Core::Resource(
												 SADI::RDF::Predicates::FETA->providedBy
							  )
		  );
		$providedBy = [] unless @$providedBy;
		$providedBy = $$providedBy[0]
		  if ref($providedBy) eq 'ARRAY' and $$providedBy[0];
		if ($providedBy) {

			# set the authoritative
			$val = $model->getObjects(
									   $providedBy,
									   new RDF::Core::Resource(
											  SADI::RDF::Predicates::FETA->authoritative
									   )
			);
			$val = "" unless $$val[0];
			$val = $$val[0]->getValue if ref($val) eq 'ARRAY' and $$val[0];
			$instance->Authoritative(
									 SADI::Utils::trim($val) =~ m/true/i ? 1 : 0 );

			# set the contact email
			$val = $model->getObjects(
									   $providedBy,
									   new RDF::Core::Resource(
											  SADI::RDF::Predicates::DC_PROTEGE->creator
									   )
			);
			$val = "" unless $$val[0];
			$val = $$val[0]->getValue if ref($val) eq 'ARRAY' and $$val[0];
			$instance->Provider( SADI::Utils::trim($val) );

			# set the authority uri
			$val = $model->getObjects(
									   $providedBy,
									   new RDF::Core::Resource(
											SADI::RDF::Predicates::DC_PROTEGE->publisher
									   )
			);
			$val = "" unless $$val[0];
			$val = $$val[0]->getValue if ref($val) eq 'ARRAY' and $$val[0];
			$instance->Authority( SADI::Utils::trim($val) );
		}

		# no longer need the providedBy node
		$providedBy = undef;

		# get hasOperation node
		my $hasOperation =
		  $model->getObjects(
							  $statement->getSubject,
							  new RDF::Core::Resource(
											   SADI::RDF::Predicates::FETA->hasOperation
							  )
		  );
		$hasOperation = [] unless @$hasOperation;
		$hasOperation = $$hasOperation[0]
		  if ref($hasOperation) eq 'ARRAY' and $$hasOperation[0];

		# if this is missing ... what's the point?
		next unless $hasOperation;

		# process any inputs
		my $inputs = $model->getObjects(
										 $hasOperation,
										 new RDF::Core::Resource(
											 SADI::RDF::Predicates::FETA->inputParameter
										 )
		);
		$inputs = [] unless @$inputs;
		foreach my $input (@$inputs) {
			# get the parameterName and objectType
			$val = $model->getObjects(
									   $input,
									   new RDF::Core::Resource(
															 SADI::RDF::Predicates::FETA->objectType()
									   )
			);
			$val = "" unless $$val[0];
			$val = $$val[0]->getURI if ref($val) eq 'ARRAY' and $$val[0];
			$instance->InputClass( SADI::Utils::trim($val) );
		}

		# dont need $inputs
		$inputs = undef;

		# process any outputs
		my $outputs = $model->getObjects(
										  $hasOperation,
										  new RDF::Core::Resource(
															 SADI::RDF::Predicates::FETA
															   ->outputParameter
										  )
		);
		$outputs = [] unless @$outputs;
		foreach my $output (@$outputs) {
			# get the parameterName and objectType
			$val = $model->getObjects(
									   $output,
									   new RDF::Core::Resource(
															 SADI::RDF::Predicates::FETA->objectType()
									   )
			);
			$val = "" unless $$val[0];
			$val = $$val[0]->getURI if ref($val) eq 'ARRAY' and $$val[0];
			$instance->OutputClass( SADI::Utils::trim($val) );
		}

		# dont need $outputs
		$outputs = undef;

		# process the performsTask
		# get performsTask node
		my $performs =
		  $model->getObjects(
							  $hasOperation,
							  new RDF::Core::Resource(
											   SADI::RDF::Predicates::FETA->performsTask
							  )
		  );
		$performs = [] unless @$performs;
		$performs = $$performs[0]
		  if ref($performs) eq 'ARRAY' and $$performs[0];
		$val = $model->getObjects(
								   $performs,
								   new RDF::Core::Resource(
														SADI::RDF::Predicates::RDF->type
								   )
		);
		if ( $$val[0] ) {
			for my $uri (@$val) {
				$val = $uri->getURI
				  if $uri->getURI ne SADI::RDF::Predicates::FETA->operationTask();
				last
				  if $uri->getURI ne SADI::RDF::Predicates::FETA->operationTask();
			}
		}
		$val = "" if ref($val) eq 'ARRAY';
		$instance->ServiceType( SADI::Utils::trim($val) );

		# dont need the performsTask node anymore
		$performs = undef;

		# process any unit test information
        my $unit_test =
          $model->getObjects(
                              $hasOperation,
                              new RDF::Core::Resource(
                                        SADI::RDF::Predicates::FETA->hasUnitTest
                              )
          );
        $unit_test = [] unless @$unit_test;
        foreach my $ut (@$unit_test) {
            my $unit = new SADI::Service::UnitTest;

            # get example input
            $val =
              $model->getObjects(
                                  $ut,
                                  new RDF::Core::Resource(
                                       SADI::RDF::Predicates::FETA->exampleInput
                                  )
              );
            $val = "" unless $$val[0];
            $val = $$val[0]->getValue if ref($val) eq 'ARRAY' and $$val[0];
            $unit->input( SADI::Utils::trim($val) );

            # get example output
            $val =
              $model->getObjects(
                                  $ut,
                                  new RDF::Core::Resource(
                                                     SADI::RDF::Predicates::FETA
                                                       ->validOutputXML
                                  )
              );
            $val = "" unless $$val[0];
            $val = $$val[0]->getValue if ref($val) eq 'ARRAY' and $$val[0];
            $unit->output( SADI::Utils::trim($val) );

            # get regex
            $val =
              $model->getObjects(
                                  $ut,
                                  new RDF::Core::Resource(
                                         SADI::RDF::Predicates::FETA->validREGEX
                                  )
              );
            $val = "" unless $$val[0];
            $val = $$val[0]->getValue if ref($val) eq 'ARRAY' and $$val[0];
            $unit->regex( SADI::Utils::trim($val) );

            # get xpath
            $val =
              $model->getObjects(
                                  $ut,
                                  new RDF::Core::Resource(
                                         SADI::RDF::Predicates::FETA->validXPath
                                  )
              );
            $val = "" unless $$val[0];
            $val = $$val[0]->getValue if ref($val) eq 'ARRAY' and $$val[0];
            $unit->xpath( SADI::Utils::trim($val) );

            # add the unit test in the service
            push @{ $instance->UnitTest }, $unit;
        }


		# this service is done ...
		push @services, $instance;

		# next if any
		$statement = $enumerator->getNext;
	}
	$enumerator->close;

	# return array ref
	return \@services;

}

1;

__END__
