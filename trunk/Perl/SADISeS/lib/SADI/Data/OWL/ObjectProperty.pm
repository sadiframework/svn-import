#-----------------------------------------------------------------
# SADI::Data::OWL::ObjectProperty
# Author: Edward Kawas <edward.kawas@gmail.com>,
# For copyright and disclaimer see below.
#
# $Id: ObjectProperty.pm,v 1.2 2009-10-27 16:24:10 ubuntu Exp $
#-----------------------------------------------------------------
package SADI::Data::OWL::ObjectProperty;
use base ("SADI::Base");
use strict;

# imports
use RDF::Core::Resource;
use RDF::Core::Statement;

use SADI::RDF::Predicates::DC_PROTEGE;
use SADI::RDF::Predicates::FETA;
use SADI::RDF::Predicates::OMG_LSID;
use SADI::RDF::Predicates::OWL;
use SADI::RDF::Predicates::RDF;
use SADI::RDF::Predicates::RDFS;

# add versioning to this module
use vars qw /$VERSION/;
$VERSION = sprintf "%d.%02d", q$Revision: 1.2 $ =~ /: (\d+)\.(\d+)/;

=head1 NAME

SADI::Data::OWL::ObjectProperty

=head1 SYNOPSIS

 use SADI::Data::OWL::ObjectProperty;

 # create a sadi owl ObjectProperty
 my $data = SADI::Data::OWL::ObjectProperty->new ();


=head1 DESCRIPTION

An object representing an OWL ObjectProperty

=head1 AUTHORS

 Edward Kawas (edward.kawas [at] gmail [dot] com)

=cut

#-----------------------------------------------------------------
# A list of allowed attribute names. See SADI::Base for details.
#-----------------------------------------------------------------

=head1 ACCESSIBLE ATTRIBUTES

Details are in L<SADI::Base>. Here just a list of them:

=over

=item B<value> - the subject, a URI, that this object property is a predicate for

=item B<range> - the range of this object property

=item B<domain> - the domain of this object property

=item B<uri> - the uri of this object property

=back

=cut

{
	my %_allowed = (
		value  => { type => SADI::Base->STRING },
		range  => { type => SADI::Base->STRING },
		domain => { type => SADI::Base->STRING },
		uri    => { type => SADI::Base->STRING },
		      #statements => {type => 'RDF::Core::Statement', is_array => 1}
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

=head1 SUBROUTINES

=cut

#-----------------------------------------------------------------
# init
#-----------------------------------------------------------------
sub init {
	my ($self) = shift;
	$self->SUPER::init();
}

sub get_statement {
	my $self     = shift;
	$self->throw('Range not set for Object property!') unless defined $self->range;
	
	my $subject = new RDF::Core::Resource( $self->value );
	my $predicate = $subject->new( SADI::RDF::Predicates::RDF->type );
	my $object = new RDF::Core::Resource( $self->range );
	return new RDF::Core::Statement($subject, $predicate, $object)
}
1;
__END__
