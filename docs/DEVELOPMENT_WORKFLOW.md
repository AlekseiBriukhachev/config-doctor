# Development workflow

The workflow is designed for a small project with Codex implementation and an independent review pass.

## Before implementation

1. Start from a clean working tree. Record the current branch and commit; never discard pre-existing user changes.
2. Read `AGENTS.md`, the relevant overview/product/architecture documents and the exact GitHub issue.
3. Check the issue's acceptance criteria against the current implementation. Inspect call sites and tests before editing.
4. Identify any Product Owner decision required for behavior, scope, compatibility or public UX. Do not silently decide it.
5. Propose one bounded implementation stage. Large work must be split into separately reviewable stages.

## Branch and commit discipline

- Use a focused branch based on the latest agreed `develop`.
- Never work directly on `develop` or `master`.
- Keep each PR scoped to one issue or one explicitly approved stage.
- Each stage/PR contains exactly one commit.
- Do not merge intermediate stages.
- Do not merge any PR without explicit Product Owner authorization.
- Do not rewrite or force-push a branch unless explicitly authorized.
- PR title and description must identify the issue, scope and stage.

## Implementation expectations

- Prefer test-first changes for domain rules and bug fixes.
- Keep production dependencies stable unless the issue justifies a change.
- Do not combine product expansion with unrelated cleanup.
- Add negative tests wherever a new diagnostic could create a false positive.
- For completion, test both positive suggestions and contexts where no suggestion must appear.
- For Quick Fixes, test comments, formatting, quoting, block scalars, lists, maps, stale PSI and preservation of unrelated YAML.
- For indexes/caches, test create/change/delete, classpath changes where feasible, duplicate metadata records and fallback equivalence.
- Update user-facing descriptions and this roadmap when the supported behavior changes.
- Record accepted Product Owner decisions in a decision record; proposals remain clearly marked as proposals.

## Verification

Run the narrowest relevant tests while iterating, then before PR:

1. `./gradlew test`
2. `./gradlew verifyPlugin`
3. `./gradlew buildPlugin`
4. `./gradlew runIde` for changes that affect plugin loading or editor behavior.

On Windows use `.\gradlew.bat`. On Unix, the wrapper executable mode is currently incorrect; see issue #14. Use `bash gradlew <task>` only as a temporary workaround. Report exact commands and observed results. Do not describe unavailable network, missing CI or unrun tests as successful.

## Pull request and review

The PR description must include:

- GitHub issue and stage;
- user story / behavior before and after;
- files/components changed;
- invariants and non-goals preserved;
- tests executed and their actual results;
- Plugin Verifier result or why it could not be run;
- compatibility, performance and migration impact;
- known limitations and follow-up issues.

After Codex opens the PR, perform an independent review of the complete diff and tests. Review correctness, false positives, threading, cache invalidation, PSI safety, compatibility, test quality and documentation. If changes are needed, provide a specific fix stage. The feature is not complete until the review is clean and the Product Owner authorizes merge.

## Roadmap and issues

- GitHub issues are the executable backlog and acceptance-criteria source.
- `docs/ROADMAP.md` is the ordered project-level view; keep issue numbers and status references synchronized.
- Update roadmap status only when the corresponding change is merged or the Product Owner explicitly changes scope.
- Do not close an issue merely because a PR was opened or code was written.
- The Project V2 board is a presentation layer; the repository roadmap and issues remain the durable source of truth.
