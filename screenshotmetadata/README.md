# 📸 Screenshot Metadata Mod

![Minecraft Version](https://img.shields.io/badge/Minecraft-1.21.x-brightgreen.svg)
![Fabric Loader](https://img.shields.io/badge/Fabric-0.18.4+-blue.svg)
![Java](https://img.shields.io/badge/Java-21+-orange.svg)
![License](https://img.shields.io/badge/License-MIT-lightgrey.svg)

A lightweight and efficient Minecraft Fabric mod that automatically embeds rich metadata into your screenshots. Effortlessly organize, search, and recall exact details about where and when you took your favorite captures.

---

## ✨ Features

### 📊 Comprehensive Metadata Collection
*   **Player Info:** Username, UUID, Experience Level
*   **Location:** Precise Coordinates (X, Y, Z), Look Target (Block/Entity)
*   **World Data:** Dimension, Biome Name & ID, World Seed, Server Address
*   **Environment:** Time of day, Weather state, Light Levels (Sky/Block)
*   **Player Status:** Health, Hunger, Saturation, Active Potion Effects, Player State (Sneaking, Sprinting, Flying, etc.)
*   **Equipment Details:** Main hand, Off hand, and full Armor inventory
*   **Technical Details:** Timestamp, Minecraft Version, Mod Version, Modpack Context
*   **Performance Metrics:** Render Distance, Simulation Distance

### 💾 Flexible Storage Options
Choose how your metadata is saved. You can use any combination of these options:
*   **PNG Text Chunks:** Metadata is invisibly embedded directly into the `.png` file.
*   **XMP Sidecar Files:** Generates a companion `.xmp` file for easy reading by Windows File Explorer and professional photo management software (like Adobe Bridge).
*   **JSON Sidecar Files:** Generates a companion `.json` file for simple parsing and data analysis tools.

### 🖼️ Seamless Integration
*   View all your screenshot metadata directly in the **Windows File Explorer** `Properties -> Details` tab (requires XMP Sidecar).
*   Built-in **Mod Menu** integration for deep customization and preset profiles (Full, Lightweight, Privacy).
*   Custom screenshot naming templates: format your screenshots exactly how you want! (e.g. `2024-05-12_Overworld_X100_Z-50.png`)

---

## 🛠️ Installation

1.  Download the latest **Screenshot Metadata Mod** JAR from the releases page.
2.  Install [Fabric Loader](https://fabricmc.net/use/) (version 0.18.4 or newer).
3.  Install [Fabric API](https://modrinth.com/mod/fabric-api).
4.  Drop the mod JAR into your `.minecraft/mods/` folder.
5.  *(Optional but highly recommended)* Install [Mod Menu](https://modrinth.com/mod/modmenu) to access the in-game configuration screen.

---

## ⚙️ Configuration

Configure the mod easily through the **Mod Menu** interface!

1.  Launch Minecraft and click on the **Mods** button in the main menu.
2.  Find **Screenshot Metadata** and click the **Config** button.
3.  Choose a preset profile, or dive into the custom settings:

### Profiles
*   **Full:** Captures every single piece of data possible.
*   **Lightweight:** Captures only essential world and location metadata (disables performance, player status, and equipment).
*   **Privacy:** Obfuscates coordinates, hides server IP, hashes the world seed, and disables modpack context. Perfect for sharing screenshots publicly!

### Selected Toggles
*   **Output Formats:** Toggle PNG embedded metadata, XMP sidecars, and JSON sidecars independently.
*   **Data Collection:** Fine-tune exactly what gets saved (e.g., disable Potion Effects, but keep Health & Hunger).
*   **Naming Templates:** Create dynamic filename templates using variables like `{date}`, `{time}`, `{dimension}`, `{biome}`, `{x}`, `{y}`, `{z}`, `{world}`, and `{player}`.

---

## 💻 Technical Details

The mod is designed to have **zero impact** on game performance.

*   **Architecture:** The mod intercepts the vanilla screenshot saving process using Mixins.
*   **Asynchronous Processing:** All metadata gathering and file writing happens on an asynchronous background thread. Taking a screenshot will never cause a stutter on the main game thread.
*   **Graceful Degradation:** If the metadata fails to write for any reason, the screenshot itself will still save normally.

---

## 🔨 Development

To compile the mod yourself:

1. Clone the repository: `git clone https://github.com/fentbuscoding/screenshotmetadata.git`
2. Open the directory: `cd screenshotmetadata`

**Build the Stable Project (1.21.x):**
```bash
./gradlew build -PmcProfile=stable
```

**Build the Beta Target:**
```bash
./gradlew build -PmcProfile=beta
```

**Run the Development Environment:**
```bash
./gradlew runClient
```

---

## 🤝 Support & Contributing

*   **Found a bug?** Please report issues on the GitHub issue tracker. Be sure to include your Minecraft version, Fabric Loader version, and the latest log file.
*   **Want to contribute?** Pull requests are welcome! Please ensure you include a clear description of your changes.

---

*Made with ❤️ by fentbuscoding*
