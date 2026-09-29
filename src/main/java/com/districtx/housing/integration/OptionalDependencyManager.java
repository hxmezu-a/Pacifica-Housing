package com.districtx.housing.integration;

import org.bukkit.plugin.java.JavaPlugin;

public final class OptionalDependencyManager {
    private final JavaPlugin plugin;

    public OptionalDependencyManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void initializeIntegrations() {
        logStatus("CrackShot");
        logStatus("WeaponMechanics");
    }

    public boolean isCrackShotAvailable() {
        return plugin.getServer().getPluginManager().isPluginEnabled("CrackShot");
    }

    public boolean isWeaponMechanicsAvailable() {
        return plugin.getServer().getPluginManager().isPluginEnabled("WeaponMechanics");
    }

    private void logStatus(String pluginName) {
        if (plugin.getServer().getPluginManager().isPluginEnabled(pluginName)) {
            plugin.getLogger().info(pluginName + " detected. " + pluginName + " integration enabled.");
        } else {
            plugin.getLogger().info(pluginName + " not detected. " + pluginName + " integration disabled.");
        }
    }
}