package Vocab::MimeTypes;

require Exporter;
our @ISA = qw(Exporter);
our @EXPORT_OK = qw(
    @n3_mime_types
    @rdfxml_mime_types
    @legal_mime_types
);

our @n3_mime_types = (
    'text/rdf+n3',
    'text/n3',
    'application/x-turtle',
);

our @rdfxml_mime_types = (
    'application/rdf+xml'
);

our @legal_mime_types = (@n3_mime_types, @rdfxml_mime_types);

1;
