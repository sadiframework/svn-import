package Bio::SeqFeature::Extended;

use strict;
use warnings;

#------------------------------------------------------------
# imports
#------------------------------------------------------------

use Utils::Dbxref qw(
    dbxref_to_entity_type
    dbxref_to_uri
    dbxref_to_id_type
    dbxref_to_record_type
);
use Utils::SO qw(
    is_a
    get_uri_for_term
);
use File::Spec::Functions qw(catfile);
use Utils::Directories qw(TEMPLATES_DIR);
use Vocab::Strand qw(
    PLUS_STRAND_TYPE_URI
    MINUS_STRAND_TYPE_URI
);
use SADI::GMOD::Config qw(get_base_url);
use URI::Escape;
use Utils::URI qw(bracket_if_uri);
use Template;

#------------------------------------------------------------
# constants
#------------------------------------------------------------

use constant FEATURE_TTL_TEMPLATE_FILE => catfile(TEMPLATES_DIR, 'ttl', 'feature.ttl.tt2');
use constant SEQUENCE_TTL_TEMPLATE_FILE => catfile(TEMPLATES_DIR, 'ttl', 'sequence.ttl.tt2');
use constant STRAND_TTL_TEMPLATE_FILE => catfile(TEMPLATES_DIR, 'ttl', 'strand.ttl.tt2');

#------------------------------------------------------------
# package variables
#------------------------------------------------------------

my $blank_node_counter = 0;

#------------------------------------------------------------
# logging
#------------------------------------------------------------

my $LOG = Log::Log4perl->get_logger(__PACKAGE__);

#------------------------------------------------------------
# methods
#------------------------------------------------------------

sub new 
{
    my $class = shift;
    my $self = {};

    $self->{seq_feature} = shift;         # the wrapped SeqFeatureI object that does all of the real work 
    $self->{feature_store} = shift;       # the Bio::DB::SeqFeature::Store that the feature comes from

    if (!$self->{seq_feature}->isa('Bio::SeqFeatureI')) {
        die "a Bio::SeqFeature::Extended can only be created from an instance of Bio::SeqFeatureI";
    }

    $self->{gff_id_to_dbxref} = shift;    # optional GFF ID => Dbxref mapping

    bless($self, $class);
    return $self;
}

# delegate undefined methods to wrapped Bio::SeqFeatureI object

our $AUTOLOAD;
sub AUTOLOAD {

    my $self = shift;
    
    my $method = $AUTOLOAD;
    $method =~ s/.*:://;

    # exception
    return if $method eq 'DESTROY';

    no strict 'refs';
    return $self->{seq_feature}->$method(@_);

}

sub primary_dbxrefs
{
    my $self = shift;
    return $self->dbxrefs($self->primary_tag);
}

sub dbxrefs
{
    my $self = shift;
    my @entity_types = @_;

    my @dbxrefs = $self->get_tag_values('Dbxref');

    #------------------------------------------------------------
    # if entity types were specified (e.g. 'protein'), filter
    # out unwanted cross-refs
    #------------------------------------------------------------
    
    if (@entity_types) {
        my @filtered_dbxrefs = ();
        foreach my $dbxref (@dbxrefs) {
            foreach my $entity_type (@entity_types) {
                my $dbxref_type = dbxref_to_entity_type($dbxref);
                if ($dbxref_type && is_a($dbxref_type, $entity_type)) {
                    push(@filtered_dbxrefs, $dbxref);
                    last;
                } 
            }
        }
        @dbxrefs = @filtered_dbxrefs;
    }
   
    #------------------------------------------------------------
    # map the GFF ID to a Dbxref, if configured to do so.
    #------------------------------------------------------------

    my $gff_id_to_dbxref = $self->{gff_id_to_dbxref};
    my $dbxref_from_gff_id;

    if ($gff_id_to_dbxref) {

        my ($id) = $self->get_tag_values('load_id');

        if (ref($gff_id_to_dbxref) eq 'HASH') {
            foreach my $mapped_type (keys %$gff_id_to_dbxref) {
                if ($self->primary_tag eq $mapped_type) {
                    my $dbname = $gff_id_to_dbxref->{$mapped_type};
                    $dbxref_from_gff_id = "$dbname:$id";
                    last;
                }
            }
        } else {
            $dbxref_from_gff_id = "$gff_id_to_dbxref:$id";    
        }       

    }

    # If we generated a dbxref from the GFF ID, we 
    # know that that dbxref identifies the feature itself and not some
    # related entity (in other words, it is a primary dbxref)

    if ($dbxref_from_gff_id) {
        if (@entity_types) {
            if (grep(is_a($self->primary_tag, $_), @entity_types)) {
               push(@dbxrefs, $dbxref_from_gff_id); 
            }
        } else {
            push(@dbxrefs, $dbxref_from_gff_id);
        }
    }
    
    return @dbxrefs;
}

