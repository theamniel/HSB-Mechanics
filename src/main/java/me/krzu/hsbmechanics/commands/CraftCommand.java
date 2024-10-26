package me.krzu.hsbmechanics.commands;

import me.fixeddev.commandflow.annotated.CommandClass;
import me.fixeddev.commandflow.annotated.annotation.Command;
import me.fixeddev.commandflow.annotated.annotation.OptArg;
import me.fixeddev.commandflow.bukkit.annotation.Sender;
import me.krzu.hsbmechanics.HSBMechanics;
import me.krzu.hsbmechanics.interfaces.AbstractMenu;
import me.krzu.hsbmechanics.inventories.crafting.CraftMenu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Command(names = {"craft"}, desc = "Open the Super Crafting Table")
public class CraftCommand implements CommandClass {

    private final HSBMechanics plugin;

    public CraftCommand(HSBMechanics plugin) {
        this.plugin = plugin;
    }

    @Command(names = "")
    public void onMainCommand(
            @Sender CommandSender sender, @OptArg Player player
    ) {
        AbstractMenu menu = new CraftMenu(plugin);

        if (player == null && sender instanceof Player) {
            menu.build((Player) sender);
        } else if (player != null) {
            menu.build(player);
        } else {
            sender.sendMessage(Component.text("You need to specify a player!", NamedTextColor.RED));
        }
    }

}
