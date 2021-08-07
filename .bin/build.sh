#!/bin/bash

deno run --allow-read --allow-write --allow-net "https://raw.githubusercontent.com/littlelanguages/parspiler-cli/main/mod.ts" kotlin --verbose --directory=src/main/kotlin --package=io.littlelanguages.p0.static src/main/kotlin/io/littlelanguages/p0/static/Grammar.llgd

./gradlew build