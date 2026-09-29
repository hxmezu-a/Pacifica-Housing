package com.districtx.housing.api;

import com.districtx.housing.HousingPlugin;
import com.districtx.housing.api.auction.AuctionCancellationResult;
import com.districtx.housing.api.auction.AuctionService;
import com.districtx.housing.api.house.HouseService;
import com.districtx.housing.api.vault.VaultService;
import com.districtx.housing.model.House;
import com.districtx.housing.model.HouseAuction;
import com.districtx.housing.model.HouseDoor;
import com.districtx.housing.model.HouseVault;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Bukkit service implementation; integrations should use {@link PacificaHousingAPI}. */
public final class PacificaHousingService implements PacificaHousingAPI {
    private final HousingPlugin plugin;
    private final HouseService houses;
    private final AuctionService auctions;
    private final VaultService vaults;

    /** Creates the service adapter for the plugin lifecycle. */
    public PacificaHousingService(HousingPlugin plugin) {
        this.plugin = plugin;
        this.houses = new HouseServiceAdapter();
        this.auctions = new AuctionServiceAdapter();
        this.vaults = new VaultServiceAdapter();
    }

    @Override
    public HouseService houses() {
        return houses;
    }

    @Override
    public AuctionService auctions() {
        return auctions;
    }

    @Override
    public VaultService vaults() {
        return vaults;
    }

    @Override
    public HouseInfo getHouse(String name) {
        return snapshot(name == null ? null : plugin.getHouseManager().get(name));
    }

    @Override
    public HouseInfo getHouseById(String id) {
        return getHouse(id);
    }

