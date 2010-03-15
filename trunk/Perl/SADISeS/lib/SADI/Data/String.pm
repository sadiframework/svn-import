#-----------------------------------------------------------------
# SADI::Data::String
# Author: Edward Kawas <edward.kawas@gmail.com>,
#         Martin Senger <martin.senger@gmail.com>
# For copyright and disclaimer see below.
#
# $Id: String.pm,v 1.3 2010-01-07 21:46:39 ubuntu Exp $
#-----------------------------------------------------------------

package SADI::Data::String;
use base ("SADI::Data::Object");
use strict;

# add versioning to this module
use vars qw /$VERSION/;
$VERSION = sprintf "%d.%02d", q$Revision: 1.3 $ =~ /: (\d+)\.(\d+)/;

=head1 NAME

SADI::Data::String - A primitive SADI data type for strings

=head1 SYNOPSIS

 use SADI::Data::String;

 # create a String with initial value of 'eddie'
 my $data = SADI::Data::String->new (value => 'eddie');
 my $data = SADI::Data::String->new ('eddie');

 # later change the value of this data object
 $data->value ('tulak');
 print $data->value();

=head1 DESCRIPTION

An object representing a String, a SADI primitive data type.

=head1 AUTHORS

 Edward Kawas (edward.kawas [at] gmail [dot] com)
 Martin Senger (martin.senger [at] gmail [dot] com)

=cut

#-----------------------------------------------------------------
# A list of allowed attribute names. See SADI::Base for details.
#-----------------------------------------------------------------

=head1 ACCESSIBLE ATTRIBUTES

Details are in L<SADI::Base>. Here just a list of them (additionally
to the attributes from the parent classes)

=over

=item B<value>

A value of this datatype. Must be an integer.

=back

=cut

{
	my %_allowed = (
					 value => { type => SADI::Base->STRING },
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
	$self->primitive('yes');
}

1;
__END__
