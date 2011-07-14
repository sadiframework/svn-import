package Implementations;
use strict;

use LWP::Simple qw(!head);
use XML::LibXML;
use URI::Escape;
use Ace;
use DBI;
use SOAP::Lite;
use Data::Dumper;
use Fcntl ':flock';
use SADI::Utils;

# SIO imports
use semanticscience::org::resource::SIO_000253;    # has reference (op)
use semanticscience::org::resource::SIO_000272;    # is variant of (op)
use semanticscience::org::resource::SIO_000136;    # description (class)
use semanticscience::org::resource::SIO_000122;    # synonym (class)
use semanticscience::org::resource::SIO_000120;    # scientific name (class)
use semanticscience::org::resource::SIO_000118;    # common name (class)
use semanticscience::org::resource::SIO_000369;    # has component part (op)
use semanticscience::org::resource::SIO_010078;    # encodes (op)
use semanticscience::org::resource::SIO_000131;    # motif (class)
use semanticscience::org::resource::SIO_000135;    # definition
use semanticscience::org::resource::SIO_000117;    # preferred name
use semanticscience::org::resource::SIO_000116;    # name


# generated classes imports
use dev::biordf::net::kawas::owl::gene2pubmed::AnnotatedGeneID_Record;
use
  dev::biordf::net::kawas::owl::getdbSNPRecordByUniprotID::getdbSNPRecordByUniprotID_Output;
use dev::biordf::net::kawas::owl::DragonServices::getDragonAlleleLocus_Output;
use dev::biordf::net::kawas::owl::DragonServices::getDragonLocusAlleles_Output;
use dev::biordf::net::kawas::owl::togows_updated::getEcEncodedByKeggGenes_Output;
use dev::biordf::net::kawas::owl::togows_updated::getEcGeneComponentParts_Output;
use dev::biordf::net::kawas::owl::togows_updated::getEcGeneComponentPartsHuman_Output;
use dev::biordf::net::kawas::owl::togows_updated::getEcGeneComponentPartsMouse_Output;
use dev::biordf::net::kawas::owl::togows_updated::getEcGeneComponentPartsRat_Output;
use dev::biordf::net::kawas::owl::togows_updated::getEnzymeGeneComponentParts_Output;
use dev::biordf::net::kawas::owl::togows_updated::getEnzymeGeneComponentPartsRat_Output;
use dev::biordf::net::kawas::owl::togows_updated::getEnzymeGeneComponentPartsHuman_Output;
use dev::biordf::net::kawas::owl::togows_updated::getEnzymeGeneComponentPartsMouse_Output;
use dev::biordf::net::kawas::owl::togows_updated::getEnzymesEncodedByKeggGenes_Output;
use dev::biordf::net::kawas::owl::getGeneInformation::getGeneInformation_OutputClass;
use dev::biordf::net::kawas::owl::togows_updated::getKeggPathwaysByEC_Output;
use dev::biordf::net::kawas::owl::togows_updated::getKeggPathwaysByEnzyme_Output;
use dev::biordf::net::kawas::owl::togows_updated::getKeggPathwaysByKeggDrug_Output;
use dev::biordf::net::kawas::owl::getPubmedIDsByKeggPathway::PMID_Records;
use dev::biordf::net::kawas::owl::getUniprotByKeggGene::UniprotByKeggGeneOutputClass;
use dev::biordf::net::kawas::owl::kegg2pfam::kegg2pfam_OutputClass;
use dev::biordf::net::kawas::owl::kegg2prosite::kegg2prosite_OutputClass;
use sadiframework::org::ontologies::chebiservice::getCHEBIEntryFromKEGGCompound_Output;
use dev::biordf::net::kawas::owl::snp2gene::dbSNP2Gene_OutputClass;
use dev::biordf::net::kawas::owl::DragonServices::getDragonAlleleDescription_Output;
use dev::biordf::net::kawas::owl::getGOTermDefinitions::getGOTermDefinitions_Output;
use dev::biordf::net::kawas::owl::getGOTermAssociations::getGOTermAssociations_Output;
use dev::biordf::net::kawas::owl::getGOTermByGeneID::getGOTermByGeneID_Output;

# LSRN imports
use purl::oclc::org::SADI::LSRN::dbSNP_Record;
use purl::oclc::org::SADI::LSRN::PMID_Record;
use purl::oclc::org::SADI::LSRN::DragonDB_Locus_Record;
use purl::oclc::org::SADI::LSRN::DragonDB_Allele_Record;
use purl::oclc::org::SADI::LSRN::KEGG_ec_Record;
use purl::oclc::org::SADI::LSRN::KEGG_Record;
use purl::oclc::org::SADI::LSRN::ENZYME_Record;
use purl::oclc::org::SADI::LSRN::KEGG_PATHWAY_Record;
use purl::oclc::org::SADI::LSRN::UniProt_Record;
use purl::oclc::org::SADI::LSRN::Pfam_Record;
use purl::oclc::org::SADI::LSRN::Prosite_Record;
use purl::oclc::org::SADI::LSRN::ChEBI_Record;
use purl::oclc::org::SADI::LSRN::GeneID_Record;
use purl::oclc::org::SADI::LSRN::GO_Record;
use purl::oclc::org::SADI::LSRN::taxon_Record;

use purl::oclc::org::SADI::LSRN::dbSNP_Identifier;
use purl::oclc::org::SADI::LSRN::PMID_Identifier;
use purl::oclc::org::SADI::LSRN::DragonDB_Locus_Identifier;
use purl::oclc::org::SADI::LSRN::DragonDB_Allele_Identifier;
use purl::oclc::org::SADI::LSRN::KEGG_ec_Identifier;
use purl::oclc::org::SADI::LSRN::KEGG_Identifier;
use purl::oclc::org::SADI::LSRN::ENZYME_Identifier;
use purl::oclc::org::SADI::LSRN::KEGG_PATHWAY_Identifier;
use purl::oclc::org::SADI::LSRN::UniProt_Identifier;
use purl::oclc::org::SADI::LSRN::Pfam_Identifier;
use purl::oclc::org::SADI::LSRN::Prosite_Identifier;
use purl::oclc::org::SADI::LSRN::ChEBI_Identifier;
use purl::oclc::org::SADI::LSRN::GeneID_Identifier;
use purl::oclc::org::SADI::LSRN::GO_Identifier;
use purl::oclc::org::SADI::LSRN::taxon_Identifier;


# global lock file for NCBI services
our $lock = '/home/kawas/Perl-SADI/generated/ncbi.lock';

# input : $input, $accession, $LOG
# output: an AnnotatedGeneID_Record, or undef if nothing found
sub entrezGene2pubmed {
    my $input     = shift;
    my $accession = shift;
    my $LOG       = shift;
    my $url =
"http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=gene&retmode=xml&id=$accession&tool=sadiframework&email="
      . uri_escape_utf8('edward.kawas@gmail.com');
    $LOG->info(
"http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=gene&retmode=xml&id=$accession&tool=sadiframework&email="
          . uri_escape_utf8('edward.kawas@gmail.com') );

    # now get the URL
    #my $lock = '/home/kawas/Perl-SADI/services/Service/ncbi.lock';
    open( my $fh, ">", "$lock" ) or $LOG->warn("Cannot open ncbi.lock - $!\n");
    flock( $fh, LOCK_EX ) or $LOG->warn("Cannot lock $lock - $!\n");
    my $xml = get($url);

    # here we execute only every 1 second ...
    sleep(1);
    flock( $fh, LOCK_UN ) or $LOG->warn("Cannot unlock $lock - $!\n");
    close $fh;
    return undef unless defined $xml;

    # xml was obtained, now use XPATH to extract gene information
    my $parser = XML::LibXML->new();
    my $doc    = $parser->parse_string($xml);
    my $xpath =
"//*[local-name() = 'Gene-commentary_type'][\@value='generif']/../Gene-commentary_refs/Pub/Pub_pmid/PubMedId";
    my $xpc = XML::LibXML::XPathContext->new();
    my $nodes = $xpc->findnodes( $xpath, $doc->documentElement );
    my %ids;

    # get unique ids ..
    foreach my $context ( $nodes->get_nodelist ) {
        my $pmid = $context->textContent();
        next unless defined $pmid;
        $ids{$pmid} = 1;
    }
    my $class =
      dev::biordf::net::kawas::owl::gene2pubmed::AnnotatedGeneID_Record->new(
                                                             $input->getURI() );
    foreach my $pmid ( keys %ids ) {
        my $pc = purl::oclc::org::SADI::LSRN::PMID_Record->new("http://lsrn.org/PMID:$pmid");
        $pc = SADI::Utils::LSRNize($pc, $pmid);
        #$pc->add_SIO_000008(purl::oclc::org::SADI::LSRN::PMID_Identifier->new(SIO_000300 => $pmid));
        $class->add_SIO_000253($pc);
    }
    return $class;
}

