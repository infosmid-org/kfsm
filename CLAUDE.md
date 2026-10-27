# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

kfsm is a Kotlin Multiplatform finite-state machine library with a DSL (turnstile-style FSMs with entry/exit actions, guarded/automatic/push/pop transitions, typed event args and return values, externalised state, timeouts, coroutine-based async variants). Group `org.infosmid`, version from `version.txt` (override with `-PreleaseVersion=...`). 

## Code base knowledge
Use codegraphcontext mcp for understanding the codebase.

## Commands

Gradle wrapper, JDK 17 toolchain, Kotlin 2.4.21. Targets are fixed in `build.gradle`: jvm, android (`com.android.kotlin.multiplatform.library`, needs an Android SDK via `local.properties`/`ANDROID_HOME`), iosArm64, iosSimulatorArm64, macosArm64, wasmJs, wasmWasi. Apple targets only compile on macOS (disabled with a warning elsewhere).

- Build + tests: `./gradlew build`
- JVM tests only: `./gradlew jvmTest`; Android host tests: `./gradlew testAndroidHostTest`
- Single test class: `./gradlew jvmTest --tests "org.infosmid.kfsm.example.TurnstileFsmTests"`
- Full local build + publish to maven local (CI-like): `./build-linux.sh`
- Docs: `./gradlew siteDocsZip` (asciidoctor 5.x model via `asciidoc {}` in `build.gradle` + Dokka V2, packaged by `gradle/site-docs.gradle`) writes `build/site-docs/*-docs.zip`; `./gradlew renderPlantUml` regenerates the PNGs in `src/docs/asciidoc` from `src/docs/plantuml` (manual, needs graphviz `dot`). Dependency and plugin versions live in `gradle/libs.versions.toml`.

CI (`.github/workflows`) runs `./gradlew build` on ubuntu (+dokka) and macOS. wasmJs browser tests need headless Chrome. Keep `mavenLocal()` restricted to group `org.infosmid` (a stale partial `kotlinx-coroutines` in `~/.m2` otherwise breaks wasm resolution).

## Architecture

All library code is in `src/commonMain/kotlin/org/infosmid/kfsm`; there is no platform-specific main code. Generic parameters throughout: `S` state, `E` event, `C` context, `A` argument, `R` return.

Definition vs. instance split (immutable, shareable definition; per-context mutable instance):
- Entry points are the top-level `stateMachine(...)` functions in `stateMachine.kt` (and `asyncStateMachine` in `async/asyncStateMachines.kt`), which create a `StateMachineBuilder` and run the DSL block.
- DSL receivers: `DslStateMachineHandler` → `DslStateMapHandler` → `DslStateMapEventHandler` / `DslStateMapDefaultEventHandler`. These record into `StateMachineBuilder` / `StateMapBuilder`, which produce `StateMachineDefinition` / `StateMapDefinition`.
- `definition.create(context)` yields a `StateMachineInstance` (with `StateMapInstance` per state map, supporting push/pop between maps) that processes `sendEvent`.
- Transitions are modelled by `SyncTransition` implementations (`DefaultSyncTransition`, `SimpleSyncTransition`, `SyncGuardedTransition`); `SyncTransitionRules` holds the shared rules.
- `CommonTypes.kt` holds shared function typealiases and exceptions.

The `async/` package is a parallel suspend-based copy of the whole stack (`Async*` counterparts of every sync class, plus `AsyncTimer`/`AsyncTimerDefinition` for timeouts). Changes to sync behaviour usually need a matching change in the async classes.

## Tests

Tests double as documentation: `src/commonTest/.../example` contains the example FSMs (turnstile, paying/secure turnstile, lock, packet reader, car) that the AsciiDoc and README reference. `jvmTest` adds visualization (uses `org.infosmid:kfsm-viz`) and timeout tests; `nativeTest` has native-specific timeout tests. Prefer adding shared tests in `commonTest`.
