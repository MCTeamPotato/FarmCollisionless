package me.kall.farmcollisionless;

import me.kall.farmcollisionless.cmd.CollisionlessCommand;
import me.kall.farmcollisionless.data.CollisionlessData;
import me.kall.jsonate.api.JsonConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

@Mod(FarmCollisionless.MOD_ID)
public final class FarmCollisionless {
    public static final String MOD_ID = "farmcollisionless";

    public static final JsonConfig CONFIG = JsonConfig.create(MOD_ID, "1.0.0")
            .put("CollisionlessProvider", "net.minecraft.world.level.block.FenceBlock")
            .initialize();

    public static final Class<?> PROVIDER;

    static {
        try {
            PROVIDER = Class.forName(CONFIG.getString("CollisionlessProvider"));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public FarmCollisionless() {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::placeProvider);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::removeProvider);
        MinecraftForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> CollisionlessCommand.register(event.getDispatcher()));
    }

    public void placeProvider(BlockEvent.@NotNull EntityPlaceEvent event) {
        if (event.isCanceled()) return;
        if (event.getEntity() instanceof ServerPlayer player && player.level() instanceof ServerLevel serverLevel && player.isCrouching() && PROVIDER.isInstance(event.getPlacedBlock().getBlock())) {
            CollisionlessData.get(serverLevel).add(serverLevel.dimension().location(), ChunkPos.asLong(event.getPos()));
            player.displayClientMessage(Component.translatable("info." + MOD_ID), false);
        }
    }

    public void removeProvider(BlockEvent.@NotNull BreakEvent event) {
        if (event.isCanceled()) return;
        Player player = event.getPlayer();
        Level level = player.level();
        if (PROVIDER.isInstance(event.getState().getBlock()) && level instanceof ServerLevel serverLevel && player.isCrouching()) {
            CollisionlessData.get(serverLevel).remove(serverLevel.dimension().location(), ChunkPos.asLong(event.getPos()));
            player.displayClientMessage(Component.translatable("info." + MOD_ID + ".cancel"), false);
        }
    }
}
