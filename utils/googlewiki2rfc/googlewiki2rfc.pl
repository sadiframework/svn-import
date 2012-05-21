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

#----------------------------------------------------------------------
# constants
#----------------------------------------------------------------------

use constant TEMP_XML_FILE => 'temp.xml';
use constant TEMP_OUTPUT_FILE => 'temp.txt';

#----------------------------------------------------------------------
# parse command line args
#----------------------------------------------------------------------

use constant USAGE => <<'HEREDOC';
Usage: googlewiki2rfc.pl [options] <docname> front.xml middle.wiki [back.wiki] > rfc.txt

<docname> is a document identifier used in the RFC submission / editing process of the 
IETF.  It can be anything, but a typical example is: 'draft-bvandervalk-sadi-01'. 

"front.xml" contains metadata about the document (e.g. authors, abstract) in the format 
used by RFC XML.  front.xml is not a complete XML document on its own.  The XML should not
be surrounded by <front></front> tags as this is added by the script.

"middle.wiki" is a Google Wiki file containing the Table of Contents (in the form of a
bulleted list, possibly nested) for the main part of the document.

"back.wiki" is optional and is a Google Wiki file containing the Table of Contents for 
the end matter of the document (e.g. References).
HEREDOC

my $help_opt = FALSE;
my $trace_opt = FALSE;
my $rfc_xml_filename;
my @reference_files = ();
my $intended_status;

my $getopt_success = GetOptions(
    'trace' => \$trace_opt,
    'xml=s' => \$rfc_xml_filename,
    'references=s' => \@reference_files,
    'help' => \$help_opt,
    'status=s' => \$intended_status,
);

die USAGE."\n" unless $getopt_success;

if ($help_opt) {
    warn USAGE;
    exit 0;
}


die USAGE."\n" unless @ARGV >= 3;

my $doc_name = $ARGV[0];
my $front_file = $ARGV[1];
my $middle_file = $ARGV[2];
my $back_file = $ARGV[3];

$::RD_ERRORS = 1;
$::RD_TRACE = 1 if $trace_opt;
$::RD_HINT = 1 if $trace_opt;

#----------------------------------------------------------------------
# main
#----------------------------------------------------------------------

# temp input/output files, because xml2rfc.tcl doesn't do STDIN/STDOUT

my $tempdir = tempdir(CLEANUP => 1);
$rfc_xml_filename ||= catfile($tempdir, TEMP_XML_FILE);
my $rfc_xml_file = IO::File->new($rfc_xml_filename, '>');
my $temp_output_filename = catfile($tempdir, TEMP_OUTPUT_FILE);

# translate Google Wiki doc hierarchy => RFC XML

my $xml_writer = new XML::Writer(
    OUTPUT => $rfc_xml_file, 
    UNSAFE => 1,               # allows us to insert front.xml verbatim
#    DATA_MODE => 1,            # newlines after tags
    DATA_INDENT => 2,          # proper indentation
);

$xml_writer->xmlDecl();
$xml_writer->doctype('rfc', undef, 'rfc2629.dtd');
$xml_writer->pi('rfc', 'toc="yes"');       # automatically build a table of contents 
$xml_writer->pi('rfc', 'symrefs="yes"');   # use anchors instead of numbers for references
$xml_writer->pi('rfc', 'compact="yes"');   # conserve vertical whitespace 
$xml_writer->pi('rfc', 'subcompact="no"'); # but keep a blank line between list items 
$xml_writer->startTag('rfc', ipr => 'trust200902', docName => "$doc_name");

$xml_writer->startTag('front');
open(my $front_fh, '<', $front_file);
$xml_writer->raw(join('',<$front_fh>));
close($front_fh);
$xml_writer->endTag('front');

$xml_writer->startTag('middle');
#wiki_toc_to_xml($xml_writer, $middle_file) if $middle_file;
wiki_toc_to_xml($xml_writer, $middle_file, \@reference_files);
$xml_writer->endTag('middle');

if ($back_file || @reference_files) {
    $xml_writer->startTag('back');
    wiki_references_to_xml($xml_writer, $_) foreach @reference_files;
    wiki_toc_to_xml($xml_writer, $back_file) if $back_file;
    $xml_writer->endTag('back');
}

$xml_writer->endTag('rfc');
$xml_writer->end();
$rfc_xml_file->close();

if ($trace_opt) {
    warn "XML output:\n";
    open (my $xml_fh, '<', $rfc_xml_filename);
    warn $_ while (<$xml_fh>);
    close($xml_fh);
    warn "\n";
}

# translate RFC XML => RFC text file

system(
    'tclsh', 
    'xml2rfc.tcl', 
    'xml2rfc', 
    $rfc_xml_filename, 
    $temp_output_filename
);

