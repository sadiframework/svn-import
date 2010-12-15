#-----------------------------------------------------------------
# Service name: getGOTermAssociationsFlybase
#    An asynchronous service
# Authority:    illuminae.com
# Created:      19-Aug-2010 13:07:16 PDT
# Contact:      myaddress@organization.org
# Description:  
#               fetches flybase records annotated with a given GO term
#-----------------------------------------------------------------

package Service::getGOTermAssociationsFlybase;

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
	 service_names => ['getGOTermAssociationsFlybase']);
}

# --- (2) this option uses pre-generated module
#  You can generate the module by calling a script:
 #    sadi-generate-services -B getGOTermAssociationsFlybase
#  then comment out the whole option above, and uncomment
#  the following line (and make sure that Perl can find it):
#use SADI::Generators::GenServices;
#use com::illuminae::getGOTermAssociationsFlybaseBase;

# (this to stay here with any of the options above)
use vars qw( @ISA );
@ISA = qw( com::illuminae::getGOTermAssociationsFlybaseBase );
use strict;

# add vocabulary use statements
use SADI::RDF::Predicates::DC_PROTEGE;
use SADI::RDF::Predicates::FETA;
use SADI::RDF::Predicates::OMG_LSID;
use SADI::RDF::Predicates::OWL;
use SADI::RDF::Predicates::RDF;
use SADI::RDF::Predicates::RDFS;

sub process_it {
    my ($self, $values, $core) = @_;
    # empty data, then return
    return unless $values;

    my @inputs = @$values;
    # iterate over each input
    foreach my $input (@inputs) {
    	use SADI::Utils;
        my $accession = SADI::Utils->unLSRNize($input, $core);
        unless ($accession) {
            $accession = $input->getURI;
	        # get just the id number
	        $accession = $1 if $accession =~ /.*#(\w+)$/;
	        $accession = $1 if $accession =~ /.*\/(\w+)$/;
	        $accession = $1 if $accession =~ /.*:(\w+)$/;
	        next unless $accession;
	        next unless $accession =~ /(\d+)$/;
        }
        # format go id
        my $id = sprintf( "GO:%07d", $accession );
        #$LOG->info("$acc is the accession from " . $input->getURI . "\n");
        use Implementations;
        my $class = Implementations::getGOTermAssociationsX($input, $id, $LOG, 'flybase');
        $core->addOutputData( node => $class);
    }
}

1;
__END__

=head1 NAME

Service::getGOTermAssociationsFlybase - a SADI service

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

fetches flybase records annotated with a given GO term

=head1 CONTACT

B<Authority>: illuminae.com

B<Email>: myaddress@organization.org

=cut
