# Screenshot Metadata Mod

A Minecraft Fabric mod that automatically adds comprehensive metadata to your screenshots, making them easier to organize and search.

## 🎯 Features

### **Comprehensive Metadata Collection**
- **Player Information**: Username and current coordinates (X, Y, Z)
- **World Data**: Current dimension/world (Overworld, Nether, End)
- **Environment**: Biome information (cleaned up for readability)
- **Technical**: Timestamp, Minecraft version, mod version

### **Dual Storage Format**
- **PNG Text Chunks**: Embedded directly in image files for technical tools
- **XMP Sidecar Files**: Separate `.xmp` files for Windows File Explorer visibility

### **File Explorer Integration**
- Metadata appears in Windows File Explorer Properties → Details
- Professional XMP format recognized by photo management software
- Easy to search and organize your screenshot collection

## 📸 What You Get

When you take a screenshot, the mod creates:
- `screenshot.png` - Your image with embedded metadata
- `screenshot.xmp` - Metadata file for File Explorer visibility

### **Example Metadata**
- **Title**: "Minecraft - Player900"
- **Description**: "Minecraft Screenshot - Player: Player900 | World: minecraft:overworld | Coords: (122, 80, 172) | Biome: Frozen Ocean"
- **Creator**: Your Minecraft username
- **Coordinates**: Individual X, Y, Z values
- **Timestamp**: When the screenshot was taken

## 🚀 Installation

1. **Requirements**: 
   - Minecraft 1.20.4
   - Fabric Loader 0.15.11+
   - Fabric API
   - Java 21+

2. **Install**:
   - Download the mod JAR file
   - Place in your `mods` folder
   - Launch Minecraft with Fabric

## 🎮 Usage

1. **Take screenshots normally** (F2 key)
2. **Find your screenshots** in `.minecraft/screenshots/`
3. **View metadata** by right-clicking → Properties → Details in File Explorer
4. **Technical users** can read PNG text chunks with tools like ExifTool

## 🔧 Technical Details

### **Architecture**
- **Package**: `com.fentbuscoding.screenshotmetadata`
- **Main Class**: `ScreenshotMetadataMod`
- **Mixin**: Intercepts vanilla screenshot saving process
- **Async Processing**: Metadata addition doesn't block game performance

### **Metadata Storage**
- **PNG tEXt Chunks**: Standard PNG metadata format
- **XMP Sidecars**: Adobe XMP standard with Dublin Core metadata
- **Custom Namespace**: `http://fentbuscoding.com/minecraft/ns/` for Minecraft-specific data

### **Error Handling**
- Comprehensive logging with SLF4J
- Graceful failure - screenshots work even if metadata fails
- Async processing prevents game thread blocking

## 🛠️ Development

### **Building**
```bash
./gradlew build
```

### **Testing**
```bash
./gradlew runClient
```

### **Structure**
```
src/main/java/com/fentbuscoding/screenshotmetadata/
├── ScreenshotMetadataMod.java          # Main mod class
├── metadata/
│   ├── PngMetadataWriter.java          # PNG text chunk writer
│   └── XmpSidecarWriter.java           # XMP sidecar file creator
└── mixin/
    └── ScreenshotRecorderMixin.java    # Screenshot interception
```

## 📝 License

MIT License - See LICENSE file for details.

## 🤝 Contributing

Contributions welcome! Please read the contributing guidelines and submit pull requests.

## 📋 Version History

- **v1.0.0**: Initial release with PNG and XMP metadata support
  - Comprehensive metadata collection
  - File Explorer integration
  - Async processing
  - Professional code structure

## 🐛 Issues

Report issues on the GitHub repository with:
- Minecraft version
- Fabric Loader version
- Log files (with debug enabled)
- Steps to reproduce

---

**Made with ❤️ by fentbuscoding**
