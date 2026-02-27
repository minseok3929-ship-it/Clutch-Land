package com.clutch.land.ui;

import org.bukkit.command.CommandSender;

public final class MessageFacade {
    public void sendInfo(CommandSender sender, String message) {
        sender.sendMessage("§a" + message);
    }

    public void sendError(CommandSender sender, String message) {
        sender.sendMessage("§c" + message);
    }
}
