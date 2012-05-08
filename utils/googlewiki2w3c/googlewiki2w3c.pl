#!/usr/bin/perl

use strict;
use warnings;
use utf8;
no warnings qw(once);
use autodie qw(:all);
use open ':encoding(utf8)';
use open ':std';

use lib 'lib';
use constant::boolean;
use XML::Writer;
use Getopt::Long;
use URI;
use File::Temp qw(tempdir);
use File::Spec::Functions qw(splitpath catfile);
use IO::File;
use GoogleWiki2Markdown;
use Markdent::Parser;
use File::Slurp;
use Template;
use DateTime;

#----------------------------------------------------------------------
# constants
#----------------------------------------------------------------------

use constant TEMP_XML_FILE => 'temp.xml';
use constant TEMP_OUTPUT_FILE => 'temp.txt';

#----------------------------------------------------------------------
# parse command line args
#----------------------------------------------------------------------

use constant USAGE => <<'HEREDOC';
Usage: gcw2rfc.pl [options] [middle.wiki] [back.wiki] > rfc.txt

"middle.wiki" is a Google Wiki file containing the Table of Contents (in the form of a
bulleted list, possibly nested) for the main part of the document.

"back.wiki" is a Google Wiki file containing the Table of Contents for the end matter 
of the document (e.g. References).
HEREDOC

my $help_opt = FALSE;
my $trace_opt = FALSE;
my $rfc_xml_filename;
my @reference_files = ();
my $intended_status;

my $getopt_success = GetOptions(
    'trace' => \$trace_opt,
    'references=s' => \@reference_files,
    'help' => \$help_opt,
);

die USAGE."\n" unless $getopt_success;

if ($help_opt) {
    warn USAGE;
    exit 0;
}

die USAGE."\n" unless @ARGV >= 1;

my $middle_file = $ARGV[0];
my $back_file = $ARGV[1];

$::RD_ERRORS = 1;
$::RD_TRACE = 1 if $trace_opt;
$::RD_HINT = 1 if $trace_opt;

#----------------------------------------------------------------------
# templates
#----------------------------------------------------------------------

use constant HTML_TEMPLATE => <<'HEREDOC';
<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en-US" lang="en-US">

<head>
    <meta http-equiv="content-type" content="text/html; charset=UTF-8"/>
    <title>[% doc_title %]</title>
    <link rel="stylesheet" type="text/css" href="http://www.w3.org/StyleSheets/TR/W3C-Member-SUBM" /> 
</head>

<body>
    <div class="head">
        <p>
            <a href="http://www.w3.org/"><img alt="W3C" height="48" src="http://www.w3.org/Icons/w3c_home" width="72"/></a>
            <a href="http://www.w3.org/Submission/"><img height="48" width="211" src="http://www.w3.org/Icons/member_subm" alt="W3C Member Submission"/></a>
        </p>
        
        <h1 style="clear:both" id="title">[% doc_title %]</h1>
        
        <h2 id="W3C-doctype">W3C Member Submission [% submission_date %]</h2>  <!-- example: '07 June 2011' -->
        
        <dl>
            <dt>This version:</dt>
            <dd><a href="[% this_version_url %]"></a>[% this_version_url %]</dd> <!-- example: http://www.w3.org/Submission/2011/SUBM-SVGTL-20110607/ -->
            <dt>Latest version:</dt>
            <dd><a href="[% latest_version_url %]">[% latest_version_url %]</a></dd>
        </dl>
        
        <dl>
            <dt>Authors:</dt>
[% FOREACH author IN authors %]
            <dd>[% author %]</dd>
[% END %]
        </dl>
        
        <p class="copyright">

            <!-- Optional copyright statement goes here, e.g.
                
                 Copyright c [% copyright_year %] <span class="notetoeditor">KDDI CORPORATION</span>. 

                 Use Creative Commons License here?
            -->
            This document is available under the <a href="http://www.w3.org/Consortium/Legal/copyright-documents">W3C Document License</a>. See the 
            <a href="http://www.w3.org/Consortium/Legal/ipr-notice#Copyright">W3C Intellectual Rights Notice and Legal Disclaimers</a> 
            for additional information.
        </p>
        
        <hr/>
    </div> 

[% body_html %]    

</body>
HEREDOC

#----------------------------------------------------------------------
# main
#----------------------------------------------------------------------

# temp input/output files, because xml2rfc.tcl doesn't do STDIN/STDOUT

