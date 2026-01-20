# Engineering Notebook: RIDDL IntelliJ IDEA Plugin

## Current Status

**Assessment Complete** - Comprehensive code review finished. The existing
implementation is a prototype requiring significant modernization before
open source release. Decision: **REWRITE** using prototype as reference.

**Target**: Marketplace release in **2 months** (mid-March 2026)

**Primary Goal**: Create a fully functional RIDDL development assistant that
helps developers write, validate, and understand RIDDL models with AI
assistance via riddl-mcp-server integration.

---

## Key Architecture Decisions

### Decision 1: Wrap RIDDL Tokenization (Not Separate Lexer)

**Rationale**: RIDDL already provides `TopLevelParser.parseToTokens` and
`mapTextAndToken` APIs specifically designed for IDE tooling. The VS Code
extension uses these successfully.

**Implementation**:
```scala
class RiddlLexerAdapter extends Lexer {
  // Wraps TopLevelParser.parseToTokens()
  // Returns IElementType for each RIDDL Token type
}
```

**Benefits**:
- Single source of truth for tokenization
- Guaranteed consistency with VS Code plugin
- Automatic updates when RIDDL grammar changes
- ~1 day implementation vs ~2 weeks for separate lexer
- No ongoing maintenance burden

**Trade-off Accepted**: Lose IntelliJ's mid-document restart optimization.
Acceptable because RIDDL has structural dependencies preventing partial
parsing anyway.

### Decision 2: Project-Level Services (Not Window-Based State)

**Rationale**: The prototype's per-window state model is confusing and
doesn't match how developers think about projects.

**Implementation**:
- `RiddlProjectService` - Per-project configuration and state
- `RiddlApplicationService` - Global settings (MCP server URL, etc.)
- Editor features work on current file, not "window number"

### Decision 3: LSP Consideration for Future

**Rationale**: An LSP server could serve VS Code, Cursor, Neovim, and other
editors from a single codebase.

**Current Plan**: Native IntelliJ plugin for best UX in JetBrains ecosystem.
Consider LSP server as future project to replace/augment both plugins.

**LSP Compatibility Note**: VS Code and Cursor both have first-class LSP
support. An LSP server would work identically in both.

### Decision 4: AI Integration via riddl-mcp-server

**Rationale**: The riddl-mcp-server provides validation, completeness
checking, and domain mapping tools designed for AI-assisted RIDDL development.

**Priority Tools**:
1. `validate-partial` - Validate incomplete models during development
2. `map-domain-to-riddl` - Convert natural language to RIDDL structure
3. `check-completeness` - Report what's missing for model completion
4. `check-simulability` - Validate model can run in riddlsim

---

## Codebase Assessment Summary

### Statistics

| Metric | Value |
|--------|-------|
| Scala Source Files | 27 |
| Lines of Code | ~1,800 |
| Test Coverage | ~5% (target: 70%) |
| RIDDL Version | 1.0.0-RC6 → needs 1.1.0 |
| sbt-ossuminc Version | 0.21.0 → needs 1.2.0 |

### Critical Defects

| ID | Issue | Impact |
|----|-------|--------|
| D1 | Thread safety - mutable state without sync | Race conditions |
| D2 | Null pointer risks - unchecked getVirtualFile | NPE crashes |
| D3 | Memory leaks - highlighter refs never cleaned | IDE slowdown |
| D4 | Blocking UI - Thread.sleep() on EDT | UI freezes |
| D5 | Single project assumption | Multi-project failure |

### Architectural Problems

1. **No PSI** - Can't support completion, navigation, refactoring
2. **Manual highlighting** - Should use Lexer + SyntaxHighlighter
3. **Mutable global state** - 15+ fields, no thread safety
4. **Window-based architecture** - Confusing UX, not project-centric

### What to Preserve from Prototype

- Token type → color mappings
- RIDDL parser integration patterns
- Tool window UI concepts (refined)
- Icon assets and color schemes
- Settings persistence keys (backward compatibility)

---

## Conversion Plan: Prototype to Production

### Timeline Overview (8 Weeks)

