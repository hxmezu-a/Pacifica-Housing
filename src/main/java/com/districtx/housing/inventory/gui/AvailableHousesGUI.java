package com.districtx.housing.inventory.gui;

import com.cryptomorin.xseries.XMaterial;
import com.districtx.housing.HousingPlugin;
import com.districtx.housing.inventory.InventoryButton;
import com.districtx.housing.inventory.InventoryGUI;
import com.districtx.housing.model.House;
import com.districtx.housing.model.HouseType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class AvailableHousesGUI extends InventoryGUI {
    private final HousingPlugin plugin;
    private final HouseType type;
    private final List<House> houses = new ArrayList<>();

    public AvailableHousesGUI(HousingPlugin plugin, HouseType type) {
        this.plugin = plugin;
        this.type = type;
        for (House house : plugin.getHouseManager().all()) {
            if (house.getOwner() == null && house.getType() == type && houses.size() < 54) {
                houses.add(house);
            }
        }
        List<Integer> contentSlots = getContentSlots();
        for (int index = 0; index < houses.size() && index < contentSlots.size(); index++) {
            final House house = houses.get(index);
            int slot = contentSlots.get(index);
            addButton(slot, new InventoryButton()
                    .creator(player -> createHouseItem(house))
                    .consumer(event -> {
                        Player player = (Player) event.getWhoClicked();
                        if (house.getOwner() == null) {
                            plugin.openAvailableHouseDetails(player, house);
                        } else {
                            player.closeInventory();
                        }
                    }));
        }
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, plugin.getGuiConfig().getInt("available-houses.size", 54),
                ChatColor.translateAlternateColorCodes('&',
                        plugin.getGuiConfig().getString("available-houses.title", "&8Available {type} Houses")
                                .replace("{type}", type.name().toLowerCase())));
    }

    private ItemStack createHouseItem(House house) {
        String material = house.getType() == HouseType.REGULAR ? "OAK_DOOR"
                : house.getType() == HouseType.PREMIUM ? "IRON_DOOR" : "CRIMSON_DOOR";
        ItemStack item = XMaterial.matchXMaterial(material).map(XMaterial::parseItem).orElse(null);
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + house.getName());
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Type: " + ChatColor.WHITE + house.getType().name().toLowerCase());
        lore.add(ChatColor.GRAY + "Price: " + ChatColor.WHITE + plugin.formatAmount(house.getPrice()));
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click to view house");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}