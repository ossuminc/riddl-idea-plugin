# riddl-idea-plugin

A JetBrains IDEA Plugin for RIDDL

For info on RIDDL, see https://github.com/ossuminc/riddl

## Quickstart

* `sbt packageArtifactZip`
* Install into local IDEA using the gear icon in Plugin Settings ->
  "Install Plugin from Disk...", then restart the IDE

## Requirements

| | |
|---|---|
| IntelliJ IDEA | 2026.1+ (build 261+) |
| JDK | 21+ (Temurin recommended) |
| RIDDL | 2.0.0-rc.10 |

## Features

- **Syntax highlighting** — driven by the RIDDL tokenizer, not a
  hand-written regex lexer
- **Error annotation** — full semantic validation (parse, resolution
  and validation passes) reported inline as you type
- **Structure view** — the definition hierarchy of the current file
- **Go-to-definition** — Cmd+Click through to a definition
- **Code completion** — context-aware keywords and types
- **Code folding** and **brace matching** for `{}`, `()`, `[]`, `<>`
- **Comment toggle** — Cmd+/ for `//` and `/* */`
- **Tool window** — run RIDDL compiler commands and read their output
- **AI assistance** — generate RIDDL from a description (Alt+Enter),
  backed by riddl-mcp-server

Not yet implemented: find usages, safe rename/refactoring, and hover
documentation.

Files that are include-fragments rather than complete documents are
still handled: the AST-based features fall back to regex parsing when
a file will not parse as a whole RIDDL document.

## How to

### Build

- `sbt compile`
- `sbt test` — 174 tests across 11 suites

### Format

- `sbt scalafmt` / `sbt scalafmtCheck`

The `.scalafmt.conf` is shared across ossuminc projects and is fetched
automatically on `sbt update`; it is deliberately not checked in. If
scalafmtCheck reports the file is missing, delete the stale
`.scalafmt.conf.etag` and re-run `sbt update` — a leftover etag makes
the fetch return 304 and the config is never written.

### Run in a sandbox IDE

- `sbt runIDE`

This launches IntelliJ **Ultimate**. The build targets `ideaIU`, the
unified distribution JetBrains ships from 2026.1 onward, so the
sandbox is Ultimate even though the plugin itself is intended to work
on Community too. Nothing in the build fails on Ultimate-only APIs,
so check platform API use by hand if it must stay Community-safe.

The SDK is cached in `~/.RIDDL4IDEAPluginIU`.

### Package

- `sbt packageArtifactZip` (local snapshot only, for testing)
- `sbt publishPlugin` (published to IntelliJ Marketplace)

## Troubleshooting

**Tests fail with `NoSuchMethodError` on a riddl class, or pass while
exercising the wrong riddl version.** sbt caches a resolved classpath
under `target/`, and after a dependency bump it can keep serving the
old one to Test scope while Compile uses the new one. Compare
`sbt "export Compile/fullClasspath"` with the Test equivalent and look
for two versions of the same jar. Fix with `rm -rf target` — removing
`target/sbtidea.cache` alone is not enough.