```
Week 1-2: Foundation
  ├─ Project setup, dependencies, CI/CD
  ├─ Lexer adapter wrapping RIDDL tokenization
  ├─ SyntaxHighlighter with color mappings
  └─ Basic file type recognition

Week 3-4: Editor Features
  ├─ Error annotation (ExternalAnnotator)
  ├─ Code folding for definitions
  ├─ Brace matching and commenter
  └─ 40% test coverage milestone

Week 5-6: Navigation & Intelligence
  ├─ Structure view (definition hierarchy)
  ├─ Basic code completion (keywords, types)
  ├─ Go-to-definition for references
  └─ 60% test coverage milestone

Week 7: AI Integration
  ├─ riddl-mcp-server connection
  ├─ Validation tool integration
  ├─ "Generate from description" intention
  └─ Completeness indicators

Week 8: Polish & Release
  ├─ Settings UI modernization
  ├─ Documentation and README
  ├─ 70% test coverage achieved
  └─ Marketplace submission
```

### Phase 1: Foundation (Weeks 1-2)

#### 1.1 Project Setup

**Tasks**:
- [ ] Update build.sbt: sbt-ossuminc 1.2.0, RIDDL 1.1.0
- [ ] Verify RIDDL 1.1.0 API compatibility (breaking changes)
- [ ] Configure test infrastructure (ScalaTest + IntelliJ fixtures)
- [ ] Enable CI/CD test execution (currently commented out)
- [ ] Set up code coverage reporting

**Dependencies**:
```scala
// project/Dependencies.scala
object V {
  val riddl = "1.1.0"
  val scalatest = "3.2.19"
}

object Dep {
  val riddlCommands = "com.ossuminc" %% "riddl-commands" % V.riddl
}
```

#### 1.2 Language Foundation

**Tasks**:
- [ ] Create `RiddlLanguage` singleton
- [ ] Create `RiddlFileType` with icon
- [ ] Register in plugin.xml

**Files**:
```
src/main/scala/.../
├── RiddlLanguage.scala      # Language singleton
├── RiddlFileType.scala      # File type definition
└── RiddlIcons.scala         # Icon loader
```

#### 1.3 Lexer Adapter

**Tasks**:
- [ ] Create `RiddlTokenTypes` object with IElementType per token kind
- [ ] Implement `RiddlLexerAdapter` wrapping `parseToTokens`
- [ ] Create `RiddlSyntaxHighlighter` mapping tokens to colors
- [ ] Create `RiddlSyntaxHighlighterFactory`
- [ ] Write lexer tests with sample RIDDL files

**Token Type Mapping**:
```scala
object RiddlTokenTypes {
  val FILE = IFileElementType(RiddlLanguage)

  // From RIDDL Token types
  val KEYWORD     = IElementType("KEYWORD", RiddlLanguage)
  val IDENTIFIER  = IElementType("IDENTIFIER", RiddlLanguage)
  val READABILITY = IElementType("READABILITY", RiddlLanguage)
  val PUNCTUATION = IElementType("PUNCTUATION", RiddlLanguage)
  val PREDEFINED  = IElementType("PREDEFINED", RiddlLanguage)
  val COMMENT     = IElementType("COMMENT", RiddlLanguage)
  val STRING      = IElementType("STRING", RiddlLanguage)
  val MARKDOWN    = IElementType("MARKDOWN", RiddlLanguage)
  val LITERAL     = IElementType("LITERAL", RiddlLanguage)
  val NUMERIC     = IElementType("NUMERIC", RiddlLanguage)
  val OTHER       = IElementType("OTHER", RiddlLanguage)
}
```

**Color Mapping** (preserve from prototype):
```scala
object RiddlColors {
  val KEYWORD     = createKey("RIDDL_KEYWORD", KEYWORD)
  val IDENTIFIER  = createKey("RIDDL_IDENTIFIER", CLASS_NAME)
  val READABILITY = createKey("RIDDL_READABILITY", METADATA)
  val PUNCTUATION = createKey("RIDDL_PUNCTUATION", OPERATION_SIGN)
  val PREDEFINED  = createKey("RIDDL_PREDEFINED", CONSTANT)
  val COMMENT     = createKey("RIDDL_COMMENT", BLOCK_COMMENT)
  val STRING      = createKey("RIDDL_STRING", STRING)
  val MARKDOWN    = createKey("RIDDL_MARKDOWN", DOC_COMMENT)
  val NUMERIC     = createKey("RIDDL_NUMERIC", NUMBER)
}
```

