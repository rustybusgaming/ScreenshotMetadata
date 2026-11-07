package com.fentbuscoding.screenshotmetadata.mixin;

import com.fentbuscoding.screenshotmetadata.ScreenshotMetadataMod;
import com.fentbuscoding.screenshotmetadata.config.ScreenshotMetadataConfig;
import com.fentbuscoding.screenshotmetadata.metadata.LegacyScreenshotSupport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.resource.language.TranslationStorage;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityRegistry;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.block.Block;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResultType;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Babric (Minecraft Beta 1.7.3) writes screenshots synchronously on the render
 * thread from {@code Screenshot.take}, so the file it hands to {@code ImageIO.write}
 * is complete by the time the method returns.
 */
@Mixin(Screenshot.class)
public class ScreenshotMixin {
    @Unique
    private static final String MINECRAFT_VERSION = "b1.7.3";

    @Unique
    private static File screenshotmetadata$savedFile;

    @ModifyArg(
        method = "take",
        at = @At(value = "INVOKE",
            target = "Ljavax/imageio/ImageIO;write(Ljava/awt/image/RenderedImage;Ljava/lang/String;Ljava/io/File;)Z"),
        index = 2,
        require = 0)
    private static File screenshotmetadata$captureFile(File file) {
        screenshotmetadata$savedFile = file;
        return file;
    }

    @Inject(method = "take", at = @At("RETURN"), require = 0)
    private static void screenshotmetadata$onTaken(File runDirectory, int width, int height,
                                                  CallbackInfoReturnable<String> cir) {
        File file = screenshotmetadata$savedFile;
        screenshotmetadata$savedFile = null;
        if (file == null || !file.isFile() || file.length() <= 0L) {
            return;
        }
        try {
            Minecraft client = MinecraftAccessor.screenshotmetadata$getInstance();
            if (client == null || client.player == null || client.world == null) {
                ScreenshotMetadataMod.LOGGER.warn("Cannot add metadata: client, player or world is null");
                return;
            }
            Map<String, String> metadata = screenshotmetadata$collect(client);
            LegacyScreenshotSupport.processAsync(file, metadata, screenshotmetadata$resourcePacks(client));
        } catch (Exception e) {
            ScreenshotMetadataMod.LOGGER.error("Failed to collect screenshot metadata", e);
        }
    }

