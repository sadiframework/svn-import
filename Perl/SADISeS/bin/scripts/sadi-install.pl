#!/usr/bin/perl -w
#
# Prepare the stage...
#
# $Id: sadi-install.pl,v 1.7 2010-03-09 16:39:14 ubuntu Exp $
# Contact: Edward Kawas <edward.kawas+sadi@gmail.com>
# -----------------------------------------------------------

BEGIN {
	use Getopt::Std;
	use vars qw/ $opt_h $opt_F /;
	getopt;

	# usage
	if ($opt_h) {
		print STDOUT <<'END_OF_USAGE';
Preparing stage for generating and running SADI web services.
Usage: [-F]

    It creates necessary files (some of them by copying from
    their templates):
       sadi-services.cfg
       log4perl.properties
       services.log
       parser.log
    The existing files are not overwritten - unless an option -F
    has been used.

END_OF_USAGE
		exit(0);
	}

	my $errors_found = 0;

	sub say { print @_, "\n"; }

	sub check_module {
		eval "require $_[0]";
		if ($@) {
			$errors_found = 1;
			say "Module $_[0] not installed.";
		} else {
			say "OK. Module $_[0] is installed.";
		}
	}

	use constant MSWIN => $^O =~ /MSWin32|Windows_NT/i ? 1 : 0;

	say 'Welcome, SADIiers. Preparing stage for Perl SADI ...';
	say '------------------------------------------------------';

	# check needed modules
	foreach $module (
					  qw (
					  Carp
					  CGI
					  File::Spec
					  Config::Simple
					  File::HomeDir
					  File::ShareDir
					  Log::Log4perl
					  HTTP::Date
					  Template
					  Params::Util
					  Class::Inspector
					  Unicode::String
					  IO::String
					  RDF::Core
					  )
	  )
	{
		check_module($module);
	}

	if (MSWIN) {
		check_module('Term::ReadLine');
		{
			local $^W = 0;
			$SimplePrompt::Terminal = Term::ReadLine->new('Installation');
		}
	} else {
		check_module('IO::Prompt');
		require IO::Prompt;
		import IO::Prompt;
	}
	if ($errors_found) {
		say "\nSorry, some needed modules were not found.";
		say "Please install them and run 'sadi-install.pl' again.";
		exit(1);
	}
	say;
}
use File::HomeDir;
use File::ShareDir;
use File::Spec;
use SADI::Base;
use File::HomeDir;
use English qw( -no_match_vars );
use strict;

# different prompt modules used for different OSs
# ('pprompt' as 'proxy_prompt')
sub pprompt {
	return prompt(@_) unless MSWIN;
	return SimplePrompt::prompt(@_);
}

# $prompt ... a prompt asking for a directory
# $prompted_dir ... suggested directory
sub prompt_for_directory {
	my ( $prompt, $prompted_dir ) = @_;
	while (1) {
		my $dir = pprompt("$prompt [$prompted_dir] ");
		$dir =~ s/^\s*//;
		$dir =~ s/\s*$//;
		$dir = $prompted_dir unless $dir;
		return $dir if -d $dir and -w $dir;    # okay: writable directory
		$prompted_dir = $dir;
		next if -e $dir and say "'$dir' is not a writable directory. Try again please.";
		next unless pprompt( "Directory '$dir' does not exists. Create? ", -yn );

		# okay, we agreed to create it
		mkdir $dir and return $dir;
		say "'$dir' not created: $!";
	}
}

# create a file from a template
#  - from $file_template to $file,
#  - $file_desc used in messages,
#  - hashref $filters tells what to change in template
sub file_from_template {
	my ( $file, $file_template, $file_desc, $filters ) = @_;
	eval {
		open FILETEMPL, "<$file_template"
		  or die "Cannot read template file '$file_template': $!\n";
		open FILEOUT, ">$file"
		  or die "Cannot open '$file' for writing: $!\n";
		while (<FILETEMPL>) {
			foreach my $token ( keys %$filters ) {
				if ( $^O eq 'MSWin32' ) {
					$filters->{$token} =~ s|\\|\/|;
				}
				s/\Q$token\E/$filters->{$token}/ge;
			}
			print FILEOUT
			  or die "Cannot write into '$file': $!\n";
		}
		close FILEOUT;
		close FILETEMPL
		  or die "Cannot close '$file': $!\n";
	};
	if ($@) {
		say "ERROR: $file_desc was (probably) not created.\n$@";
	} else {
		say "\n$file_desc created: '$file'\n";
	}
}

