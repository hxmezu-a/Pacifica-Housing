package com.districtx.housing.inventory.gui;

import com.districtx.housing.inventory.InventoryGUI;
import com.districtx.housing.inventory.InventoryHandler;
import com.districtx.housing.inventory.InventoryButton;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class GUIManager {
    private final Map<Inventory, InventoryHandler> activeInventories = new HashMap<>();
    private final Map<UUID, List<NavigationEntry>> navigationHistory = new HashMap<>();
    private final Set<UUID> switchingInventories = new HashSet<>();

    public void openGUI(InventoryGUI gui, Player player) {
        openGUI(gui, player, true);
    }

    private void openGUI(InventoryGUI gui, Player player, boolean rememberCurrent) {
        UUID playerId = player.getUniqueId();
        InventoryHandler currentHandler = activeInventories.get(player.getOpenInventory().getTopInventory());
        if (rememberCurrent && currentHandler instanceof InventoryGUI && currentHandler != gui) {
            InventoryGUI currentGUI = (InventoryGUI) currentHandler;
            if (currentGUI.supportsNavigation()) {
                navigationHistory.computeIfAbsent(playerId, ignored -> new ArrayList<>())
                        .add(new NavigationEntry(currentGUI, player.getOpenInventory().getTitle()));
            } else {
                navigationHistory.remove(playerId);
            }
        } else if (rememberCurrent && !(currentHandler instanceof InventoryGUI)) {
            navigationHistory.remove(playerId);
        }

        List<NavigationEntry> history = navigationHistory.get(playerId);
        gui.removeButton(45);
        if (gui.supportsNavigation() && history != null && !history.isEmpty()
                && gui.getInventory().getSize() > 45) {
            NavigationEntry parent = history.get(history.size() - 1);
            gui.addButton(45, new InventoryButton()
                    .creator(viewer -> createBackItem(parent.title))
                    .consumer(event -> openPrevious((Player) event.getWhoClicked())));
        }

        activeInventories.put(gui.getInventory(), gui);
        switchingInventories.add(playerId);
        try {
            player.openInventory(gui.getInventory());
        } finally {
            switchingInventories.remove(playerId);
        }
    }

    public void openPrevious(Player player) {
        UUID playerId = player.getUniqueId();
        List<NavigationEntry> history = navigationHistory.get(playerId);
        if (history == null || history.isEmpty()) {
            return;
        }
        InventoryGUI previous = history.remove(history.size() - 1).gui;
        if (history.isEmpty()) {
            navigationHistory.remove(playerId);
        }
        openGUI(previous, player, false);
    }

    public void refreshOpenMyListings() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            InventoryHandler handler = activeInventories.get(player.getOpenInventory().getTopInventory());
            if (handler instanceof MyListingsGUI) {
                ((MyListingsGUI) handler).decorate(player);
            }
        }
    }

    private ItemStack createBackItem(String parentTitle) {
        ItemStack item = XMaterial.matchXMaterial("REDSTONE").map(XMaterial::parseItem).orElse(null);
        if (item == null) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&c&lBack"));
        String parentName = ChatColor.stripColor(parentTitle);
        meta.setLore(java.util.Collections.singletonList(ChatColor.translateAlternateColorCodes('&',
                "&7Return to the " + parentName + " menu.")));
        item.setItemMeta(meta);
        return item;
    }

    public void handleClick(InventoryClickEvent event) {
        InventoryHandler handler = findHandler(event.getView().getTopInventory(), event.getWhoClicked());
        if (handler != null) {
            handler.onClick(event);
        }
    }

    public void handleDrag(InventoryDragEvent event) {
        InventoryHandler handler = findHandler(event.getView().getTopInventory(), event.getWhoClicked());
        if (handler != null) {
            handler.onDrag(event);
        }
    }

    public void handlePrepare(PrepareAnvilEvent event) {
        InventoryHandler handler = findHandler(event.getInventory(), event.getView().getPlayer());
        if (handler != null) {
            handler.onPrepare(event);
        }
    }

    public void handleOpen(InventoryOpenEvent event) {
        InventoryHandler handler = findHandler(event.getInventory(), event.getPlayer());
        if (handler != null) {
            handler.onOpen(event);
        }
    }

    public void handleClose(InventoryCloseEvent event) {
        InventoryHandler handler = findHandler(event.getInventory(), event.getPlayer());
        activeInventories.remove(event.getInventory());
        if (!switchingInventories.contains(event.getPlayer().getUniqueId())) {
            navigationHistory.remove(event.getPlayer().getUniqueId());
        }
        if (handler != null) {
            handler.onClose(event);
        }
    }

    private InventoryHandler findHandler(Inventory inventory, org.bukkit.entity.HumanEntity player) {
        return activeInventories.get(inventory);
    }

    private static final class NavigationEntry {
        private final InventoryGUI gui;
        private final String title;

        private NavigationEntry(InventoryGUI gui, String title) {
            this.gui = gui;
            this.title = title;
        }
    }
}