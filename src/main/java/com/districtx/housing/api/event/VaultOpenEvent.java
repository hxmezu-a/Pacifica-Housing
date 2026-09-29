package com.districtx.housing.api.event;

import com.districtx.housing.api.HouseInfo;
import com.districtx.housing.api.VaultInfo;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Fired after ownership validation and before a house vault inventory opens. */
public final class VaultOpenEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final HouseInfo house;
    private final VaultInfo vault;
    private boolean cancelled;

    /** Creates the event for an authorized vault-open request. */
    public VaultOpenEvent(Player player, HouseInfo house, VaultInfo vault) {
        this.player = player;
        this.house = house;
        this.vault = vault;
    }

    /** Returns the player requesting to open the vault. */
    public Player getPlayer() {
        return player;
    }

    /** Returns an immutable snapshot of the containing house. */
    public HouseInfo getHouse() {
        return house;
    }

    /** Returns an immutable snapshot of the requested vault. */
    public VaultInfo getVault() {
        return vault;
    }

    /** Returns whether an integration cancelled the open request. */
    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    /** Sets whether the open request should be cancelled. */
    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    /** Gets the event handler list. */
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    /** Gets the static event handler list. */
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}