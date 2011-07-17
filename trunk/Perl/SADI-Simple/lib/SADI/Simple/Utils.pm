#-----------------------------------------------------------------
# SADI::Utils
# Author: Edward Kawas <edward.kawas@gmail.com>
# For copyright and disclaimer see LICENSE.
#
# $Id: Utils.pm,v 1.2 2009-10-02 15:53:46 ubuntu Exp $
#-----------------------------------------------------------------
#package SADI::Utils;
package SADI::Simple::Utils;

use strict;

use File::Spec;
use LWP::UserAgent;
use HTTP::Request;
use RDF::Trine::Model;
use RDF::Trine::Parser;
use RDF::Trine::Serializer;
use File::Spec::Functions;

require Exporter;
our @ISA = qw(Exporter);
our @EXPORT_OK = qw(init_log4perl);

# add versioning to this module
use vars qw /$VERSION/;
$VERSION = sprintf "%d.%02d", q$Revision: 1.4 $ =~ /: (\d+)\.(\d+)/;

# logging related

use constant LOG4PERL_CONFIG_FILE => 'log4perl.properties';
use constant DEFAULT_LOG_FILE => catfile(File::Spec->tmpdir(), 'sadi.log');
use constant DEFAULT_LOG4PERL_CONFIG => \<<CONFIG;

log4perl.rootLogger = INFO, Screen, Log

log4perl.appender.Screen = Log::Log4perl::Appender::Screen
log4perl.appender.Screen.stderr = 1
log4perl.appender.Screen.Threshold = ERROR
log4perl.appender.Screen.layout = Log::Log4perl::Layout::PatternLayout
log4perl.appender.Screen.layout.ConversionPattern = %d (%r) %p> %F{1}:%L - %m%n

log4perl.appender.Log = Log::Log4perl::Appender::File
log4perl.appender.Log.filename = @{[DEFAULT_LOG_FILE]}
log4perl.appender.Log.mode = append
log4perl.appender.Log.Threshold = INFO
log4perl.appender.Log.layout = Log::Log4perl::Layout::PatternLayout
log4perl.appender.Log.layout.ConversionPattern = %d (%r) %p> %F{1}:%L - %m%n

CONFIG

=head1 NAME

SADI::Utils - what does not fit elsewhere

=cut

=head1 SYNOPSIS

 # load the Utils module
 use SADI::Utils;

 # find a file located somewhere in @INC
 my $file = SADI::Utils->find_file ('resource.file');

 # get file from url
 $file = SADI::Utils->getHttpRequestByURL('http://sadiframework.org');

 # remove leading/trailing whitespace from a string
 print SADI::Utils->trim('  http://sadiframework.org  ');

=cut

=head1 DESCRIPTION

General purpose utilities.

=cut

=head1 AUTHORS

 Edward Kawas (edward.kawas [at] gmail [dot] com)
 Martin Senger (martin.senger [at] gmail [dot] com) 

=head1 SUBROUTINES

=cut

=head2 new

Create a SADI::Utils reference; useless since the methods in this module can be called statically.

=cut

sub new {
    my ( $class, %options ) = @_;

    # create an object
    my $self = { };
    bless $self, ref($class) || $class;
    return $self;
}

#-----------------------------------------------------------------
# find_file
#-----------------------------------------------------------------

=head2 find_file

Try to locate a file whose name is created from the C<$default_start>
and all elements of C<@names>. If it does not exist, try to replace
the C<$default_start> by elements of @INC (one by one). If neither of
them points to an existing file, go back and return the
C<$default_start> and all elements of C<@names> (even - as we know now
- such file does not exist).

There are two or more arguments: C<$default_start> and C<@names>.

=cut

my %full_path_of = ();

sub find_file {
    my $self = shift;

    my ( $default_start, @names );
#    if ( ref($self) =~ /^SADI::Utils/ or $self =~ /^SADI::Utils/) {
    if ( ref($self) =~ /^SADI::Simple::Utils/ or $self =~ /^SADI::Simple::Utils/) {
        ( $default_start, @names ) = @_;
    } else {
        $default_start = $self;
        (@names) = @_;
    }

    my $fixed_part = File::Spec->catfile(@names);
    return $full_path_of{$fixed_part} if exists $full_path_of{$fixed_part};
    my $result = File::Spec->catfile( $default_start, $fixed_part );
    if ( -e $result ) {
        $full_path_of{$fixed_part} = $result;
        return $result;
    }
    foreach my $idx ( 0 .. $#INC ) {
        $result = File::Spec->catfile( $INC[$idx], $fixed_part );
        if ( -e $result ) {
            $full_path_of{$fixed_part} = $result;
            return $result;
        }
    }
    $result = File::Spec->catfile( $default_start, $fixed_part );
    $full_path_of{$fixed_part} = $result;
    return $result;
}

