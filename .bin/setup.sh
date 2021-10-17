#!/bin/bash

if [ "`uname`" == "Darwin" ]
then
    echo Setting up Mac

    echo Using brew to install autoconf and automake
    brew install autoconf automake

    if [ -d "./bdwgc" ]
    then
        echo "./bdwgc exists and is a directory - assuming then all is in order and doing nothing"
        echo "If you need to redo then delete this directory and rerun this script"
    else
        echo "Getting bdwgc and building"
        (
            git clone git://github.com/ivmai/bdwgc.git
            cd bdwgc
            git clone git://github.com/ivmai/libatomic_ops.git
            ./autogen.sh
            ./configure
            make -j
            make check
            make -f Makefile.direct
        )
    fi
else
    echo "Exiting: uname == `uname`: Unable to setup"
    echo "Winging it..."

    (
        git clone git://github.com/ivmai/bdwgc.git
        cd bdwgc
        git clone git://github.com/ivmai/libatomic_ops.git
        ./autogen.sh
        ./configure
        make -j
        make check
        make -f Makefile.direct
    )
    # exit 1
fi