# input : $input, $accession, $LOG
# output: an getdbSNPRecordByUniprotID_Output, or undef if nothing found
sub getdbSNPRecordByUniprotID {
    my $input     = shift;
    my $accession = shift;
    my $LOG       = shift;
$LOG->warn(sprintf("accession(%s), input(%s)",$accession, $input->getURI));
    my $protein_sequence_url =
'http://modbase.compbio.ucsf.edu/LS-SNP-cgi/LS_SNP_query.pl?PropertySelect=Protein_sequence&Range=chr10%3A77300000-82600000&RequestType=QueryById&Validated=on&idtype=sprotID&idvalue='
      . "$accession";
    my $content = get($protein_sequence_url);
    my %ids;
    if ( defined $content ) {

        # get snp ids
        while ( $content =~ m|>(rs\d+)</a>|gi ) {
            $ids{$1} = 1;
        }
    } else {
        $LOG->warn("Didnt get any content form LS_SNP_query");
        return undef;
    }
    my $class =
      dev::biordf::net::kawas::owl::getdbSNPRecordByUniprotID::getdbSNPRecordByUniprotID_Output
      ->new( $input->getURI );
    $class->add_SIO_000272(
                            SADI::Utils::LSRNize(purl::oclc::org::SADI::LSRN::dbSNP_Record->new(
                                                     "http://lsrn.org/dbSNP:$_"), $_)
    ) foreach ( keys %ids );
    return $class;
}

# input : $input, $accession, $LOG
# output: an getDragonAlleleLocus_Output, or undef if nothing found
sub getDragonAlleleLocus {
    my $input = shift;
    my $id    = shift;
    my $LOG   = shift;
    my $q     = qq{find Locus Allele = "$id"};
    my $db = Ace->connect(
                           -host => 'antirrhinum.net',
                           -port => 23100,
                           -user => 'anonymous',
                           -pass => 'guestguest'
    );
    my $class =
      dev::biordf::net::kawas::owl::DragonServices::getDragonAlleleLocus_Output
      ->new( $input->getURI );
    return $class unless defined $db;
    my @res   = $db->find($q);
    my $locus = shift(@res);
    next unless $locus;
    
    $class->add_SIO_000272(
                        SADI::Utils::LSRNize(purl::oclc::org::SADI::LSRN::DragonDB_Locus_Record->new(
                                      'http://lsrn.org/DragonDB_Locus:' . $locus
                        ), $locus)
    );
    return $class;
}

# input : $input, $accession, $LOG
# output: an getDragonLocusAlleles_Output, or undef if nothing found
sub getDragonLocusAlleles {
    my $input = shift;
    my $id    = shift;
    my $LOG   = shift;
    my $q     = qq{select Locus->Allele from Locus in object("Locus","$id")};
    my $db = Ace->connect(
                           -host => 'antirrhinum.net',
                           -port => 23100,
                           -user => 'anonymous',
                           -pass => 'guestguest'
    );
    
    my $class =
      dev::biordf::net::kawas::owl::DragonServices::getDragonLocusAlleles_Output
      ->new( $input->getURI );
    return $class unless defined $db;
    my @res = $db->aql($q);
    foreach (@res) {
        my $Allele = shift @{$_};
        $class->add_SIO_000272(
                       SADI::Utils::LSRNize(purl::oclc::org::SADI::LSRN::DragonDB_Allele_Record->new(
                                  'http://lsrn.org/DragonDB_Allele:' . "$Allele"
                       ), $Allele)
        );
    }
    return $class;
}

# input : $input, $accession, $LOG
# output: an getEcEncodedByKeggGenes_Output, or undef if nothing found
sub getEcEncodedByKeggGenes {
    my $input     = shift;
    my $accession = shift;
    my $LOG       = shift;
    my $resp = get("http://togows.dbcls.jp/entry/genes/$accession/eclinks");
    return undef unless $resp;
    my $class =
      dev::biordf::net::kawas::owl::togows_updated::getEcEncodedByKeggGenes_Output
      ->new( $input->getURI );
    my @enzymes = split( /\s+/, $resp );
    $class->add_SIO_010078(
                            SADI::Utils::LSRNize(purl::oclc::org::SADI::LSRN::KEGG_ec_Record->new(
                                                        "http://lsrn.org/EC:$_"), $_)
    ) foreach (@enzymes);
    return $class;
}

