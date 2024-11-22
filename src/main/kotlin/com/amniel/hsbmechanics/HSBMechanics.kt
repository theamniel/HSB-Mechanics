package com.amniel.hsbmechanics

import com.amniel.hsbmechanics.commands.CraftCommand
import com.amniel.hsbmechanics.listeners.CraftListeners
import me.fixeddev.commandflow.annotated.AnnotatedCommandTreeBuilder
import me.fixeddev.commandflow.annotated.part.PartInjector
import me.fixeddev.commandflow.annotated.part.defaults.DefaultsModule
import me.fixeddev.commandflow.bukkit.BukkitCommandManager
import me.fixeddev.commandflow.bukkit.factory.BukkitModule
import org.bukkit.plugin.java.JavaPlugin

class HSBMechanics : JavaPlugin() {

    override fun onEnable() {
        val injector = PartInjector.create()
        injector.install(DefaultsModule())
        injector.install(BukkitModule())

        val treeBuilder = AnnotatedCommandTreeBuilder.create(injector)
        val commandManager = BukkitCommandManager(name)
        commandManager.registerCommands(treeBuilder.fromClass(CraftCommand(this)))

        val pluginManager = server.pluginManager
        pluginManager.registerEvents(CraftListeners(this), this)
    }

    override fun onDisable() {
    }
}
