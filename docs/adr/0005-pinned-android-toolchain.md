# ADR-0005: Pinned Android bootstrap toolchain

- **Status:** Accepted
- **Accepted:** Gil on 2026-10-06 in [pull request #2](https://github.com/Gil-netizen/aseh/pull/2)
- **Date:** 2026-10-06
- **Owner:** Gil (`@Gil-netizen`); future re-review roles: Android build and release maintainers
- **Decision scope:** Android project bootstrap, Gradle build hosts, CI images, compile and target SDK, minimum Android version
- **Supersedes:** none
- **Related:** [Release policy](../RELEASE.md), [Threat model](../THREAT_MODEL.md)

## Context

The repository must bootstrap against a complete compatibility set that is both
available from authoritative repositories and pinned in source. Live verification
on 2026-10-06 found stable Android SDK Platform 37.0 and Build Tools 37.0.0,
Gradle 9.6.0, AGP 9.4.1, Kotlin 2.2.10, the Kotlin Compose plugin 2.2.10, and
Compose BOM 2026.09.00. Google's AGP 9.4 compatibility record names API 37 as
the maximum supported API, Gradle 9.6.0, JDK 17, and Kotlin Gradle plugin 2.2.10.

ASEH supports Android API 26 and later. The Android, Java, Gradle, AGP, Kotlin,
Compose compiler, and Compose library versions form one reviewed compatibility
set. Dynamic, preview, or workstation-inherited versions are prohibited.

## Decision

The accepted bootstrap host toolchain is:

| Component | Pinned value |
|---|---|
| Android Studio | Rabbit 1, 2026.2.1 |
| JDK vendor and runtime | Eclipse Temurin 17.0.20.1+1 (Windows package 17.0.20.101) |
| Java source/target and Kotlin JVM toolchain | 17 |
| Gradle wrapper | 9.6.0 |
| Gradle distribution SHA-256 | `bbaeb2fef8710818cf0e261201dab964c572f92b942812df0c3620d62a529a01` |
| Android Gradle Plugin | 9.4.1 |
| Built-in Kotlin / Kotlin Gradle plugin runtime | 2.2.10 |
| Kotlin Compose compiler plugin | 2.2.10 |
| Jetpack Compose BOM | 2026.09.00 |
| Android SDK Command-line Tools | 22.0 |
| `compileSdk` | API 37, minor API level 0 |
| `targetSdk` | 37 |
| Android SDK Platform package | `platforms;android-37.0` revision 2 |
| Android SDK Build Tools | 37.0.0 |
| Android SDK Platform-Tools | 37.0.1 |
| `minSdk` | 26 |

Android Studio is the supported interactive environment, but CI and command-line
Gradle builds are the reproducibility authority. The scaffold records every
value above in the wrapper, version catalog, Android DSL, compiler configuration,
and CI setup. It uses AGP 9 built-in Kotlin and does not also apply
`org.jetbrains.kotlin.android`. All plugin and dependency versions use exact
selectors, and Gradle dependency verification is committed after the first
successful resolution.

The scaffolding pull request must still prove this available set by resolving
dependencies and completing a clean build, tests, lint, and both debug and
release-like assembly. A real compatibility failure may propose a replacement
set in a superseding ADR; silently changing one member is not permitted.

Raising `compileSdk` or `targetSdk`, lowering `minSdk`, changing the JDK major
version, or changing any Gradle, AGP, Kotlin, Compose compiler, Compose BOM, or
SDK compatibility member requires a reviewed ADR before the build change.

## Alternatives considered

- **Use latest available versions without pins:** rejected because runner and
  workstation drift would make builds and provenance irreproducible.
- **Use API 36 because `platforms;android-37` is not a package name:** rejected
  because the stable minor-API package is `platforms;android-37.0`; exact package
  enumeration confirms it is available.
- **Apply the standalone Kotlin Android plugin beside AGP 9 built-in Kotlin:**
  rejected because duplicate Kotlin integration creates an unsupported build
  configuration. AGP supplies the pinned KGP runtime.
- **Adopt a newer unreviewed Kotlin or Compose line:** rejected for bootstrap;
  upgrades require compatibility evidence and a reviewed ADR.
- **Lower `minSdk` below 26 immediately:** rejected because no device research
  currently justifies the compatibility and security cost.

## Consequences

The initial app compiles against Platform 37.0, targets API 37, and supports API
26 and later. Contributors need the pinned SDK packages and JDK, while Android
Studio remains optional for CI. The set is intentionally conservative about
Kotlin and Compose upgrades even when newer artifacts exist; deterministic
bootstrap and AGP's built-in-Kotlin contract take priority.

## Verification

- Record `studio --version`, `java -version`, and `sdkmanager --version` output
  in redacted bootstrap evidence and match the table above.
- From a clean SDK directory, install only the declared command-line tools,
  Platform 37.0, and Build Tools 37.0.0, then run the Gradle wrapper without
  Android Studio state.
- Assert `minSdk = 26`, API 37.0 compile SDK, `targetSdk = 37`, Build Tools
  37.0.0, JVM toolchain 17, built-in Kotlin 2.2.10, and the Compose pins from
  the resolved build model.
- Verify the Gradle 9.6.0 wrapper against the recorded checksum and confirm
  dependency resolution contains no dynamic or preview versions.
- Run clean assemble, unit tests, lint/static analysis, and dependency
  verification locally and in CI with the exact resolved compatibility set.
- Fail environment diagnostics when the required SDK packages or JDK major
  version are missing rather than silently selecting another installed version.
