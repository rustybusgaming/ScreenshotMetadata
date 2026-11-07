# Changelog

All notable changes to the Screenshot Metadata Mod are documented here. This changelog focuses on functional changes to the mod itself.

## [1.4.3] - 2026-11-07

### Added
- Babric (Minecraft Beta 1.7.3) support: `screenshotmetadata-babric-b1.7.3-<version>.jar`,
  built with `./gradlew -p babric build`. Records player, coordinates, facing, dimension,
  biome, time, weather, world name and seed, difficulty, health, held item and armour,
  render distance, player state, light levels, the block or entity being looked at, the
  texture pack and the mod list.
- Better than Adventure! 8.0.1 support: `screenshotmetadata-bta8.0.1-<version>.jar`, built
  with `./gradlew -p bta build`. Records the same fields plus BTA's game mode, season and
  named weather. BTA writes screenshots a few frames after F2 (GPU read-back), and the
  metadata is added once the PNG is actually on disk.
- Both builds share the existing metadata writers and config file; there is no Mod Menu
  screen for them yet.
- NeoForge support for Minecraft 1.21.1, 1.21.3 - 1.21.11 and 26.1 - 26.3:
  `screenshotmetadata-neoforge-mc<version>-<version>.jar`, built with
  `./gradlew -p neoforge build -PmcProfile=<profile>`. Records the same metadata as the
  Fabric jars, and NeoForge's mod list **Config** button opens the same settings screen
  that ModMenu does. 1.21.2 is skipped (NeoForge never left its first beta there), and
  the 1.21.6, 1.21.7, 1.21.9, 26.1 and 26.1.1 jars are marked beta because NeoForge
  itself only has beta builds for those versions.
- Quilt support: the Fabric jars run on Quilt Loader unchanged and are now published for
  Quilt as well. The metadata records which loader (Fabric, Quilt or NeoForge) took it.

### Changed
- The shared code no longer calls Fabric Loader directly; loader-specific bits (config
  folder, mod list, version) go through a small `Platform` interface with Fabric and
  NeoForge implementations. The Fabric entrypoint class is now
  `com.fentbuscoding.screenshotmetadata.fabric.ScreenshotMetadataFabric`.
- The `beta` profile now tracks the live snapshot line instead of 25w46a. It was
  pinned to the last Yarn snapshot, which has not moved since Yarn stopped
  publishing, while the publish workflow separately resolved the Fabric API and the
  store version from the newest actual snapshot. Those drifted apart across 26.1,
  26.2 and 26.3, and the 1.4.2 release failed on CurseForge with
  "Failed to find version: 26.4-snapshot-2" — a version the build was not for.
  The profile is now a deobfuscated no-remap target like the other Minecraft 26
  profiles, and every value the workflow resolves comes from one snapshot alias.
- The `beta` profile publishes to Modrinth only. CurseForge adds new snapshot
  versions well after Modrinth does and rejects an upload naming one it does not
  list yet. Any profile can opt out with `<profile>_publish_curseforge=false`.
- The config screen is now split by Screen-API generation. Minecraft 26.4 dropped the
  mouse and delta arguments from `extractRenderState` and `extractBackground`, so one
  file cannot compile against both 26.3 and 26.4: `src/mojang_screen_26_3` serves
  26.1-26.3 and `src/mojang_screen_26_4` serves 26.4. The mixin is unaffected and
  stays shared across every 26.x target.
- Fabric Loader 0.20.1, which Fabric API for 26.4 requires; ModMenu 22.0.0-alpha.3
  for the snapshot profile.
- Dropped the `beta`-only `fabric-key-binding-api-v1` dependency. That artifact does
  not exist for Minecraft 26, and the profile now takes the full Fabric API like
  every other target.

### Fixed
- PNG text metadata is no longer lost. With the default settings the embedded-XMP pass
  re-encoded the screenshot from its pixels and dropped the text chunks written just
  before it, so screenshots carried the XMP packet but none of the PNG text entries.
  Both writers now keep the chunks already in the file and replace only their own.

## [1.3.3] - 2026-04-15

### Added
- Build and release support for Minecraft 1.21.1 through 1.21.10 with Fabric Loader 0.19.1 and ModMenu support:
  - 1.21.1 — Fabric API 0.116.11, ModMenu 11.0.4
  - 1.21.2 — Fabric API 0.106.1, ModMenu 12.0.1
  - 1.21.3 — Fabric API 0.114.1, ModMenu 12.0.1
  - 1.21.4 — Fabric API 0.119.4, ModMenu 13.0.4
  - 1.21.5 — Fabric API 0.128.2, ModMenu 14.0.2
  - 1.21.6 — Fabric API 0.128.2, ModMenu 15.0.2
  - 1.21.7 — Fabric API 0.129.0, ModMenu 15.0.2
  - 1.21.8 — Fabric API 0.136.1, ModMenu 15.0.2
  - 1.21.9 — Fabric API 0.134.1, ModMenu 16.0.1
  - 1.21.10 — Fabric API 0.138.4, ModMenu 16.0.1
