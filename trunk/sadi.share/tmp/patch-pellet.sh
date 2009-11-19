#!/bin/sh
#
# this script must be run from the sadi.share output directory

find org/mindswap/pellet -name *.class | xargs jar uvf ../../sadi.libraries/sadi-0.1pre/pellet.jar
