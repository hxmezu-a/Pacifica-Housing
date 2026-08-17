package com.kodari.housing.inventory.gui;

import com.kodari.housing.HousingPlugin;
import com.kodari.housing.inventory.InventoryGUI;
import com.kodari.housing.model.House;
import com.kodari.housing.model.HouseVault;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;

public class VaultGUI extends InventoryGUI {
    private final HousingPlugin plugin;
    private final House house;
    private final HouseVault vault;
    private Inventory sourceInventory;

    public VaultGUI(HousingPlugin plugin, House house, HouseVault vault) {
        this.plugin = plugin;
        this.house = house;
        this.vault = vault;
    }

    @Override
    protected Inventory createInventory() {
        sourceInventory = plugin.getVaultInventory(vault);
        if (sourceInventory == null) {
            return Bukkit.createInventory(null, 27, title());
        }
        Inventory inventory = Bukkit.createInventory(null, sourceInventory.getSize(), title());
        inventory.setContents(sourceInventory.getContents());
        return inventory;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)
                || !((Player) event.getPlayer()).getUniqueId().equals(house.getOwner())) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)
                || !((Player) event.getWhoClicked()).getUniqueId().equals(house.getOwner())) {
            event.setCancelled(true);
            event.getWhoClicked().closeInventory();
        }
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)
                || !((Player) event.getPlayer()).getUniqueId().equals(house.getOwner())
                || !house.getVaults().contains(vault) || sourceInventory == null) {
            return;
        }
        Inventory current = plugin.getVaultInventory(vault);
        if (current != null) {
            current.setContents(getInventory().getContents());
        }
    }

    private String title() {
        int number = plugin.getHouseManager().getVaultNumber(house, vault);
        return ChatColor.translateAlternateColorCodes('&', "&8Vault #" + Math.max(1, number));
    }
}