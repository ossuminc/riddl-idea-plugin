# Engineering Notebook: RIDDL IntelliJ IDEA Plugin

## HANDOFF

**State** (verified 2026-08-04, not recalled):

- Branch `main`, working tree **clean**, nothing pushed since
  `9ee401c`. Count them with
  `git log --oneline origin/main..HEAD` rather than trusting a
  number written here — this line is one commit stale the moment the
  handoff itself is committed.
- `sbt test` → **174 passing, 11 suites, 0 failed**. Run this session.
- riddl dependency: `2.0.0-rc.9-42-37b0db94`.
- Descriptor `plugin.xml`: version **0.10.0**, `since-build="253"`.
  Only tag in the repo is still `0.9.0-beta` — 0.10.0 is prepared but
  **not tagged and not released**.

**In flight / next**

Nothing is half-written; the tree is clean. The next decision is
BACKLOG.md item 1: the pinned riddl version exists only in
`~/.ivy2/local` (GitHub Packages 404s), so **pushing turns CI red**.
Decide whether to wait for riddl to publish, pin back
to `2.0.0-rc.9`, or accept red CI. Nothing else should be pushed
before that call.

**Traps that already bit someone here**

1. **Stale `target/` fakes a green build.** `target/streams` and
   `target/sbtidea.cache` can serve an old classpath to *Test* scope
   while *Compile* uses the new one. It once ran the whole suite green
   against riddl 1.13.3 — a version never declared in this repo — and
   shipped a zip with duplicate riddl jars. `rm -rf target` after any
   dependency change; deleting `sbtidea.cache` alone is not enough.
   Full write-up below under "Trap: stale `target/`".
2. **`export Test/fullClasspath` is unreliable in this build** — it
   prints stale or empty output. Use `Test/dependencyTree` instead.
3. **Ivy-local jars are packaged without a version in the filename**
   (`riddl-lib_3.jar`). Reading the version off a jar name does not
   work for locally staged builds; compare checksums against
   `~/.ivy2/local` instead.
4. **`sbt runPluginVerifier` verifies against `ideaIU` only.** It does
   not establish Community compatibility. See BACKLOG item 4.

**Certainty**

- Verified this session: git state, test count, riddlc version, the
  GitHub Packages 404, descriptor contents, Marketplace absence.
- Assumed, not verified: that the plugin still works on IDEA
  Community (BACKLOG item 4); that the 41 internal/experimental/
  deprecated API usages will pass Marketplace review.

**Pointers**

- Open work → `BACKLOG.md` (all of it, with evidence).
- Durable architecture and API facts → `CLAUDE.md`.
- Release procedure incl. Marketplace setup → `.claude/skills/ship/`.

**`task/` is empty of untriaged items** — two files sit in
`task/done/`. Nothing awaits triage, but run
`/ossuminc-skills:check-tasks` in the new session anyway; that call is
the driver's, not the handoff's.

---

## Incoming Tasks

**At session start**, check the `task/` directory for pending
work requests from other projects. Each `.md` file describes a
task (e.g., dependency upgrade). Treat unresolved tasks as to-do
items unless already completed (verifiable from this notebook,
CLAUDE.md, or git log). After completing a task, append results
to the task file and note completion in this notebook.

---

## Current Status

**Version**: 0.9.0-beta (released 2026-01-21)
**Branch**: `main`
**RIDDL**: 2.0.0-rc.9-42-37b0db94 | **Scala**: 3.9.0-RC4
**sbt-ossuminc**: 1.4.0
**sbt**: 1.12.3 | **sbt-idea-plugin**: 6.0.0-RC3
**IntelliJ Platform**: 253.29346.240 (2025.3)
**Tests**: 174 passing across 11 suites

All core features implemented. Plugin is functional and ready
for beta testing. RiddlLib integration complete (2026-02-11).

---

## Changes Since 0.9.0-beta

These changes have been made since the beta tag. They should be
included in the next version bump.

1. **RiddlLib Integration** (2026-02-11)
   - Annotator: `RiddlLib.validateString()` for full semantic
     validation (parse + resolution + validation passes); falls
     back to `RiddlLib.parseNebula()` for include fragments
   - Structure view: `RiddlLib.getTree()` for AST-accurate
     recursive hierarchy; falls back to regex for fragments
   - Navigation: `RiddlLib.getOutline()` for AST-accurate
     go-to-definition; falls back to regex for fragments
   - All three use the pattern: try RiddlLib first, regex
     fallback on parse failure

2. **Dependency Upgrades** (2026-02-11)
   - RIDDL: 1.0.0 -> 1.8.0
   - Scala: 3.4.3 -> 3.7.4
   - sbt-ossuminc: 1.2.5 -> 1.3.2
   - Added riddl-lib dependency

3. **Test Improvements** (2026-02-11)
   - Tests: 149 -> 162 (13 new)
   - Added semantic error detection test (annotator)
   - Added fragment fallback test (annotator)
   - Added deep hierarchy test (structure, 3+ levels)
   - Added fragment fallback test (structure)
   - Adjusted valid-RIDDL tests: `validateString()` produces
     MissingWarning for minimal models; tests now check for
     no Error-level messages rather than empty messages

