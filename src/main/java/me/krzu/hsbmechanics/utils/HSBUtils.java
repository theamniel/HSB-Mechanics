package me.krzu.hsbmechanics.utils;

import com.destroystokyo.paper.profile.PlayerProfile;
import me.krzu.hsbmechanics.HSBMechanics;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerTextures;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.UUID;
import java.util.logging.Logger;

public class HSBUtils {
    private final static Logger LOGGER = Logger.getLogger(HSBMechanics.class.getSimpleName());

    public static ItemStack createPlayerHead(String texture)  {
        ItemStack itemStack = new ItemStack(Material.PLAYER_HEAD);

        if (itemStack.getItemMeta() instanceof SkullMeta skullMeta) {
            try {
                PlayerProfile playerProfile = Bukkit.createProfile(UUID.randomUUID());
                PlayerTextures textures = playerProfile.getTextures();
                textures.setSkin(URI.create("http://textures.minecraft.net/texture/" + texture).toURL());
                playerProfile.setTextures(textures);
                skullMeta.setPlayerProfile(playerProfile);
                itemStack.setItemMeta(skullMeta);
            } catch (MalformedURLException exception) {
                LOGGER.warning("Can't load custom texture: " + exception.getMessage());
            }
        }

        return itemStack;
    }

}