- Automatic publishing to Modrinth and CurseForge on release (all supported MC versions)
- XMP sidecar files (`.xmp`) are written alongside each screenshot, embedding player name, world, coordinates, biome, weather, tags, and timestamp in an industry-standard format readable by Windows File Explorer and photo management tools

### Fixed
- Resolved intermittent HTTP 400 errors when resolving ModMenu from the TerraformersMC Maven repository by restricting Gradle to Maven POM metadata only (the server does not serve Gradle module metadata files)

## [1.4.2] - 2026-10-04

### Fixed
- Publishing failed for every profile with "At least one of client or server must be
  set to true". mod-publish-plugin 2.2 requires the CurseForge block to declare which
  side the mod runs on, which 2.1 did not; the 2.2.1 bump in 1.4.1 therefore broke
  publishing. The CurseForge block now sets `client = true` / `server = false` and the
  Modrinth block sets `environment = CLIENT_ONLY`, matching the `"environment":
  "client"` already declared in `fabric.mod.json`.

  CurseForge publishes before Modrinth, so this aborted the run before Modrinth was
  ever contacted.

## [1.4.1] - 2026-10-04

### Fixed
- The publish workflow never ran for 1.4.0, so nothing reached Modrinth or
  CurseForge. It triggered on `release: published`, but the release is created by
  the release workflow using `GITHUB_TOKEN`, and GitHub does not let a
  `GITHUB_TOKEN`-triggered event start another workflow. Publishing now triggers on
  the `v*.*.*` tag itself, the same event the release workflow uses.
- A missing or empty `MODRINTH_TOKEN` or `CURSEFORGE_TOKEN` used to skip the upload
  step and leave the job green, so a release could publish nothing without failing.
  The workflow now checks both secrets up front and fails with a clear error.

## [1.4.0] - 2026-10-03

### Added
- Build and publish support for Minecraft 26.1.2, 26.2 and 26.3:
  - 26.1.2 — Fabric API 0.155.3, ModMenu 18.0.2
  - 26.2 — Fabric API 0.161.0, ModMenu 20.0.3
  - 26.3 — Fabric API 0.161.0, ModMenu 21.0.0-beta.1
- A second set of sources under `src/mojang/java` targeting the names used by the
  deobfuscated Minecraft 26 client, alongside the existing Yarn-named sources in
  `src/yarn/java`. The metadata writers stay shared in `src/main/java`.

### Changed
- Minecraft 26 ships deobfuscated, and neither Yarn nor Mojang publish mappings for
  it, so the 26.x profiles now build with Loom's no-remap plugin
  (`net.fabricmc.fabric-loom`) and a `mappings_channel=none` setting instead of
  referencing Yarn mappings that do not exist.
- Renamed the 26.x build profiles so each one matches its Minecraft version, in line
  with the `mc1_21_*` naming: `mc26` is now `mc26_1` (26.1), the old `mc26_1` is now
  `mc26_1_1` (26.1.1), and the old `mc26_2` is now `mc26_1_2` (26.1.2). `mc26_2` and
  `mc26_3` now mean 26.2 and 26.3.
- Corrected the Fabric API versions recorded for the 26.1.x profiles; the previous
  values were not published for those Minecraft versions.
- Updated Gradle to 9.7.1 and Fabric Loom to 1.18.2, which are required for the
  Minecraft 26 toolchain. Loom now needs Java 25 to run, so CI installs both JDK 21
  (for the 1.21.x compile toolchain) and JDK 25.

- `fabric.mod.json` now declares a per-version minimum Fabric Loader instead of a
  flat `>=0.16.0`, so an out-of-date loader gives a clear message rather than a
  confusing Fabric API error: `>=0.19.3` on 26.3, `>=0.18.4` on 26.1-26.2,
  `>=0.17.3` on 1.21.11.

### Changed
- Updated dependencies: Fabric Loader 0.19.5, Fabric Language Kotlin
  1.14.1+kotlin.2.4.20, Kotlin plugin 2.4.20, mod-publish-plugin 2.2.1, Yarn
  1.21.11+build.6, Fabric API 0.141.6 (1.21.11) and 0.116.17 (1.21.1), and ModMenu
  11.0.5 / 17.0.1-beta.1 / 18.0.2 / 20.0.3 on the lines that had newer builds.
- Updated GitHub Actions: checkout v7, setup-java v6, cache v6,
  gradle/actions/wrapper-validation v6, action-gh-release v3.
- The release and publish workflows now cover all 17 build profiles. Previously a
  tagged release shipped no 1.21.x jars and published only 7 of the 17 targets.

### Fixed
- Dropped the `refmap` entry from the mixin config; Loom 1.18 remaps mixins at build
  time and no longer produces the refmap file the config pointed at.
- Removed the publish step from the build workflow. Every push to `main` uploaded
  every profile to Modrinth and CurseForge, which both duplicated the release
  workflows and published straight off `main`. Publishing now happens on a tag:
  `release.yml` builds and creates the GitHub Release, and `publish.yml` uploads to
  Modrinth and CurseForge when that release is published.
- Removed the unused `loom_version` property, which was never read; `build.gradle`
  pins the Loom plugin directly.

