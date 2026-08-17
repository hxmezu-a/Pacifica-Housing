package com.kodari.housing;

import com.cryptomorin.xseries.XMaterial;
import com.kodari.housing.model.House;
import com.kodari.housing.model.HouseDoor;
import com.kodari.housing.model.HouseVault;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.List;

public class HouseListener implements Listener {
    private final HousingPlugin plugin;

    public HouseListener(HousingPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.isCancelled()) {
            plugin.cancelTransition(event.getPlayer());
            return;
        }
        if (!plugin.isExpectedTransition(event.getPlayer(), event.getTo())) {
            plugin.clearPlayerState(event.getPlayer());
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        plugin.clearPlayerState(event.getPlayer());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.clearPlayerState(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.clearPlayerState(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (plugin.isInside(player) && plugin.isWeaponRestrictionEnabled() && isWeapon(event.getItem())) {
            event.setCancelled(true);
            plugin.getMessages().send(player, "weapons-disabled", java.util.Collections.emptyMap());
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        House vaultHouse = plugin.getHouseManager().findByVault(event.getClickedBlock());
        if (vaultHouse != null) {
            if (vaultHouse.getOwner() == null || !vaultHouse.getOwner().equals(player.getUniqueId())
                    || plugin.getHouseFor(player) != vaultHouse) {
                event.setCancelled(true);
                plugin.getMessages().send(player, "not-owner", java.util.Collections.emptyMap());
            } else {
                HouseVault vault = plugin.getHouseManager().findVault(vaultHouse, event.getClickedBlock());
                event.setCancelled(true);
                if (vault != null) {
                    plugin.openVault(player, vaultHouse, vault);
                }
            }
            return;
        }
        if (!HouseManager.isIronDoor(event.getClickedBlock())) {
            return;
        }
        House house = plugin.getHouseManager().findByDoor(event.getClickedBlock());
        HouseDoor door = house == null ? null : plugin.getHouseManager().findDoor(house, event.getClickedBlock());
        if (house == null || door == null || plugin.isTransitioning(player)) {
            return;
        }
        event.setCancelled(true);
        if (house.getOwner() == null) {
            plugin.openAvailableHouseDetails(player, house);
            return;
        }
        if (house.getOwner().equals(player.getUniqueId())) {
            if (player.isSneaking()) {
                plugin.openOwner(player, house);
            } else {
                plugin.teleportOwnerThroughDoor(player, house, door);
            }
            return;
        }
        if (player.isSneaking()) {
            plugin.openOwnedHouseDetails(player, house);
            return;
        }
        plugin.openOwnedHouseDetails(player, house);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVaultBreak(BlockBreakEvent event) {
        House house = plugin.getHouseManager().findByVault(event.getBlock());
        if (house == null) {
            return;
        }
        Player player = event.getPlayer();
        if (house.getOwner() == null || !house.getOwner().equals(player.getUniqueId())
                || plugin.getHouseFor(player) != house) {
            event.setCancelled(true);
            plugin.getMessages().send(player, "not-owner", java.util.Collections.emptyMap());
            return;
        }
        HouseVault vault = plugin.getHouseManager().findVault(house, event.getBlock());
        if (vault != null) {
            plugin.getHouseManager().removeVault(house, vault);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onAgentInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        Entity entity = event.getRightClicked();
        String name = entity.getCustomName();
        if (name == null || !"Real Estate Agent".equalsIgnoreCase(ChatColor.stripColor(name))) return;
        event.setCancelled(true);
        plugin.openRealEstateAgent(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectile(ProjectileLaunchEvent event) {
        if (!plugin.isWeaponRestrictionEnabled() || !(event.getEntity().getShooter() instanceof Player)) return;
        Player player = (Player) event.getEntity().getShooter();
        if (plugin.isInside(player) && isConfiguredWeapon(player)) {
            event.setCancelled(true);
            plugin.getMessages().send(player, "weapons-disabled", java.util.Collections.emptyMap());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (plugin.isWeaponRestrictionEnabled() && plugin.isInside(player) && isWeapon(event.getBow())) {
            event.setCancelled(true);
            plugin.getMessages().send(player, "weapons-disabled", java.util.Collections.emptyMap());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player victim = (Player) event.getEntity();
        Player attacker = null;
        if (event.getDamager() instanceof Player) {
            attacker = (Player) event.getDamager();
        } else if (event.getDamager() instanceof Projectile
                && ((Projectile) event.getDamager()).getShooter() instanceof Player) {
            attacker = (Player) ((Projectile) event.getDamager()).getShooter();
        }
        if (attacker != null && plugin.isInside(victim) && !plugin.isInside(attacker)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player) || !plugin.isInside((Player) event.getWhoClicked())
                || !plugin.isGoldenChestplateRestrictionEnabled()) return;
        Player player = (Player) event.getWhoClicked();
        plugin.getServer().getScheduler().runTask(plugin, () -> removeGoldenChestplate(player));
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player) || !plugin.isInside((Player) event.getWhoClicked())
                || !plugin.isGoldenChestplateRestrictionEnabled()) return;
        Player player = (Player) event.getWhoClicked();
        plugin.getServer().getScheduler().runTask(plugin, () -> removeGoldenChestplate(player));
    }

    private void removeGoldenChestplate(Player player) {
        PlayerInventory inventory = player.getInventory();
        ItemStack chestplate = inventory.getChestplate();
        if (!isGoldenChestplate(chestplate)) return;
        inventory.setChestplate(null);
        java.util.Map<Integer, ItemStack> leftovers = inventory.addItem(chestplate);
        for (ItemStack leftover : leftovers.values()) player.getWorld().dropItemNaturally(player.getLocation(), leftover);
    }

    private boolean isWeapon(ItemStack item) {
        if (item == null) return false;
        String name = XMaterial.matchXMaterial(item.getType()).name();
        List<String> configured = plugin.getConfig().getStringList("restrictions.weapon-materials");
        return configured.stream().anyMatch(value -> value.equalsIgnoreCase(name));
    }

    private boolean isConfiguredWeapon(Player player) {
        return isWeapon(player.getInventory().getItemInMainHand())
                || isWeapon(player.getInventory().getItemInOffHand());
    }

    private boolean isGoldenChestplate(ItemStack item) {
        if (item == null) return false;
        String name = XMaterial.matchXMaterial(item.getType()).name();
        return "GOLDEN_CHESTPLATE".equals(name) || "GOLD_CHESTPLATE".equals(name);
    }
}