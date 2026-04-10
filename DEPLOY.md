# Deploying c3kit libraries

Each c3kit library — `apron`, `bucket`, `wire`, `scaffold` — releases
**independently**, with its own `VERSION`, its own `CHANGES.md`, its own git
tags, and its own Clojars coordinates. There is no root-level release process.

All artifacts publish to Clojars under the `com.cleancoders.c3kit` group:

| Module   | Clojars                                                      |
|----------|--------------------------------------------------------------|
| apron    | `com.cleancoders.c3kit/apron`                                |
| bucket   | `com.cleancoders.c3kit/bucket`                               |
| wire     | `com.cleancoders.c3kit/wire`                                 |
| scaffold | `com.cleancoders.c3kit/scaffold`                             |

A release is invoked from **inside** the target submodule by running
`clj -T:build deploy`. Everything below describes how to make sure that
command is going to do the right thing.

## Prerequisites (one-time setup)

1. **Clojars group membership.** You must be a member of the
   `com.cleancoders.c3kit` group. Non-members cannot push.
2. **Clojars deploy token.** Generate at https://clojars.org/tokens with
   scope `com.cleancoders.c3kit/*`. Store the token value — you can't view
   it again later.
3. **Environment variables.** In your shell (or `.envrc`):

   ```bash
   export CLOJARS_USERNAME="your-clojars-username"
   export CLOJARS_PASSWORD="your-deploy-token"   # NOT your login password
   ```