    @Override
    public HouseInfo getHouseAt(Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }
        for (House house : plugin.getHouseManager().all()) {
            for (HouseDoor door : house.getDoors()) {
                if (sameBlock(door.getDoor(), location)) {
                    return snapshot(house);
                }
            }
        }
        return null;
    }

    @Override
    public Collection<HouseInfo> getHouses() {
        List<HouseInfo> result = new ArrayList<>();
        for (House house : plugin.getHouseManager().all()) {
            result.add(snapshot(house));
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public Collection<HouseInfo> getAvailableHouses() {
        List<HouseInfo> result = new ArrayList<>();
        for (House house : plugin.getHouseManager().all()) {
            HouseInfo info = snapshot(house);
            if (info != null && info.isAvailable()) {
                result.add(info);
            }
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public Collection<HouseInfo> getHousesByOwner(UUID owner) {
        if (owner == null) {
            return Collections.emptyList();
        }
        List<HouseInfo> result = new ArrayList<>();
        for (House house : plugin.getHouseManager().all()) {
            if (owner.equals(house.getOwner())) {
                result.add(snapshot(house));
            }
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public HouseInfo getOwnedHouse(UUID owner) {
        if (owner == null) {
            return null;
        }
        for (House house : plugin.getHouseManager().all()) {
            if (owner.equals(house.getOwner())) {
                return snapshot(house);
            }
        }
        return null;
    }

    @Override
    public HouseInfo getHouseFor(Player player) {
        return player == null ? null : snapshot(plugin.getHouseFor(player));
    }

    @Override
    public boolean isPlayerInside(Player player) {
        return player != null && plugin.isInside(player);
    }

    @Override
    public AuctionInfo getAuction(String houseNameOrId) {
        return auctionSnapshot(houseNameOrId == null ? null : plugin.getAuctionManager().findAuction(houseNameOrId));
    }

    @Override
    public Collection<AuctionInfo> getAuctions() {
        List<AuctionInfo> result = new ArrayList<>();
        for (HouseAuction auction : plugin.getAuctionManager().all()) {
            result.add(auctionSnapshot(auction));
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public Collection<AuctionInfo> getActiveListings(UUID ownerUuid) {
        List<AuctionInfo> result = new ArrayList<>();
        for (HouseAuction auction : plugin.getAuctionManager().getActiveListings(ownerUuid)) {
            result.add(auctionSnapshot(auction));
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public boolean hasBids(String houseNameOrAuctionId) {
        return plugin.getAuctionManager().hasBids(houseNameOrAuctionId);
    }

    @Override
    public boolean canCancelAuction(String houseNameOrAuctionId, UUID ownerUuid) {
        return plugin.getAuctionManager().canCancelAuction(houseNameOrAuctionId, ownerUuid);
    }

    @Override
    public boolean cancelAuction(String houseNameOrAuctionId, UUID ownerUuid) {
        return auctions.cancelAuction(houseNameOrAuctionId, ownerUuid) == AuctionCancellationResult.SUCCESS;
    }

    private HouseInfo snapshot(House house) {
        if (house == null) {
            return null;
        }
        List<HouseDoorInfo> doors = new ArrayList<>();
        for (HouseDoor door : house.getDoors()) {
            doors.add(new HouseDoorInfo(door.getId(), door.getNumber(), door.getDoor(),
                    door.getOutside(), door.getInside()));
        }
        HouseDoor primary = house.getDoors().isEmpty() ? null : house.getDoors().get(0);
        AuctionInfo auction = auctionSnapshot(plugin.getAuctionManager().get(house.getName()));
        HouseStatus status = status(house, primary, auction);
        HouseAvailability availability = house.getOwner() != null ? HouseAvailability.OWNED
                : auction != null && auction.isActive() ? HouseAvailability.AUCTION
                : status == HouseStatus.NOT_READY ? HouseAvailability.UNAVAILABLE
                : HouseAvailability.AVAILABLE;
        String ownerName = house.getOwner() == null ? null : Bukkit.getOfflinePlayer(house.getOwner()).getName();
        return new HouseInfo(house.getName().toLowerCase(java.util.Locale.ROOT), house.getName(), house.getPrice(), type(house),
                house.getOwner(), ownerName, primary == null ? null : primary.getDoor(),
                primary == null ? null : primary.getOutside(), null, status, availability, doors, auction,
                house.getVaults().size());
    }

    private HouseStatus status(House house, HouseDoor primary, AuctionInfo auction) {
        if (primary == null || primary.getOutside() == null || primary.getInside() == null
                || primary.getDoor() == null) {
            return HouseStatus.NOT_READY;
        }
        if (auction != null && auction.isActive()) {
            return HouseStatus.AUCTION;
        }
        if (house.getOwner() != null) {
            return HouseStatus.OWNED;
        }
        return HouseStatus.AVAILABLE;
    }

    private AuctionInfo auctionSnapshot(HouseAuction auction) {
        if (auction == null) {
            return null;
        }
        String sellerName = Bukkit.getOfflinePlayer(auction.getSeller()).getName();
        UUID bidder = auction.getHighestBidder();
        String bidderName = bidder == null ? null : Bukkit.getOfflinePlayer(bidder).getName();
        return new AuctionInfo(auction.getAuctionId(), auction.getHouseName().toLowerCase(java.util.Locale.ROOT),
                auction.getHouseName(), auction.getSeller(), sellerName,
                auction.getStartingDiamonds(), auction.getStartingBalance(), auction.getCurrentDiamonds(),
                auction.getCurrentBalance(), auction.isBalanceAllowed(), auction.getEndAt(),
                auction.getSettlementState(), bidder, bidderName, auction.getBuyoutPrice(),
                auction.isBuyoutEnabled(), auction.getCommittedBids() == null
                        ? 0 : auction.getCommittedBids().size(), auction.getCreatedAt());
    }

    private HouseType type(House house) {
        switch (house.getType()) {
            case PREMIUM:
                return HouseType.PREMIUM;
            case LUXURY:
                return HouseType.LUXURY;
            default:
                return HouseType.REGULAR;
        }
    }

    private boolean sameBlock(Location first, Location second) {
        return first != null && first.getWorld() != null && second != null && second.getWorld() != null
                && first.getWorld().equals(second.getWorld())
                && first.getBlockX() == second.getBlockX() && first.getBlockY() == second.getBlockY()
                && first.getBlockZ() == second.getBlockZ();
    }

    private final class HouseServiceAdapter implements HouseService {
        @Override
        public Optional<HouseInfo> getHouse(String houseId) {
            return Optional.ofNullable(PacificaHousingService.this.getHouse(houseId));
        }

        @Override
        public List<HouseInfo> getHouses() {
            return Collections.unmodifiableList(new ArrayList<>(PacificaHousingService.this.getHouses()));
        }

        @Override
        public List<HouseInfo> getHousesByOwner(UUID ownerUuid) {
            return Collections.unmodifiableList(new ArrayList<>(
                    PacificaHousingService.this.getHousesByOwner(ownerUuid)));
        }

        @Override
        public boolean ownsHouse(UUID playerUuid, String houseId) {
            return playerUuid != null && getHouse(houseId)
                    .map(house -> playerUuid.equals(house.getOwnerUUID())).orElse(false);
        }

        @Override
        public boolean isAvailable(String houseId) {
            return getHouse(houseId).map(HouseInfo::isAvailable).orElse(false);
        }

        @Override
        public boolean isPremium(String houseId) {
            return getHouse(houseId).map(HouseInfo::isPremium).orElse(false);
        }

        @Override
        public boolean isLuxury(String houseId) {
            return getHouse(houseId).map(HouseInfo::isLuxury).orElse(false);
        }

        @Override
        public Optional<HouseInfo> getHouseAt(Location location) {
            return Optional.ofNullable(PacificaHousingService.this.getHouseAt(location));
        }

        @Override
        public boolean teleportToHouse(Player player, String houseId) {
            if (player == null || !player.isOnline() || houseId == null) {
                return false;
            }
            House house = plugin.getHouseManager().get(houseId);
            if (house == null || !player.getUniqueId().equals(house.getOwner())
                    || house.getDoors().isEmpty() || plugin.isTransitioning(player)) {
                return false;
            }
            HouseDoor door = house.getDoors().get(0);
            Location target = door.getOutside();
            if (target == null || target.getWorld() == null) {
                return false;
            }
            plugin.beginOwnedHouseTeleport(player, house, door);
            return true;
        }
    }

    private final class AuctionServiceAdapter implements AuctionService {
        @Override
        public Optional<AuctionInfo> getAuction(String auctionIdOrHouseId) {
            return Optional.ofNullable(PacificaHousingService.this.getAuction(auctionIdOrHouseId));
        }

        @Override
        public List<AuctionInfo> getAuctions() {
            return Collections.unmodifiableList(new ArrayList<>(PacificaHousingService.this.getAuctions()));
        }

        @Override
        public List<AuctionInfo> getActiveAuctions() {
            List<AuctionInfo> result = new ArrayList<>();
            for (AuctionInfo auction : getAuctions()) {
                if (auction.isActive() && plugin.getAuctionManager()
                        .getActiveListing(auction.getAuctionId(), auction.getSeller()) != null) {
                    result.add(auction);
                }
            }
            return Collections.unmodifiableList(result);
        }

        @Override
        public List<AuctionInfo> getActiveListings(UUID ownerUuid) {
            return Collections.unmodifiableList(new ArrayList<>(
                    PacificaHousingService.this.getActiveListings(ownerUuid)));
        }

        @Override
        public boolean hasBids(String auctionIdOrHouseId) {
            return PacificaHousingService.this.hasBids(auctionIdOrHouseId);
        }

        @Override
        public boolean canCancel(String auctionIdOrHouseId, UUID ownerUuid) {
            return PacificaHousingService.this.canCancelAuction(auctionIdOrHouseId, ownerUuid);
        }

        @Override
        public AuctionCancellationResult cancelAuction(String auctionIdOrHouseId, UUID ownerUuid) {
            AuctionCancellationResult result = plugin.getAuctionManager()
                    .cancelAuctionDetailed(auctionIdOrHouseId, ownerUuid);
            if (result == AuctionCancellationResult.SUCCESS) {
                plugin.refreshOpenMyListings();
            }
            return result;
        }
    }

    private final class VaultServiceAdapter implements VaultService {
        @Override
        public int getVaultCount(String houseId) {
            House house = houseId == null ? null : plugin.getHouseManager().get(houseId);
            return house == null ? 0 : house.getVaults().size();
        }

        @Override
        public Optional<VaultInfo> getVault(String houseId, int vaultNumber) {
            if (houseId == null || vaultNumber < 1) {
                return Optional.empty();
            }
            House house = plugin.getHouseManager().get(houseId);
            if (house == null || vaultNumber > house.getVaults().size()) {
                return Optional.empty();
            }
            HouseVault vault = house.getVaults().get(vaultNumber - 1);
            return Optional.of(vaultSnapshot(house, vault, vaultNumber));
        }

        @Override
        public List<VaultInfo> getVaults(String houseId) {
            House house = houseId == null ? null : plugin.getHouseManager().get(houseId);
            if (house == null) {
                return Collections.emptyList();
            }
            List<VaultInfo> result = new ArrayList<>();
            for (int index = 0; index < house.getVaults().size(); index++) {
                result.add(vaultSnapshot(house, house.getVaults().get(index), index + 1));
            }
            return Collections.unmodifiableList(result);
        }

        @Override
        public boolean vaultExists(String houseId, int vaultNumber) {
            return getVault(houseId, vaultNumber).isPresent();
        }

        private VaultInfo vaultSnapshot(House house, HouseVault vault, int number) {
            String title = org.bukkit.ChatColor.translateAlternateColorCodes('&',
                    "&a&l" + house.getName() + ":#" + number);
            return new VaultInfo(vault.getId(), house.getName().toLowerCase(java.util.Locale.ROOT),
                    house.getName(), house.getOwner(), number, title, vault.getLocation(), vault.getContents());
        }
    }
}