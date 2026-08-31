#!/usr/bin/env bash
# Copyright (c) 2026 Miguel Guerra
# SPDX-License-Identifier: MIT
#
# Bumps Remarkable version for CI releases:
#   1.0.0 -> 1.0.1 -> … -> 1.0.9 -> 1.1.0
# Codename advances when major.minor changes (e.g. 1.0.x -> 1.1.0).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PROPS="$ROOT/gradle.properties"
NAMES="$ROOT/meta/codenames.txt"
OUT_DIR="${1:-$ROOT/dist}"
mkdir -p "$OUT_DIR"

get_prop() {
  local key="$1"
  grep -E "^${key}=" "$PROPS" | head -n1 | cut -d= -f2- | tr -d '\r'
}

set_prop() {
  local key="$1"
  local value="$2"
  local tmp
  tmp="$(mktemp)"
  awk -v k="$key" -v v="$value" '
    BEGIN { done=0 }
    $0 ~ "^"k"=" { print k"="v; done=1; next }
    { print }
    END { if (!done) print k"="v }
  ' "$PROPS" > "$tmp"
  mv "$tmp" "$PROPS"
}

code="$(get_prop REMARKABLE_VERSION_CODE)"
name="$(get_prop REMARKABLE_VERSION_NAME)"
codename="$(get_prop REMARKABLE_VERSION_CODENAME)"

IFS=. read -r major minor patch <<<"$name"
major="${major:-1}"
minor="${minor:-0}"
patch="${patch:-0}"

prev_name="$name"
prev_codename="$codename"
prev_tag="v${prev_name}"

patch=$((patch + 1))
codename_changed=0
if (( patch > 9 )); then
  patch=0
  minor=$((minor + 1))
  codename_changed=1
fi
if (( minor > 9 )); then
  minor=0
  major=$((major + 1))
  codename_changed=1
fi

code=$((code + 1))
name="${major}.${minor}.${patch}"

if (( codename_changed == 1 )); then
  mapfile -t names < <(grep -vE '^\s*$' "$NAMES" | tr -d '\r')
  next_codename="${names[0]:-Quiet Peak}"
  found=0
  for i in "${!names[@]}"; do
    if [[ "${names[$i]}" == "$codename" ]]; then
      next_idx=$(( (i + 1) % ${#names[@]} ))
      next_codename="${names[$next_idx]}"
      found=1
      break
    fi
  done
  if (( found == 0 )); then
    next_codename="${names[0]:-Quiet Peak}"
  fi
  codename="$next_codename"
fi

set_prop REMARKABLE_VERSION_CODE "$code"
set_prop REMARKABLE_VERSION_NAME "$name"
set_prop REMARKABLE_VERSION_CODENAME "$codename"

{
  echo "## Remarkable ${name} · ${codename}"
  echo
  echo "- Build: \`${code}\`"
  echo "- Previous: \`${prev_name} ${prev_codename}\`"
  echo
  echo "### Changelog"
  echo
  if git rev-parse "$prev_tag" >/dev/null 2>&1; then
    range="${prev_tag}..HEAD"
  elif git rev-parse "v${prev_name}" >/dev/null 2>&1; then
    range="v${prev_name}..HEAD"
  else
    # First automated release: last 30 commits.
    range="$(git rev-list --max-count=30 HEAD | tail -n1)..HEAD"
  fi
  log="$(git log "$range" --pretty=format:'* %s (%h)' --no-merges 2>/dev/null || true)"
  if [[ -z "${log// }" ]]; then
    echo "* Build and packaging updates"
  else
    echo "$log"
  fi
  echo
  echo "---"
  echo "Install the APK below, or open this page from Remarkable → Settings when an update is offered."
} > "$OUT_DIR/CHANGELOG.md"

{
  echo "code=$code"
  echo "name=$name"
  echo "codename=$codename"
  echo "prev_name=$prev_name"
  echo "tag=v${name}"
} > "$OUT_DIR/version.env"

echo "Bumped ${prev_name} → ${name} (${codename}), code ${code}"
