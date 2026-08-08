# RIDDL IntelliJ IDEA Plugin - Claude Code Guide

This file provides specific guidance for working with the RIDDL
IntelliJ IDEA plugin. For general ossuminc organization patterns,
common build system info, Git workflow, and collaboration
philosophy, see `../CLAUDE.md`.

## Project Overview

The RIDDL IntelliJ IDEA Plugin provides comprehensive IDE support
for RIDDL (Reactive Interface to Domain Definition Language) within
JetBrains IntelliJ IDEA. It is a **fully functional RIDDL
development assistant** that helps developers write, validate, and
understand RIDDL models with AI assistance via riddl-mcp-server.

**Version**: 0.9.0-beta (released 2026-01-21)
**Branch**: `main`

### Implemented Features

| Feature | Mechanism | Status |
|---------|-----------|--------|
| Syntax Highlighting | Lexer wrapping RIDDL tokenizer | Done |
| Error Annotation | RiddlLib.validateString() | Done |
| Code Folding | Regex-based region detection | Done |
| Brace Matching | {}, (), [], <> pairs | Done |
| Comment Toggle | Cmd+/ for // and /* */ | Done |
| Structure View | RiddlLib.getTree() + regex fallback | Done |
| Code Completion | Context-aware keywords + types | Done |
| Go-to-Definition | RiddlLib.getOutline() + regex fallback | Done |
| MCP Integration | JSON-RPC 2.0 to riddl-mcp-server | Done |
| AI Generation | Alt+Enter intention action | Done |
| Tool Window | Compiler command runner + console | Done |

### Not Yet Implemented

- Find usages (reverse references)
- Safe rename / refactoring
- Hover documentation / quick info
- Incremental parsing / caching
- LSP server (future consideration)

## Build Commands

```bash
sbt compile            # Compile the plugin
sbt test               # Run tests (174 tests, 11 suites)
sbt packageArtifactZip # Package ZIP for local install
sbt runIDE             # Launch sandboxed IntelliJ with plugin
sbt publishPlugin      # Publish to IntelliJ Marketplace
sbt scalafmtCheck      # Check code formatting
sbt scalafmt           # Format code
```

## Installation (Development)

After packaging with `sbt packageArtifactZip`:

1. Open IntelliJ IDEA
2. Settings/Preferences -> Plugins
3. Gear icon -> "Install Plugin from Disk..."
4. Select the generated ZIP from `target/`
5. Restart IntelliJ IDEA

## Technology Stack

| Component | Technology | Version |
|-----------|------------|---------|
| Language | Scala 3 (JVM only) | 3.9.0-RC4 |
| Build | sbt | 1.12.3 |
| Build | sbt-ossuminc | 1.4.0 |
| Build | sbt-idea-plugin | 6.0.0-RC3 |
| IDE SDK | IntelliJ Platform | 261.26222.65 |
| RIDDL | riddl-commands + riddl-lib | 2.0.0-rc.10 |
| Kotlin | stdlib (IntelliJ interop) | 2.0.20 |
| Tests | ScalaTest | 3.2.19 |

**The build cannot move to sbt 2.0** — `sbt-idea-plugin` has no sbt 2
artifact, and sbt-ossuminc 3.x is sbt 2 only. See NOTEBOOK.md
"Blocker: sbt 2.0 / sbt-ossuminc 3.x".

## Project Structure

```
riddl-idea-plugin/
├── build.sbt
├── project/
│   ├── plugins.sbt
│   └── Dependencies.scala
├── resources/
│   └── META-INF/plugin.xml       # 24 extension points
├── src/main/scala/.../plugins/idea/
│   ├── RiddlLanguage.scala        # Language singleton
│   ├── RiddlIcons.scala           # Icon loader
│   ├── actions/                   # Toolbar/menu actions
│   ├── annotator/                 # Error annotation
│   ├── completion/                # Code completion
│   ├── editor/                    # Brace matching, commenter
│   ├── files/                     # File type, legacy utils
│   ├── folding/                   # Code folding
│   ├── highlighting/              # Syntax highlighter
│   ├── intentions/                # AI generation intention
│   ├── lexer/                     # Token types, lexer adapter
│   ├── mcp/                       # MCP client, service, actions
│   ├── navigation/                # Go-to-definition
│   ├── project/                   # Startup activity
│   ├── settings/                  # Settings UI + persistence
│   ├── structure/                 # Structure view
│   ├── ui/                        # Tool window, console
│   └── utils/                     # Parsing, tool window utils
└── src/test/scala/.../plugins/idea/
    ├── annotator/                 # Annotator tests
    ├── completion/                # Completion tests
    ├── editor/                    # Brace/commenter tests
    ├── folding/                   # Folding tests
    ├── highlighting/              # Highlighting tests
    ├── lexer/                     # Lexer tests
    ├── mcp/                       # MCP integration tests
    ├── navigation/                # Navigation tests
    ├── performance/               # Performance benchmarks
    └── structure/                 # Structure view tests
```