#### 1.4 Milestone: Basic Highlighting Working

**Acceptance Criteria**:
- [ ] `.riddl` files recognized with RIDDL icon
- [ ] Keywords colored distinctly (pink/magenta)
- [ ] Identifiers colored as class names
- [ ] Comments colored as comments
- [ ] Strings colored as strings
- [ ] All prototype test files highlight correctly
- [ ] 20% test coverage

### Phase 2: Editor Features (Weeks 3-4)

#### 2.1 Error Annotation

**Tasks**:
- [ ] Implement `RiddlExternalAnnotator` for async validation
- [ ] Use `TopLevelParser.parseNebula()` for syntax validation
- [ ] Map RIDDL `Messages` to IntelliJ annotations
- [ ] Support Error, Warning, Info severity levels
- [ ] Add squiggly underlines with hover messages
- [ ] Add error stripe markers

**Implementation**:
```scala
class RiddlExternalAnnotator extends ExternalAnnotator[PsiFile, List[Message]] {
  override def collectInformation(file: PsiFile): PsiFile = file

  override def doAnnotate(file: PsiFile): List[Message] = {
    val text = file.getText
    val rpi = RiddlParserInput(text, file.getVirtualFile.getPath)
    TopLevelParser.parseNebula(rpi) match {
      case Left(messages) => messages.toList
      case Right(_) => Nil
    }
  }

  override def apply(file: PsiFile, messages: List[Message], holder: AnnotationHolder): Unit = {
    messages.foreach { msg =>
      val severity = msg.kind.severity match {
        case Error.severity   => HighlightSeverity.ERROR
        case Warning.severity => HighlightSeverity.WARNING
        case _                => HighlightSeverity.INFORMATION
      }
      holder.newAnnotation(severity, msg.format)
        .range(TextRange.create(msg.loc.offset, msg.loc.endOffset))
        .create()
    }
  }
}
```

#### 2.2 Code Folding

**Tasks**:
- [ ] Implement `RiddlFoldingBuilder`
- [ ] Fold definitions: domain, context, entity, handler, type, etc.
- [ ] Show placeholder text (e.g., `domain Foo {...}`)

**Foldable Regions**:
- `domain Name is { ... }` → `domain Name {...}`
- `context Name is { ... }` → `context Name {...}`
- `entity Name is { ... }` → `entity Name {...}`
- `handler Name is { ... }` → `handler Name {...}`
- `type Name is { ... }` → `type Name {...}`
- `/* ... */` → `/*...*/`

#### 2.3 Brace Matching & Commenter

**Tasks**:
- [ ] Implement `RiddlBraceMatcher` for `{}`, `()`, `[]`, `<>`
- [ ] Implement `RiddlCommenter` for `//` line comments
- [ ] Auto-insert closing braces/quotes

#### 2.4 Milestone: Pleasant Editing Experience

**Acceptance Criteria**:
- [ ] Errors underlined with red squiggles
- [ ] Warnings underlined with yellow squiggles
- [ ] Hover shows error message
- [ ] Code folds for all major definitions
- [ ] Brace matching highlights pairs
- [ ] Cmd+/ toggles line comments
- [ ] 40% test coverage

### Phase 3: Navigation & Intelligence (Weeks 5-6)

#### 3.1 Structure View

**Tasks**:
- [ ] Implement `RiddlStructureViewFactory`
- [ ] Create `RiddlStructureViewModel` showing definition hierarchy
- [ ] Icons for each definition type
- [ ] Click to navigate to definition

**Hierarchy Display**:
```
📁 Domain: MyDomain
  📁 Context: MyContext
    📦 Entity: User
      📋 State: UserState
      ⚡ Handler: UserHandler
    📦 Entity: Order
    🗄️ Repository: UserRepo
  📁 Context: OtherContext
```

#### 3.2 Basic Code Completion

