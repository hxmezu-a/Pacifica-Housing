package com.districtx.housing.inventory;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;

public interface InventoryHandler {
    void onClick(InventoryClickEvent event);
    void onDrag(InventoryDragEvent event);
    default void onPrepare(PrepareAnvilEvent event) {
    }
    void onOpen(InventoryOpenEvent event);
    void onClose(InventoryCloseEvent event);
}