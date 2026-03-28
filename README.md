# ⚔ SabreTooth IDE

**Lightweight Android IDE for APK generation — no bloat, no Gradle sync, no layout editor.**

Package: `com.vitalsoft.sabretooth`  
Version: 1.0.0  
Platform: Cross-platform (Windows, macOS, Linux)  
Requires: Java 21+

---

## Overview

SabreTooth IDE is a minimalist Android development environment focused on one thing: **getting your code compiled into a valid APK as fast as possible**. It supports both Gradle-based and Eclipse-style project structures, automatically detects your installed tools, and keeps all configuration in a simple `.sabreconf` file per project.

No Gradle sync. No layout editor. No 5-minute startup. Just open, edit, build.

---

## Features

- **Dark IDE UI** — Android Studio-inspired layout: project tree, tabbed editor, console, menu bar
- **Syntax highlighting** — Java, Kotlin, XML, Groovy, Gradle, Properties files
- **Auto-indentation** — Smart indent on Enter/Tab, brace-aware
- **Project types** — Gradle (standard) and Eclipse-style projects
- **APK build** — Debug and release APK via Gradle; cancel mid-build
- **Tool auto-detection** — Finds all installed Gradle versions, JDKs, and Android SDK
- **`.sabreconf` per project** — Stores build config: SDK versions, paths, signing, extra Gradle args
- **Find & Replace** — With case sensitivity and replace-all
- **File tree** — With context menu (new file, new dir, rename, delete)
- **New Project wizard** — Configures min/target/compile SDK, package name, Gradle, JDK
- **Recent projects** — Quick re-open from File menu
- **Tabs with close** — Middle-click or × button; unsaved-change prompts
- **Status bar** — Line/column, language, build status, project name
- **Font size zoom** — Ctrl+/Ctrl-
- **Keyboard shortcuts** — Full set, see below

---

## Requirements

| Requirement | Details |
|---|---|
| Java | **21 or higher** (JDK for building projects) |
| Gradle | Any version installed locally (or use gradlew in project) |
| Android SDK | Required for APK builds (`ANDROID_HOME` env or set in Preferences) |
| OS | Windows 10+, macOS 12+, Linux (any desktop) |

---

## Installation & Running

### Linux / macOS
```bash
chmod +x sabretooth.sh
./sabretooth.sh
```

### macOS (alternative)
```bash
chmod +x sabretooth-mac.sh
./sabretooth-mac.sh
```

### Windows
```
Double-click sabretooth.bat
```

### Any Platform (direct)
```bash
java --enable-preview -jar SabreTooth.jar
```

---

## Project Configuration: `.sabreconf`

Every project managed by SabreTooth has a `.sabreconf` file in its root. Example:

```ini
# SabreTooth IDE Project Configuration

[project]
project.name = MyApp
project.package = com.example.myapp
project.type = gradle
android.mainActivity = MainActivity

[android]
android.minSdk = 21
android.targetSdk = 34
android.compileSdk = 34
android.versionCode = 1
android.versionName = 1.0

[build]
build.type = debug
build.gradlePath = /usr/local/bin/gradle
build.jdkPath = /usr/lib/jvm/java-21-openjdk-amd64
build.androidJar = /home/user/Android/Sdk/platforms/android-34/android.jar
build.outputDir = build/outputs/apk
build.extraGradleArgs = --stacktrace

[signing]
sign.keystorePath = /path/to/my.jks
sign.keystoreAlias = mykey
```

You can edit this file directly or use **Project → Project Settings**.

---

## Keyboard Shortcuts