my $tempdir = tempdir(CLEANUP => 1);
$rfc_xml_filename ||= catfile($tempdir, TEMP_XML_FILE);
my $rfc_xml_file = IO::File->new($rfc_xml_filename, '>');
my $temp_output_filename = catfile($tempdir, TEMP_OUTPUT_FILE);

## translate Google Wiki doc hierarchy => RFC XML
#
#my $xml_writer = new XML::Writer(
##    OUTPUT => $rfc_xml_file, 
#    OUTPUT => \$body_html, 
#    UNSAFE => 1,               # allows us to insert front.xml verbatim
##    DATA_MODE => 1,            # newlines after tags
#    DATA_INDENT => 2,          # proper indentation
#);

#wiki_toc_to_html($xml_writer, $middle_file, \@reference_files) if $middle_file;
my $body_html = wiki_toc_to_html($middle_file, \@reference_files) if $middle_file;

#if ($back_file || @reference_files) {
#    $xml_writer->startTag('back');
#    wiki_references_to_xml($xml_writer, $_) foreach @reference_files;
#    wiki_toc_to_html($xml_writer, $back_file) if $back_file;
#    $xml_writer->endTag('back');
#}

#$xml_writer->end();

my $now = DateTime->now;

my $templater = Template->new();
my $template = HTML_TEMPLATE;
my $output_html;

$templater->process(
    \$template,
    {
        doc_title => 'Semantic Automated Discovery and Integration (SADI)',
        authors => 
            [
                'E. Luke McCarthy, University of British Columbia',
                'Ben Vandervalk, University of British Columbia',
                'Mark D. Wilkinson, Universidad Politécnica de Madrid', 
            ],
        submission_date => sprintf('%02d %s %d', $now->day, $now->month_name, $now->year),
        this_version_url => '',
        latest_version_url => '',
        copyright_year => $now->year,
        body_html => $body_html,
    },
    \$output_html,
);

#print $body_html;
print $output_html;

#$rfc_xml_file->close();
#
#if ($trace_opt) {
#    warn "XML output:\n";
#    open (my $xml_fh, '<', $rfc_xml_filename);
#    warn $_ while (<$xml_fh>);
#    close($xml_fh);
#    warn "\n";
#}
#
## translate RFC XML => RFC text file
#
#system(
#    'tclsh', 
#    'xml2rfc.tcl', 
#    'xml2rfc', 
#    $rfc_xml_filename, 
#    $temp_output_filename
#);
#
## send output to STDOUT
#
#my $output = read_file($temp_output_filename);
#if ($intended_status) {
#    my $status_string = "Intended status: $intended_status";
#    my $length = length($status_string);
#    $output =~ s/^ {$length}/$status_string/m;
#}
#print $output;

# Inner class to track section numbers

eval {
    package SectionNumber;

    sub new {
        my ($class, $first_section) = @_;
        my @parts = ();
        @parts = split(/\./, $first_section) if defined($first_section);
        my $self = {
            parts => \@parts,
        };
        return bless($self, $class);
    }

    sub get_section {
        return join('.', @{$_[0]->{parts}});
    }

    sub inc {
        ($_[0]->{parts}->[-1])++;
    }

    sub add_level {
        push(@{$_[0]->{parts}}, 0);
    }

    sub remove_level {
        pop(@{$_[0]->{parts}});
    }

    sub num_levels {
        my ($self, $new_val) = @_;
        if (defined($new_val)) {
            while ($self->num_levels() < $new_val) {
                $self->add_level();
            }
            while ($self->num_levels() > $new_val) {
                $self->remove_level();
            }
        }
        return scalar(@{$self->{parts}});
    }

    package __PACKAGE__;
};

die $@ if $@;

#----------------------------------------------------------------------
# callbacks from wiki parser (generates RFC XML)
#----------------------------------------------------------------------

