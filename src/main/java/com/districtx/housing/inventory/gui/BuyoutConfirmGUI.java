package com.districtx.housing.inventory.gui;

import com.districtx.housing.HousingPlugin;
import com.districtx.housing.inventory.InventoryButton;
import com.districtx.housing.inventory.InventoryGUI;
import com.districtx.housing.model.HouseAuction;
import com.districtx.housing.util.GuiItems;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.Collections;

public class BuyoutConfirmGUI extends InventoryGUI {
    private final HousingPlugin plugin;
    private final HouseAuction auction;

    public BuyoutConfirmGUI(HousingPlugin plugin, HouseAuction auction) {
        this.plugin = plugin;
        this.auction = auction;
        ConfigurationSection confirm = plugin.getGuiConfig().getConfigurationSection("buyout.confirm");
        ConfigurationSection cancel = plugin.getGuiConfig().getConfigurationSection("buyout.cancel");
        for (int slot : plugin.getGuiConfig().getIntegerList("buyout.confirm.slots")) {
            addButton(slot, new InventoryButton().creator(player -> GuiItems.create(confirm,
                    Collections.singletonMap("buyout-price", plugin.formatBalance(auction.getBuyoutPrice()))))
                    .consumer(event -> {
                        Player player = (Player) event.getWhoClicked();
                        if (plugin.buyoutAuction(player, auction)) player.closeInventory();
                    }));
        }
        for (int slot : plugin.getGuiConfig().getIntegerList("buyout.cancel.slots")) {
            addButton(slot, new InventoryButton().creator(player -> GuiItems.create(cancel, Collections.emptyMap()))
                    .consumer(event -> plugin.openBid((Player) event.getWhoClicked(), auction)));
        }
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, plugin.getGuiConfig().getInt("buyout.size", 9),
                ChatColor.translateAlternateColorCodes('&', plugin.getGuiConfig()
                        .getString("buyout.title", "&8Confirm Buyout")));
    }
}