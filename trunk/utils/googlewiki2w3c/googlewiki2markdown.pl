#!/usr/bin/perl

use strict;
use warnings;

use lib 'lib';
use URI;
use File::Slurp;
use File::Spec::Functions qw(splitpath catfile);
use Getopt::Long;
use Markdent::Parser;
use GoogleWiki2Markdown;
use Markdent::Handler::TableOfContents;  # my custom Markdown parser
use Log::Log4perl qw(:easy);
use constant::boolean;

#----------------------------------------------------------------------
# constants
#----------------------------------------------------------------------

use constant USAGE => <<'HEREDOC';
USAGE: ./googlewiki2markdown.pl [options] GoogleWikiPage.wiki

Convert a Google Wiki document to equivalent Markdown (on STDOUT).

Options:

--toc-mode                    Treat GoogleWikiPage.wiki as a table of contents file.  Build
                              a markdown document that includes all pages/sections
                              referenced in the table of contents.

--reference-file <wiki file>  This option is only relevant when the --toc-mode option
                              is used.  It specifies that a file contains citations.
                              Such wiki files are formatted specially; headers at level 2 
                              or lower are treated as labels for citations, and the 
                              corresponding sections are converted to items in an HTML list.

--debug                       Turn on debugging output on STDERR.

--help                        Show this help page.

HEREDOC

#----------------------------------------------------------------------
# command line parsing
#----------------------------------------------------------------------

my @reference_files;
my $toc_mode = FALSE;
my $help = FALSE;
my $debug = FALSE;
my $getopt_success = GetOptions(
    'toc-mode' => \$toc_mode, 
    'reference-file=s' => \@reference_files,
    'help' => \$help,
    'debug' => \$debug,
);

if ($help) { warn USAGE; exit 0; }
die USAGE."\n" unless $getopt_success;
die USAGE."\n" unless @ARGV == 1;

#----------------------------------------------------------------------
# logging
#----------------------------------------------------------------------

Log::Log4perl->easy_init($debug ? $DEBUG : $WARN);

#----------------------------------------------------------------------
# main
#----------------------------------------------------------------------

my $input_file = $ARGV[0];
my $google = read_file($input_file);
my $markdown = GoogleWiki2Markdown::convert($google);

if ($toc_mode) {
    my $working_dir = (splitpath($input_file))[1];
    my $handler = Markdent::Handler::TableOfContents->new($working_dir, @reference_files);
    my $parser = Markdent::Parser->new(handler => $handler);
    $parser->parse(markdown => $markdown);
    $markdown = $handler->get_generated_markdown();
} 

print $markdown;
