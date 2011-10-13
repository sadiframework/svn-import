package Utils::BiopolymerRegion;

#------------------------------------------------------------
# imports
#------------------------------------------------------------

use strict;
use warnings;

use Vocab::Strand qw(
    PLUS_STRAND_TYPE_URI
    MINUS_STRAND_TYPE_URI
);
use Utils::Dbxref qw(id_type_to_dbxref);
use Utils::URI qw(
    parse_feature_uri
    parse_sequence_uri
);
use Utils::Trine qw(
    is_uri_resource
    build_query
    execute_query
);
use Log::Log4perl;
use File::Spec::Functions;

#------------------------------------------------------------
# exports
#------------------------------------------------------------

require Exporter;

our @ISA = qw(Exporter);
our @EXPORT_OK = qw(parse_biopolymer_region);

#------------------------------------------------------------
# logging
#------------------------------------------------------------

my $LOG = Log::Log4perl->get_logger(__PACKAGE__);

#------------------------------------------------------------
# subroutines
#------------------------------------------------------------

sub parse_biopolymer_region
{
    my ($input_model, $root_uri, $feature_store) = @_;

    my ($query, @results);

    $query = build_query(
        catfile('resources', 'templates', 'sparql', 'parse.biopolymer.region.sparql.tt2'), 
        { biopolymer_region_uri => $root_uri }
    );

    @results = execute_query($query, $input_model);

    if (@results == 0) {
        $LOG->warn("unable to parse biopolymer region $root_uri, graph is malformed or is missing required data");
        return undef;
    }

    # The position of the BiopolymerRegion may be defined relative to multiple
    # reference features. Regardless of which reference feature is used, the real
    # physical position of the region should be the same, so we just use the 
    # first reference feature that we can find in database.

    my $result;

    my $ref_feature;
    my $start_pos;
    my $end_pos;
    my $strand;

    foreach $result (@results) {

        # check that the bindings are of the expected type (resource/literal)

        next unless $result->{start_pos}->is_literal;
        next unless $result->{end_pos}->is_literal;
        next unless is_uri_resource($result->{ref_sequence}); 

        $start_pos = $result->{ start_pos }->value;
        $end_pos = $result->{ end_pos }->value;

        my ($gff_id, $strand) = parse_sequence_uri($result->{ref_sequence}->uri);
        next unless $gff_id;

#        # convert strand type to number
#
#        if ($result->{strand_type}->is_nil) {
#            $strand = 0;
#        } elsif ($result->{strand_type}->uri eq PLUS_STRAND_TYPE_URI) {
#            $strand = 1;
#        } elsif ($result->{strand_type}->uri eq MINUS_STRAND_TYPE_URI) {
#            $strand = -1;
#        } else {
#            next;
#        }
#
        # try to find the reference feature in the database
#
#        my $gff_id = get_gff_id_from_feature_uri($result->{ref_feature}->uri);
#        next unless $gff_id;

        my @features = $feature_store->features(-attributes => { load_id => $gff_id });
        
        if (@features > 1) {
            $LOG->warn("found multiple features for GFF ID '$gff_id', returning only first match");
        } 

        return ($features[0], $start_pos, $end_pos, $strand);
    }

    return undef;
}

1;
