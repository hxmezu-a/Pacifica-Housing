package com.kodari.housing.util;

import com.cryptomorin.xseries.XMaterial;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class GuiItems {
    private GuiItems() {
    }

    public static ItemStack create(ConfigurationSection section, Map<String, String> replacements) {
        if (section == null) {
            return null;
        }
        String materialName = section.getString("material", "STONE");
        ItemStack item = XMaterial.matchXMaterial(materialName)
                .map(XMaterial::parseItem)
                .orElseGet(() -> XMaterial.matchXMaterial("STONE").map(XMaterial::parseItem).orElse(null));
        if (item == null) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(format(section.getString("name", ""), replacements));
        List<String> lore = new ArrayList<>();
        for (String line : section.getStringList("lore")) {
            lore.add(format(line, replacements));
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static String format(String value, Map<String, String> replacements) {
        for (Map.Entry<String, String> replacement : replacements.entrySet()) {
            value = value.replace("{" + replacement.getKey() + "}", replacement.getValue());
            value = value.replace("%" + replacement.getKey() + "%", replacement.getValue());
        }
        return ChatColor.translateAlternateColorCodes('&', value);
    }
}