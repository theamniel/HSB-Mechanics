package com.amniel.hsbmechanics.utils

import com.amniel.hsbmechanics.HSBMechanics
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta

import java.net.MalformedURLException
import java.net.URI
import java.util.UUID
import java.util.logging.Logger

class HSBUtils {
    companion object {
        val LOGGER: Logger = Logger.getLogger(HSBMechanics::class.simpleName)

        fun createPlayerHead(texture: String): ItemStack {
            val itemStack = ItemStack(Material.PLAYER_HEAD)
            if (itemStack.itemMeta is SkullMeta) {
                val skullMeta = itemStack.itemMeta as SkullMeta
                try {
                    val playerProfile = Bukkit.createProfile(UUID.randomUUID())
                    val textures = playerProfile.textures
                    textures.skin = URI.create("http://textures.minecraft.net/texture/$texture").toURL()
                    playerProfile.setTextures(textures)
                    skullMeta.playerProfile = playerProfile
                    itemStack.setItemMeta(skullMeta)
                } catch (exception: MalformedURLException) {
                    LOGGER.warning("Can't load custom texture: ${exception.message}")
                }
            }
            return itemStack
        }
    }
}
