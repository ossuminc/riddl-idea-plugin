# Backlog: RIDDL IntelliJ IDEA Plugin

The single home for open work. If it is not here, it is not tracked.
Completed items leave this file: lessons go to NOTEBOOK.md, durable
facts to CLAUDE.md.

---

## Blocking a release

### 1. riddl dependency is local-only, so CI cannot build

`project/Dependencies.scala` pins `2.0.0-rc.10-37-1d87a109`, which
exists **only** in `~/.ivy2/local`. It matches `../bin/riddlc`
(verified 2026-08-08).

The Build workflow was revived in `6f6e1d9` and runs on push to
`main`, so **pushing turns CI red** — GitHub Actions has no
`~/.ivy2/local`.

**A published forward option now exists.** `2.0.0-rc.10` was published
to GitHub Packages on 2026-08-05 (verified 2026-08-08 via
`gh api /orgs/ossuminc/packages/maven/com.ossuminc.riddl-lib_3/versions`;
this supersedes the earlier note that `2.0.0-rc.9` was the newest
published). `37b0db94` is an ancestor of the `2.0.0-rc.10` tag, so
pinning to published `2.0.0-rc.10` moves *forward* from where this repo
was on 2026-08-04 — it is no longer a rollback. It costs the 37 staged
commits (A56, A57, `Riddl.Envelope`, the `external context` unused-warning
suppression).

Resolve by one of:
- pin to published `2.0.0-rc.10`, turning CI green at the cost of the
  staged language work; or
- riddl publishes `rc.10-37` or a later rc, then bump here; or
- decide CI stays red until 2.0.0 final and say so out loud.

**Reid's call 2026-08-08: stay on the staged `rc.10-37`** — the newest
work matters more than green CI for now. CI is therefore red *by
choice*. Revisit at release time.

### 2. Marketplace submission is under review; assets still missing

**The plugin IS on the Marketplace** — plugin id **33312**, xmlId
`com.ossuminc.riddl.plugins.idea`, uploaded 2026-08-03. This supersedes
the 2026-08-04 note that it was absent; the first manual upload has
happened, so that blocker is cleared.

Verified 2026-08-08 via `gh`-free `curl` against the Marketplace API:
`"approve": false`, `"listed": false` — i.e. **status "Under review"**.
Update id `1123790` is labelled version `0.10.0`, but the uploaded file
is `RIDDL4IDEA-0.9.0-beta-23-1ad6a8f8-20260802-2220.zip`, built from
commit `1ad6a8f` against riddl `2.0.0-rc.9-13`. **The submitted artifact
is not what HEAD builds** — a resubmission is needed regardless of the
review outcome.

Still outstanding:
- A permanent upload token at `~/.ij-plugin-repo-token`. Verified
  2026-08-08: neither that file nor `IJ_PLUGIN_REPO_TOKEN` exists, so
  `sbt publishPlugin` would still fail today.
- Marketplace assets (icon, screenshots).
- Resubmit a zip built from current HEAD — see item 2a.

### 2a. Resubmit with the 2026.1 / clean-API build

