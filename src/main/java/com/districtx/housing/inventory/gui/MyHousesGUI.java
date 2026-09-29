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
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MyHousesGUI extends InventoryGUI {
    private final HousingPlugin plugin;
    private final Player owner;
    private final List<House> houses = new ArrayList<>();

    public MyHousesGUI(HousingPlugin plugin, Player owner) {
        this.plugin = plugin;
        this.owner = owner;
        List<Integer> contentSlots = getContentSlots();
        for (House house : plugin.getHouseManager().all()) {
            if (owner.getUniqueId().equals(house.getOwner()) && houses.size() < contentSlots.size()) {
                houses.add(house);
            }
        }
        for (int index = 0; index < houses.size(); index++) {
            final House house = houses.get(index);
            addButton(contentSlots.get(index), new InventoryButton()
                    .creator(player -> createHouseItem(house))
                    .consumer(event -> {
                        Player clicker = (Player) event.getWhoClicked();
                        if (!owner.getUniqueId().equals(clicker.getUniqueId())) {
                            return;
                        }
                        HouseDoor door = house.getDoors().isEmpty() ? null : house.getDoors().get(0);
                        plugin.beginOwnedHouseTeleport(clicker, house, door);
                    }));
        }
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, ChatColor.translateAlternateColorCodes('&',
                plugin.getGuiConfig().getString("myhouses.title", "&8My Houses")));
    }

    private ItemStack createHouseItem(House house) {
        String material = house.getType() == HouseType.REGULAR ? "OAK_DOOR"
                : house.getType() == HouseType.PREMIUM ? "IRON_DOOR" : "CRIMSON_DOOR";
        ItemStack item = XMaterial.matchXMaterial(material).map(XMaterial::parseItem).orElse(null);
        if (item == null) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&9&lHouse " + house.getName()));
        meta.setLore(Arrays.asList(
                ChatColor.translateAlternateColorCodes('&', "&7Cost: &a" + plugin.formatAmount(house.getPrice())),
                ChatColor.translateAlternateColorCodes('&', "&7Chests: &6" + house.getVaults().size()),
                ChatColor.translateAlternateColorCodes('&', "&7House Type: &6" + displayType(house.getType())),
                ChatColor.translateAlternateColorCodes('&', "&7&lClick to teleport")));
        item.setItemMeta(meta);
        return item;
    }

    private String displayType(HouseType type) {
        String value = type == null ? "Unknown" : type.name().toLowerCase();
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}