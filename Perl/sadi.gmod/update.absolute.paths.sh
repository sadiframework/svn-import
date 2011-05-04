#!/bin/sh

if [ $# -lt 2 ]; then
    echo "usage: ./update.absolute.paths.sh <old absolute path> <new absolute path>";
    exit 1;
fi

perl -i -pe "s|$1|$2|g" log4perl.properties sadi-services.cfg cgi/*
