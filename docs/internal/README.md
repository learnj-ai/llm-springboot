# Internal docs — workshop maintenance trail

These files are not part of the published workshop tutorial. They're the
working artifacts produced during the May 2026 accuracy + Spring Boot 4
migration pass, kept here so future maintainers have context for the
non-obvious decisions in the codebase.

| File | What it is |
|------|-----------|
| [`01-tasks-original.md`](01-tasks-original.md) | The original task list for the workshop-review-fixes branch (PR #10, merged). Items are checked off in commit messages, not here. Useful for understanding why specific changes were made. |
| [`02-accuracy-review-2026-05-12.md`](02-accuracy-review-2026-05-12.md) | The independent accuracy review that drove PR #11 and PR #12. Lists what was verified vs broken at the time, with file/line citations. The fixes for everything in here have landed; the document is preserved as the audit trail. |

If you're reading this because you found something inconsistent and want to
know the history: start with the relevant section in `02-accuracy-review-…`,
then check `git log -- <file>` for the commits that addressed it.
