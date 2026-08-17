package com.kodari.housing;

import com.kodari.housing.inventory.gui.GUIListener;
import com.kodari.housing.inventory.gui.GUIManager;
import com.kodari.housing.inventory.gui.OwnerGUI;
import com.kodari.housing.inventory.gui.OwnedHousesGUI;
import com.kodari.housing.inventory.gui.OwnedHouseDetailsGUI;
import com.kodari.housing.inventory.gui.AvailableHousesGUI;
import com.kodari.housing.inventory.gui.RealEstateAgentGUI;
import com.kodari.housing.inventory.gui.AvailableHouseDetailsGUI;
import com.kodari.housing.inventory.gui.PurchaseGUI;
import com.kodari.housing.inventory.gui.SellConfirmGUI;
import com.kodari.housing.inventory.gui.VaultGUI;
import com.kodari.housing.inventory.gui.VaultsGUI;
import com.kodari.housing.model.House;
import com.kodari.housing.model.HouseAuction;
import com.kodari.housing.model.CommittedBid;
import com.kodari.housing.model.HouseDoor;
import com.kodari.housing.model.HouseType;
import com.kodari.housing.model.HouseVault;
import com.kodari.housing.util.MessageService;
import com.cryptomorin.xseries.XPotion;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class HousingPlugin extends JavaPlugin {
    private static final long AUCTION_DURATION_SECONDS = 12L * 60L * 60L;
    private HouseManager houseManager;
    private AuctionManager auctionManager;
    private CurrencyServiceBridge currencyService;
    private EconomyManager economyManager;
    private GUIManager guiManager;
    private MessageService messages;
    private FileConfiguration guiConfig;
    private final Set<UUID> transitioning = new HashSet<>();
    private final Map<UUID, House> insidePlayers = new HashMap<>();
    private final Map<UUID, Location> transitionTargets = new HashMap<>();
    private final Map<UUID, PendingAuction> pendingAuctions = new HashMap<>();
    private final Map<UUID, HouseAuction> pendingBids = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);
        saveResource("INVENTORY GUI.yml", false);
        messages = new MessageService(YamlConfiguration.loadConfiguration(new File(getDataFolder(), "messages.yml")));
        guiConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "INVENTORY GUI.yml"));
        houseManager = new HouseManager(this);
        auctionManager = new AuctionManager(this);
        currencyService = new CurrencyServiceBridge(this);
        economyManager = new EconomyManager(this);
        guiManager = new GUIManager();
        getServer().getPluginManager().registerEvents(new GUIListener(guiManager), this);
        getServer().getPluginManager().registerEvents(new HouseListener(this), this);
        getServer().getPluginManager().registerEvents(new AuctionChatListener(this), this);
        HousesCommand command = new HousesCommand(this);
        getCommand("houses").setExecutor(command);
        getCommand("houses").setTabCompleter(command);
        HousesAdminCommand adminCommand = new HousesAdminCommand(this);
        getCommand("housesadmin").setExecutor(adminCommand);
        getCommand("housesadmin").setTabCompleter(adminCommand);
        getServer().getScheduler().runTaskTimer(this, this::processAuctions, 20L, 20L);
    }

    @Override
    public void onDisable() {
        if (houseManager != null) houseManager.save();
        if (auctionManager != null) auctionManager.save();
    }

    public void reloadConfiguration() {
        reloadConfig();
        messages = new MessageService(YamlConfiguration.loadConfiguration(new File(getDataFolder(), "messages.yml")));
        guiConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "INVENTORY GUI.yml"));
        houseManager.load();
        auctionManager.load();
        currencyService.refresh();
    }

    public void openPurchase(Player player, House house) {
        guiManager.openGUI(new PurchaseGUI(this, house), player);
    }

    public void openOwner(Player player, House house) {
        if (!house.getOwner().equals(player.getUniqueId())) {
            messages.send(player, "not-owner", java.util.Collections.emptyMap());
            return;
        }
        guiManager.openGUI(new OwnerGUI(this, house), player);
    }

    public void openVaults(Player player, House house) {
        if (!player.getUniqueId().equals(house.getOwner())) {
            messages.send(player, "not-owner", java.util.Collections.emptyMap());
            return;
        }
        if (house.getType() == HouseType.REGULAR) {
            messages.send(player, "vault-unavailable", java.util.Collections.emptyMap());
            return;
        }
        if (house.getVaults().isEmpty()) {
            messages.send(player, "no-vaults", java.util.Collections.emptyMap());
            return;
        }
        if (house.getVaults().size() == 1) {
            openVault(player, house, house.getVaults().get(0));
            return;
        }
        guiManager.openGUI(new VaultsGUI(this, house), player);
    }

    public void openVault(Player player, House house, HouseVault vault) {
        if (!player.getUniqueId().equals(house.getOwner())) {
            messages.send(player, "not-owner", java.util.Collections.emptyMap());
            return;
        }
        if (house.getType() == HouseType.REGULAR) {
            messages.send(player, "vault-unavailable", java.util.Collections.emptyMap());
            return;
        }
        if (!house.getVaults().contains(vault) || getVaultInventory(vault) == null) {
            messages.send(player, "vault-unavailable", java.util.Collections.emptyMap());
            return;
        }
        guiManager.openGUI(new VaultGUI(this, house, vault), player);
    }

    public Inventory getVaultInventory(HouseVault vault) {
        if (vault == null || vault.getLocation() == null || vault.getLocation().getWorld() == null) {
            return null;
        }
        Block block = vault.getLocation().getBlock();
        if (!HouseManager.isVaultContainer(block) || !(block.getState() instanceof Container)) {
            return null;
        }
        return ((Container) block.getState()).getInventory();
    }

    public void openOwnedHouses(Player player) {
        guiManager.openGUI(new OwnedHousesGUI(this, player), player);
    }

    public void openOwnedHouseDetails(Player player, House house) {
        guiManager.openGUI(new OwnedHouseDetailsGUI(this, house, player), player);
    }

    public void openRealEstateAgent(Player player) {
        guiManager.openGUI(new RealEstateAgentGUI(this), player);
    }

    public void openAvailableCategories(Player player) {
        guiManager.openGUI(new com.kodari.housing.inventory.gui.AvailableCategoriesGUI(this), player);
    }

    public void openAuctionSetup(Player player, House house) {
        if (!player.getUniqueId().equals(house.getOwner()) || house.getType() != HouseType.PREMIUM) {
            messages.send(player, "auction-premium-only", java.util.Collections.emptyMap());
            return;
        }
        if (auctionManager.get(house.getName()) != null) {
            messages.send(player, "auction-already-active", java.util.Collections.emptyMap());
            return;
        }
        guiManager.openGUI(new com.kodari.housing.inventory.gui.AuctionSetupGUI(this, house), player);
    }

    public boolean createAuction(Player player, House house, double diamonds, double balance,
                                 boolean balanceAllowed, long durationSeconds) {
        if (!player.getUniqueId().equals(house.getOwner()) || house.getType() != HouseType.PREMIUM
                || auctionManager.get(house.getName()) != null || diamonds <= 0 || balance < 0
                || !Double.isFinite(diamonds) || !Double.isFinite(balance)
                || (!balanceAllowed && balance != 0)) return false;
        return auctionManager.create(new HouseAuction(house.getName(), player.getUniqueId(), diamonds, balance,
                balanceAllowed,
                System.currentTimeMillis() + AUCTION_DURATION_SECONDS * 1000L));
    }

    public void openAuctions(Player player) {
        processAuctions();
        guiManager.openGUI(new com.kodari.housing.inventory.gui.AuctionsGUI(this), player);
    }

    public void openAuctionMenu(Player player) {
        guiManager.openGUI(new com.kodari.housing.inventory.gui.AuctionMenuGUI(this), player);
    }

    public void openOwnedPremiumHouses(Player player) {
        guiManager.openGUI(new com.kodari.housing.inventory.gui.OwnedPremiumHousesGUI(this, player), player);
    }

    public void openAuctionInformation(Player player) {
        guiManager.openGUI(new com.kodari.housing.inventory.gui.AuctionInformationGUI(this), player);
    }

    public void startAuctionInput(Player player, House house, String mode) {
        if (!player.getUniqueId().equals(house.getOwner()) || house.getType() != HouseType.PREMIUM
                || (!"diamonds".equals(mode) && !"both".equals(mode))) {
            messages.send(player, "auction-premium-only", java.util.Collections.emptyMap());
            return;
        }
        if (auctionManager.get(house.getName()) != null) {
            messages.send(player, "auction-already-active", java.util.Collections.emptyMap());
            return;
        }
        pendingAuctions.put(player.getUniqueId(), new PendingAuction(house, mode));
        player.closeInventory();
        messages.send(player, "diamonds".equals(mode) ? "auction-input-diamonds" : "auction-input-both",
                java.util.Collections.emptyMap());
    }

    public void startBidInput(Player player, HouseAuction auction) {
        HouseAuction current = auction == null ? null : auctionManager.get(auction.getHouseName());
        House house = current == null ? null : houseManager.get(current.getHouseName());
        if (current == null || house == null || house.getType() != HouseType.PREMIUM
                || !current.getSeller().equals(house.getOwner())
                || current.getEndAt() <= System.currentTimeMillis()) {
            messages.send(player, "auction-ended", java.util.Collections.emptyMap());
            return;
        }
        pendingBids.put(player.getUniqueId(), current);
        player.closeInventory();
        messages.send(player, current.isBalanceAllowed() ? "bid-input-both" : "bid-input-diamonds",
                java.util.Collections.emptyMap());
    }

    public PendingAuction getPendingAuction(UUID uuid) {
        return pendingAuctions.get(uuid);
    }

    public void clearPendingAuction(UUID uuid) {
        pendingAuctions.remove(uuid);
    }

    public HouseAuction getPendingBid(UUID uuid) {
        return pendingBids.get(uuid);
    }

    public void clearPendingBid(UUID uuid) {
        pendingBids.remove(uuid);
    }

    public void openBid(Player player, HouseAuction auction) {
        processAuctions();
        HouseAuction current = auction == null ? null : auctionManager.get(auction.getHouseName());
        House house = current == null ? null : houseManager.get(current.getHouseName());
        if (current == null || house == null || house.getType() != HouseType.PREMIUM
                || !current.getSeller().equals(house.getOwner())
                || current.getEndAt() <= System.currentTimeMillis()) {
            messages.send(player, "auction-ended", java.util.Collections.emptyMap());
            return;
        }
        guiManager.openGUI(new com.kodari.housing.inventory.gui.BidGUI(this, current), player);
    }

    public void placeBid(Player player, HouseAuction auction, double diamonds, double balance) {
        HouseAuction current = auction == null ? null : auctionManager.get(auction.getHouseName());
        House house = current == null ? null : houseManager.get(current.getHouseName());
        if (current == null || house == null || house.getType() != HouseType.PREMIUM
                || !current.getSeller().equals(house.getOwner())
                || current.getEndAt() <= System.currentTimeMillis()) {
            messages.send(player, "auction-ended", java.util.Collections.emptyMap());
            return;
        }
        if (player.getUniqueId().equals(current.getSeller())) {
            messages.send(player, "auction-owner-bid", java.util.Collections.emptyMap());
            return;
        }
        if (!Double.isFinite(diamonds) || !Double.isFinite(balance) || diamonds <= 0 || balance < 0
                || (!current.isBalanceAllowed() && balance != 0)) {
            messages.send(player, "bid-invalid", java.util.Collections.emptyMap());
            return;
        }
        if (diamonds < current.getCurrentDiamonds() || balance < current.getCurrentBalance()
                || (diamonds == current.getCurrentDiamonds() && balance == current.getCurrentBalance())) {
            messages.send(player, "bid-too-low", values("house", current.getHouseName(),
                    "current-diamonds", formatAmount(current.getCurrentDiamonds()),
                    "current-balance", current.isBalanceAllowed() ? formatAmount(current.getCurrentBalance()) : "Disabled",
                    "remaining", auctionRemaining(current), "status", "ACTIVE"));
            return;
        }
        if (current.isBalanceAllowed() && balance > 0 && !economyManager.available()) {
            messages.send(player, "economy-unavailable", java.util.Collections.emptyMap());
            return;
        }
        if (!currencyService.isAvailable()) {
            messages.send(player, "currency-unavailable", java.util.Collections.emptyMap());
            return;
        }
        CommittedBid committedBid = new CommittedBid(UUID.randomUUID().toString(), player.getUniqueId(),
                diamonds, balance, false, false, "CHARGING");
        current.getCommittedBids().put(committedBid.getId(), committedBid);
        auctionManager.save();
        if (!currencyService.take(player, diamonds)) {
            current.getCommittedBids().remove(committedBid.getId());
            auctionManager.save();
            messages.send(player, "not-enough-currency", values("currency", currencyService.getCurrencyName()));
            return;
        }
        committedBid.setDiamondsHeld(true);
        auctionManager.save();
        if (current.isBalanceAllowed() && balance > 0 && !economyManager.withdraw(player, balance)) {
            committedBid.setStatus("REFUND_PENDING");
            auctionManager.save();
            if (currencyService.give(player, diamonds)) {
                committedBid.setDiamondsHeld(false);
                committedBid.setStatus("REFUNDED");
                auctionManager.save();
            }
            messages.send(player, "not-enough-money", java.util.Collections.emptyMap());
            return;
        }
        committedBid.setBalanceHeld(current.isBalanceAllowed() && balance > 0);
        committedBid.setStatus("HELD");
        current.setCurrentDiamonds(diamonds);
        current.setCurrentBalance(balance);
        current.setHighestBidder(player.getUniqueId());
        auctionManager.save();
        messages.send(player, "bid-placed", values("house", current.getHouseName(),
                "diamonds", formatAmount(diamonds), "balance", formatAmount(balance),
                "bidder", player.getName(), "current-diamonds", formatAmount(current.getCurrentDiamonds()),
                "current-balance", current.isBalanceAllowed() ? formatAmount(current.getCurrentBalance()) : "Disabled",
                "remaining", auctionRemaining(current), "status", "ACTIVE"));
        player.closeInventory();
    }

    private void processAuctions() {
        for (HouseAuction auction : auctionManager.all()) {
            if (auction.getEndAt() > System.currentTimeMillis()) continue;
            completeAuction(auction);
        }
    }

    private void completeAuction(HouseAuction auction) {
        House house = houseManager.get(auction.getHouseName());
        if (house == null) {
            auctionManager.remove(auction);
            return;
        }
        if ("SETTLED".equals(auction.getSettlementState())) {
            auctionManager.remove(auction);
            return;
        }
        if (!"SETTLING".equals(auction.getSettlementState())) {
            auction.setSettlementState("SETTLING");
            auctionManager.save();
        }
        CommittedBid winningBid = findWinningBid(auction);
        if (winningBid == null) {
            for (CommittedBid bid : auction.getCommittedBids().values()) {
                if ("REFUNDED".equals(bid.getStatus())) continue;
                if (!refundCommittedBid(auction, bid)) return;
            }
            Player sellerPlayer = Bukkit.getPlayer(auction.getSeller());
            if (sellerPlayer != null) {
                messages.send(sellerPlayer, "auction-no-bids",
                        values("house", house.getName(), "status", "ENDED"));
            }
            auctionManager.remove(auction);
            return;
        }

        winningBid.setStatus("WINNING");
        auctionManager.save();
        org.bukkit.OfflinePlayer seller = Bukkit.getOfflinePlayer(auction.getSeller());
        if (!auction.isSellerDiamondsPaid()) {
            if (!currencyService.isAvailable() || !currencyService.give(seller, winningBid.getDiamonds())) return;
            winningBid.setDiamondsHeld(false);
            auction.setSellerDiamondsPaid(true);
            auctionManager.save();
        }
        if (winningBid.getBalance() > 0 && !auction.isSellerBalancePaid()) {
            if (!economyManager.available() || !economyManager.deposit(seller, winningBid.getBalance())) return;
            winningBid.setBalanceHeld(false);
            auction.setSellerBalancePaid(true);
            auctionManager.save();
        }
        for (CommittedBid bid : auction.getCommittedBids().values()) {
            if (bid == winningBid || "REFUNDED".equals(bid.getStatus())) continue;
            boolean notify = !"REFUNDED".equals(bid.getStatus());
            if (!refundCommittedBid(auction, bid)) return;
            if (notify) {
                Player loser = Bukkit.getPlayer(bid.getBidder());
                if (loser != null) {
                    messages.send(loser, "bid-refunded", values(
                            "house", house.getName(), "diamonds", formatAmount(bid.getDiamonds()),
                            "balance", formatAmount(bid.getBalance())));
                }
            }
        }
        winningBid.setStatus("SETTLED");
        if (!auction.isOwnershipTransferred() || !winningBid.getBidder().equals(house.getOwner())) {
            if (!transferHouseOwnership(house, winningBid.getBidder())) return;
            auction.setOwnershipTransferred(true);
            auctionManager.save();
        }
        auction.setSettlementState("SETTLED");
        auctionManager.save();
        Player winner = Bukkit.getPlayer(winningBid.getBidder());
        Map<String, String> result = values("house", house.getName(),
                "winner", Bukkit.getOfflinePlayer(winningBid.getBidder()).getName(),
                "diamonds", formatAmount(winningBid.getDiamonds()),
                "balance", formatAmount(winningBid.getBalance()), "status", "COMPLETED");
        if (winner != null) messages.send(winner, "auction-won", result);
        Player sellerPlayer = Bukkit.getPlayer(auction.getSeller());
        if (sellerPlayer != null) messages.send(sellerPlayer, "auction-settled", result);
        auctionManager.remove(auction);
    }

    private CommittedBid findWinningBid(HouseAuction auction) {
        CommittedBid winning = null;
        for (CommittedBid bid : auction.getCommittedBids().values()) {
            if ("WINNING".equals(bid.getStatus())
                    || ("SETTLED".equals(bid.getStatus()) && auction.isOwnershipTransferred())) return bid;
            if (auction.getHighestBidder() != null && auction.getHighestBidder().equals(bid.getBidder())
                    && bid.getDiamonds() == auction.getCurrentDiamonds()
                    && bid.getBalance() == auction.getCurrentBalance()
                    && ("HELD".equals(bid.getStatus()) || "WINNING".equals(bid.getStatus()))) {
                winning = bid;
            }
        }
        return winning;
    }

    private boolean refundCommittedBid(HouseAuction auction, CommittedBid bid) {
        if (bid.isDiamondsHeld()) {
            if (!currencyService.isAvailable() || !currencyService.give(Bukkit.getOfflinePlayer(bid.getBidder()), bid.getDiamonds())) {
                bid.setStatus("REFUND_PENDING");
                auctionManager.save();
                return false;
            }
            bid.setDiamondsHeld(false);
            auctionManager.save();
        }
        if (bid.isBalanceHeld()) {
            if (!economyManager.available() || !economyManager.deposit(Bukkit.getOfflinePlayer(bid.getBidder()), bid.getBalance())) {
                bid.setStatus("REFUND_PENDING");
                auctionManager.save();
                return false;
            }
            bid.setBalanceHeld(false);
            auctionManager.save();
        }
        bid.setStatus("REFUNDED");
        auctionManager.save();
        return true;
    }

    private boolean transferHouseOwnership(House house, UUID newOwner) {
        UUID previousOwner = house.getOwner();
        house.setOwner(newOwner);
        houseManager.save();
        if (!newOwner.equals(house.getOwner())) {
            return false;
        }
        if (previousOwner != null) {
            House occupiedHouse = insidePlayers.get(previousOwner);
            if (occupiedHouse == house || (occupiedHouse != null
                    && house.getName().equals(occupiedHouse.getName()))) {
                insidePlayers.remove(previousOwner);
            }
            transitioning.remove(previousOwner);
            transitionTargets.remove(previousOwner);
            Player previousOwnerPlayer = Bukkit.getPlayer(previousOwner);
            if (previousOwnerPlayer != null) previousOwnerPlayer.closeInventory();
        }
        Player newOwnerPlayer = Bukkit.getPlayer(newOwner);
        if (newOwnerPlayer != null) newOwnerPlayer.closeInventory();
        return true;
    }

    public void openAvailableHouses(Player player, HouseType type) {
        guiManager.openGUI(new AvailableHousesGUI(this, type), player);
    }

    public void openAvailableHouseDetails(Player player, House house) {
        if (house.getOwner() != null) {
            messages.send(player, "already-owned", java.util.Collections.emptyMap());
            player.closeInventory();
            return;
        }
        guiManager.openGUI(new AvailableHouseDetailsGUI(this, house), player);
    }

    public void beginPreviewTeleport(Player player, House house, HouseDoor door) {
        Location target = door.getOutside();
        if (target == null || target.getWorld() == null) {
            messages.send(player, "house-not-ready", java.util.Collections.emptyMap());
            return;
        }
        beginTransition(player, house, target, player.getUniqueId(), getEntryDelaySeconds(player), true, false, true);
    }

    public void beginOwnedHouseTeleport(Player player, House house, HouseDoor door) {
        if (!player.getUniqueId().equals(house.getOwner())) {
            messages.send(player, "not-owner", java.util.Collections.emptyMap());
            return;
        }
        Location target = door.getOutside();
        if (target == null || target.getWorld() == null) {
            messages.send(player, "house-not-ready", java.util.Collections.emptyMap());
            return;
        }
        beginTransition(player, house, target, player.getUniqueId(), getEntryDelaySeconds(player), true, true);
    }

    public void teleportOwnerThroughDoor(Player player, House house, HouseDoor door) {
        if (house == null || door == null || !player.getUniqueId().equals(house.getOwner())) {
            messages.send(player, "not-owner", java.util.Collections.emptyMap());
            return;
        }
        House insideHouse = getHouseFor(player);
        boolean leaving;
        if (isAtDoorSide(player, door, true)) {
            leaving = true;
        } else if (isAtDoorSide(player, door, false)) {
            leaving = false;
        } else {
            leaving = insideHouse != null && house.getName().equalsIgnoreCase(insideHouse.getName());
        }
        beginTransition(player, house, door, leaving);
    }

    public void openSellConfirmation(Player player, House house) {
        guiManager.openGUI(new SellConfirmGUI(this, house), player);
    }

    public void purchaseHouse(Player player, House house) {
        if (house.getOwner() != null) {
            messages.send(player, "already-owned", java.util.Collections.emptyMap());
            player.closeInventory();
            return;
        }
        int limit = getHouseLimit(player);
        if (limit >= 0 && houseManager.countOwned(player.getUniqueId()) >= limit) {
            messages.send(player, "limit-reached", java.util.Collections.emptyMap());
            player.closeInventory();
            return;
        }
        boolean purchased;
        if (house.getType() == HouseType.REGULAR) {
            if (!economyManager.available()) {
                messages.send(player, "economy-unavailable", java.util.Collections.emptyMap());
                player.closeInventory();
                return;
            }
            purchased = economyManager.withdraw(player, house.getPrice());
            if (!purchased) {
                messages.send(player, "not-enough-money", java.util.Collections.emptyMap());
                player.closeInventory();
                return;
            }
        } else if (house.getType() == HouseType.PREMIUM) {
            if (!currencyService.isAvailable()) {
                messages.send(player, "currency-unavailable", java.util.Collections.emptyMap());
                player.closeInventory();
                return;
            }
            purchased = currencyService.take(player, house.getPrice());
            if (!purchased) {
                messages.send(player, "not-enough-currency", values("currency", currencyService.getCurrencyName()));
                player.closeInventory();
                return;
            }
        } else {
            messages.send(player, "luxury-external", java.util.Collections.emptyMap());
            player.closeInventory();
            return;
        }
        house.setOwner(player.getUniqueId());
        houseManager.save();
        messages.send(player, "house-purchased", values("house", house.getName()));
        player.closeInventory();
    }

    public void sellHouse(Player player, House house) {
        if (!player.getUniqueId().equals(house.getOwner())) {
            messages.send(player, "not-owner", java.util.Collections.emptyMap());
            player.closeInventory();
            return;
        }
        if (auctionManager.get(house.getName()) != null) {
            messages.send(player, "auction-already-active", java.util.Collections.emptyMap());
            player.closeInventory();
            return;
        }
        if (house.getType() == HouseType.LUXURY) {
            messages.send(player, "cannot-sell-luxury", java.util.Collections.emptyMap());
            player.closeInventory();
            return;
        }
        double refund = refundAmount(house);
        boolean refunded = house.getType() == HouseType.REGULAR
                ? economyManager.deposit(player, refund)
                : givePremium(player, refund);
        if (!refunded) {
            messages.send(player, "economy-unavailable", java.util.Collections.emptyMap());
            player.closeInventory();
            return;
        }
        house.setOwner(null);
        houseManager.save();
        messages.send(player, "house-sold", values("house", house.getName(), "refund", formatAmount(refund)));
        player.closeInventory();
    }

    private boolean givePremium(Player player, double amount) {
        return currencyService.isAvailable() && currencyService.give(player, amount);
    }

    public void beginTransition(Player player, House house, HouseDoor door, boolean leaving) {
        Location target = leaving ? door.getOutside() : door.getInside();
        if (target == null || target.getWorld() == null) {
            messages.send(player, "house-not-ready", java.util.Collections.emptyMap());
            return;
        }
        UUID uuid = player.getUniqueId();
        messages.send(player, leaving ? "leaving" : "entering", values("house", house.getName()));
        int delay = getEntryDelaySeconds(player);
        beginTransition(player, house, target, uuid, Math.max(0, delay), leaving, false, false);
    }

    private void beginTransition(Player player, House house, Location target, UUID uuid,
                                 int delaySeconds, boolean leaving, boolean ownedHouseTeleport) {
        beginTransition(player, house, target, uuid, delaySeconds, leaving, ownedHouseTeleport, false);
    }

    private void beginTransition(Player player, House house, Location target, UUID uuid,
                                 int delaySeconds, boolean leaving, boolean ownedHouseTeleport, boolean preview) {
        if (transitioning.contains(uuid)) {
            cancelTransition(player);
        }
        transitioning.add(uuid);
        transitionTargets.put(uuid, target.clone());
        if (ownedHouseTeleport || preview) {
            int blindnessTicks = Math.max(0,
                    getConfig().getInt("transition.blindness-duration-seconds", 3)) * 20;
            if (blindnessTicks > 0) {
                XPotion.matchXPotion("BLINDNESS").map(effect -> effect.buildPotionEffect(blindnessTicks, 0))
                        .ifPresent(player::addPotionEffect);
            }
            messages.send(player, preview ? "preview-teleport-start" : "teleport-start",
                    values("house", house.getName()));
            scheduleTransition(player, house, target, uuid, delaySeconds - 1, leaving, ownedHouseTeleport, preview);
        } else if (delaySeconds == 0) {
            finishTransition(player, house, target, uuid, leaving);
        } else {
            int blindnessTicks = getConfig().getInt("transition.blindness-duration-seconds", 3) * 20;
            XPotion.matchXPotion("BLINDNESS").map(effect -> effect.buildPotionEffect(blindnessTicks, 0))
                    .ifPresent(player::addPotionEffect);
            messages.send(player, "teleport-countdown", values("house", house.getName(),
                    "seconds", String.valueOf(delaySeconds)));
            scheduleTransition(player, house, target, uuid, delaySeconds - 1, leaving, false, false);
        }
    }

    private void scheduleTransition(Player player, House house, Location target, UUID uuid,
                                    int remaining, boolean leaving, boolean ownedHouseTeleport, boolean preview) {
        getServer().getScheduler().runTaskLater(this, () -> {
            if (!player.isOnline() || !transitioning.contains(uuid)) {
                transitioning.remove(uuid);
                transitionTargets.remove(uuid);
                return;
            }
            if (remaining <= 0) {
                finishTransition(player, house, target, uuid, leaving, !preview);
                return;
            }
            if ((!ownedHouseTeleport && !preview) || remaining <= 5) {
                messages.send(player, "teleport-countdown", values("house", house.getName(),
                        "seconds", String.valueOf(remaining)));
            }
            scheduleTransition(player, house, target, uuid, remaining - 1, leaving, ownedHouseTeleport, preview);
        }, 20L);
    }

    private void finishTransition(Player player, House house, Location target, UUID uuid, boolean leaving) {
        finishTransition(player, house, target, uuid, leaving, true);
    }

    private void finishTransition(Player player, House house, Location target, UUID uuid,
                                  boolean leaving, boolean updateInsideState) {
        if (player.teleport(target)) {
            if (updateInsideState) {
                if (leaving) insidePlayers.remove(uuid); else insidePlayers.put(uuid, house);
            }
        }
        transitioning.remove(uuid);
        transitionTargets.remove(uuid);
    }

    public void cancelTransition(Player player) {
        UUID uuid = player.getUniqueId();
        transitioning.remove(uuid);
        transitionTargets.remove(uuid);
    }

    private int getEntryDelaySeconds(Player player) {
        if (player.hasPermission("house.timer.instant")) {
            return 0;
        }
        int delay = getConfig().getInt("transition.delay-seconds", 3);
        for (int seconds = 0; seconds <= 1000; seconds++) {
            if (player.hasPermission("house.timer." + seconds)) {
                delay = seconds;
            }
        }
        return Math.max(0, delay);
    }

    public void setDoorLocation(House house, HouseDoor door, Location location, boolean inside) {
        if (inside) door.setInside(location.clone()); else door.setOutside(location.clone());
        houseManager.save();
    }

    public boolean isInside(Player player) {
        return insidePlayers.containsKey(player.getUniqueId());
    }

    public House getHouseFor(Player player) {
        return insidePlayers.get(player.getUniqueId());
    }

    public boolean isTransitioning(Player player) {
        return transitioning.contains(player.getUniqueId());
    }

    public boolean isExpectedTransition(Player player, Location target) {
        if (!transitioning.contains(player.getUniqueId())) {
            return false;
        }
        Location expected = transitionTargets.get(player.getUniqueId());
        if (expected == null || target == null || expected.getWorld() == null || target.getWorld() == null) {
            return false;
        }
        return expected.getWorld().equals(target.getWorld())
                && expected.distanceSquared(target) < 0.01;
    }

    public void clearPlayerState(Player player) {
        UUID uuid = player.getUniqueId();
        transitioning.remove(uuid);
        transitionTargets.remove(uuid);
        insidePlayers.remove(uuid);
    }

    private boolean isAtDoorSide(Player player, HouseDoor door, boolean inside) {
        Location target = inside ? door.getInside() : door.getOutside();
        return target != null && target.getWorld() != null && target.getWorld().equals(player.getWorld())
                && player.getLocation().distanceSquared(target) <= 16.0;
    }

    public boolean isWeaponRestrictionEnabled() {
        return getConfig().getBoolean("restrictions.weapons", true);
    }

    public int getHouseLimit(Player player) {
        int configuredLimit = getConfig().getInt("limits.max-houses-per-player", -1);
        if (configuredLimit < -1) configuredLimit = -1;
        int permissionLimit = -1;
        for (int limit = 0; limit <= 1000; limit++) {
            if (player.hasPermission("houses.ownlimit." + limit)) {
                permissionLimit = limit;
            }
        }
        return permissionLimit >= 0 ? permissionLimit : configuredLimit;
    }

    public boolean isGoldenChestplateRestrictionEnabled() {
        return getConfig().getBoolean("restrictions.golden-chestplate", true);
    }

    public double refundAmount(House house) {
        double refundPercent = getConfig().getDouble("selling.refund-percent", 60.0);
        refundPercent = Math.max(0.0, Math.min(100.0, refundPercent));
        return house.getPrice() * refundPercent / 100.0;
    }

    public String formatAmount(double amount) {
        return String.format("%.2f", amount);
    }

    private String auctionRemaining(HouseAuction auction) {
        long seconds = Math.max(0, (auction.getEndAt() - System.currentTimeMillis()) / 1000L);
        return seconds / 3600 + "h " + (seconds % 3600) / 60 + "m " + seconds % 60 + "s";
    }

    private Map<String, String> values(String... values) {
        Map<String, String> map = new HashMap<>();
        for (int index = 0; index + 1 < values.length; index += 2) map.put(values[index], values[index + 1]);
        return map;
    }

    public HouseManager getHouseManager() { return houseManager; }
    public AuctionManager getAuctionManager() { return auctionManager; }
    public MessageService getMessages() { return messages; }
    public FileConfiguration getGuiConfig() { return guiConfig; }

    public static class PendingAuction {
        private final House house;
        private final String mode;

        public PendingAuction(House house, String mode) {
            this.house = house;
            this.mode = mode;
        }

        public House getHouse() { return house; }
        public String getMode() { return mode; }
    }
}