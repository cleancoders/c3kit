# Contributing to c3kit

Thanks for your interest! c3kit is a family of independent libraries —
`apron`, `bucket`, `wire`, `scaffold` — each living in its own GitHub
repository and mounted here as a git submodule.

**Most contributions happen inside a submodule, not at the meta-repo root.**
Pick the right place to work, then read that submodule's `CONTRIBUTING.md`
for module-specific test commands and conventions:

- [c3kit-apron/CONTRIBUTING.md](https://github.com/cleancoders/c3kit-apron/blob/master/CONTRIBUTING.md)
- [c3kit-bucket/CONTRIBUTING.md](https://github.com/cleancoders/c3kit-bucket/blob/master/CONTRIBUTING.md)
- [c3kit-wire/CONTRIBUTING.md](https://github.com/cleancoders/c3kit-wire/blob/master/CONTRIBUTING.md)
- [c3kit-scaffold/CONTRIBUTING.md](https://github.com/cleancoders/c3kit-scaffold/blob/master/CONTRIBUTING.md)

## Where does my change belong?

| Change | Where |
|---|---|
| Bug fix or new feature in a library's source | Inside that submodule's repo |
| Updating a submodule's docs, CI, or templates | Inside that submodule's repo |
| Cross-module shell helpers in `bin/` | Meta-repo (here) |
| Bumping pinned submodule SHAs | Meta-repo (here), after the submodule release |
| Updating `AGENTS.md`, `DEPLOY.md`, or this `CONTRIBUTING.md` | Meta-repo (here) |

`apron` is the foundation — `bucket`, `wire`, and `scaffold` depend on it.
Changes that touch apron's public API may need follow-up bumps in dependent
modules.

## Workflow

**All pull requests must be linked to an open issue.** PRs without a linked,
maintainer-acknowledged issue will be auto-closed by the
`require-linked-issue` workflow. Open (or find) an issue first, get a
thumbs-up from a maintainer, then start work.

1. Open or find an issue describing the change.
2. Branch off `master` in the relevant repo.
3. Use TDD — write a failing spec first, then the minimum code to make it
   pass, then refactor.
4. Keep commits small and focused.
5. Update `CHANGES.md` in the affected submodule(s) with a one-line entry
   under the current version header (skip for doc-only changes).
6. Reference the issue with `Closes #N` (or `Fixes #N` / `Resolves #N`) in
   your PR description.
7. Ensure CI is green before requesting review.

## Local setup

```bash
git clone git@github.com:cleancoders/c3kit.git
cd c3kit
git submodule update --init
```

Each submodule has its own toolchain requirements (JDK 21+, Clojure CLI,
Babashka for apron, Redis for wire, Postgres/MSSQL for bucket, etc.). See
that submodule's `CONTRIBUTING.md` for the full list.

## Code style

- Idiomatic Clojure: prefer `->` / `->>` threading; small focused functions.
- `!`-suffix for fns that throw; `->type` / `<-type` symmetry for converters.
- Don't column-align values in maps; use single spaces.
- Keep `cond` predicates and their results on the same line (unless > 120
  chars).

## Code of Conduct

This project follows the [Contributor Covenant Code of Conduct](./CODE_OF_CONDUCT.md).
By participating you agree to abide by it.

## Reporting security issues

Please **do not** open a public issue for security reports. See
[SECURITY.md](./SECURITY.md) for how to reach us privately.
