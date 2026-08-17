package com.kodari.housing.inventory.gui;

import com.kodari.housing.HousingPlugin;
import com.kodari.housing.inventory.InventoryButton;
import com.kodari.housing.inventory.InventoryGUI;
import com.kodari.housing.model.House;
import com.kodari.housing.util.GuiItems;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class OwnedPremiumHousesGUI extends InventoryGUI {
    private final HousingPlugin plugin;
    private final UUID owner;
    private final List<House> houses = new ArrayList<>();

    public OwnedPremiumHousesGUI(HousingPlugin plugin, Player player) {
        this.plugin = plugin;
        this.owner = player.getUniqueId();
        for (House house : plugin.getHouseManager().all()) {
            if (owner.equals(house.getOwner()) && house.getType() == com.kodari.housing.model.HouseType.PREMIUM
                    && houses.size() < 54) {
                houses.add(house);
            }
        }
        ConfigurationSection item = plugin.getGuiConfig().getConfigurationSection("auction-create.item");
        for (int slot = 0; slot < houses.size(); slot++) {
            final House house = houses.get(slot);
            addButton(slot, new InventoryButton()
                    .creator(player1 -> GuiItems.create(item, values(house)))
                    .consumer(event -> {
                        Player clicker = (Player) event.getWhoClicked();
                        if (owner.equals(clicker.getUniqueId())) {
                            plugin.openAuctionSetup(clicker, house);
                        }
                    }));
        }
    }

    private HashMap<String, String> values(House house) {
        HashMap<String, String> values = new HashMap<>();
        values.put("house", house.getName());
        values.put("type", house.getType().name().toLowerCase());
        return values;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, plugin.getGuiConfig().getInt("auction-create.size", 54),
                ChatColor.translateAlternateColorCodes('&',
                        plugin.getGuiConfig().getString("auction-create.title", "&8Create Auction / Bid")));
    }
}