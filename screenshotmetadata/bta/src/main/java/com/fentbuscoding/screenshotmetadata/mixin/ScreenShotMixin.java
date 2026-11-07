package com.fentbuscoding.screenshotmetadata.mixin;

import com.fentbuscoding.screenshotmetadata.ScreenshotMetadataMod;
import com.fentbuscoding.screenshotmetadata.config.ScreenshotMetadataConfig;
import com.fentbuscoding.screenshotmetadata.metadata.LegacyScreenshotSupport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.PlayerLocal;
import net.minecraft.client.option.GameSettings;
import net.minecraft.client.render.texturepack.TexturePack;
import net.minecraft.client.util.helper.ScreenShot;
import net.minecraft.core.block.Block;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.EntityDispatcher;
import net.minecraft.core.entity.Mob;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.enums.HumanArmorShape;
import net.minecraft.core.enums.LightLayer;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.lang.I18n;
import net.minecraft.core.util.phys.HitResult;
import net.minecraft.core.world.World;
import net.minecraft.core.world.biome.Biome;
import net.minecraft.core.world.pos.TilePos;
import net.minecraft.core.world.pos.TilePosc;
import net.minecraft.core.world.season.Season;
import net.minecraft.core.world.weather.Weather;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Better than Adventure! reads screenshots back from the GPU asynchronously:
 * {@code saveScreenshot} only queues a pixel transfer and picks the file name, and a
 * later {@code tickScreenShot} call writes the PNG once the GPU fence has signalled.
 * The PNG is complete when a {@code tickScreenShot} call that started with a
 * pending transfer returns with the fence consumed.
 */
@Mixin(ScreenShot.class)
public class ScreenShotMixin {
    @Shadow
    private static long fence;

    @Shadow
    private static File destFile;

    @Unique
    private static File screenshotmetadata$pendingFile;

    @Inject(method = "tickScreenShot", at = @At("HEAD"), require = 0)
    private static void screenshotmetadata$beforeTick(CallbackInfoReturnable<String> cir) {
        screenshotmetadata$pendingFile = fence >= 0L ? destFile : null;
    }