## Registered Extension Points (plugin.xml)

| Extension | Class | Purpose |
|-----------|-------|---------|
| `fileType` | RiddlFileType | `.riddl` file registration |
| `lang.syntaxHighlighterFactory` | RiddlSyntaxHighlighterFactory | Token coloring |
| `colorSettingsPage` | RiddlColorSettingsPage | Theme customization |
| `externalAnnotator` | RiddlExternalAnnotator | Async error/warning marks |
| `lang.foldingBuilder` | RiddlFoldingBuilder | Collapsible regions |
| `lang.braceMatcher` | RiddlBraceMatcher | Brace pair highlighting |
| `lang.commenter` | RiddlCommenter | Comment toggle (Cmd+/) |
| `lang.psiStructureViewFactory` | RiddlStructureViewFactory | Definition hierarchy |
| `completion.contributor` | RiddlCompletionContributor | Keyword/type completion |
| `gotoDeclarationHandler` | RiddlGotoDeclarationHandler | Cmd+Click navigation |
| `applicationService` | RiddlIdeaSettings | Persistent state |
| `projectConfigurable` | RiddlIdeaSettingsConfigurable | Settings UI |
| `projectService` | RiddlMcpService | MCP server connection |
| `intentionAction` | GenerateFromDescriptionIntention | AI generation |
| `toolWindow` | RiddlToolWindowFactory | Compiler output pane |
| `postStartupActivity` | RiddlProject | Document listener setup |
| `notificationGroup` | (balloon) | Plugin notifications |

Plus 4 MCP actions in Tools > RIDDL MCP menu and 2 project
listeners (FileEditorManagerListener, DocumentListener).

## RIDDL API Integration

The plugin uses two RIDDL libraries: `riddl-language` for
tokenization and `riddl-lib` for high-level validation, structure,
and navigation.

### RiddlLib (riddl-lib 2.0.0-rc.7) — High-Level API

As of riddl 2.0 these return a `RiddlResult` ADT rather than
`Either[Messages, T]`. Match on `RiddlResult.Success(value)` and
`RiddlResult.Failure`, whose `errors` replaces the old `Left`
payload.

```scala
import com.ossuminc.riddl.{RiddlLib, RiddlResult}
import com.ossuminc.riddl.utils.{NullLogger, pc}

// Full semantic validation (parse + resolution + validation)
// Returns ValidateResult with categorized messages
pc.withLogger(NullLogger()) { _ =>
  val vr = RiddlLib.validateString(source, origin)(using pc)
  // vr.parseErrors, vr.errors, vr.warnings, vr.info, vr.all
}

// Fragment parsing (tolerates partial definitions)
RiddlLib.parseNebula(source, origin)(using pc)
// Returns RiddlResult[Nebula]

// Recursive definition tree for structure view
RiddlLib.getTree(source, origin)(using pc)
// Returns RiddlResult[Seq[TreeNode]]
// TreeNode(kind, id, line, col, offset, children)

// Flat outline for navigation
RiddlLib.getOutline(source, origin)(using pc)
// Returns RiddlResult[Seq[OutlineEntry]]
// OutlineEntry(kind, id, depth, line, col, offset)
```

Note that `TopLevelParser.parseNebula` (riddl-language) still
returns `Either` — only the `RiddlLib` facade changed.

`RiddlParserInput.fromPath` is deprecated in 2.0; use
`fromPathSafe`, which returns
`Future[Either[Messages, RiddlParserInput]]` and reports load
failures as messages instead of throwing.

