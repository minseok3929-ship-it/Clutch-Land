package com.clutch.land.ui;

import org.bukkit.command.CommandSender;

public final class MessageFacade {
    private static final String PREFIX = "§6[CLUTCH] §r";

    public void info(CommandSender sender, String message) {
        sender.sendMessage(PREFIX + "§a" + message);
    }

    public void warn(CommandSender sender, String message) {
        sender.sendMessage(PREFIX + "§e" + message);
    }

    public void error(CommandSender sender, String message) {
        sender.sendMessage(PREFIX + "§c" + message);
    }
}
