package me.kall.farmcollisionless.mixin;

import me.kall.farmcollisionless.FarmCollisionless;
import me.kall.farmcollisionless.data.CollisionlessData;
import me.kall.farmcollisionless.data.EntityTracker;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @Inject(method = "tickNonPassenger", at = @At("HEAD"))
    private void onTickEntity(Entity entity, CallbackInfo ci) {
        if (FarmCollisionless.AUTO) {
            ServerLevel level = (ServerLevel) (Object) this;
            if (level.getServer().getTickCount() % FarmCollisionless.INTERVAL == 0) {
                if (entity instanceof Enemy && FarmCollisionless.SKIP_ENEMY) return;
                ResourceLocation type = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
                if (FarmCollisionless.USE_WHITELIST && !FarmCollisionless.WHITELIST.contains(type)) return;
                long chunkPos = entity.chunkPosition().toLong();
                ResourceLocation dim = level.dimension().location();
                level.getServer().execute(() -> {
                    int size = EntityTracker.getEntities(level, chunkPos, type).size();
                    if (size >= FarmCollisionless.GAP) {
                        CollisionlessData.get(level).add(dim, chunkPos);
                    } else if (CollisionlessData.get(level).isCollisionlessChunk(dim, chunkPos)) {
                        CollisionlessData.get(level).remove(dim, chunkPos);
                    }
                });
            }
        }
    }
}
