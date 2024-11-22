package com.amniel.hsbmechanics.listeners

import com.amniel.hsbmechanics.HSBMechanics
import com.amniel.hsbmechanics.inventories.crafting.CraftMenu
import org.bukkit.Material
import org.bukkit.event.Listener
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerInteractEvent

class CraftListeners(val plugin: HSBMechanics) : Listener {

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        val action = event.action

        if (action.isRightClick) {
            if (player.isSneaking && event.isBlockInHand) {
                return
            }
            val block = event.clickedBlock

            if (block != null && block.type == Material.CRAFTING_TABLE) {
                event.isCancelled = true
                CraftMenu(plugin).build(player)
            }
        }
    }
}