package com.districtx.housing;

import com.districtx.housing.model.House;
import com.districtx.housing.model.HouseDoor;
import com.districtx.housing.model.HouseType;
import com.districtx.housing.model.HouseVault;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.block.Container;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HouseManager {
    private final HousingPlugin plugin;
    private final File file;
    private final Map<String, House> houses = new HashMap<>();
    private FileConfiguration data;

    public HouseManager(HousingPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "houses.yml");
        load();
    }

    public void load() {
        if (!file.exists()) {
            plugin.saveResource("houses.yml", false);
        }
        data = YamlConfiguration.loadConfiguration(file);
        houses.clear();
        ConfigurationSection root = data.getConfigurationSection("houses");
        if (root == null) {
            return;
        }
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            HouseType type = HouseType.from(section.getString("type"));
            if (type == null) {
                continue;
            }
            House house = new House(section.getString("name", key), type, section.getDouble("price"));
            String owner = section.getString("owner");
            if (owner != null && !owner.isEmpty()) {
                try {
                    house.setOwner(UUID.fromString(owner));
                } catch (IllegalArgumentException ignored) {
                }
            }
            ConfigurationSection doors = section.getConfigurationSection("doors");
            if (doors != null) {
                int fallbackNumber = 1;
                for (String id : doors.getKeys(false)) {
                    ConfigurationSection doorSection = doors.getConfigurationSection(id);
                    Location doorLocation = readLocation(doorSection.getConfigurationSection("door"));
                    if (doorLocation == null) {
                        continue;
                    }
                    int number = doorSection.getInt("number", fallbackNumber++);
                    HouseDoor door = new HouseDoor(id, number, doorLocation);
                    door.setOutside(readLocation(doorSection.getConfigurationSection("outside")));
                    door.setInside(readLocation(doorSection.getConfigurationSection("inside")));
                    house.getDoors().add(door);
                }
            }
            ConfigurationSection vaults = section.getConfigurationSection("vaults");
            if (vaults != null) {
                for (String id : vaults.getKeys(false)) {
                    ConfigurationSection vaultSection = vaults.getConfigurationSection(id);
                    Location vaultLocation = readLocation(vaultSection);
                    if (vaultLocation != null) {
                        HouseVault vault = new HouseVault(id, vaultLocation);
                        ConfigurationSection contents = vaultSection.getConfigurationSection("contents");
                        if (contents != null) {
                            for (int slot = 0; slot < HouseVault.INVENTORY_SIZE; slot++) {
                                vault.getContents()[slot] = contents.getItemStack(String.valueOf(slot));
                            }
                        } else if (vaultLocation.getWorld() != null
                                && vaultLocation.getBlock().getState() instanceof Container) {
                            Container container = (Container) vaultLocation.getBlock().getState();
                            for (int slot = 0; slot < Math.min(HouseVault.INVENTORY_SIZE,
                                    container.getInventory().getSize()); slot++) {
                                vault.getContents()[slot] = container.getInventory().getItem(slot);
                            }
                        }
                        house.getVaults().add(vault);
                    }
                }
            }
            houses.put(key.toLowerCase(), house);
        }
    }

    public boolean save() {
        data.set("houses", null);
        for (House house : houses.values()) {
            String path = "houses." + house.getName();
            data.set(path + ".name", house.getName());
            data.set(path + ".type", house.getType().name().toLowerCase());
            data.set(path + ".price", house.getPrice());
            data.set(path + ".owner", house.getOwner() == null ? null : house.getOwner().toString());
            for (HouseDoor door : house.getDoors()) {
                String doorPath = path + ".doors." + door.getId();
                data.set(doorPath + ".number", door.getNumber());
                writeLocation(doorPath + ".door", door.getDoor());
                writeLocation(doorPath + ".outside", door.getOutside());
                writeLocation(doorPath + ".inside", door.getInside());
            }
            for (HouseVault vault : house.getVaults()) {
                String vaultPath = path + ".vaults." + vault.getId();
                writeLocation(vaultPath, vault.getLocation());
                for (int slot = 0; slot < HouseVault.INVENTORY_SIZE; slot++) {
                    data.set(vaultPath + ".contents." + slot, vault.getContents()[slot]);
                }
            }
        }
        try {
            data.save(file);
            return true;
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not save houses.yml: " + exception.getMessage());
            return false;
        }
    }

    public House get(String name) {
        return name == null ? null : houses.get(name.toLowerCase());
    }

    public Collection<House> all() {
        return houses.values();
    }

    public House create(String name, HouseType type, double price, Location doorLocation) {
        if (get(name) != null || findByDoor(doorLocation.getBlock()) != null) {
            return null;
        }
        House house = new House(name, type, price);
        house.getDoors().add(new HouseDoor(UUID.randomUUID().toString(), 1, doorLocation.clone()));
        houses.put(name.toLowerCase(), house);
        save();
        return house;
    }

    public HouseDoor addDoor(House house, Location doorLocation) {
        if (findByDoor(doorLocation.getBlock()) != null) {
            return null;
        }
        int number = 1;
        for (HouseDoor door : house.getDoors()) {
            number = Math.max(number, door.getNumber() + 1);
        }
        HouseDoor addedDoor = new HouseDoor(UUID.randomUUID().toString(), number, doorLocation.clone());
        house.getDoors().add(addedDoor);
        save();
        return addedDoor;
    }

    public HouseVault addVault(House house, Location vaultLocation) {
        if (house == null || house.getType() == HouseType.REGULAR
                || !isVaultContainer(vaultLocation.getBlock()) || findByVault(vaultLocation.getBlock()) != null) {
            return null;
        }
        HouseVault vault = new HouseVault(UUID.randomUUID().toString(), vaultLocation.clone());
        house.getVaults().add(vault);
        save();
        return vault;
    }

    public void removeVault(House house, HouseVault vault) {
        if (house.getVaults().remove(vault)) {
            save();
        }
    }

    public int getVaultNumber(House house, HouseVault vault) {
        return house == null || vault == null ? -1 : house.getVaults().indexOf(vault) + 1;
    }

    public House findByDoor(Block block) {
        for (House house : houses.values()) {
            for (HouseDoor door : house.getDoors()) {
                if (sameDoor(door.getDoor(), block)) {
                    return house;
                }
            }
        }
        return null;
    }

    public HouseDoor findDoor(House house, Block block) {
        for (HouseDoor door : house.getDoors()) {
            if (sameDoor(door.getDoor(), block)) {
                return door;
            }
        }
        return null;
    }

    public House findByVault(Block block) {
        for (House house : houses.values()) {
            for (HouseVault vault : house.getVaults()) {
                if (sameBlock(vault.getLocation(), block)) {
                    return house;
                }
            }
        }
        return null;
    }

    public HouseVault findVault(House house, Block block) {
        for (HouseVault vault : house.getVaults()) {
            if (sameBlock(vault.getLocation(), block)) {
                return vault;
            }
        }
        return null;
    }

    public HouseDoor getDoor(House house, int number) {
        for (HouseDoor door : house.getDoors()) {
            if (door.getNumber() == number) {
                return door;
            }
        }
        return null;
    }

    public int countOwned(UUID uuid) {
        int count = 0;
        for (House house : houses.values()) {
            if (uuid.equals(house.getOwner())) {
                count++;
            }
        }
        return count;
    }

    public static boolean isIronDoor(Block block) {
        if (block == null) {
            return false;
        }
        String name = XMaterial.matchXMaterial(block.getType()).name();
        return "IRON_DOOR".equals(name) || "IRON_DOOR_BLOCK".equals(name);
    }

    public static boolean isVaultContainer(Block block) {
        if (block == null) {
            return false;
        }
        String name = XMaterial.matchXMaterial(block.getType()).name();
        return "CHEST".equals(name) || "TRAPPED_CHEST".equals(name) || "BARREL".equals(name);
    }

    private boolean sameBlock(Location location, Block block) {
        return block != null && location != null && location.getWorld() != null
                && location.getWorld().equals(block.getWorld())
                && location.getBlockX() == block.getX() && location.getBlockY() == block.getY()
                && location.getBlockZ() == block.getZ();
    }

    private boolean sameDoor(Location location, Block block) {
        if (block == null || location == null || location.getWorld() == null
                || !location.getWorld().equals(block.getWorld())
                || location.getBlockX() != block.getX() || location.getBlockZ() != block.getZ()) {
            return false;
        }
        return Math.abs(location.getBlockY() - block.getY()) <= 1;
    }

    private void writeLocation(String path, Location location) {
        if (location == null || location.getWorld() == null) {
            data.set(path, null);
            return;
        }
        data.set(path + ".world", location.getWorld().getName());
        data.set(path + ".x", location.getX());
        data.set(path + ".y", location.getY());
        data.set(path + ".z", location.getZ());
        data.set(path + ".yaw", location.getYaw());
        data.set(path + ".pitch", location.getPitch());
    }

    private Location readLocation(ConfigurationSection section) {
        if (section == null || section.getString("world") == null) {
            return null;
        }
        return new Location(Bukkit.getWorld(section.getString("world")), section.getDouble("x"),
                section.getDouble("y"), section.getDouble("z"), (float) section.getDouble("yaw"),
                (float) section.getDouble("pitch"));
    }
}