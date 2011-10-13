package SADI::GMOD::Config;

use strict;
use warnings;

#------------------------------------------------------------
# imports
#------------------------------------------------------------

use Config::IniFiles;
use Bio::Graphics::FeatureFile; # GBrowse config file parser
use File::Spec::Functions qw(catfile);
use constant::boolean;
use Text::ParseWords;
use Log::Log4perl;

#------------------------------------------------------------
# exports
#------------------------------------------------------------

require Exporter;

our @ISA = qw(Exporter);

our @EXPORT_OK = qw(
    $SADI_GMOD_CONFIG
    $DBXREF_TO_LSRN
    $LSRN_TO_ENTITY_TYPE
    get_db_args
    get_base_url
    get_gff_id_to_dbxref_mapping
    get_gmod_name
    get_primary_dbxref
);

#------------------------------------------------------------
# constants
#------------------------------------------------------------

use constant SADI_GMOD_CONFIG_FILE => 'sadi.gmod.conf';
use constant DBXREF_CONFIG_FILE => 'dbxref.conf';

use constant DBXREF_TO_LSRN_STANZA => 'DBXREF_TO_LSRN';
use constant LSRN_TO_ENTITY_TYPE_STANZA => 'LSRN_TO_ENTITY_TYPE';
use constant GFF_ID_TO_DBXREF_STANZA => 'GFF_ID_TO_DBXREF';

#------------------------------------------------------------
# package variables
#------------------------------------------------------------

# I use the GBrowse config file parser (FeatureFile) here so that GBrowse users
# don't have to learn anything new when configuring SADI for GMOD.  However, 
# I can't use FeatureFile for the DBXREF_CONFIG_FILE due to the following 
# limitations of FeatureFile:
#
# 1) key values in the config file are case-insensitive
# 2) key values cannot contain dashes ('-') and there is no way to escape them
#
# For DBXREF_CONFIG I use Parse::IniFiles, which is a similar format
# but not exactly the same.

our $SADI_GMOD_CONFIG = Bio::Graphics::FeatureFile->new(-file => SADI_GMOD_CONFIG_FILE);

#my %DBXREF_TO_LSRN = %{get_hashref_for_config_stanza(\%SADI_GMOD_CONFIG, DBXREF_TO_LSRN_STANZA)};
#my %LSRN_TO_ENTITY_TYPE = %{get_hashref_for_config_stanza(\%SADI_GMOD_CONFIG, LSRN_TO_ENTITY_TYPE_STANZA)};

my %DBXREF_CONFIG;
tie(%DBXREF_CONFIG, 'Config::IniFiles', -file => DBXREF_CONFIG_FILE);

our $GFF_ID_TO_DBXREF = $DBXREF_CONFIG{ &GFF_ID_TO_DBXREF_STANZA };
our $DBXREF_TO_LSRN = $DBXREF_CONFIG{ &DBXREF_TO_LSRN_STANZA };
our $LSRN_TO_ENTITY_TYPE = $DBXREF_CONFIG{ &LSRN_TO_ENTITY_TYPE_STANZA };

#------------------------------------------------------------
# logging
#------------------------------------------------------------

my $LOG = Log::Log4perl->get_logger(__PACKAGE__);

#------------------------------------------------------------
# subroutines
#------------------------------------------------------------

# Note: This method goes with Bio::Graphics::FeatureFile.
#
#sub get_hashref_for_config_stanza
#{
#    my $config = shift;
#    my $stanza = shift;
#
#    print "loading stanza $stanza into hashref\n";
#
#    my @keys = $config->setting($stanza);
#    
#    my %hash = ();
#
#    foreach my $key (@keys) {
#        print "key: $key\n";
#        $hash{$key} = $config->setting($stanza => $key);    
#    }
#
#    return \%hash; 
#}

sub get_db_args
{
    my $conf = $SADI_GMOD_CONFIG;

    my $ERROR_PREFIX = 'Error in ' . SADI_GMOD_CONFIG_FILE . ': ';

    if (!$conf->setting(general => 'db_adaptor') || !$conf->setting(general => 'db_args')) {
        die  $ERROR_PREFIX . 'Values missing for \'db_adaptor\' and/or \'db_args\' in section [GENERAL]. ' .
             'See comments in ' .  SADI_GMOD_CONFIG_FILE . " for details.\n";
    }   

    if ($conf->setting(general => 'db_adaptor') ne 'Bio::DB::SeqFeature::Store') {
        die $ERROR_PREFIX . "The only supported value for 'db_adaptor' is 'Bio::DB::SeqFeature::Store'.\n"
    }

    my @args = parse_line('\s+', FALSE, $conf->setting(general => 'db_args'));
    return @args;
}

sub get_base_url
{
    return $SADI_GMOD_CONFIG->setting(general => 'base_url');
}

sub get_gmod_name
{
    return $SADI_GMOD_CONFIG->setting(general => 'gmod_name');
}

sub get_primary_dbxref
{
    return $SADI_GMOD_CONFIG->setting(general => 'primary_dbxref');
}

sub get_gff_id_to_dbxref_mapping
{
    return undef unless $GFF_ID_TO_DBXREF;
    return $GFF_ID_TO_DBXREF->{ALL} if $GFF_ID_TO_DBXREF->{ALL};
    return $GFF_ID_TO_DBXREF;
}


1;
