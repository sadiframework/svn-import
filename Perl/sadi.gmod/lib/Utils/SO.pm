package Utils::SO;

#------------------------------------------------------------
# imports
#------------------------------------------------------------

use strict;
use warnings;

use constant::boolean;

use Carp; # corrects missing dependency for OBO::Parser::OBOParser
use OBO::Parser::OBOParser;
use File::Spec::Functions qw(catfile);
use Log::Log4perl;
use Utils::Directories qw(
    ONTOLOGIES_DIR
);
use Vocab::SO qw(SO_ONTOLOGY_PREFIX);

#------------------------------------------------------------
# exports
#------------------------------------------------------------

require Exporter;
our @ISA = qw(Exporter);
our @EXPORT_OK = qw(
    is_a
    get_uri_for_term
);

#------------------------------------------------------------
# logging
#------------------------------------------------------------

my $LOG = Log::Log4perl->get_logger(__PACKAGE__);

#------------------------------------------------------------
# constants 
#------------------------------------------------------------

use constant OBO_FILE => catfile(ONTOLOGIES_DIR, 'so_2_4_3.obo');

#------------------------------------------------------------
# package vars
#------------------------------------------------------------

my $ontology = load_ontology();
 
#------------------------------------------------------------
# subroutines
#------------------------------------------------------------

sub load_ontology
{
    my $parser = OBO::Parser::OBOParser->new;
    return $parser->work(OBO_FILE);
}

sub is_a
{
    my ($term1_name, $term2_name) = @_;

    my $is_a = $ontology->get_relationship_type_by_id('is_a');
   
    my $term1 = $ontology->get_term_by_name_or_synonym($term1_name);
    my $term2 = $ontology->get_term_by_name_or_synonym($term2_name);
    
    $LOG->warn("there is no '$term1_name' term in the sequence ontology'") unless $term1;
    $LOG->warn("there is no '$term2_name' term in the sequence ontology'") unless $term2;

    return FALSE unless $term1 && $term2;

    my $ancestors = $ontology->get_ancestor_terms_by_relationship_type($term1, $is_a);
    
    # every term is a subclass of itself
    push(@$ancestors, $term1);

    my $match = 0;

    return scalar(grep($term2->equals($_), @$ancestors));
}

sub get_uri_for_term
{
    my $term_name = shift;

    my $term = $ontology->get_term_by_name_or_synonym($term_name);

    if (!$term) {
        $LOG->warn("there is no '$term_name' term in the sequence ontology'");
        return undef;
    }

    my $id = $term->id;
    $id =~ s/^SO:/SO_/;

    return SO_ONTOLOGY_PREFIX . $id;
}

1;

