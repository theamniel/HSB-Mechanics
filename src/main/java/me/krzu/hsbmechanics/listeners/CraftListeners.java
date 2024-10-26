package me.krzu.hsbmechanics.listeners;

import me.krzu.hsbmechanics.HSBMechanics;
import me.krzu.hsbmechanics.inventories.crafting.CraftMenu;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class CraftListeners implements Listener {

    private final HSBMechanics plugin;

    public CraftListeners(HSBMechanics plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract (PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();

        if (action.isRightClick()) {
            if (player.isSneaking() && event.isBlockInHand()) {
                return;
            }

            Block block = event.getClickedBlock();

            if (block != null && block.getType() == Material.CRAFTING_TABLE) {
                event.setCancelled(true);

                new CraftMenu(plugin)
                        .build(player);
            }
        }
    }
}
