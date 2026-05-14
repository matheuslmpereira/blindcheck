# Claude Review Workflow

## Goal

Use a local Claude Code instance to review each PR without rebuilding repository context from scratch.

`CLAUDE.md`, `AGENTS.md`, and `docs/` are the persistent context. The review script collects PR metadata, changed files, and the PR diff, then runs Claude in a PR-linked session.

## Command

```bash
scripts/claude-review-pr.sh
```

By default, the script reviews the PR associated with the current branch.

Useful options:

```bash
scripts/claude-review-pr.sh --pr 2
scripts/claude-review-pr.sh --pr https://github.com/matheuslmpereira/blindcheck/pull/2
scripts/claude-review-pr.sh --base codex/pr1-base-contracts --head HEAD
scripts/claude-review-pr.sh --output /tmp/blindcheck-claude-review.md
```

## Behavior

The script:

* requires `claude`, `gh`, and `git`;
* requires Claude Code to be authenticated locally; run `claude` once and use `/login` if the CLI reports `Not logged in`;
* resolves PR title, body, base branch, head branch, URL, changed files, and diff;
* embeds `CLAUDE.md`, `AGENTS.md`, and the docs index in the review prompt;
* runs `claude --from-pr <pr-url> --print` so Claude can keep a PR-linked session;
* writes the review to `.claude/reviews/` by default.

`.claude/reviews/` and `.claude/worktrees/` are local-only and ignored by git.

## Follow-up Loop

After a PR is created:

```bash
scripts/claude-review-pr.sh --pr <number-or-url>
```

Then:

1. read the generated review file;
2. apply only relevant, in-scope fixes;
3. rerun validation commands;
4. commit and push fixes to the same PR branch;
5. answer Claude feedback in the PR if the feedback came from a PR comment.

If Claude reports no actionable issues, keep the generated review as local evidence and continue the normal PR validation flow.
