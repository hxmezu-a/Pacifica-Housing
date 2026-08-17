package com.kodari.housing.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Map;

public class MessageService {
    private final FileConfiguration config;

    public MessageService(FileConfiguration config) {
        this.config = config;
    }

    public String get(String path, Map<String, String> replacements) {
        String message = config.getString(path, path);
        String prefix = config.getString("prefix", "");
        message = message.replace("{prefix}", prefix);
        for (Map.Entry<String, String> replacement : replacements.entrySet()) {
            message = message.replace("{" + replacement.getKey() + "}", replacement.getValue());
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public void send(CommandSender sender, String path, Map<String, String> replacements) {
        sender.sendMessage(get(path, replacements));
    }
}