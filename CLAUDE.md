# RIDDL IntelliJ IDEA Plugin - Claude Code Guide

This file provides specific guidance for working with the RIDDL IntelliJ IDEA
plugin. For general ossuminc organization patterns, common build system info,
Git workflow, and collaboration philosophy, see `../CLAUDE.md`.

## Project Overview

The RIDDL IntelliJ IDEA Plugin provides comprehensive IDE support for the RIDDL
(Reactive Interface to Domain Definition Language) language within JetBrains
IntelliJ IDEA. The goal is to create a **fully functional RIDDL development
assistant** that helps developers write, validate, and understand RIDDL models.

### Target Features

1. **Syntax Highlighting** - Token-based coloring using RIDDL parser
2. **Error Annotation** - Real-time error/warning underlines with messages
3. **Validation** - Syntax checking and semantic validation
4. **Navigation** - Go-to-definition, find usages, structure view
5. **Code Completion** - Keyword and context-aware suggestions
6. **AI Assistance** - Integration with riddl-mcp-server for model generation
7. **Refactoring** - Safe rename, extract, and restructure operations
8. **Documentation** - Hover documentation and quick info

### Current State

The existing implementation is a **prototype** with basic functionality:
- File type recognition (`.riddl` extension)
- Token-based syntax highlighting
- Basic error annotation from parser messages
- Tool window for running compiler commands
- Settings persistence

**Important**: The current codebase needs significant modernization. It was
contributed code and has architectural issues that may warrant a rewrite.
See `NOTEBOOK.md` for detailed assessment.

## Build Commands

```bash
# Compile the plugin
sbt compile

# Run tests
sbt test

# Package for local testing (creates ZIP file)
sbt packageArtifactZip

# Run IntelliJ with plugin loaded (for debugging)
sbt runIDE

# Publish to IntelliJ Marketplace
sbt publishPlugin

# Check code formatting
sbt scalafmtCheck

# Format code
sbt scalafmt
```

## Installation (Development)

After packaging with `sbt packageArtifactZip`:

1. Open IntelliJ IDEA
2. Go to Settings/Preferences → Plugins
3. Click the gear icon → "Install Plugin from Disk..."
4. Select the generated ZIP file from `target/scala-3.x/`
5. Restart IntelliJ IDEA

## Technology Stack

| Component | Technology | Notes |
|-----------|------------|-------|
| Language | Scala 3.6.x | JVM only (no cross-platform) |
| Build | sbt + sbt-ossuminc 1.2.0 | Standard ossuminc build |
| IDE SDK | IntelliJ Platform SDK | Currently 2024.2.x (243.x) |
| RIDDL API | riddl-commands 1.1.0 | Parser, AST, validation |
| Kotlin | stdlib 2.0.x | Required for IntelliJ interop |

## Project Structure

```
riddl-idea-plugin/
├── build.sbt                    # Main build configuration
├── project/
│   ├── plugins.sbt             # sbt plugins (sbt-idea-plugin)
│   └── Dependencies.scala       # Version constants
├── resources/
│   └── META-INF/plugin.xml     # Plugin manifest
├── src/main/scala/.../idea/
│   ├── actions/                # Toolbar and menu actions
│   ├── files/                  # File type, syntax highlighting
│   ├── project/                # Project initialization
│   ├── settings/               # Plugin settings UI and persistence
│   ├── ui/                     # Tool window, console UI
│   └── utils/                  # Parsing, highlighting utilities
└── src/test/scala/             # Tests (currently minimal)
```

## Key Extension Points

The plugin registers these IntelliJ extension points in `plugin.xml`:

| Extension | Class | Purpose |
|-----------|-------|---------|
| `fileType` | `RiddlFileType` | Register `.riddl` file extension |
| `annotator` | `RiddlAnnotator` | Error/warning inline marks |
| `colorSettingsPage` | `RiddlColorSettingsPage` | Theme customization |
| `applicationService` | `RiddlIdeaSettings` | Persistent state |
| `projectConfigurable` | `RiddlIdeaSettingsConfigurable` | Settings UI |
| `toolWindow` | `RiddlToolWindowFactory` | RIDDL compilation pane |
| `postStartupActivity` | `RiddlProject` | Initialization hook |