| Shortcut | Action |
|---|---|
| `Ctrl+N` | New File |
| `Ctrl+Shift+N` | New Project |
| `Ctrl+O` | Open File |
| `Ctrl+Shift+O` | Open Project |
| `Ctrl+S` | Save |
| `Ctrl+Shift+S` | Save All |
| `Ctrl+W` | Close Tab |
| `Ctrl+F` | Find / Replace |
| `F10` | Build Debug APK |
| `Shift+F10` | Build Release APK |
| `F9` | Clean Build |
| `F5` | Refresh File Tree |
| `Ctrl++` | Increase Font Size |
| `Ctrl+-` | Decrease Font Size |
| `Ctrl+Z` | Undo |
| `Ctrl+Y` | Redo |
| `Tab` | Insert 4 spaces |

---

## Supported Project Structures

### Gradle (recommended)
```
MyApp/
├── .sabreconf
├── settings.gradle
├── build.gradle
└── app/
    ├── build.gradle
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/example/myapp/
        │   └── MainActivity.java
        └── res/
            ├── layout/activity_main.xml
            └── values/strings.xml
```

### Eclipse
```
MyApp/
├── .sabreconf
├── AndroidManifest.xml
├── project.properties
├── .classpath
└── src/com/example/myapp/
    └── MainActivity.java
```

---

## Build Process

1. **Save All** — All open files are saved before any build
2. **Gradle invoked** — Using the path from `.sabreconf` (or auto-detected gradlew/system gradle)
3. **JDK override** — If a JDK path is set, `JAVA_HOME` is set for that process
4. **Output streamed** — Console shows real-time output with color coding
5. **APK located** — On success, SabreTooth finds the APK and offers to open its folder

For **release builds**, set up your keystore in Project Settings → Signing. You'll need to configure the signing config in your `build.gradle` manually (SabreTooth sets up the paths, Gradle does the signing).

---

## Extra Gradle Arguments

You can pass arbitrary extra arguments to Gradle via `.sabreconf`:
```ini
build.extraGradleArgs = --stacktrace --info
```

Or per-task via the Project Settings dialog under Build Tools.

---

## IDE Preferences

Stored in `~/.sabretooth/ide.conf`:

- Font size, tab size, line numbers, word wrap
- Default Android SDK root
- Default Gradle installation
- Default JDK
- Gradle user home directory
- Window position/size

---

## Building from Source

```bash
# Compile
find src -name "*.java" > sources.txt
javac --enable-preview --release 21 -d out @sources.txt

# Package
echo "Main-Class: com.vitalsoft.sabretooth.Main" > manifest.mf
jar --create --file=SabreTooth.jar --manifest=manifest.mf -C out .

# Run
java --enable-preview -jar SabreTooth.jar
```

---

## Architecture

```
com.vitalsoft.sabretooth
├── Main.java                    — Entry point
├── build/
│   └── BuildManager.java        — Gradle invocation, process management
├── config/
│   ├── SabreConf.java           — .sabreconf file read/write
│   └── IdePreferences.java      — IDE-wide settings (~/.sabretooth/)
├── project/
│   └── Project.java             — Project model + scaffold generator
├── ui/
│   ├── MainWindow.java          — Main JFrame, menus, actions
│   ├── EditorTabPanel.java      — Tabbed editor with close buttons
│   ├── CodeEditorPane.java      — Editor + line numbers + save/load
│   ├── SyntaxHighlighter.java   — Java/XML/Groovy/Properties highlighter
│   ├── ProjectTreePanel.java    — File tree with context menu
│   ├── ConsolePanel.java        — Build output console
│   ├── StatusBar.java           — Bottom status bar
│   ├── MainToolbar.java         — Build/save toolbar
│   ├── NewProjectDialog.java    — New project wizard
│   ├── ProjectSettingsDialog.java — Per-project settings
│   ├── IdePreferencesDialog.java  — IDE global preferences
│   ├── FindReplaceDialog.java   — Find & replace
│   └── Theme.java               — Dark color palette + fonts
└── util/
    └── ToolDetector.java        — Finds Gradle, JDK, Android SDK installs
```

---

## License

MIT License — free to use, modify, distribute.

SabreTooth IDE — Vitalsoft
