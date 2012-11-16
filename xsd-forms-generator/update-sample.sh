#!/bin/bash
set -e
cp target/generated-webapp/census-form.html src/main/webapp/census.html
cp target/generated-webapp/demo-form.html src/main/webapp/demo.html
cp target/generated-webapp/simple-form.html src/main/webapp/simple.html


