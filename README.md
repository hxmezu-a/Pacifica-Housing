# Pacifica-Housing

Pacifica-Housing is a Bukkit/Paper plugin for managing houses, vaults, and house auctions.

## Public Java API

The public API lives in `com.districtx.housing.api` and is registered through Bukkit's
`ServicesManager`. API queries read live Bukkit state, so invoke them on the server thread.
House, vault, and auction results are snapshots; locations and vault contents are copied.
Editing a returned vault contents array does not change stored vault data.

### Hook detection and obtaining the API

Declare Pacifica-Housing as a soft or hard dependency in your plugin's `plugin.yml` so it is
loaded first when present:

```yaml
softdepend:
  - Pacifica-Housing
```

Then check availability and retrieve the registered service:

```java
if (PacificaHousingAPI.isAvailable()) {
    PacificaHousingAPI.get().ifPresent(api -> {
        // Use api.houses(), api.vaults(), and api.auctions().
    });
}
```

The static lookup returns an empty `Optional` when the service is not registered. The service is
registered during Pacifica-Housing startup and unregistered on shutdown.

### Houses

```java
api.houses().getHouse("Beach House").ifPresent(house -> {
    String houseId = house.getId();
    String name = house.getName();
    HouseType type = house.getType();
    UUID owner = house.getOwnerUuid();
    Location location = house.getLocation();
    int vaultCount = house.getVaultCount();
});

List<HouseInfo> ownedHouses = api.houses().getHousesByOwner(player.getUniqueId());
boolean ownsHouse = api.houses().ownsHouse(player.getUniqueId(), "Beach House");
Optional<HouseInfo> atLocation = api.houses().getHouseAt(player.getLocation());
```

House IDs are stable, lower-case versions of their names. House snapshots expose status,
availability, owner, type, configured doors, location, teleport destination, vault count, and any
recorded auction. Region data and player names can be absent when the plugin has no such value.
`teleportToHouse(player, houseId)` uses Pacifica-Housing's existing owner-only teleport flow and
does not grant access to houses the player does not own.

### Vaults

```java
Optional<VaultInfo> vault = api.vaults().getVault("Beach House", 1);
List<VaultInfo> houseVaults = api.vaults().getVaults("Beach House");
int count = api.vaults().getVaultCount("Beach House");
boolean exists = api.vaults().vaultExists("Beach House", 1);
```

Vault numbers are one-based and match the number in the inventory title. A `VaultInfo` includes its
ID, house, owner, title, linked physical location, and a copied contents snapshot. The API does not
expose a live Bukkit inventory or a way to bypass vault ownership rules.

### Auctions

```java
Optional<AuctionInfo> auction = api.auctions().getAuction("Beach House");
List<AuctionInfo> activeAuctions = api.auctions().getActiveAuctions();
```

Auction snapshots expose seller UUID/name, current and minimum bid values, highest bidder,
creation/end times, status, and buyout data. Owner cancellation is available through
`api.auctions().cancelAuction(auctionIdOrHouseId, ownerUuid)` and returns an
`AuctionCancellationResult` describing the outcome.

### API events

Register listeners through Bukkit's plugin manager for:

* `HouseTeleportEvent` — fired before a house teleport completes; cancellable.
* `VaultOpenEvent` — fired after vault ownership checks and before the vault opens; cancellable.

Both events expose public API snapshots rather than internal house or vault storage classes.

### Dependency configuration

The Gradle project coordinates are group `com.districtx`, artifact `Pacifica-Housing`, version
`1.0.0`. After publishing the plugin jar to a Maven repository available to your build, use:

```gradle
dependencies {
    compileOnly("com.districtx:Pacifica-Housing:1.0.0")
}
```

For local development without a Maven repository, add the plugin jar to your project and use:

```gradle
dependencies {
    compileOnly(files("libs/Pacifica-Housing-1.0.0.jar"))
}
```