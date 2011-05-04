#!/bin/sh
# Ctrl-] breaks in gvim, if any of the lines in the tag file are longer than 512 bytes
find . -iname '*.pm' -print0 | xargs -0 ctags -f - | perl -ple 'next if length > 256' > tags
