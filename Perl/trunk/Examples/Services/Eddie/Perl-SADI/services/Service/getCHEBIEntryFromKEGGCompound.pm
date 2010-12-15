#-----------------------------------------------------------------
# Service name: getCHEBIEntryFromKEGGCompound
# Authority:    mark.ubic.ca
# Created:      05-Feb-2010 08:15:09 PST
# Contact:      markw@organization.org
# Description:  
#               This service uses the TOGO web service interface to retrieve the CHEBI ID corresponding to a KEGG compound ID
#-----------------------------------------------------------------

package Service::getCHEBIEntryFromKEGGCompound;

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
	 service_names => ['getCHEBIEntryFromKEGGCompound']);
}

# (this to stay here with any of the options above)
use vars qw( @ISA );
@ISA = qw( ca::ubic::mark::getCHEBIEntryFromKEGGCompoundBase );
use strict;

# add vocabulary use statements
use SADI::RDF::Predicates::DC_PROTEGE;
use SADI::RDF::Predicates::FETA;
use SADI::RDF::Predicates::OMG_LSID;
use SADI::RDF::Predicates::OWL;
use SADI::RDF::Predicates::RDF;
use SADI::RDF::Predicates::RDFS;

use Implementations;

sub process_it {
    my ($self, $values, $core) = @_;
    # empty data, then return
    return unless $values;

    my @inputs = @$values;
    # iterate over each input
    foreach my $input (@inputs) {
        # do something with $input ... (sorry, can't help with that)
        use SADI::Utils;
        my $accession = SADI::Utils->unLSRNize($input, $core);
        unless ($accession) {
            $accession = $input->getURI;
	        # extract the kegg compound id
	        if ($accession =~ m/KEGG_COMPOUND:(.*)/gi) {
	            $accession = $1;	
	        } else {
	        	next;
	        }
        }
        my $class = Implementations::getCHEBIEntryFromKEGGCompound($input, $accession, $LOG);
        # fill in the output nodes - this is what you need to do!
        $core->addOutputData(node => $class);
    }
}

1;
__END__

=head1 NAME

Service::getCHEBIEntryFromKEGGCompound - a SADI service

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

This service uses the TOGO web service interface to retrieve the CHEBI ID corresponding to a KEGG compound ID

=head1 CONTACT

B<Authority>: mark.ubic.ca

B<Email>: markw@organization.org

=cut