# send output to STDOUT

my $output = read_file($temp_output_filename);
if ($intended_status) {
    my $status_string = "Intended status: $intended_status";
    my $length = length($status_string);
    $output =~ s/^ {$length}/$status_string/m;
}
print $output;

#----------------------------------------------------------------------
# callbacks from wiki parser (generates RFC XML)
#----------------------------------------------------------------------

sub wiki_toc_to_xml {

    my ($xml_writer, $wiki_file, $reference_files) = @_;
    
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

        with 'Markdent::Role::EventsAsMethods';

        our $AUTOLOAD; 
        sub AUTOLOAD { 
#            ::trace(sprintf('unhandled event %s with args (%s)', $AUTOLOAD, join(', ', @_))); 
        }

        sub new {
            my ($class, $xml_writer, $working_dir, $page, $reference_pages) = @_;
            my $self = {
                xml_writer => $xml_writer,
                working_dir => $working_dir,
                list_level => 0,
                visited => {$page => 1},      # visited pages or sections of pages
                reference_pages => $reference_pages,
                skip_item => FALSE,
                in_link => FALSE,
            };
            return bless($self, $class);
        }

        sub start_unordered_list { 
            $_[0]->{list_level}++; 
        }

        sub end_unordered_list { 
            $_[0]->{list_level}--; 
        }

        sub end_list_item {
            my $self = shift;
            $self->{xml_writer}->endTag('section') unless $self->{skip_item};
            $self->{skip_item} = FALSE;
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
            my $visited = $self->{visited};
            my $text = ::trim($_[1]);
            
            # ignore whitespace text events (e.g. newlines at end of list items)
            return unless $text; 

            my $in_link = $self->{in_link};
            my $anchor = $in_link ? $self->{link_uri}->as_string : main::get_anchor('',$text);

#            ::tracef('anchor: \'%s\'', $anchor);
#            ::tracef('visited? %s', ($visited->{$anchor} ? 'yes' : 'no'));
#            ::tracef('visited: (%s)', join(',', %$visited));

            if ($visited->{$anchor}) {
                $self->{skip_item} = TRUE;
                ::tracef('skipping TOC entry %s, page/section already visited', $anchor);
                return;
            }

            $xml_writer->startTag('section', anchor => $anchor, title => $text);
           
            if ($in_link) {
                ::trace("following TOC link: $_{uri}");
                ::wiki_to_xml(
                    $xml_writer, 
                    $self->{working_dir},
                    $self->{visited}, 
                    $self->{link_uri}->path, # page
                    ($self->{list_level} - 1),
                    $self->{reference_pages},
                    $self->{link_uri}->fragment # section (optional)
                );
            }

        }

        package __PACKAGE__; 

    };

    die $@ if $@;

    my $markdown = GoogleWiki2Markdown::convert($input);
