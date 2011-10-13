package CGI::Test;

use IO::String;
use URI::Escape;

require Exporter;
our @ISA = qw(Exporter);
our @EXPORT_OK = qw(simulate_cgi_request);

sub simulate_cgi_request
{
    my %args = @_;

    my @required_args = (
       'request_method',
       'cgi_script',
       'input_file',
       'content_type',
    );
    
    foreach my $arg (@required_args) {
        die "missing required arg '$arg'" unless grep($_ eq $arg, keys %args);
    }

    local %ENV;

    $ENV{REQUEST_METHOD} = uc($args{request_method});
    $ENV{CONTENT_TYPE} = $args{content_type};
    $ENV{HTTP_ACCEPT} = $args{http_accept};
    $ENV{QUERY_STRING} = _build_query_string($args{params}) if $args{params};

    local *STDIN;
    local *STDOUT;
    
    open(STDIN, '<', $args{input_file}) or die sprintf('couldn\'t open %s as STDIN: %s', $args{input_file}, $!);
    tie *STDOUT, 'IO::String', my $cgi_output or die "couldn\'t redirect STDOUT to variable: $!";
  
    do $args{cgi_script}; 

#    open(my $fh, '-|', $args{cgi_script}) or die "error running '$args{cgi_script}'";
#    my $cgi_output = join("\n", <$fh>);

    my ($headers, $output) = split(/\cM\cJ\cM\cJ/s, $cgi_output, 2);
    my $headers_hash = _parse_headers($headers);

    return wantarray ? ($output, $headers_hash) : $output;   
}

sub _build_query_string
{
    my $params = shift;

    my $query_string = '';        
    my $first_param = 1;

    foreach my $key (keys %$params) {
        $query_string .= '&' unless $first_param;
        $query_string .= sprintf('%s=%s', uri_escape($key), uri_escape($params->{$key}));
        $first_param = 0;
    }

    return $query_string;
}

sub _parse_headers
{
    my $headers = shift;
    my %hash = ();

    foreach my $header_line (split(/\cM\cJ/, $headers)) {
        my ($key, $value) = split(/:/, $header_line, 2);
        $value =~ s/^\s*(\S*)\s*$/$1/;
        $hash{$key} = $value;
    }

    return \%hash;
}

1;