Done in the working tree, not yet released: platform floor raised to
2026.1 and every API notice eliminated (see NOTEBOOK.md § "All 40
verifier API notices were compiler-generated"). Verified 2026-08-08 by
`sbt runPluginVerifier` after `rm -rf target`: verdict is plain
**`Compatible`**, and the verifier emits no `internal-api-usages.txt`,
`deprecated-usages.txt` or `experimental-api-usages.txt` at all — down
from 8 internal + 12 scheduled-for-removal + 9 deprecated + 11
experimental.

Note this narrows the audience: the plugin no longer installs on
2025.3, which the current Marketplace listing advertises.

---

## Correctness

### 3. Completion keyword tables have drifted from the grammar

`completion/RiddlCompletionContributor.scala` § `RiddlKeywords`
offers keywords that do not match riddl's EBNF. Checked 2026-08-04
against `ebnf-grammar.ebnf` at riddl commit `37b0db94`:

- `while` — **not a riddl keyword at all**. Outright invalid
  suggestion.
- `application` — real keyword, wrong position. It is a *Context
  intention prefix* (`intention = "application" | "external" |
  "gateway" | "service"`, grammar:81), written *before* `context`,
  not a context-level definition.
- `epic` at CONTEXT_LEVEL — `epic` is a `domain_content`
  (grammar:domain_content), not a `context_definition`.
- `constant` at DOMAIN_LEVEL — `constant` lives in
  `processor_definition_contents`, i.e. context/entity level.
- Missing valid keywords: `group`, `user`, `handler` and `invariant`
  at context level; statements `when`, `match`, `yield`, `reply`,
  `let`, `put`, `prompt`, `require`.

All pre-existing — the same drift exists at rc.7, so no upgrade caused
it. Fix means rewriting `RiddlKeywords` against the grammar, plus
tests pinning each level. Completion is a headline feature, so this is
worth doing properly rather than patching one entry.

### 4. IDEA Community compatibility is unverified

Since `f8de90d` the build targets `ideaIU` (unified distribution).
Nothing now fails on an Ultimate-only API: the compiler does not, and
`runPluginVerifier` checks only against the SDK the build targets
(observed: `IU-253.29346.240`).

The plugin declares `<depends>com.intellij.modules.java</depends>` and
is intended to work on Community. Wire `pluginVerifierOptions`'
`overrideIDEs` at an IC installation so this is actually checked.
Until then treat Community support as an assumption.

### 5. Flaky performance test

`performance/RiddlPerformanceSpec` → "should show consistent
performance across multiple runs" failed once on 2026-08-03 during a
full run, then passed in isolation (avg 9ms, max deviation 2ms) and on
two consecutive full re-runs. Timing-sensitive threshold; will
intermittently redden CI. Either widen the tolerance or drop the
assertion.

---

## Build and release

### 6. Consider patchPluginXml to auto-sync the descriptor version

`plugin.xml`'s `<version>` is maintained by hand; sbt-dynver names the
zip. They can disagree, and the Marketplace reads the descriptor — so
tagging without editing the file publishes a mislabelled release.
`/ship` step 8 exists purely to catch that.

sbt-idea-plugin's `patchPluginXml` (settingKey `pluginXmlOptions`, with
fields `version`, `sinceBuild`, `untilBuild`, `pluginDescription`,
`changeNotes`; default `DISABLED`) can inject the version at build
time, making the git tag the single source of truth and deleting the
error class. Deferred because it changes local build behaviour: between
tags dynver yields `0.10.0-3-abc…`, which would land in the descriptor
for local builds.

### 7. sbt 2.0 / sbt-ossuminc 3.x remains blocked

`sbt-idea-plugin` publishes only for sbt 1.x. Detail and the recheck
one-liner are in NOTEBOOK.md § "Blocker: sbt 2.0 / sbt-ossuminc 3.x".
Recheck for an `_sbt2_3` artifact at
`https://repo1.maven.org/maven2/org/jetbrains/scala/`.

---

## Feature work (from NOTEBOOK, unchanged priority)

8. **Test coverage** — enable a 70% threshold. Scoverage does not
   instrument IntelliJ platform classes, so those need excluding from
   the report. Build currently sets `With.coverage(0)`.
9. **README screenshots** and MCP server setup docs. (Feature list,
   requirements, troubleshooting were added 2026-08-01; screenshots
   were not.)
10. **Find usages** — reverse reference finding; `RiddlLib.getOutline()`
    could support it.
11. **Performance** — incremental parsing, AST/token caching with
    invalidation on document change, debounced annotation (300–500ms).
12. **Legacy cleanup** — `utils/ParsingUtils.scala` still uses
    `Thread.sleep()` and calls `TopLevelParser.parseNebula()` directly;
    `files/utils.scala` deprecated manual highlighting;
    `settings/RiddlIdeaSettings.scala` is window-scoped and should be
    project-scoped.
13. **Hover documentation** — type signatures and descriptions on hover.
14. **LSP server** (long-term) — could serve VS Code, Cursor and Neovim
    from one codebase.
15. **Refactoring support** (long-term) — safe rename, extract
    definition.

---

## Open questions (need a decision, not code)

- **Marketplace pricing** — free or paid?
- **MCP server deployment** — where does the production server run for
  end users? The plugin's AI features point at riddl-mcp-server.
- **RIDDL version tracking** — how should the plugin track riddl
  releases, given `RiddlLib`'s API changed shape twice in 2.0
  (`Either` → `RiddlResult`, `fromPath` → `fromPathSafe`)?
