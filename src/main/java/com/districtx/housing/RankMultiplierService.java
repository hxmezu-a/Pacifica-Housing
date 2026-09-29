package com.districtx.housing;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.math.BigDecimal;

public class RankMultiplierService {
    private final HousingPlugin plugin;

    public RankMultiplierService(HousingPlugin plugin) {
        this.plugin = plugin;
    }

    public BigDecimal getMultiplier(Player player) {
        ConfigurationSection ranks = plugin.getConfig().getConfigurationSection("listing-fee.rank-multipliers");
        BigDecimal fallback = decimalValue(ranks == null ? null : ranks.getString("default"), BigDecimal.ONE);
        if (ranks == null || player == null) return fallback;

        String group = getPrimaryGroup(player);
        if (group == null) return fallback;
        for (String key : ranks.getKeys(false)) {
            if ("default".equalsIgnoreCase(key) || !key.equalsIgnoreCase(group)) continue;
            return decimalValue(ranks.getString(key), fallback);
        }
        return fallback;
    }

    public String getPrimaryGroup(Player player) {
        if (player == null) return "default";
        try {
            LuckPerms luckPerms = LuckPermsProvider.get();
            User user = luckPerms.getUserManager().getUser(player.getUniqueId());
            return user == null || user.getPrimaryGroup() == null || user.getPrimaryGroup().isEmpty()
                    ? "default" : user.getPrimaryGroup();
        } catch (IllegalStateException | NoClassDefFoundError ignored) {
            return "default";
        }
    }

    public boolean hasConfiguredMultiplier(Player player) {
        ConfigurationSection ranks = plugin.getConfig().getConfigurationSection("listing-fee.rank-multipliers");
        if (ranks == null) return false;
        String group = getPrimaryGroup(player);
        if (group == null) return false;
        for (String key : ranks.getKeys(false)) {
            if (!"default".equalsIgnoreCase(key) && key.equalsIgnoreCase(group)) return true;
        }
        return false;
    }

    private BigDecimal decimalValue(String value, BigDecimal fallback) {
        if (value == null) return fallback;
        try {
            BigDecimal result = new BigDecimal(value.trim());
            return result.signum() < 0 ? fallback : result;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}