**Fallback pattern**: All features that use RiddlLib try the
AST-based API first. If parsing fails (e.g., include-fragment
files that aren't complete Root documents), they fall back to
regex-based implementations.

### TopLevelParser (riddl-language) — Tokenization

```scala
import com.ossuminc.riddl.language.parsing.TopLevelParser

// Token-based parsing for syntax highlighting (via lexer)
TopLevelParser.parseToTokens(rpi, withVerboseFailures = false)
```

### Commands (riddl-commands) — CLI Execution

```scala
import com.ossuminc.riddl.commands.Commands

// Tool window command execution (about, info, from)
Commands.runCommandWithArgs(Array("from", confPath, option))
```

### Token Types

From `com.ossuminc.riddl.language.AST.Token`:

| Token | Description | Example |
|-------|-------------|---------|
| Keyword | RIDDL keywords | domain, context, entity |
| Identifier | User-defined names | MyDomain, userId |
| Punctuation | Delimiters | { } ( ) [ ] |
| Readability | Optional words | is, of, for, with |
| Predefined | Built-in types | String, Integer, UUID |
| Comment | Comments | // or /* */ |
| QuotedString | String literals | "hello" |
| MarkdownLine | Doc strings | description text |
| LiteralCode | Embedded code | code blocks |
| Numeric | Numbers | 42, 3.14 |
| Other | Unrecognized | fallback |

## Development Guidelines

### IntelliJ Plugin Best Practices

1. **Use IntelliJ Platform APIs** — Don't reinvent what the SDK
   provides (file browsing, tree structures, tool windows)
2. **Threading** — Use `invokeLater()` for UI updates from
   background threads; use ExternalAnnotator for slow operations
3. **Services** — Application services for global state, project
   services for per-project state
4. **Disposables** — Properly dispose resources on plugin/project
   close

### Scala codegen vs. the Plugin Verifier

The Plugin Verifier reads **bytecode**, and Scala 3 emits methods that
are not in the source. Both of these below produced verifier findings
naming methods that appear nowhere in `src/`. Keep both rules or they
come straight back — see NOTEBOOK.md "All 40 verifier API notices were
compiler-generated" (2026-08-08).

1. **Never write `object Foo extends <JavaClass>`.** An object's mirror
   class carries a `public static` forwarder for every inherited public
   method — including `@ApiStatus.Internal` ones. Use a `final class`
   with a private constructor plus a companion holding `INSTANCE`, as
   `RiddlLanguage` does.
2. **`build.sbt` sets `-Xmixin-force-forwarders:false`.** Without it,
   every Java interface `default` method a Scala class inherits becomes
   a generated override, which the verifier counts as a usage.

Before chasing any verifier finding, `grep` the method name in `src/`.
Zero hits means the compiler wrote it and no call site exists to fix.

### Testing Strategy

- **174 tests** across 11 suites, all passing
- Unit tests cover lexer, completion, navigation, structure,
  folding, annotation, brace matching, MCP data classes,
  performance
- Tests use ScalaTest `AnyWordSpec` + `Matchers`
- Coverage threshold: 0% in build (IntelliJ platform classes
  not instrumentable by Scoverage); target 70% for plugin code
- Performance tests validate tokenization/parsing at scale
  (up to 500 entities, 200KB files)

### Performance Considerations

1. **Async Annotation** — ExternalAnnotator runs on background
   thread, doesn't block UI
2. **Lexer Wrap** — Single-pass tokenization via RIDDL parser
3. **Fallback Pattern** — Regex fallback is fast for fragments
4. **Future**: Incremental parsing, caching, debouncing not yet
   implemented

### Legacy Code

The `utils/` and `files/` packages contain legacy code from the
original prototype (pre-rewrite). Notable items:

- `files/utils.scala` — Deprecated manual highlighting; replaced
  by lexer-based `RiddlSyntaxHighlighter`
- `utils/ParsingUtils.scala` — Tool window command execution;
  still uses `TopLevelParser.parseNebula()` and `Thread.sleep()`
- `settings/RiddlIdeaSettings.scala` — Window-scoped state model
  from prototype; works but architecturally questionable

These are functional but candidates for future cleanup.

## RIDDL Language Context

When working on this plugin, load these reference documents:
- **EBNF Grammar**: `../ossum.tech/docs/riddl/references/ebnf-grammar.md`
- **Language Reference**: `../ossum.tech/docs/riddl/references/language-reference.md`

RIDDL is a reactive system design language with:
- Hierarchical structure: Domain -> Context -> Entity/Repository/Saga
- Event-driven patterns with commands, events, states, handlers
- Rich type system (predefined, aggregate, enumeration, union)
- Streaming data flow with inlets, outlets, connectors

## Related Projects

| Project | Relationship |
|---------|--------------|
| `../riddl/` | Core compiler — parser, AST, passes, RiddlLib |
| `../riddl-vscode/` | Similar plugin for VS Code (TypeScript) |
| `../riddl-mcp-server/` | AI integration for RIDDL assistance |
| `../synapify/` | Visual RIDDL editor (Electron + Scala.js) |

## Version Compatibility

- **IntelliJ IDEA**: 2026.1+ (build 261+)
- **RIDDL**: 2.0.0-rc.10
- **JDK**: 21+ (Temurin recommended)
- **Scala**: 3.9.0-RC4

## Debugging Tips

1. **Run IDE**: `sbt runIDE` launches sandboxed IntelliJ
2. **Plugin Verifier**: Run before marketplace submission
3. **IDE Logs**: Help -> Show Log in Finder/Explorer
4. **Debug Breakpoints**: Attach debugger to runIDE process
5. **Plugin DevKit**: Install for enhanced plugin tooling
