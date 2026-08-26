# Engineering Notebook: RIDDL IntelliJ IDEA Plugin

## HANDOFF

**State** (verified 2026-08-26 by running the commands, not recalled):

- Branch `main`. Build: riddl pinned to `2.0.0-rc.10-37-1d87a109`,
  IntelliJ SDK `261.26222.65`, descriptor `plugin.xml` at **0.11.0**
  with `since-build="261"`.
- `sbt test` → **174 passing, 11 suites, 0 failed**.
- **0.11.0 is released and public on JetBrains Marketplace.** Update
  `1130942` reports `approve: true, listed: true`; the public endpoint
  returns `200`. Tag `0.11.0`, GitHub release with the zip attached.
- **CI on `main` is red**, and expected to be: the pinned riddl exists
  only in `~/.ivy2/local`, which GitHub Actions does not have.

**In flight**

Nothing. The 0.11.0 release completed end to end and the tree is clean.

**Start here**

BACKLOG item 1, which changed character while nobody was looking.
riddl now publishes through **`2.0.0-rc.26`**, so moving off the
ivy-local pin goes *forward* 16 release candidates **and** turns CI
green. In August that swap cost the staged language work; it no longer
costs anything. Do it before judging anything else in the backlog.

**Traps a fresh session would fall into**

1. **Stale `target/` fakes a green build**, and a stale zip fakes a
   verifier verdict. `rm -rf target` after any dependency change.
   Full write-up below.
2. **`packageArtifactZip` fails while a sandbox IDE is running** —
   `No options search index built for plugin root`. Quit `runIDE`
   first. Below, trap 5.
3. **Do not "fix" two things that look wrong.** `RiddlLanguage` is a
   `class` with a companion holding `INSTANCE`, not an `object extends
   Language`; and `build.sbt` sets `-Xmixin-force-forwarders:false`.
   Reverting either reintroduces ~40 verifier API findings. See
   "All 40 verifier API notices were compiler-generated" below and
   CLAUDE.md § "Scala codegen vs. the Plugin Verifier".
4. **`idea.log` is years long and never truncated.** Mark the line
   count before a run and read only past it; two "findings" this
   August were dated 2025-01-13.
5. **This plugin logs nothing on the happy path.** No `ossuminc`
   frames in the log is not evidence it failed to load.
6. **`runPluginVerifier` verifies against `ideaIU` only** — it does
   not establish Community compatibility (BACKLOG item 4).

**Certainty**

- Verified 2026-08-26: git state, test count, the riddl pin still
  resolvable from `~/.ivy2/local`, riddl published through rc.26,
  `../bin/riddlc` at rc.26, Marketplace approved/listed, CI red.
- Assumed, not verified: that the plugin works on IDEA Community
  (BACKLOG 4); that the rc.26 upgrade is mechanical — 16 RCs is a lot
  of language change and nobody has assessed the delta for this
  consumer.

**Pointers**

- Open work → `BACKLOG.md`, all of it, with the evidence attached.
- Durable architecture and API facts → `CLAUDE.md`.
- Release procedure → `.claude/skills/ship/`.

**`task/` holds no untriaged items** — it contains only `done/` (three
files). Notably no upgrade task ever arrived for rc.11..rc.26. Run
`/ossuminc-skills:check-tasks` in the new session anyway; that call is
the driver's, not the handoff's.

---

## All 40 verifier API notices were compiler-generated (2026-08-08)

JetBrains Marketplace review flagged the 0.10.0 submission with
**8 internal + 12 scheduled-for-removal + 9 deprecated + 11
experimental** API usages. The instinct — go find those calls and
replace them per the Internal API Migration table — was wrong.

**Not one of them existed in `src/`.** `grep -rn` across the whole
source tree for `isApplicable`, `isDoNotActivateOnStart`, `getAnchor`,
`manage`, `fileOpenedSync`, `navigationRequest`, `getDisplayNameFast`
returns **zero hits**. `RiddlLanguage.scala` was 17 lines defining a
single `val`, yet the verifier reported it using
`Language.findMyFileType`, `Language.unregisterLanguage`,
`Language.unregisterDialect` and `UserDataHolderBase.getUserMap`.

The verifier reads **bytecode**, and Scala 3 puts methods in the class
file that are not in the source. Two separate generators:

1. **Mixin forwarders.** For a Scala class implementing a Java
   interface, Scala emits a concrete override of each `default` method
   that just calls the interface implementation. So
   `RiddlIdeaSettingsConfigurable extends Configurable` produced
   `getDisplayNameFast()` plus three `isModified(...)` overloads —
   which the verifier reads as *both* "is overridden in" *and*
   "is invoked in", which is why each finding appeared twice.
   Fixed with `Compile / scalacOptions += "-Xmixin-force-forwarders:false"`.
2. **Static forwarders on an object's mirror class.** `object Foo
   extends SomeJavaClass` generates a `Foo.class` carrying a `public
   static` forwarder for *every* inherited public method — 28 of them
   for `object RiddlLanguage extends Language("RIDDL")`. No scalac flag
   suppresses these; the object had to become
   `final class RiddlLanguage private () extends Language("RIDDL")`
   with `object RiddlLanguage { val INSTANCE = ... }`, and the 14 call
   sites updated to `RiddlLanguage.INSTANCE`.

Result: verdict went from "Compatible. 12 usages of scheduled for
removal API and 8 usages of deprecated API. 11 usages of experimental
API. 8 usages of internal API" to plain **`Compatible`**, with the
verifier emitting no `*-usages.txt` files at all.

**The general lesson: a bytecode-level report about a JVM language
that generates code is not a report about your source.** Before
chasing any such finding, `grep` the method name. Zero hits means the
compiler wrote it, and the fix is in the build or in the shape of the
declaration — never at a call site that does not exist.

**Genuinely hand-written, and separately fixed:** exactly one —
`new TerminalExecutionConsole(project, processHandler)` in
`utils/ToolWindowUtils.scala`. Its constructor is deprecated as of
2026.1 in favour of `TerminalExecutionConsoleBuilder(project).build()`
followed by `attachToProcess(handler)` (identical semantics; the old
constructor called `attachToProcess` itself). This one was caught by
**scalac**, not the verifier, and because the build runs `-Werror` it
was a hard compile error — i.e. **the SDK bump to 2026.1 does not
build until it is fixed**.

### Trap: the verifier will happily check a stale zip

After fixing `RiddlLanguage`, `javap` confirmed the mirror class was
down to one `public static` — and `runPluginVerifier` still reported
`RiddlLanguage.getUserMap()`. It had re-verified a previously packaged
artifact. `rm -rf target` then re-run; only then did the verdict change.
This is the same stale-`target/` trap recorded below, in a new costume:
**a verifier verdict is evidence only if the run began from a clean
`target/`.**

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

**Version**: 0.11.0 (released 2026-08-08)
**Branch**: `main`
**RIDDL**: 2.0.0-rc.10-37-1d87a109 | **Scala**: 3.9.0-RC4
**sbt-ossuminc**: 1.4.0
**sbt**: 1.12.3 | **sbt-idea-plugin**: 6.0.0-RC3
**IntelliJ Platform**: 261.26222.65 (2026.1)
**Tests**: 174 passing across 11 suites

All core features implemented. **Published and publicly listed on
JetBrains Marketplace** (plugin id 33312) as of 0.11.0.
RiddlLib integration complete (2026-02-11).

---

## Changes Since 0.11.0

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