**Tasks**:
- [ ] Implement `RiddlCompletionContributor`
- [ ] Keyword completion based on context
- [ ] Predefined type completion
- [ ] Definition name completion (entities, types in scope)

**Completion Contexts**:
- After `is` → suggest `{` or type keywords
- At definition start → suggest `domain`, `context`, `entity`, etc.
- After type reference → suggest known type names
- After `of` → suggest type names

#### 3.3 Go-to-Definition

**Tasks**:
- [ ] Implement reference resolution for type references
- [ ] Implement reference resolution for entity references
- [ ] Register `GotoDeclarationHandler`
- [ ] Cmd+Click navigates to definition

#### 3.4 Milestone: IDE-Quality Navigation

**Acceptance Criteria**:
- [ ] Structure view shows full definition hierarchy
- [ ] Keywords autocomplete contextually
- [ ] Type names autocomplete
- [ ] Cmd+Click goes to definition
- [ ] 60% test coverage

### Phase 4: AI Integration (Week 7)

#### 4.1 MCP Server Connection

**Tasks**:
- [ ] Create `RiddlMcpService` for server communication
- [ ] Settings UI for MCP server URL configuration
- [ ] Connection status indicator in tool window
- [ ] Health check and reconnection logic

**Settings**:
```scala
class RiddlMcpSettings {
  var serverUrl: String = "http://localhost:8080"
  var apiKey: String = ""
  var autoConnect: Boolean = true
}
```

#### 4.2 Validation Integration

**Tasks**:
- [ ] Call `validate-partial` for incomplete model validation
- [ ] Call `check-completeness` to show missing elements
- [ ] Display results in Problems tool window
- [ ] Filter "expected but not found" resolution errors

#### 4.3 AI-Assisted Generation

**Tasks**:
- [ ] "Generate RIDDL from Description" intention action
- [ ] Calls `map-domain-to-riddl` tool
- [ ] Insert generated code at cursor
- [ ] Preview dialog before insertion

**UI Flow**:
1. User selects text or places cursor
2. Alt+Enter → "Generate RIDDL from description..."
3. Dialog: Enter description of what to generate
4. Preview generated RIDDL
5. Insert or cancel

#### 4.4 Milestone: AI-Powered Development

**Acceptance Criteria**:
- [ ] MCP server connects automatically
- [ ] validate-partial shows semantic errors
- [ ] "Generate RIDDL" intention works
- [ ] Completeness hints in editor

### Phase 5: Polish & Release (Week 8)

#### 5.1 Settings UI

**Tasks**:
- [ ] Modernize settings using Kotlin UI DSL or declarative
- [ ] Per-project configuration (top-level file, conf file)
- [ ] Application-level settings (MCP server, colors)
- [ ] Remove window-based state concept

#### 5.2 Documentation

**Tasks**:
- [ ] README with screenshots and feature list
- [ ] CONTRIBUTING.md for contributors
- [ ] CHANGELOG.md for version history
- [ ] Plugin description for marketplace

#### 5.3 Testing & Coverage

**Tasks**:
- [ ] Achieve 70% code coverage
- [ ] Integration tests for all features
- [ ] UI tests for dialogs and tool windows
- [ ] Performance tests with large RIDDL files

#### 5.4 Marketplace Submission

**Tasks**:
- [ ] Run plugin verifier
- [ ] Create marketplace assets (icon, screenshots)
- [ ] Write marketplace description
- [ ] Submit for review

**Acceptance Criteria**:
- [ ] All features working
- [ ] 70% test coverage
- [ ] No plugin verifier errors
- [ ] Marketplace submission accepted

---

## File Structure (Target)

