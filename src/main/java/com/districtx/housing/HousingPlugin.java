package com.districtx.housing;

import com.districtx.housing.inventory.gui.GUIListener;
import com.districtx.housing.inventory.gui.GUIManager;
import com.districtx.housing.inventory.gui.OwnerGUI;
import com.districtx.housing.inventory.gui.OwnedHousesGUI;
import com.districtx.housing.inventory.gui.OwnedHouseDetailsGUI;
import com.districtx.housing.inventory.gui.MyHousesGUI;
import com.districtx.housing.inventory.gui.AvailableHousesGUI;
import com.districtx.housing.inventory.gui.RealEstateAgentGUI;
import com.districtx.housing.inventory.gui.AvailableHouseDetailsGUI;
import com.districtx.housing.inventory.gui.PurchaseGUI;
import com.districtx.housing.inventory.gui.SellConfirmGUI;
import com.districtx.housing.inventory.gui.VaultGUI;
import com.districtx.housing.inventory.gui.VaultsGUI;
import com.districtx.housing.inventory.gui.AuctionListingGUI;
import com.districtx.housing.inventory.gui.AuctionManagementGUI;
import com.districtx.housing.inventory.gui.MyListingsGUI;
import com.districtx.housing.inventory.gui.BuyoutConfirmGUI;
import com.districtx.housing.api.PacificaHousingAPI;
import com.districtx.housing.api.PacificaHousingService;
import com.districtx.housing.api.auction.AuctionCancellationResult;
import com.districtx.housing.model.House;
import com.districtx.housing.model.HouseAuction;
import com.districtx.housing.model.CommittedBid;
import com.districtx.housing.model.AuctionCreationSession;
import com.districtx.housing.model.HouseDoor;
import com.districtx.housing.model.HouseType;
import com.districtx.housing.model.HouseVault;
import com.districtx.housing.util.MessageService;
import com.districtx.housing.integration.OptionalDependencyManager;
import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XPotion;
import com.cryptomorin.xseries.XSound;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Pattern;
import java.util.Locale;

public class HousingPlugin extends JavaPlugin {
    private static final long AUCTION_DURATION_SECONDS = 12L * 60L * 60L;
    private static final int TAXI_DELAY_SECONDS = 10;
    private HouseManager houseManager;
    private AuctionManager auctionManager;
    private CurrencyServiceBridge currencyService;
    private EconomyManager economyManager;
    private GUIManager guiManager;
    private MessageService messages;
    private RankMultiplierService rankMultiplierService;
    private FileConfiguration guiConfig;
    private PacificaHousingAPI housingApi;
    private OptionalDependencyManager optionalDependencyManager;
    private final Set<UUID> transitioning = new HashSet<>();
    private final Map<UUID, House> insidePlayers = new HashMap<>();
    private final Map<UUID, Location> transitionTargets = new HashMap<>();
    private final Map<UUID, Long> transitionTokens = new HashMap<>();
    private final Map<UUID, BukkitTask> transitionTasks = new HashMap<>();
    private final Map<String, Inventory> vaultInventories = new HashMap<>();
    private long nextTransitionToken;

