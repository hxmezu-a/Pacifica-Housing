package com.kodari.housing.inventory.gui;

import com.cryptomorin.xseries.XMaterial;
import com.kodari.housing.HousingPlugin;
import com.kodari.housing.inventory.InventoryButton;
import com.kodari.housing.inventory.InventoryGUI;
import com.kodari.housing.model.House;
import com.kodari.housing.model.HouseDoor;
import com.kodari.housing.model.HouseType;
import com.kodari.housing.util.GuiItems;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.HashMap;
import java.util.Map;

public class OwnedHouseDetailsGUI extends InventoryGUI {
    private final HousingPlugin plugin;
    private final House house;
    private final java.util.UUID viewerId;

    public OwnedHouseDetailsGUI(HousingPlugin plugin, House house, Player viewer) {
        this.plugin = plugin;
        this.house = house;
        this.viewerId = viewer.getUniqueId();

        ConfigurationSection info = plugin.getGuiConfig().getConfigurationSection("owned-details.info");
        for (int slot : plugin.getGuiConfig().getIntegerList("owned-details.info.slots")) {
            addButton(slot, new InventoryButton().creator(player -> GuiItems.create(info, values()))
                    .consumer(event -> plugin.openVaults((Player) event.getWhoClicked(), house)));
        }

        ConfigurationSection owner = plugin.getGuiConfig().getConfigurationSection("owned-details.owner");
        for (int slot : plugin.getGuiConfig().getIntegerList("owned-details.owner.slots")) {
            addButton(slot, new InventoryButton().creator(player -> createOwnerItem(owner, values()))
                    .consumer(event -> { }));
        }

        ConfigurationSection teleport = plugin.getGuiConfig().getConfigurationSection("owned-details.teleport");
        if (isOwner(viewerId)) {
            for (int slot : plugin.getGuiConfig().getIntegerList("owned-details.teleport.slots")) {
                addButton(slot, new InventoryButton().creator(player -> GuiItems.create(teleport, values()))
                        .consumer(event -> {
                            Player player = (Player) event.getWhoClicked();
                            if (!isOwner(player.getUniqueId())) {
                                plugin.getMessages().send(player, "not-owner", new HashMap<>());
                                return;
                            }
                            HouseDoor door = house.getDoors().isEmpty() ? null : house.getDoors().get(0);
                            if (door == null) {
                                plugin.getMessages().send(player, "house-not-ready", new HashMap<>());
                                return;
                            }
                            player.closeInventory();
                            plugin.beginOwnedHouseTeleport(player, house, door);
                        }));
            }
        }

        ConfigurationSection bidding = plugin.getGuiConfig().getConfigurationSection("owned-details.bidding");
        if (bidding != null && house.getType() == HouseType.PREMIUM && isOwner(viewerId)) {
            for (int slot : bidding.getIntegerList("slots")) {
                addButton(slot, new InventoryButton().creator(player -> GuiItems.create(bidding, values()))
                        .consumer(event -> {
                            Player player = (Player) event.getWhoClicked();
                            if (isOwner(player.getUniqueId())) {
                                plugin.openAuctionSetup(player, house);
                            } else {
                                plugin.getMessages().send(player, "not-owner", new HashMap<>());
                            }
                        }));
            }
        }

        ConfigurationSection close = plugin.getGuiConfig().getConfigurationSection("owned-details.close");
        for (int slot : plugin.getGuiConfig().getIntegerList("owned-details.close.slots")) {
            addButton(slot, new InventoryButton().creator(player -> GuiItems.create(close, values()))
                    .consumer(event -> event.getWhoClicked().closeInventory()));
        }
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, plugin.getGuiConfig().getInt("owned-details.size", 36),
                ChatColor.translateAlternateColorCodes('&',
                        plugin.getGuiConfig().getString("owned-details.title", "&8House: &f{house}")
                                .replace("{house}", house.getName())));
    }

    private ItemStack createOwnerItem(ConfigurationSection section, Map<String, String> replacements) {
        ItemStack item = GuiItems.create(section, replacements);
        if (item == null || house.getOwner() == null || !(item.getItemMeta() instanceof SkullMeta)) {
            return item;
        }
        OfflinePlayer owner = Bukkit.getOfflinePlayer(house.getOwner());
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(owner);
        item.setItemMeta(meta);
        return item;
    }

    private Map<String, String> values() {
        Map<String, String> values = new HashMap<>();
        values.put("house", house.getName());
        values.put("type", house.getType().name().toLowerCase());
        values.put("price", plugin.formatAmount(house.getPrice()));
        OfflinePlayer owner = house.getOwner() == null ? null : Bukkit.getOfflinePlayer(house.getOwner());
        values.put("owner", owner == null || owner.getName() == null ? "Unknown" : owner.getName());
        values.put("vaults", String.valueOf(house.getVaults().size()));
        return values;
    }

    private boolean isOwner(java.util.UUID playerId) {
        return house.getOwner() != null && house.getOwner().equals(playerId);
    }
}