#!/bin/bash
set -e
rm -rf src/docs/showcase
mkdir src/docs/showcase
cp -r target/generated-webapp/* src/docs/showcase
mvn scala:doc
rm -rf src/docs/scaladocs
mkdir src/docs/scaladocs
cp -r target/site/scaladocs/* src/docs/scaladocs