    @Override
    public void onEnable() {
        migrateDataFolder();
        saveDefaultConfig();
        saveResource("messages.yml", false);
        saveResource("INVENTORY GUI.yml", false);
        messages = new MessageService(YamlConfiguration.loadConfiguration(new File(getDataFolder(), "messages.yml")));
        guiConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "INVENTORY GUI.yml"));
        houseManager = new HouseManager(this);
        auctionManager = new AuctionManager(this);
        currencyService = new CurrencyServiceBridge(this);
        economyManager = new EconomyManager(this);
        rankMultiplierService = new RankMultiplierService(this);
        optionalDependencyManager = new OptionalDependencyManager(this);
        optionalDependencyManager.initializeIntegrations();
        guiManager = new GUIManager();
        getServer().getPluginManager().registerEvents(new GUIListener(guiManager), this);
        getServer().getPluginManager().registerEvents(new HouseListener(this), this);
        HousesCommand command = new HousesCommand(this);
        getCommand("houses").setExecutor(command);
        getCommand("houses").setTabCompleter(command);
        getCommand("myhouses").setExecutor(new MyHousesCommand(this));
        HousesAdminCommand adminCommand = new HousesAdminCommand(this);
        getCommand("housesadmin").setExecutor(adminCommand);
        getCommand("housesadmin").setTabCompleter(adminCommand);
        getServer().getScheduler().runTaskTimer(this, this::processAuctions, 20L, 20L);
        housingApi = new PacificaHousingService(this);
        getServer().getServicesManager().register(PacificaHousingAPI.class, housingApi, this,
                ServicePriority.Normal);
    }

    @Override
    public void onDisable() {
        if (housingApi != null) {
            getServer().getServicesManager().unregister(PacificaHousingAPI.class, housingApi);
            housingApi = null;
        }
        saveVaultInventories();
        if (houseManager != null) houseManager.save();
        if (auctionManager != null) auctionManager.save();
    }

    public void reloadConfiguration() {
        saveVaultInventories();
        vaultInventories.clear();
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
        if (house == null || house.getOwner() == null || !house.getOwner().equals(player.getUniqueId())) {
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
        if (vault == null) {
            return null;
        }
        Inventory inventory = vaultInventories.get(vault.getId());
        if (inventory == null) {
            inventory = Bukkit.createInventory(null, HouseVault.INVENTORY_SIZE, "Vault");
            inventory.setContents(Arrays.copyOf(vault.getContents(), HouseVault.INVENTORY_SIZE));
            vaultInventories.put(vault.getId(), inventory);
            syncPhysicalVault(vault, inventory);
        }
        return inventory;
    }

    public void saveVaultInventory(HouseVault vault) {
        if (vault == null) {
            return;
        }
        Inventory inventory = vaultInventories.get(vault.getId());
        if (inventory == null) {
            return;
        }
        vault.setContents(Arrays.copyOf(inventory.getContents(), HouseVault.INVENTORY_SIZE));
        syncPhysicalVault(vault, inventory);
        houseManager.save();
    }

    private void saveVaultInventories() {
        if (houseManager == null) {
            return;
        }
        for (House house : houseManager.all()) {
            for (HouseVault vault : house.getVaults()) {
                saveVaultInventory(vault);
            }
        }
    }

    private void syncPhysicalVault(HouseVault vault, Inventory source) {
        if (vault.getLocation() == null || vault.getLocation().getWorld() == null) {
            return;
        }
        Block block = vault.getLocation().getBlock();
        if (!HouseManager.isVaultContainer(block) || !(block.getState() instanceof Container)) {
            return;
        }
        Inventory target = ((Container) block.getState()).getInventory();
        target.clear();
        for (int slot = 0; slot < Math.min(source.getSize(), target.getSize()); slot++) {
            target.setItem(slot, source.getItem(slot));
        }
    }

    public void openOwnedHouses(Player player) {
        guiManager.openGUI(new OwnedHousesGUI(this, player), player);
    }

    public void openMyHouses(Player player) {
        guiManager.openGUI(new MyHousesGUI(this, player), player);
    }

    public void openOwnedHouseDetails(Player player, House house) {
        guiManager.openGUI(new OwnedHouseDetailsGUI(this, house, player), player);
    }

    public void openRealEstateAgent(Player player) {
        guiManager.openGUI(new RealEstateAgentGUI(this), player);
    }

    public void openAvailableCategories(Player player) {
        guiManager.openGUI(new com.districtx.housing.inventory.gui.AvailableCategoriesGUI(this), player);
    }

    public void openAuctionInput(Player player, House house) {
        if (!player.getUniqueId().equals(house.getOwner()) || house.getType() != HouseType.PREMIUM) {
            messages.send(player, "auction-premium-only", java.util.Collections.emptyMap());
            return;
        }
        if (auctionManager.get(house.getName()) != null) {
            messages.send(player, "auction-already-active", java.util.Collections.emptyMap());
            return;
        }
        guiManager.openGUI(new AuctionListingGUI(this, player, house, null, null), player);
    }

    public synchronized boolean createAuction(Player player, House house, double balance, long durationSeconds) {
        return false;
    }

    public void openListingMinimumInput(Player player, AuctionListingGUI listing) {
        if (!validListingOwner(player, listing)) return;
        ItemStack paper = XMaterial.matchXMaterial("DIAMOND").map(XMaterial::parseItem).orElse(null);
        new AnvilGUI.Builder().itemLeft(paper).text(" ")
                .title(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&aEnter Minimum Bid")).plugin(this)
                .onClick((slot, snapshot) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) return Collections.emptyList();
                    Long value = parseWholeAmount(snapshot.getText());
                    double minimum = listing.getHouse().getPrice() * 0.50;
                    double maximum = listing.getHouse().getPrice() * 0.90;
                    if (value == null || value < Math.ceil(minimum) || value > Math.floor(maximum)) {
                        messages.send(player, "auction-minimum-invalid", values("minimum", formatAmount(minimum),
                                "maximum", formatAmount(maximum)));
                        return Collections.emptyList();
                    }
                    if (!currencyService.isAvailable()) {
                        messages.send(player, "currency-unavailable", Collections.emptyMap());
                        return Collections.emptyList();
                    }
                    listing.getSession().setMinimumDiamondBid(BigDecimal.valueOf(value));
                    listing.getSession().setDiamondBidConfigured(true);
                    return Arrays.asList(AnvilGUI.ResponseAction.close(),
                            AnvilGUI.ResponseAction.run(() -> guiManager.openGUI(new AuctionListingGUI(this, player,
                                    listing.getHouse(), listing.getSession()), player)));
                }).open(player);
    }

    public void openListingFeeInput(Player player, AuctionListingGUI listing) {
        if (!validListingOwner(player, listing)) return;
        ItemStack paper = XMaterial.matchXMaterial("PAPER").map(XMaterial::parseItem).orElse(null);
        new AnvilGUI.Builder().itemLeft(paper).text(" ")
                .title(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&6Enter Starting Bid"))
                .plugin(this)
                .onClick((slot, snapshot) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) return Collections.emptyList();
                    BigDecimal startingBid = parsePositiveDecimal(snapshot.getText());
                    BigDecimal maximum = decimalConfig("listing-fee.max-starting-bid", new BigDecimal("1000000000000"));
                    if (startingBid == null || startingBid.compareTo(maximum) > 0) {
                        messages.send(player, "listing-fee-invalid", values("fee", formatBalance(maximum.doubleValue())));
                        return Collections.emptyList();
                    }
                    listing.getSession().setStartingBid(startingBid);
                    listing.getSession().setStartingBidConfigured(true);
                    listing.getSession().setListingFee(calculateListingFeeAmount(player, startingBid));
                    listing.getSession().setListingFeeConfigured(true);
                    return Arrays.asList(AnvilGUI.ResponseAction.close(),
                            AnvilGUI.ResponseAction.run(() -> guiManager.openGUI(new AuctionListingGUI(this, player,
                                    listing.getHouse(), listing.getSession()), player)));
                }).open(player);
    }

    public void openListingBuyoutInput(Player player, AuctionListingGUI listing) {
        if (!validListingOwner(player, listing)) return;
        ItemStack paper = XMaterial.matchXMaterial("PAPER").map(XMaterial::parseItem).orElse(null);
        new AnvilGUI.Builder().itemLeft(paper).text(" ")
                .title("Set a Buyout Price").plugin(this)
                .onClick((slot, snapshot) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) return Collections.emptyList();
                    String input = extractBidAmount(snapshot.getText());
                    if ("0".equals(input)) {
                        listing.getSession().setBuyoutPrice(null);
                        listing.getSession().setBuyoutConfigured(false);
                        return Arrays.asList(AnvilGUI.ResponseAction.close(),
                                AnvilGUI.ResponseAction.run(() -> guiManager.openGUI(new AuctionListingGUI(this,
                                        player, listing.getHouse(), listing.getSession()), player)));
                    }
                    BigDecimal value = parsePositiveDecimal(input);
                    if (value == null) {
                        messages.send(player, "auction-buyout-invalid", Collections.emptyMap());
                        return Collections.emptyList();
                    }
                    listing.getSession().setBuyoutPrice(value);
                    listing.getSession().setBuyoutConfigured(true);
                    return Arrays.asList(AnvilGUI.ResponseAction.close(),
                            AnvilGUI.ResponseAction.run(() -> guiManager.openGUI(new AuctionListingGUI(this, player,
                                    listing.getHouse(), listing.getSession()), player)));
                }).open(player);
    }

    private boolean validListingOwner(Player player, AuctionListingGUI listing) {
        House house = listing == null ? null : listing.getHouse();
        return player != null && listing != null && house != null
                && player.getUniqueId().equals(house.getOwner())
                && player.getUniqueId().equals(listing.getSession().getOwner())
                && house.getName().equals(listing.getSession().getHouseName())
                && house.getType() == HouseType.PREMIUM
                && auctionManager.get(house.getName()) == null;
    }

    public synchronized void publishAuction(Player player, AuctionListingGUI listing) {
        if (!validListingOwner(player, listing) || !listing.getSession().isDiamondBidConfigured()
                || listing.getSession().getMinimumDiamondBid() == null || !currencyService.isAvailable()) {
            messages.send(player, "auction-invalid", Collections.emptyMap());
            return;
        }
        if (!listing.getSession().isListingFeeConfigured()
                || !listing.getSession().isStartingBidConfigured()
                || listing.getSession().getStartingBid() == null || listing.getSession().getListingFee() == null) {
            messages.send(player, "listing-fee-required", Collections.emptyMap());
            return;
        }
        House house = listing.getHouse();
        double minimum = listing.getSession().getMinimumDiamondBid().doubleValue();
        double minimumAllowed = house.getPrice() * 0.50;
        double maximumAllowed = house.getPrice() * 0.90;
        if (minimum < Math.ceil(minimumAllowed) || minimum > Math.floor(maximumAllowed)) {
            messages.send(player, "auction-minimum-invalid", values("minimum", formatAmount(minimumAllowed),
                    "maximum", formatAmount(maximumAllowed)));
            return;
        }
        if (listing.getSession().isBuyoutConfigured()
                && (listing.getSession().getBuyoutPrice() == null
                || listing.getSession().getBuyoutPrice().signum() <= 0
                || !Double.isFinite(listing.getSession().getBuyoutPrice().doubleValue()))) {
            messages.send(player, "auction-buyout-invalid", Collections.emptyMap());
            return;
        }
        BigDecimal minimumAmount = listing.getSession().getMinimumDiamondBid();
        BigDecimal startingBid = listing.getSession().getStartingBid();
        BigDecimal calculatedFeeAmount = calculateListingFeeAmount(player, startingBid);
        if (calculatedFeeAmount == null || calculatedFeeAmount.signum() < 0) {
            messages.send(player, "listing-fee-insufficient", Collections.emptyMap());
            return;
        }
        listing.getSession().setListingFee(calculatedFeeAmount);
        listing.getSession().setListingFeeConfigured(true);
        double feePercentage = getListingFeeMultiplier(player).doubleValue();
        double fee = calculatedFeeAmount.doubleValue();
        AuctionCreationSession session = listing.getSession();
        if (session.isCreatingAuction()) return;
        session.setCreatingAuction(true);
        try {
            if (!Double.isFinite(fee) || !economyManager.available()
                    || economyManager.balance(player) < fee || !economyManager.withdraw(player, fee)) {
                messages.send(player, "listing-fee-insufficient", values("fee", formatBalance(fee)));
                return;
            }
            HouseAuction auction = new HouseAuction(house.getName(), player.getUniqueId(), minimum,
                    startingBid.doubleValue(), true,
                    System.currentTimeMillis() + AUCTION_DURATION_SECONDS * 1000L);
            auction.setListingFee(fee);
            auction.setListingFeePercentage(feePercentage);
            auction.setSellerRank(rankMultiplierService.getPrimaryGroup(player));
            auction.setListingFeePaid(true);
            if (session.isBuyoutConfigured()) {
                auction.setBuyoutEnabled(true);
                auction.setBuyoutPrice(session.getBuyoutPrice().doubleValue());
            }
            try {
                if (!auctionManager.create(auction)) {
                    economyManager.deposit(player, fee);
                    messages.send(player, "auction-invalid", Collections.emptyMap());
                    return;
                }
            } catch (RuntimeException exception) {
                economyManager.deposit(player, fee);
                messages.send(player, "auction-invalid", Collections.emptyMap());
                return;
            }
            refreshOpenMyListings();
            messages.send(player, "auction-created", Collections.singletonMap("house", house.getName()));
            player.closeInventory();
        } finally {
            session.setCreatingAuction(false);
        }
    }

    public BigDecimal getListingFeeMultiplier(Player player) {
        return rankMultiplierService.getMultiplier(player);
    }

    public double calculateListingFee(Player player, double minimumDiamonds) {
        BigDecimal fee = calculateListingFeeAmount(player, BigDecimal.valueOf(minimumDiamonds));
        return fee == null ? Double.NaN : fee.doubleValue();
    }

    private BigDecimal calculateListingFeeAmount(Player player, BigDecimal minimumBid) {
        if (minimumBid == null || minimumBid.signum() <= 0) return null;
        return minimumBid.multiply(getListingFeeMultiplier(player)).setScale(2, RoundingMode.HALF_UP);
    }

    public String formatBalance(double amount) {
        return "$" + formatAmount(amount);
    }

    private Long parseWholeAmount(String raw) {
        if (raw == null || !Pattern.matches("[0-9]+", raw.trim())) return null;
        try {
            return Long.valueOf(raw.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private BigDecimal parsePositiveDecimal(String raw) {
        if (raw == null || !Pattern.matches("[0-9]+(?:\\.[0-9]+)?", raw.trim())) return null;
        try {
            BigDecimal value = new BigDecimal(raw.trim());
            return value.signum() > 0 ? value : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private BigDecimal decimalConfig(String path, BigDecimal fallback) {
        try {
            BigDecimal value = new BigDecimal(getConfig().getString(path, fallback.toPlainString()));
            return value.signum() > 0 ? value : fallback;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public void openAuctions(Player player) {
        processAuctions();
        guiManager.openGUI(new com.districtx.housing.inventory.gui.AuctionsGUI(this), player);
    }

    public void openAuctionMenu(Player player) {
        guiManager.openGUI(new com.districtx.housing.inventory.gui.AuctionMenuGUI(this), player);
    }

    public void openMyListings(Player player) {
        guiManager.openGUI(new MyListingsGUI(this), player);
    }

    public void refreshOpenMyListings() {
        if (guiManager == null) {
            return;
        }
        if (Bukkit.isPrimaryThread()) {
            guiManager.refreshOpenMyListings();
        } else {
            getServer().getScheduler().runTask(this, guiManager::refreshOpenMyListings);
        }
    }

    public void openAuctionManagement(Player player, String auctionId) {
        HouseAuction auction = auctionManager.getActiveListing(auctionId, player.getUniqueId());
        if (auction == null) {
            player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                    "&cThis auction listing is no longer active."));
            return;
        }
        guiManager.openGUI(new AuctionManagementGUI(this, player.getUniqueId(), auction.getAuctionId()), player);
    }

    public boolean cancelAuction(Player player, String auctionId) {
        if (player == null) {
            return false;
        }
        AuctionCancellationResult result = housingApi.auctions().cancelAuction(auctionId, player.getUniqueId());
        if (result != AuctionCancellationResult.SUCCESS) {
            String message = result == AuctionCancellationResult.HAS_BIDS
                    ? "&cThis auction cannot be cancelled. Active bids must be resolved first."
                    : result == AuctionCancellationResult.TRANSACTION_ERROR
                    ? "&cThe auction could not be cancelled because the change could not be saved."
                    : "&cThis auction is no longer available for cancellation.";
            player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', message));
            return false;
        }
        player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&aAuction cancelled."));
        guiManager.openPrevious(player);
        return true;
    }

    public void openOwnedPremiumHouses(Player player) {
        guiManager.openGUI(new com.districtx.housing.inventory.gui.OwnedPremiumHousesGUI(this, player), player);
    }

    public void openAuctionInformation(Player player) {
        guiManager.openGUI(new com.districtx.housing.inventory.gui.AuctionInformationGUI(this), player);
    }

    public void openBidInput(Player player, HouseAuction auction) {
        HouseAuction current = auction == null ? null : auctionManager.get(auction.getHouseName());
        House house = current == null ? null : houseManager.get(current.getHouseName());
        if (current == null || house == null || house.getType() != HouseType.PREMIUM
                || !current.getSeller().equals(house.getOwner())
                || current.getEndAt() <= System.currentTimeMillis()
                || !"ACTIVE".equals(current.getSettlementState())) {
            messages.send(player, "auction-ended", java.util.Collections.emptyMap());
            return;
        }
        openBidAmountGui(player, current);
    }

    public void openBidAmountGui(Player player, HouseAuction auction) {
        openAuctionAmountGui(player, null, auction);
    }

    private void openAuctionAmountGui(Player player, House house, HouseAuction auction) {
        ItemStack paper = XMaterial.matchXMaterial("PAPER").map(XMaterial::parseItem).orElse(null);
        new AnvilGUI.Builder()
                .onClick((slot, stateSnapshot) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) {
                        return Collections.emptyList();
                    }
                    return processAuctionInput(player, house, auction, stateSnapshot.getText());
                })
                .itemLeft(paper)
                .text(" ")
                .title(auction == null ? "Enter Auction Amount" : "House Auction Bid")
                .plugin(this)
                .open(player);
    }

    private java.util.List<AnvilGUI.ResponseAction> processAuctionInput(Player player, House house,
                                                                          HouseAuction auction, String inputText) {
        String amountText = extractBidAmount(inputText);
        if (amountText == null) {
            messages.send(player, auction == null ? "auction-input-invalid" : "bid-input-invalid",
                    Collections.emptyMap());
            return Collections.emptyList();
        }

        Double amount = parsePositiveAmount(amountText);
        if (amount == null) {
            messages.send(player, auction == null ? "auction-input-invalid" : "bid-input-invalid",
                    Collections.emptyMap());
            return Collections.emptyList();
        }

        if (auction == null) {
            if (!createAuction(player, house, amount, 0L)) {
                messages.send(player, "auction-invalid", Collections.emptyMap());
                return Collections.emptyList();
            }
            messages.send(player, "auction-created", Collections.singletonMap("house", house.getName()));
            return Arrays.asList(
                    AnvilGUI.ResponseAction.close(),
                    AnvilGUI.ResponseAction.run(() -> openAuctionMenu(player)));
        }

        if (!placeBid(player, auction, amount)) {
            return Collections.emptyList();
        }
        return Arrays.asList(
                AnvilGUI.ResponseAction.close(),
                AnvilGUI.ResponseAction.run(() -> openBid(player, auction)));
    }

    private String extractBidAmount(String inputText) {
        String input = inputText == null ? "" : inputText.trim();
        return Pattern.matches("[0-9]+", input) ? input : null;
    }

    private Double parsePositiveAmount(String raw) {
        if (raw == null) return null;
        try {
            BigDecimal decimal = new BigDecimal(raw.trim());
            double amount = decimal.doubleValue();
            return decimal.signum() > 0 && Double.isFinite(amount) ? amount : null;
        } catch (NumberFormatException | ArithmeticException ignored) {
            return null;
        }
    }

    public void openBid(Player player, HouseAuction auction) {
        processAuctions();
        HouseAuction current = auction == null ? null : auctionManager.get(auction.getHouseName());
        House house = current == null ? null : houseManager.get(current.getHouseName());
        if (current == null || house == null || house.getType() != HouseType.PREMIUM
                || !current.getSeller().equals(house.getOwner())
                || current.getEndAt() <= System.currentTimeMillis()
                || !"ACTIVE".equals(current.getSettlementState())) {
            messages.send(player, "auction-ended", java.util.Collections.emptyMap());
            return;
        }
        guiManager.openGUI(new com.districtx.housing.inventory.gui.BidGUI(this, current), player);
    }

    public synchronized boolean placeBid(Player player, HouseAuction auction, double balance) {
        HouseAuction current = auction == null ? null : auctionManager.get(auction.getHouseName());
        House house = current == null ? null : houseManager.get(current.getHouseName());
        double diamonds = current == null ? 0 : current.getStartingDiamonds();
        if (current == null || house == null || house.getType() != HouseType.PREMIUM
                || !current.getSeller().equals(house.getOwner())
                || current.getEndAt() <= System.currentTimeMillis()
                || !"ACTIVE".equals(current.getSettlementState())) {
            messages.send(player, "auction-ended", java.util.Collections.emptyMap());
            return false;
        }
        if (player.getUniqueId().equals(current.getSeller())) {
            messages.send(player, "auction-owner-bid", java.util.Collections.emptyMap());
            return false;
        }
        if (!Double.isFinite(diamonds) || !Double.isFinite(balance) || diamonds <= 0 || balance <= 0) {
            messages.send(player, "bid-invalid", java.util.Collections.emptyMap());
            return false;
        }
        if (balance <= current.getCurrentBalance()) {
            messages.send(player, "bid-too-low", values("house", current.getHouseName(),
                    "current-diamonds", formatAmount(current.getCurrentDiamonds()),
                    "current-balance", formatAmount(current.getCurrentBalance()),
                    "remaining", auctionRemaining(current), "status", "ACTIVE"));
            return false;
        }
        if (!currencyService.isAvailable()) {
            messages.send(player, "currency-unavailable", java.util.Collections.emptyMap());
            return false;
        }
        if (balance > 0) {
            if (!economyManager.available()) {
                messages.send(player, "economy-unavailable", java.util.Collections.emptyMap());
                return false;
            }
            if (economyManager.balance(player) < balance) {
                messages.send(player, "not-enough-money", java.util.Collections.emptyMap());
                return false;
            }
        }
        CommittedBid committedBid = new CommittedBid(UUID.randomUUID().toString(), player.getUniqueId(),
                diamonds, balance, false, false, "CHARGING");
        double previousDiamonds = current.getCurrentDiamonds();
        double previousBalance = current.getCurrentBalance();
        UUID previousBidder = current.getHighestBidder();
        boolean committed = currencyService.process(player, diamonds, balance, () -> {
            try {
                committedBid.setDiamondsHeld(true);
                committedBid.setBalanceHeld(balance > 0);
                committedBid.setStatus("HELD");
                current.getCommittedBids().put(committedBid.getId(), committedBid);
                current.setCurrentDiamonds(diamonds);
                current.setCurrentBalance(balance);
                current.setHighestBidder(player.getUniqueId());
                if (!auctionManager.save()) {
                    throw new IllegalStateException("Could not save the auction bid.");
                }
                return true;
            } catch (RuntimeException exception) {
                current.getCommittedBids().remove(committedBid.getId());
                current.setCurrentDiamonds(previousDiamonds);
                current.setCurrentBalance(previousBalance);
                current.setHighestBidder(previousBidder);
                return false;
            }
        });
        if (!committed) {
            if (!economyManager.available() || economyManager.balance(player) < balance) {
                messages.send(player, "not-enough-money", Collections.emptyMap());
            } else {
                messages.send(player, "not-enough-currency", values("currency", currencyService.getCurrencyName()));
            }
            return false;
        }
        messages.send(player, "bid-placed", values("house", current.getHouseName(),
                "diamonds", formatAmount(diamonds), "balance", formatAmount(balance),
                "bidder", player.getName(), "current-diamonds", formatAmount(current.getCurrentDiamonds()),
                "current-balance", formatAmount(current.getCurrentBalance()),
                "remaining", auctionRemaining(current), "status", "ACTIVE"));
        refreshOpenMyListings();
        player.closeInventory();
        return true;
    }

    public void openBuyoutConfirmation(Player player, HouseAuction auction) {
        HouseAuction current = auction == null ? null : auctionManager.get(auction.getHouseName());
        if (current == null || !current.isBuyoutEnabled() || current.getBuyoutPrice() <= 0
                || current.getEndAt() <= System.currentTimeMillis()
                || !"ACTIVE".equals(current.getSettlementState())) {
            messages.send(player, "buyout-unavailable", Collections.emptyMap());
            return;
        }
        guiManager.openGUI(new BuyoutConfirmGUI(this, current), player);
    }

    public synchronized boolean buyoutAuction(Player player, HouseAuction requested) {
        HouseAuction auction = requested == null ? null : auctionManager.get(requested.getHouseName());
        House house = auction == null ? null : houseManager.get(auction.getHouseName());
        if (auction == null || house == null || !auction.isBuyoutEnabled() || auction.getBuyoutPrice() <= 0
                || auction.getEndAt() <= System.currentTimeMillis() || !"ACTIVE".equals(auction.getSettlementState())
                || !auction.getSeller().equals(house.getOwner())
                || player.getUniqueId().equals(auction.getSeller())) {
            messages.send(player, "buyout-unavailable", Collections.emptyMap());
            return false;
        }
        double price = auction.getBuyoutPrice();
        if (!economyManager.available() || economyManager.balance(player) < price
                || !economyManager.withdraw(player, price)) {
            messages.send(player, "not-enough-money", Collections.emptyMap());
            return false;
        }
        auction.setSettlementState("SETTLING");
        if (!auctionManager.save()) {
            economyManager.deposit(player, price);
            auction.setSettlementState("ACTIVE");
            messages.send(player, "buyout-failed", Collections.emptyMap());
            return false;
        }
        for (CommittedBid bid : auction.getCommittedBids().values()) {
            if (!"REFUNDED".equals(bid.getStatus()) && !refundCommittedBid(auction, bid)) {
                economyManager.deposit(player, price);
                auction.setSettlementState("ACTIVE");
                auctionManager.save();
                messages.send(player, "buyout-failed", Collections.emptyMap());
                return false;
            }
        }
        org.bukkit.OfflinePlayer seller = Bukkit.getOfflinePlayer(auction.getSeller());
        if (!transferHouseOwnership(house, player.getUniqueId())) {
            economyManager.deposit(player, price);
            auction.setSettlementState("ACTIVE");
            auctionManager.save();
            messages.send(player, "buyout-failed", Collections.emptyMap());
            return false;
        }
        if (!economyManager.deposit(seller, price)) {
            transferHouseOwnership(house, auction.getSeller());
            economyManager.deposit(player, price);
            auction.setSettlementState("ACTIVE");
            auctionManager.save();
            messages.send(player, "buyout-failed", Collections.emptyMap());
            return false;
        }
        auction.setOwnershipTransferred(true);
        auction.setSettlementState("SETTLED");
        auctionManager.save();
        auctionManager.remove(auction);
        refreshOpenMyListings();
        messages.send(player, "buyout-complete", values("house", house.getName(), "buyout", formatBalance(price)));
        return true;
    }

    private synchronized void processAuctions() {
        for (HouseAuction auction : auctionManager.all()) {
            if (auction.getEndAt() > System.currentTimeMillis()) continue;
            completeAuction(auction);
        }
    }

    private void completeAuction(HouseAuction auction) {
        House house = houseManager.get(auction.getHouseName());
        if (house == null) {
            auctionManager.remove(auction);
            refreshOpenMyListings();
            return;
        }
        if ("SETTLED".equals(auction.getSettlementState())) {
            auctionManager.remove(auction);
            refreshOpenMyListings();
            return;
        }
        if (!"SETTLING".equals(auction.getSettlementState())) {
            auction.setSettlementState("SETTLING");
            auctionManager.save();
            refreshOpenMyListings();
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
            refreshOpenMyListings();
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
        if (!auction.isOwnershipTransferred() || !winningBid.getBidder().equals(house.getOwner())) {
            if (!transferHouseOwnership(house, winningBid.getBidder())) return;
            auction.setOwnershipTransferred(true);
            auctionManager.save();
        }
        winningBid.setStatus("SETTLED");
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
        refreshOpenMyListings();
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
        if (!houseManager.save() || !newOwner.equals(house.getOwner())) {
            house.setOwner(previousOwner);
            houseManager.save();
            return false;
        }
        if (previousOwner != null) {
            House occupiedHouse = insidePlayers.get(previousOwner);
            if (occupiedHouse == house || (occupiedHouse != null
                    && house.getName().equals(occupiedHouse.getName()))) {
                insidePlayers.remove(previousOwner);
            }
            Player previousOwnerPlayer = Bukkit.getPlayer(previousOwner);
            if (previousOwnerPlayer != null) {
                cancelTransition(previousOwnerPlayer);
                previousOwnerPlayer.closeInventory();
            } else {
                transitionTokens.remove(previousOwner);
                BukkitTask task = transitionTasks.remove(previousOwner);
                if (task != null) task.cancel();
                transitioning.remove(previousOwner);
                transitionTargets.remove(previousOwner);
            }
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
        if (house == null || door == null || !player.getUniqueId().equals(house.getOwner())) {
            messages.send(player, "not-owner", java.util.Collections.emptyMap());
            return;
        }
        if (isTransitioning(player)) {
            return;
        }
        Location target = door.getOutside();
        if (target == null || target.getWorld() == null) {
            messages.send(player, "house-not-ready", java.util.Collections.emptyMap());
            return;
        }
        beginTaxiTeleport(player, house, target);
    }

    public void teleportOwnerThroughDoor(Player player, House house, HouseDoor door) {
        if (house == null || door == null || !player.getUniqueId().equals(house.getOwner())) {
            messages.send(player, "not-owner", java.util.Collections.emptyMap());
            return;
        }
        House insideHouse = getHouseFor(player);
        boolean leaving;
        boolean atInside = isAtDoorSide(player, door, true);
        boolean atOutside = isAtDoorSide(player, door, false);
        if (atInside && atOutside) {
            leaving = distanceToDoorSide(player, door, true) < distanceToDoorSide(player, door, false);
        } else if (atInside) {
            leaving = true;
        } else if (atOutside) {
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
            UUID previousOwner = house.getOwner();
            purchased = currencyService.process(player, house.getPrice(), () -> {
                house.setOwner(player.getUniqueId());
                boolean saved = false;
                try {
                    saved = houseManager.save();
                    return saved;
                } finally {
                    if (!saved) house.setOwner(previousOwner);
                }
            });
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

    private void beginTaxiTeleport(Player player, House house, Location target) {
        UUID uuid = player.getUniqueId();
        long token = ++nextTransitionToken;
        transitioning.add(uuid);
        transitionTargets.put(uuid, target.clone());
        transitionTokens.put(uuid, token);
        player.closeInventory();
        messages.send(player, "taxi-called", java.util.Collections.emptyMap());
        sendTaxiCountdown(player, TAXI_DELAY_SECONDS);
        scheduleTaxiTransition(player, house, target, uuid, token, TAXI_DELAY_SECONDS - 1);
    }

    private void scheduleTaxiTransition(Player player, House house, Location target, UUID uuid,
                                         long token, int remaining) {
        BukkitTask task = getServer().getScheduler().runTaskLater(this, () -> {
            if (!player.isOnline() || !isCurrentTransition(uuid, token)) {
                return;
            }
            transitionTasks.remove(uuid);
            if (remaining <= 0) {
                int effectTicks = 2 * 20;
                XPotion.matchXPotion("BLINDNESS")
                        .map(effect -> effect.buildPotionEffect(effectTicks, 0))
                        .ifPresent(player::addPotionEffect);
                XPotion.matchXPotion("INVISIBILITY")
                        .map(effect -> effect.buildPotionEffect(effectTicks, 0))
                        .ifPresent(player::addPotionEffect);
                boolean teleported = player.teleport(target);
                if (teleported) {
                    refreshPlayerHouseStatus(player, target, true);
                }
                clearTransition(uuid);
                if (teleported) {
                    messages.send(player, "taxi-dropped", java.util.Collections.emptyMap());
                }
                return;
            }
            if (remaining <= 5) {
                sendTaxiCountdown(player, remaining);
            }
            scheduleTaxiTransition(player, house, target, uuid, token, remaining - 1);
        }, 20L);
        transitionTasks.put(uuid, task);
    }

    private void sendTaxiCountdown(Player player, int seconds) {
        messages.send(player, "taxi-arriving", values("seconds", String.valueOf(seconds)));
        XSound.matchXSound("UI_BUTTON_CLICK").ifPresent(sound -> sound.play(player));
    }

    private void clearTransition(UUID uuid) {
        transitioning.remove(uuid);
        transitionTargets.remove(uuid);
        transitionTokens.remove(uuid);
        transitionTasks.remove(uuid);
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
        cancelTransition(player);
        long token = ++nextTransitionToken;
        transitioning.add(uuid);
        transitionTargets.put(uuid, target.clone());
        transitionTokens.put(uuid, token);
        if (delaySeconds == 0) {
            finishTransition(player, house, target, uuid, token, leaving, !preview);
        } else if (ownedHouseTeleport || preview) {
            int blindnessTicks = Math.max(0,
                    getConfig().getInt("transition.blindness-duration-seconds", 3)) * 20;
            if (blindnessTicks > 0) {
                XPotion.matchXPotion("BLINDNESS").map(effect -> effect.buildPotionEffect(blindnessTicks, 0))
                        .ifPresent(player::addPotionEffect);
            }
            messages.send(player, preview ? "preview-teleport-start" : "teleport-start",
                    values("house", house.getName()));
            scheduleTransition(player, house, target, uuid, token, delaySeconds - 1, leaving, ownedHouseTeleport, preview);
        } else {
            int blindnessTicks = getConfig().getInt("transition.blindness-duration-seconds", 3) * 20;
            XPotion.matchXPotion("BLINDNESS").map(effect -> effect.buildPotionEffect(blindnessTicks, 0))
                    .ifPresent(player::addPotionEffect);
            messages.send(player, "teleport-countdown", values("house", house.getName(),
                    "seconds", String.valueOf(delaySeconds)));
            scheduleTransition(player, house, target, uuid, token, delaySeconds - 1, leaving, false, false);
        }
    }

    private void scheduleTransition(Player player, House house, Location target, UUID uuid,
                                    long token, int remaining, boolean leaving,
                                    boolean ownedHouseTeleport, boolean preview) {
        BukkitTask task = getServer().getScheduler().runTaskLater(this, () -> {
            if (!player.isOnline() || !isCurrentTransition(uuid, token)) {
                return;
            }
            transitionTasks.remove(uuid);
            if (remaining <= 0) {
                finishTransition(player, house, target, uuid, token, leaving, !preview);
                return;
            }
            if ((!ownedHouseTeleport && !preview) || remaining <= 5) {
                messages.send(player, "teleport-countdown", values("house", house.getName(),
                        "seconds", String.valueOf(remaining)));
            }
            scheduleTransition(player, house, target, uuid, token, remaining - 1, leaving, ownedHouseTeleport, preview);
        }, 20L);
        transitionTasks.put(uuid, task);
    }

    private void finishTransition(Player player, House house, Location target, UUID uuid,
                                  long token, boolean leaving, boolean updateInsideState) {
        if (!isCurrentTransition(uuid, token)) {
            return;
        }
        if (player.teleport(target) && updateInsideState) {
            refreshPlayerHouseStatus(player, target, true);
        }
        transitioning.remove(uuid);
        transitionTargets.remove(uuid);
        transitionTokens.remove(uuid);
        transitionTasks.remove(uuid);
    }

    public void cancelTransition(Player player) {
        UUID uuid = player.getUniqueId();
        BukkitTask task = transitionTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
        transitioning.remove(uuid);
        transitionTargets.remove(uuid);
        transitionTokens.remove(uuid);
    }

    private int getEntryDelaySeconds(Player player) {
        if (isInstantTeleport(player)) {
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

    private int getOwnedHouseTeleportDelaySeconds(Player player) {
        if (isInstantTeleport(player)) {
            return 0;
        }
        return Math.max(0, getConfig().getInt("transition.owned-house-delay-seconds", 10));
    }

    private boolean isInstantTeleport(Player player) {
        return player.hasPermission("kodarihousing.admin") || player.hasPermission("house.timer.instant");
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
        cancelTransition(player);
        UUID uuid = player.getUniqueId();
        insidePlayers.remove(uuid);
    }

    public void refreshPlayerHouseStatus(Player player, Location location, boolean resetOutside) {
        if (player == null || location == null || location.getWorld() == null) {
            return;
        }
        if (!resetOutside && isTransitioning(player)) {
            return;
        }
        House insideHouse = findHouseAtInside(location, player.getUniqueId());
        if (insideHouse != null) {
            insidePlayers.put(player.getUniqueId(), insideHouse);
            return;
        }
        House currentHouse = insidePlayers.get(player.getUniqueId());
        if (currentHouse != null && isNearOutside(currentHouse, location)) {
            insidePlayers.remove(player.getUniqueId());
        } else if (resetOutside) {
            insidePlayers.remove(player.getUniqueId());
        }
    }

    private House findHouseAtInside(Location location, UUID playerId) {
        for (House house : houseManager.all()) {
            if (house.getOwner() == null || !house.getOwner().equals(playerId)) {
                continue;
            }
            for (HouseDoor door : house.getDoors()) {
                Location inside = door.getInside();
                if (inside != null && inside.getWorld() != null && inside.getWorld().equals(location.getWorld())
                        && inside.distanceSquared(location) <= 4.0) {
                    return house;
                }
            }
        }
        return null;
    }

    private boolean isNearOutside(House house, Location location) {
        for (HouseDoor door : house.getDoors()) {
            Location outside = door.getOutside();
            if (outside != null && outside.getWorld() != null && outside.getWorld().equals(location.getWorld())
                    && outside.distanceSquared(location) <= 9.0) {
                return true;
            }
        }
        return false;
    }

    private boolean isCurrentTransition(UUID uuid, long token) {
        return transitioning.contains(uuid) && Long.valueOf(token).equals(transitionTokens.get(uuid));
    }

    private boolean isAtDoorSide(Player player, HouseDoor door, boolean inside) {
        Location target = inside ? door.getInside() : door.getOutside();
        return target != null && target.getWorld() != null && target.getWorld().equals(player.getWorld())
                && player.getLocation().distanceSquared(target) <= 9.0;
    }

    private double distanceToDoorSide(Player player, HouseDoor door, boolean inside) {
        Location target = inside ? door.getInside() : door.getOutside();
        return target == null || target.getWorld() == null || !target.getWorld().equals(player.getWorld())
                ? Double.MAX_VALUE : player.getLocation().distanceSquared(target);
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

    /** @deprecated Use the registered {@link PacificaHousingAPI} service. */
    @Deprecated
    public HouseManager getHouseManager() { return houseManager; }
    /** @deprecated Use the registered {@link PacificaHousingAPI} service. */
    @Deprecated
    public AuctionManager getAuctionManager() { return auctionManager; }
    EconomyManager getEconomyManager() { return economyManager; }
    public MessageService getMessages() { return messages; }
    public FileConfiguration getGuiConfig() { return guiConfig; }
    public PacificaHousingAPI getHousingApi() { return housingApi; }

    private void migrateDataFolder() {
        File target = getDataFolder();
        File parent = target.getParentFile();
        if (parent == null) {
            return;
        }
        target.mkdirs();
        File[] legacyFolders = {new File(parent, "Kodari"), new File(parent, "Pacifica-Housing")};
        for (File legacy : legacyFolders) {
            if (!legacy.exists() || legacy.equals(target)) {
                continue;
            }
            copyMissingFiles(legacy, target);
        }
    }

    private void copyMissingFiles(File source, File target) {
        if (source.isDirectory()) {
            File[] files = source.listFiles();
            if (files == null) {
                return;
            }
            for (File file : files) {
                copyMissingFiles(file, new File(target, file.getName()));
            }
            return;
        }
        if (target.exists()) {
            return;
        }
        File parent = target.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        try {
            Files.copy(source.toPath(), target.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
        } catch (IOException exception) {
            getLogger().warning("Could not migrate " + source.getName() + " to DistrictX: "
                    + exception.getMessage());
        }
    }

}