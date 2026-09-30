#!/usr/bin/env bash
# Attaches a CI report bundle to the "ci-reports" pre-release and links it from the job's summary
# page, then deletes that release's bundles older than RETENTION_DAYS. Used instead of
# actions/upload-artifact, whose very first request (CreateArtifact) always timed out from the
# self-hosted runner - release assets go through uploads.github.com instead, don't count against
# the artifact storage quota, and don't grow the git repo (the release only holds files).
#
# Needs the gh CLI on the runner and GH_TOKEN with contents: write (to create the release/tag and
# upload/delete its assets).
#
# Usage: publish-reports.sh <bundle file>
set -euo pipefail

file="$1"
tag="ci-reports"
retention_days=14

command -v gh >/dev/null || { echo "::error::gh CLI not found on this runner (brew install gh)"; exit 1; }

# unit-test.yml and android-test.yml run at the same time, so both may try to create the release on
# the first run - if the create fails, the other job has won the race, as long as it exists now
if ! gh release view "$tag" >/dev/null 2>&1; then
  gh release create "$tag" --prerelease --target "$GITHUB_SHA" --title "CI reports" \
    --notes "Test and coverage report bundles from CI runs, linked from each run's summary page. Bundles older than $retention_days days are deleted automatically." \
    || gh release view "$tag" >/dev/null
fi

# run id + attempt in the name, since all runs share this one release (a re-run gets its own file
# instead of replacing the reports of the attempt before it)
name="${GITHUB_RUN_ID}-${GITHUB_RUN_ATTEMPT}-$(basename "$file")"
staged="$RUNNER_TEMP/$name"
cp "$file" "$staged"
gh release upload "$tag" "$staged" --clobber
rm -f "$staged"

url="$GITHUB_SERVER_URL/$GITHUB_REPOSITORY/releases/download/$tag/$name"
{
  echo "## Reports: $(basename "$file")"
  echo "[Download $name]($url) - attached to the [$tag]($GITHUB_SERVER_URL/$GITHUB_REPOSITORY/releases/tag/$tag) pre-release, kept for $retention_days days"
} >> "$GITHUB_STEP_SUMMARY"

# BSD date (macOS runner) first, GNU date as a fallback
cutoff=$(date -u -v-"${retention_days}"d +%Y-%m-%dT%H:%M:%SZ 2>/dev/null \
  || date -u -d "$retention_days days ago" +%Y-%m-%dT%H:%M:%SZ)
gh api --paginate "repos/$GITHUB_REPOSITORY/releases/tags/$tag" \
  --jq ".assets[] | select(.created_at < \"$cutoff\") | .id" |
  while read -r id; do
    gh api -X DELETE "repos/$GITHUB_REPOSITORY/releases/assets/$id" || echo "::warning::couldn't delete old asset $id"
  done
