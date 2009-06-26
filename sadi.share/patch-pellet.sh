#!/bin/sh

find bin/org/mindswap/pellet -name *.class | xargs jar uvf ../sadi.libraries/sadi-0.1pre/pellet.jar
