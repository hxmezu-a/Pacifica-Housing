package com.kodari.housing.inventory.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;

public class GUIListener implements Listener {
    private final GUIManager manager;

    public GUIListener(GUIManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        manager.handleClick(event);
    }

    @EventHandler
    public void onOpen(InventoryOpenEvent event) {
        manager.handleOpen(event);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        manager.handleClose(event);
    }
}