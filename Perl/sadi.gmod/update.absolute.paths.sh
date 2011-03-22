#!/bin/sh

if [ $# -lt 2 ]; then
    echo "usage: ./update.absolute.paths.sh <old absolute path> <new absolute path>";
    exit 1;
fi

find . -not -name README -not \( -name .svn -prune \) | perl -ne 'chomp; print "$_\0" if -f' | xargs -0 perl -i -pe "s|$1|$2|g"