#    printf("markdown:\n%s\n", $markdown);
    my $handler = Markdent::Handler::TOC->new($xml_writer, $working_dir, $page, \@reference_pages);
    my $parser = Markdent::Parser->new(handler => $handler);
    $parser->parse(markdown => $markdown);

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
        use Data::Dumper;

        with 'Markdent::Role::EventsAsMethods';

        our $AUTOLOAD; 
        sub AUTOLOAD { 
            ::trace(sprintf('unhandled event %s with args (%s)', $AUTOLOAD, join(', ', @_))); 
        }

        sub new {
            my ($class, $xml_writer, $working_dir, $page) = @_;
            my $self = {
                xml_writer => $xml_writer,
                working_dir => $working_dir,
                page => $page,
                in_header => FALSE,
                header_count => 0,
                # Indicates which part of a reference a piece of text belongs to (e.g. an author string, document title, etc.)
                # Item types are indicated in the GoogleWiki source by span tags with an appropriate
                # class attribute, e.g. <span class="title">Key words for use in RFCs to Indicate Requirement Levels</span>
                current_item_type => [],  
                current_citation  => {},
            };
            return bless($self, $class);
        }

        sub start_html_tag {
            my ($self, %args) = @_;
            if (%args && $args{attributes}) {
                push(@{$self->{current_item_type}}, $args{attributes}->{class});
            }
        }
        
        sub end_html_tag {
            pop(@{$_[0]->{current_item_type}});  # note: popping on empty array returns undef
        }

        sub end_document {
            my $self = shift;
            $self->output_citation();  # output the last citation
            $self->{xml_writer}->endTag('references');
        }

        sub start_header  {
            ::trace('start_header');
            my $self = shift; 
            $self->{in_header} = TRUE;
            $self->{header_count}++;
        }

        sub end_header {
            ::trace('end_header');
            $_[0]->{in_header} = FALSE;
        }

        sub text {
            my $self = $_[0];
            my $text = main::trim($_[2]);
            my $xml_writer = $self->{xml_writer};
            my $header_count = $self->{header_count};
            if ($self->{in_header}) {
                ::tracef('header text event (%d): %s', $header_count, $text);
                $xml_writer->startTag('references', title => $text) if ($header_count == 1);
                if ($header_count >= 2) {
                    $self->output_citation() if $header_count >= 3;  # output RFC XML for previous citation
                    $self->{current_citation} = {};
                    $self->{current_citation}->{anchor} = main::get_anchor(undef, $text);
                }
            } else {
                ::tracef('non-header text event: %s', $text);
                unless ($header_count == 1) {

                    # Markdent assumes ISO-8859-1, but we need UTF-8 here 
                    # (wiki citations contain Unicode quote chars)
                    my $text = decode('utf8', encode('iso-8859-1', $text));
                    
                    my $citation = $self->{current_citation};
                    my $item_type = $self->{current_item_type}->[-1];
                    if ($item_type) {

                        # NOTE: Markdent visits nested HTML tags in a very strange
                        # order (I don't know why).  For example the following HTML
                        #
                        #    <span class="outer">
                        #       <span class="inner1">A</span>
                        #       <span class="inner2">B</span>
                        #       <span class="inner3">C</span>
                        #    </span>
                        #
                        # generates start_html_tag events in the order: inner1, inner2, outer, inner3
                        #
                        # It seems that the inner tags are always visited in the correct order, 
                        # so I've based my code on that, and I avoid relying on the outer level tags. 
                        # I am not sure what would happen with a third level of nesting.

                        ::tracef("item_type: '%s'\n", $item_type);

                        my %child_key = (
                            'author' => ['firstname', 'lastname', 'organization'],
                            'docid'   => ['docseries', 'docnumber'],
                            'date'    => ['day', 'month', 'year'],
                        );

                        # invert child_key to make parent_key
                        my %parent_key = (); 
                        while (my ($k, $v) = each(%child_key)) {
                            foreach my $i (@$v) {
                                $parent_key{$i} = $k;
                            }
                        }

                        my $parent = $parent_key{$item_type};

                        if ($parent) {
                            # 'date' is treated differently than 'author' and 'docid' because
                            # there can only be one instance of date per citation.
                            if ($parent eq 'date') {
                                $citation->{$parent} = {} unless defined($citation->{date});
                                $citation->{$parent}->{$item_type} = $text;
                            } else {  # 'author' or 'docid'
                                $citation->{$parent} = [ {} ] unless $citation->{$parent};
                                push(@{$citation->{$parent}}, {}) if defined($citation->{$parent}->[-1]->{$item_type});
                                $citation->{$parent}->[-1]->{$item_type} = $text;
                            }
                        } elsif (!grep($item_type eq $_, values(%parent_key))) {  # simple field, no parent tags
                            $citation->{$item_type} = $text;
                        }

                    }
                }
            }
        }

        sub output_citation {

            my ($self) = @_; 
            my $xml_writer = $self->{xml_writer};
            my $citation = $self->{current_citation};

            # make sure required parts of the citation are present. 
            
            unless ($citation->{anchor}) {
                warn "ERROR: skipping citation, no anchor found\n";
                return;
            }
            
            unless ($citation->{title}) {
                warn sprintf("ERROR: skipping citation [%s], no title found\n", $citation->{anchor});
                return;
            }
            
            unless ($citation->{author} && @{$citation->{author}}) {
                warn sprintf("ERROR: skipping citation [%s], no authors found\n", $citation->{anchor});
                return;
            }

            # output the RFC XML
            
            $xml_writer->startTag('reference', anchor => $citation->{anchor}, title => $citation->{anchor});    
            $xml_writer->startTag('front');
            $xml_writer->startTag('title');
            $xml_writer->characters($citation->{title});
            $xml_writer->endTag('title');
            my @authors = @{$citation->{author}};
            while (@authors) {
                my $author = shift(@authors);
                my $first = $author->{firstname};
                my $last = $author->{lastname};
                my $organization = $author->{organization};
                if (!$first || !$last) {
                    warn "WARNING: omitting author, missing firstname or lastname\n";
                    next;
                }
                $xml_writer->startTag(
                    'author', 
                    initials => $first,
                    surname => $last,
                    fullname => "$first $last"
                );
                if ($organization) {
                    $xml_writer->startTag('organization');
                    $xml_writer->characters($organization);
                    $xml_writer->endTag('organization');
                }
                $xml_writer->endTag('author');
            }
            
            my $date = $citation->{date};
            if ($date) {
                # require at least a year to be specified
                if (!$date->{year}) {
                    warn "WARN: omitting date from citation, no year found\n";
                } else {
                    my @date_attribs = ();
                    push(@date_attribs, day => $date->{day}) if $date->{day};
                    push(@date_attribs, month => $date->{month}) if $date->{month};
                    $xml_writer->emptyTag('date', year => $date->{year}, @date_attribs);
                }
            }
            my $area = $citation->{area};
            if ($area) {
                $xml_writer->startTag('area');
                $xml_writer->characters($area);
                $xml_writer->endTag('area');
            }
            $xml_writer->endTag('front');
            if ($citation->{docid}) {
                my @docids = @{$citation->{docid}};
                while (@docids) {
                    my $docid = shift(@docids);
                    my $docseries = $docid->{docseries};
                    my $docnumber = $docid->{docnumber};
                    unless ($docseries && $docnumber) {
                        warn "WARN: omitting document ID (e.g. 'RFC 1234') from citation, missing doc series  or doc number\n";
                        next;
                    }
                    $xml_writer->emptyTag('seriesInfo', name => $docseries, value => $docnumber);
                }
            }
            $xml_writer->endTag('reference');
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
    
    my ($xml_writer, $working_dir, $visited, $page, $section_level_offset, $reference_pages, $section) = @_;

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
            my ($class, $xml_writer, $visited, $page, $section_level_offset, $reference_pages, $section) = @_;
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
            $_[0]->{xml_writer}->startTag('t');
            $_[0]->{xml_writer}->startTag('list', style => 'symbols');
        }

        sub end_unordered_list {
            $_[0]->{xml_writer}->endTag('list');
            $_[0]->{xml_writer}->endTag('t');
        }

        sub start_ordered_list {
            $_[0]->{xml_writer}->startTag('t');
            $_[0]->{xml_writer}->startTag('list', style => 'numbers');
        }

        sub end_ordered_list {
            $_[0]->{xml_writer}->endTag('list');
            $_[0]->{xml_writer}->endTag('t');
        }

        sub start_list_item {
            $_[0]->{xml_writer}->startTag('t');
        }

        sub end_list_item {
            $_[0]->{xml_writer}->endTag('t');
        }

        sub start_paragraph {
            my $self = $_[0];
            $self->{xml_writer}->startTag('t') unless $self->{in_inline_code};
        }

        sub end_paragraph {
            my $self = $_[0];
            $self->{xml_writer}->endTag('t') unless $self->{in_inline_code};
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

                $xml_writer->startTag('figure');
                $xml_writer->startTag('artwork');

                # separate code from preceding text/figure/list with blank line
                $code =~ s/^(.*\S.*)/\n$1/;

                # indent 6 spaces
                $code =~ s/^/' 'x6/emg;

                # wrap lines at 72 chars
                $code =~ s/^(.{72})(.)/$1\n$2/mg;

                $xml_writer->characters($code);

                $xml_writer->endTag('artwork');
                $xml_writer->endTag('figure');

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
            my $section = $self->{section};
           
            if ($section && ($anchor eq ::get_anchor($page, $section))) {
                $self->output_enabled(TRUE);
            } 

            unless ($self->output_enabled) {
                ::trace(sprintf('skipping %s, outside of section %s', $anchor, $section));
                return;
            }

            while(@$open_sections && ($open_sections->[-1] >= $level)) {
                $xml_writer->endTag('section');
                pop(@$open_sections);
            }
            
            # we only need to write out <section> start/end tags for subsections of $section.
            # The section tags for $section are handled in the parent (wiki_toc_to_xml).

            my $omit_section_tag = FALSE;
            if ($section) {
                $omit_section_tag = TRUE if ($anchor eq ::get_anchor($page, $section));
            } else {
                $omit_section_tag = TRUE if $self->{first_section_on_page};
            }

            ::tracef('visiting page section: %s', $anchor); 
            ::tracef('omitting section tag for %s', $anchor) if $omit_section_tag;
            
            unless ($omit_section_tag) {
                $xml_writer->startTag('section', anchor => $anchor, title => $header);
                push(@$open_sections, $level);
            }
            
            $visited->{$anchor} = 1;
            $self->{first_section_on_page} = FALSE;


        }

        sub end_document { 
            my $self = shift;
            my $open_sections = $self->{open_sections};
            while (pop(@$open_sections)) {
                $self->{xml_writer}->endTag('section');
            }
        }

        package __PACKAGE__; 

    };

    die $@ if $@;

    my $markdown = GoogleWiki2Markdown::convert($input);
#    ::tracef('input markdown: \'%s\'', $markdown);
    my $handler = Markdent::Handler::Page->new($xml_writer, $visited, $page, $section_level_offset, $reference_pages, $section);
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
