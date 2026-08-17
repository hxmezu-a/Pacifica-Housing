package com.kodari.housing;

import com.kodari.housing.model.House;
import com.kodari.housing.model.HouseAuction;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Locale;
import java.util.UUID;

public class AuctionChatListener implements Listener {
    private final HousingPlugin plugin;

    public AuctionChatListener(HousingPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (plugin.getPendingAuction(uuid) != null) {
            event.setCancelled(true);
            String[] parts = event.getMessage().trim().split("\\s+");
            House house = plugin.getPendingAuction(uuid).getHouse();
            String mode = plugin.getPendingAuction(uuid).getMode();
            plugin.clearPendingAuction(uuid);
            if (parts.length != 2) {
                plugin.getMessages().send(event.getPlayer(),
                        "diamonds".equals(mode) ? "auction-input-diamonds" : "auction-input-both",
                        java.util.Collections.emptyMap());
                return;
            }
            try {
                double diamonds = Double.parseDouble(parts[0]);
                double balance = Double.parseDouble(parts[1]);
                boolean balanceAllowed = "both".equals(mode);
                if (!"diamonds".equals(mode) && !balanceAllowed) {
                    plugin.getMessages().send(event.getPlayer(), "auction-invalid", java.util.Collections.emptyMap());
                    return;
                }
                if (!balanceAllowed && balance != 0) {
                    plugin.getMessages().send(event.getPlayer(), "auction-invalid", java.util.Collections.emptyMap());
                    return;
                }
                final double finalDiamonds = diamonds;
                final double finalBalance = balance;
                final boolean finalBalanceAllowed = balanceAllowed;
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (!plugin.createAuction(event.getPlayer(), house, finalDiamonds, finalBalance,
                            finalBalanceAllowed, 0L)) {
                        plugin.getMessages().send(event.getPlayer(), "auction-invalid", java.util.Collections.emptyMap());
                    } else {
                        plugin.getMessages().send(event.getPlayer(), "auction-created", java.util.Collections.singletonMap("house", house.getName()));
                    }
                });
            } catch (NumberFormatException exception) {
                plugin.getMessages().send(event.getPlayer(), "auction-invalid", java.util.Collections.emptyMap());
            }
            return;
        }
        HouseAuction auction = plugin.getPendingBid(uuid);
        if (auction == null) return;
        event.setCancelled(true);
        plugin.clearPendingBid(uuid);
        String input = event.getMessage().trim();
        String normalizedInput = input.toUpperCase(Locale.ROOT);
        boolean balanceAllowed = auction.isBalanceAllowed();
        try {
            double diamonds;
            double balance;
            if (balanceAllowed) {
                if (!normalizedInput.matches("C:[0-9]+(?:\\.[0-9]+)?/[0-9]+(?:\\.[0-9]+)?")) {
                    throw new NumberFormatException();
                }
                String[] values = normalizedInput.substring(2).split("/", -1);
                if (values.length != 2) {
                    throw new NumberFormatException();
                }
                diamonds = Double.parseDouble(values[0]);
                balance = Double.parseDouble(values[1]);
            } else {
                if (!normalizedInput.matches("D:[0-9]+(?:\\.[0-9]+)?")) {
                    throw new NumberFormatException();
                }
                diamonds = Double.parseDouble(normalizedInput.substring(2));
                balance = 0;
            }
            plugin.getServer().getScheduler().runTask(plugin,
                    () -> plugin.placeBid(event.getPlayer(), auction, diamonds, balance));
        } catch (NumberFormatException exception) {
            plugin.getMessages().send(event.getPlayer(), "bid-invalid", java.util.Collections.emptyMap());
        }
    }
}