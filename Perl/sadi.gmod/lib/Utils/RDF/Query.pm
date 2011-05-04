package Utils::RDF::Query;

#------------------------------------------------------------
# imports
#------------------------------------------------------------

use Template;
use RDF::Query; 

#------------------------------------------------------------
# exports
#------------------------------------------------------------

require Exporter;

use vars qw(@ISA @EXPORT_OK);
@ISA = qw(Exporter);
@EXPORT_OK = qw(build_query execute_query);

#------------------------------------------------------------
# global vars
#------------------------------------------------------------

my $template_config = {
    INCLUDE_PATH => '.',
    ABSOLUTE => 1,          # allow absolute paths to template files 
};

my $templater = Template->new($template_config);

#------------------------------------------------------------
# subroutines
#------------------------------------------------------------

sub build_query
{
    my ($query_template_file, $var_hashref) = @_;

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
