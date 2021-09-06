#!/bin/bash

.bin/build.sh && ./gradlew distTar && tar -xvf ./build/distributions/ll-mini-ilisp-kotlin-llvm.tar

