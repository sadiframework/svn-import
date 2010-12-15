#!/usr/bin/perl -w
use strict;

use SOAP::Lite;
use Data::Dumper;

sub SOAP::Serializer::as_ArrayOfstring{
  my ($self, $value, $name, $type, $attr) = @_;
  return [$name, {'xsi:type' => 'array', %$attr}, $value];
}

sub SOAP::Serializer::as_ArrayOfint{
  my ($self, $value, $name, $type, $attr) = @_;
  return [$name, {'xsi:type' => 'array', %$attr}, $value];
}

sub test {
my $serv = SOAP::Lite -> service("http://soap.genome.jp/KEGG.wsdl");

my $result = $serv->get_linkdb_between_databases('hsa:50616',"up", 1, 100);

$_->{type} eq "equivalent" && print $_->{entry_id2}, "\n" foreach @{$result};

}