    @Unique
    private static Map<String, String> screenshotmetadata$collect(Minecraft client) {
        Map<String, String> metadata = new HashMap<>();
        ScreenshotMetadataConfig config = ScreenshotMetadataConfig.get();
        ClientPlayerEntity player = client.player;
        World world = client.world;

        if (client.session != null) {
            LegacyScreenshotSupport.putIfPresent(metadata, "Username", client.session.username);
        }
        if (!metadata.containsKey("Username")) {
            LegacyScreenshotSupport.putIfPresent(metadata, "Username", player.name);
        }

        if (config.includeCoordinates) {
            int x = (int) Math.floor(player.x);
            int y = (int) Math.floor(player.y);
            int z = (int) Math.floor(player.z);
            if (config.privacyMode) {
                x = LegacyScreenshotSupport.roundToNearest(x, 100);
                y = LegacyScreenshotSupport.roundToNearest(y, 100);
                z = LegacyScreenshotSupport.roundToNearest(z, 100);
                metadata.put("CoordinatesObfuscated", "true");
            }
            metadata.put("X", String.valueOf(x));
            metadata.put("Y", String.valueOf(y));
            metadata.put("Z", String.valueOf(z));
            metadata.put("Yaw", String.format("%.1f", player.yaw));
            metadata.put("Pitch", String.format("%.1f", player.pitch));
            metadata.put("Facing", LegacyScreenshotSupport.getFacingDirection(player.yaw));
        }

        String dimensionId = screenshotmetadata$dimensionId(world.dimension != null ? world.dimension.id : player.dimensionId);
        metadata.put("World", dimensionId);
        metadata.put("DimensionId", dimensionId);
        metadata.put("Dimension", screenshotmetadata$dimensionName(dimensionId));

        if (config.includeBiomeInfo) {
            try {
                Biome biome = world.method_1781().getBiome((int) Math.floor(player.x), (int) Math.floor(player.z));
                if (biome != null && biome.name != null) {
                    metadata.put("Biome", biome.name);
                    metadata.put("BiomeId", "minecraft:" + biome.name.toLowerCase().replace(' ', '_'));
                }
            } catch (Exception e) {
                ScreenshotMetadataMod.LOGGER.debug("Could not read biome", e);
            }
        }

        long timeOfDay = Math.floorMod(world.getTime(), 24000L);
        metadata.put("TimeOfDayTicks", String.valueOf(timeOfDay));
        metadata.put("TimeOfDay", LegacyScreenshotSupport.formatTimeOfDay(timeOfDay));

        if (config.includeWeatherInfo) {
            boolean raining = world.isRaining();
            boolean thundering = world.isThundering();
            metadata.put("Weather", thundering ? "Thunder" : (raining ? "Rain" : "Clear"));
            metadata.put("IsRaining", String.valueOf(raining));
            metadata.put("IsThundering", String.valueOf(thundering));
            metadata.put("RainGradient", String.format("%.2f", world.getRainGradient(1.0f)));
            metadata.put("ThunderGradient", String.format("%.2f", world.getThunderGradient(1.0f)));
        }

        if (client.isWorldRemote()) {
            // Beta 1.7.3 keeps no record of which server the player joined.
            metadata.put("ServerType", "Multiplayer");
        } else {
            metadata.put("ServerType", "Singleplayer");
            if (world.getProperties() != null) {
                LegacyScreenshotSupport.putIfPresent(metadata, "WorldName", world.getProperties().getName());
            }
            if (config.includeWorldSeed) {
                long seed = world.getSeed();
                if (config.privacyMode) {
                    metadata.put("WorldSeed", LegacyScreenshotSupport.hashSeed(seed));
                    metadata.put("WorldSeedHashed", "true");
                } else {
                    metadata.put("WorldSeed", String.valueOf(seed));
                }
            }
        }

        LegacyScreenshotSupport.putCommonMetadata(metadata, MINECRAFT_VERSION);

        if (config.includePlayerStatus) {
            metadata.put("Difficulty", screenshotmetadata$difficultyName(world.difficulty));
            // Beta 1.7.3 only has survival; creative arrived in Beta 1.8.
            metadata.put("GameMode", "Survival");
            metadata.put("Health", String.format("%.1f", (float) player.health));
            metadata.put("MaxHealth", String.format("%.1f", (float) (player.maxHealth > 0 ? player.maxHealth : 20)));
        }

        if (config.includePerformanceMetrics) {
            metadata.put("CaptureTimeMs", String.valueOf(System.currentTimeMillis()));
            if (client.options != null) {
                int setting = Math.max(0, Math.min(3, client.options.viewDistance));
                // Far, Normal, Short and Tiny render 16, 8, 4 and 2 chunks out.
                metadata.put("RenderDistance", String.valueOf(16 >> setting));
                metadata.put("RenderDistanceSetting", new String[]{"Far", "Normal", "Short", "Tiny"}[setting]);
            }
        }

        if (config.includeEquipment) {
            ItemStack hand = player.getHand();
            if (hand != null && hand.count > 0) {
                metadata.put("MainHandItem", screenshotmetadata$itemName(hand));
                metadata.put("MainHandCount", String.valueOf(hand.count));
            }
            if (player.inventory != null && player.inventory.armor != null) {
                // Beta stores armour feet-first.
                String[] slots = {"Feet", "Legs", "Chest", "Head"};
                ItemStack[] armor = player.inventory.armor;
                for (int i = 0; i < Math.min(slots.length, armor.length); i++) {
                    if (armor[i] != null && armor[i].count > 0) {
                        metadata.put("Armor" + slots[i], screenshotmetadata$itemName(armor[i]));
                    }
                }
            }
        }

        if (config.includePlayerState) {
            metadata.put("IsSneaking", String.valueOf(player.isSneaking()));
            metadata.put("IsOnGround", String.valueOf(player.onGround));
            metadata.put("IsOnFire", String.valueOf(player.isOnFire()));
            metadata.put("IsInWater", String.valueOf(player.isSubmergedInWater()));
            metadata.put("IsRiding", String.valueOf(player.hasVehicle()));
            metadata.put("IsSleeping", String.valueOf(player.isSleeping()));
        }

        if (config.includeLightLevels) {
            int bx = (int) Math.floor(player.x);
            int by = (int) Math.floor(player.y);
            int bz = (int) Math.floor(player.z);
            if (world.isPosLoaded(bx, by, bz)) {
                metadata.put("LightLevelBlock", String.valueOf(world.getBrightness(LightType.BLOCK, bx, by, bz)));
                metadata.put("LightLevelSky", String.valueOf(world.getBrightness(LightType.SKY, bx, by, bz)));
            }
        }

        if (config.includeLookTarget && client.crosshairTarget != null) {
            screenshotmetadata$addLookTarget(client.crosshairTarget, world, metadata);
        }

        return metadata;
    }

