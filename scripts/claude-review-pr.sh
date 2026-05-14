#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Usage:
  scripts/claude-review-pr.sh [--pr <number-or-url>] [--base <ref>] [--head <ref>] [--output <path>]

Reviews the current branch PR by default. The script sends Claude a persistent
BlindCheck briefing, PR metadata, changed files, and the PR diff.

Options:
  --pr       GitHub PR number or URL. Defaults to the PR for the current branch.
  --base     Base ref for local diff fallback. Defaults to PR base or origin/main.
  --head     Head ref for local diff fallback. Defaults to PR head or HEAD.
  --output   Review output path. Defaults to .claude/reviews/<pr-or-branch>-<timestamp>.md.
  -h, --help Show this help.
USAGE
}

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

json_field() {
  local json="$1"
  local field="$2"
  python3 -c 'import json,sys; data=json.loads(sys.argv[1]); value=data.get(sys.argv[2]); print("" if value is None else value)' "$json" "$field"
}

repo_root="$(git rev-parse --show-toplevel)"
cd "$repo_root"

pr_selector=""
base_ref=""
head_ref=""
output_path=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --pr)
      pr_selector="${2:?Missing value for --pr}"
      shift 2
      ;;
    --base)
      base_ref="${2:?Missing value for --base}"
      shift 2
      ;;
    --head)
      head_ref="${2:?Missing value for --head}"
      shift 2
      ;;
    --output)
      output_path="${2:?Missing value for --output}"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage >&2
      exit 1
      ;;
  esac
done

require_command git
require_command gh
require_command claude
require_command python3

repo_full_name="$(gh repo view --json nameWithOwner --jq '.nameWithOwner')"
current_branch="$(git branch --show-current)"

pr_json=""
if [[ -n "$pr_selector" ]]; then
  pr_json="$(gh pr view "$pr_selector" --json number,url,title,body,baseRefName,headRefName,state,isDraft)"
else
  if pr_json="$(gh pr view --json number,url,title,body,baseRefName,headRefName,state,isDraft 2>/dev/null)"; then
    :
  else
    pr_json=""
  fi
fi

if [[ -n "$pr_json" ]]; then
  pr_number="$(json_field "$pr_json" number)"
  pr_url="$(json_field "$pr_json" url)"
  pr_title="$(json_field "$pr_json" title)"
  pr_body="$(json_field "$pr_json" body)"
  pr_state="$(json_field "$pr_json" state)"
  pr_is_draft="$(json_field "$pr_json" isDraft)"
  base_ref="${base_ref:-$(json_field "$pr_json" baseRefName)}"
  head_ref="${head_ref:-$(json_field "$pr_json" headRefName)}"
  review_label="pr-${pr_number}"
else
  pr_number=""
  pr_url=""
  pr_title="Local review for ${current_branch:-HEAD}"
  pr_body=""
  pr_state="local"
  pr_is_draft=""
  base_ref="${base_ref:-origin/main}"
  head_ref="${head_ref:-HEAD}"
  review_label="${current_branch:-local}"
fi

timestamp="$(date +%Y%m%d-%H%M%S)"
if [[ -z "$output_path" ]]; then
  mkdir -p .claude/reviews
  output_path=".claude/reviews/${review_label}-${timestamp}.md"
else
  mkdir -p "$(dirname "$output_path")"
fi

prompt_file="$(mktemp "${TMPDIR:-/tmp}/blindcheck-claude-review.XXXXXX.md")"
diff_file="$(mktemp "${TMPDIR:-/tmp}/blindcheck-claude-diff.XXXXXX.patch")"
changed_files_file="$(mktemp "${TMPDIR:-/tmp}/blindcheck-claude-files.XXXXXX.txt")"
cleanup() {
  rm -f "$prompt_file" "$diff_file" "$changed_files_file"
}
trap cleanup EXIT

if [[ -n "$pr_json" ]]; then
  gh pr diff "${pr_selector:-$pr_number}" --patch > "$diff_file"
  gh pr diff "${pr_selector:-$pr_number}" --name-only > "$changed_files_file"
else
  git diff --find-renames "$base_ref...$head_ref" > "$diff_file"
  git diff --name-only "$base_ref...$head_ref" > "$changed_files_file"
fi

{
  cat <<PROMPT
You are reviewing a BlindCheck pull request.

Return a concise code review. Prioritize correctness, behavioral regressions,
missing tests, privacy risks, and drift from the BlindCheck MVP scope.

Do not request MVP-out-of-scope work such as visual contrast, low-vision checks,
Android Studio plugin work, TTS spy work, or exact TalkBack speech guarantees.

Use this response format:

Findings:
- [P0/P1/P2/P3] file:line - issue and why it matters

Tests:
- note missing or weak tests, if any

Summary:
- short overall assessment

If there are no actionable findings, say:
"Findings: none"

Repository: $repo_full_name
Current branch: ${current_branch:-detached}
PR number: ${pr_number:-none}
PR URL: ${pr_url:-none}
PR state: $pr_state
PR draft: ${pr_is_draft:-unknown}
Base ref: $base_ref
Head ref: $head_ref
PR title: $pr_title

PR body:
$pr_body

===== CLAUDE.md =====
PROMPT
  sed -n '1,260p' CLAUDE.md
  cat <<'PROMPT'

===== AGENTS.md =====
PROMPT
  sed -n '1,260p' AGENTS.md
  cat <<'PROMPT'

===== docs index =====
PROMPT
  find docs -maxdepth 1 -type f -name '*.md' -print | sort
  cat <<'PROMPT'

===== changed files =====
PROMPT
  cat "$changed_files_file"
  cat <<'PROMPT'

===== diff =====
PROMPT
  cat "$diff_file"
} > "$prompt_file"

if [[ -n "$pr_url" ]]; then
  claude --from-pr "$pr_url" --print --tools "" < "$prompt_file" | tee "$output_path"
else
  claude --print --tools "" < "$prompt_file" | tee "$output_path"
fi

echo
echo "Claude review written to: $output_path"
