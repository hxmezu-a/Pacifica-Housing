package com.districtx.housing;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.function.BooleanSupplier;

public class CurrencyServiceBridge {
    private final HousingPlugin housing;
    private Object diamondCurrencyService;
    private Object transactionService;
    private String lastStatus;

    public CurrencyServiceBridge(HousingPlugin housing) {
        this.housing = housing;
        refresh();
    }

    public void refresh() {
        Plugin pacificaCore = Bukkit.getPluginManager().getPlugin("Pacifica-Core");
        if (pacificaCore == null) {
            clearServices();
            logStatus("missing", "Pacifica-Core was not detected. Diamond Currency integration disabled.");
            return;
        }
        if (!pacificaCore.isEnabled()) {
            clearServices();
            logStatus("disabled", "Pacifica-Core is disabled. Diamond Currency integration disabled.");
            return;
        }

        try {
            Class<?> coreClass = Class.forName("com.districtx.pacificacore.PacificaCore");
            Object api = coreClass.getMethod("getAPI").invoke(null);
            if (api == null) {
                throw new IllegalStateException("Pacifica-Core API is unavailable");
            }
            Object diamonds = invoke(api, "getDiamondCurrencyService");
            Object transactions = invoke(api, "getTransactionService");
            if (diamonds == null || transactions == null) {
                throw new IllegalStateException("Pacifica-Core currency services are unavailable");
            }
            diamondCurrencyService = diamonds;
            transactionService = transactions;
            logStatus("available", "Pacifica-Core detected. Diamond Currency API connected.");
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            clearServices();
            logStatus("error", "Pacifica-Core was detected, but its Diamond Currency API could not be initialized.");
        }
    }

    public boolean isAvailable() {
        refresh();
        return diamondCurrencyService != null;
    }

    public String getCurrencyName() {
        if (!isAvailable()) return "Diamond";
        try {
            Object name = invoke(diamondCurrencyService, "getCurrencyName");
            return name instanceof String && !((String) name).isEmpty() ? (String) name : "Diamond";
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            return "Diamond";
        }
    }

    public boolean take(OfflinePlayer player, double amount) {
        BigDecimal value = toAmount(amount);
        if (player == null || value == null || !isAvailable()) return false;
        try {
            return (Boolean) invoke(diamondCurrencyService, "withdraw",
                    new Class<?>[]{UUID.class, BigDecimal.class}, player.getUniqueId(), value);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            return false;
        }
    }

    public boolean give(OfflinePlayer player, double amount) {
        BigDecimal value = toAmount(amount);
        if (player == null || value == null || !isAvailable()) return false;
        try {
            return (Boolean) invoke(diamondCurrencyService, "deposit",
                    new Class<?>[]{UUID.class, BigDecimal.class}, player.getUniqueId(), value);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            return false;
        }
    }

    public BigDecimal getDiamondBalance(UUID player) {
        if (player == null || !isAvailable()) return BigDecimal.ZERO;
        try {
            Object balance = invoke(diamondCurrencyService, "getBalance",
                    new Class<?>[]{UUID.class}, player);
            return balance instanceof BigDecimal ? (BigDecimal) balance : BigDecimal.ZERO;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            return BigDecimal.ZERO;
        }
    }

    public boolean hasDiamonds(UUID player, double amount) {
        BigDecimal value = toAmount(amount);
        if (player == null || value == null || !isAvailable()) return false;
        try {
            return (Boolean) invoke(diamondCurrencyService, "has",
                    new Class<?>[]{UUID.class, BigDecimal.class}, player, value);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            return false;
        }
    }

    public boolean setDiamondBalance(UUID player, double amount) {
        BigDecimal value = toNonNegativeAmount(amount);
        if (player == null || value == null || !isAvailable()) return false;
        try {
            return (Boolean) invoke(diamondCurrencyService, "setBalance",
                    new Class<?>[]{UUID.class, BigDecimal.class}, player, value);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            return false;
        }
    }

    public boolean resetDiamondBalance(UUID player) {
        if (player == null || !isAvailable()) return false;
        try {
            return (Boolean) invoke(diamondCurrencyService, "resetBalance",
                    new Class<?>[]{UUID.class}, player);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            return false;
        }
    }

    public boolean process(OfflinePlayer player, double amount, BooleanSupplier action) {
        return process(player, amount, 0, action);
    }

    public boolean process(OfflinePlayer player, double diamonds, double balance, BooleanSupplier action) {
        BigDecimal diamondAmount = toAmount(diamonds);
        BigDecimal balanceAmount = toNonNegativeAmount(balance);
        if (player == null || diamondAmount == null || balanceAmount == null || action == null
                || !isAvailable() || transactionService == null) return false;

        boolean balanceWithdrawn = false;
        if (balanceAmount.signum() > 0) {
            EconomyManager economy = housing.getEconomyManager();
            if (economy == null || !economy.available()
                    || !economy.withdraw(player, balanceAmount.doubleValue())) return false;
            balanceWithdrawn = true;
        }
        try {
            boolean processed = (Boolean) invoke(transactionService, "process",
                    new Class<?>[]{UUID.class, BigDecimal.class, BigDecimal.class, BooleanSupplier.class},
                    player.getUniqueId(), diamondAmount, BigDecimal.ZERO, action);
            if (!processed && balanceWithdrawn) {
                housing.getEconomyManager().deposit(player, balanceAmount.doubleValue());
            }
            return processed;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            if (balanceWithdrawn) {
                housing.getEconomyManager().deposit(player, balanceAmount.doubleValue());
            }
            return false;
        }
    }

    private void clearServices() {
        diamondCurrencyService = null;
        transactionService = null;
    }

    private void logStatus(String status, String message) {
        if (!status.equals(lastStatus)) {
            housing.getLogger().info(message);
            lastStatus = status;
        }
    }

    private static BigDecimal toAmount(double amount) {
        if (!Double.isFinite(amount) || amount <= 0) return null;
        return BigDecimal.valueOf(amount);
    }

    private static BigDecimal toNonNegativeAmount(double amount) {
        if (!Double.isFinite(amount) || amount < 0) return null;
        return BigDecimal.valueOf(amount);
    }

    private static Object invoke(Object target, String name, Class<?>[] parameterTypes, Object... arguments)
            throws ReflectiveOperationException {
        Method method = target.getClass().getMethod(name, parameterTypes);
        return method.invoke(target, arguments);
    }

    private static Object invoke(Object target, String name) throws ReflectiveOperationException {
        return invoke(target, name, new Class<?>[0]);
    }
}