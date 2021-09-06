#!/bin/bash

deno run --allow-read --allow-write --allow-net "https://raw.githubusercontent.com/littlelanguages/parspiler-cli/main/mod.ts" kotlin --verbose --directory=src/main/kotlin --package=io.littlelanguages.mil.static src/main/kotlin/io/littlelanguages/mil/static/Grammar.llgd || exit 1

(
  cd ./src/main/c || exit 1
  make || exit 1
)

./gradlew build || exit 1
