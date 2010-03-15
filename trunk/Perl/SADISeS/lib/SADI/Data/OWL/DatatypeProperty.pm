#-----------------------------------------------------------------
# SADI::Data::OWL::DatatypeProperty
# Author: Edward Kawas <edward.kawas@gmail.com>,
# For copyright and disclaimer see below.
#
# $Id: DatatypeProperty.pm,v 1.5 2009-11-12 21:11:30 ubuntu Exp $
#-----------------------------------------------------------------
package SADI::Data::OWL::DatatypeProperty;
use base ("SADI::Base");
use strict;

# imports
use RDF::Core::Resource;
use RDF::Core::Statement;
use RDF::Core::Literal;
use RDF::Core::NodeFactory;

use SADI::RDF::Predicates::DC_PROTEGE;
use SADI::RDF::Predicates::FETA;
use SADI::RDF::Predicates::OMG_LSID;
use SADI::RDF::Predicates::OWL;
use SADI::RDF::Predicates::RDF;
use SADI::RDF::Predicates::RDFS;

# add versioning to this module
use vars qw /$VERSION/;
$VERSION = sprintf "%d.%02d", q$Revision: 1.5 $ =~ /: (\d+)\.(\d+)/;

=head1 NAME

SADI::Data::OWL::DatatypeProperty

=head1 SYNOPSIS

 use SADI::Data::OWL::DatatypeProperty;

 # create a sadi owl DatatypeProperty
 my $data = SADI::Data::OWL::DatatypeProperty->new ();


=head1 DESCRIPTION

An object representing an OWL DatatypeProperty

=head1 AUTHORS

 Edward Kawas (edward.kawas [at] gmail [dot] com)

=cut

#-----------------------------------------------------------------
# A list of allowed attribute names. See SADI::Base for details.
#-----------------------------------------------------------------

=head1 ACCESSIBLE ATTRIBUTES

Details are in L<SADI::Base>. Here just a list of them:

=over

=item B<value> - the value that this datatype property assumes

=item B<range> - the range of this datatype property

=item B<domain> - the domain for this datatype property

=item B<uri> - the uri of this datatype property

=back

=cut

=head1 subroutines

=cut

{
	my %_allowed = (
		value  => { 
			type => SADI::Base->STRING,
		},
		range   => { type => SADI::Base->STRING },
		domain  => { type => SADI::Base->STRING },
		uri     => { type => SADI::Base->STRING },
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

1;
__END__
