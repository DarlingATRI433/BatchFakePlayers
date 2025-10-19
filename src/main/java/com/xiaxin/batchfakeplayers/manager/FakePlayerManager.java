package com.xiaxin.batchfakeplayers.manager;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.xiaxin.batchfakeplayers.BatchFakeplayers;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class FakePlayerManager {
    private static FakePlayerManager instance;
    private final AtomicBoolean isSpawning = new AtomicBoolean(false);
    private CompletableFuture<Void> currentTask = null;

    private FakePlayerManager() {}

    public static FakePlayerManager getInstance() {
        if (instance == null) {
            instance = new FakePlayerManager();
        }
        return instance;
    }

    public void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("fakeplayer")
                .then(CommandManager.literal("spawn")
                        .then(CommandManager.argument("count", IntegerArgumentType.integer(1, 100))
                                .then(CommandManager.literal("at")
                                        .then(CommandManager.argument("pos", Vec3ArgumentType.vec3())
                                                .executes(this::spawnFakePlayersWithCoords)))
                                .executes(this::spawnFakePlayers)))
                .then(CommandManager.literal("stop")
                        .executes(this::stopSpawning)));
    }

    private int spawnFakePlayers(CommandContext<ServerCommandSource> context) {
        return spawnFakePlayersWithCoords(context, null);
    }

    private int spawnFakePlayersWithCoords(CommandContext<ServerCommandSource> context) {
        try {
            Vec3d pos = Vec3ArgumentType.getVec3(context, "pos");
            return spawnFakePlayersWithCoords(context, pos);
        } catch (Exception e) {
            return spawnFakePlayersWithCoords(context, null);
        }
    }

    private int spawnFakePlayersWithCoords(CommandContext<ServerCommandSource> context, Vec3d pos) {
        if (isSpawning.get()) {
            context.getSource().sendError(Text.translatable("command.batchfakeplayers.spawning.in_progress"));
            return 0;
        }

        int count = IntegerArgumentType.getInteger(context, "count");
        ServerCommandSource source = context.getSource();
        
        isSpawning.set(true);
        
        String coordsText = pos != null ? String.format(" at (%.1f, %.1f, %.1f)", pos.x, pos.y, pos.z) : "";
        source.sendFeedback(() -> Text.translatable("command.batchfakeplayers.spawning.start.coords", count, coordsText), true);

        currentTask = CompletableFuture.runAsync(() -> {
            try {
                for (int i = 1; i <= count && isSpawning.get(); i++) {
                    String playerName = "fakeplayer" + i;
                    
                    // 构建命令
                    String command;
                    if (pos != null) {
                        command = String.format("/player %s spawn at %.1f %.1f %.1f", 
                            playerName, pos.x, pos.y, pos.z);
                    } else {
                        command = "/player " + playerName + " spawn";
                    }
                    
                    // 执行/player <玩家名字> spawn命令
                    source.getServer().getCommandManager().executeWithPrefix(source, command);
                    
                    // 增加延迟到1秒
                     Thread.sleep(1000);
                }
                
                if (isSpawning.get()) {
                    source.sendFeedback(() -> Text.translatable("command.batchfakeplayers.spawning.complete.coords", count, coordsText), true);
                } else {
                    source.sendFeedback(() -> Text.translatable("command.batchfakeplayers.spawning.stopped"), true);
                }
            } catch (Exception e) {
                BatchFakeplayers.LOGGER.error("生成假玩家时发生错误", e);
                source.sendError(Text.translatable("command.batchfakeplayers.spawning.error"));
            } finally {
                isSpawning.set(false);
            }
        });

        return 1;
    }

    private int stopSpawning(CommandContext<ServerCommandSource> context) {
        if (!isSpawning.get()) {
            context.getSource().sendError(Text.translatable("command.batchfakeplayers.spawning.not_running"));
            return 0;
        }

        isSpawning.set(false);
        if (currentTask != null) {
            currentTask.cancel(true);
        }
        
        context.getSource().sendFeedback(() -> Text.translatable("command.batchfakeplayers.spawning.stopped"), true);
        return 1;
    }

    public boolean isSpawning() {
        return isSpawning.get();
    }
}