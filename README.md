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
- Fabric Loader — the minimum depends on your Minecraft version, and the jar
  declares it: 0.19.3+ for 26.3, 0.18.4+ for 26.1 - 26.2, 0.17.3+ for 1.21.11,
  0.16.0+ for the older 1.21.x builds
- Fabric API
- Java 21 or newer (Java 25 for the Minecraft 26 builds)

Download the jar that matches your Minecraft version; each one is named
`screenshotmetadata-mc<version>-<mod version>.jar`.

#### Quilt
The Fabric jars run on Quilt Loader as they are (Quilt Loader 0.30+ reports itself
to Fabric mods as Fabric Loader 0.19.5, which meets every jar's minimum). Use the
same `screenshotmetadata-mc<version>-...jar` with Fabric API (or QFAPI, where it
exists for your version). The metadata records `Quilt` as the loader.

#### NeoForge
- Minecraft 1.21.1, 1.21.3 - 1.21.11, or 26.1 - 26.3, with NeoForge for that version
- Jar: `screenshotmetadata-neoforge-mc<version>-<mod version>.jar`
- No other mods needed; the settings screen opens from the **Config** button in
  NeoForge's mod list
- Java 21 (Java 25 for Minecraft 26)

There is no 1.21.2 jar: NeoForge only ever published one beta for 1.21.2 before
moving to 1.21.3. 1.21.6, 1.21.7, 1.21.9, 26.1 and 26.1.1 never got a stable
NeoForge release, so those jars are built against (and published as) beta.

#### Babric (Minecraft Beta 1.7.3)
- A Babric instance (Fabric Loader 0.16+ on Babric's intermediary), Java 17+
- Jar: `screenshotmetadata-babric-b1.7.3-<mod version>.jar`
- No Fabric API or StationAPI needed

#### Better than Adventure!
- BTA 8.0.1 with the BTA build of Fabric Loader (0.18.4-bta.11+), Java 17+
- Jar: `screenshotmetadata-bta8.0.1-<mod version>.jar`
- No Fabric API or HalpLibe needed

Beta 1.7.3 and BTA simply don't have some of the modern data (hunger, potion effects,
experience, off-hand), so those fields are left out. BTA screenshots additionally
record the current season and BTA's named weather. Neither build has a Mod Menu
screen yet: edit `config/screenshotmetadata.json` instead (same settings, same
profiles).

### Setup Steps
1. Download the mod JAR file
2. Place it in your mods folder (usually .minecraft/mods/)
3. Launch Minecraft with Fabric Loader, Quilt Loader or NeoForge
4. Optional (Fabric/Quilt): Install ModMenu for easy configuration

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
| `beta` | the current snapshot (Modrinth only) | 25 |
| `mc26_1`, `mc26_1_1`, `mc26_1_2` | 26.1, 26.1.1, 26.1.2 | 25 |
| `mc26_2`, `mc26_3` | 26.2, 26.3 | 25 |

Babric and Better than Adventure! need different Loom setups (Babric's Loom
extension; BTA's own loader and game manifest), so they are separate Gradle builds
in their own folders rather than profiles. They share the wrapper and the
`src/main/java` code, and take `mod_version` from the main `gradle.properties`:

```
cd screenshotmetadata
./gradlew -p babric build   # babric/build/libs/
./gradlew -p bta build      # bta/build/libs/
./gradlew -p bta runClient  # works for babric too
```

Both run on Java 21 (Loom 1.17) and target Java 17.

NeoForge uses ModDevGradle, so it is a separate build too, with its own profiles
(listed in `neoforge/gradle.properties`, one per Minecraft version):

```
cd screenshotmetadata
./gradlew -p neoforge build -PmcProfile=mc1_21_1   # neoforge/build/libs/
./gradlew -p neoforge runClient -PmcProfile=mc26_3
```

| NeoForge profile | Minecraft | Java |
| --- | --- | --- |
| `mc1_21_1`, `mc1_21_3` ... `mc1_21_11` | 1.21.1, 1.21.3 - 1.21.11 | 21 |
| `mc26_1`, `mc26_1_1`, `mc26_1_2`, `mc26_2`, `mc26_3` | 26.1 - 26.3 | 25 |

Like the main build, it needs Gradle on Java 25 with a JDK 21 also installed.
Quilt needs no build of its own: the Fabric jars are published for Quilt as well.

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
src/main/java/com/fentbuscoding/screenshotmetadata/   (shared by every build)
- ScreenshotMetadataMod.java: Mod constants and init
- platform/Platform.java: What the shared code needs from the loader
- config/: Configuration management
- metadata/: Metadata writers (PNG, XMP, JSON)

src/fabric/...      (Fabric Loader glue: entrypoint, Platform for Fabric/Quilt/Babric/BTA)
src/yarn/java/...   (Fabric on 1.21.x)
src/mojang/java/... (Mojang names: Fabric on 26, NeoForge on 1.21.x and 26)
- mixin/: Minecraft interception hooks
- compat/: ModMenu integration (Fabric only)
src/mojang26/java/...          (26.x: MinecraftCompat)
src/mojang_screen_26_3/java/   (config screen for 26.1 - 26.3, Fabric and NeoForge)
src/mojang_screen_26_4/java/   (config screen for 26.4 and the beta snapshot profile)
src/mojang121/java/...         (1.21.x, NeoForge: MinecraftCompat and the config screen)

neoforge/src/main/... (NeoForge entrypoint, Platform, neoforge.mods.toml)
babric/src/main/...   (Babric build: Beta 1.7.3 mixin, fabric.mod.json)
bta/src/main/...      (Better than Adventure! build: BTA mixin, fabric.mod.json)
```
The Mojang-names mixin is shared by Fabric 26 and every NeoForge version; the few
calls that were renamed between 1.21.x and 26 go through `compat/MinecraftCompat`.
The config screen is split once more because 26.4 changed Screen's render hooks
(`<profile>_screen_api` picks the copy). The Babric and BTA mixins only read the
game state; renaming and writing the metadata goes through
`metadata/LegacyScreenshotSupport.java` in the shared code. The classes that touch
Minecraft directly exist once per naming scheme, because Yarn and the deobfuscated
26 client name the same APIs differently; a change to any of them usually needs the
same change in its siblings.

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
