package com.districtx.housing.inventory.gui;

import com.cryptomorin.xseries.XMaterial;
import com.districtx.housing.HousingPlugin;
import com.districtx.housing.inventory.InventoryButton;
import com.districtx.housing.inventory.InventoryGUI;
import com.districtx.housing.model.House;
import com.districtx.housing.model.HouseDoor;
import com.districtx.housing.model.HouseType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;
import java.util.Collections;

public class OwnerGUI extends InventoryGUI {
    private final HousingPlugin plugin;
    private final House house;

    public OwnerGUI(HousingPlugin plugin, House house) {
        this.plugin = plugin;
        this.house = house;

        addButton(11, new InventoryButton()
                .creator(player -> createCostItem())
                .consumer(event -> { }));
        addButton(13, new InventoryButton()
                .creator(player -> createOwnerItem())
                .consumer(event -> { }));
        addButton(15, new InventoryButton()
                .creator(player -> createVaultItem())
                .consumer(event -> {
                    Player player = (Player) event.getWhoClicked();
                    if (!isOwner(player)) {
                        plugin.getMessages().send(player, "not-owner", Collections.emptyMap());
                        return;
                    }
                    plugin.openVaults(player, house);
                }));
        addButton(29, new InventoryButton()
                .creator(this::createTeleportItem)
                .consumer(event -> {
                    Player player = (Player) event.getWhoClicked();
                    if (!isOwner(player)) {
                        plugin.getMessages().send(player, "not-owner", Collections.emptyMap());
                        return;
                    }
                    if (isTooCloseToHouseDoor(player)) {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                "&cYou are too close to the house door to teleport"));
                        return;
                    }
                    HouseDoor door = house.getDoors().isEmpty() ? null : house.getDoors().get(0);
                    plugin.beginOwnedHouseTeleport(player, house, door);
                }));
        addButton(31, new InventoryButton()
                .creator(player -> createItem("SLIME_BALL", "&a&lSell House", "&7Click to sell"))
                .consumer(event -> {
                    Player player = (Player) event.getWhoClicked();
                    if (!isOwner(player)) {
                        plugin.getMessages().send(player, "not-owner", Collections.emptyMap());
                        return;
                    }
                    if (house.getType() == HouseType.LUXURY) {
                        plugin.getMessages().send(player, "cannot-sell-luxury", Collections.emptyMap());
                        return;
                    }
                    plugin.openSellConfirmation(player, house);
                }));
        addButton(33, new InventoryButton()
                .creator(player -> createItem("BOOK", "&6&lHouse Info",
                        "&7House ID: &6" + house.getName(),
                        "&7House type: &6" + displayType(house.getType())))
                .consumer(event -> {
                    Player player = (Player) event.getWhoClicked();
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6&lHouse Info"));
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            "&7House ID: &6" + house.getName()));
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            "&7House type: &6" + displayType(house.getType())));
                }));
        addButton(49, new InventoryButton()
                .creator(player -> createItem(doorMaterial(), "&9&lHouse " + house.getName()))
                .consumer(event -> { }));
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, ChatColor.translateAlternateColorCodes('&',
                plugin.getGuiConfig().getString("owner.title", "&8Manage: &f{house}")
                        .replace("{house}", house.getName())));
    }

    private ItemStack createCostItem() {
        String cost = plugin.formatAmount(house.getPrice());
        String line = house.getType() == HouseType.REGULAR ? "&7Cost: &a$" + cost
                : house.getType() == HouseType.PREMIUM ? "&7Cost: &b&l" + cost
                : "&7Cost: &6&l$" + cost;
        return createItem("PAPER", "&7&lHouse Cost", line);
    }

    private ItemStack createOwnerItem() {
        ItemStack item = XMaterial.matchXMaterial("PLAYER_HEAD").map(XMaterial::parseItem).orElse(null);
        if (item == null) {
            return null;
        }
        ItemMeta itemMeta = item.getItemMeta();
        if (!(itemMeta instanceof SkullMeta)) {
            return item;
        }
        OfflinePlayer owner = house.getOwner() == null ? null : Bukkit.getOfflinePlayer(house.getOwner());
        SkullMeta meta = (SkullMeta) itemMeta;
        if (owner != null) {
            meta.setOwningPlayer(owner);
        }
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                "&7&lOwner: &a&l" + ownerName()));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createVaultItem() {
        return createItem("BARREL", "&7House Vaults: &6" + house.getVaults().size());
    }

    private ItemStack createTeleportItem(Player player) {
        if (isTooCloseToHouseDoor(player)) {
            return createItem("CHEST_MINECART", "&6&lTeleport to House",
                    "&cYou are too close to the house door to teleport");
        }
        return createItem("CHEST_MINECART", "&6&lTeleport to House", "&7Click to teleport");
    }

    private ItemStack createItem(String material, String name, String... lore) {
        ItemStack item = XMaterial.matchXMaterial(material).map(XMaterial::parseItem).orElse(null);
        if (item == null) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        meta.setLore(Arrays.stream(lore)
                .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                .collect(java.util.stream.Collectors.toList()));
        item.setItemMeta(meta);
        return item;
    }

    private boolean isOwner(Player player) {
        return house.getOwner() != null && house.getOwner().equals(player.getUniqueId());
    }

    private boolean isTooCloseToHouseDoor(Player player) {
        for (HouseDoor door : house.getDoors()) {
            if (door.getDoor() == null || door.getDoor().getWorld() == null
                    || !door.getDoor().getWorld().equals(player.getWorld())) {
                continue;
            }
            if (player.getLocation().distanceSquared(door.getDoor()) <= 9.0) {
                return true;
            }
        }
        return false;
    }

    private String doorMaterial() {
        return house.getType() == HouseType.REGULAR ? "OAK_DOOR"
                : house.getType() == HouseType.PREMIUM ? "IRON_DOOR" : "CRIMSON_DOOR";
    }

    private String ownerName() {
        if (house.getOwner() == null) {
            return "Unknown";
        }
        String name = Bukkit.getOfflinePlayer(house.getOwner()).getName();
        return name == null ? "Unknown" : name;
    }

    private String displayType(HouseType type) {
        String value = type == null ? "Unknown" : type.name().toLowerCase();
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}