sub wiki_toc_to_html {

#    my ($xml_writer, $wiki_file, $reference_files) = @_;
    my ($wiki_file, $reference_files) = @_;

    my $toc_html;
    my $body_html;

    my $toc_xml_writer = new XML::Writer(
        OUTPUT => \$toc_html, 
        UNSAFE => 1,               # allows us to insert front.xml verbatim
#        DATA_MODE => 1,            # newlines after tags
        DATA_INDENT => 2,          # proper indentation
    );

    my $xml_writer = new XML::Writer(
        OUTPUT => \$body_html, 
        UNSAFE => 1,               # allows us to insert front.xml verbatim
#        DATA_MODE => 1,            # newlines after tags
        DATA_INDENT => 2,          # proper indentation
    );

    my $working_dir = (splitpath($wiki_file))[1];
    my $input = read_file($wiki_file);
    my $page = filename_to_wiki_page($wiki_file);

    my @reference_pages = ();
    push(@reference_pages, (splitpath($_))[2]) foreach @$reference_files;
    s/\.wiki$// foreach @reference_pages;

    # create a class containing parser callbacks
    eval {

        package Markdent::Handler::TOC;

        use strict; 
        use warnings;

        use URI;
        use Moose;
        use GoogleWiki2Markdown;
        use constant::boolean;
        use Clone qw(clone);

        with 'Markdent::Role::EventsAsMethods';

        our $AUTOLOAD; 
        sub AUTOLOAD { 
            ::trace(sprintf('unhandled event %s with args (%s)', $AUTOLOAD, join(', ', @_))); 
        }

        sub new {
#            my ($class, $xml_writer, $working_dir, $page, $reference_pages) = @_;
            my ($class, $toc_xml_writer, $xml_writer, $working_dir, $page, $reference_pages) = @_;
            my $self = {
                toc_xml_writer => $toc_xml_writer,
                xml_writer => $xml_writer,
                working_dir => $working_dir,
                list_level => 0,
                visited => {$page => 1},   # visited pages or sections of pages
                reference_pages => $reference_pages,
                skip_item => FALSE,
                in_link => FALSE,
                section_number => SectionNumber->new(),
            };
            return bless($self, $class);
        }

        sub start_document {
            my $self = shift;
            $self->{toc_xml_writer}->startTag('h2', id => 'TableOfContents');
                $self->{toc_xml_writer}->characters('Table of Contents');                
            $self->{toc_xml_writer}->endTag('h2');
        }

        sub start_unordered_list { 
            my $self = shift;
            $self->{list_level}++; 
            printf("open list: %d\n", $self->{list_level});
            $self->{section_number}->add_level();
            $self->{toc_xml_writer}->startTag('ul');
        }

        sub end_unordered_list { 
            my $self = shift;
            $self->{list_level}--; 
            printf("close list: %d\n", $self->{list_level});
            $self->{section_number}->remove_level();
            $self->{toc_xml_writer}->endTag('ul');
        }

        sub start_list_item {
            my $self = shift;
            $self->{toc_xml_writer}->startTag('li');
        }

        sub end_list_item {
            my $self = shift;
#            $self->{xml_writer}->endTag('section') unless $self->{skip_item};
            $self->{skip_item} = FALSE;
            $self->{toc_xml_writer}->endTag('li');
        }

        sub start_link { 
            my $self = shift;
            %_ = @_;
            my $uri = URI->new($_{uri});
            # if uri is relative
            if (!defined($uri->scheme())) {
                $self->{in_link} = TRUE;
                $self->{link_uri} = $uri;
           }
        }

        sub end_link {
           $_[0]->{in_link} = FALSE; 
        }

        sub text {

            my $self = shift;
            my $xml_writer = $self->{xml_writer};
            my $toc_xml_writer = $self->{toc_xml_writer};
            my $visited = $self->{visited};
            my $text = ::trim($_[1]);
            my $section_number = $self->{section_number};
            
            # ignore whitespace text events (e.g. newlines at end of list items)
            return unless $text; 

            print "item: $text\n";

#            $self->inc_section_number();
            $section_number->inc();
 

            my $in_link = $self->{in_link};
#            my $anchor = $in_link ? $self->{link_uri}->as_string : main::get_anchor('',$text);
            my $anchor = $in_link ? $self->{link_uri}->as_string : main::get_anchor('',$text);

#            ::tracef('anchor: \'%s\'', $anchor);
#            ::tracef('visited? %s', ($visited->{$anchor} ? 'yes' : 'no'));
#            ::tracef('visited: (%s)', join(',', %$visited));

            $toc_xml_writer->startTag('a', href => '#'.$anchor);
                $toc_xml_writer->characters($section_number->get_section() . ' ' . $text);
            $toc_xml_writer->endTag('a');

            if ($visited->{$anchor}) {
                $self->{skip_item} = TRUE;
                ::tracef('skipping TOC entry %s, page/section already visited', $anchor);
                return;
            }

#            $xml_writer->startTag('section', anchor => $anchor, title => $text);
            my $header_tag = sprintf('h%d', $self->{list_level} + 1);
            $xml_writer->startTag($header_tag, id => $anchor);
                $xml_writer->characters($section_number->get_section() . ' ' . $text);
            $xml_writer->endTag($header_tag);
           
#            if ($in_link) {
#                ::trace("following TOC link: $_{uri}");
#                ::wiki_to_xml(
#                    $xml_writer, 
#                    $self->{working_dir},
#                    $self->{visited}, 
#                    $self->{link_uri}->path, # page
#                    ($self->{list_level} - 1),
#                    clone($section_number),
#                    $self->{reference_pages},
#                    $self->{link_uri}->fragment # section (optional)
#                );
#            }
           
        }

#        sub get_section_number {
#            return join('.', @{$_[0]->{section_number}});
#        }
#        
#        sub inc_section_number {
#            ($_[0]->{section_number}->[-1])++;
#        }
#        
#        sub add_section_number_level {
#            push(@{$_[0]->{section_number}}, 0);
#        }
#       
#        sub remove_section_number_level {
#            pop(@{$_[0]->{section_number}});
#        }

        package __PACKAGE__; 

    };

    die $@ if $@;

    my $markdown = GoogleWiki2Markdown::convert($input);
    write_file('markdown', $markdown);
#    printf("markdown:\n%s\n", $markdown);
    my $handler = Markdent::Handler::TOC->new($toc_xml_writer, $xml_writer, $working_dir, $page, \@reference_pages);
    my $parser = Markdent::Parser->new(handler => $handler);
    $parser->parse(markdown => $markdown);

    $toc_xml_writer->end();
    $xml_writer->end();

    return $toc_html . "\n" . $body_html;
    
}


