# c3kit

**Clean Coders Clojure Kit** — a family of Clojure libraries published at
`com.cleancoders.c3kit/*` and consumed via Clojars or git deps. This working
directory is a **meta-repo**: each library lives in its own GitHub repo and is
mounted here as a git submodule. You will do most of your real work inside
those submodules, not at the root.

## Toolbox

This project uses [toolbox](https://raw.githubusercontent.com/slagyr/toolbox/main/SKILL.md)
to manage agent components. If `.toolbox/` doesn't exist, fetch the toolbox
SKILL.md from the URL above and follow its instructions. Once bootstrapped:

- **Skills:** Load from `.toolbox/skills/{name}/SKILL.md` when their descriptions match the task at hand.
- **Commands:** When the user invokes a command by name (e.g., "/plan"), read and follow `.toolbox/commands/{name}.md`.

### Skills

- [tdd](https://raw.githubusercontent.com/slagyr/agent-lib/main/skills/tdd/SKILL.md)
- [crap4clj](https://raw.githubusercontent.com/unclebob/crap4clj/master/SKILL.md)
- [clj-mutate](https://raw.githubusercontent.com/slagyr/clj-mutate/master/SKILL.md)
- [speclj-structure-check](https://raw.githubusercontent.com/unclebob/speclj-structure-check/master/.claude/skills/speclj-structure-check/SKILL.md)
- [gherclj](https://raw.githubusercontent.com/slagyr/agent-lib/main/skills/gherclj/SKILL.md)

### Commands

- [plan](https://raw.githubusercontent.com/slagyr/agent-lib/main/commands/plan.md)
- [todo](https://raw.githubusercontent.com/slagyr/agent-lib/main/commands/todo.md)
- [work](https://raw.githubusercontent.com/slagyr/agent-lib/main/commands/work.md)
- [plan-with-features](https://raw.githubusercontent.com/slagyr/agent-lib/main/commands/plan-with-features.md)

## Repo Layout

```
c3kit/
├── apron/      ← git submodule — cleancoders/c3kit-apron (the foundation)
├── bucket/     ← git submodule — cleancoders/c3kit-bucket (datastore abstraction)
├── wire/       ← git submodule — cleancoders/c3kit-wire   (HTTP / transport)
├── scaffold/   ← git submodule — cleancoders/c3kit-scaffold (build / cljs / css tooling)
├── config/     ← config loading (local, not yet a submodule on disk)
├── bin/        ← shell helpers that fan operations across every submodule
├── deps.edn    ← tiny shell for the meta-repo itself; almost nothing lives here
└── README.md
```

**Dependency direction:** `apron` is the root of the chain. Everything depends
on apron; apron depends on nothing in c3kit. `bucket`, `wire`, and `scaffold`
all declare `com.cleancoders.c3kit/apron` in their `deps.edn`. Changes in apron
can ripple; prefer fixing at the lowest module that makes sense.

**Submodule state:** Run `git submodule status` to see pinned commits. On a
fresh clone:

```bash
git submodule init
git submodule update
```

Each submodule has its own git history, branches, tags, and releases. A change
inside a submodule is a commit in *that* repo, and the parent c3kit repo
records the new submodule SHA as its own commit.

### When to work at the root vs. inside a submodule

- **Inside the submodule** — source changes, tests, version bumps, module-specific docs. `cd apron && clj -M:test:spec`, for example.
- **At the root** — only for cross-module scripts under `bin/`, updating pinned submodule SHAs, or meta docs like this one.

## Module Cheatsheet

| Module     | Purpose                                    | Paths              | Platforms     |
|------------|--------------------------------------------|--------------------|---------------|
| `apron`    | Core utilities: app lifecycle, schema, log, time, corec, util, refresh, cursor, etc. | `src/clj`, `src/cljc` | JVM + **Babashka** + cljs |
| `bucket`   | Datastore abstraction (Datomic, JDBC, H2, SQLite, Postgres, MSSQL) | `src/clj`, `src/cljc`, `src/cljs` | JVM + cljs    |
| `wire`     | HTTP/transport (ring, http-kit, cljs-http, Redis streams, JWT, anti-forgery) | `src/clj`, `src/cljc`, `src/cljs` | JVM + cljs    |
| `scaffold` | Build tooling (cljs compiler wrapper, CSS via garden, Playwright) | `src/`             | JVM           |
| `config`   | Config loading                             | `src/`             | cljc          |

**Bucket has its own `AGENTS.md`** at `bucket/AGENTS.md` — read it before doing
work in that module. It uses `bd` (beads) for issue tracking and has a
"landing the plane" session-completion workflow that MUST be followed
(including `git push`) before you consider bucket work done. Root-level work
does not use beads.

## Running Tests

Every module follows the same speclj-based conventions. From inside a module:

```bash
# Delete build artifacts
bb clean                     # where a bb.edn exists (apron)

# JVM specs (Clojure)
clj -M:test:spec             # one shot
clj -M:test:spec -a          # auto/watch runner

# ClojureScript specs (where the module has cljs sources)
clj -M:test:cljs once        # one shot
clj -M:test:cljs             # auto/watch runner
```

```bash
# From apron/
bb spec                      # one shot
bb spec -a                   # auto runner
bb spec-cst                  # Central time zone variant
bb spec-mst                  # Mountain time zone variant
```

**Timezone-variant suites.** Apron and most modules ship `:spec-cst` and
`:spec-mst` aliases that pin `user.timezone` to `America/Chicago` and
`America/Phoenix` respectively. These catch DST/timezone bugs. The default
`:spec` alias excludes `cst`/`mst`-tagged tests (`-t ~cst -t ~mst`) so you
don't run them three times accidentally. If you touch time-sensitive code,
run all three.

**Running everything across all modules:**

```bash
bin/testall.sh
```

Note `bin/c3kit.env` enables `set -ex`, so any failure halts the script
immediately.

## Conventions

- **Test framework:** [speclj](https://github.com/slagyr/speclj). Use
  `describe`/`context`/`it` with concrete, behavioral names. Specs live under
  `spec/clj`, `spec/cljc`, `spec/cljs`, mirroring `src/`. Filename pattern:
  `foo_spec.clj(c|s)` for `c3kit.module.foo`.
- **Namespace prefix:** everything under `c3kit.<module>.*` (e.g.
  `c3kit.apron.refresh`, `c3kit.bucket.db`).
- **Path layout:** code is split by platform dialect:
  `src/clj` (JVM-only), `src/cljc` (portable), `src/cljs` (browser-only). Put
  code in the broadest dialect that works — prefer `cljc` when you can.
- **TDD is the expected workflow** in this family of projects. Load the `tdd`
  skill from `.toolbox/skills/tdd/SKILL.md` whenever you're about to write or
  change code. Red → green → refactor; no production code without a failing
  test.
- **Babashka compatibility matters for apron.** Apron's `.clj` files are
  expected to load in bb. `apron/CHANGES.md` tracks this as a first-class
  concern (2.4.0 added bb support; 2.4.1 extended it to remaining clj nses).
  Don't introduce JVM-only constructs into apron's clj namespaces without
  checking that bb still loads them.
- **Versioning:** each module has its own `VERSION` file and its own
  `CHANGES.md`. Bumping a version is a commit in that submodule; the parent
  meta-repo then records the new SHA.
- **No file bloat at the root.** The root `src/` and `spec/` directories exist
  but are essentially empty scaffolds. All real code belongs in a module.

## bin/ Scripts

Fan operations across every submodule listed in `bin/c3kit.env` (currently
`apron scaffold bucket wire config`):

| Script              | What it does                                            |
|---------------------|---------------------------------------------------------|
| `bin/cloneall.sh`   | Clone every submodule fresh                             |
| `bin/pullall.sh`    | `git pull` inside every submodule                       |
| `bin/pushall.sh`    | `git push` from every submodule                         |
| `bin/testall.sh`    | Run `:test:spec` and `:test:cljs once` in every module  |
| `bin/tagall.sh`     | **Deprecated** — reads a root `VERSION` that no longer exists. See Releases below. |

All scripts `source bin/c3kit.env` and run with `set -ex`.

## Releases

Each submodule releases independently to Clojars under
`com.cleancoders.c3kit/<module>` via `clj -T:build deploy`, run **from inside
the submodule**. There is no root-level release workflow. Full pre-flight
checklist, prerequisites, and troubleshooting in [`DEPLOY.md`](DEPLOY.md).
Read it before preparing any release — the `tag` step pushes to origin, so
"dry runs" are not a thing once `deploy` is invoked.

## Quick Orientation Checklist for a New Agent

1. `git submodule status` — confirm submodules are present and on expected SHAs.
2. Skim `apron/README.md` and `apron/CHANGES.md` for module-specific
   conventions and the bb-support history.
3. If the task mentions a specific module, read that module's `AGENTS.md` (if
   any) before writing code. Bucket has one; the others currently don't.
4. Load the `tdd` skill before making code changes.
5. When you touch `apron`, remember to run **both** `clj -M:test:spec` and
   `bb spec` from inside `apron/`.