sub uri
{
    my $self = shift;

    my ($gff_id) = $self->get_tag_values('load_id');
    
    return undef unless $gff_id;
    return sprintf('%sfeature?id=%s', get_base_url, uri_escape($gff_id));
    
#    foreach my $dbxref ($self->primary_dbxrefs) {
#        return dbxref_to_uri($dbxref) if dbxref_to_uri($dbxref);
#    }
#
#    return undef;
}

sub ref_feature
{
    my $self = shift;
    
    my $ref_feature_id = $self->seq_id;
    my @ref_features = $self->{feature_store}->features(-attributes => { load_id => $ref_feature_id }); 
   
    if (@ref_features > 1) {
        $LOG->warn("multiple features matching ID '$ref_feature_id'! Using first match only");
    } 

    return $ref_features[0];
}

sub is_double_stranded 
{
    my $self = shift;

    # cache this value, as it is expensive to compute

    if (defined($self->{is_double_stranded})) {
        return $self->{is_double_stranded};
    }

    $self->{is_double_stranded} = 
            is_a($self->primary_tag, 'chromosome') || 
            is_a($self->primary_tag, 'chromosome_part');
 
    return $self->{is_double_stranded};
}

#sub ttl
#{
#    my $self = shift;
#    my $ttl;
#    $self->_append_ttl(\$ttl, @_);  
#    return $ttl; 
#}

#sub _append_ttl
sub ttl
{
    #------------------------------------------------------------
    # process args
    #------------------------------------------------------------

    my $self = shift;
#    my $output_var_ref = shift;
    my %options = @_;
    my $ttl;

    $options{link_feature_to_ref_feature} = 1 unless defined $options{link_feature_to_ref_feature};
#    $options{include_ref_feature} = 1 unless defined $options{include_ref_feature};

    #------------------------------------------------------------
    # determine URIs / blank node labels for entities
    #------------------------------------------------------------

    my $feature_uri = $options{feature_uri} || $self->uri || '_:feature'.$blank_node_counter++;
    bracket_if_uri(\$feature_uri);

    my $ref_feature = $self->ref_feature;

    my $ref_feature_uri = $options{ref_feature_uri} || $ref_feature->uri || '_:feature'.$blank_node_counter++;
    bracket_if_uri(\$ref_feature_uri);

    my $strand;
    if ($self->strand == 0 && $ref_feature->is_double_stranded) {
        $strand = 1;
    } else {
        $strand = $self->strand;
    }       

    my $ref_sequence_uri = 
            $options{ref_sequence_uri} ||
#            get_sequence_uri($ref_feature, $self->strand) ||
            $ref_feature->sequence_uri($strand) ||
            '_:sequence'.$blank_node_counter++;

    bracket_if_uri(\$ref_sequence_uri);

    #------------------------------------------------------------
    # build parameters for TTL template
    #------------------------------------------------------------

    my $feature_type_uri = sprintf('<%s>', get_uri_for_term($self->primary_tag));
    
    my $primary_dbxrefs = [];
   
    foreach my $dbxref ($self->primary_dbxrefs) {
        my ($dbname, $id) = split(/:/, $dbxref, 2);
        my $record_uri = dbxref_to_uri($dbxref);
        my $record_type_uri = dbxref_to_record_type($dbxref);
        my $id_type_uri = dbxref_to_id_type($dbxref);
        push(@$primary_dbxrefs, [ 
                    "<$record_uri>",
                    "<$record_type_uri>",
                    "<$id_type_uri>",
                    $id 
            ]) if $record_uri;
    } 
    
    #------------------------------------------------------------
    # generate TTL from template
    #------------------------------------------------------------

    my $templater = Template->new;
    
    $templater->process(

        FEATURE_TTL_TEMPLATE_FILE,

        {
            feature_uri => $feature_uri,
            feature_type_uri => $feature_type_uri,
            primary_dbxrefs => $primary_dbxrefs,
            ref_sequence_uri => $ref_sequence_uri,
            start_pos => $self->start,
            end_pos => $self->end,
        },

#        $output_var_ref,
        \$ttl,

    ) or die $templater->error;

    #------------------------------------------------------------
    # statements linking feature => ref sequence => ref strand => ref feature
    # (optional)
    #------------------------------------------------------------
   
    if ($options{link_feature_to_ref_feature}) {

#        my $strand_type_uri = ($self->strand == -1) ? MINUS_STRAND_TYPE_URI : PLUS_STRAND_TYPE_URI;
#        bracket_if_uri(\$strand_type_uri);
#
#        if (!$ref_feature->is_double_stranded && $self->strand == -1) {
#            $LOG->error('failed to build TTL for feature, feature is located on negative strand of a single stranded feature!');
#            return undef;
#        }
#        
#        $templater->process(
#
#            REF_SEQUENCE_TTL_TEMPLATE_FILE,
#
#            {
#                ref_sequence_uri => $ref_sequence_uri,
#                ref_feature_uri => $ref_feature_uri,
#                ref_feature_is_double_stranded => $ref_feature->is_double_stranded,
#                strand_type_uri => $strand_type_uri, 
#            },
#
#            $output_var_ref
#
#        ) or die $templater->error;

#        my $ttl = $ref_feature->ttl_for_sequence(


#        $$output_var_ref .= $ttl;

        $ttl .= $ref_feature->ttl_for_sequence(
                $strand,
                feature_is_double_stranded => $ref_feature->is_double_stranded,
                feature_uri => $ref_feature_uri,
                sequence_uri => $ref_sequence_uri,
            );

        if ($ref_feature->is_double_stranded) {
            $ttl .= $ref_feature->ttl_for_strand($strand, feature_uri => $ref_feature_uri); 
        }

    } 

#    if ($options{include_ref_feature}) {
#
#        $ref_feature->_append_ttl($output_var_ref, 
#                feature_uri => $ref_feature_uri,
#                link_feature_to_ref_feature => 0,
#                include_ref_feature => 0,
#            );
#
#    }
}

