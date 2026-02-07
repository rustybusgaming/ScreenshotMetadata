# Changelog

All notable changes to the Screenshot Metadata Mod are documented here. This changelog focuses on functional changes to the mod itself.

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
