# Go Portable

Write & run Go in **IntelliJ IDEA Community** for free — no GoLand license, no system-wide Go
install. IDEA Community doesn't bundle the official Go plugin (that's Ultimate / GoLand only),
so this fills the gap.

## Features
- **Go Portable SDK** — download a Go toolchain from inside the IDE (from `go.dev/dl`);
  stored under `~/.go-portable`.
- **Run a `.go` file** — `go run` via a run configuration (right-click ▸ Run, or the gutter ▶).
- **Syntax highlighting** — lexer-based highlighting for `.go` files.
- **Code intelligence (optional)** — completion, go-to-definition, hover docs and diagnostics via
  **gopls** (the official Go language server), bridged with the free **LSP4IJ** plugin. Fully
  offline. gopls is installed with the portable toolchain (`go install …gopls@latest`).
- **Run go tools** — `go mod` / `get` / `build` / `test` / `vet` / `fmt` in a console.

On **GoLand / IDEA Ultimate** the native Go support owns `.go`, and this plugin steps aside.

## Architecture
A re-instantiation of the `php-portable` skeleton for Go: portable SDK downloader, runtime
file-type activation (Community-only), `go run` config, LSP4IJ bridge (languageId **must** be
`"go"`), and a one-click "Enable code intelligence" onboarding banner.

## Build
```
JAVA_HOME=<a JBR 17> ./gradlew buildPlugin      # -> build/distributions/go-portable-*.zip
JAVA_HOME=<a JBR 17> ./gradlew runIde           # sandbox IDE for testing
JAVA_HOME=<a JBR 17> ./gradlew verifyPlugin     # JetBrains Plugin Verifier (publish gate)
```
