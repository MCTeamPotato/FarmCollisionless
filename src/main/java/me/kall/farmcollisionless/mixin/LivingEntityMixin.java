package me.kall.farmcollisionless.mixin;

import me.kall.farmcollisionless.data.CollisionlessData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    public LivingEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "pushEntities", at = @At("HEAD"), cancellable = true)
    private void onPush(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity instanceof Player) return;
        if (entity.level() instanceof ServerLevel serverLevel && CollisionlessData.get(serverLevel).isCollisionlessChunk(serverLevel.dimension().location(), entity.chunkPosition().toLong())) {
            ci.cancel();
        }
    }
}
