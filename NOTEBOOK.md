# Engineering Notebook: RIDDL IntelliJ IDEA Plugin

## Current Status

**Version**: 0.9.0-beta (released 2026-01-21)
**Branch**: `main`
**RIDDL**: 1.8.0 | **Scala**: 3.7.4 | **sbt-ossuminc**: 1.3.2
**IntelliJ Platform**: 253.29346.240 (2025.3)
**Tests**: 162 passing across 11 suites

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

**Recommended version**: 0.10.0 (new features, no breaking
changes to user-facing behavior).

---

## Next Steps

### Near-Term (Ready to Do)

1. **Marketplace Submission**
   - Run plugin verifier (last check: 8 internal, 13
     experimental, 8 deprecated API usages — acceptable)
   - Create marketplace assets (icon, screenshots)
   - Submit for JetBrains review

2. **Test Coverage**
   - Enable coverage threshold (target 70%)
   - Scoverage doesn't instrument IntelliJ platform classes;
     may need to exclude those from reporting
   - Current unit tests are comprehensive for plugin logic

3. **Update README.md**
   - Add feature screenshots
   - Document MCP server setup
   - Installation instructions

### Medium-Term (Feature Work)

4. **Find Usages** — Reverse reference finding (complement to
   go-to-definition); RiddlLib.getOutline() could support this

5. **Performance Optimization**
   - Incremental parsing (don't re-parse on every keystroke)
   - Cache AST/token results, invalidate on document change
   - Debounce annotation (300-500ms after typing pause)

6. **Legacy Code Cleanup**
   - `utils/ParsingUtils.scala` — Uses `Thread.sleep()`,
     still calls `TopLevelParser.parseNebula()` directly
   - `files/utils.scala` — Deprecated manual highlighting
   - `settings/RiddlIdeaSettings.scala` — Window-scoped state
     model from prototype; should be project-scoped

7. **Hover Documentation** — Show definition info on hover
   (type signatures, descriptions from RIDDL model)

### Long-Term (Future Consideration)

8. **LSP Server** — Could serve VS Code, Cursor, Neovim from
   a single codebase; would replace/augment both IDE plugins

9. **Refactoring Support** — Safe rename, extract definition

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

1. **Marketplace Pricing** — Free or paid plugin?
2. **MCP Server Deployment** — Where will production server
   run for end users?
3. **RIDDL Version Tracking** — Plugin should track RIDDL
   releases; RiddlLib API may change in future versions
