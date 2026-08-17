package com.kodari.housing.inventory.gui;

import com.kodari.housing.HousingPlugin;
import com.kodari.housing.inventory.InventoryButton;
import com.kodari.housing.inventory.InventoryGUI;
import com.kodari.housing.model.House;
import com.kodari.housing.model.HouseDoor;
import com.kodari.housing.util.GuiItems;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;

public class AvailableHouseDetailsGUI extends InventoryGUI {
    private final HousingPlugin plugin;
    private final House house;
    private final Map<String, String> values;

    public AvailableHouseDetailsGUI(HousingPlugin plugin, House house) {
        this.plugin = plugin;
        this.house = house;
        this.values = values();
        ConfigurationSection info = plugin.getGuiConfig().getConfigurationSection("available-details.info");
        for (int slot : plugin.getGuiConfig().getIntegerList("available-details.info.slots")) {
            addButton(slot, new InventoryButton().creator(player -> GuiItems.create(info, values))
                    .consumer(event -> { }));
        }
        ConfigurationSection chests = plugin.getGuiConfig().getConfigurationSection("available-details.chests");
        if (chests != null) for (int slot : chests.getIntegerList("slots")) {
            addButton(slot, new InventoryButton().creator(player -> GuiItems.create(chests, values))
                    .consumer(event -> { }));
        }
        ConfigurationSection preview = plugin.getGuiConfig().getConfigurationSection("available-details.preview");
        for (int slot : plugin.getGuiConfig().getIntegerList("available-details.preview.slots")) {
            addButton(slot, new InventoryButton().creator(player -> GuiItems.create(preview, values))
                    .consumer(event -> {
                        Player player = (Player) event.getWhoClicked();
                        if (house.getOwner() != null) {
                            plugin.getMessages().send(player, "already-owned", new HashMap<>());
                            player.closeInventory();
                            return;
                        }
                        HouseDoor door = house.getDoors().isEmpty() ? null : house.getDoors().get(0);
                        if (door == null || door.getOutside() == null || door.getOutside().getWorld() == null) {
                            plugin.getMessages().send(player, "house-not-ready", new HashMap<>());
                            return;
                        }
                        player.closeInventory();
                        plugin.beginPreviewTeleport(player, house, door);
                    }));
        }
        ConfigurationSection purchase = plugin.getGuiConfig().getConfigurationSection("available-details.purchase");
        for (int slot : plugin.getGuiConfig().getIntegerList("available-details.purchase.slots")) {
            addButton(slot, new InventoryButton().creator(player -> GuiItems.create(purchase, values))
                    .consumer(event -> plugin.openPurchase((Player) event.getWhoClicked(), house)));
        }
        ConfigurationSection close = plugin.getGuiConfig().getConfigurationSection("available-details.close");
        for (int slot : plugin.getGuiConfig().getIntegerList("available-details.close.slots")) {
            addButton(slot, new InventoryButton().creator(player -> GuiItems.create(close, values))
                    .consumer(event -> event.getWhoClicked().closeInventory()));
        }
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, plugin.getGuiConfig().getInt("available-details.size", 36),
                ChatColor.translateAlternateColorCodes('&',
                        plugin.getGuiConfig().getString("available-details.title", "&8House: &f{house}")
                                .replace("{house}", house.getName())));
    }

    private Map<String, String> values() {
        Map<String, String> values = new HashMap<>();
        values.put("house", house.getName());
        values.put("type", house.getType().name().toLowerCase());
        values.put("price", plugin.formatAmount(house.getPrice()));
        values.put("availability", house.getOwner() == null ? "Available" : "Unavailable");
        values.put("vault_counts", String.valueOf(house.getVaults().size()));
        return values;
    }
}