sub wiki_references_to_xml {

    my ($xml_writer, $wiki_file) = @_;
    
    my $working_dir = (splitpath($wiki_file))[1];
    my $input = read_file($wiki_file);
    my $page = filename_to_wiki_page($wiki_file);

    # create a class containing parser callbacks
    eval {

        package Markdent::Handler::ReferencePage;

        use strict; 
        use warnings;
        use utf8;
        use open ':encoding(utf8)';
        use open ':std';
            
        use URI;
        use Encode;
        use Moose;
        use GoogleWiki2Markdown;
        use constant::boolean;

        with 'Markdent::Role::EventsAsMethods';

        our $AUTOLOAD; 
        sub AUTOLOAD { 
#            ::trace(sprintf('unhandled event %s with args (%s)', $AUTOLOAD, join(', ', @_))); 
        }

        sub new {
            my ($class, $xml_writer, $working_dir, $page) = @_;
            my $self = {
                xml_writer => $xml_writer,
                working_dir => $working_dir,
                page => $page,
                current_header => undef,
                in_header => FALSE,
                first_header => TRUE,
            };
            return bless($self, $class);
        }

        sub end_document {
            $_[0]->{xml_writer}->endTag('references');
        }

        sub start_header  {
            ::trace('start_header');
            $_[0]->{in_header} = TRUE;
        }

        sub end_header {
            ::trace('end_header');
            $_[0]->{in_header} = FALSE;
            $_[0]->{first_header} = FALSE;
        }

        sub text {
            my $self = $_[0];
            my $text = main::trim($_[2]);
            my $xml_writer = $self->{xml_writer};
            if ($self->{in_header}) {
                ::tracef('header text event (%d): %s', $self->{first_header}, $text);
                $xml_writer->startTag('references', title => $text) if $self->{first_header};
                $self->{current_header} = $text;
            } else {
                ::tracef('non-header text event: %s', $text);
                unless ($self->{first_header}) {

                    # Markdent assumes ISO-8859-1, but we need UTF-8 here 
                    # (wiki citations contain Unicode quote chars)
                    my $text = decode('utf8', encode('iso-8859-1', $text));

                    my $anchor = main::get_anchor(undef, $self->{current_header});
        
                    my @citation = split(/,\s*/, $text);
                    my @authors = ();
                    my $title;
                    my @doc_ids;
                    my $year;

                    # parse authors list
                    while ($citation[0] !~ /\s*["'“”]/) {
                        my $last = shift(@citation);
                        # last author might have "and" before last name
                        $last =~ s/^\s*and\s*//;
                        last if ($last =~ /et al/);
                        my $first = shift(@citation);
                        # doc with exactly two authors might separate
                        # author names with an "and" (and no comma)
                        if ($first =~ /(.*)\s+and\s+(.*)/) {
                            $first = $1;
                            unshift(@citation, $2);
                        }
                        push(@authors, $first, $last);
                    }
                    
                    $title = shift(@citation);
                    $title = main::trim_quotes($title);

                    # parse list of document IDs (e.g. "RFC 1234")
                    while (@citation > 1) {
                        my $id = shift(@citation);
                        $id = main::trim_quotes($id);
                        my @parts = split(/\s+/, $id);
                        if (grep($parts[0] =~ /$_/i, ('RFC', 'STD', 'BCP'))) {
                            push(@doc_ids, $parts[0], $parts[1]);
                        } else {
                            push(@doc_ids, $id, "");
                        }
                    }

                    $year = shift(@citation);
                    $year =~ s/\.\s*$//;

                    unless (@authors && (@authors % 2 == 0) && $title && @doc_ids && (@doc_ids % 2 == 0) && $year) {
                        warn "failed to parse citation: $text\n";
                        warn sprintf("\@authors: (%s)", join(', ', @authors));
                        warn "title: $title\n";
                        warn sprintf("\@doc_ids: (%s)", join(', ', @doc_ids));
                        warn "year: $year\n"; 
                        return;
                    }

                    $xml_writer->startTag('reference', anchor => $anchor, title => $self->{current_header});    
                    $xml_writer->startTag('front');
                    $xml_writer->startTag('title');
                    $xml_writer->characters($title);
                    $xml_writer->endTag('title');
                    while (@authors) {
                        my $first = shift(@authors);
                        my $last = shift(@authors);
                        $xml_writer->emptyTag(
                                'author', 
                                initials => $first,
                                surname => $last,
                                fullname => "$first $last"
                            );
                    }
                    $xml_writer->emptyTag('date', year => $year);
                    $xml_writer->endTag('front');
                    while (@doc_ids) {
                        my $series = shift(@doc_ids);
                        my $id = shift(@doc_ids);
                        $xml_writer->emptyTag('seriesInfo', name => $series, value => $id);
                    }
                    $xml_writer->endTag('reference');

                }
            }
        }

        package __PACKAGE__; 

    };

    die $@ if $@;

    my $markdown = GoogleWiki2Markdown::convert($input);
#    printf("markdown:\n%s\n", $markdown);
    my $handler = Markdent::Handler::ReferencePage->new($xml_writer, $working_dir, $page);
    my $parser = Markdent::Parser->new(handler => $handler);
    $parser->parse(markdown => $markdown);

}

sub wiki_to_xml {
    
    my ($xml_writer, $working_dir, $visited, $page, $section_level_offset, $section_number, $reference_pages, $section) = @_;

    my $uri = $section ? get_anchor($page, $section) : $page;

    if ($visited->{$uri}) {
        trace(sprintf('skipping %s, page/section already visited', $uri));
        return;
    }

    trace(sprintf('visiting %s', $uri));
    $visited->{$uri} = 1;

    my $input = read_file(wiki_page_to_filename($working_dir, $page));

    # create a class containing parser callbacks
    eval {

        package Markdent::Handler::Page;

        use strict; 
        use warnings;

        use constant::boolean;
        use Moose;

        with 'Markdent::Role::EventsAsMethods';

        our $AUTOLOAD; 
        sub AUTOLOAD { 
#            ::trace(sprintf('unhandled event %s with args (%s)', $AUTOLOAD, join(', ', @_))); 
        }

        sub new {
            my ($class, $xml_writer, $visited, $page, $section_level_offset, $section_number, $reference_pages, $section) = @_;
            my $self = {
                xml_writer => $xml_writer,
                visited => $visited,
                page => $page,
                section_level_offset => $section_level_offset,
                reference_pages => $reference_pages,
                section => $section,
                open_sections => [],
                output_enabled => ($section ? 0 : 1),
                header_level => 0,
                section_number => $section_number,
                first_section_on_page => TRUE,
                in_inline_code => FALSE,
                in_link => FALSE,
            };
            return bless($self, $class);
        }

        sub output_enabled {
            my $self = $_[0];
            my $value = $_[1];
            $self->{output_enabled} = $value if defined($value);
            return $self->{output_enabled};
        }

        sub start_header { 
            $_[0]->{header_level} = $_[2];
        }

        sub end_header { 
            $_[0]->{header_level} = 0;
        }
        
        sub text {
            my $self = $_[0];
            my $text = $_[2];
            my $xml_writer = $self->{xml_writer};
            if ($self->{header_level}) {
                $self->header($self->{header_level}, $text); 
            } else {
                ::tracef('text event: \'%s\'', $text);
                if ($self->{in_link}) {
                    my $uri = $self->{link_uri};
                    unless (defined($uri->scheme)) {
                        my $page = $uri->path;
                        my $section = $uri->fragment;
                        my $anchor = main::get_anchor($page, $section);
                        if (grep(/$page/, @{$self->{reference_pages}})) {
                            $xml_writer->emptyTag('xref', target => main::get_anchor(undef, $section), format => 'title');
                        } else {
                            $xml_writer->characters($text);
                            $xml_writer->characters(' (');
                            $xml_writer->emptyTag('xref', target => $anchor);
                            $xml_writer->characters(')');
                        }
                    }
                } else {
                    $xml_writer->characters($text);
                }
            }
        } 

        sub start_strong {
            $_[0]->{xml_writer}->characters('*');
        }

        sub end_strong {
            $_[0]->{xml_writer}->characters('*');
        }

        sub start_emphasis {
            $_[0]->{xml_writer}->characters('_');
        }

        sub end_emphasis {
            $_[0]->{xml_writer}->characters('_');
        }

        sub start_unordered_list {
#            $_[0]->{xml_writer}->startTag('t');
#            $_[0]->{xml_writer}->startTag('list', style => 'symbols');
            $_[0]->{xml_writer}->startTag('ul');
        }

        sub end_unordered_list {
#            $_[0]->{xml_writer}->endTag('list');
            $_[0]->{xml_writer}->endTag('ul');
#            $_[0]->{xml_writer}->endTag('t');
        }

        sub start_ordered_list {
#            $_[0]->{xml_writer}->startTag('t');
#            $_[0]->{xml_writer}->startTag('list', style => 'numbers');
            $_[0]->{xml_writer}->startTag('ol');
        }

        sub end_ordered_list {
#            $_[0]->{xml_writer}->endTag('list');
            $_[0]->{xml_writer}->endTag('ol');
#            $_[0]->{xml_writer}->endTag('t');
        }

        sub start_list_item {
#            $_[0]->{xml_writer}->startTag('t');
            $_[0]->{xml_writer}->startTag('li');
        }

        sub end_list_item {
#            $_[0]->{xml_writer}->endTag('t');
            $_[0]->{xml_writer}->endTag('li');
        }

        sub start_paragraph {
            my $self = $_[0];
#            $self->{xml_writer}->startTag('t') unless $self->{in_inline_code};
            $self->{xml_writer}->startTag('p') unless $self->{in_inline_code};
        }

        sub end_paragraph {
            my $self = $_[0];
#            $self->{xml_writer}->endTag('t') unless $self->{in_inline_code};
            $self->{xml_writer}->endTag('p') unless $self->{in_inline_code};
        }

        sub start_link { 
            ::trace('start_link');

            my $self = shift;
            my $xml_writer = $self->{xml_writer};
           
            %_ = @_;
#            my $uri = URI->new($_{uri});
#            my $uri_is_absolute = defined($uri->scheme());

            $self->{in_link} = TRUE;
            $self->{link_uri} = URI->new($_{uri});

#            if ($uri_is_absolute) {
#            } else {
#                my $reference_pages = $self->{reference_pages};
#                my $page = $uri->path;
#                my $section = $uri->fragment;
#                my $anchor = main::get_anchor($page, $section);
#                $xml_writer->emptyTag('xref', target => $anchor);
#            }

        }

        sub end_link {
            ::trace('end_link');
            $_[0]->{in_link} = FALSE;
        }

        sub html_block {

            ::tracef("html_block event: (%s)\n", join(',',@_));

            my $xml_writer = $_[0]->{xml_writer};
            my $html = $_[2];
            
            # code blocks

            if ($html =~ /^\s*<pre>\s*<code>(.*?)<\/code>\s*<\/pre>\s*$/sm) {

                my $code = $1;

#                $xml_writer->startTag('figure');
#                $xml_writer->startTag('artwork');
                $xml_writer->startTag('div', class => 'source');
                $xml_writer->startTag('pre');

#                # separate code from preceding text/figure/list with blank line
#                $code =~ s/^(.*\S.*)/\n$1/;
#
#                # indent 6 spaces
#                $code =~ s/^/' 'x6/emg;
#
#                # wrap lines at 72 chars
#                $code =~ s/^(.{72})(.)/$1\n$2/mg;
#
                $xml_writer->characters($code);

#                $xml_writer->endTag('artwork');
#                $xml_writer->endTag('figure');
                $xml_writer->endTag('pre');
                $xml_writer->endTag('div');

            }
        }

        sub header { 

            my ($self, $level, $header) = @_;

            $level += $self->{section_level_offset};
            $header = ::trim($header);

            my $xml_writer = $self->{xml_writer};
            my $visited = $self->{visited};
            my $page = $self->{page};
            my $anchor = ::get_anchor($page, $header);
            my $open_sections = $self->{open_sections};
            my $section_number = $self->{section_number};
            my $section = $self->{section};
           
            if ($section && ($anchor eq ::get_anchor($page, $section))) {
                $self->output_enabled(TRUE);
            } 

            unless ($self->output_enabled) {
                ::trace(sprintf('skipping %s, outside of section %s', $anchor, $section));
                return;
            }

            while(@$open_sections && ($open_sections->[-1] >= $level)) {
#                $xml_writer->endTag('section');
                pop(@$open_sections);
            }
            
            # we only need to write out <section> start/end tags for subsections of $section.
            # The section tags for $section are handled in the parent (wiki_toc_to_html).

            my $omit_section_tag = FALSE;
            if ($section) {
                $omit_section_tag = TRUE if ($anchor eq ::get_anchor($page, $section));
            } else {
                $omit_section_tag = TRUE if $self->{first_section_on_page};
            }

            ::tracef('visiting page section: %s', $anchor); 
            ::tracef('omitting section tag for %s', $anchor) if $omit_section_tag;
            
            unless ($omit_section_tag) {
#                $xml_writer->startTag('section', anchor => $anchor, title => $header);
                $section_number->num_levels($level);
                $section_number->inc();
                my $header_tag = sprintf('h%d', $level + 1);
                $xml_writer->startTag($header_tag, id => $anchor);
                    $xml_writer->characters($section_number->get_section() . ' ' . $header);
                $xml_writer->endTag($header_tag);
                push(@$open_sections, $level);
            }
            
            $visited->{$anchor} = 1;
            $self->{first_section_on_page} = FALSE;

        }

        sub end_document { 
            my $self = shift;
            my $open_sections = $self->{open_sections};
            while (pop(@$open_sections)) {
#                $self->{xml_writer}->endTag('section');
            }
        }

        package __PACKAGE__; 

    };

    die $@ if $@;

    my $markdown = GoogleWiki2Markdown::convert($input);
#    ::tracef('input markdown: \'%s\'', $markdown);
    my $handler = Markdent::Handler::Page->new($xml_writer, $visited, $page, $section_level_offset, $section_number, $reference_pages, $section);
    my $parser = Markdent::Parser->new(handler => $handler);
    $parser->parse(markdown => $markdown);

}

#----------------------------------------------------------------------
# utility subs
#----------------------------------------------------------------------

sub get_anchor {
    my ($page, $section) = @_;
    $page =~ s/ /_/g if $page;
    $section =~ s/ /_/g if $section;
    return "${page}#${section}" if ($page && $section);
    return $page if $page;
    return $section if $section;
    return undef;
}

sub tracef {
    trace(sprintf(shift, @_)) if $trace_opt;
}

sub trace {
    warn sprintf("TRACE => %s\n", shift) if $trace_opt;
}

sub trace_sub_call {
    warn sprintf('%s: (%s)', shift, join(',', @_));
}

sub filename_to_wiki_page {
    my $page = (splitpath($_[0]))[2];
    $page =~ s/.wiki$//;
    return $page;
}

sub wiki_page_to_filename {
    return catfile($_[0], $_[1] . '.wiki');
}

sub trim {
    my $text = shift;
    $text =~ s/^\s*(.*?)\s*$/$1/; 
    return $text;
}

sub trim_quotes {
    my $text = shift;
    $text =~ s/^\s*["'“”]\s*//;
    $text =~ s/\s*["'“”]\s*$//;
    return $text;
}
