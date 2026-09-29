package com.districtx.housing;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;

public class MyHousesCommand implements CommandExecutor {
    private final HousingPlugin plugin;

    public MyHousesCommand(HousingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessages().send(sender, "player-only", Collections.emptyMap());
            return true;
        }
        plugin.openMyHouses((Player) sender);
        return true;
    }
}