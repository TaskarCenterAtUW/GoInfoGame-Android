#!/usr/bin/env bash
# Prints "<tests> <failed> <skipped>" over all JUnit XML in an extracted report bundle - from both
# Gradle (test-results) and AGP (androidTest-results): one <testsuite> per file with
# tests/failures/errors/skipped attributes. Failed counts failures and errors.
#
# Usage: junit-totals.sh <extracted bundle dir>
set -euo pipefail

# the AGP file names contain spaces ("...(AVD) - 11-_app-.xml"), so the files are read through
# find -exec instead of being passed around as a word list
find "$1" -path '*-results/*' -name '*.xml' -exec cat {} + | awk '
  function attr(name) { return match($0, name "=\"[0-9]+\"") ? substr($0, RSTART + length(name) + 2, RLENGTH - length(name) - 3) + 0 : 0 }
  /<testsuite[ >]/ { t += attr("tests"); f += attr("failures") + attr("errors"); s += attr("skipped") }
  END { print t + 0, f + 0, s + 0 }'
