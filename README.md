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
- Profiles: One-click capture profiles in Mod Menu (Full, Lightweight, Privacy)

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
- Minecraft 1.21.1 - 1.21.11, or 26.1 - 26.3
- Fabric Loader 0.18.4 or newer
- Fabric API
- Java 21 or newer (Java 25 for the Minecraft 26 builds)

Download the jar that matches your Minecraft version; each one is named
`screenshotmetadata-mc<version>-<mod version>.jar`.

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

### Configuration
1. Open Minecraft
2. From main menu, click Mods
3. Find Screenshot Metadata and click Config
4. Pick a capture profile in Mod Menu (Full, Lightweight, Privacy)
5. Optionally fine-tune individual toggles
6. Click Save and Close

### Toggle Options
- Capture Profiles: Apply curated metadata presets
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

### Build a Specific Minecraft Version
Every target is a build profile; pass it with `-PmcProfile`:

```
cd screenshotmetadata
./gradlew build -PmcProfile=mc26_3
```

| Profile | Minecraft | Java |
| --- | --- | --- |
| `stable` | 1.21.11 | 21 |
| `mc1_21_1` ... `mc1_21_10` | 1.21.1 - 1.21.10 | 21 |
| `beta` | 25w46a (26.1 snapshot line) | 25 |
| `mc26_1`, `mc26_1_1`, `mc26_1_2` | 26.1, 26.1.1, 26.1.2 | 25 |
| `mc26_2`, `mc26_3` | 26.2, 26.3 | 25 |

Minecraft 26 ships a deobfuscated client, so there are no Yarn or Mojang mappings to
apply to it. Those profiles set `mappings_channel=none` and build with Loom's
no-remap plugin, which means the game, the mod and its dependencies all share one
set of names. Everything up to 1.21.11 still builds against Yarn mappings as before.

Gradle itself has to run on Java 25, because Fabric Loom 1.18 requires it. The
1.21.x profiles still *compile* against a Java 21 toolchain, so keep a JDK 21
installed as well and Gradle will find it.

### Run in Development
```
./gradlew runClient
```

### Project Structure
```
src/main/java/com/fentbuscoding/screenshotmetadata/   (shared by every profile)
- ScreenshotMetadataMod.java: Main mod initialization
- config/: Configuration management
- metadata/: Metadata writers (PNG, XMP, JSON)

src/yarn/java/...   (used by the 1.21.x and snapshot profiles)
src/mojang/java/... (used by the Minecraft 26 profiles)
- mixin/: Minecraft interception hooks
- compat/: Mod compatibility (ModMenu integration)
```
The two classes that touch Minecraft directly exist once per naming scheme, because
Yarn and the deobfuscated 26 client name the same APIs differently. A change to
either one usually needs the same change in the other.

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
