# Backlog: RIDDL IntelliJ IDEA Plugin

The single home for open work. If it is not here, it is not tracked.
Completed items leave this file: lessons go to NOTEBOOK.md, durable
facts to CLAUDE.md.

---

## Blocking a release

### 1. riddl pin is 16 release candidates behind, and CI is red for it

`project/Dependencies.scala:7` pins `2.0.0-rc.10-37-1d87a109`, which
exists **only** in `~/.ivy2/local`. Verified 2026-08-26: still staged
there for all three consumed coordinates (`riddl-lib_3`,
`riddl-commands_3`, `riddl-language_3`), so the repo still builds
locally — `sbt test` gives 174 passing, 11 suites.

Because GitHub Actions has no `~/.ivy2/local`, **CI on `main` is red**:
`gh run list` shows failures for both 0.11.0 release commits.

**The tradeoff that justified this pin has evaporated.** In August the
choice was "staged build with the new language work" versus "published
build that is 37 commits older". As of 2026-08-26 riddl has published
through **`2.0.0-rc.26`** to GitHub Packages, and `../bin/riddlc` is
itself `2.0.0-rc.26`. Moving to published `2.0.0-rc.26` now goes
*forward* by 16 release candidates **and** turns CI green. There is no
longer a cost on either side.

Do this first — it is the cheapest win in this file, and every other
item is easier to judge against a green CI.

Caveats to check when doing it, not before:
- 16 RCs is a lot of language change. Expect the `RiddlLib` facade to
  have moved; the plugin uses `validateString`, `getTree`, `getOutline`
  (see CLAUDE.md § RIDDL API Integration).
- `rm -rf target` first. This repo's stale-`target/` trap is real and
  documented in NOTEBOOK.md.
- No upgrade task ever arrived in `task/` for rc.11..rc.26, so nobody
  has assessed the delta for this consumer.

### 2. Marketplace assets are still missing

**The plugin is published and public.** Verified 2026-08-26: update
`1130942` for version **0.11.0** reports `approve: true, listed: true`,
and the public endpoint
`/api/plugins/intellij/com.ossuminc.riddl.plugins.idea` returns `200`.
The stale 0.10.0 submission is **gone from the updates list entirely** —
JetBrains superseded rather than reviewed it, so the "resubmit a
non-stale zip" item is moot and has been removed.

(The plugin-level `approve` field still reads `false` while the update
reads `true`. The update-level flags plus the 200 are the ones that
track reality; do not chase the plugin-level field.)

What is genuinely still missing:

- **Marketplace assets** — a real icon and screenshots. The listing has
  neither. This is the main thing holding back a listing anyone would
  install from.
- Nothing else. The upload token at `~/.ij-plugin-repo-token` exists and
  works (0.11.0 went up through `sbt publishPlugin` on 2026-08-08).

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
(observed 2026-08-08: `IU-261.26222.65`).

The plugin declares `<depends>com.intellij.modules.java</depends>` and
is intended to work on Community. Wire `pluginVerifierOptions`'
`overrideIDEs` at an IC installation so this is actually checked.
Until then treat Community support as an assumption.

### 4a. Tool window console ignores the IDE theme

Observed 2026-08-08 in `sbt runIDE` on 2026.1: the run pane renders
"project's configuration file not configured in settings" as **black
text on a white background**, which does not match the surrounding UI
in a dark theme.

**Not a regression from the 0.11.0 builder change.** Both the
deprecated `TerminalExecutionConsole(project, processHandler)`
constructor and the `TerminalExecutionConsoleBuilder` path call the
same `createDefaultConsoleSettingsProvider()` with the same
`DEFAULT_INITIAL_TERM_SIZE`, so colours are identical either way
(verified against the 261 SDK sources).

The console is a `TerminalExecutionConsole` whose colours come from
`JBTerminalSystemSettingsProviderBase`. Fix means supplying a settings
provider that follows `EditorColorsManager`'s scheme via
`TerminalExecutionConsoleBuilder.settingsProvider(...)` — which is
precisely what that builder method exists for. Cosmetic only.

### 4b. Two objects are never disposed

Both reported as `SEVERE ... Memory leak detected` at sandbox IDE
shutdown on 2026-08-08, each registered in the Disposer as a child of
`ROOT_DISPOSABLE` and never disposed:

- `RiddlProject$$Lambda` — the document listener registered by the
  `postStartupActivity`.
- `TerminalExecutionConsole` — created per run pane in
  `ToolWindowUtils.createRunConsole`.

Both are **pre-existing**, not introduced by the 0.11.0 work: the
console registers itself through `TerminalConsoleContentHelper`, which
is built in a *field initializer* of `TerminalExecutionConsole` and so
runs on every constructor path, old and new.

Reid's call 2026-08-08: **low value, deferred.** They leak once per
tool window per IDE session, not per edit. Fixing means giving both a
real parent `Disposable` (the tool window's `Content`, or the project)
instead of the default root.

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

### 6a. `/ship` does not say that step 12 completes the upload

`.claude/skills/ship/SKILL.md` step 12 runs `sbt publishPlugin`, which
uploads the zip. Nothing in the skill says so plainly, so it is easy to
follow the release, then upload the same zip by hand and have JetBrains
reject it as a duplicate version. That happened on 2026-08-08 with
0.11.0 — harmless, but confusing, and a duplicate-rejected version
cannot be re-uploaded under the same number.

One or two sentences in step 12 fixes it: the upload is already done;
only bump and re-ship if the Marketplace *rejects* the version.

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