# --- main ---
no warnings 'once';

my $sadi_home = File::Spec->catdir( File::HomeDir->my_home, "Perl-SADI" );
my $samples_home =
  File::Spec->catdir( File::HomeDir->my_home, "Perl-SADI", "sample-resources" );
say "Installing in $sadi_home\n";

# create install directory if necessary
eval {
	my ( $v, $d, $f ) = File::Spec->splitpath($sadi_home);
	my $dir = File::Spec->catdir($v);
	foreach my $part ( File::Spec->splitdir( ( $d . $f ) ) ) {
		$dir = File::Spec->catdir( $dir, $part );
		next if -d $dir or -e $dir;
		mkdir($dir)
		  || die(
				"Error creating installation directory directory '" . $dir . "':\n$!" );
	}
};
say $@ ? $@ : "Created install directory '$sadi_home'.";

# create directory for cgi scripts files
eval {
	my ( $v, $d, $f ) = File::Spec->splitpath( $sadi_home . "/cgi" );
	my $dir = File::Spec->catdir($v);
	foreach my $part ( File::Spec->splitdir( ( $d . $f ) ) ) {
		$dir = File::Spec->catdir( $dir, $part );
		next if -d $dir or -e $dir;
		mkdir($dir)
		  || die( "Error creating cgi scripts directory '" . $dir . "':\n$!" );
	}
	open (FHO,">$sadi_home/cgi/README") 
	   and print FHO 'This directory contains entry scripts to your generated SADI services.';
    close(FHO);
};
say $@ ? $@ : "Created install directory '$sadi_home/cgi'.";

# create sample resources directory if necessary
eval {
	my ( $v, $d, $f ) = File::Spec->splitpath($samples_home);
	my $dir = File::Spec->catdir($v);
	foreach my $part ( File::Spec->splitdir( ( $d . $f ) ) ) {
		$dir = File::Spec->catdir( $dir, $part );
		next if -d $dir or -e $dir;
		mkdir($dir)
		  || die(
				"Error creating installation directory directory '" . $dir . "':\n$!" );
	}
	open (FHO,">$samples_home/README") 
       and print FHO 'This directory is for resources that your SADI service utilizes.';
    close(FHO);
};
say $@ ? $@ : "Created sample-resources directory '$samples_home'.";

# create service definitions directory if necessary
my $definitions_dir = $SADICFG::GENERATORS_IMPL_DEFINITIONS || "$sadi_home/definitions";;
eval {
	my ( $v, $d, $f ) = File::Spec->splitpath( $definitions_dir );
	my $dir = File::Spec->catdir($v);
	foreach my $part ( File::Spec->splitdir( ( $d . $f ) ) ) {
		$dir = File::Spec->catdir( $dir, $part );
		next if -d $dir or -e $dir;
		mkdir($dir)
		  || die( "Error creating service definitions directory '" . $dir . "':\n$!" );
	}
	open (FHO,">$sadi_home/definitions/README") 
       and print FHO 'This directory contains the definitions for your SADI services.';
    close(FHO);
};
say $@ ? $@ : "Created service defintions directory '$sadi_home/definitions'.";

# create service async directory if necessary
my $async_dir = $SADICFG::ASYNC_TMP || "$sadi_home/async";
eval {
    my ( $v, $d, $f ) = File::Spec->splitpath( $async_dir );
    my $dir = File::Spec->catdir($v);
    foreach my $part ( File::Spec->splitdir( ( $d . $f ) ) ) {
        $dir = File::Spec->catdir( $dir, $part );
        next if -d $dir or -e $dir;
        mkdir($dir)
          || die( "Error creating service async directory '" . $dir . "':\n$!" );
    }
    chmod 0777, $async_dir;
    open (FHO,">$async_dir/README") 
       and print FHO 'This directory contains the temporary files needed to run asynchronous SADI services.';
    close(FHO);
};
say $@ ? $@ : "Created service async directory '$async_dir'.";

