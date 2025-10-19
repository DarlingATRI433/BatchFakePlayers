package com.xiaxin.batchfakeplayers.command;

import com.xiaxin.batchfakeplayers.manager.FakePlayerManager;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class ModCommands {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            FakePlayerManager.getInstance().registerCommands(dispatcher);
        });
    }
}