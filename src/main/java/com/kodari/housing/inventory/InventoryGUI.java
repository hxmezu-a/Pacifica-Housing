package com.kodari.housing.inventory;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public abstract class InventoryGUI implements InventoryHandler {
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

    public void decorate(Player player) {
        buttonMap.forEach((slot, button) -> {
            ItemStack icon = button.getIconCreator().apply(player);
            if (icon != null) {
                getInventory().setItem(slot, icon);
            }
        });
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getRawSlot() < 0 || event.getRawSlot() >= getInventory().getSize()) {
            return;
        }
        InventoryButton button = buttonMap.get(event.getRawSlot());
        if (button != null && button.getEventConsumer() != null) {
            button.getEventConsumer().accept(event);
        }
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