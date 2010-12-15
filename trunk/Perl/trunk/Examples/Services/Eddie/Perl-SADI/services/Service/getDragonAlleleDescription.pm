#-----------------------------------------------------------------
# Service name: getDragonAlleleDescription
# Authority:    antirrhinum.net
# Created:      10-May-2010 06:17:56 PDT
# Contact:      edward.kawas@gmail.com
# Description:
#               This service consumes a DragonDB_Allele_Record and retrieves the allele phenotypic description from the Antirrhinum majus (Snapdragon) DragonDB database.
#-----------------------------------------------------------------
package Service::getDragonAlleleDescription;
use FindBin qw( $Bin );
use lib $Bin;

#-----------------------------------------------------------------
# This is a mandatory section - but you can still choose one of
# the two options (keep one and commented out the other):
#-----------------------------------------------------------------
use SADI::Base;

# --- (1) this option loads dynamically everything
BEGIN {
	use SADI::Generators::GenServices;
	new SADI::Generators::GenServices->async_load(
							  service_names => ['getDragonAlleleDescription'] );
}

# (this to stay here with any of the options above)
use vars qw( @ISA );
@ISA = qw( net::antirrhinum::getDragonAlleleDescriptionBase );
use strict;

use Implementations;

sub process_it {
	my ( $self, $values, $core ) = @_;

	# empty data, then return
	return unless $values;
	my @inputs = @$values;

	# iterate over each input
	foreach my $input (@inputs) {
		use SADI::Utils;
        my $id = SADI::Utils->unLSRNize($input, $core);
        unless ($id) {
            $id = $input->getURI;
			# get just the id number
			# example: http://lsrn.org/DragonDB_Allele:cho
	        $id = $1 if $id =~ /.*#(\w+)$/;
	        $id = $1 if $id =~ /.*\/(\w+)$/;
	        $id = $1 if $id =~ /.*:(\w+)$/;
        }
		my $class = Implementations::getDragonAlleleDescription($input, $id, $LOG);
		$core->addOutputData( node => $class);
	}
}
1;
__END__

=head1 NAME

Service::getDragonAlleleDescription - a SADI service

=head1 SYNOPSIS

 # the only thing that you need to do is provide your
 # business logic in process_it()!
 #
 # This method consumes an array reference of input data
 # (RDF::Core::Resource),$values, and a reference to 
 # a SADI::RDF::Core object, $core.
 #
 # Basically, iterate over the the inputs, do your thing
 # and then $core->addOutputData().
 #
 # Since this is an asynchronous implementation of a SADI
 # web service, I am assuming that your task takes a while
 # to run. So to save what you have so far, do store($core).
 # 

=head1 DESCRIPTION

This service consumes a DragonDB_Allele_Record and retrieves the allele phenotypic description from the Antirrhinum majus (Snapdragon) DragonDB database.

=head1 CONTACT

B<Authority>: antirrhinum.net

B<Email>: edward.kawas@gmail.com

=cut
