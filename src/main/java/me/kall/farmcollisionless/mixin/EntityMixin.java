package me.kall.farmcollisionless.mixin;

import me.kall.farmcollisionless.data.CollisionlessData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collections;
import java.util.List;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Shadow
    public static Vec3 collideBoundingBox(@Nullable Entity entity, Vec3 vec, AABB collisionBox, Level level, List<VoxelShape> potentialHits) {
        throw new RuntimeException();
    }

    @Inject(method = "collide", at = @At("HEAD"), cancellable = true)
    private void onCollide(Vec3 vec, CallbackInfoReturnable<Vec3> cir) {
        Entity entity = (Entity) (Object) this;
        if (entity.level() instanceof ServerLevel level && CollisionlessData.get(level).isCollisionlessChunk(level.dimension().location(), entity.chunkPosition().toLong())) {
            cir.setReturnValue(vec.lengthSqr() == 0.0 ? vec : collideBoundingBox(entity, vec, entity.getBoundingBox(), level, Collections.emptyList()));
        }
    }
}
