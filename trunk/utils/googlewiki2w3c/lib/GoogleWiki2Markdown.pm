package GoogleWiki2Markdown;

use strict;
use warnings;

use constant::boolean;
use HTML::Entities;

sub convert {

    $_ = shift;

    # strip comments
    s/<wiki:comment>.*?<\/wiki:comment>//sg;

    # strip pragmas
    s/^#(summary|sidebar).*?\n$//smg;

    # temporarily extract code blocks so their contents aren't modified
    my @code_blocks = ();
    #s/\Q{{{\E[ \t]*(\r?\n)(.*?(\r?\n))[ \t]*\Q}}}\E/push(@code_blocks, $2); '<pre><code><\/code><\/pre>'/emsg;
    s/\Q{{{\E[ \t]*(\r?\n)(.*?(\r?\n))[ \t]*\Q}}}\E/push(@code_blocks, $2); '{{{}}}'/emsg;

    # temporarily extract inline code spans so their contents aren't modified
    my @code_spans = ();
    s/`([^\n`]+?)`/push(@code_spans, $1);'``'/eg;

    # escape numbers at the beginnings of lines that look like numbered list markers
    s/^([ \t]*\d+)(\.)/$1\\./mg;

    # numbered list markup
    s/^[ \t]+\#/1./mg;

    # links
    s/\[\s*([^\]\s]+)\s*([^\]]+)\s*\]/[$2]($1)/g;

    # strip '!'s used to prevent WikiWords from being interpreted as links (e.g. !CatsAndDogs)
    s/([;,\.\s]+)!([A-Z]+[a-z]+[A-Z]+[a-z]*)\b/$1$2/g;

    # header markup
    s/^[ \t]*(=+)[ \t]*([^=]+)[ \t]*(=+)[ \t]*/sprintf('%s %s', '#'x(length($1)), $2)/meg;

    # re-insert code blocks
    #s/\Q{{{\E/$1 . encode_entities(shift(@code_blocks))/eg;
    foreach my $code_block (@code_blocks) {
        #$code_block = encode_entities($code_block);
        $code_block =~ s/^/\t/mg;
    }
    s/\Q{{{}}}\E/shift(@code_blocks)/eg;
    
    # re-insert code spans
    s/``/'`' . encode_entities(shift(@code_spans)) . '`'/eg;

    return $_;

}

1;