    @Inject(method = "tickScreenShot", at = @At("RETURN"), require = 0)
    private static void screenshotmetadata$afterTick(CallbackInfoReturnable<String> cir) {
        File file = screenshotmetadata$pendingFile;
        screenshotmetadata$pendingFile = null;
        // Still waiting on the GPU, or nothing was queued.
        if (file == null || fence >= 0L) {
            return;
        }
        if (!file.isFile() || file.length() <= 0L) {
            return;
        }
        try {
            Minecraft client = Minecraft.getMinecraft();
            if (client == null || client.thePlayer == null || client.currentWorld == null) {
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
        PlayerLocal player = client.thePlayer;
        World world = client.currentWorld;

        if (client.session != null) {
            LegacyScreenshotSupport.putIfPresent(metadata, "Username", client.session.username);
        }
        if (!metadata.containsKey("Username")) {
            LegacyScreenshotSupport.putIfPresent(metadata, "Username", player.username);
        }
        if (player.uuid != null) {
            metadata.put("PlayerUuid", player.uuid.toString());
        }

        int bx = (int) Math.floor(player.x);
        int by = (int) Math.floor(player.y);
        int bz = (int) Math.floor(player.z);
        TilePos playerPos = new TilePos(bx, by, bz);

        if (config.includeCoordinates) {
            int x = bx;
            int y = by;
            int z = bz;
            if (config.privacyMode) {
                x = LegacyScreenshotSupport.roundToNearest(x, 100);
                y = LegacyScreenshotSupport.roundToNearest(y, 100);
                z = LegacyScreenshotSupport.roundToNearest(z, 100);
                metadata.put("CoordinatesObfuscated", "true");
            }
            metadata.put("X", String.valueOf(x));
            metadata.put("Y", String.valueOf(y));
            metadata.put("Z", String.valueOf(z));
            metadata.put("Yaw", String.format("%.1f", player.yRot));
            metadata.put("Pitch", String.format("%.1f", player.xRot));
            metadata.put("Facing", LegacyScreenshotSupport.getFacingDirection(player.yRot));
        }

        if (world.dimension != null) {
            String key = world.dimension.languageKey;
            String dimensionId = "minecraft:" + key;
            metadata.put("World", dimensionId);
            metadata.put("DimensionId", dimensionId);
            String name = LegacyScreenshotSupport.toTitleCase(key);
            metadata.put("Dimension", name != null ? name : dimensionId);
        }

        if (config.includeBiomeInfo) {
            try {
                Biome biome = world.getBlockBiome(playerPos);
                if (biome != null) {
                    String registryKey = biome.getRegistryKey();
                    String fallback = registryKey == null ? null
                        : LegacyScreenshotSupport.toTitleCase(registryKey.substring(registryKey.lastIndexOf('.') + 1));
                    metadata.put("Biome", screenshotmetadata$translate(biome.translationKey, fallback));
                    LegacyScreenshotSupport.putIfPresent(metadata, "BiomeId", registryKey);
                }
            } catch (Exception e) {
                ScreenshotMetadataMod.LOGGER.debug("Could not read biome", e);
            }
            try {
                if (world.getSeasonManager() != null) {
                    Season season = world.getSeasonManager().getCurrentSeason();
                    if (season != null) {
                        LegacyScreenshotSupport.putIfPresent(metadata, "Season", season.getTranslatedName());
                        metadata.put("DayInSeason", String.valueOf(world.getSeasonManager().getDayInSeason()));
                    }
                }
            } catch (Exception e) {
                ScreenshotMetadataMod.LOGGER.debug("Could not read season", e);
            }
        }

        long timeOfDay = Math.floorMod(world.getWorldTime(), 24000L);
        metadata.put("TimeOfDayTicks", String.valueOf(timeOfDay));
        metadata.put("TimeOfDay", LegacyScreenshotSupport.formatTimeOfDay(timeOfDay));

        if (config.includeWeatherInfo) {
            try {
                Weather weather = world.getCurrentWeather();
                if (weather != null) {
                    String key = weather.getLanguageKey();
                    String fallback = key == null ? "Clear"
                        : LegacyScreenshotSupport.toTitleCase(key.substring(key.lastIndexOf('.') + 1));
                    boolean thundering = key != null && key.contains("storm");
                    metadata.put("Weather", screenshotmetadata$translate(weather.getNameLanguageKey(), fallback));
                    metadata.put("IsRaining", String.valueOf(weather.isDamp()));
                    metadata.put("IsThundering", String.valueOf(thundering));
                }
                if (world.getWeatherManager() != null) {
                    metadata.put("WeatherIntensity", String.format("%.2f", world.getWeatherManager().getWeatherIntensity()));
                }
            } catch (Exception e) {
                ScreenshotMetadataMod.LOGGER.debug("Could not read weather", e);
            }
        }

        if (client.isMultiplayerWorld()) {
            // BTA does not keep a reference to the server entry that was joined.
            metadata.put("ServerType", "Multiplayer");
        } else {
            metadata.put("ServerType", "Singleplayer");
            if (world.getLevelData() != null) {
                LegacyScreenshotSupport.putIfPresent(metadata, "WorldName", world.getLevelData().getWorldName());
            }
            if (config.includeWorldSeed) {
                long seed = world.getRandomSeed();
                if (config.privacyMode) {
                    metadata.put("WorldSeed", LegacyScreenshotSupport.hashSeed(seed));
                    metadata.put("WorldSeedHashed", "true");
                } else {
                    metadata.put("WorldSeed", String.valueOf(seed));
                }
            }
        }

        // getMinecraftVersion() rather than the VERSION constant, which javac would inline.
        LegacyScreenshotSupport.putCommonMetadata(metadata, "BTA " + client.getMinecraftVersion());

        if (config.includePlayerStatus) {
            if (world.getDifficulty() != null) {
                metadata.put("Difficulty", world.getDifficulty().name().toLowerCase());
            }
            if (player.getGamemode() != null) {
                // Ids are namespaced, e.g. "minecraft:gamemode/survival".
                String id = player.getGamemode().getId();
                if (id != null) {
                    id = id.substring(Math.max(id.lastIndexOf(':'), id.lastIndexOf('/')) + 1);
                    LegacyScreenshotSupport.putIfPresent(metadata, "GameMode", LegacyScreenshotSupport.toTitleCase(id));
                }
            }
            metadata.put("Health", String.format("%.1f", (float) player.getHealth()));
            metadata.put("MaxHealth", String.format("%.1f", (float) player.getMaxHealth()));
            metadata.put("Score", String.valueOf(player.getScore()));
        }

        if (config.includePerformanceMetrics) {
            metadata.put("CaptureTimeMs", String.valueOf(System.currentTimeMillis()));
            try {
                Integer renderDistance = GameSettings.RENDER_DISTANCE.value;
                if (renderDistance != null) {
                    metadata.put("RenderDistance", String.valueOf(renderDistance));
                }
            } catch (Exception e) {
                ScreenshotMetadataMod.LOGGER.debug("Could not read render distance", e);
            }
        }

        if (config.includeEquipment) {
            ItemStack hand = player.getHeldItem();
            if (screenshotmetadata$hasItem(hand)) {
                metadata.put("MainHandItem", screenshotmetadata$itemName(hand));
                metadata.put("MainHandCount", String.valueOf(hand.stackSize));
            }
            HumanArmorShape[] slots = {HumanArmorShape.HEAD, HumanArmorShape.CHEST, HumanArmorShape.LEGS, HumanArmorShape.BOOTS};
            String[] names = {"Head", "Chest", "Legs", "Feet"};
            for (int i = 0; i < slots.length; i++) {
                ItemStack armor = player.getItemInArmorSlot(slots[i]);
                if (screenshotmetadata$hasItem(armor)) {
                    metadata.put("Armor" + names[i], screenshotmetadata$itemName(armor));
                }
            }
        }

        if (config.includePlayerState) {
            metadata.put("IsSneaking", String.valueOf(player.isSneaking()));
            metadata.put("IsSprinting", String.valueOf(player.isSprinting()));
            metadata.put("IsOnGround", String.valueOf(player.onGround));
            metadata.put("IsOnFire", String.valueOf(player.isOnFire()));
            metadata.put("IsInWater", String.valueOf(player.isInWater()));
            metadata.put("IsRiding", String.valueOf(player.isPassenger()));
        }

        if (config.includeLightLevels && world.isBlockLoaded(playerPos)) {
            metadata.put("LightLevelBlock", String.valueOf(world.getSavedLightValue(LightLayer.Block, playerPos)));
            metadata.put("LightLevelSky", String.valueOf(world.getSavedLightValue(LightLayer.Sky, playerPos)));
        }

        if (config.includeLookTarget && client.objectMouseOver != null) {
            screenshotmetadata$addLookTarget(client.objectMouseOver, world, metadata);
        }

        return metadata;
    }

    @Unique
    private static void screenshotmetadata$addLookTarget(HitResult hit, World world, Map<String, String> metadata) {
        try {
            if (hit instanceof HitResult.Tile tileHit) {
                TilePosc pos = tileHit.tilePos;
                Block<?> block = world.getBlockType(pos);
                if (block == null) {
                    return;
                }
                String languageKey = block.getLanguageKey(world.getBlockData(pos));
                metadata.put("LookingAtBlock", screenshotmetadata$translate(languageKey + ".name", block.getKey()));
                if (block.namespaceId() != null) {
                    metadata.put("LookingAtBlockId", block.namespaceId().toString());
                }
                metadata.put("LookingAtBlockX", String.valueOf(pos.x()));
                metadata.put("LookingAtBlockY", String.valueOf(pos.y()));
                metadata.put("LookingAtBlockZ", String.valueOf(pos.z()));
                if (tileHit.side != null) {
                    metadata.put("LookingAtBlockSide", tileHit.side.name().toLowerCase());
                }
            } else if (hit instanceof HitResult.Entity entityHit && entityHit.entity != null) {
                Entity entity = entityHit.entity;
                if (entity instanceof Player other) {
                    metadata.put("LookingAtEntity", "minecraft:player");
                    LegacyScreenshotSupport.putIfPresent(metadata, "LookingAtEntityName", other.username);
                    if (other.uuid != null) {
                        metadata.put("LookingAtEntityUUID", other.uuid.toString());
                    }
                    return;
                }
                EntityDispatcher.EntityDispatcherEntry<?> entry = EntityDispatcher.getInstance().entryForClass(entity.getClass());
                metadata.put("LookingAtEntity", entry != null && entry.namespaceID != null
                    ? entry.namespaceID.toString()
                    : entity.getClass().getSimpleName());
                if (entity instanceof Mob mob && mob.nickname != null && !mob.nickname.isBlank()) {
                    metadata.put("LookingAtEntityName", mob.nickname);
                }
            }
        } catch (Exception e) {
            ScreenshotMetadataMod.LOGGER.debug("Could not collect look target", e);
        }
    }

    @Unique
    private static List<String> screenshotmetadata$resourcePacks(Minecraft client) {
        List<String> packs = new ArrayList<>();
        try {
            if (client.texturePackList != null && client.texturePackList.selectedTexturePacks() != null) {
                for (TexturePack pack : client.texturePackList.selectedTexturePacks()) {
                    if (pack == null) {
                        continue;
                    }
                    String name = pack.fileName != null ? pack.fileName : pack.packId;
                    if (name != null) {
                        packs.add(name);
                    }
                }
            }
        } catch (Exception e) {
            ScreenshotMetadataMod.LOGGER.debug("Could not read texture packs", e);
        }
        return packs;
    }

    @Unique
    private static boolean screenshotmetadata$hasItem(ItemStack stack) {
        return stack != null && stack.stackSize > 0 && stack.getItem() != null;
    }

    @Unique
    private static String screenshotmetadata$itemName(ItemStack stack) {
        try {
            String name = stack.getDisplayName();
            if (name != null && !name.isBlank()) {
                return name;
            }
        } catch (Exception ignored) {
            // Fall through to the item key.
        }
        return stack.getItemKey();
    }

    @Unique
    private static String screenshotmetadata$translate(String key, String fallback) {
        if (key == null) {
            return fallback != null ? fallback : "Unknown";
        }
        try {
            String translated = I18n.getInstance().translateKey(key);
            if (translated != null && !translated.isBlank() && !translated.equals(key)) {
                return translated;
            }
        } catch (Exception ignored) {
            // Fall through.
        }
        return fallback != null ? fallback : key;
    }
}