4. **Tooling.** `clojure` CLI (tools.build is pulled in via the
   submodule's `:build` alias) and `git` on PATH.

## Pre-flight checklist

Run every step **from inside the target submodule** (`cd apron`, `cd bucket`,
etc.). Do not skip. `clj -T:build deploy` will happily push a broken or
half-staged release if you let it.

### 1. Working tree is clean

```bash
git status --short
```

Expected: no output. If anything shows, commit or stash it first. The build's
`tag` task aborts on non-empty `git diff`, but that check only sees unstaged
modifications — staged-but-uncommitted changes slip past it and end up
*outside* the tagged commit. Don't rely on the safety net; start clean.

### 2. Local master matches origin master

```bash
git fetch origin
git log origin/master..HEAD    # must be empty
git log HEAD..origin/master    # must be empty
```

If you have unpushed commits, `git push origin master` first. If origin is
ahead, `git pull --rebase origin master`. The tag must point at a commit that
already lives on origin, otherwise `git push --tags` succeeds locally but
downstream consumers can't fetch the tagged commit.

### 3. `VERSION` is bumped

```bash
cat VERSION
```

Compare against what's already published on Clojars:

    https://clojars.org/com.cleancoders.c3kit/<lib-name>/versions

If the local `VERSION` is the same as (or older than) the published version,
bump it. Use semver:
 * **patch** (`2.5.0` → `2.5.1`) — bug fix, no API changes
 * **minor** (`2.5.0` → `2.6.0`) — backward-compatible feature additions
 * **major** (`2.5.0` → `3.0.0`) — breaking changes

Commit the bump as part of the release commit, not separately.

### 4. `CHANGES.md` top entry matches `VERSION`

Open `CHANGES.md`. The top heading must be `### <new-version>` — not
`### Unreleased`, not a stale previous version. Every change shipping in this
release should be listed under that heading. If the CHANGES header is missing
or stale, fix it before proceeding.

### 5. Install locally and smoke-test in a downstream project

```bash
clj -T:build install
```

`install` chains `clean` → `pom` → `jar` → `aether/install`, producing
`target/<lib-name>-<version>.jar` **and** placing it at
`~/.m2/repository/com/cleancoders/c3kit/<lib-name>/<version>/` where other
local projects can resolve it as a maven dependency.

**Then smoke-test it from a consuming project.** Pick a real downstream
project (another c3kit module, an application, etc.), pin the new version
in its `deps.edn`, and run its test suite:

```clojure
;; in the consumer's deps.edn
com.cleancoders.c3kit/<lib-name> {:mvn/version "<new-version>"}
```

```bash
cd ../consumer-project
clj -M:test:spec
```

This is the real pre-deploy check. Clojars releases are **immutable** — once
you push a version, you can't re-cut it, only bump to the next patch. Don't
skip this step just because tests pass inside the library itself; cross-
project resolution and compilation are their own surface area.

A `jar`-only build (without `install`) is not a sufficient smoke test —
`target/` is invisible to maven resolvers in other projects.

### 6. Clojars credentials are set

```bash
[ -n "$CLOJARS_USERNAME" ] && [ -n "$CLOJARS_PASSWORD" ] && echo "OK"
```

Should print `OK`. If not, see Prerequisites above.

### 7. Tests are green

Run the full test suite one last time on the commit you're about to tag:

```bash
clj -M:test:spec
bb spec                 # apron only — verifies bb compatibility
clj -M:test:cljs once   # modules with cljs
```

Don't deploy red. CI caught what it could on the PR, but the release commit
deserves a local green check too.

## Release command

```bash
clj -T:build deploy
```

That's it. This single command:

1. **`tag`** — verifies clean tree, checks the tag doesn't already exist,
   `git tag $VERSION`, `git push --tags`
2. **`jar`** — cleans `target/`, writes `pom.xml`, builds the jar
3. **`aether/deploy`** — uploads the jar and pom to Clojars

If any step fails, subsequent steps don't run. A failed `tag` leaves no
artifacts; a failed `jar` leaves no upload; a failed upload leaves a local jar
and a pushed tag — which is recoverable but annoying (see Troubleshooting).

## After a successful release

1. **Verify on Clojars.** Browse to
   `https://clojars.org/com.cleancoders.c3kit/<lib-name>` and confirm the new
   version appears. Give it ~30 seconds; the page is cached briefly.
2. **Bump the meta-repo submodule SHA.** The parent `c3kit` meta-repo pins a
   specific commit for each submodule. After a release, that pin is stale.
   From the meta-repo root:

   ```bash
   cd ..                          # back to c3kit root
   git add apron                  # stages the new submodule SHA
   git commit -m "bump apron to <version>"
   git push
   ```

3. **Announce / update downstream.** If this release fixes a bug or adds an
   API someone is waiting on, let them know.

## Module-specific notes

### apron

Apron is the foundation every other c3kit module depends on. Changes ripple.
Before releasing apron:
 * Verify `bb spec` passes. Apron is expected to load and run under Babashka;
   breaking that is a regression even if JVM tests are green.
 * If the release touches the public API of any namespace that downstream
   modules use (`schema`, `corec`, `time`, `log`, `util`, `app`, `refresh`),
   think about whether bucket/wire/scaffold will need corresponding bumps.

### bucket, wire

Both depend on apron. If you're releasing apron and one of these needs the
new apron, the order is:
 1. Release apron
 2. Update the dependent module's `deps.edn` to pin the new apron version
 3. Commit, push, test
 4. Release the dependent module

### scaffold

Build tooling. Lower churn than the others. `scaffold/dev/build.clj` inlines
its `pom-data` instead of defining a `pom-template` var — functionally
identical to the others.

## Troubleshooting

**`tag` aborts with "commit master before tagging".**
Working tree is dirty. `git status`, clean it up, retry.

**`tag` says "tag already exists".**
You already tagged this `VERSION` locally. Either bump `VERSION` or delete the
local tag (`git tag -d $VERSION`) if it's stale — but *never* delete a tag
that's already been pushed to origin.

**`aether/deploy` fails with 401.**
`CLOJARS_PASSWORD` is wrong or expired. Regenerate the token on
https://clojars.org/tokens and re-export.

**`aether/deploy` fails with 403.**
You're not a member of the `com.cleancoders.c3kit` group. Ask a current
member to add you.

**`aether/deploy` fails after `tag` succeeded (tag is pushed, jar isn't
uploaded).**
The tag is fine — it points at the right commit. Fix whatever broke the
upload, then re-run just the upload:

```bash
clj -T:build jar    # rebuild (jar may or may not exist in target/)
clj -T:build install  # optional: verify locally
# then call aether/deploy directly via a REPL, OR bump VERSION + redeploy
```

The simpler path for this situation is usually to bump the patch version
(`2.5.1` → `2.5.2`), re-commit, and run `deploy` fresh — Clojars versions
are immutable, so the half-released version is just a tag with no jar.

## Deprecated: `bin/tagall.sh`

The `bin/tagall.sh` script in the meta-repo is **stale** and should not be
used. It reads a root-level `VERSION` file that no longer exists (commit
`b25bfa1 removes VERSION`) and assumes a single synchronized version across
all submodules, which hasn't been the case for a long time. Tagging is
handled per-module by `build.clj`'s `tag` task, invoked as part of
`clj -T:build deploy`. If you find yourself reaching for `tagall.sh`, use
`deploy` instead.
