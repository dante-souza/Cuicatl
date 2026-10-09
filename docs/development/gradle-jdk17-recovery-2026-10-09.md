# Gradle / JDK 17 incident and recovery (Windows)

## Observed incident — 2026-10-09

Android Studio reported that the project's **Gradle 8.13** was incompatible with its selected **JetBrains Runtime 25**. Selecting **Eclipse Temurin 17.0.20.1** as the IDE Gradle JDK allowed an IDE-triggered build to complete, but a separate PowerShell invocation of `./gradlew` still selected Java 25.

Terminal evidence before correction:

```text
Gradle 8.13
Kotlin: 2.0.21 (Gradle-bundled Kotlin, not necessarily the app plugin version)
Launcher JVM: 25.0.3 (JetBrains)
Daemon JVM: C:\Program Files\Android\Android Studio\jbr
./gradlew assembleDebug: BUILD FAILED in 2s; What went wrong: 25.0.3
```

The Java native-access warnings were secondary, not proof of a separate Cuicatl defect.

Session-local correction (original verified reproduction):

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -version
.\gradlew --stop
.\gradlew --version
.\gradlew assembleDebug
```

Observed result: **Gradle 8.13, launcher JVM 17.0.20.1, daemon JVM Temurin 17**, `BUILD SUCCESSFUL in 15s`, 37 tasks up-to-date.

## Root cause and boundaries

Android Studio's **Gradle JDK** setting does not automatically change the environment inherited by standalone PowerShell sessions. Invoking `gradlew` directly picked up JBR 25 from the terminal environment. Gradle 8.13 does not support running on Java 25.

The repository already uses `scripts/gradle-windows.ps1` for Make-based Gradle commands, and that launcher selects a validated JDK 17 and SDK Platform 36 in its child process. The new diagnostic/recovery targets make that protection visible and actionable; **they do not rewrite system-wide JAVA_HOME or PATH**. The application itself targets JVM 17 through Gradle configuration. Gradle's `--version` Kotlin field reports bundled Kotlin (2.0.21); the app's Kotlin Gradle plugin is 2.3.21 in `gradle/libs.versions.toml`.

## Standard Make commands (repository root)

```powershell
make jdk-doctor       # report PATH Java, JAVA_HOME, selected compatible JDK
make gradle-stop      # stop existing Gradle daemons using the JDK 17-aware launcher
make gradle-verify    # show Gradle and launcher/daemon JVM versions
make build-debug      # build debug APK using selected JDK 17
make jdk-repair       # diagnose -> stop -> verify -> build, fail-fast
make check            # unit tests + lint + APK assembly
```

Use `make jdk-repair` after seeing a Java 25 incompatibility, or `make check` for the stronger validation gate. `make jdk-repair` intentionally builds rather than running the full test/lint gate; run `make check` separately before treating a change as validated. The J8 install/open commands remain independently preflight-gated.

**Expected verification:** `make gradle-verify` reports Gradle 8.13 and Java 17 for the launcher/daemon. `make build-debug` ends with `BUILD SUCCESSFUL`. APK path: `app/build/outputs/apk/debug/app-debug.apk`.

If `make jdk-doctor` cannot locate JDK 17, install Temurin 17 or point `JAVA_HOME` at an existing JDK 17 and retry. If the JDK is found but a build fails, capture the complete error and run `make check`; do not assume every build failure is a JVM mismatch.

## IDE setup

In Android Studio: **Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK**, choose the installed Temurin 17, then sync. This setting is independent of standalone terminal Gradle.

## Scope and verification

The Makefile and launcher changes were committed to the Phase 1 feature branch. The user executed both recovery and quality-gate targets on the Windows 11 / Cygwin Make / PowerShell lab host on **2026-10-09**, with the following reported results:

| Command / step | Observed result |
| --- | --- |
| `git pull --ff-only origin feature/phase-1-usable-session-export` | Fast-forward from `704f91d` to `096c77b`; 3 changed/new files |
| `make jdk-doctor` | `JAVA_HOME` and PATH resolve to Temurin 17.0.20.1; selected build JDK 17.0.20.1 |
| `make gradle-stop` | One Gradle daemon stopped |
| `make gradle-verify` | Gradle 8.13; launcher JVM 17.0.20.1; daemon JVM Temurin 17 |
| `make build-debug` (inside `make jdk-repair`) | **BUILD SUCCESSFUL in 13s**; 37 tasks up-to-date |
| `make check` | **BUILD SUCCESSFUL in 14s**; 53 tasks: 1 executed, 52 up-to-date |

SDK Platform 36 was detected at `C:\\Users\\dante\\AppData\\Local\\Android\\Sdk\\platforms\\android-36`. The message explaining a single-use daemon under `--no-daemon` is informational, not a failure.

**Validation conclusion:** Windows host Make recovery chain and test/lint/build quality gate both passed. This evidence does **not** constitute a device-install or acoustic-capture validation. No dependency/AGP upgrade was made.

**Outstanding local-file item:** `git status` before the pull reported an unstaged modification to `gradle.properties`. The fast-forward pull succeeded without overwriting that file; inspect `git diff -- gradle.properties` and preserve or intentionally resolve the change before creating a release/archaeology snapshot.
