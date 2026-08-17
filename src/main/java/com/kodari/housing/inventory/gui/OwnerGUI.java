package com.kodari.housing.inventory.gui;

import com.kodari.housing.HousingPlugin;
import com.kodari.housing.inventory.InventoryButton;
import com.kodari.housing.inventory.InventoryGUI;
import com.kodari.housing.model.House;
import com.kodari.housing.model.HouseType;
import com.kodari.housing.util.GuiItems;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;

public class OwnerGUI extends InventoryGUI {
    private final HousingPlugin plugin;
    private final House house;

    public OwnerGUI(HousingPlugin plugin, House house) {
        this.plugin = plugin;
        this.house = house;
        ConfigurationSection info = plugin.getGuiConfig().getConfigurationSection("owner.info");
        for (int slot : plugin.getGuiConfig().getIntegerList("owner.info.slots")) {
            addButton(slot, new InventoryButton().creator(player -> GuiItems.create(info, values()))
                    .consumer(event -> { }));
        }
        ConfigurationSection vault = plugin.getGuiConfig().getConfigurationSection("owner.vault");
        if (house.getType() != HouseType.REGULAR) {
            for (int slot : plugin.getGuiConfig().getIntegerList("owner.vault.slots")) {
                addButton(slot, new InventoryButton().creator(player -> GuiItems.create(vault, values()))
                        .consumer(event -> { }));
            }
        }
        ConfigurationSection sell = plugin.getGuiConfig().getConfigurationSection("owner.sell");
        for (int slot : plugin.getGuiConfig().getIntegerList("owner.sell.slots")) {
            addButton(slot, new InventoryButton().creator(player -> GuiItems.create(sell, values()))
                    .consumer(event -> {
                        Player player = (Player) event.getWhoClicked();
                        if (house.getType() == HouseType.LUXURY) {
                            plugin.getMessages().send(player, "cannot-sell-luxury", new HashMap<>());
                            return;
                        }
                        plugin.openSellConfirmation(player, house);
                    }));
        }
        ConfigurationSection close = plugin.getGuiConfig().getConfigurationSection("owner.close");
        for (int slot : plugin.getGuiConfig().getIntegerList("owner.close.slots")) {
            addButton(slot, new InventoryButton().creator(player -> GuiItems.create(close, values()))
                    .consumer(event -> event.getWhoClicked().closeInventory()));
        }
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, plugin.getGuiConfig().getInt("owner.size", 27),
                ChatColor.translateAlternateColorCodes('&',
                        plugin.getGuiConfig().getString("owner.title", "Manage {house}")
                                .replace("{house}", house.getName())));
    }

    private Map<String, String> values() {
        Map<String, String> values = new HashMap<>();
        values.put("house", house.getName());
        values.put("type", house.getType().name().toLowerCase());
        values.put("price", plugin.formatAmount(house.getPrice()));
        values.put("refund", plugin.formatAmount(plugin.refundAmount(house)));
        values.put("vaults", String.valueOf(house.getVaults().size()));
        return values;
    }
}