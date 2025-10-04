package me.kall.farmcollisionless;

import com.google.common.collect.Lists;
import me.kall.farmcollisionless.cmd.CollisionlessCommand;
import me.kall.farmcollisionless.data.CollisionlessData;
import me.kall.jsonate.api.JsonConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.stream.Collectors;

@Mod(FarmCollisionless.MOD_ID)
public final class FarmCollisionless {
    public static final String MOD_ID = "farmcollisionless";

    public static JsonConfig CONFIG;

    public static Class<?> PROVIDER;
    public static boolean AUTO;
    public static int GAP;
    public static boolean SKIP_ENEMY;
    public static Set<ResourceLocation> WHITELIST;
    public static boolean USE_WHITELIST;
    public static int INTERVAL;

    static {
        initConfig();
    }

    public static void initConfig() {
        CONFIG = JsonConfig.create(MOD_ID, "2")
                .put("CollisionlessProvider", "net.minecraft.world.level.block.FenceBlock")
                .put("AutoDetectFarm", true)
                .put("SkipEnemyMonsterFarm", true)
                .put("TheNumberOfEntitiesInAChunkThatValidatesDetection", 10)
                .put("DetectionIntervalTicks", 20)
                .put("FarmEntitiesRegistryNameWhitelist", Lists.newArrayList())
                .initialize();
        try {
            PROVIDER = Class.forName(CONFIG.getString("CollisionlessProvider"));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        AUTO = CONFIG.getBoolean("AutoDetectFarm");
        GAP = CONFIG.getInt("TheNumberOfEntitiesInAChunkThatValidatesDetection");
        SKIP_ENEMY = CONFIG.getBoolean("SkipEnemyMonsterFarm");
        WHITELIST = CONFIG.getStream("FarmEntitiesRegistryNameWhitelist", String.class).map(ResourceLocation::parse).collect(Collectors.toSet());
        USE_WHITELIST = !WHITELIST.isEmpty();
        INTERVAL = CONFIG.getInt("DetectionIntervalTicks");
    }

    public FarmCollisionless(IEventBus modBus, Dist dist, ModContainer container) {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::placeProvider);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::removeProvider);
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> CollisionlessCommand.register(event.getDispatcher()));
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
