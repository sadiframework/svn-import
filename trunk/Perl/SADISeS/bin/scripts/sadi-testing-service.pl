#!/usr/bin/perl -w
#
# Calling a BioMoby services (with or without SOAP).
#
# $Id: sadi-testing-service.pl,v 1.12 2010-01-28 16:30:50 ubuntu Exp $
# Contact: Martin Senger <martin.senger@gmail.com>
# -----------------------------------------------------------

BEGIN {

    # some command-line options
    use Getopt::Std;
    use vars qw/ $opt_h $opt_d $opt_v $opt_l $opt_e $opt_g $opt_p/;
    getopts('hdvl:e:g:C:a:p');

    # usage
    if ( $opt_h or ( not $opt_e and not $opt_g and scalar @ARGV == 0) ) {
        print STDOUT <<'END_OF_USAGE';
Calling SADI services remotely or locally.

   Usage: # calling a local module representing a service
       [-vd] [-l <lib-location>] <package-name> [<input-file>]

       # calling a real service, using HTTP
       -e <service-url> <service-name> [<input-file>]

       # calling a real service to obtain service interface
       -g <service-url>

    <package-name> is a full name of a called module (service)
        e.g. Service::HelloSadiWorld

    -l <lib-location>
        A directory where is called service stored.
        Default: Perl-SADI/services   

    -e <service-url>
        A SADI service url
        (e.g. http://localhost/cgi-bin/HelloSadiWorld)

    -g <cgi-service-url>
        A SADI service url
        (e.g. http://localhost/cgi-bin/HelloSadiWorld)

    <input-file>
        A SADI RDF/XML file with input data for the service.
        Default: an empty SADI request

    -p ... if the service is asynchronous, keep checking the
           service for a result.

    -v ... verbose
    -d ... debug
    -h ... help
END_OF_USAGE
        exit(0);
    }

    if ($opt_e) {
        # calling a real service, using cgi
        eval "use HTTP::Request; 1;"
          or die "$@\n";
        eval "use LWP::UserAgent; 1;"
          or die "$@\n";
    } elsif ($opt_g) {
        # calling a real service, using cgi
        eval "use HTTP::Request; 1;"
          or die "$@\n";
        eval "use LWP::UserAgent; 1;"
          or die "$@\n";
    } else {
        # calling a local service module, without HTTP
        eval "use SADI::Base; 1;";

        # take the lib location from the config file
        require lib;
        lib->import( SADI::Config->param("generators.impl.outdir") );
        require lib;
        lib->import( SADI::Config->param("generators.outdir") );
        unshift( @INC, $opt_l ) if $opt_l;
        $LOG->level('INFO')  if $opt_v;
        $LOG->level('DEBUG') if $opt_d;
    }

}

use strict;
use Carp;
sub _empty_input {
    eval "use SADI::Utils; 1;" or die "$@\n";
    return SADI::Utils::empty_rdf();
}


# --- what service to call
my $module = shift unless $opt_e or $opt_g;    # eg. Service::Mabuhay, or just Mabuhay
my $service;
( $service = $module ) =~ s/.*::// unless $opt_e or $opt_g;

# --- call the service
if ($opt_e) {

    # calling a real service, using HTTP Post
    my $req = HTTP::Request->new( POST => $opt_e );
    my $ua = LWP::UserAgent->new;
    my $input = '';
    if ( @ARGV > 0 ) {
        my $data = shift;    # a file name
        open INPUT, "<$data"
          or die "Cannot read '$data': $!\n";
        while (<INPUT>) { $input .= $_; }
        close INPUT;
    } else {
        $input = _empty_input;
    }
    print "\nSending to $opt_e the following:\n$input\n" if $opt_d or $opt_v;
    $req->content_type('application/rdf+xml');
    $req->content("$input");
    my $response = $ua->request($req); 
    print "\n" . $response->as_string . "\n";
    if ($opt_p) {
        if ($response->status_line =~ m/202|302/ 
          or ($response->header('pragma') 
          and $response->header('pragma') =~ m/sadi-please-wait/)) {
            print "\nAsynchronous service detected ... Going to attempt to poll it!\n";
            &_poll_until_done($response->content);
        }
    }
    print "\nDone!\n";

} elsif ($opt_g) {
    # calling a real SADI service, using HTTP Get
    my $ua = LWP::UserAgent->new;
    my $req = HTTP::Request->new( GET => $opt_g );
    my $response = $ua->request($req);
    print "\n" . $response->as_string . "\n";
    if ($opt_p) {
        if ($response->status_line =~ m/202|302/
         or ($response->header('pragma') 
         and $response->header('pragma') =~ m/sadi-please-wait/)) {
            print "\nAsynchronous service detected ... Going to attempt to poll service!\n" if $opt_d or $opt_v;
            &_poll_until_done($response->content);
        }
    }
    print "\nDone!\n";
    exit;
} else {

    # calling a local service module, without HTTP
    my $data;
    if ( @ARGV > 0 ) {
        $data = shift;    # a file name
    } else {
        use File::Temp qw( tempfile );
        my $fh;
        ( $fh, $data ) = tempfile( UNLINK => 1 );
        print $fh _empty_input();
        close $fh;
    }
    eval "require $module" or croak $@;
    eval {
        my $target = new $module;
        print $target->$service($data), "\n";
    } or croak $@;
}

sub _poll_until_done {
    my $content = shift;
    require RDF::Core::Model;
    require RDF::Core::Storage::Memory;
    require RDF::Core::Model::Parser;
    require RDF::Core::Resource;
    my $storage = new RDF::Core::Storage::Memory;
    my $model = new RDF::Core::Model (Storage => $storage);
    
    my %options = (Model => $model,
              Source => $content,
              SourceType => 'string',
              BaseURI => "http://www.foo.com/",
    );
    my $parser = new RDF::Core::Model::Parser(%options);
    $parser->parse;
    
    # extract all of the polling urls
    my %urls;
    my $enumerator = $model->getStmts(undef, new RDF::Core::Resource('http://www.w3.org/2000/01/rdf-schema#isDefinedBy') , undef);
    my $statement = $enumerator->getFirst;
    while (defined $statement) {
        my $url = $statement->getObject;
        $urls{$url->getURI} = 1;
        $statement = $enumerator->getNext
    }
    $enumerator->close;
    # free memory
    $model=undef; $storage = undef; $parser = undef; $enumerator = undef;
    
    my $sleep_time = 15;
    # now keep polling our unique urls ...
    while (scalar(keys(%urls)) > 0) {
        foreach (keys %urls) {
            print "   polling $_\n" if $opt_d or $opt_v;
            my $ua = LWP::UserAgent->new;
            my $req = HTTP::Request->new( GET => $_ );
            my $response = $ua->request($req);
            unless ($response->status_line =~ m/202|302/ 
                or ($response->header('pragma') 
                and $response->header('pragma') =~ m/sadi-please-wait/)) {
                print "\n", $response->as_string, "\n";
                delete $urls{$_};
            }
        }
        next if scalar(keys(%urls)) == 0;
        # should sleep as long as sadi-please-wait says to ... but
        if (($sleep_time * 1.5 ) < 300) {
            # progressively sleep longer as to not piss off servers
            $sleep_time = int ($sleep_time*1.5);
        }
        print "   waiting $sleep_time seconds to poll again ...\n" if $opt_d or $opt_v;
        sleep($sleep_time);
    }
}
__END__