## RIDDL Integration Points

The plugin uses these RIDDL APIs from `riddl-language` and `riddl-commands`:

```scala
// Token-based parsing for syntax highlighting
TopLevelParser.parseToTokens(input, withVerboseFailures = false)

// Full parsing for validation
TopLevelParser.parseNebula(input)

// Command execution (about, info, from)
Commands.runCommandWithArgs(Array("from", confPath, option))
```

**Token Types** (from `com.ossuminc.riddl.language.AST.Token`):
- `Keyword` - RIDDL keywords (domain, context, entity, etc.)
- `Identifier` - User-defined names
- `Punctuation` - Braces, brackets, colons
- `Readability` - Optional words (is, of, for, etc.)
- `Predefined` - Built-in types (String, Integer, UUID, etc.)
- `Comment` - Line and block comments
- `QuotedString` - String literals
- `MarkdownLine` - Documentation strings
- `LiteralCode` - Embedded code blocks

## Development Guidelines

### IntelliJ Plugin Best Practices

1. **Use IntelliJ Platform APIs** - Don't reinvent functionality the SDK
   provides (file browsing, tree structures, tool windows)
2. **Threading** - Use `ApplicationManager.getApplication().invokeLater()`
   for UI updates from background threads
3. **Services** - Use application/project services for singletons
4. **Disposables** - Properly dispose resources when plugins/projects close
5. **Light Services** - Prefer `@Service` annotation over XML registration

### Testing Strategy

The plugin needs comprehensive testing:
- **Unit Tests** - Parser integration, token mapping, state management
- **Integration Tests** - Use IntelliJ test framework (`BasePlatformTestCase`)
- **UI Tests** - Use IntelliJ test automation for dialogs and tool windows

Target: 70% code coverage (per ossuminc standards for plugins)

### Performance Considerations

1. **Incremental Parsing** - Don't re-parse entire document on every keystroke
2. **Background Processing** - Use `ReadAction`/`WriteAction` appropriately
3. **Caching** - Cache AST and token results, invalidate on change
4. **Debouncing** - Delay parsing until typing pause (300-500ms)

## RIDDL Language Context

When working on this plugin, load these reference documents for RIDDL context:
- **EBNF Grammar**: `../ossum.tech/docs/riddl/references/ebnf-grammar.md`
- **Language Reference**: `../ossum.tech/docs/riddl/references/language-reference.md`

RIDDL is a reactive system design language with:
- Hierarchical structure: Domain → Context → Entity/Repository/Saga
- Event-driven patterns with commands, events, states, handlers
- Rich type system (predefined, aggregate, enumeration, union types)
- Streaming data flow with inlets, outlets, connectors

## Related Projects

| Project | Relationship |
|---------|--------------|
| `../riddl/` | Core compiler - provides parser, AST, passes |
| `../riddl-vscode/` | Similar plugin for VS Code (TypeScript) |
| `../riddl-mcp-server/` | AI integration for RIDDL assistance |
| `../synapify/` | Visual RIDDL editor (Electron + Scala.js) |

## Version Compatibility

- **IntelliJ IDEA**: 2024.1+ (build 241+)
- **RIDDL**: 1.1.0+ (update when released)
- **JDK**: 21+ (Temurin recommended)
- **Scala**: 3.6.x LTS

## Debugging Tips

1. **Run IDE**: Use `sbt runIDE` to launch a sandboxed IntelliJ with plugin
2. **Plugin Verifier**: Run before marketplace submission
3. **IDE Logs**: Help → Show Log in Finder/Explorer
4. **Debug Breakpoints**: Attach debugger to runIDE process
5. **Plugin DevKit**: Install IntelliJ Plugin DevKit for enhanced tooling