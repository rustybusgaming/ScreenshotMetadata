# Screenshot Metadata Mod

A Minecraft Fabric mod that automatically adds comprehensive metadata to your screenshots, making them easier to organize and search.

## Features

### Comprehensive Metadata Collection
- Player Information: Username and current coordinates (X, Y, Z)
- World Data: Current dimension and world information (Overworld, Nether, End)
- Environment: Biome information with readable formatting
- Technical Details: Timestamp, Minecraft version, and mod version
- Player Status: Health, hunger, potion effects, and equipped items
- Performance Metrics: Render distance and simulation distance
- Equipment Details: Full armor and item inventory tracking
- Tags: Optional user-supplied tags for searching and organization

### Flexible Storage Options
- PNG Text Chunks: Embedded directly in image files for technical tools
- XMP Sidecar Files: Separate .xmp files for Windows File Explorer compatibility
- JSON Sidecar Files: Easy-to-read JSON format for data analysis

### File Explorer Integration
- View metadata in Windows File Explorer Properties - Details tab
- Professional XMP format recognized by photo management software
- Easily search and organize your screenshot collection

## What Gets Saved

When you take a screenshot, the mod creates:
- screenshot.png: Your image with embedded metadata
- screenshot.xmp: Metadata in XMP format for file managers
- screenshot.json: Metadata in JSON format for easy parsing

### Example Metadata Collected
- Username and player UUID
- Player coordinates (X, Y, Z) and direction (Yaw/Pitch)
- Current biome and dimension
- World name and server information
- Current game time and timestamp
- Health and hunger levels
- Active potion effects with durations
- Equipped armor and items
- Render and simulation distances
- World difficulty and game mode

## Installation

### Requirements
- Minecraft 1.21.11
- Fabric Loader 0.18.4 or newer
- Fabric API
- Java 21 or newer

### Setup Steps
1. Download the mod JAR file
2. Place it in your mods folder (usually .minecraft/mods/)
3. Launch Minecraft with Fabric Loader
4. Optional: Install ModMenu for easy configuration

## Usage

### Basic Usage
1. Press F2 to take a screenshot
2. Find your screenshots in .minecraft/screenshots/
3. Right-click any screenshot and select Properties
4. View metadata in the Details tab

### Tagging Screenshots
1. Press `T` (default) to open the tag input screen
2. Type comma-separated tags or click preset buttons
3. Click Save to apply tags to the next screenshot
4. Configure presets in ModMenu under the Tags section

### Configuration
1. Open Minecraft
2. From main menu, click Mods
3. Find Screenshot Metadata and click Config
4. Toggle options for what metadata to include
5. Click Save and Close

### Toggle Options
- PNG Metadata: Embed data in PNG chunks
- XMP Sidecar: Create XMP companion files
- JSON Sidecar: Create JSON companion files
- World Seed: Include the world seed
- Biome Info: Record biome name and ID
- Coordinates: Log player position and angles
- Health and Hunger: Track health and food status
- Potion Effects: Record active status effects
- Armor and Items: Log equipped items and armor
- Performance Metrics: Record render and simulation distance
- Tag Presets: Comma-separated presets for the tag input screen

## Technical Details

### Architecture
- Package: com.fentbuscoding.screenshotmetadata
- Main Class: ScreenshotMetadataMod
- Mixin Target: Intercepts vanilla screenshot saving process
- Processing: Async to prevent game performance impact

### Metadata Storage Formats
- PNG tEXt Chunks: Standard PNG metadata format
- XMP Sidecars: Adobe XMP standard with Dublin Core metadata
- JSON Sidecars: Simple key-value pairs for easy parsing

### Error Handling
- Comprehensive logging with SLF4J
- Graceful failure: screenshots work even if metadata fails
- Async processing prevents game thread blocking
- File detection with exponential backoff retry logic
- Fallback file location checking (temp directory, downloads folder)

## Development

### Build the Project
```
cd screenshotmetadata
./gradlew build -PmcProfile=stable
```

### Build Beta Target
```
cd screenshotmetadata
./gradlew build -PmcProfile=beta
```
Beta profile uses official Mojang mappings when Yarn is unavailable.

### Run in Development
```
./gradlew runClient
```

### Project Structure
```
src/main/java/com/fentbuscoding/screenshotmetadata/
- ScreenshotMetadataMod.java: Main mod initialization
- config/: Configuration management
- metadata/: Metadata writers (PNG, XMP, JSON)
- mixin/: Minecraft interception hooks
- compat/: Mod compatibility (ModMenu integration)
```

## License

MIT License - See LICENSE file for details.

## Support

Report issues on the GitHub repository with:
- Your Minecraft version
- Your Fabric Loader version
- The complete log file (logs/latest.log)
- Steps to reproduce the issue

## Contributing

Contributions are welcome! Please submit pull requests with:
- Clear description of changes
- Tests where applicable
- Updated documentation

---

Made by fentbuscoding