# input : $input, $accession, $LOG
# output: an getEcGeneComponentParts_Output, or undef if nothing found
sub getEcGeneComponentParts {
    my $input     = shift;
    my $accession = shift;
    my $LOG       = shift;
    my $resp      = get("http://togows.dbcls.jp/entry/enzyme/$accession/genes");
    return undef unless $resp;
    my %acceptable_orgs = ( 'hsa:' => 1, 'mmu:' => 1, 'rno:' => 1, );
    my %processed;
    my @genes = split( /(?=\w{3}:)/, $resp );
    my $class =
      dev::biordf::net::kawas::owl::togows_updated::getEcGeneComponentParts_Output
      ->new( $input->getURI );

    foreach my $str (@genes) {

        # trim whitespace
        $str =~ s/^\s*//g;
        $str =~ s/\s*$//g;

        # extract the hsa: portion from the string
        my $organism = $1 if $str =~ m/^(\w{3}:)/;
        next unless $acceptable_orgs{ lc($organism) };
        $processed{$organism} = 1;

        # remove the hsa: portion from the string
        $str =~ s/^\w{3}:\s*//g;

 # items are now space separated and may contain a (\w*) that we dont care about
        my @ids = split( / /, $str );
        @ids = map { lc($organism) . ( $_ =~ m/^(\w+)\(/ ? $1 : $_ ) } @ids;

        # create the output
        $class->add_SIO_000369(
                                SADI::Utils::LSRNize(purl::oclc::org::SADI::LSRN::KEGG_Record->new(
                                                      "http://lsrn.org/KEGG:$_"), $_)
        ) foreach (@ids);

        # stop here ...
        last if keys(%acceptable_orgs) == keys(%processed);
    }
    return $class;
}

# input : $input, $accession, $LOG
# output: an getdbSNPRecordByUniprotID_Output, or undef if nothing found
sub getEnzymeGeneComponentParts {
    my ( $input, $accession, $LOG ) = @_;
    my $resp = get("http://togows.dbcls.jp/entry/enzyme/$accession/genes");
    return undef unless $resp;
    my %acceptable_orgs = ( 'hsa:' => 1, 'mmu:' => 1, 'rno:' => 1, );
    my %processed;
    my @genes = split( /(?=\w{3}:)/, $resp );
    my $class =
      dev::biordf::net::kawas::owl::togows_updated::getEnzymeGeneComponentParts_Output
      ->new( $input->getURI );
    foreach my $str (@genes) {

        # trim whitespace
        $str =~ s/^\s*//g;
        $str =~ s/\s*$//g;

        # extract the hsa: portion from the string
        my $organism = $1 if $str =~ m/^(\w{3}:)/;
        next unless $acceptable_orgs{ lc($organism) };
        $processed{$organism} = 1;

        # remove the hsa: portion from the string
        $str =~ s/^\w{3}:\s*//g;

 # items are now space separated and may contain a (\w*) that we dont care about
        my @ids = split( / /, $str );
        @ids = map { lc($organism) . ( $_ =~ m/^(\w+)\(/ ? $1 : $_ ) } @ids;

        # create the output
        $class->add_SIO_000369(
                                SADI::Utils::LSRNize(purl::oclc::org::SADI::LSRN::KEGG_Record->new(
                                                      "http://lsrn.org/KEGG:$_"), $_)
        ) foreach (@ids);

        # stop here ...
        last if keys(%acceptable_orgs) == keys(%processed);
    }
    return $class;
}

# input : $input, $accession, $LOG
# output: an getEnzymesEncodedByKeggGenes_Output, or undef if nothing found
sub getEnzymesEncodedByKeggGenes {
    my ( $input, $accession, $LOG ) = @_;
    my $resp = get("http://togows.dbcls.jp/entry/genes/$accession/eclinks");
    my $class =
      dev::biordf::net::kawas::owl::togows_updated::getEnzymesEncodedByKeggGenes_Output
      ->new( $input->getURI );
    my @enzymes = split( /\s+/, $resp );
    $class->add_SIO_010078(
                            SADI::Utils::LSRNize(purl::oclc::org::SADI::LSRN::ENZYME_Record->new(
                                                    "http://lsrn.org/ENZYME:$_"), $_)
    ) foreach (@enzymes);
    return $class;
}

# input : $input, $accession, $LOG
# output: an getGeneInformation_OutputClass, or undef if nothing found
sub getGeneInformation {
    my ( $input, $accession, $LOG ) = @_;
    my $url =
"http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=gene&retmode=xml&id=$accession&tool=sadiframework&email="
      . uri_escape_utf8('edward.kawas@gmail.com');
    $LOG->info($accession);

    # now get the URL
    #my $lock = '/home/kawas/Perl-SADI/services/Service/ncbi.lock';
    open( my $fh, ">", "$lock" ) or $LOG->warn("Cannot open ncbi.lock - $!\n");
    flock( $fh, LOCK_EX ) or $LOG->warn("Cannot lock $lock - $!\n");
    my $xml = get($url);

    # here we execute only every 1 second ...
    sleep(1);
    flock( $fh, LOCK_UN ) or $LOG->warn("Cannot unlock $lock - $!\n");
    close $fh;
    return undef unless defined $xml;

    # declare stuff i need to extract
    my ( $name, $desc, $summary, @synonyms );

    # xml was obtained, now use XPATH to extract PubMed ids
    my $parser = XML::LibXML->new();
    my $doc    = $parser->parse_string($xml);

    # extract the name
    my $xpath = "//*[local-name() = 'Gene-ref_locus']/text()";
    my $xpc   = XML::LibXML::XPathContext->new();
    $name = $xpc->findvalue( $xpath, $doc->documentElement );

    # extract the description
    $xpath = "//*[local-name() = 'Gene-ref_desc']/text()";
    $xpc   = XML::LibXML::XPathContext->new();
    $desc  = $xpc->findvalue( $xpath, $doc->documentElement );

    # extract the summary
    $xpath   = "//*[local-name() = 'Entrezgene_summary']/text()";
    $xpc     = XML::LibXML::XPathContext->new();
    $summary = $xpc->findvalue( $xpath, $doc->documentElement );

    # extract the synonyms
    $xpath = "//*[local-name() = 'Gene-ref_syn_E']";
    $xpc   = XML::LibXML::XPathContext->new();
    my $nodes = $xpc->findnodes( $xpath, $doc->documentElement );
    foreach my $context ( $nodes->get_nodelist ) {
        my $syn = $context->textContent();
        next unless defined $syn;
        push @synonyms, $syn;
    }
    my $class =
      dev::biordf::net::kawas::owl::getGeneInformation::getGeneInformation_OutputClass
      ->new("http://lsrn.org/GeneID:$accession");
    $class->add_hasScientificName(
        semanticscience::org::resource::SIO_000120->new( SIO_000300 => $name ) )
      if defined $name;
    $class->add_hasDescription(
                            semanticscience::org::resource::SIO_000136->new(
                                                          SIO_000300 => $summary
                            )
    ) if defined $summary;
    # add the preferred name
    $class->add_hasName(
        semanticscience::org::resource::SIO_000117->new( SIO_000300 => $desc ) )
      if defined $desc;
    $class->label($desc);
    $class->add_hasName( # other names
           semanticscience::org::resource::SIO_000116->new( SIO_000300 => $_ ) )
      foreach (@synonyms);
    return $class;
}

# input : $input, $accession, $LOG
# output: an UniprotByKeggGeneOutputClass, or undef if nothing found
sub getUniprotByKeggGene {
    my ( $input, $accession, $LOG ) = @_;
    my $serv = SOAP::Lite->service("http://soap.genome.jp/KEGG.wsdl");

    #$LOG->info("accession is: '$accession'");
    my $result =
      $serv->get_linkdb_between_databases( $accession, "up", 1, 100 );

    my $output =
      dev::biordf::net::kawas::owl::getUniprotByKeggGene::UniprotByKeggGeneOutputClass
      ->new( $input->getURI );
    foreach (@$result) {
        next unless $_->{type} eq "equivalent";
        my $id = $_->{entry_id2};
        $id = $1 if $id =~ m/^up:(.*)$/;
$LOG->warn(sprintf("kegg2uni hit(%s)", $id));
        my $m = SADI::Utils::LSRNize(purl::oclc::org::SADI::LSRN::UniProt_Record->new(
                                                 "http://lsrn.org/UniProt:$id"), $id);
        $output->add_SIO_010078($m);
    }
    return $output;
}

# input : $input, $accession, $LOG
# output: an dbSNP2Gene_OutputClass, or undef if nothing found
sub snp2gene {
    my ( $input, $accession, $LOG ) = @_;
    
    $accession =~ s/^rs//;
    
    my $url =
"http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=snp&retmode=xml&id=$accession&tool=sadiframework.org&email="
      . uri_escape_utf8('edward.kawas@gmail.com');

    # now get the URL
    #my $lock = '/home/kawas/Perl-SADI/services/Service/ncbi.lock';
    open( my $fh, ">", "$lock" ) or $LOG->warn("Cannot open ncbi.lock - $!\n");
    flock( $fh, LOCK_EX ) or $LOG->warn("Cannot lock $lock - $!\n");
    my $xml = get($url);

    # here we execute only every 1 second ...
    sleep(1);
    flock( $fh, LOCK_UN ) or $LOG->warn("Cannot unlock $lock - $!\n");
    close $fh;
    return undef unless defined $xml;

    #        $LOG->info($url);
    # xml was obtained, now use XPATH to extract gene information
    my $parser = XML::LibXML->new();
    my $doc    = $parser->parse_string($xml);
    my $xpath  = "//*[local-name() = 'FxnSet'][\@geneId]";
    my $xpc    = XML::LibXML::XPathContext->new();
    my $nodes  = $xpc->findnodes( $xpath, $doc->documentElement );
    my %ids;

    # get unique ids ..
    foreach my $context ( $nodes->get_nodelist ) {
        my $pmid = $context->getAttribute('geneId');
        next unless defined $pmid;
        $ids{$pmid} = 1;
    }

    #$LOG->info(Dumper(\%ids));
    my $class =
      dev::biordf::net::kawas::owl::snp2gene::dbSNP2Gene_OutputClass->new(
                                                             $input->getURI() );
    foreach my $geneid ( keys %ids ) {
        $class->add_SIO_000272(
                                SADI::Utils::LSRNize(purl::oclc::org::SADI::LSRN::GeneID_Record->new(
                                                 "http://lsrn.org/GeneID:$geneid"), $geneid)
        );
    }
    return $class;
}

# input : $input, $accession, $LOG
# output: an getCHEBIEntryFromKEGGCompound_Output, or undef if nothing found
sub getCHEBIEntryFromKEGGCompound {
    my ( $input, $accession, $LOG ) = @_;
    
    
    
    my $resp = get("http://togows.dbcls.jp/entry/compound/$accession/dblinks");
    $LOG->warn("unable to get chebi id for $accession") && return undef
      unless $resp =~ m/ChEBI:\s+(\S+)/;
    my $class =
      sadiframework::org::ontologies::chebiservice::getCHEBIEntryFromKEGGCompound_Output
      ->new( $input->getURI );
    $class->add_SIO_000253(
                            SADI::Utils::LSRNize(new purl::oclc::org::SADI::LSRN::ChEBI_Record(
                                                  "http://lsrn.org/ChEBI:$1"), $1) );
    return $class;
}

# input : $input, $accession, $LOG
# output: an getDragonAlleleDescription_Output, or undef if nothing found
sub getDragonAlleleDescription {
    my ( $input, $id, $LOG ) = @_;
    $LOG->info("$id is the DragonDB_Allele");
    my $q = qq{select Phenotype from Phenotype in object("Allele","$id")};
    my $db = Ace->connect(
                           -host => 'antirrhinum.net',
                           -port => 23100,
                           -user => 'anonymous',
                           -pass => 'guestguest'
    );
    my $class =
      dev::biordf::net::kawas::owl::DragonServices::getDragonAlleleDescription_Output
      ->new( $input->getURI );
    unless (defined $db) {
       $LOG->warn('Couldn\'t connect to antirrhinum.net!');
       return $class;   
    }

    return $class unless defined $db;
    my @res = $db->aql($q);
    my $description;
    
    foreach ( @{ shift @res } ) {
        $description .= join "\n", $_->Phenotype;
        if ($description) {
            # add $description to the graph
            $class->add_hasDescription(
                 semanticscience::org::resource::SIO_000136->new(SIO_000300 => $description)
            );
        }
    }
    return $class;
}

# input : $input, $accession, $LOG
# output: an getKeggPathwaysByEC_Output, or undef if nothing found
sub getKeggPathwaysByEC {
    my ( $input, $accession, $LOG ) = @_;

    #http://togows.dbcls.jp/entry/enzyme/1.1.1.1/pathways
    my $resp = get("http://togows.dbcls.jp/entry/enzyme/$accession/pathways");

# PATH: ec00010  Glycolysis / Gluconeogenesis   PATH: ec00071  Fatty acid metabolism
    $LOG->warn("unable to get kegg_pathway for enzyme id :$accession") && next
      unless $resp =~ m/PATH:\s+(\S+)/
          or $resp =~ m/^\S+\s+(\S+)/;
    my $class =
      dev::biordf::net::kawas::owl::togows_updated::getKeggPathwaysByEC_Output
      ->new( $input->getURI );
    my @pathways = split( /(?=PATH:)/, $resp );
    @pathways = split( /(?=\t)/, $resp )
      if scalar(@pathways) == 1 and $pathways[0] eq $resp;
    foreach my $str (@pathways) {

        # trim whitespace
        $str =~ s/^\s*//g;
        $str =~ s/\s*$//g;

        # strip the PATH: from the string
        $str =~ s/^PATH:\s*//g;

        #print $LOG->info("$str");
        my ( $path, $label ) = split( /  /, $str );
        next unless $path and $label;
        my $record = new purl::oclc::org::SADI::LSRN::KEGG_PATHWAY_Record(
                                          "http://lsrn.org/KEGG_PATHWAY:$path");
        $record = SADI::Utils::LSRNize($record, $path);

        # add the label
        $record->label($label);
        $class->add_SIO_000062($record);
    }
    return $class;
}

# input : $input, $accession, $LOG
# output: an getKeggPathwaysByEnzyme_Output, or undef if nothing found
sub getKeggPathwaysByEnzyme {
    my ( $input, $accession, $LOG ) = @_;

    #http://togows.dbcls.jp/entry/enzyme/1.1.1.1/pathways
    my $resp = get("http://togows.dbcls.jp/entry/enzyme/$accession/pathways");

# PATH: ec00010  Glycolysis / Gluconeogenesis   PATH: ec00071  Fatty acid metabolism
    $LOG->warn("unable to get kegg_pathway for enzyme id :$accession") && next
      unless $resp =~ m/PATH:\s+(\S+)/
          or $resp =~ m/^\S+\s+(\S+)/;
    my $class =
      dev::biordf::net::kawas::owl::togows_updated::getKeggPathwaysByEnzyme_Output
      ->new( $input->getURI );
    my @pathways = split( /(?=PATH:)/, $resp );
    @pathways = split( /(?=\t)/, $resp )
      if scalar(@pathways) == 1 and $pathways[0] eq $resp;
    foreach my $str (@pathways) {

        # trim whitespace
        $str =~ s/^\s*//g;
        $str =~ s/\s*$//g;

        # strip the PATH: from the string
        $str =~ s/^PATH:\s*//g;

        #print "$str\t";
        my ( $path, $label ) = split( /  /, $str );
        next unless $path and $label;
        my $record = SADI::Utils::LSRNize(new purl::oclc::org::SADI::LSRN::KEGG_PATHWAY_Record(
                                          "http://lsrn.org/KEGG_PATHWAY:$path"), $path);

        # add the label
        $record->label($label);
        $class->add_SIO_000062($record);
    }
    return $class;
}

# input : $input, $accession, $LOG
# output: an getKeggPathwaysByKeggDrug_Output, or undef if nothing found
sub getKeggPathwaysByKeggDrug {
    my ( $input, $accession, $LOG ) = @_;
    my $resp = get("http://togows.dbcls.jp/entry/drug/$accession/pathways");

    # check $resp
    $LOG->warn("unable to get kegg_pathway for kegg drug: $accession") && next
      unless ( $resp and $resp =~ m/PATH:\s+(\S+)/ )
      or $resp =~ m/^\S+\s+(\S+)/;
    my @genes = split( /(?=PATH:)/, $resp );
    @genes = split( /(?=\t)/, $resp )
      if scalar(@genes) == 1 and $genes[0] eq $resp;
    my $class =
      dev::biordf::net::kawas::owl::togows_updated::getKeggPathwaysByKeggDrug_Output
      ->new( $input->getURI );
    foreach my $str (@genes) {

        # trim whitespace
        $str =~ s/^\s*//g;
        $str =~ s/\s*$//g;

 # items are now space separated and may contain a (\w*) that we dont care about
        my ( $path, $name ) = split( /  /, $str );
        next unless $path and $name;
        my $record = SADI::Utils::LSRNize(new purl::oclc::org::SADI::LSRN::KEGG_PATHWAY_Record(
                                          "http://lsrn.org/KEGG_PATHWAY:$path"), $path);

        # add the label
        $record->label($name);
        $class->add_SIO_000062($record);
    }
    return $class;
}

# input : $input, $accession, $LOG
# output: an kegg2pfam_OutputClass, or undef if nothing found
sub kegg2pfam {
    my ( $input, $accession, $LOG ) = @_;
    my $output =
      dev::biordf::net::kawas::owl::kegg2pfam::kegg2pfam_OutputClass->new(
                                                               $input->getURI );
    my $serv = SOAP::Lite->service("http://soap.genome.jp/KEGG.wsdl");
    my $result = $serv->get_motifs_by_gene( $accession, 'pfam' );
    my %ids;
    foreach my $motif (@$result) {
        my $id = $motif->{'motif_id'};
        $id = $1 if $id =~ m/^pfam:(.*)$/;
        next if $ids{$id};
        my $label = $motif->{'definition'};
        my $m = SADI::Utils::LSRNize(purl::oclc::org::SADI::LSRN::Pfam_Record->new(
                                                    "http://lsrn.org/Pfam:$id"), $id);
        $m->label($label);
        $output->add_hasMotif(semanticscience::org::resource::SIO_000131->new("http://lsrn.org/Pfam:$id"));
        $ids{$id} = 1;
    }
    return $output;
}

# input : $input, $accession, $LOG
# output: an kegg2prosite_OutputClass, or undef if nothing found
sub kegg2prosite {
    my ( $input, $accession, $LOG ) = @_;
    my $serv = SOAP::Lite->service("http://soap.genome.jp/KEGG.wsdl");
    my $output =
      dev::biordf::net::kawas::owl::kegg2prosite::kegg2prosite_OutputClass->new(
                                                               $input->getURI );

    #my $result = $serv->get_motifs_by_gene($accession, 'pspf');
    my %ids;
    my $result = $serv->get_motifs_by_gene( $accession, 'all' );
    foreach my $motif (@$result) {
        my $id = $motif->{'motif_id'};
        next unless $id =~ m/^ps:(.*)$/;
        $id = $1 if $id =~ m/^ps:(.*)$/;
        next if $ids{$id};
        my $label = $motif->{'definition'};
        my $m = SADI::Utils::LSRNize(purl::oclc::org::SADI::LSRN::Prosite_Record->new(
                                                 "http://lsrn.org/Prosite:$id"), $id);
        $m->label($label);
        $output->add_hasMotif(semanticscience::org::resource::SIO_000131->new("http://lsrn.org/Prosite:$id"));
        $ids{$id} = 1;
    }
    return $output;
}

# input : $input, $accession, $LOG
# output: an PMID_Records, or undef if nothing found
sub getPubmedIDsByKeggPathway {
    my ( $input, $accession, $LOG ) = @_;

    # iterate over each input
    my $serv   = SOAP::Lite->service("http://soap.genome.jp/KEGG.wsdl");
    my $result = $serv->get_references_by_pathway("path:$accession");
    my $class =
      new dev::biordf::net::kawas::owl::getPubmedIDsByKeggPathway::PMID_Records(
                                                               $input->getURI );
    $class->add_SIO_000253(
                            SADI::Utils::LSRNize(purl::oclc::org::SADI::LSRN::PMID_Record->new(
                                                      "http://lsrn.org/PMID:$_"), $_)
    ) foreach @{$result};
    return $class;
}



sub getGOTermAssociations {
    my ( $input, $accession, $LOG ) = @_;

    # The determining factor for the result limit is the amount of
    # RAM used by the SADI service. (Based on my experiments, the 
    # Gene Ontology SQL service is very sturdy and is capable 
    # of handling queries with *millions* of results.) 
    # Local RAM consumption for a result set of size 40,000
    # is approximately 2GB. It is typical for GO terms
    # to be associated with more than 40,000 proteins, and
    # so service results are likely to be truncated in many
    # cases. -- Ben V.
    my $limit = 40000;

    # Connect to the database.
    my $dbh =
      DBI->connect( "DBI:mysql:database=go_latest;host=mysql.ebi.ac.uk:4085",
                    "go_select", "amigo", { 'RaiseError' => 1 } );
    my $class =
          dev::biordf::net::kawas::owl::getGOTermAssociations::getGOTermAssociations_Output->new( $input->getURI );

    # Now retrieve data from the table.

    my $queryTemplate = <<QUERY;
SELECT DISTINCT
 term.name,
 term.term_type,
 dbxref.xref_key
FROM term
 INNER JOIN graph_path ON (term.id=graph_path.term1_id)
 INNER JOIN association ON (graph_path.term2_id=association.term_id)
 INNER JOIN gene_product ON (association.gene_product_id=gene_product.id)
 INNER JOIN dbxref ON (gene_product.dbxref_id=dbxref.id)
WHERE
 (dbxref.xref_dbname = "UniProtKB" OR dbxref.xref_dbname = "UniProt")
 AND
 term.acc = ?
LIMIT ?
QUERY

    # Old query:
    #
    #    my $sth = $dbh->prepare(
    #		'SELECT distinct (db.xref_key), t.name, t.term_type FROM dbxref as db, term as t, term_definition as d WHERE t.acc =? and db.xref_dbname="uniprot" and t.id=d.term_id Limit ?;'
    #		    );

    my $sth = $dbh->prepare($queryTemplate);
    my @ids = ($accession);
    foreach my $id (@ids) {
        $sth->execute($id,$limit);
        while ( my $ref = $sth->fetchrow_hashref() ) {
            my $name = $ref->{'name'};
            my $protID = $ref->{'xref_key'};
            my $ontology = $ref->{'term_type'};
            # add property based on ontology
            if ($ontology eq 'biological_process') {
                # biological_process - has participant
                $class->add_SIO_000132(SADI::Utils::LSRNize(purl::oclc::org::SADI::LSRN::UniProt_Record->new(
                                                 "http://lsrn.org/UniProt:$protID"), $protID));
            } elsif ($ontology eq 'cellular_component') {
                # cellular_component - has location in
                $class->add_SIO_000145(SADI::Utils::LSRNize(purl::oclc::org::SADI::LSRN::UniProt_Record->new(
                                                 "http://lsrn.org/UniProt:$protID"), $protID));
            } elsif ($ontology eq 'molecular_function') {
                # molecular_function - has function
                $class->add_SIO_000226(SADI::Utils::LSRNize(purl::oclc::org::SADI::LSRN::UniProt_Record->new(
                                                 "http://lsrn.org/UniProt:$protID"), $protID));
            } else {
                # all others: is related to
                $class->add_SIO_000219(SADI::Utils::LSRNize(purl::oclc::org::SADI::LSRN::UniProt_Record->new(
                                                 "http://lsrn.org/UniProt:$protID"), $protID));
            }
        }
    }
    $sth->finish();
    # Disconnect from the database.
    $dbh->disconnect();
    return $class;
}

sub getGOTermAssociationsX {
    my ( $input, $accession, $LOG, $db ) = @_;
    
    my %services = (
             # 0:db, 1:out_class, 2:out_datatype, 3:uri, 4:is_valid(sub that checks validity of output identifier)
             pdb           => ['pdb', 'dev::biordf::net::kawas::owl::getGOTermAssociations::getGOTermAssociationsPDB_Output', 'purl::oclc::org::SADI::LSRN::PDB_Record', 'http://lsrn.org/PDB:%s', sub{return 1;} ],
             locusid       => ['locusid', 'dev::biordf::net::kawas::owl::getGOTermAssociations::getGOTermAssociationsLocusID_Output', 'purl::oclc::org::SADI::LSRN::LocusID_Record', 'http://lsrn.org/GeneID:%s', sub{return 1;} ],
             kegg_pathway  => ['kegg_pathway', 'dev::biordf::net::kawas::owl::getGOTermAssociations::getGOTermAssociationsKeggPathway_Output', 'purl::oclc::org::SADI::LSRN::KEGG_PATHWAY_Record', 'http://lsrn.org/KEGG_PATHWAY:%s', sub{return 1;}],
             genbank       => ['genbank', 'dev::biordf::net::kawas::owl::getGOTermAssociations::getGOTermAssociationsGenBank_Output', 'purl::oclc::org::SADI::LSRN::GenBank_Record', 'http://lsrn.org/GenBank:%s', sub{return 1;}],
             embl          => ['embl', 'dev::biordf::net::kawas::owl::getGOTermAssociations::getGOTermAssociationsEMBL_Output', 'purl::oclc::org::SADI::LSRN::EMBL_Record', 'http://lsrn.org/EMBL:%s', sub{return 1;}],
             chebi         => ['chebi', 'dev::biordf::net::kawas::owl::getGOTermAssociations::getGOTermAssociationsChEBI_Output', 'purl::oclc::org::SADI::LSRN::ChEBI_Record', 'http://lsrn.org/CHEBI:%s', sub{return 1;}],
             agi_locuscode => ['agi_locuscode', 'dev::biordf::net::kawas::owl::getGOTermAssociations::getGOTermAssociationsAGI_Output', 'purl::oclc::org::SADI::LSRN::AGI_LocusCode_Record', 'http://lsrn.org/AGI_LocusCode:%s', sub{return 1;}],
             kegg_compound => ['kegg', 'dev::biordf::net::kawas::owl::getGOTermAssociations::getGOTermAssociationsKeggCompound_Output', 'purl::oclc::org::SADI::LSRN::KEGG_COMPOUND_Record', 'http://lsrn.org/KEGG_COMPOUND:%s', sub{my $dt = shift; return 1 if $dt =~ m/^c/i; return 0;}],
             interpro      => ['interpro', 'dev::biordf::net::kawas::owl::getGOTermAssociations::getGOTermAssociationsInterPro_Output', 'purl::oclc::org::SADI::LSRN::InterPro_Record', 'http://lsrn.org/InterPro:%s', sub{return 1;}],
             flybase       => ['flybase', 'dev::biordf::net::kawas::owl::getGOTermAssociations::getGOTermAssociationsFlybase_Output', 'purl::oclc::org::SADI::LSRN::FLYBASE_Record', 'http://lsrn.org/FLYBASE:%s', sub{return 1;}],
             pmid          => ['pmid', 'dev::biordf::net::kawas::owl::getGOTermAssociations::getGOTermAssociationsPMID_Output', 'purl::oclc::org::SADI::LSRN::PMID_Record', 'http://lsrn.org/PMID:%s', sub{return 1;}],
             
             ec                => ['ec', 'dev::biordf::net::kawas::owl::getGOTermAssociations::getGOTermAssociationsEC_Output', 'purl::oclc::org::SADI::LSRN::EC_Record', 'http://lsrn.org/EC:%s', sub{return 1;}],
             brenda            => ['brenda', 'dev::biordf::net::kawas::owl::getGOTermAssociations::getGOTermAssociationsBRENDA_Output', 'purl::oclc::org::SADI::LSRN::BRENDA_Record', 'http://lsrn.org/BRENDA:%s', sub{return 1;}],
             ensembl           => ['ensembl', 'dev::biordf::net::kawas::owl::getGOTermAssociations::getGOTermAssociationsENSEMBL_Output', 'purl::oclc::org::SADI::LSRN::ENSEMBL_Record', 'http://lsrn.org/ENSEMBL:%s', sub{return 1;}],
             omim              => ['omim', 'dev::biordf::net::kawas::owl::getGOTermAssociations::getGOTermAssociationsOMIM_Output', 'purl::oclc::org::SADI::LSRN::OMIM_Record', 'http://lsrn.org/OMIM:%s', sub{return 1;}],
             pfam              => ['pfam', 'dev::biordf::net::kawas::owl::getGOTermAssociations::getGOTermAssociationsPFAM_Output', 'purl::oclc::org::SADI::LSRN::Pfam_Record', 'http://lsrn.org/Pfam:%s', sub{return 1;}],
             prints            => ['prints', 'dev::biordf::net::kawas::owl::getGOTermAssociations::getGOTermAssociationsPRINTS_Output', 'purl::oclc::org::SADI::LSRN::PRINTS_Record', 'http://lsrn.org/PRINTS:%s', sub{return 1;}],
             prosite           => ['prosite', 'dev::biordf::net::kawas::owl::getGOTermAssociations::getGOTermAssociationsPROSITE_Output', 'purl::oclc::org::SADI::LSRN::Prosite_Record', 'http://lsrn.org/Prosite:%s', sub{return 1;}],
             reactome          => ['reactome', 'dev::biordf::net::kawas::owl::getGOTermAssociations::getGOTermAssociationsREACTOME_Output', 'purl::oclc::org::SADI::LSRN::Reactome_Record', 'http://lsrn.org/Reactome:%s', sub{return 1;}],
             tigr_ath1         => ['tigr_ath1', 'dev::biordf::net::kawas::owl::getGOTermAssociations::getGOTermAssociationsTIGR_ATH1_Output', 'purl::oclc::org::SADI::LSRN::TIGR_Ath1_Record', 'http://lsrn.org/TIGR_Ath1:%s', sub{return 1;}],
             tigr_cmr          => ['tigr_cmr', 'dev::biordf::net::kawas::owl::getGOTermAssociations::getGOTermAssociationsTIGR_CMR_Output', 'purl::oclc::org::SADI::LSRN::TIGR_CMR_Record', 'http://lsrn.org/TIGR_CMR:%s', sub{return 1;}],
             tigr_egad         => ['tigr_egad', 'dev::biordf::net::kawas::owl::getGOTermAssociations::getGOTermAssociationsTIGR_EGAD_Output', 'purl::oclc::org::SADI::LSRN::TIGR_EGAD_Record', 'http://lsrn.org/TIGR_EGAD:%s', sub{return 1;}],
             tigr_genprop      => ['tigr_genprop', 'dev::biordf::net::kawas::owl::getGOTermAssociations::getGOTermAssociationsTIGR_GENPROP_Output', 'purl::oclc::org::SADI::LSRN::TIGR_GenProp_Record', 'http://lsrn.org/TIGR_GenProp:%s', sub{return 1;}],
             trembl            => ['trembl', 'dev::biordf::net::kawas::owl::getGOTermAssociations::getGOTermAssociationsTREMBL_Output', 'purl::oclc::org::SADI::LSRN::TrEMBL_Record', 'http://lsrn.org/TrEMBL:%s', sub{return 1;}],
             pubchem_substance => ['pubchem_substance', 'dev::biordf::net::kawas::owl::getGOTermAssociations::getGOTermAssociationsPubChem_Substance_Output', 'purl::oclc::org::SADI::LSRN::PubChem_Substance_Record', 'http://lsrn.org/PubChem_Substance:%s', sub{return 1;}],
             pubchem_compound  => ['pubchem_compound', 'dev::biordf::net::kawas::owl::getGOTermAssociations::getGOTermAssociationsPubChem_Compound_Output', 'purl::oclc::org::SADI::LSRN::PubChem_Compound_Record', 'http://lsrn.org/PubChem_Compound:%s', sub{return 1;}],
    );
    return undef unless exists $services{$db};
    # make sure that we can instantiate our datatype
    eval "require $services{$db}->[1]";
    $LOG->warn ($@) if $@;
    return undef if $@;
    eval "require $services{$db}->[2]";    
    $LOG->warn ($@) if $@;
    return undef if $@;
    
    # The determining factor for the result limit is the amount of
    # RAM used by the SADI service. (Based on my experiments, the 
    # Gene Ontology SQL service is very sturdy and is capable 
    # of handling queries with *millions* of results.) 
    # Local RAM consumption for a result set of size 40,000
    # is approximately 2GB. It is typical for GO terms
    # to be associated with more than 40,000 proteins, and
    # so service results are likely to be truncated in many
    # cases. -- Ben V.
    my $limit = 40000;

    # Connect to the database.
    my $dbh =
      DBI->connect( "DBI:mysql:database=go_latest;host=mysql.ebi.ac.uk:4085",
                    "go_select", "amigo", { 'RaiseError' => 1 } );
    my $class =
          $services{$db}->[1]->new( $input->getURI );

    # Now retrieve data from the table.

    my $queryTemplate = <<QUERY;
SELECT DISTINCT
 term.name,
 term.term_type,
 dbxref.xref_key
FROM term
 INNER JOIN graph_path ON (term.id=graph_path.term1_id)
 INNER JOIN association ON (graph_path.term2_id=association.term_id)
 INNER JOIN gene_product ON (association.gene_product_id=gene_product.id)
 INNER JOIN dbxref ON (gene_product.dbxref_id=dbxref.id)
WHERE
 dbxref.xref_dbname = ? 
 AND
 term.acc = ?
LIMIT ?
QUERY

    # Old query:
    #
    #     my $sth = $dbh->prepare(sprintf(
    #'SELECT distinct (db.xref_key), t.name, t.term_type FROM dbxref as db, term as t, term_definition as d WHERE t.acc =? and db.xref_dbname="%s" and t.id=d.term_id Limit ?;'
    #    , $services{$db}->[0]));

    my $sth = $dbh->prepare($queryTemplate);
    my $dbname = $services{$db}->[0];
    my @ids = ($accession);
    foreach my $id (@ids) {
        $sth->execute($dbname, $id, $limit);
        while ( my $ref = $sth->fetchrow_hashref() ) {
            my $name = $ref->{'name'};
            my $protID = $ref->{'xref_key'};
            my $ontology = $ref->{'term_type'};
            next unless &{$services{$db}->[4]}($protID);
            # add property based on ontology
            if ($ontology eq 'biological_process') {
                # biological_process - has participant
                $class->add_SIO_000132(SADI::Utils::LSRNize($services{$db}->[2]->new(
                                                 sprintf($services{$db}->[3], $protID)), $protID));
            } elsif ($ontology eq 'cellular_component') {
                # cellular_component - has location in
                $class->add_SIO_000145(SADI::Utils::LSRNize($services{$db}->[2]->new(
                                                 sprintf($services{$db}->[3], $protID)), $protID));
            } elsif ($ontology eq 'molecular_function') {
                # molecular_function - has function
                $class->add_SIO_000226(SADI::Utils::LSRNize($services{$db}->[2]->new(
                                                 sprintf($services{$db}->[3], $protID)), $protID));
            } else {
                # all others: is related to
                $class->add_SIO_000219(SADI::Utils::LSRNize($services{$db}->[2]->new(
                                                 sprintf($services{$db}->[3], $protID)), $protID));
            }
        }
    }
    $sth->finish();
    # Disconnect from the database.
    $dbh->disconnect();
    return $class;
}

sub getGOTermByGeneID {
    my ( $input, $accession, $LOG ) = @_;
    
    my $url ="http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=gene&retmode=xml&id=$accession&tool=sadiframework&email=" . uri_escape_utf8( 'edward.kawas+sadi@gmail.com');
    $LOG->info($accession);
    # now get the URL
    #my $lock = '/home/kawas/Perl-SADI/services/Service/ncbi.lock';
    open( my $fh, ">", "$lock" ) or $LOG->warn( "Cannot open ncbi.lock - $!\n");
    flock($fh, LOCK_EX) or $LOG->warn( "Cannot lock $lock - $!\n");
    my $xml = get($url);
    # here we execute only every 1 second ...
    sleep(1);
    flock($fh, LOCK_UN) or $LOG->warn( "Cannot unlock $lock - $!\n");
    close $fh;
    next unless defined $xml;
        
    # xml was obtained, now use XPATH to extract GO ids / names
    my $parser = XML::LibXML->new();
    my $doc    = $parser->parse_string($xml);
        
    # get nodes of interest
    my $xpath = "//Dbtag [Dbtag_db = \"GO\"]/../..";
    my $xpc = XML::LibXML::XPathContext->new();
    my $nodes = $xpc->findnodes( $xpath, $doc->documentElement );
    my $class = dev::biordf::net::kawas::owl::getGOTermByGeneID::getGOTermByGeneID_Output->new($input->getURI);
    foreach my $context ( $nodes->get_nodelist ) {
        my $id = $context->findvalue(".//Object-id_id/text()");
        my $name = $context->findvalue("./Other-source_anchor/text()");
        next unless $id and $name;
        # format go id
        $id = sprintf("GO:%07d", $id);
        my $GO = SADI::Utils::LSRNize(purl::oclc::org::SADI::LSRN::GO_Record->new("http://lsrn.org/$id"), $id);
        my $hash = _get_go_term_definitions("$id");
        next unless defined $hash->{$id};
        $GO->label($hash->{$id}{name});
        my $ontology = $hash->{$id}{ontology};
        if ($ontology eq 'biological_process') {
            # biological_process - is participant in
            $class->add_SIO_000062($GO);
        } elsif ($ontology eq 'cellular_component') {
            # cellular_component - is located in
            $class->add_SIO_000061($GO);
        } elsif ($ontology eq 'molecular_function') {
            # molecular_function - is function of
            $class->add_SIO_000226($GO);
        } else {
            # all others: is related to
            $class->add_SIO_000219($GO);
        }
     }
     return $class;
}

# input : $input, $accession, $LOG
# output: an getGOTermDefinitions_Output, or undef if nothing found
sub getGOTermDefinitions {
    my ( $input, $accession, $LOG ) = @_;
    
    my $hash = _get_go_term_definitions( $accession );
    my $class =
          dev::biordf::net::kawas::owl::getGOTermDefinitions::getGOTermDefinitions_Output
          ->new( $input->getURI );
    return $class unless defined $hash->{$accession};
    $class->label($hash->{$accession}{name});
    # common name
    $class->add_hasName(
        semanticscience::org::resource::SIO_000118->new( SIO_000300 => $hash->{$accession}{name} ) );
    # definition
    $class->add_hasDescription(
        semanticscience::org::resource::SIO_000136->new( SIO_000300 => $hash->{$accession}{definition} ) );
    # dont need ontology in this case
    return $class;
}

sub getEcGeneComponentPartsHuman {
    my ( $input, $accession, $LOG ) = @_;
    my $resp      = get("http://togows.dbcls.jp/entry/enzyme/$accession/genes");
    return undef unless $resp;
    my %acceptable_orgs = ( 'hsa:' => 1, );
    my %processed;
    my @genes = split( /(?=\w{3}:)/, $resp );
    my $class =
      dev::biordf::net::kawas::owl::togows_updated::getEcGeneComponentPartsHuman_Output
      ->new( $input->getURI );

    foreach my $str (@genes) {

        # trim whitespace
        $str =~ s/^\s*//g;
        $str =~ s/\s*$//g;

        # extract the hsa: portion from the string
        my $organism = $1 if $str =~ m/^(\w{3}:)/;
        next unless $acceptable_orgs{ lc($organism) };
        $processed{$organism} = 1;

        # remove the hsa: portion from the string
        $str =~ s/^\w{3}:\s*//g;

 # items are now space separated and may contain a (\w*) that we dont care about
        my @ids = split( / /, $str );
        @ids = map { lc($organism) . ( $_ =~ m/^(\w+)\(/ ? $1 : $_ ) } @ids;

        # create the output
        $class->add_SIO_000369(
                                SADI::Utils::LSRNize(purl::oclc::org::SADI::LSRN::KEGG_Record->new(
                                                      "http://lsrn.org/KEGG:$_"), $_)
        ) foreach (@ids);

        # stop here ...
        last if keys(%acceptable_orgs) == keys(%processed);
    }
    return $class;
}
sub getEcGeneComponentPartsMouse {
    my ( $input, $accession, $LOG ) = @_;
    my $resp      = get("http://togows.dbcls.jp/entry/enzyme/$accession/genes");
    return undef unless $resp;
    my %acceptable_orgs = ( 'mmu:' => 1, );
    my %processed;
    my @genes = split( /(?=\w{3}:)/, $resp );
    my $class =
      dev::biordf::net::kawas::owl::togows_updated::getEcGeneComponentPartsMouse_Output
      ->new( $input->getURI );

    foreach my $str (@genes) {

        # trim whitespace
        $str =~ s/^\s*//g;
        $str =~ s/\s*$//g;

        # extract the hsa: portion from the string
        my $organism = $1 if $str =~ m/^(\w{3}:)/;
        next unless $acceptable_orgs{ lc($organism) };
        $processed{$organism} = 1;

        # remove the hsa: portion from the string
        $str =~ s/^\w{3}:\s*//g;

 # items are now space separated and may contain a (\w*) that we dont care about
        my @ids = split( / /, $str );
        @ids = map { lc($organism) . ( $_ =~ m/^(\w+)\(/ ? $1 : $_ ) } @ids;

        # create the output
        $class->add_SIO_000369(
                                SADI::Utils::LSRNize(purl::oclc::org::SADI::LSRN::KEGG_Record->new(
                                                      "http://lsrn.org/KEGG:$_"), $_)
        ) foreach (@ids);

        # stop here ...
        last if keys(%acceptable_orgs) == keys(%processed);
    }
    return $class;
}
sub getEcGeneComponentPartsRat  {
    my ( $input, $accession, $LOG ) = @_;
    my $resp      = get("http://togows.dbcls.jp/entry/enzyme/$accession/genes");
    return undef unless $resp;
    my %acceptable_orgs = ( 'rno:' => 1, );
    my %processed;
    my @genes = split( /(?=\w{3}:)/, $resp );
    my $class =
      dev::biordf::net::kawas::owl::togows_updated::getEcGeneComponentPartsRat_Output
      ->new( $input->getURI );

    foreach my $str (@genes) {

        # trim whitespace
        $str =~ s/^\s*//g;
        $str =~ s/\s*$//g;

        # extract the hsa: portion from the string
        my $organism = $1 if $str =~ m/^(\w{3}:)/;
        next unless $acceptable_orgs{ lc($organism) };
        $processed{$organism} = 1;

        # remove the hsa: portion from the string
        $str =~ s/^\w{3}:\s*//g;

 # items are now space separated and may contain a (\w*) that we dont care about
        my @ids = split( / /, $str );
        @ids = map { lc($organism) . ( $_ =~ m/^(\w+)\(/ ? $1 : $_ ) } @ids;

        # create the output
        $class->add_SIO_000369(
                                SADI::Utils::LSRNize(purl::oclc::org::SADI::LSRN::KEGG_Record->new(
                                                      "http://lsrn.org/KEGG:$_"), $_)
        ) foreach (@ids);

        # stop here ...
        last if keys(%acceptable_orgs) == keys(%processed);
    }
    return $class;
}
sub getEnzymeGeneComponentPartsHuman {
    my ( $input, $accession, $LOG ) = @_;
    my $resp = get("http://togows.dbcls.jp/entry/enzyme/$accession/genes");
    return undef unless $resp;
    my %acceptable_orgs = ( 'hsa:' => 1 );
    my %processed;
    my @genes = split( /(?=\w{3}:)/, $resp );
    my $class =
      dev::biordf::net::kawas::owl::togows_updated::getEnzymeGeneComponentPartsHuman_Output
      ->new( $input->getURI );
    foreach my $str (@genes) {

        # trim whitespace
        $str =~ s/^\s*//g;
        $str =~ s/\s*$//g;

        # extract the hsa: portion from the string
        my $organism = $1 if $str =~ m/^(\w{3}:)/;
        next unless $acceptable_orgs{ lc($organism) };
        $processed{$organism} = 1;

        # remove the hsa: portion from the string
        $str =~ s/^\w{3}:\s*//g;

 # items are now space separated and may contain a (\w*) that we dont care about
        my @ids = split( / /, $str );
        @ids = map { lc($organism) . ( $_ =~ m/^(\w+)\(/ ? $1 : $_ ) } @ids;

        # create the output
        $class->add_SIO_000369(
                                SADI::Utils::LSRNize(purl::oclc::org::SADI::LSRN::KEGG_Record->new(
                                                      "http://lsrn.org/KEGG:$_"), $_)
        ) foreach (@ids);

        # stop here ...
        last if keys(%acceptable_orgs) == keys(%processed);
    }
    return $class;
}
sub getEnzymeGeneComponentPartsMouse {
    my ( $input, $accession, $LOG ) = @_;
    my $resp = get("http://togows.dbcls.jp/entry/enzyme/$accession/genes");
    return undef unless $resp;
    my %acceptable_orgs = ( 'mmu:' => 1 );
    my %processed;
    my @genes = split( /(?=\w{3}:)/, $resp );
    my $class =
      dev::biordf::net::kawas::owl::togows_updated::getEnzymeGeneComponentPartsMouse_Output
      ->new( $input->getURI );
    foreach my $str (@genes) {

        # trim whitespace
        $str =~ s/^\s*//g;
        $str =~ s/\s*$//g;

        # extract the hsa: portion from the string
        my $organism = $1 if $str =~ m/^(\w{3}:)/;
        next unless $acceptable_orgs{ lc($organism) };
        $processed{$organism} = 1;

        # remove the hsa: portion from the string
        $str =~ s/^\w{3}:\s*//g;

 # items are now space separated and may contain a (\w*) that we dont care about
        my @ids = split( / /, $str );
        @ids = map { lc($organism) . ( $_ =~ m/^(\w+)\(/ ? $1 : $_ ) } @ids;

        # create the output
        $class->add_SIO_000369(
                                SADI::Utils::LSRNize(purl::oclc::org::SADI::LSRN::KEGG_Record->new(
                                                      "http://lsrn.org/KEGG:$_"), $_)
        ) foreach (@ids);

        # stop here ...
        last if keys(%acceptable_orgs) == keys(%processed);
    }
    return $class;
}
sub getEnzymeGeneComponentPartsRat {
    my ( $input, $accession, $LOG ) = @_;
    my $resp = get("http://togows.dbcls.jp/entry/enzyme/$accession/genes");
    return undef unless $resp;
    my %acceptable_orgs = ( 'rno:' => 1, );
    my %processed;
    my @genes = split( /(?=\w{3}:)/, $resp );
    my $class =
      dev::biordf::net::kawas::owl::togows_updated::getEnzymeGeneComponentPartsRat_Output
      ->new( $input->getURI );
    foreach my $str (@genes) {

        # trim whitespace
        $str =~ s/^\s*//g;
        $str =~ s/\s*$//g;

        # extract the hsa: portion from the string
        my $organism = $1 if $str =~ m/^(\w{3}:)/;
        next unless $acceptable_orgs{ lc($organism) };
        $processed{$organism} = 1;

        # remove the hsa: portion from the string
        $str =~ s/^\w{3}:\s*//g;

 # items are now space separated and may contain a (\w*) that we dont care about
        my @ids = split( / /, $str );
        @ids = map { lc($organism) . ( $_ =~ m/^(\w+)\(/ ? $1 : $_ ) } @ids;

        # create the output
        $class->add_SIO_000369(
                                SADI::Utils::LSRNize(purl::oclc::org::SADI::LSRN::KEGG_Record->new(
                                                      "http://lsrn.org/KEGG:$_"), $_)
        ) foreach (@ids);

        # stop here ...
        last if keys(%acceptable_orgs) == keys(%processed);
    }
    return $class;
}

# pass in an array of go term identifiers
# returns a hash reference where the key to the hash are GO IDs
# and the value is another hash with name, ontology, definition
# ontology is either: 
sub _get_go_term_definitions {
    my @ids = @_;
    my %results;
    # Connect to the database.
    my $dbh =
      DBI->connect( "DBI:mysql:database=go_latest;host=mysql.ebi.ac.uk:4085",
                    "go_select", "amigo", { 'RaiseError' => 1 } );

    # Now retrieve data from the table.
    my $sth = $dbh->prepare(
"SELECT t.name, t.term_type, t.acc, d.term_definition FROM term as t, term_definition as d WHERE t.acc= ? and t.id=d.term_id"
    );
    foreach my $id (@ids) {
        $sth->execute(($id));
        while ( my $ref = $sth->fetchrow_hashref() ) {
            $results{$ref->{'acc'}} = {
                name => $ref->{'name'}, 
                ontology => $ref->{'term_type'},
                definition => $ref->{'term_definition'} 
            };
        }
    }
    $sth->finish();

    # Disconnect from the database.
    $dbh->disconnect();
    return \%results;
}

# the following SOAP serializer methods are for use with KEGG
sub SOAP::Serializer::as_ArrayOfstring {
    my ( $self, $value, $name, $type, $attr ) = @_;
    return [ $name, { 'xsi:type' => 'array', %$attr }, $value ];
}

sub SOAP::Serializer::as_ArrayOfint {
    my ( $self, $value, $name, $type, $attr ) = @_;
    return [ $name, { 'xsi:type' => 'array', %$attr }, $value ];
}

1;