```
riddl-idea-plugin/
├── build.sbt
├── project/
│   ├── plugins.sbt
│   └── Dependencies.scala
├── resources/
│   └── META-INF/
│       └── plugin.xml
├── src/main/scala/com/ossuminc/riddl/plugins/idea/
│   ├── RiddlLanguage.scala
│   ├── RiddlFileType.scala
│   ├── RiddlIcons.scala
│   ├── lexer/
│   │   ├── RiddlTokenTypes.scala
│   │   ├── RiddlLexerAdapter.scala
│   │   └── RiddlParserDefinition.scala
│   ├── highlighting/
│   │   ├── RiddlColors.scala
│   │   ├── RiddlSyntaxHighlighter.scala
│   │   └── RiddlColorSettingsPage.scala
│   ├── annotator/
│   │   └── RiddlExternalAnnotator.scala
│   ├── folding/
│   │   └── RiddlFoldingBuilder.scala
│   ├── editor/
│   │   ├── RiddlBraceMatcher.scala
│   │   ├── RiddlCommenter.scala
│   │   └── RiddlQuoteHandler.scala
│   ├── structure/
│   │   ├── RiddlStructureViewFactory.scala
│   │   └── RiddlStructureViewModel.scala
│   ├── completion/
│   │   └── RiddlCompletionContributor.scala
│   ├── navigation/
│   │   ├── RiddlReference.scala
│   │   └── RiddlGotoDeclarationHandler.scala
│   ├── mcp/
│   │   ├── RiddlMcpService.scala
│   │   ├── RiddlMcpClient.scala
│   │   └── RiddlMcpSettings.scala
│   ├── intentions/
│   │   └── GenerateFromDescriptionIntention.scala
│   ├── settings/
│   │   ├── RiddlProjectSettings.scala
│   │   ├── RiddlApplicationSettings.scala
│   │   └── RiddlSettingsConfigurable.scala
│   └── toolwindow/
│       ├── RiddlToolWindowFactory.scala
│       └── RiddlToolWindowPanel.scala
├── src/test/scala/com/ossuminc/riddl/plugins/idea/
│   ├── lexer/
│   │   └── RiddlLexerTest.scala
│   ├── highlighting/
│   │   └── RiddlHighlightingTest.scala
│   ├── annotator/
│   │   └── RiddlAnnotatorTest.scala
│   ├── completion/
│   │   └── RiddlCompletionTest.scala
│   └── fixtures/
│       ├── everything.riddl
│       ├── syntax-error.riddl
│       └── complete-domain.riddl
└── src/test/resources/
    └── testData/
        └── highlighting/
```

---

## Risk Mitigation

| Risk | Mitigation |
|------|------------|
| RIDDL 1.1.0 breaking changes | Test compatibility early in Week 1 |
| MCP server not ready | AI features can be optional/disabled |
| Test coverage target missed | Prioritize critical path tests |
| Marketplace rejection | Run verifier early and often |
| Performance issues | Profile with large files in Week 7 |

---

## Open Questions (Resolved)

| Question | Resolution |
|----------|------------|
| Separate lexer vs wrap RIDDL? | **Wrap RIDDL** - less maintenance |
| LSP vs native plugin? | **Native** now, LSP consideration future |
| AI integration priority? | validate-partial, map-domain-to-riddl first |
| Timeline? | **8 weeks** to marketplace |

## Open Questions (Remaining)

1. **RIDDL 1.1.0 Compatibility** - Need to verify API changes when released
2. **MCP Server Deployment** - Where will production server run?
3. **Marketplace Pricing** - Free or paid plugin?

---

## Design Decisions Log

| Decision | Rationale | Date |
|----------|-----------|------|
| Rewrite over refactor | Architecture incompatible with target features | 2026-01-19 |
| Wrap RIDDL tokenization | Single source of truth, ~1 day vs ~2 weeks | 2026-01-19 |
| Project-level services | Better UX than window-based state | 2026-01-19 |
| Native plugin (not LSP) | Best UX for JetBrains, LSP future option | 2026-01-19 |
| AI via riddl-mcp-server | Leverages existing validation tools | 2026-01-19 |
| 8-week timeline | Aggressive but achievable for marketplace | 2026-01-19 |

---

## Resources

- [IntelliJ Platform SDK](https://plugins.jetbrains.com/docs/intellij/)
- [Custom Language Support Tutorial](https://plugins.jetbrains.com/docs/intellij/custom-language-support-tutorial.html)
- [RIDDL Language Reference](../ossum.tech/docs/riddl/references/language-reference.md)
- [riddl-vscode Tokenization](../riddl-vscode/src/semanticTokensProvider.ts)
- [riddl-mcp-server Tools](../riddl-mcp-server/CLAUDE.md)