# create unit test directory if necessary
my $unittest_dir = $SADICFG::UNITTEST_DIR || "$sadi_home/unittest";
eval {
    my ( $v, $d, $f ) = File::Spec->splitpath( $unittest_dir );
    my $dir = File::Spec->catdir($v);
    foreach my $part ( File::Spec->splitdir( ( $d . $f ) ) ) {
        $dir = File::Spec->catdir( $dir, $part );
        next if -d $dir or -e $dir;
        mkdir($dir)
          || die( "Error creating service unit test directory '" . $dir . "':\n$!" );
    }
    chmod 0777, $unittest_dir;
    open (FHO,">$unittest_dir/README") 
       and print FHO 'This directory contains unit tests for your SADI services.';
    close(FHO);
};
say $@ ? $@ : "Created unit test directory '$unittest_dir'.";

# create directory for xml that user can store input/output in, if necessary
my $xmlin_dir = "$sadi_home/xml";
eval {
    my ( $v, $d, $f ) = File::Spec->splitpath( $xmlin_dir );
    my $dir = File::Spec->catdir($v);
    foreach my $part ( File::Spec->splitdir( ( $d . $f ) ) ) {
        $dir = File::Spec->catdir( $dir, $part );
        next if -d $dir or -e $dir;
        mkdir($dir)
          || die( "Error creating xml directory '" . $dir . "':\n$!" );
    }
    chmod 0777, $unittest_dir;
    open (FHO,">$xmlin_dir/README") 
       and print FHO 'This directory is a place to put your example in/outputs for your SADI services.';
    close(FHO);
};
say $@ ? $@ : "Created example xml directory '$xmlin_dir'.";

# log files (create, or just change their write permissions)
my $log_file1 = $SADICFG::LOG_FILE
  || File::Spec->catfile( "$sadi_home", "services.log" );
my $log_file2 = File::Spec->catfile( "$sadi_home", "parser.log" );
foreach my $file ( $log_file1, $log_file2 ) {
	unless ( -e $file ) {
		eval {
			open LOGFILE, ">$file" or die "Cannot create file '$file': $!\n";
			close LOGFILE or die "Cannot create file '$file': $!\n";
		};
		say $@ ? $@ : "Created log file '$file'.";
	}
	chmod 0666, $file;    # just in case a web server will be writing here
}

# log4perl property file (will be found and used, or created)
my $log4perl_file = $SADICFG::LOG_CONFIG
  || File::Spec->catfile( "$sadi_home", "log4perl.properties" );
if ( -e $log4perl_file and !$opt_F ) {
	say "\nLogging property file '$log4perl_file' exists.";
	say "It will not be overwritten unless you start 'install.pl -F'.\n";
} else {
	file_from_template(
						$log4perl_file,
						File::ShareDir::dist_file(
												  'SADI', 'log4perl.properties.template'
						),
						'Log properties file',
						{
						   '@LOGFILE@'  => $log_file1,
						   '@LOGFILE2@' => $log_file2,
						}
	);
}

# define some directories
my $generated_dir = $SADICFG::GENERATORS_OUTDIR
  || "$sadi_home/generated";
my $services_dir = $SADICFG::GENERATORS_IMPL_OUTDIR
  || "$sadi_home/services";

eval {
    my ( $v, $d, $f ) = File::Spec->splitpath( $generated_dir );
    my $dir = File::Spec->catdir($v);
    foreach my $part ( File::Spec->splitdir( ( $d . $f ) ) ) {
        $dir = File::Spec->catdir( $dir, $part );
        next if -d $dir or -e $dir;
        mkdir($dir)
          || die( "Error creating generated_dir directory '" . $dir . "':\n$!" );
    }
    open (FHO,">$generated_dir/README") 
       and print FHO 'This directory will contain any generated OWL2Perl modules or base SADI service modules.';
    close(FHO);
};
say $@ ? $@ : "Created service defintions directory '$generated_dir'.";

