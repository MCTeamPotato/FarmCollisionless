package me.kall.farmcollisionless.data;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class CollisionlessData extends SavedData {
    private static final String NAME = "farmcollisionless";

    private final Map<ResourceLocation, LongSet> collisionlessChunks = new Object2ObjectOpenHashMap<>();

    public static @NotNull CollisionlessData get(@NotNull ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(CollisionlessData::load, CollisionlessData::new, NAME);
    }

    public static @NotNull CollisionlessData load(@NotNull CompoundTag tag) {
        CollisionlessData data = new CollisionlessData();

        for (String dimKey : tag.getAllKeys()) {
            ResourceLocation dim = ResourceLocation.parse(dimKey);
            ListTag chunksTag = tag.getList(dimKey, CompoundTag.TAG_LONG);
            LongSet chunks = new LongOpenHashSet();
            for (Tag value : chunksTag) {
                if (value instanceof LongTag) {
                    chunks.add(((LongTag) value).getAsLong());
                }
            }
            data.collisionlessChunks.put(dim, chunks);
        }

        return data;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag) {
        for (Map.Entry<ResourceLocation, LongSet> entry : collisionlessChunks.entrySet()) {
            ListTag list = new ListTag();
            for (long chunk : entry.getValue()) {
                list.add(LongTag.valueOf(chunk));
            }
            tag.put(entry.getKey().toString(), list);
        }
        return tag;
    }

    public boolean isCollisionlessChunk(ResourceLocation dim, long chunkPos) {
        return collisionlessChunks.getOrDefault(dim, LongSets.emptySet()).contains(chunkPos);
    }

    public LongSet getChunks(ResourceLocation dim) {
        return collisionlessChunks.getOrDefault(dim, LongSets.emptySet());
    }

    public void add(ResourceLocation dim, long chunkPos) {
        collisionlessChunks.computeIfAbsent(dim, k -> new LongOpenHashSet()).add(chunkPos);
        setDirty();
    }

    public void remove(ResourceLocation dim, long chunkPos) {
        if (collisionlessChunks.containsKey(dim)) {
            LongSet set = collisionlessChunks.get(dim);
            set.remove(chunkPos);
            if (set.isEmpty()) collisionlessChunks.remove(dim);
            setDirty();
        }
    }

    public void clear(ResourceLocation dim) {
        collisionlessChunks.remove(dim);
        setDirty();
    }
}