    @Unique
    private static void screenshotmetadata$addLookTarget(HitResult hit, World world, Map<String, String> metadata) {
        try {
            if (hit.type == HitResultType.BLOCK) {
                int blockId = world.getBlockId(hit.blockX, hit.blockY, hit.blockZ);
                Block block = blockId > 0 && blockId < Block.BLOCKS.length ? Block.BLOCKS[blockId] : null;
                if (block == null) {
                    return;
                }
                String name = screenshotmetadata$translate(block.getTranslationKey());
                metadata.put("LookingAtBlock", name);
                metadata.put("LookingAtBlockNumericId", String.valueOf(blockId));
                metadata.put("LookingAtBlockX", String.valueOf(hit.blockX));
                metadata.put("LookingAtBlockY", String.valueOf(hit.blockY));
                metadata.put("LookingAtBlockZ", String.valueOf(hit.blockZ));
                metadata.put("LookingAtBlockSide", screenshotmetadata$sideName(hit.side));
            } else if (hit.type == HitResultType.ENTITY && hit.entity != null) {
                Entity entity = hit.entity;
                if (entity instanceof PlayerEntity other) {
                    metadata.put("LookingAtEntity", "Player");
                    LegacyScreenshotSupport.putIfPresent(metadata, "LookingAtEntityName", other.name);
                } else {
                    String id = EntityRegistry.getId(entity);
                    metadata.put("LookingAtEntity", id != null ? id : entity.getClass().getSimpleName());
                }
                metadata.put("LookingAtEntityNumericId", String.valueOf(entity.id));
            }
        } catch (Exception e) {
            ScreenshotMetadataMod.LOGGER.debug("Could not collect look target", e);
        }
    }

    @Unique
    private static List<String> screenshotmetadata$resourcePacks(Minecraft client) {
        List<String> packs = new ArrayList<>();
        try {
            if (client.texturePacks != null && client.texturePacks.selected != null) {
                String name = client.texturePacks.selected.name;
                packs.add(name != null ? name : "Default");
            }
        } catch (Exception e) {
            ScreenshotMetadataMod.LOGGER.debug("Could not read texture pack", e);
        }
        return packs;
    }

    @Unique
    private static String screenshotmetadata$itemName(ItemStack stack) {
        try {
            return screenshotmetadata$translate(stack.getTranslationKey());
        } catch (Exception e) {
            return "Item " + stack.itemId;
        }
    }

    @Unique
    private static String screenshotmetadata$translate(String key) {
        if (key == null) {
            return "Unknown";
        }
        String translated = TranslationStorage.getInstance().getClientTranslation(key);
        return translated == null || translated.isBlank() ? key : translated;
    }

    @Unique
    private static String screenshotmetadata$dimensionId(int id) {
        return switch (id) {
            case 0 -> "minecraft:overworld";
            case -1 -> "minecraft:the_nether";
            case 1 -> "minecraft:skylands";
            default -> "minecraft:dimension_" + id;
        };
    }

    @Unique
    private static String screenshotmetadata$dimensionName(String id) {
        return switch (id) {
            case "minecraft:overworld" -> "Overworld";
            case "minecraft:the_nether" -> "Nether";
            case "minecraft:skylands" -> "Skylands";
            default -> LegacyScreenshotSupport.toTitleCase(id.substring(id.indexOf(':') + 1));
        };
    }

    @Unique
    private static String screenshotmetadata$difficultyName(int difficulty) {
        return switch (difficulty) {
            case 0 -> "peaceful";
            case 1 -> "easy";
            case 2 -> "normal";
            case 3 -> "hard";
            default -> String.valueOf(difficulty);
        };
    }

    @Unique
    private static String screenshotmetadata$sideName(int side) {
        return switch (side) {
            case 0 -> "down";
            case 1 -> "up";
            case 2 -> "north";
            case 3 -> "south";
            case 4 -> "west";
            case 5 -> "east";
            default -> String.valueOf(side);
        };
    }
}
