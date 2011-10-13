package Bio::DB::SeqFeature::Store::Extended;

#----------------------------------------------------------------------
# imports
#----------------------------------------------------------------------

use strict;
use warnings;

use constant::boolean;
use Bio::DB::SeqFeature::Store;
use Bio::DB::SeqFeature::Store::Extended::TypeFilteringIterator;
use Utils::SO qw(is_a);
use SADI::GMOD::Config qw($SADI_GMOD_CONFIG);


#----------------------------------------------------------------------
# methods
#----------------------------------------------------------------------

sub new {

    my $class = shift;
    my %args = @_;
   
    my $self = {};

    $self->{gff_id_to_dbxref} = $args{-gff_id_to_dbxref};
    delete $args{-gff_id_to_dbxref};

    $self->{store} = Bio::DB::SeqFeature::Store->new(%args);

    bless ($self, $class);
    return $self;

}

# delegate undefined methods to wrapped Bio::DB::SeqFeature::Store object

our $AUTOLOAD;
sub AUTOLOAD {

    my $self = shift;
    
    my $method = $AUTOLOAD;
    $method =~ s/.*:://;

    # exception
    return if $method eq 'DESTROY';

    no strict 'refs';
    return $self->{store}->$method(@_);

}

sub features 
{
    #------------------------------------------------------------
    # process args
    #------------------------------------------------------------

    my $self = shift;
    my %args = $self->_map_dbxref_to_gff_id(@_);

    my $include_subtypes = defined($args{-include_subtypes}) ? $args{-include_subtypes} : TRUE;
    my @types = defined($args{-types}) ? @{$args{-types}} : ();

    delete($args{-include_subtypes});

    #------------------------------------------------------------
    # build a wrapped iterator that returns
    # Bio::DB::SeqFeature::Extended objects
    #------------------------------------------------------------
    
    my $wrapped_iterator; 
    my $iterator;

    if ($include_subtypes && @types) { 

        # Case 1: Subtype inferencing is required

        delete($args{-types});
        $wrapped_iterator = $self->{store}->get_seq_stream(%args); 
        $iterator = Bio::DB::SeqFeature::Store::Extended::TypeFilteringIterator->new($wrapped_iterator, $self, $self->{gff_id_to_dbxref}, @types);

    } else {

        # Case 2: Subtype inferencing is not required

        $wrapped_iterator = $self->{store}->get_seq_stream(%args); 
        $iterator = Bio::DB::SeqFeature::Store::Extended::Iterator->new($wrapped_iterator, $self, $self->{gff_id_to_dbxref});

    }

    #------------------------------------------------------------
    # return an feature iterator or feature array, as requested
    #------------------------------------------------------------
  
    if ($args{-iterator}) {
        return $iterator;
    }

    my @matching_features = ();

    while (my $feature = $iterator->next_seq) {
        push(@matching_features, $feature);
    }

    return @matching_features;
}

sub _map_dbxref_to_gff_id
{
    #------------------------------------------------------------
    # process args
    #------------------------------------------------------------

    my $self = shift;
    my %args = @_;    # user's original args to the feature method
    my $attributes = $args{-attributes};
    my $include_subtypes = defined($args{-include_subtypes}) ? $args{-include_subtypes} : TRUE;

    #------------------------------------------------------------
    # if user has not specified a GFF ID <=> Dbxref mapping, do nothing
    #------------------------------------------------------------

    return %args unless $self->{gff_id_to_dbxref};

    #------------------------------------------------------------
    # get Dbxref arg (if any)
    #------------------------------------------------------------

    my $dbxref;

    if ($attributes) {
        if (ref($attributes->{Dbxref})) {
            die 'this class does not support feature lookup by multiple Dbxrefs';
        }
        $dbxref = $attributes->{Dbxref};
    }

    # if there is no Dbxref arg, do nothing
    return %args unless $dbxref;

    my ($dbname, $id) = split(/:/, $dbxref, 2);

    #------------------------------------------------------------
    # simple case: 
    #
    # GFF ID <=> Dbxref mapping is the same Dbxref prefix 
    # for all feature types
    #------------------------------------------------------------

    if (!ref($self->{gff_id_to_dbxref})) {

        if ($dbname eq $self->{gff_id_to_dbxref}) {
            delete $attributes->{Dbxref};
            $attributes->{load_id} = $id;
        }

        return %args;
        
    }

    #------------------------------------------------------------
    # complex case: 
    #
    # GFF ID <=> Dbxref mapping depends on feature type
    #------------------------------------------------------------

    my %type_to_dbname = %{$self->{gff_id_to_dbxref}}; # if the prefix of the given Dbxref is not mapped to a GFF ID, # no need to do anything 
    return %args unless grep($_ eq $dbname, values %type_to_dbname);
    
    my @types = defined($args{-types}) ? @{$args{-types}} : ();

    # Tricky part.
    #
    # The user has requested features using a Dbxref that
    # is mapped to the GFF IDs of one or more feature types. 
    #
    # Restrict our query to those feature types whose GFF IDs 
    # are mapped to $dbname, and that also match one of the types 
    # specified in the original call to this method.

    my @matching_types = ();

    if (!@types) {

        # Case 1: The user didn't specify any feature types
        
        @matching_types = grep($type_to_dbname{$_} eq $dbname, keys %type_to_dbname);

    } else {

        # Case 2: The user specified some feature types, intersect them
        # with the types that are mapped to the Dbxref prefix of interest

        foreach my $mapped_type (keys %type_to_dbname) {
            for my $type (@types) {
                my $match = $include_subtypes ? is_a($mapped_type, $type) : ($type eq $mapped_type);
                if ($match) {
                    push (@matching_types, $mapped_type);
                    last;
                }
            }
        } 

    } 

    if (@matching_types) {

        delete $attributes->{Dbxref};
        $attributes->{load_id} = $id;
        $args{-types} = [ @matching_types ];
       
    } else {

        # Hack: 
        #
        # If @matching_types is empty, it means that there are
        # no features in the database matching the user's given args.  
        #
        # However, calling the features() method with zero type
        # args means "get features of all types".  To ensure that
        # we get no results, use a non-existent feature type.

        @matching_types = ('xxx_non_existent_feature_type_xxx') unless @matching_types;

    }

    # Subtype reasoning is not needed after this point, because the gff_id_to_dbxref
    # hash only specifies mappings for specific feature types, not entire subtrees of 
    # the sequence ontology.

    $args{-include_subtypes} = 0;

    return %args;
}

1;
