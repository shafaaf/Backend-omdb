# GitHub Workflows Guide

This directory contains automation workflows that integrate Claude Code with your GitHub issue → plan → PR → merge workflow.

## The Loop

```
1. Open issue, tag @claude
   ↓
2. claude-plan.yml runs → posts plan comment, adds label
   ↓
3. Review plan, comment @claude implement
   ↓
4. claude-implement.yml runs → tests, screenshots, opens PR
   ↓
5. tests.yml runs → independent CI checks
   ↓
6. Review PR, comment @claude merge
   ↓
7. claude-merge.yml runs → merges to main
```

## Workflow Reference

| File | Trigger | What it does | Tools | Owner only? |
|------|---------|-------------|-------|------------|
| **claude-plan.yml** | `@claude` on new/existing issue | Explores code, posts structured plan (no code changes) | Read-only: `Read`, `Glob`, `Grep` | ✅ Yes |
| **claude-implement.yml** | `@claude implement` on issue with `claude:plan-ready` label | Implements plan, runs tests, boots app, screenshots UI, opens PR | Full access except merges: `Bash` (but blocks `gh pr merge` and `git push main`) | ✅ Yes |
| **tests.yml** | PR opened/updated | Runs backend (`mvnw test`) + frontend (`vitest`, `lint`) independently | None (plain CI) | ❌ Public |
| **claude-merge.yml** | `@claude merge` on PR | Merges PR if sender is owner (non-LLM, deterministic) | None (plain Actions) | ✅ Yes |
| **claude.yml** | `@claude` mention anywhere (issues/PRs/reviews) | General assistant mode (read + code changes, full access) | All tools | ❌ Public |
| **claude-code-review.yml** | Auto-trigger on PR open/update | Analyzes PR for bugs/style | All tools except merge | ❌ Public |

## Required Secrets

Before running any workflows, add these as GitHub repo secrets:

```bash
gh secret set JWT_SECRET --body "$(grep JWT_SECRET .env | cut -d= -f2)"
gh secret set OMDB_API_KEY --body "$(grep OMDB_API_KEY .env | cut -d= -f2)"
gh secret set CLAUDE_CODE_OAUTH_TOKEN --body "<your-oauth-token>"
```

(The last one should already exist; the first two are needed for `claude-implement.yml` and `tests.yml` to boot the real app.)

## Labels

The workflows use two labels to track state:

- **`claude:plan-ready`** — Issue has a plan posted, waiting for approval (`@claude implement`)
- **`claude:pr-created`** — Implementation in progress, PR created

These are added/removed automatically by the workflows.

## Ownership & Safety

- **Owner-only triggers** on `claude-plan.yml` and `claude-implement.yml` prevent unauthorized Claude credit spending and accidental app boots
- **`claude-merge.yml` is non-LLM** — merges only happen via your explicit `@claude merge` comment, never via Claude's decision
- **Merge-blocking rules** in `claude-implement.yml` explicitly prevent Claude from running `gh pr merge` even though it has raw `Bash` access
- **Independent test CI** (`tests.yml`) means "tests passed" is a real GitHub status check, not just Claude's claim

## Example Workflow

1. Create an issue: "Add logging to SearchPage component"
2. Comment `@claude`
3. Claude posts a plan → label `claude:plan-ready` added
4. You review, decide it's good, comment `@claude implement`
5. Claude: creates branch, edits files, runs tests (both pass ✅), boots frontend, screenshots the search page, opens PR with screenshots inline
6. You review PR (see tests passed, see UI looks right), comment `@claude merge`
7. PR merges, branch deleted, back to main
8. Label changes to `claude:pr-created` (done)

## Troubleshooting

**"Workflow did not trigger"**
- For plan: issue must mention `@claude` AND you must be the repo owner
- For implement: you must have commented `@claude implement` AND the issue must have `claude:plan-ready` label AND you must be owner
- For merge: you must have commented `@claude merge` on a PR AND you must be owner

**"Tests failed in implement stage"**
- Claude will report the failure in the workflow transcript
- Review the error, then either:
  - Comment the fix as a new issue and try again, or
  - Fix it locally, push to the branch, and Claude will re-run tests

**"Screenshot step failed"**
- Make sure `JWT_SECRET` and `OMDB_API_KEY` secrets are set
- Frontend tests must pass first (Playwright needs a valid app to screenshot)

**"PR was created but has wrong content"**
- Merge it (or close it) and try again with a clearer issue description
- You remain in control — Claude never auto-merges

## Permissions Layers

Two independent permission systems protect your code:

1. **GitHub token permissions** (`permissions:` block in each workflow)
   - Controls what GitHub API actions are allowed (read issues, write PRs, etc.)
2. **Claude's tool access** (`claude_args: --allowedTools/--disallowedTools`)
   - Controls what Claude itself can execute (Bash, Read, Edit, Write, etc.)
   - Example: `claude-implement.yml` gives Bash but explicitly blocks `gh pr merge` commands

Both layers must allow an action for it to happen.
