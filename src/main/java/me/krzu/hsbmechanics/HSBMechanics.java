package me.krzu.hsbmechanics;

import me.fixeddev.commandflow.CommandManager;
import me.fixeddev.commandflow.annotated.AnnotatedCommandTreeBuilder;
import me.fixeddev.commandflow.annotated.part.PartInjector;
import me.fixeddev.commandflow.annotated.part.defaults.DefaultsModule;
import me.fixeddev.commandflow.bukkit.BukkitCommandManager;
import me.fixeddev.commandflow.bukkit.factory.BukkitModule;
import me.krzu.hsbmechanics.commands.CraftCommand;
import me.krzu.hsbmechanics.listeners.CraftListeners;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class HSBMechanics extends JavaPlugin {

    @Override
    public void onEnable() {
        PartInjector injector = PartInjector.create();
        injector.install(new DefaultsModule());
        injector.install(new BukkitModule());

        AnnotatedCommandTreeBuilder treeBuilder = AnnotatedCommandTreeBuilder.create(injector);

        CommandManager commandManager = new BukkitCommandManager(getName());
        commandManager.registerCommands(treeBuilder.fromClass(new CraftCommand(this)));

        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new CraftListeners(this), this);
    }

    @Override
    public void onDisable() {
    }
}