sub ttl_for_sequence
{
    my $self = shift;
    my $strand = shift;
    my %options = @_;

    my $feature_uri = $options{feature_uri} || $self->uri || '_:feature'.$blank_node_counter++;
    bracket_if_uri(\$feature_uri);

#    my $strand_uri = $options{strand_uri} || get_strand_uri($self, $strand) || '_:strand'.$blank_node_counter++;
    my $strand_uri = $options{strand_uri} || $self->strand_uri($strand) || '_:strand'.$blank_node_counter++;
    bracket_if_uri(\$strand_uri);

#    my $sequence_uri  = $options{sequence_uri} || get_sequence_uri($self, $strand) || '_:sequence'.$blank_node_counter++;
    my $sequence_uri  = $options{sequence_uri} || $self->sequence_uri($strand) || '_:sequence'.$blank_node_counter++;
    bracket_if_uri(\$sequence_uri);

    my $ttl;
    my $templater = Template->new;

    $templater->process(

        SEQUENCE_TTL_TEMPLATE_FILE,

        {
            feature_is_double_stranded => $self->is_double_stranded,
            sequence_uri => $sequence_uri,
            strand_uri => $strand_uri,
            feature_uri => $feature_uri,
        },

        \$ttl

    ) or die $templater->error;
    
    return $ttl;
}

sub ttl_for_strand
{
    my $self = shift;
    my $strand = shift;
    my %options = @_;

    return undef unless $self->is_double_stranded;

#    my $strand_uri = $options{strand_uri} || get_strand_uri($self, $strand) || '_:strand'.$blank_node_counter++;
    my $strand_uri = $options{strand_uri} || $self->strand_uri($strand) || '_:strand'.$blank_node_counter++;
    bracket_if_uri(\$strand_uri);

    my $feature_uri = $options{feature_uri} || $self->uri || '_:feature'.$blank_node_counter++;
    bracket_if_uri(\$feature_uri);
     
    my $strand_type_uri;

    if($strand == -1) {
        $strand_type_uri = MINUS_STRAND_TYPE_URI;
    } else {
        $strand_type_uri = PLUS_STRAND_TYPE_URI;
    }

    bracket_if_uri(\$strand_type_uri);

    my $ttl;
    my $templater = Template->new;

    $templater->process(

        STRAND_TTL_TEMPLATE_FILE,

        {
            strand_uri => $strand_uri,
            strand_type_uri => $strand_type_uri, 
            feature_uri => $feature_uri,
        },

        \$ttl

    ) or die $templater->error;
    
    return $ttl;
}

sub strand_uri
{
    my $self = shift;
    my $strand = shift;

    my ($gff_id) = $self->get_tag_values('load_id');

    return undef unless $gff_id;
    return undef unless $strand;

    return sprintf('%sstrand?id=%s&strand=%s',
            get_base_url,
            $gff_id,
            $strand,
        );
}

sub sequence_uri 
{
    my $self = shift;
    my $strand = shift;

    my ($gff_id) = $self->get_tag_values('load_id');

    return undef unless $gff_id;
    
    return sprintf('%ssequence?id=%s%s',
            get_base_url,
            $gff_id,
            $strand ? "&strand=$strand" : ''
    );
}    
      
    

1;
