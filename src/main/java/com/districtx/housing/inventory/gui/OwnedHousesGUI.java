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
import java.util.UUID;

public class OwnedHousesGUI extends InventoryGUI {
    private final HousingPlugin plugin;
    private final UUID owner;
    private final List<House> houses = new ArrayList<>();

    public OwnedHousesGUI(HousingPlugin plugin, Player player) {
        this.plugin = plugin;
        this.owner = player.getUniqueId();
        int capacity = Math.min(getDisplaySize(), getContentSlots().size());
        for (House house : plugin.getHouseManager().all()) {
            if (owner.equals(house.getOwner()) && houses.size() < capacity) {
                houses.add(house);
            }
        }
        List<Integer> contentSlots = getContentSlots();
        for (int index = 0; index < houses.size() && index < contentSlots.size(); index++) {
            final House house = houses.get(index);
            int slot = contentSlots.get(index);
            addButton(slot, new InventoryButton()
                    .creator(player1 -> createHouseItem(house))
                    .consumer(event -> {
                        Player clicker = (Player) event.getWhoClicked();
                        if (!owner.equals(clicker.getUniqueId())) {
                            return;
                        }
                        plugin.openOwner(clicker, house);
                    }));
        }
        int houseLimit = plugin.getHouseLimit(player);
        int lockedSlots = houseLimit < 0 ? 0 : Math.min(capacity - houses.size(), Math.max(0, houseLimit - houses.size()));
        for (int index = houses.size(); index < houses.size() + lockedSlots && index < contentSlots.size(); index++) {
            int slot = contentSlots.get(index);
            addButton(slot, new InventoryButton().creator(player1 -> createLockedItem())
                    .consumer(event -> {
                    }));
        }
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, getDisplaySize(), ChatColor.translateAlternateColorCodes('&',
                plugin.getGuiConfig().getString("owned.title", "&8Your Houses")));
    }

    private int getDisplaySize() {
        int configuredSize = plugin.getGuiConfig().getInt("owned.size", 54);
        int size = Math.max(9, Math.min(54, configuredSize));
        return Math.min(54, ((size + 8) / 9) * 9);
    }

    private ItemStack createHouseItem(House house) {
        String material = house.getType() == HouseType.REGULAR ? "OAK_DOOR"
                : house.getType() == HouseType.PREMIUM ? "IRON_DOOR" : "CRIMSON_DOOR";
        ItemStack item = XMaterial.matchXMaterial(material).map(XMaterial::parseItem).orElse(null);
        if (item == null) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', house.getName()));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createLockedItem() {
        ItemStack item = XMaterial.matchXMaterial("SPRUCE_DOOR").map(XMaterial::parseItem).orElse(null);
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&7Locked House"));
        item.setItemMeta(meta);
        return item;
    }
}