#!/usr/bin/perl -w
use SOAP::Lite;
use Data::Dumper;

my $serv = SOAP::Lite -> service("http://soap.genome.jp/KEGG.wsdl");

# uniprot by keyword
my $result = $serv->get_motifs_by_gene('hsa:50616', 'pspt');
foreach my $motif (@$result) {
            my $id = $motif->{'motif_id'};
            $id = $1 if $id =~ m/^ps:(.*)$/;
            my $label = $motif->{'definition'};
            print "$id -> $label\n";
}


$result = $serv->get_motifs_by_gene('hsa:50616', 'pspf');
foreach my $motif (@$result) {
            my $id = $motif->{'motif_id'};
            $id = $1 if $id =~ m/^ps:(.*)$/;
            my $label = $motif->{'definition'};
            print "$id -> $label\n";
}


# sub routines implicitly used in the above code
sub SOAP::Serializer::as_ArrayOfstring {
    my ( $self, $value, $name, $type, $attr ) = @_;
    return [ $name, { 'xsi:type' => 'array', %$attr }, $value ];
}

sub SOAP::Serializer::as_ArrayOfint {
    my ( $self, $value, $name, $type, $attr ) = @_;
    return [ $name, { 'xsi:type' => 'array', %$attr }, $value ];
}

