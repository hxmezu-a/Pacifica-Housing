package com.kodari.housing.inventory.gui;

import com.cryptomorin.xseries.XMaterial;
import com.kodari.housing.HouseManager;
import com.kodari.housing.HousingPlugin;
import com.kodari.housing.inventory.InventoryButton;
import com.kodari.housing.inventory.InventoryGUI;
import com.kodari.housing.model.House;
import com.kodari.housing.model.HouseVault;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;

public class VaultsGUI extends InventoryGUI {
    private final HousingPlugin plugin;
    private final House house;

    public VaultsGUI(HousingPlugin plugin, House house) {
        this.plugin = plugin;
        this.house = house;
        int slot = 0;
        for (HouseVault vault : house.getVaults()) {
            if (slot >= 27) {
                break;
            }
            final HouseVault selectedVault = vault;
            addButton(slot++, new InventoryButton()
                    .creator(player -> createVaultItem(selectedVault, plugin.getHouseManager().getVaultNumber(house, selectedVault)))
                    .consumer(event -> plugin.openVault((Player) event.getWhoClicked(), house, selectedVault)));
        }
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, ChatColor.translateAlternateColorCodes('&',
                "&8Vaults: &f" + house.getName()));
    }

    private ItemStack createVaultItem(HouseVault vault, int number) {
        String material = "CHEST";
        if (HouseManager.isVaultContainer(vault.getLocation().getBlock())
                && "BARREL".equals(XMaterial.matchXMaterial(vault.getLocation().getBlock().getType()).name())) {
            material = "BARREL";
        }
        ItemStack item = XMaterial.matchXMaterial(material).map(XMaterial::parseItem).orElse(null);
        if (item == null) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&eVault #" + number));
        meta.setLore(Collections.singletonList(ChatColor.translateAlternateColorCodes('&', "&7Click to open")));
        item.setItemMeta(meta);
        return item;
    }
}