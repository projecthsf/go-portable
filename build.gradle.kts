import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    id("org.jetbrains.intellij.platform") version "2.18.0"
}

group = "io.genai"
version = "0.1.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        // Compile against 2024.2 (242): LSP4IJ requires build 242+, and the optional
        // code-intelligence module compiles against its API. Core sinceBuild stays 233 —
        // the LSP layer is an OPTIONAL dependency, active only where LSP4IJ can install.
        intellijIdeaCommunity("2024.2")

        // Optional Go code-intelligence layer (completion / navigation / errors) via
        // LSP4IJ + gopls. Compile-time dep here; runtime it's optional (see plugin.xml).
        plugin("com.redhat.devtools.lsp4ij", "0.20.1")
    }
}

java {
    toolchain { languageVersion = JavaLanguageVersion.of(17) }
}

kotlin {
    jvmToolchain(17)
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "233"
            untilBuild = provider { null }
        }
    }

    // `./gradlew publishPlugin` reads the JetBrains Marketplace token from the PUBLISH_TOKEN
    // env var (set as a GitHub Actions secret). No signing configured, so uploads are unsigned.
    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }

    // `./gradlew verifyPlugin` runs the JetBrains Plugin Verifier (same tool Marketplace uses).
    // This is a publish gate in CI (see .github/workflows/publish.yml).
    pluginVerification {
        failureLevel.set(listOf(
            FailureLevel.COMPATIBILITY_PROBLEMS,
            FailureLevel.INTERNAL_API_USAGES,
            FailureLevel.MISSING_DEPENDENCIES,
            FailureLevel.INVALID_PLUGIN,
        ))
        ides {
            latest {
                types.set(listOf(IntelliJPlatformType.IntellijIdea))
            }
        }
    }
}

// Indexing settings via a headless IDE is slow and clashes with a running runIde sandbox;
// not needed for a dev build.
tasks.named("buildSearchableOptions") { enabled = false }
