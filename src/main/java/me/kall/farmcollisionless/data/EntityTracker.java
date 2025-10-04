package me.kall.farmcollisionless.data;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import me.kall.farmcollisionless.FarmCollisionless;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, modid = FarmCollisionless.MOD_ID)
public class EntityTracker {
    private static final Map<ResourceLocation, Long2ObjectMap<Map<ResourceLocation, Set<UUID>>>> ENTITIES = new Object2ObjectOpenHashMap<>();

    public static ResourceLocation type(@NotNull Entity entity) {
        return ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
    }

    public static @NotNull @UnmodifiableView Set<UUID> getEntities(@NotNull ServerLevel level, long chunkPos, ResourceLocation entityType) {
        if (!level.getServer().isSameThread()) throw new UnsupportedOperationException("Entities reading is only available on the server thread.");
        return Collections.unmodifiableSet(ENTITIES.getOrDefault(level.dimension().location(), Long2ObjectMaps.emptyMap())
                .getOrDefault(chunkPos, Collections.emptyMap())
                .getOrDefault(entityType, Collections.emptySet()));
    }

    public static void removeEntity(ResourceLocation dim, long chunkPos, ResourceLocation type, UUID uuid, boolean isEnemy) {
        if (isEnemy && FarmCollisionless.SKIP_ENEMY) return;
        if (FarmCollisionless.USE_WHITELIST && !FarmCollisionless.WHITELIST.contains(type)) return;
        Long2ObjectMap<Map<ResourceLocation, Set<UUID>>> entitiesInChunk = ENTITIES.get(dim);
        if (entitiesInChunk == null) return;

        Map<ResourceLocation, Set<UUID>> entitiesByType = entitiesInChunk.get(chunkPos);
        if (entitiesByType == null) return;

        Set<UUID> entities = entitiesByType.get(type);

        if (entities == null) return;
        entities.remove(uuid);

        if (entities.isEmpty()) {
            entitiesByType.remove(type);
            if (entitiesByType.isEmpty()) {
                entitiesInChunk.remove(chunkPos);
            }
            if (entitiesInChunk.isEmpty()) {
                ENTITIES.remove(dim);
            }
        }
    }

    public static void removeEntity(Entity entity, ServerLevel level) {
        if (entity instanceof Enemy && FarmCollisionless.SKIP_ENEMY) return;
        long chunkPos = entity.chunkPosition().toLong();
        ResourceLocation dim = level.dimension().location();
        ResourceLocation type = type(entity);

        if (FarmCollisionless.USE_WHITELIST && !FarmCollisionless.WHITELIST.contains(type)) return;
        removeEntity(dim, chunkPos, type, entity.getUUID(), false);
    }

    public static void addEntity(ResourceLocation dim, long chunkPos, ResourceLocation type, UUID uuid, boolean isEnemy) {
        if (isEnemy && FarmCollisionless.SKIP_ENEMY) return;
        if (FarmCollisionless.USE_WHITELIST && !FarmCollisionless.WHITELIST.contains(type)) return;
        ENTITIES.computeIfAbsent(dim, key -> new Long2ObjectOpenHashMap<>())
                .computeIfAbsent(chunkPos, key -> new Object2ObjectOpenHashMap<>())
                .computeIfAbsent(type, key -> new ObjectOpenHashSet<>())
                .add(uuid);
    }

    public static void addEntity(Entity entity, ServerLevel level) {
        if (entity instanceof Enemy && FarmCollisionless.SKIP_ENEMY) return;
        long chunkPos = entity.chunkPosition().toLong();
        ResourceLocation dim = level.dimension().location();
        ResourceLocation type = type(entity);
        if (FarmCollisionless.USE_WHITELIST && !FarmCollisionless.WHITELIST.contains(type)) return;
        addEntity(dim, chunkPos, type, entity.getUUID(), false);
    }

    @SubscribeEvent
    public static void onChunkUnLoad(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level && event.getChunk() instanceof LevelChunk chunk) {
            level.getServer().execute(() -> {
                Long2ObjectMap<Map<ResourceLocation, Set<UUID>>> entitiesInChunk = ENTITIES.get(level.dimension().location());
                if (entitiesInChunk == null) return;
                entitiesInChunk.remove(chunk.getPos().toLong());
            });
        }
    }

    @SubscribeEvent
    public static void onLevelUnLoad(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            level.getServer().execute(() -> ENTITIES.remove(level.dimension().location()));
        }
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.isCanceled()) return;
        if (event.getLevel() instanceof ServerLevel level) {
            level.getServer().execute(() -> addEntity(event.getEntity(), level));
        }
    }

    @SubscribeEvent
    public static void onLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            level.getServer().execute(() -> removeEntity(event.getEntity(), level));
        }
    }
}