eval {
    my ( $v, $d, $f ) = File::Spec->splitpath( $services_dir );
    my $dir = File::Spec->catdir($v);
    foreach my $part ( File::Spec->splitdir( ( $d . $f ) ) ) {
        $dir = File::Spec->catdir( $dir, $part );
        next if -d $dir or -e $dir;
        mkdir($dir)
          || die( "Error creating service implementation directory '" . $dir . "':\n$!" );
    }
    open (FHO,">$services_dir/README") 
       and print FHO 'This directory contains your SADI service implementation modules.';
    close(FHO);
};
say $@ ? $@ : "Created service implementation directory '$services_dir'.";

# configuration file (will be found and used, or created)
my $config_file =
  File::Spec->catfile( $ENV{$SADI::Config::ENV_CONFIG_DIR} || $sadi_home,
					   $SADI::Config::DEFAULT_CONFIG_FILE );
if ( -e $config_file and !$opt_F ) {
	say "Configuration file $config_file exists.";
	say "It will be used and not overwritten unless you start 'sadi-install.pl -F'.\n";
} else {
	file_from_template(
						$config_file,
						File::ShareDir::dist_file(
												   'SADI', 'sadi-services.cfg.template'
						),
						'Configuration file',
						{
						   '@GENERATED_DIR@'    => $generated_dir,
						   '@SERVICES_DIR@'     => $services_dir,
						   '@DEFINITIONS_DIR@'  => $definitions_dir,
						   '@ASYNC_OUTDIR@'     => $async_dir,
						   '@UNIT_TEST_DIR@'   => $unittest_dir,
						   '@HOME_DIR@'         => $sadi_home,
						   '@LOG4PERL_FILE@'    => $log4perl_file,
						   '@LOGFILE@'          => $log_file1,
						}
	);
}

say 'Done.';

package SimplePrompt;

use vars qw/ $Terminal /;

sub prompt {
	my ( $msg, $flags, $others ) = @_;

	# simple prompt
	return get_input($msg)
	  unless $flags;

	$flags =~ s/^-//o;    # ignore leading dash

	# 'waiting for yes/no' prompt, possibly with a default value
	if ( $flags =~ /^yn(d)?/i ) {
		return yes_no( $msg, $others );
	}

	# prompt with a menu of possible answers
	if ( $flags =~ /^m/i ) {
		return menu( $msg, $others );
	}

	# default: again a simple prompt
	return get_input($msg);
}

sub yes_no {
	my ( $msg, $default_answer ) = @_;
	while (1) {
		my $answer = get_input($msg);
		return $default_answer if $default_answer and $answer =~ /^\s*$/o;
		return 'y' if $answer =~ /^(1|y|yes|ano)$/;
		return 'n' if $answer =~ /^(0|n|no|ne)$/;
	}
}

sub get_input {
	my ($msg) = @_;
	local $^W = 0;
	my $line = $Terminal->readline($msg);
	chomp $line;    # remove newline
	$line =~ s/^\s*//;
	$line =~ s/\s*$//;    # trim whitespaces
	$Terminal->addhistory($line) if $line;
	return $line;
}

sub menu {
	my ( $msg, $ra_menu ) = @_;
	my @data = @$ra_menu;

	my $count = @data;

	#    die "Too many -menu items" if $count > 26;
	#    die "Too few -menu items"  if $count < 1;

	my $max_char = chr( ord('a') + $count - 1 );
	my $menu     = '';

	my $next = 'a';
	foreach my $item (@data) {
		$menu .= '     ' . $next++ . '.' . $item . "\n";
	}
	while (1) {
		print STDOUT $msg . "\n$menu";
		my $answer = get_input(">");

		# blank and escape answer accepted as undef
		return undef if $answer =~ /^\s*$/o;
		return undef
		  if length $answer == 1 && $answer eq "\e";

		# invalid answer not accepted
		if ( length $answer > 1 || ( $answer lt 'a' || $answer gt $max_char ) ) {
			print STDOUT "(Please enter a-$max_char)\n";
			next;
		}

		# valid answer
		return $data[ ord($answer) - ord('a') ];
	}
}

__END__
