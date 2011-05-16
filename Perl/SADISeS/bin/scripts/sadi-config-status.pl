#!/usr/bin/perl -w
#
# Print the current status of configuration and logging files.
#
# $Id: sadi-config-status.pl,v 1.5 2009-08-26 20:02:20 ubuntu Exp $
# Contact: Martin Senger <martin.senger@gmail.com>
# -----------------------------------------------------------

# some command-line options
use Getopt::Std;
use vars qw/ $opt_h $opt_d $opt_v /;
getopt;

# usage
if ($opt_h) {
    print STDOUT <<'END_OF_USAGE';
Print the current status of configuration and logging files.
Usage: [-vdh]

    It also needs those configuration files it reports on
    (but if they do not exists it will be reported, as well).

    -v ... verbose
    -d ... debug
    -h ... help
END_OF_USAGE
    exit (0);
}
# -----------------------------------------------------------

use File::HomeDir;
use SADI::Base;
use strict;

$LOG->level ('INFO') if $opt_v;
$LOG->level ('DEBUG') if $opt_d;


sub say { print @_, "\n"; }

say "Perl-SADI VERSION: $SADI::VERSION\n";

say 'Configuration';
say '-------------';

say "Default configuration file: $SADI::Config::DEFAULT_CONFIG_FILE";
say "Environment variable $SADI::Config::ENV_CONFIG_DIR" .
    ( exists $ENV{$SADI::Config::ENV_CONFIG_DIR} ? ": $ENV{$SADI::Config::ENV_CONFIG_DIR}" : ' is not set');

say 'Successfully read configuration files:';
foreach my $file (SADI::Config->ok_files) {
    say "\t$file";
}
my %failed = SADI::Config->failed_files;
if (keys %failed > 0) {
    say 'Failed configuration files:';
    foreach my $file (sort keys %failed) {
	my $msg = $failed{$file}; $msg =~ s/\n$//;
	say "\t$file => $msg";
    }
}

say 'All configuration parameters:';
foreach my $name (sort SADI::Config->param()) {
    say "\t$name => " . SADI::Config->param ($name);
}

say 'All imported names (equivalent to parameters above): ';
say "\t", join ("\n\t", map { "\$SADICFG::$_" } sort keys %SADICFG::);

say 'Logging';
say '-------';

my $logger_name = $SADI::Base::LOGGER_NAME;
my $logger = Log::Log4perl->get_logger ($logger_name);
say "Logger name (use it in the configuration file): $logger_name";

my @appender_names = @{ $logger->{appender_names} };
my %appenders = %{ Log::Log4perl->appenders };
say 'Available appenders (log destinations):';
foreach my $appender_name (@appender_names) {
    my $appender = $appenders{$appender_name};
    my $details = ($appender->{appender}->{stderr} ? 'stderr' :
		   ($appender->{appender}->{filename} ? $appender->{appender}->{filename} : ''));
    say "\t" . $appender->{name} . ": $details";
}

say 'Logging level FATAL: ' . ($logger->is_fatal ? 'true' : 'false');
say 'Logging level ERROR: ' . ($logger->is_error ? 'true' : 'false');
say 'Logging level WARN:  ' . ($logger->is_warn  ? 'true' : 'false');
say 'Logging level INFO:  ' . ($logger->is_info  ? 'true' : 'false');
say 'Logging level DEBUG: ' . ($logger->is_debug ? 'true' : 'false');

if ($SADICFG::LOG_CONFIG) {
    say "\nLogging configuration file\n\tName: $SADICFG::LOG_CONFIG\n\tContents:";
    open (CONF, '<', $SADICFG::LOG_CONFIG) or say "\tError: $!";
    print $_ while (<CONF>);
    close CONF;
}

say "\nTesting log messages (some may go only to a logfile):";

$LOG->fatal ("Missing Tim Hortons' Donuts");
$LOG->error ('...and we are out of coffee!');

__END__


