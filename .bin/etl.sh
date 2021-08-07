#!/bin/bash

# The etl is the Edit-Test-Loop - a script that continuously monitors whether any files have changed and,
# if there are changes, will rebuild the application and run all of the tests.

if [ "`which entr`" == "" ]
then
    echo "error: entr needs to be installed"
    exit 1
fi

find . | grep -e ".llld$" -e ".kt$" -e ".yaml$" | entr -s .bin/test.sh
