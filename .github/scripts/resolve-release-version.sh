#!/usr/bin/env bash
set -euo pipefail

FULL_VERSION_PATTERN='^[0-9]+\.[0-9]+\.[0-9]+-[0-9]+(\.[0-9]+)*-[0-9]+$'

validate_full_version() {
  local value="$1"
  local label="$2"
  if [[ ! "${value}" =~ ${FULL_VERSION_PATTERN} ]]; then
    echo "${label} '${value}' must match format x.x.x-<digits[.digits...]>-<digits>."
    exit 1
  fi
}

current_version="$(grep '^mod_version=' gradle.properties | cut -d'=' -f2- | tr -d '\r')"
custom_version="$(printf '%s' "${CUSTOM_VERSION:-}" | tr -d '\r')"

if [[ -n "${custom_version}" ]]; then
  if [[ "${BUMP_TYPE}" != "custom" ]]; then
    echo "custom_version can only be used when bump_type is 'custom'."
    exit 1
  fi
  if [[ "${custom_version}" =~ [[:space:]] ]]; then
    echo "custom_version cannot contain whitespace."
    exit 1
  fi
  validate_full_version "${custom_version}" "custom_version"
  new_version="${custom_version}"
else
  if [[ "${BUMP_TYPE}" == "custom" ]]; then
    echo "bump_type 'custom' requires custom_version to be set."
    exit 1
  fi
  version_core="${current_version%%-*}"
  version_suffix=""
  if [[ "${current_version}" == *-* ]]; then
    version_suffix="-${current_version#*-}"
  fi

  IFS='.' read -r major minor patch rest <<< "${version_core}"
  if [[ -n "${rest:-}" || -z "${major:-}" || -z "${minor:-}" || -z "${patch:-}" ]]; then
    echo "Current mod_version '${current_version}' is not bumpable. Use custom_version."
    exit 1
  fi
  if ! [[ "${major}" =~ ^[0-9]+$ && "${minor}" =~ ^[0-9]+$ && "${patch}" =~ ^[0-9]+$ ]]; then
    echo "Current mod_version '${current_version}' has a non-numeric semver core. Use custom_version."
    exit 1
  fi

  case "${BUMP_TYPE}" in
    patch) patch=$((patch + 1)) ;;
    minor)
      minor=$((minor + 1))
      patch=0
      ;;
    major)
      major=$((major + 1))
      minor=0
      patch=0
      ;;
    *)
      echo "Unsupported bump_type '${BUMP_TYPE}'."
      exit 1
      ;;
  esac

  new_version="${major}.${minor}.${patch}${version_suffix}"
fi

validate_full_version "${new_version}" "Resolved version"

if [[ "${new_version}" == "${current_version}" ]]; then
  echo "New version matches current version (${current_version})."
  exit 1
fi

tag="${TAG_PREFIX}${new_version}"
if git rev-parse -q --verify "refs/tags/${tag}" >/dev/null; then
  echo "Tag '${tag}' already exists in local history."
  exit 1
fi
if git ls-remote --exit-code --tags origin "refs/tags/${tag}" >/dev/null 2>&1; then
  echo "Tag '${tag}' already exists on origin."
  exit 1
fi

echo "current_version=${current_version}" >> "${GITHUB_OUTPUT}"
echo "new_version=${new_version}" >> "${GITHUB_OUTPUT}"
echo "tag=${tag}" >> "${GITHUB_OUTPUT}"
