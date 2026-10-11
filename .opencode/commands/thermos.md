---
description: Run Thermos security/correctness and maintainability reviews in parallel
---

Review the current working-tree changes for this project with both existing subagents:
`thermo-security-review` and `thermo-quality-review`.

First gather the working-tree diff and changed-file contents, including untracked files.
Invoke both subagents in parallel with the same review scope. Ask the security/correctness
reviewer to report only verified bugs, breaking changes, security issues, or developer
workflow regressions in changed code. Ask the quality reviewer to report only high-confidence
maintainability, structure, complexity, or boundary issues in changed code. Neither reviewer
may edit files or report pre-existing issues.

Wait for both reviews, deduplicate overlapping findings, preserve file-and-line evidence,
and synthesize a single verdict with findings ordered by severity. If there are no findings,
say so explicitly. Do not make code changes.
