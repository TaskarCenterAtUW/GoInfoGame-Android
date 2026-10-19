#!/usr/bin/env bash
# Downloads the report bundles attached to a version's test-report pre-release, totals the JUnit
# results (and unit test line coverage) per suite, and writes them as the release notes and the
# job summary. Used by release-test-report.yml after the test jobs have uploaded their bundles.
#
# Needs the gh CLI on the runner and GH_TOKEN with contents: write.
#
# Usage: release-report-summary.sh <release tag> <version>
set -euo pipefail

tag="$1"
version="$2"
work="$RUNNER_TEMP/release-report"
notes="$work/notes.md"
rm -rf "$work"
mkdir -p "$work/bundles"

gh release download "$tag" --pattern '*.tar.gz' --dir "$work/bundles" || true

run_url="$GITHUB_SERVER_URL/$GITHUB_REPOSITORY/actions/runs/$GITHUB_RUN_ID"
{
  echo "# $version release test reports summary"
  echo
  echo "- Branch: \`$GITHUB_REF_NAME\`"
  echo "- Commit: [\`${GITHUB_SHA:0:9}\`]($GITHUB_SERVER_URL/$GITHUB_REPOSITORY/commit/$GITHUB_SHA)"
  echo "- Run: [$GITHUB_RUN_ID]($run_url)"
  echo "- Date: $(date -u +%Y-%m-%d) (UTC)"
  echo
  echo "| Suite | Result | Tests | Passed | Failed | Skipped | Report |"
  echo "|---|---|---:|---:|---:|---:|---|"
} > "$notes"

failures="$work/failures.md"
: > "$failures"
coverage=""

for suite in "Unit tests:unit-test-reports.tar.gz" \
             "Instrumentation tests (API 30, small screen):android-test-reports-api30.tar.gz" \
             "Instrumentation tests (API 34, Pixel 6):android-test-reports-api34.tar.gz"; do
  label="${suite%%:*}"
  bundle="${suite#*:}"
  if [ ! -f "$work/bundles/$bundle" ]; then
    echo "| $label | ⚠️ no report | | | | | |" >> "$notes"
    continue
  fi
  dir="$work/extract/${bundle%.tar.gz}"
  mkdir -p "$dir"
  tar -xzf "$work/bundles/$bundle" -C "$dir"

  # JUnit XML from both Gradle (test-results) and AGP (androidTest-results): one <testsuite> per
  # file with tests/failures/errors/skipped attributes, failing <testcase>s followed by <failure>
  # or <error>
  xmls=$(find "$dir" -path '*-results/*' -name '*.xml' | sort)
  if [ -z "$xmls" ]; then
    echo "| $label | ⚠️ no results | | | | | |" >> "$notes"
    continue
  fi
  # shellcheck disable=SC2086
  read -r tests failed skipped < <(awk '
    function attr(name,   m) { return match($0, name "=\"[0-9]+\"") ? substr($0, RSTART + length(name) + 2, RLENGTH - length(name) - 3) + 0 : 0 }
    /<testsuite[ >]/ { t += attr("tests"); f += attr("failures") + attr("errors"); s += attr("skipped") }
    END { print t + 0, f + 0, s + 0 }' $xmls)
  passed=$((tests - failed - skipped))
  result="✅ passed"
  [ "$failed" -gt 0 ] && result="❌ failed"
  url="$GITHUB_SERVER_URL/$GITHUB_REPOSITORY/releases/download/$tag/$bundle"
  echo "| $label | $result | $tests | $passed | $failed | $skipped | [$bundle]($url) |" >> "$notes"

  if [ "$failed" -gt 0 ]; then
    echo "**$label**" >> "$failures"
    # shellcheck disable=SC2086
    awk '
      function attr(name,   m) { return match($0, name "=\"[^\"]*\"") ? substr($0, RSTART + length(name) + 2, RLENGTH - length(name) - 3) : "" }
      /<testcase[ >]/ { tc = attr("classname") "." attr("name") }
      /<failure[ >]|<error[ >]/ { if (tc != "") print "- `" tc "`"; tc = "" }' $xmls | sort -u >> "$failures"
    echo >> "$failures"
  fi

  kover="$dir/app/build/reports/kover/report.xml"
  if [ -f "$kover" ]; then
    line_counter=$(grep -o '<counter type="LINE" missed="[0-9]*" covered="[0-9]*"/>' "$kover" | tail -1)
    missed=$(echo "$line_counter" | grep -o 'missed="[0-9]*"' | grep -o '[0-9]*')
    covered=$(echo "$line_counter" | grep -o 'covered="[0-9]*"' | grep -o '[0-9]*')
    total=$((missed + covered))
    if [ "$total" -gt 0 ]; then
      coverage="**$(awk "BEGIN { printf \"%.1f\", ($covered/$total)*100 }")%** ($covered/$total lines)"
    fi
  fi
done

{
  if [ -n "$coverage" ]; then
    echo
    echo "Unit test line coverage: $coverage"
  fi
  if [ -s "$failures" ]; then
    echo
    echo "## Failed tests"
    echo
    cat "$failures"
  fi
  echo
  echo "Each bundle holds the HTML report and JUnit XML; the instrumentation bundles also have the screen recording and per-test screenshots."
} >> "$notes"

gh release edit "$tag" --notes-file "$notes"
cat "$notes" >> "$GITHUB_STEP_SUMMARY"