## [1.2.0] - 2026-02-13

### Added
- Capture profiles in ModMenu (`Full`, `Lightweight`, `Privacy`) that apply metadata settings with one click.
- Privacy section now shows an explicit redaction preview for coordinates, server address, and world seed handling.
- Added game mode metadata capture (`GameMode`) alongside difficulty.

### Changed
- Removed keybind-based tag entry flow and shifted configuration to ModMenu only.
- Config now stores `configSchemaVersion` and auto-migrates older config files on load.
- Updated development runtime Mod Menu compatibility for Minecraft `1.21.11`.

### Fixed
- Reduced risk of Mod Menu config screen freezes by removing per-frame UI reinitialization during scrolling.

## [1.1.0] - 2026-02-04

### Added
- Privacy mode toggle (obfuscates coords, hides server IP, hashes world seed)
- Screenshot filename templates with presets
- Tag input screen (keybind) with tag export to JSON/XMP
- ModMenu tooltips and collapsible sections

### Fixed
- Mixin package crash when loading helper classes in the mixin package
- Fixed PNG metadata write failures by correcting PNG `iTXtEntry` fields (`text` instead of `value`), which prevented metadata merge/write errors.
- Fixed metadata fallback reliability by rebuilding image metadata before switching from `iTXt` to `tEXt`.
- Fixed low-information PNG write logs (`null`) by reporting the underlying exception reason.

## [1.0.4.2] - 2026-02-03

### Added
- Weather metadata (rain/thunder state + gradients) with ModMenu toggle
- Optional JSON-only modpack context: enabled resource packs, shader pack name, and truncated mod list

### Improved
- ModMenu config screen: visible section headers and dynamic scroll bounds
- PNG metadata now prefers iTXt chunks with tEXt fallback for broader Unicode support

## [1.0.4.1]

### Fixed
- Fixed blur effect crash in ModMenu config screen by preventing double blur rendering
- Fixed Gradle configuration cache compatibility issue in processResources task

### Improved
- Optimized config screen rendering to reduce FPS impact - eliminated redundant render calls
- Improved scroll performance by removing unnecessary UI reconstruction on every scroll event

## [1.0.4]

### Added
- Performance metrics: Render distance and simulation distance tracking
- Player status metadata: Game difficulty level tracking
- Equipment tracking: Full armor inventory and equipped items
- Potion effects: Active status effects with amplifiers and durations
- Metadata filtering: Configuration options to include/exclude specific metadata categories

### Improved
- File detection with exponential backoff retry strategy (100ms to 1000ms increments)
- Fallback file detection: Checks temp directory and downloads folder if primary location fails
- Configuration system with granular metadata filtering options

## [1.0.3.10] - 2026-01-30

### Fixed
- Fixed Gradle build error in processResources task by using project.version for Gradle 9.3.0 compatibility

## [1.0.3.9] - 2026-01-30

### Changed
- Do not store server address metadata for Realms sessions

## [1.0.3.8] - 2026-01-30

### Changed
- Reduced startup logging and deferred config loading until first use

### Fixed
- Prevented hard crash if screenshot mixin injection fails by making injections non-fatal

## [1.0.3.7] - 2026-01-30

### Changed
- Enabled Gradle build cache and configuration cache to speed up builds

## [1.0.3.6] - 2026-01-30

### Fixed
- Fixed a mixin injection failure on screenshot capture by switching to a stable HEAD injection point

## [1.0.3.5] - 2026-01-30

### Fixed
- Kept the Mod Menu config screen visible in smaller windowed resolutions

## [1.0.3.4] - 2026-01-30

### Changed
- Improved ModMenu config screen layout and toggle label readability

## [1.0.3.1] - 2026-01-30

### Fixed
- Critical bug where metadata was written to wrong screenshot files when taking multiple screenshots in quick succession
  - Implemented pre-save state capture to identify existing files before screenshot save
  - Added filtering logic to only process newly created screenshots
- Fixed IllegalStateException in ModMenu integration with screen rendering

### Changed
- Improved file detection algorithm with pre-save file state tracking
- Enhanced screenshot file selection to lexicographically compare filenames
- Updated to Minecraft 1.21.11 with Fabric Loader 0.18.4

## [1.0.0] - 2025-09-06

### Added
- Initial release of Screenshot Metadata Mod
- Comprehensive metadata collection for Minecraft screenshots
- Dual metadata storage: PNG text chunks and XMP sidecar files
- File Explorer integration for Windows users
- Player information metadata (username, coordinates, UUID)
- World and environment data (dimension, biome, coordinates)
- Timestamp and version information
- Async processing to prevent game performance impact
- Support for Minecraft 1.21.8 with Fabric Loader 0.16.0+

### Features
- Automatic metadata addition with no user action required
- File Explorer visibility on Windows (Properties - Details tab)
- Comprehensive data collection: player name, coordinates (X,Y,Z), world, dimension, biome, timestamp
- Dual format support: embedded PNG metadata and XMP sidecar files
- Clean biome names with user-friendly formatting
- Performance optimized async processing with robust error handling
- Standards compliant XMP format with Dublin Core metadata
