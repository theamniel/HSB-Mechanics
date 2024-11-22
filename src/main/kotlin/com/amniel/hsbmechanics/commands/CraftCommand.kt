package com.amniel.hsbmechanics.commands

import com.amniel.hsbmechanics.HSBMechanics
import com.amniel.hsbmechanics.inventories.crafting.CraftMenu
import me.fixeddev.commandflow.annotated.CommandClass
import me.fixeddev.commandflow.annotated.annotation.Command
import me.fixeddev.commandflow.annotated.annotation.OptArg
import me.fixeddev.commandflow.bukkit.annotation.Sender
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@Command(names = ["craft"], desc = "Open the Super Crafting Table")
class CraftCommand(private val plugin: HSBMechanics) : CommandClass {

    @Suppress("UNUSED")
    @Command(names = [""])
    fun onMainCommand(@Sender sender: CommandSender, @OptArg player: Player?) {
        val menu = CraftMenu(plugin)
        when {
            player == null && sender is Player -> menu.build(sender)
            player != null -> menu.build(player)
            else -> {
                sender.sendMessage(Component.text("You need to specify a player!", NamedTextColor.RED))
            }
        }
    }
}