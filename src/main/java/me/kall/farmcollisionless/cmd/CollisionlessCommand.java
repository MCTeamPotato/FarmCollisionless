package me.kall.farmcollisionless.cmd;

import com.mojang.brigadier.CommandDispatcher;
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
                .then(Commands.literal("here").executes(ctx -> toggleHere(ctx.getSource()))));
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
}