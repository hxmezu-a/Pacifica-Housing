package com.districtx.housing.inventory;

import com.cryptomorin.xseries.XMaterial;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collections;

public abstract class InventoryGUI implements InventoryHandler {
    private static final Set<Integer> WHITE_SLOTS = new HashSet<>(Arrays.asList(
            0, 9, 18, 27, 36, 45, 8, 17, 26, 35, 44, 53));
    private static final Set<Integer> BLACK_SLOTS = new HashSet<>(Arrays.asList(
            1, 2, 3, 4, 5, 6, 7, 10, 16, 19, 25, 28, 34, 37, 43, 46, 52));
    private Inventory inventory;
    private final Map<Integer, InventoryButton> buttonMap = new HashMap<>();

    public Inventory getInventory() {
        if (inventory == null) {
            inventory = createInventory();
        }
        return inventory;
    }

    public void addButton(int slot, InventoryButton button) {
        buttonMap.put(slot, button);
    }

    public void removeButton(int slot) {
        buttonMap.remove(slot);
    }

    public boolean supportsNavigation() {
        return true;
    }

    public void decorate(Player player) {
        if (getInventory().getType() == InventoryType.CHEST) {
            decorateBackground();
        }
        buttonMap.forEach((slot, button) -> {
            ItemStack icon = button.getIconCreator().apply(player);
            if (icon != null) {
                getInventory().setItem(slot, icon);
            }
        });
    }

    protected List<Integer> getContentSlots() {
        List<Integer> slots = new ArrayList<>();
        for (int slot = 0; slot < getInventory().getSize(); slot++) {
            if (!WHITE_SLOTS.contains(slot) && !BLACK_SLOTS.contains(slot)) {
                slots.add(slot);
            }
        }
        return slots;
    }

    protected void decorateBackground() {
        Inventory target = getInventory();
        for (int slot = 0; slot < target.getSize(); slot++) {
            String material = WHITE_SLOTS.contains(slot) ? "WHITE_STAINED_GLASS_PANE"
                    : BLACK_SLOTS.contains(slot) ? "BLACK_STAINED_GLASS_PANE" : "LIGHT_GRAY_STAINED_GLASS_PANE";
            ItemStack pane = XMaterial.matchXMaterial(material).map(XMaterial::parseItem).orElse(null);
            if (pane != null) {
                ItemMeta meta = pane.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&f"));
                    meta.setLore(Collections.emptyList());
                    pane.setItemMeta(meta);
                }
                target.setItem(slot, pane);
            }
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if ((event.getClick() != ClickType.LEFT && event.getClick() != ClickType.RIGHT)
                || event.getRawSlot() < 0 || event.getRawSlot() >= getInventory().getSize()) {
            return;
        }
        InventoryButton button = buttonMap.get(event.getRawSlot());
        if (button != null && button.getEventConsumer() != null) {
            button.getEventConsumer().accept(event);
        }
    }

    @Override
    public void onDrag(InventoryDragEvent event) {
        event.setCancelled(true);
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player) {
            decorate((Player) event.getPlayer());
        }
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
    }

    protected abstract Inventory createInventory();
}