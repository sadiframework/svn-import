package Utils::Text;

use Text::Wrap;

require Exporter;

use vars qw(@ISA @EXPORT_OK);
@ISA = qw(Exporter);
@EXPORT_OK = qw(wrap_text);

sub wrap_text 
{
    wrap('', '', @_);
}

1;

