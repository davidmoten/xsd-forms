#!/bin/bash
set -e
rm -rf src/docs/showcase
mkdir src/docs/showcase
cp -r target/generated-webapp/* src/docs/showcase

