package me.kall.farmcollisionless.mixin;

import me.kall.farmcollisionless.FarmCollisionless;
import me.kall.farmcollisionless.data.CollisionlessData;
import me.kall.farmcollisionless.data.EntityTracker;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Shadow
    public static Vec3 collideBoundingBox(@Nullable Entity entity, Vec3 vec, AABB collisionBox, Level level, List<VoxelShape> potentialHits) {
        throw new RuntimeException();
    }

    @Shadow private Level level;

    @Shadow public abstract void setUUID(UUID uniqueId);

    @Inject(method = "collide", at = @At("HEAD"), cancellable = true)
    private void onCollide(Vec3 vec, CallbackInfoReturnable<Vec3> cir) {
        Entity entity = (Entity) (Object) this;
        if (entity.level() instanceof ServerLevel serverLevel && CollisionlessData.get(serverLevel).isCollisionlessChunk(serverLevel.dimension().location(), entity.chunkPosition().toLong())) {
            cir.setReturnValue(vec.lengthSqr() == 0.0 ? vec : collideBoundingBox(entity, vec, entity.getBoundingBox(), serverLevel, Collections.emptyList()));
        }
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setPos(DDD)V"), require = 0)
    private void init(@NotNull Entity instance, double x, double y, double z) {
        if (this.level instanceof ServerLevel serverLevel) {
            UUID id = FarmCollisionless.randomUUID();
            while (serverLevel.getEntity(id) != null) id = FarmCollisionless.randomUUID();
            this.setUUID(id);
        }
        instance.setPos(x, y, z);
    }

    @Inject(method = "setPosRaw", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ChunkPos;<init>(Lnet/minecraft/core/BlockPos;)V"))
    private void chunkPosUpdatePre(double x, double y, double z, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self.level() instanceof ServerLevel serverLevel) {
            ResourceLocation dim = serverLevel.dimension().location();
            long chunkPos = self.chunkPosition().toLong();
            ResourceLocation type = EntityTracker.type(self);
            UUID uuid = self.getUUID();
            serverLevel.getServer().execute(() -> EntityTracker.removeEntity(dim, chunkPos, type, uuid, self instanceof Enemy));
        }
    }

    @Inject(method = "setPosRaw", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ChunkPos;<init>(Lnet/minecraft/core/BlockPos;)V", shift = At.Shift.AFTER))
    private void chunkPosUpdatePost(double x, double y, double z, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self.level() instanceof ServerLevel serverLevel) {
            ResourceLocation dim = serverLevel.dimension().location();
            long chunkPos = self.chunkPosition().toLong();
            ResourceLocation type = EntityTracker.type(self);
            UUID uuid = self.getUUID();
            serverLevel.getServer().execute(() -> EntityTracker.addEntity(dim, chunkPos, type, uuid, self instanceof Enemy));
        }
    }
}
