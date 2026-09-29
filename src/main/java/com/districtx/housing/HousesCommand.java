package com.districtx.housing;

import com.districtx.housing.util.MessageService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HousesCommand implements CommandExecutor, TabCompleter {
    private final HousingPlugin plugin;
    private final MessageService messages;

    public HousesCommand(HousingPlugin plugin) {
        this.plugin = plugin;
        this.messages = plugin.getMessages();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("owned")) {
            if (!(sender instanceof Player)) {
                messages.send(sender, "player-only", Collections.emptyMap());
                return true;
            }
            plugin.openOwnedHouses((Player) sender);
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("realestate")) {
            if (!(sender instanceof Player)) {
                messages.send(sender, "player-only", Collections.emptyMap());
                return true;
            }
            plugin.openRealEstateAgent((Player) sender);
            return true;
        }
        sendUsage(sender, label);
        return true;
    }

    private void sendUsage(CommandSender sender, String usage) {
        messages.send(sender, "usage", values("usage", usage));
    }

    private Map<String, String> values(String... values) {
        Map<String, String> map = new HashMap<>();
        for (int index = 0; index + 1 < values.length; index += 2) {
            map.put(values[index], values[index + 1]);
        }
        return map;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("owned", "realestate"), args[0]);
        }
        return Collections.emptyList();
    }
    private List<String> filter(List<String> values, String input) {
        List<String> result = new ArrayList<>();
        for (String value : values) if (value.toLowerCase().startsWith(input.toLowerCase())) result.add(value);
        return result;
    }
}