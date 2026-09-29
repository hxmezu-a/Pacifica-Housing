package com.districtx.housing.inventory.gui;

import com.districtx.housing.HousingPlugin;
import com.districtx.housing.inventory.InventoryButton;
import com.districtx.housing.inventory.InventoryGUI;
import com.districtx.housing.model.House;
import com.districtx.housing.util.GuiItems;
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
            if (owner.equals(house.getOwner()) && house.getType() == com.districtx.housing.model.HouseType.PREMIUM
                    && houses.size() < 54) {
                houses.add(house);
            }
        }
        ConfigurationSection item = plugin.getGuiConfig().getConfigurationSection("auction-create.item");
        java.util.List<Integer> contentSlots = getContentSlots();
        for (int index = 0; index < houses.size() && index < contentSlots.size(); index++) {
            final House house = houses.get(index);
            int slot = contentSlots.get(index);
            addButton(slot, new InventoryButton()
                    .creator(player1 -> GuiItems.create(item, values(house)))
                    .consumer(event -> {
                        Player clicker = (Player) event.getWhoClicked();
                        if (owner.equals(clicker.getUniqueId())) {
                            plugin.openAuctionInput(clicker, house);
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