=head2 getHttpRequestByURL

returns a scalar of text obtained from the url or dies if there was no success

=cut

sub getHttpRequestByURL {
    my ( $self, $url ) = @_;
    $url = $self
#      unless ref($self) =~ m/^SADI::Utils/ or $self =~ /^SADI::Utils/;
      unless ref($self) =~ m/^SADI::Simple::Utils/ or $self =~ /^SADI::Simple::Utils/;
    my $ua = LWP::UserAgent->new;
    $ua->agent("SADI/SeS/perl/$VERSION");
    my $req = HTTP::Request->new( GET => $url );

    # accept gzip encoding
    $req->header( 'Accept-Encoding' => 'gzip' );

    # send request
    my $res = $ua->request($req);

    # check the outcome
    if ( $res->is_success ) {
        if (     $res->header('content-encoding')
             and $res->header('content-encoding') eq 'gzip' )
        {
            return $res->decoded_content;
        } else {
            return $res->content;
        }
    } else {
        die "Error getting data from URL:\n\t" . $res->status_line;
    }
}

=head2 empty_rdf

returns a string of RDF that represents a syntactically correct RDF file

=cut

sub empty_rdf {
    return <<'END_OF_RDF';
<?xml version="1.0"?>
<rdf:RDF 
  xmlns:b="http://www.w3.org/2000/01/rdf-schema#"
  xmlns:a="http://protege.stanford.edu/plugins/owl/dc/protege-dc.owl#"
  xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
</rdf:RDF>
END_OF_RDF
}

=head2 trim

trims whitespace from the begining and end of a string

=cut

sub trim {
    my ( $self, $text ) = @_;
    $text = $self
#      unless ref($self) =~ m/^SADI::Utils/ or $self =~ /^SADI::Utils/;
      unless ref($self) =~ m/^SADI::Simple::Utils/ or $self =~ /^SADI::Simple::Utils/;

    # return empty string if $text is not defined
    return "" unless $text;
    $text =~ s/^\s+//;
    $text =~ s/\s+$//;
    return $text;
}


sub rdfxml_to_n3
{
    my ($self, $rdfxml) = @_;

    $rdfxml = $self
      unless ref($self) =~ m/^SADI::Simple::Utils/ or $self =~ /^SADI::Simple::Utils/;

    my $model = RDF::Trine::Model->temporary_model;
    my $parser = RDF::Trine::Parser->new('rdfxml');

    eval { $parser->parse_into_model(undef, $rdfxml, $model) };

    die "failed to convert RDF/XML to TTL, error parsing RDF/XML: $@" if $@;
    
    my $serializer = RDF::Trine::Serializer->new('turtle');
    
    return $self->serialize_model($model, 'text/rdf+n3');
}

sub n3_to_rdfxml
{
    my ($self, $n3) = @_;

    $n3 = $self
      unless ref($self) =~ m/^SADI::Simple::Utils/ or $self =~ /^SADI::Simple::Utils/;

    my $model = RDF::Trine::Model->temporary_model;
    my $parser = RDF::Trine::Parser->new('turtle');

    eval { $parser->parse_into_model(undef, $n3, $model) };

    die "failed to convert N3 to RDF/XML, error parsing N3: $@" if $@;
    
    my $serializer = RDF::Trine::Serializer->new('rdfxml');
    
    return $self->serialize_model($model, 'text/rdf+n3');
}

my @N3_MIME_TYPES = (
    'text/rdf+n3',
    'text/n3',
    'application/x-turtle',
);

sub serialize_model
{
    my ($self, $model, $mime_type) = @_;

    unless(ref($self) =~ m/^SADI::Simple::Utils/ or $self =~ /^SADI::Simple::Utils/) {
        ($model, $mime_type) = @_;
    }

    my $serializer;

    if (grep($_ eq $mime_type, @N3_MIME_TYPES)) {
        $serializer = RDF::Trine::Serializer->new('turtle');
    } else {
        $serializer = RDF::Trine::Serializer->new('rdfxml');
    }
    
    return $serializer->serialize_model_to_string($model);
}

sub init_log4perl
{
    if (-r LOG4PERL_CONFIG_FILE) {
        eval { Log::Log4perl->init(LOG4PERL_CONFIG_FILE) };
        return unless $@;
        warn 'error in ' . LOG4PERL_CONFIG_FILE . ": $@\n";
        warn "reverting to default logging config\n";
    } 

    Log::Log4perl->init(DEFAULT_LOG4PERL_CONFIG);
}

1;
__END__
