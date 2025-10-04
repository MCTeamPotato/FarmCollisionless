package me.kall.farmcollisionless.cmd;

import com.mojang.brigadier.CommandDispatcher;
import me.kall.farmcollisionless.FarmCollisionless;
import me.kall.farmcollisionless.data.CollisionlessData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

public class CollisionlessCommand {
    public static void register(@NotNull CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("collisionless")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("here").executes(ctx -> toggleHere(ctx.getSource())))
                .then(Commands.literal("dump").executes(ctx -> dumpAll(ctx.getSource())))
                .then(Commands.literal("clear").executes(ctx -> clearAll(ctx.getSource())))
                .then(Commands.literal("reload").executes(ctx -> reloadConfig(ctx.getSource()))));
    }

    private static int toggleHere(@NotNull CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) return 0;

        ServerLevel level = player.serverLevel();
        long chunk = player.chunkPosition().toLong();
        CollisionlessData data = CollisionlessData.get(level);
        boolean currently = data.isCollisionlessChunk(level.dimension().location(), chunk);

        if (currently) {
            data.remove(level.dimension().location(), chunk);
            player.displayClientMessage(Component.translatable("info.farmcollisionless.cancel"), false);
        } else {
            data.add(level.dimension().location(), chunk);
            player.displayClientMessage(Component.translatable("info.farmcollisionless"), false);
        }
        return 1;
    }

    private static int dumpAll(@NotNull CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) return 0;

        ServerLevel level = player.serverLevel();
        CollisionlessData data = CollisionlessData.get(level);

        var chunks = data.getChunks(level.dimension().location());
        if (chunks.isEmpty()) {
            player.displayClientMessage(Component.literal("No collisionless chunks in this dimension."), false);
            return 1;
        }

        player.displayClientMessage(Component.literal("Collisionless chunks:"), false);
        for (long chunkPos : chunks) {
            int chunkX = (int)(chunkPos & 0xFFFFFFFFL);
            int chunkZ = (int)(chunkPos >>> 32);
            int centerX = (chunkX << 4) + 8;
            int centerZ = (chunkZ << 4) + 8;
            player.displayClientMessage(Component.literal(" - (" + centerX + ", " + centerZ + ")"), false);
        }

        return 1;
    }

    private static int clearAll(@NotNull CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) return 0;

        ServerLevel level = player.serverLevel();
        CollisionlessData data = CollisionlessData.get(level);
        int size = data.getChunks(level.dimension().location()).size();

        data.clear(level.dimension().location());

        player.displayClientMessage(Component.literal("Cleared " + size + " collisionless chunks."), false);
        return 1;
    }

    private static int reloadConfig(@NotNull CommandSourceStack source) {
        FarmCollisionless.initConfig();
        source.sendSuccess(() -> Component.literal("Reloaded FarmCollisionless config."), true);
        return 1;
    }
}
