package com.districtx.housing.inventory.gui;

import com.districtx.housing.HousingPlugin;
import com.districtx.housing.inventory.InventoryButton;
import com.districtx.housing.inventory.InventoryGUI;
import com.districtx.housing.model.House;
import com.districtx.housing.model.AuctionCreationSession;
import com.districtx.housing.util.GuiItems;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;

public class AuctionListingGUI extends InventoryGUI {
    private final HousingPlugin plugin;
    private final Player owner;
    private final House house;
    private final AuctionCreationSession session;

    public AuctionListingGUI(HousingPlugin plugin, Player owner, House house,
                             Long minimumDiamonds, Double buyoutPrice) {
        this(plugin, owner, house, new AuctionCreationSession(owner.getUniqueId(), house.getName()));
        if (minimumDiamonds != null) {
            session.setMinimumDiamondBid(java.math.BigDecimal.valueOf(minimumDiamonds));
            session.setDiamondBidConfigured(true);
        }
        if (buyoutPrice != null) {
            session.setBuyoutPrice(java.math.BigDecimal.valueOf(buyoutPrice));
            session.setBuyoutConfigured(true);
        }
    }

    public AuctionListingGUI(HousingPlugin plugin, Player owner, House house,
                             AuctionCreationSession session) {
        this.plugin = plugin;
        this.owner = owner;
        this.house = house;
        this.session = session;
        addButton(13, button("auction-listing.house", this::houseItem, event -> { }));
        addButton(29, button("auction-listing.minimum-diamonds", player -> item("auction-listing.minimum-diamonds",
                values("minimum-diamonds", minimumDiamonds() == null ? "NONE" : plugin.formatAmount(minimumDiamonds()))),
                event -> plugin.openListingMinimumInput((Player) event.getWhoClicked(), this)));
        addButton(31, button("auction-listing.buyout", player -> item("auction-listing.buyout",
                values("buyout-price", session.isBuyoutConfigured() ? plugin.formatBalance(buyoutPrice()) : "NONE")),
                event -> plugin.openListingBuyoutInput((Player) event.getWhoClicked(), this)));
        addButton(33, button("auction-listing.fee", player -> item("auction-listing.fee", feeValues(player)), event ->
                plugin.openListingFeeInput((Player) event.getWhoClicked(), this)));
        addButton(40, button("auction-listing.start", player -> item("auction-listing.start", feeValues(player)),
                event -> plugin.publishAuction((Player) event.getWhoClicked(), this)));
    }

    private InventoryButton button(String path, java.util.function.Function<Player, org.bukkit.inventory.ItemStack> creator,
                                   java.util.function.Consumer<org.bukkit.event.inventory.InventoryClickEvent> consumer) {
        return new InventoryButton().creator(creator).consumer(consumer);
    }

    private org.bukkit.inventory.ItemStack houseItem(Player player) {
        return item("auction-listing.house", values(
                "house-name", house.getName(),
                "vault-count", String.valueOf(house.getVaults().size()),
                "house-price", plugin.formatAmount(house.getPrice()),
                "owner-name", owner.getName()));
    }

    private Map<String, String> feeValues(Player player) {
        Map<String, String> values = values("minimum-diamonds",
                minimumDiamonds() == null ? "NONE" : plugin.formatAmount(minimumDiamonds()));
        values.put("starting-bid", session.getStartingBid() == null ? "NONE"
                : plugin.formatBalance(session.getStartingBid().doubleValue()));
        values.put("listing-fee", session.getListingFee() == null ? "NONE"
                : plugin.formatBalance(session.getListingFee().doubleValue()));
        values.put("fee-percent", plugin.formatAmount(plugin.getListingFeeMultiplier(player).doubleValue()));
        return values;
    }

    private Long minimumDiamonds() {
        return session.getMinimumDiamondBid() == null ? null : session.getMinimumDiamondBid().longValue();
    }

    private Double buyoutPrice() {
        return session.getBuyoutPrice() == null ? null : session.getBuyoutPrice().doubleValue();
    }

    private org.bukkit.inventory.ItemStack item(String path, Map<String, String> values) {
        ConfigurationSection section = plugin.getGuiConfig().getConfigurationSection(path);
        return GuiItems.create(section, values);
    }

    private Map<String, String> values(String... entries) {
        Map<String, String> values = new HashMap<>();
        for (int i = 0; i + 1 < entries.length; i += 2) values.put(entries[i], entries[i + 1]);
        return values;
    }

    public House getHouse() { return house; }
    public Long getMinimumDiamonds() { return minimumDiamonds(); }
    public Double getBuyoutPrice() { return buyoutPrice(); }
    public AuctionCreationSession getSession() { return session; }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, plugin.getGuiConfig().getInt("auction-listing.size", 54),
                ChatColor.translateAlternateColorCodes('&', plugin.getGuiConfig()
                        .getString("auction-listing.title", "&a&lCreate Auction Listing")));
    }
}