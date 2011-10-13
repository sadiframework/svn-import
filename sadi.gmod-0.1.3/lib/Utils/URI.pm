package Utils::URI;

use strict;
use warnings;

use SADI::GMOD::Config qw(get_base_url);
use URI::QueryParam;

require Exporter;
our @ISA = qw(Exporter);
our @EXPORT_OK = qw(
    get_sequence_uri
    get_strand_uri
    bracket_if_uri
    parse_feature_uri
    parse_sequence_uri
);

sub get_sequence_uri 
{
    my ($feature, $strand) = @_;

    my ($gff_id) = $feature->get_tag_values('load_id');

    return undef unless $gff_id;
    
    return sprintf('%ssequence?id=%s%s',
            get_base_url,
            $gff_id,
            $strand ? "&strand=$strand" : ''
        );
}

sub get_strand_uri
{
    my ($feature, $strand) = @_;

    my ($gff_id) = $feature->get_tag_values('load_id');

    return undef unless $gff_id;
    return undef unless $strand;

    return sprintf('%sstrand?id=%s&strand=%s',
            get_base_url,
            $gff_id,
            $strand,
        );
}

sub bracket_if_uri
{
    my $uri_or_bnode_ref = shift;

    return undef unless $uri_or_bnode_ref;
       
    if($$uri_or_bnode_ref !~ /^_:/ && $$uri_or_bnode_ref !~ /^<.*>$/) {
        $$uri_or_bnode_ref = "<$$uri_or_bnode_ref>";
    } 
}

sub parse_feature_uri
{
    my $feature_uri = URI->new(shift);
    return undef unless $feature_uri->as_string =~ /^@{[get_base_url]}feature/;
    my $params = $feature_uri->query_form_hash;
    return $params->{id};
}

sub parse_sequence_uri
{
    my $sequence_uri = URI->new(shift);
    return undef unless $sequence_uri->as_string =~ /^@{[get_base_url]}sequence/;
    my $params = $sequence_uri->query_form_hash;
    return ($params->{id}, $params->{strand});
}
