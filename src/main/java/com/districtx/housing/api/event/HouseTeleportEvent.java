package com.districtx.housing.api.event;

import com.districtx.housing.api.HouseInfo;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Fired before Pacifica-Housing completes a house teleport. */
public final class HouseTeleportEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final HouseInfo house;
    private final Location destination;
    private boolean cancelled;

    /** Creates the event for an accepted house teleport transition. */
    public HouseTeleportEvent(Player player, HouseInfo house, Location destination) {
        this.player = player;
        this.house = house;
        this.destination = destination == null ? null : destination.clone();
    }

    /** Returns the player being teleported. */
    public Player getPlayer() {
        return player;
    }

    /** Returns an immutable snapshot of the destination house. */
    public HouseInfo getHouse() {
        return house;
    }

    /** Returns a copy of the destination location. */
    public Location getDestination() {
        return destination == null ? null : destination.clone();
    }

    /** Returns whether an integration cancelled the teleport. */
    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    /** Sets whether the teleport should be cancelled. */
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