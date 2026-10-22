#!/usr/bin/env bash
# Downloads the report bundles attached to a version's test-report pre-release, totals the JUnit
# results (and unit test line coverage) per suite, and writes them as the release notes and the
# job summary. Used by release-test-report.yml after the test jobs have uploaded their bundles.
#
# REUSED_UNIT, REUSED_API30 and REUSED_API34 hold the id of the earlier run whose bundle was reused
# for that suite (see reuse-verified-reports.sh), empty if this run tested it.
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
  echo "| Suite | Result | Tests | Passed | Failed | Skipped | Tested in | Report |"
  echo "|---|---|---:|---:|---:|---:|---|---|"
} > "$notes"

failures="$work/failures.md"
: > "$failures"
coverage=""

for suite in "${REUSED_UNIT:-}:Unit tests:unit-test-reports.tar.gz" \
             "${REUSED_API30:-}:Instrumentation tests (API 30, small screen):android-test-reports-api30.tar.gz" \
             "${REUSED_API34:-}:Instrumentation tests (API 34, Pixel 6):android-test-reports-api34.tar.gz"; do
  reused_from="${suite%%:*}"
  rest="${suite#*:}"
  label="${rest%%:*}"
  bundle="${rest#*:}"
  tested_in="this run"
  if [ -n "$reused_from" ]; then
    tested_in="[run $reused_from]($GITHUB_SERVER_URL/$GITHUB_REPOSITORY/actions/runs/$reused_from), same code"
  fi
  if [ ! -f "$work/bundles/$bundle" ]; then
    echo "| $label | ⚠️ no report | | | | | $tested_in | |" >> "$notes"
    continue
  fi
  dir="$work/extract/${bundle%.tar.gz}"
  mkdir -p "$dir"
  tar -xzf "$work/bundles/$bundle" -C "$dir"

  if [ -z "$(find "$dir" -path '*-results/*' -name '*.xml')" ]; then
    echo "| $label | ⚠️ no results | | | | | $tested_in | |" >> "$notes"
    continue
  fi
  read -r tests failed skipped < <(bash "$(dirname "$0")/junit-totals.sh" "$dir")
  passed=$((tests - failed - skipped))
  result="✅ passed"
  [ "$failed" -gt 0 ] && result="❌ failed"
  url="$GITHUB_SERVER_URL/$GITHUB_REPOSITORY/releases/download/$tag/$bundle"
  echo "| $label | $result | $tests | $passed | $failed | $skipped | $tested_in | [$bundle]($url) |" >> "$notes"

  if [ "$failed" -gt 0 ]; then
    echo "**$label**" >> "$failures"
    # failing <testcase>s are followed by <failure> or <error>; the AGP file names contain spaces,
    # see junit-totals.sh
    find "$dir" -path '*-results/*' -name '*.xml' -exec cat {} + | awk '
      function attr(name,   m) { return match($0, name "=\"[^\"]*\"") ? substr($0, RSTART + length(name) + 2, RLENGTH - length(name) - 3) : "" }
      /<testcase[ >]/ { tc = attr("classname") "." attr("name") }
      /<failure[ >]|<error[ >]/ { if (tc != "") print "- `" tc "`"; tc = "" }' | sort -u >> "$failures"
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
