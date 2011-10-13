package Utils::Trine;

#------------------------------------------------------------
# imports
#------------------------------------------------------------

use Template;
use RDF::Query; 

#------------------------------------------------------------
# exports
#------------------------------------------------------------

require Exporter;

our @ISA = qw(Exporter);
our @EXPORT_OK = qw(
    is_uri_resource
    build_query
    execute_query
);

#------------------------------------------------------------
# subroutines
#------------------------------------------------------------

sub is_uri_resource
{
    my $node = shift;
    return $node->is_resource && !$node->is_blank;
}

sub build_query
{
    my ($query_template_file, $var_hashref) = @_;

    my $templater = Template->new;

    my $query_string;
    $templater->process($query_template_file, $var_hashref, \$query_string)
        or die $templater->error(); 
        
    return $query_string;
}

sub execute_query
{
    my ($query_string, $trine_model) = @_;

    my $query = RDF::Query->new($query_string, { lang => 'sparql11' });
    return $query->execute($trine_model);
}

1;
