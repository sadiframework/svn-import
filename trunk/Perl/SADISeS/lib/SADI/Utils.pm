#-----------------------------------------------------------------
# SADI::Utils
# Author: Edward Kawas <edward.kawas@gmail.com>
# For copyright and disclaimer see LICENSE.
#
# $Id: Utils.pm,v 1.2 2009-10-02 15:53:46 ubuntu Exp $
#-----------------------------------------------------------------

package SADI::Utils;
use File::Spec;
use LWP::UserAgent;
use HTTP::Request;
use strict;

# add versioning to this module
use vars qw /$VERSION/;
$VERSION = sprintf "%d.%02d", q$Revision: 1.3 $ =~ /: (\d+)\.(\d+)/;

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
    my ($self, $default_start, @names) = @_;
    my $fixed_part = File::Spec->catfile (@names);
    return $full_path_of{ $fixed_part } if exists $full_path_of{ $fixed_part };

    my $result = File::Spec->catfile ($default_start, $fixed_part);
    if (-e $result) {
        $full_path_of{ $fixed_part } = $result;
        return $result;
    }

    foreach my $idx (0 .. $#INC) {
        $result = File::Spec->catfile ($INC[$idx], $fixed_part);
        if (-e $result) {
            $full_path_of{ $fixed_part } = $result;
            return $result;
        }
    }
    $result = File::Spec->catfile ($default_start, $fixed_part);
    $full_path_of{ $fixed_part } = $result;
    return $result;
}

=head2 getHttpRequestByURL

returns a scalar of text obtained from the url or dies if there was no success

=cut

sub getHttpRequestByURL {
	my ($self, $url) = @_;
	$url = $self 
		unless ref($self) =~ m/^SADI::Utils/;
	my $ua = LWP::UserAgent->new;
	$ua->agent( "SADI/SeS/perl/$VERSION");

	my $req =
	  HTTP::Request->new( GET =>
		  $url );

	# accept gzip encoding
	$req->header( 'Accept-Encoding' => 'gzip' );

	# send request
	my $res = $ua->request($req);

	# check the outcome
	if ( $res->is_success ) {
		if ( $res->header('content-encoding') and $res->header('content-encoding') eq 'gzip' ) {
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
	my ($self, $text) = @_;
	$text = $self 
		unless ref($self) =~ m/^SADI::Utils/;
	# return empty string if $text is not defined
	return "" unless $text;
	$text =~ s/^\s+//;
	$text =~ s/\s+$//;
	return $text;
}

=head2 lsrnize

Augments LSRN records with ('has attribute' some Class and ('has value' some String))
 
Input: 
    $class: the LSRN record (an OWL::Data::OWL::Class object), 
    $id: a literal representation of the identifier
    
Output:
    $class with the following added to it:

<$class, SIO_000008, Y>
<Y, rdf:type, $identifier>
<Y, SIO_000300, $id>

=cut

sub LSRNize {
	my ($self) = shift;
	my ($class, $id);
	if (ref($self) =~ /^SADI::Utils/) {
		($class, $id) = @_;
	} else {
		$class = $self;
		($id) = @_;
	}
	my $identifier = ref($class);
	$identifier =~ s/_Record$/_Identifier/;
	return $class unless defined $id and defined $identifier;
	eval "require $identifier";
	return $class if $@;
	eval {$class->add_SIO_000008( $identifier->new( SIO_000300 => $id ) );};
	return $class;
}

1;
__END__