4. **Dependency Upgrades for RIDDL 2.0** (2026-08-01)
   - RIDDL: 1.13.1 -> 2.0.0-rc.7
   - Scala: 3.7.4 -> 3.9.0-RC4
   - sbt-ossuminc: 1.3.5 -> 1.4.0
   - sbt-idea-plugin: 5.1.0 -> 6.0.0-RC3 (overridden in
     `project/plugins.sbt`; sbt-ossuminc 1.4.0 re-exports 5.1.0)
   - Source changes forced by the upgrades:
     - `RiddlLib` returns a `RiddlResult` ADT instead of
       `Either[Messages, T]` (annotator, navigation, structure)
     - Scala 3.9 rejects type ascriptions after tuple patterns
       (`utils.scala`)
     - New `KindOfMessage` cases -- `Tip`, `Deprecation`,
       `CompletenessWarning` -- made the annotator's severity
       mapping non-exhaustive
     - `RiddlParserInput.fromPath` deprecated for `fromPathSafe`,
       which reports load failures as `Messages` instead of
       throwing (`ParsingUtils.scala`)

5. **Release plumbing and further riddl upgrades** (2026-08-01..04)
   - `/ship` skill added (`.claude/skills/ship/`); CI revived — every
     build/test step in `build.yaml` had been commented out
   - `plugin.xml`: version 0.10.0, `change-notes` added,
     `since-build` relaxed 253.29346.240 -> 253
   - `runPluginVerifier` made usable as a gate: sbt-idea-plugin
     defaults `failureLevels` to every level, so it failed on any
     deprecated/experimental/internal API notice while the verdict
     was still "Compatible"
   - RIDDL: 2.0.0-rc.7 -> 2.0.0-rc.9-13 -> 2.0.0-rc.9-42, no source
     changes required. The only syntax change in that window (saga
     bodies may hold types and comments) is purely permissive.
   - Colour-settings DEMO_TEXT rewritten as valid RIDDL, verified
     with `riddlc validate`. It had four pre-existing defects
     (`Integer(0,150)`, `state ... of X`, `@msg.field`, redefining
     the predefined `UserId`) that nothing caught because the text
     is only tokenized, never parsed.

**Recommended version**: 0.10.0 (new features, no breaking
changes to user-facing behavior).

---

## Blocker: sbt 2.0 / sbt-ossuminc 3.x

**This project cannot move to sbt 2.0 yet.** Verified 2026-08-01.

`sbt-idea-plugin` publishes only for sbt 1.x -- the sole coordinate
on Maven Central is `org.jetbrains.scala:sbt-idea-plugin_2.12_1.0`.
There is no `_sbt2_3` variant, and 6.0.0-RC3 (released 2026-07-31)
is still sbt 1.x only. sbt-ossuminc's own `build.sbt` lists this
plugin under "Blocked: no sbt 2.0 artifact yet" and names this repo
as the affected consumer.

Because sbt-ossuminc 3.x publishes as `sbt-ossuminc_sbt2_3` (sbt 2
only), the two requirements are mutually exclusive. The build must
stay on sbt 1.12.3 + sbt-ossuminc 1.4.0 until JetBrains ships an
sbt 2 build of sbt-idea-plugin.

This matters because the entire build depends on that plugin for
`intellijBuild`, `intellijPlatform`, `intellijPlugins`, `runIDE`,
`packageArtifactZip`, `publishPlugin` and IntelliJ SDK resolution.

**Recheck** by looking for an `_sbt2_3` artifact at
`https://repo1.maven.org/maven2/org/jetbrains/scala/`.

---

## Resolved: now targeting ideaIU (2026-08-01)

`IntelliJPlatform.IdeaCommunity` is deprecated in sbt-idea-plugin
6.x, and the replacement is **not a rename**:

| Setting | SDK artifact | platformPrefix |
|---------|--------------|----------------|
| `IdeaCommunity` (deprecated) | `ideaIC` | `Idea` |
| `Idea` (adopted) | `ideaIU` | (empty) |

We now use `Idea`, following JetBrains' unified distribution.
Consequences to keep in mind:

- The SDK lives in `~/.RIDDL4IDEAPluginIU`, not `...IC`. The old IC
  tree is still on disk and can be deleted to reclaim ~5 GB.
- `runIDE` launches **Ultimate**.
- Building against IU no longer fails on Ultimate-only APIs the way
  IC did. Nothing enforces that the plugin stays Community-safe, so
  new platform API use needs a manual check.

---

## Trap: stale `target/` can silently fake a green build

Hit on 2026-08-01 and worth recognising again. Symptom: tests fail
with `NoSuchMethodError` on a riddl class, or -- worse -- PASS while
exercising the wrong riddl version.

`target/streams` and `target/sbtidea.cache` persist a resolved
classpath. After a dependency bump they can keep serving the old one
to **Test** scope while **Compile** correctly uses the new one. In
this case Test resolved riddl 1.13.3 -- a version never declared in
`Dependencies.scala` at all, left over from a transient edit in an
earlier session -- alongside 2.0.0-rc.7, with 1.13.3 winning for
`Messages` classes.

