#!/usr/bin/env bash
set -e

# tests
cat app/build/reports/tests/testDebugUnitTest/index.html
cat auth/build/reports/tests/testDebugUnitTest/index.html
cat database/build/reports/tests/testDebugUnitTest/index.html
cat storage/build/reports/tests/testDebugUnitTest/index.html

# app
cat app/build/reports/checkstyle.html
cat app/build/reports/lint-results.xml
cat app/build/reports/lint-results.html
cat app/build/reports/findbugs.html
cat app/build/reports/pmd.html