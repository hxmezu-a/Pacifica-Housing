package com.districtx.housing.inventory.gui;

import com.districtx.housing.HousingPlugin;
import com.districtx.housing.inventory.InventoryGUI;
import com.districtx.housing.model.House;
import com.districtx.housing.model.HouseVault;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;

public class VaultGUI extends InventoryGUI {
    private final HousingPlugin plugin;
    private final House house;
    private final HouseVault vault;

    public VaultGUI(HousingPlugin plugin, House house, HouseVault vault) {
        this.plugin = plugin;
        this.house = house;
        this.vault = vault;
    }

    @Override
    public boolean supportsNavigation() {
        return false;
    }

    @Override
    protected Inventory createInventory() {
        return plugin.getVaultInventory(vault);
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)
                || !((Player) event.getPlayer()).getUniqueId().equals(house.getOwner())) {
            event.setCancelled(true);
            return;
        }
        plugin.getVaultInventory(vault);
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)
                || !((Player) event.getWhoClicked()).getUniqueId().equals(house.getOwner())) {
            event.setCancelled(true);
            event.getWhoClicked().closeInventory();
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> plugin.saveVaultInventory(vault));
    }

    @Override
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)
                || !((Player) event.getWhoClicked()).getUniqueId().equals(house.getOwner())) {
            event.setCancelled(true);
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> plugin.saveVaultInventory(vault));
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player
                && ((Player) event.getPlayer()).getUniqueId().equals(house.getOwner())) {
            plugin.saveVaultInventory(vault);
        }
    }
}