It also inflated the packaged artifact: `packageArtifactZip` bundled
duplicate riddl jars, 41 files / 35 MB instead of 27 files / 20 MB.

**Diagnosis**: compare `export Compile/fullClasspath` with
`export Test/fullClasspath` and look for two versions of the same
jar. **Fix**: `rm -rf target`. Deleting `target/sbtidea.cache` alone
is not enough.

---

## Next Steps

All open work now lives in **BACKLOG.md** — features, bugs,
blockers and open questions, each with the verification already
done. This section is intentionally a pointer so there is exactly
one place to look.

---

## Architecture Decisions

### Wrap RIDDL Tokenization (Not Separate Lexer)

RIDDL provides `TopLevelParser.parseToTokens` designed for IDE
tooling. Wrapping it in `RiddlLexerAdapter` gives:
- Single source of truth (consistent with VS Code plugin)
- Automatic updates when RIDDL grammar changes
- ~1 day implementation vs ~2 weeks for separate lexer

**Trade-off**: Lose IntelliJ's mid-document restart optimization.
Acceptable because RIDDL has structural dependencies preventing
partial parsing anyway.

### RiddlLib First, Regex Fallback

For annotator, structure view, and navigation: try RiddlLib's
AST-based methods first (accurate, handles hierarchy). Fall back
to regex when parsing fails (include-fragment `.riddl` files
aren't complete Root documents and can't be parsed by the full
pipeline).

### Project-Level Services (Not Window-Based)

The prototype used per-window mutable state. The rewrite uses:
- `RiddlMcpService` — Per-project MCP connection
- `RiddlIdeaSettings` — Application-level persistent state

The legacy window-scoped state in `RiddlIdeaSettings` still
exists and functions but is architecturally outdated.

### AI Integration via riddl-mcp-server

The plugin communicates with riddl-mcp-server over JSON-RPC 2.0:
- `validate-text` / `validate-partial` — Model validation
- `check-completeness` — Missing element detection
- `map-domain-to-riddl` — Natural language to RIDDL generation

---

## Known Issues

### Plugin Verifier Results (as of 2026-01-21)

- **8 internal API usages** — Mostly unavoidable
  Language/Configurable bridge methods
- **13 experimental API usages** — ToolWindowFactory methods
- **8 deprecated API usages** — Scala-generated bridge methods
- **0 non-extendable API violations** — Fixed

These are acceptable for marketplace submission and don't affect
functionality.

### Test Environment

- Tests run on JDK 25 (Temurin)
- Some IntelliJ-dependent tests require platform fixtures;
  performance tests use only RIDDL APIs and work standalone
- Coverage threshold set to 0% in build.sbt (Scoverage can't
  instrument IntelliJ platform classes)

---

## Session Log

### 2026-02-11 — RiddlLib Integration

Integrated RiddlLib high-level API (introduced in RIDDL 1.8.0)
into three core features:

1. **Annotator** — `RiddlLib.validateString()` replaces
   `TopLevelParser.parseNebula()`. Now provides full semantic
   validation (parse + resolution + validation passes), not
   just syntax checking. Fragment files fall back to
   `RiddlLib.parseNebula()`.

2. **Structure View** — `RiddlLib.getTree()` replaces regex.
   Now provides accurate recursive hierarchy (3+ levels deep
   vs 2-level regex). Root node is unwrapped so structure view
   starts at domain level. Fragment files fall back to regex.

3. **Navigation** — `RiddlLib.getOutline()` replaces regex.
   AST-accurate definition finding. Fragment files fall back
   to regex.

Upgraded RIDDL 1.0.0 -> 1.8.0, Scala 3.4.3 -> 3.7.4,
sbt-ossuminc 1.2.5 -> 1.3.2. All 162 tests pass.

### 2026-01-21 — 0.9.0-beta Release

Released beta for testing. Created git tag `0.9.0-beta`.
Packaged as `RIDDL4IDEA-0.9.0-beta.zip`. 149 tests passing.

### 2026-01-19 through 2026-01-20 — Full Rewrite

Complete rewrite of the plugin over 4 phases:

- **Phase 1** (Foundation): Lexer adapter wrapping RIDDL
  tokenizer, syntax highlighter with 10 color attributes,
  file type recognition
- **Phase 2** (Editor): External annotator for async
  validation, code folding (23 definition types), brace
  matching, comment toggle
- **Phase 3** (Navigation): Structure view with hierarchy,
  context-aware completion (8 contexts, 30+ types), go-to-
  definition for 23 definition kinds
- **Phase 4** (AI): MCP server integration (JSON-RPC 2.0),
  4 MCP actions in Tools menu, Alt+Enter AI generation
  intention, validate-partial and check-completeness tools

Also: legacy code refactoring, performance test suite,
plugin verification, packaging.


---

## Open Questions

Moved to **BACKLOG.md** § Open questions, so decisions and open
work live in one place.
