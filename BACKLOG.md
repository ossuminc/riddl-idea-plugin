# Backlog: RIDDL IntelliJ IDEA Plugin

The single home for open work. If it is not here, it is not tracked.
Completed items leave this file: lessons go to NOTEBOOK.md, durable
facts to CLAUDE.md.

---

## Blocking a release

### 1. riddl dependency is local-only, so CI cannot build

`project/Dependencies.scala` pins `2.0.0-rc.9-42-37b0db94`, which
exists **only** in `~/.ivy2/local`. Verified 2026-08-04: GitHub
Packages returns **404** for that version; the newest published
release there is `2.0.0-rc.9`.

The Build workflow was revived in `6f6e1d9` and runs on push to
`main`, so **pushing turns CI red** — GitHub Actions has no
`~/.ivy2/local`.

Resolve by one of:
- riddl publishes this build (or a later rc) to GitHub Packages, then
  bump here; or
- pin back to a published version (`2.0.0-rc.9`) and accept losing the
  staged fixes; or
- decide CI stays red until 2.0.0 final and say so out loud.

Related: `../bin/riddlc` is now **`2.0.0-rc.9-54-64b7b413`** (verified
2026-08-04), ahead of the pinned library. `2.0.0-rc.9-54-64b7b413` IS
staged in `~/.ivy2/local`, so upgrading to match riddlc is a one-line
change — but it does not fix CI, because that version is unpublished
too.

### 2. Marketplace first-time setup (human, cannot be automated)

The plugin is **not on the Marketplace** — verified: the API returns
404 for `com.ossuminc.riddl.plugins.idea`, and a search for "riddl"
returns two unrelated plugins.

JetBrains requires that "the first plugin publication must always be
uploaded manually" before the upload API accepts this plugin ID. Full
walkthrough is in `.claude/skills/ship/SKILL.md` § First-Time Setup.

Needs from Reid: Marketplace account + vendor profile, manual upload
of the first zip, moderation review (no guaranteed timeframe; chase
marketplace@jetbrains.com after 3–4 working days), then a permanent
token at `~/.ij-plugin-repo-token`. Verified 2026-08-04: neither that
file nor `IJ_PLUGIN_REPO_TOKEN` exists, so `sbt publishPlugin` would
fail today.

Also still to do before submitting: marketplace assets (icon,
screenshots).

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
