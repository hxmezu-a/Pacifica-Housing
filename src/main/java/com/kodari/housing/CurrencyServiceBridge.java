package com.kodari.housing;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.UUID;

public class CurrencyServiceBridge {
    private final HousingPlugin housing;
    private Plugin currencyPlugin;
    private Method getCurrencyName;
    private Method take;
    private Method give;

    public CurrencyServiceBridge(HousingPlugin housing) {
        this.housing = housing;
        refresh();
    }

    public void refresh() {
        currencyPlugin = housing.getServer().getPluginManager().getPlugin("KodariCurrency");
        getCurrencyName = null;
        take = null;
        give = null;
        if (currencyPlugin == null || !currencyPlugin.isEnabled()) {
            return;
        }
        try {
            Class<?> type = currencyPlugin.getClass();
            getCurrencyName = type.getMethod("getCurrencyName");
            take = type.getMethod("take", OfflinePlayer.class, double.class);
            give = type.getMethod("give", OfflinePlayer.class, double.class);
        } catch (NoSuchMethodException exception) {
            housing.getLogger().warning("KodariCurrency does not expose the required API.");
        }
    }

    public boolean isAvailable() {
        return currencyPlugin != null && currencyPlugin.isEnabled()
                && getCurrencyName != null && take != null && give != null;
    }

    public String getCurrencyName() {
        if (!isAvailable()) return "Diamond Currency";
        try {
            return String.valueOf(getCurrencyName.invoke(currencyPlugin));
        } catch (ReflectiveOperationException exception) {
            return "Diamond Currency";
        }
    }

    public boolean take(OfflinePlayer player, double amount) {
        return invokeBoolean(take, player, amount);
    }

    public boolean give(OfflinePlayer player, double amount) {
        return invokeBoolean(give, player, amount);
    }

    private boolean invokeBoolean(Method method, OfflinePlayer player, double amount) {
        if (!isAvailable() || method == null) return false;
        try {
            return Boolean.TRUE.equals(method.invoke(currencyPlugin, player, amount));
        } catch (ReflectiveOperationException exception) {
            housing.getLogger().warning("Could not communicate with KodariCurrency: " + exception.getMessage());
            return false;
        }
    }
}