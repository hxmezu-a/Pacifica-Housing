package com.kodari.housing;

import com.kodari.housing.model.House;
import com.kodari.housing.model.HouseType;
import com.kodari.housing.util.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HousesAdminCommand implements CommandExecutor, TabCompleter {
    private final HousingPlugin plugin;
    private final MessageService messages;

    public HousesAdminCommand(HousingPlugin plugin) {
        this.plugin = plugin;
        this.messages = plugin.getMessages();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!requireAdmin(sender)) return true;
        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }
        if (!(sender instanceof Player)) {
            messages.send(sender, "player-only", Collections.emptyMap());
            return true;
        }
        Player player = (Player) sender;
        String subcommand = args[0].toLowerCase();
        switch (subcommand) {
            case "create":
                create(player, label, args);
                break;
            case "adddoor":
                addDoor(player, label, args);
                break;
            case "setoutsidehouse":
            case "setoutside":
                setLocation(player, label, args, false);
                break;
            case "setinsidehouse":
            case "setinside":
                setLocation(player, label, args, true);
                break;
            case "grant":
                grant(player, label, args);
                break;
            case "addvault":
                addVault(player, label, args);
                break;
            case "reload":
                plugin.reloadConfiguration();
                messages.send(sender, "reload", new HashMap<>());
                break;
            default:
                sendUsage(sender, label);
        }
        return true;
    }

    private void create(Player player, String label, String[] args) {
        if (args.length != 4) {
            sendUsage(player, label + " create <name> <type> <price>");
            return;
        }
        Block block = targetedDoor(player);
        if (!HouseManager.isIronDoor(block)) {
            messages.send(player, "house-created-not-door", new HashMap<>());
            return;
        }
        HouseType type = HouseType.from(args[2]);
        if (type == null) {
            messages.send(player, "invalid-type", new HashMap<>());
            return;
        }
        double price;
        try {
            price = Double.parseDouble(args[3]);
        } catch (NumberFormatException exception) {
            price = -1;
        }
        if (price <= 0) {
            messages.send(player, "invalid-price", new HashMap<>());
            return;
        }
        House house = plugin.getHouseManager().create(args[1], type, price, block.getLocation());
        if (house == null) {
            messages.send(player, "house-already-exists", new HashMap<>());
            return;
        }
        messages.send(player, "house-created", values("house", house.getName()));
    }

    private void addDoor(Player player, String label, String[] args) {
        if (args.length != 2) {
            sendUsage(player, label + " adddoor <name>");
            return;
        }
        House house = plugin.getHouseManager().get(args[1]);
        Block block = targetedDoor(player);
        if (house == null) {
            messages.send(player, "unknown-house", new HashMap<>());
        } else if (!HouseManager.isIronDoor(block)) {
            messages.send(player, "house-created-not-door", new HashMap<>());
        } else {
            com.kodari.housing.model.HouseDoor addedDoor = plugin.getHouseManager().addDoor(house, block.getLocation());
            if (addedDoor == null) {
                messages.send(player, "door-already-registered", new HashMap<>());
            } else {
                messages.send(player, "door-added", values("house", house.getName(), "door", String.valueOf(addedDoor.getNumber())));
            }
        }
    }

    private void setLocation(Player player, String label, String[] args, boolean inside) {
        if (args.length != 3) {
            sendUsage(player, label + (inside ? " setinside <name> <door number>" : " setoutside <name> <door number>"));
            return;
        }
        House house = plugin.getHouseManager().get(args[1]);
        if (house == null) {
            messages.send(player, "unknown-house", new HashMap<>());
            return;
        }
        int number;
        try {
            number = Integer.parseInt(args[2]);
        } catch (NumberFormatException exception) {
            messages.send(player, "invalid-door-number", new HashMap<>());
            return;
        }
        com.kodari.housing.model.HouseDoor door = plugin.getHouseManager().getDoor(house, number);
        if (number < 1 || door == null) {
            messages.send(player, "invalid-door-number", new HashMap<>());
            return;
        }
        plugin.setDoorLocation(house, door, player.getLocation(), inside);
        messages.send(player, inside ? "inside-set" : "outside-set", values("house", house.getName(), "door", String.valueOf(number)));
    }

    private void grant(Player player, String label, String[] args) {
        if (args.length != 3) {
            sendUsage(player, label + " grant <player> <house>");
            return;
        }
        House house = plugin.getHouseManager().get(args[2]);
        if (house == null) {
            messages.send(player, "unknown-house", new HashMap<>());
            return;
        }
        if (house.getType() != HouseType.LUXURY) {
            messages.send(player, "invalid-type", new HashMap<>());
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            messages.send(player, "unknown-house", new HashMap<>());
            return;
        }
        int limit = plugin.getHouseLimit(target);
        if (limit >= 0 && plugin.getHouseManager().countOwned(target.getUniqueId()) >= limit) {
            messages.send(player, "limit-reached", new HashMap<>());
            return;
        }
        house.setOwner(target.getUniqueId());
        plugin.getHouseManager().save();
        messages.send(player, "luxury-granted", values("house", house.getName(), "player", target.getName()));
    }

    private void addVault(Player player, String label, String[] args) {
        if (args.length != 2) {
            sendUsage(player, label + " addvault <house name>");
            return;
        }
        House house = plugin.getHouseManager().get(args[1]);
        if (house == null) {
            messages.send(player, "unknown-house", new HashMap<>());
            return;
        }
        if (house.getType() == HouseType.REGULAR) {
            messages.send(player, "invalid-type", new HashMap<>());
            return;
        }
        Block block = player.getTargetBlock(null, 5);
        if (!HouseManager.isVaultContainer(block)) {
            messages.send(player, "vault-not-container", new HashMap<>());
            return;
        }
        if (plugin.getHouseManager().findByVault(block) != null) {
            messages.send(player, "vault-already-registered", new HashMap<>());
            return;
        }
        plugin.getHouseManager().addVault(house, block.getLocation());
        messages.send(player, "vault-added", values("house", house.getName(), "vaults", String.valueOf(house.getVaults().size())));
    }

    private Block targetedDoor(Player player) {
        return player.getTargetBlock(null, 5);
    }

    private boolean requireAdmin(CommandSender sender) {
        if (!sender.hasPermission("kodarihousing.admin")) {
            messages.send(sender, "no-permission", new HashMap<>());
            return false;
        }
        return true;
    }

    private void sendUsage(CommandSender sender, String usage) {
        messages.send(sender, "usage", values("usage", usage));
    }

    private Map<String, String> values(String... values) {
        Map<String, String> map = new HashMap<>();
        for (int index = 0; index + 1 < values.length; index += 2) map.put(values[index], values[index + 1]);
        return map;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return filter(Arrays.asList("create", "adddoor", "addvault", "setoutside", "setinside", "grant", "reload"), args[0]);
        if (args.length == 2 && (args[0].equalsIgnoreCase("setoutside") || args[0].equalsIgnoreCase("setinside")
                || args[0].equalsIgnoreCase("setoutsidehouse") || args[0].equalsIgnoreCase("setinsidehouse")
                || args[0].equalsIgnoreCase("adddoor") || args[0].equalsIgnoreCase("addvault"))) {
            List<String> names = new ArrayList<>();
            for (House house : plugin.getHouseManager().all()) names.add(house.getName());
            return filter(names, args[1]);
        }
        if (args.length == 3 && (args[0].equalsIgnoreCase("setoutside") || args[0].equalsIgnoreCase("setinside")
                || args[0].equalsIgnoreCase("setoutsidehouse") || args[0].equalsIgnoreCase("setinsidehouse"))) {
            House house = plugin.getHouseManager().get(args[1]);
            if (house == null) return Collections.emptyList();
            List<String> numbers = new ArrayList<>();
            for (com.kodari.housing.model.HouseDoor door : house.getDoors()) numbers.add(String.valueOf(door.getNumber()));
            return filter(numbers, args[2]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> values, String input) {
        List<String> result = new ArrayList<>();
        for (String value : values) if (value.toLowerCase().startsWith(input.toLowerCase())) result.add(value);
        return result;
    }
}