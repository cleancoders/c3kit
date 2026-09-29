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

**Releases run in CI, not on your machine.** Each library's `Release`
workflow (`.github/workflows/release.yml`) builds, publishes, verifies and
tags. `clj -T:build deploy` refuses to run outside GitHub Actions ("runs in
CI only"). The release logic lives in the shared build library
[`cleancoders/github-actions`](https://github.com/cleancoders/github-actions),
pinned by each library's `:build` alias; its
[releasing guide](https://github.com/cleancoders/github-actions/blob/master/docs/releasing.md)
is the full reference.

## Prerequisites (one-time setup)

1. **Clojars credentials live in the repo's `clojars` environment**
   (Settings → Environments → `clojars`), as the `CLOJARS_USERNAME` and
   `CLOJARS_PASSWORD` secrets. The password is a deploy token from
   https://clojars.org/tokens scoped to `com.cleancoders.c3kit/*`, owned by a
   member of the `com.cleancoders.c3kit` group. Nobody needs them in a local
   shell.
2. **The `clojars` environment gates the release.** Its required reviewers
   approve each run, and its deployment-branch policy limits it to `master`.
3. **Tooling:** `gh` (authenticated, with write access to the repo), plus the
   `clojure` CLI and `bb` for the local checks below.

## Pre-flight checklist

Run from **inside the target submodule** (`cd apron`, `cd bucket`, etc.).

### 1. Everything is on origin master

```bash
git status --short                                 # no output
git fetch origin
git log origin/master..HEAD; git log HEAD..origin/master   # both empty
```

The release builds whatever `master` points at on GitHub, so unpushed local
work is not in it.

### 2. `VERSION` is bumped and `CHANGES.md` matches

```bash
cat resources/c3kit/<lib-name>/VERSION   # apron's path; check each lib's
head -1 CHANGES.md                       # must be "### <that version>"
```

The version must not already be tagged or published: Clojars versions are
immutable, and the workflow refuses a version that is already tagged. Use
semver: **patch** for fixes, **minor** for backward-compatible additions,
**major** for breaking changes.

### 3. Optional: smoke-test in a downstream project

```bash
clj -T:build install        # builds the jar into ~/.m2
```

Then pin `{:mvn/version "<new-version>"}` in a real consumer's `deps.edn` and
run its suite. Local build tasks (`jar`, `install`) work; only `deploy` is
CI-only.

### 4. CI is green on master's head

```bash
gh run list --repo cleancoders/c3kit-<lib-name> --branch master --limit 3
```

The workflow checks this itself and refuses to release a commit whose CI did
not pass, so wait for the push's CI run to finish green.

## Release command

```bash
gh workflow run release.yml --repo cleancoders/c3kit-<lib-name> --ref master
gh run watch --repo cleancoders/c3kit-<lib-name> \
  $(gh run list --repo cleancoders/c3kit-<lib-name> --workflow release.yml \
      --limit 1 --json databaseId -q '.[0].databaseId')
```

Approve the `clojars` deployment when GitHub asks (in the run's page, or the
Actions tab). The same thing from the browser: Actions → **Release** → **Run
workflow** on `master`.

The job, in order: verifies CI succeeded for that exact commit, refuses a
version that is already tagged, builds the jar, publishes the jar and pom to
Clojars, re-fetches the jar from Clojars and compares digests, records the
digests in the job summary, and only then pushes the version tag. Later steps
attest build provenance and the SBOM on GitHub. A failed publish leaves no tag.

## After a successful release

1. **Verify on Clojars.** Browse to
   `https://clojars.org/com.cleancoders.c3kit/<lib-name>` and confirm the new
   version appears. Give it ~30 seconds; the page is cached briefly.
2. **Bump the meta-repo submodule SHA.** The parent `c3kit` meta-repo pins a
   specific commit for each submodule. After a release, that pin is stale.
   From the meta-repo root:

   ```bash
   cd ..                          # back to c3kit root
   git -C <lib-name> pull --ff-only   # the submodule at the released commit
   git add <lib-name>             # stages the new submodule SHA
   git commit -m "bump <lib-name> to <version>"
   git push
   ```

3. **Close linked issues and announce.** If this release fixes a bug or adds
   an API someone is waiting on, let them know.

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

Read the failed run's log: `gh run view <run-id> --repo cleancoders/c3kit-<lib-name> --log-failed`.

**"CI is not green for <sha>".**
The commit's CI run failed or hasn't finished. Fix or wait, then rerun.

**"already tagged".**
That `VERSION` was released before. Bump `VERSION` and `CHANGES.md`, push,
wait for CI, rerun.

**Clojars 401.**
The `CLOJARS_PASSWORD` secret in the `clojars` environment is wrong or
expired. Regenerate the token on https://clojars.org/tokens and update the
secret.

**Clojars 403 "Non-SNAPSHOT redeploy".**
Either the version is already on Clojars (bump it), or the build uploaded the
same file twice in one deploy. The latter was a build-library bug fixed in
`cleancoders/github-actions` c910fe3; make sure the lib's `:build` pin is at
or after it.

**Clojars 400 on an artifact.**
Clojars only accepts `.pom`, `.jar`, `.asc`, `.sha1`, `.md5`, `.module` and
`.sig` uploads. Anything else in the upload set (e.g. a `.json` SBOM) is
rejected; the build library stopped uploading the SBOM in c910fe3.

**The publish failed.**
No tag is pushed and nothing is released, unless the log shows the jar was
accepted. Fix the cause and rerun the workflow with the same version. If the
jar did land but verification failed, see the build library's
[verifying a release](https://github.com/cleancoders/github-actions/blob/master/docs/verifying-a-release.md)
guide before doing anything else.

**Break glass.**
If the workflow itself is broken and a release can't wait, the build
library's `clj -T:build emergency-publish` publishes from a machine, gated by
the `EMERGENCY_RELEASE` variable naming the exact version. Read the
[releasing guide](https://github.com/cleancoders/github-actions/blob/master/docs/releasing.md)
first: it skips the CI check, and every use is recorded.

## Tagging

Tags are created by the Release workflow, after the artifact is published and
verified. Don't tag by hand. Each library tags its own commits, and submodule
versions drift independently by design.
