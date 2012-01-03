package GoogleWiki2Markdown;

use strict;
use warnings;

use constant::boolean;

sub convert {

    $_ = shift;

    # strip comments
    s/<wiki:comment>.*?<\/wiki:comment>//sg;

    # strip pragmas
    s/^#(summary|sidebar).*?\n$//smg;

#    warn "before extract: $_\n";

    # temporarily extract code blocks so their contents aren't modified
    my @code_blocks = ();
    pos($_) = undef;
    while(s/\G(.*)[ \t]*\Q{{{\E[ \t]*(\r?\n)(.*?(\r?\n))[ \t]*\Q}}}\E/$1<pre><code><\/code><\/pre>/msg) {
        push(@code_blocks, $3);
    }

#    warn "after extract: $_\n";

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

#    warn sprintf("code blocks:\n%s\n", join("\n----------\n", @code_blocks));

    # re-insert code blocks
    s/(<pre><code>)/$1 . shift(@{code_blocks})/eg;

    return $_;
}

1;
