# Changelog

## [1.0.3.4] - 2026-01-30

### Changed
- Upgraded ModMenu config screen with clearer layout, section headers, and color-coded toggle labels
- Improved toggle text to show explicit ON/OFF state for faster readability

## [1.0.3.1] - 2026-01-30

### Fixed
- **Critical Bug**: Mod was writing metadata to old screenshot files instead of newly created ones
  - Implemented pre-save state capture to identify existing files before screenshot save
  - Added filtering logic to only process newly created screenshots
  - Fixes issue where metadata appeared on wrong screenshots when taking multiple in quick succession
- **ModMenu Crash**: Fixed IllegalStateException "Can only blur once per frame" in ModMenu integration
  - Reordered render() calls to prevent double background blur
  - Ensures compatibility with ModMenu screen rendering

### Changed
- Improved file detection algorithm to use pre-save file state
- Enhanced screenshot file selection to lexicographically compare filenames (which contain timestamps)
- Updated to Minecraft 1.21.11 with Fabric Loader 0.18.4

### Technical Improvements
- Added LAST_SCREENSHOT_FILE ThreadLocal for pre-save state tracking
- Implemented dual-injection approach: capture state before save, process after save
- File filtering now compares both filename and lastModified timestamp
- Maintains existing retry and file stability checking logic

## [1.0.0] - 2025-09-06

### Added
- Initial release of Screenshot Metadata Mod
- Comprehensive metadata collection for Minecraft screenshots
- Dual metadata storage: PNG text chunks and XMP sidecar files
- File Explorer integration for Windows users
- Player information metadata (username, coordinates)
- World and environment data (dimension, biome)
- Timestamp and version information
- Async processing to prevent game performance impact
- Professional code structure with proper error handling
- Support for Minecraft 1.21.8 with Fabric

### Features
- **Automatic Metadata Addition**: No user action required, works automatically with F2 screenshots
- **File Explorer Visibility**: Metadata appears in Windows Properties → Details
- **Comprehensive Data**: Player name, coordinates (X,Y,Z), world/dimension, biome, timestamp
- **Dual Format Support**: Both embedded PNG metadata and XMP sidecar files
- **Clean Biome Names**: User-friendly biome names instead of technical references
- **Performance Optimized**: Async processing with robust error handling
- **Standards Compliant**: Proper XMP format with Dublin Core metadata

### Technical Details
- **Package**: com.fentbuscoding.screenshotmetadata
- **Minecraft Version**: 1.21.8
- **Fabric Loader**: 0.16.0+
- **Java Version**: 21+
- **Dependencies**: Fabric API

### Requirements
- Minecraft 1.21.8
- Fabric Loader 0.16.0 or higher
- Fabric API
- Java 21 or higher

### Installation
1. Download the mod JAR file
2. Place in your Minecraft mods folder
3. Launch Minecraft with Fabric
4. Take screenshots as normal (F2 key)
5. Check File Explorer properties to see metadata!
