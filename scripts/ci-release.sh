#!/usr/bin/env bash
# Copyright (c) 2026 Miguel Guerra
# SPDX-License-Identifier: MIT
#
# Usage:
#   ./scripts/ci-release.sh prepare [out_dir]
#       Use the version already in gradle.properties. Never bumps here.
#       Sets should_release=true only when tag v$VERSION does not exist yet.
#   ./scripts/ci-release.sh bump-next
#       After a successful *new* release, advance version for the next one:
#       1.0.0 -> 1.0.1 -> … -> 1.0.9 -> 1.1.0 (new codename on minor/major).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PROPS="$ROOT/gradle.properties"
NAMES="$ROOT/meta/codenames.txt"
MODE="${1:-prepare}"
OUT_DIR="${2:-$ROOT/dist}"
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

bump_version() {
  local code name codename
  code="$(get_prop REMARKABLE_VERSION_CODE)"
  name="$(get_prop REMARKABLE_VERSION_NAME)"
  codename="$(get_prop REMARKABLE_VERSION_CODENAME)"

  local major minor patch
  IFS=. read -r major minor patch <<<"$name"
  major="${major:-1}"
  minor="${minor:-0}"
  patch="${patch:-0}"

  local prev_name="$name"
  local prev_codename="$codename"
  local codename_changed=0

  patch=$((patch + 1))
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
    local next_codename="${names[0]:-Quiet Peak}"
    local found=0
    local i next_idx
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

  echo "Bumped ${prev_name} ${prev_codename} → ${name} ${codename} (code ${code})"
}

write_changelog() {
  local name="$1"
  local codename="$2"
  local code="$3"
  local prev_tag=""
  local -a vers=()
  local i

  # Previous release = highest semver tag strictly below this version
  # (ignores leftover higher tags from mistaken CI runs).
  mapfile -t vers < <(
    {
      git tag -l 'v*' | sed 's/^v//'
      echo "$name"
    } | grep -E '^[0-9]+\.[0-9]+\.[0-9]+$' | sort -u -V
  )
  for i in "${!vers[@]}"; do
    if [[ "${vers[$i]}" == "$name" ]]; then
      if (( i > 0 )); then
        prev_tag="v${vers[$((i - 1))]}"
      fi
      break
    fi
  done

  {
    echo "## Remarkable ${name} · ${codename}"
    echo
    echo "- Build: \`${code}\`"
    echo
    echo "### Changelog"
    echo
    local range log
    if [[ -n "$prev_tag" ]] && git rev-parse "$prev_tag" >/dev/null 2>&1; then
      range="${prev_tag}..HEAD"
      echo "_Changes since ${prev_tag}_"
      echo
    else
      echo "_Initial release_"
      echo
      range=""
    fi
    if [[ -n "$range" ]]; then
      log="$(
        git log "$range" --pretty=format:'* %s (%h)' --no-merges \
          --invert-grep --grep='\[skip ci\]' --grep='^chore(release)' \
          2>/dev/null || true
      )"
    else
      log=""
    fi
    if [[ -z "${log// }" ]]; then
      if [[ -z "$prev_tag" ]]; then
        echo "* First public build of Remarkable"
      else
        echo "* Maintenance and packaging updates"
      fi
    else
      echo "$log"
    fi
    echo
    echo "---"
    echo "Assets: \`remarkable-${name}-debug.apk\` and \`remarkable-${name}-release.apk\`."
    echo "Install from here, or open this page from Remarkable → Settings when an update is offered."
  } > "$OUT_DIR/CHANGELOG.md"
}

case "$MODE" in
  prepare)
    code="$(get_prop REMARKABLE_VERSION_CODE)"
    name="$(get_prop REMARKABLE_VERSION_NAME)"
    codename="$(get_prop REMARKABLE_VERSION_CODENAME)"

    if git rev-parse "v${name}" >/dev/null 2>&1; then
      echo "Tag v${name} already exists — will not republish or bump"
      should_release=false
    else
      echo "Will release ${name} ${codename} (code ${code})"
      should_release=true
    fi

    write_changelog "$name" "$codename" "$code"
    {
      echo "code=$code"
      echo "name=$name"
      echo "codename=$codename"
      echo "tag=v${name}"
      echo "should_release=$should_release"
    } > "$OUT_DIR/version.env"
    ;;
  bump-next)
    bump_version
    ;;
  *)
    echo "Unknown mode: $MODE (use prepare|bump-next)" >&2
    exit 1
    ;;
esac
