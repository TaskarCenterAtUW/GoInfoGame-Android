#!/usr/bin/env bash
# Looks on the ci-reports pre-release for the newest bundle of the given suite that was made from
# exactly the checked-out code (same git tree, see publish-reports.sh) - e.g. by the PR run before
# merging, or by an earlier release report run whose summary failed. If it passed (tests ran, none
# failed), attaches it to the version's pre-release under its plain name, as if this run had
# tested it, and prints the id of the run that made it. Prints nothing if there's none to reuse,
# then the suite has to be tested again.
#
# Only the newest bundle counts: if the newest run on that code failed, an older passing one
# doesn't make it verified.
#
# Needs the gh CLI on the runner and GH_TOKEN with contents: write.
#
# Usage: reuse-verified-reports.sh <release tag> <bundle name, e.g. unit-test-reports.tar.gz>
set -euo pipefail

tag="$1"
bundle="$2"
tree=$(git rev-parse 'HEAD^{tree}')
suffix="-tree-$tree-$bundle"

release_id=$(gh api "repos/$GITHUB_REPOSITORY/releases/tags/ci-reports" --jq .id 2>/dev/null) || {
  echo "No ci-reports pre-release, testing $bundle" >&2
  exit 0
}
name=$(gh api --paginate "repos/$GITHUB_REPOSITORY/releases/$release_id/assets" \
  --jq ".[] | select(.name | endswith(\"$suffix\")) | [.created_at, .name] | @tsv" |
  sort | tail -1 | cut -f2)
if [ -z "$name" ]; then
  echo "No $bundle tested on tree $tree, testing it" >&2
  exit 0
fi

work="$RUNNER_TEMP/reuse-$bundle"
rm -rf "$work"
mkdir -p "$work/extract"
gh release download ci-reports --pattern "$name" --dir "$work"
tar -xzf "$work/$name" -C "$work/extract"
read -r tests failed skipped < <(bash "$(dirname "$0")/junit-totals.sh" "$work/extract")
if [ "$tests" -eq 0 ] || [ "$failed" -gt 0 ]; then
  echo "$name has $failed of $tests tests failed, testing $bundle again" >&2
  exit 0
fi

cp "$work/$name" "$work/$bundle"
gh release upload "$tag" "$work/$bundle" --clobber
rm -rf "$work"
echo "Reusing $name: $tests tests, none failed, $skipped skipped" >&2
# <run id>-<attempt>-tree-...
echo "${name%%-*}"
