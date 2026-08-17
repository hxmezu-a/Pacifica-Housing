package com.kodari.housing.inventory.gui;

import com.kodari.housing.HousingPlugin;
import com.kodari.housing.inventory.InventoryButton;
import com.kodari.housing.inventory.InventoryGUI;
import com.kodari.housing.util.GuiItems;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;

public class AvailableCategoriesGUI extends InventoryGUI {
    private final HousingPlugin plugin;

    public AvailableCategoriesGUI(HousingPlugin plugin) {
        this.plugin = plugin;
        addCategory("regular", com.kodari.housing.model.HouseType.REGULAR);
        addCategory("premium", com.kodari.housing.model.HouseType.PREMIUM);
        addCategory("luxury", com.kodari.housing.model.HouseType.LUXURY);
    }

    private void addCategory(String key, com.kodari.housing.model.HouseType type) {
        ConfigurationSection section = plugin.getGuiConfig().getConfigurationSection("available-categories." + key);
        if (section == null) return;
        for (int slot : section.getIntegerList("slots")) {
            addButton(slot, new InventoryButton().creator(player -> GuiItems.create(section, new HashMap<>()))
                    .consumer(event -> plugin.openAvailableHouses((Player) event.getWhoClicked(), type)));
        }
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, plugin.getGuiConfig().getInt("available-categories.size", 27),
                ChatColor.translateAlternateColorCodes('&', plugin.getGuiConfig().getString("available-categories.title", "&8Available Houses")));
    }
}