package com.amniel.hsbmechanics.inventories.crafting

import com.amniel.hsbmechanics.HSBMechanics
import com.amniel.hsbmechanics.interfaces.AbstractMenu
import com.amniel.hsbmechanics.utils.CraftUtils
import dev.triumphteam.gui.guis.Gui
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentBuilder
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import java.util.Objects

class CraftMenu(val plugin: HSBMechanics) : AbstractMenu {
    private val craftSuggestionsMenu = CraftSuggestionsMenu(plugin, this)

    override fun build(player: Player) {
        val gui = Gui.gui()
            .title(Component.text("Enhanced Crafting Table"))
            .rows(6)
            .create()


        gui.setOpenGuiAction(::onOpen)
        gui.setCloseGuiAction(::onClose)
        gui.setDefaultClickAction(::onClick)
        gui.setDragAction { event -> checkValidRecipes(player.world, event.inventory.holder!!) }

        gui.open(player)
    }

    fun onOpen(event: InventoryOpenEvent) {
        val inventory = event.inventory
        val gui = inventory.holder
        if (gui !is Gui) {
            return
        }

        gui.filler.fillBetweenPoints(0, 0, 5, 9, CraftUtils.FILL_BACKGROUND.asGuiItem())
        gui.filler.fillBetweenPoints(
            CraftUtils.QUICK_CRAFT_FIRST_ROW, CraftUtils.QUICK_CRAFT_COLUMN,
            CraftUtils.QUICK_CRAFT_FIRST_ROW + CraftUtils.QUICK_CRAFT_SLOTS - 1,
            CraftUtils.QUICK_CRAFT_COLUMN, CraftUtils.QUICK_CRAFT_SLOT.asGuiItem()
        )
        clearResult(gui)
        gui.update()

        val player = event.player as Player
        object : BukkitRunnable() {
            override fun run() {
                checkRecipeSuggestions(player, gui)
            }
        }.runTaskAsynchronously(plugin)
    }

    fun onClose(event: InventoryCloseEvent) {
        val inventory = event.inventory
        val gui = inventory.holder
        if (gui !is Gui) {
            return
        }

        val player = event.player
        val playerInv = player.inventory

        val itemsFailedToAdd: MutableList<ItemStack> = ArrayList()
        for (slot in CraftUtils.CRAFT_SLOTS) {
            val item = inventory.getItem(slot)

            if (item != null && item.type != Material.AIR) {
                itemsFailedToAdd.addAll(playerInv.addItem(item).values)
            }
        }

        if (!itemsFailedToAdd.isEmpty()) {
            val playerLocation = player.location
            val playerWorld = playerLocation.world

            for (item in itemsFailedToAdd) {
                playerWorld.dropItemNaturally(playerLocation, item)
            }
        }
    }


    fun onClick(event: InventoryClickEvent) {
        val inventory = event.inventory
        val gui = inventory.holder
        if (gui !is Gui) {
            return
        }

        val player = event.whoClicked as Player
        val item = event.currentItem

        val slot = event.slot
        if (slot == CraftUtils.CLOSE_MENU_SLOT && item != null && item.type == Material.BARRIER) {
            gui.close(player)
            event.isCancelled = true
        }

        object : BukkitRunnable() {
            override fun run() {
                checkRecipeSuggestions(player, gui)
            }
        }.runTaskAsynchronously(plugin)

        val clickedInv = event.clickedInventory
        if (clickedInv == null) {
            return
        }

        if (slot != CraftUtils.RESULT_SLOT && !CraftUtils.CRAFT_SLOTS.contains(slot) && clickedInv == inventory) {
            event.isCancelled = true
            return
        }

        val world = player.world

        if (slot == CraftUtils.RESULT_SLOT && clickedInv == inventory) {
            if (item != null && item.type != Material.BARRIER) {
                object : BukkitRunnable() {
                    override fun run() {
                        craftRecipe(event, world, gui)
                    }
                }.runTaskAsynchronously(plugin)
            }
            event.isCancelled = true
        } else {
            object : BukkitRunnable() {
                override fun run() {
                    checkValidRecipes(world, gui)
                }
            }.runTaskAsynchronously(plugin)
        }
    }

    fun craftRecipe(event: InventoryClickEvent, world: World, gui: Gui) {
        // TODO: work fine?
        val items = CraftUtils.CRAFT_SLOTS.mapNotNull(gui.inventory::getItem).toTypedArray()
        val recipe = plugin.server.getCraftingRecipe(items, world)
        if (recipe == null) {
            return
        }

        var newMatrix = arrayOf<ItemStack>()
        val requiredItems = CraftUtils.getRequiredItems(recipe).toMutableList()

        for (i in 0..items.size) {
            val item = items[i]
            if (item.type == Material.AIR) {
                newMatrix[i] = ItemStack.of(Material.AIR)
                continue
            }

            val itemStream = requiredItems.stream().filter { requiredItem -> requiredItem.isSimilar(item) }
            if (itemStream.findFirst().isPresent) {
                newMatrix[i] = item.asQuantity(item.amount - 1)
                for (j in 0..requiredItems.size) {
                    val requiredItem = requiredItems[j]
                    if (requiredItem.isSimilar(item)) {
                        requiredItems.removeAt(j)
                        break
                    }
                }
            }
        }

        val currentCursor = event.cursor
        val result = recipe.result
        if (currentCursor.isEmpty) {
            @Suppress("DEPRECATION") // TODO
            event.setCursor(result)
        } else {
            if (!currentCursor.isSimilar(result)) return
            if ((currentCursor.amount + result.amount) >= currentCursor.maxStackSize) return
            currentCursor.amount = currentCursor.amount + result.amount
        }
        clearResult(gui, false)

        var i2 = 0
        for (j in CraftUtils.CRAFT_SLOTS) {
            gui.inventory.setItem(j, newMatrix[i2])
            ++i2
        }
        checkValidRecipes(world, gui)
    }

    fun checkRecipeSuggestions(player: Player, gui: Gui) {
        val suggestions: MutableMap<ItemStack, MutableSet<ItemStack>> = craftSuggestionsMenu.getCraftableItems(player)
        var totalSuggestions = suggestions.size
        val suggestionIterator = suggestions.entries.iterator()
        for (i in 0..CraftUtils.QUICK_CRAFT_SLOTS) {
            val slot = (9 * (CraftUtils.QUICK_CRAFT_FIRST_ROW + i)) - (10 - CraftUtils.QUICK_CRAFT_COLUMN)
            gui.inventory.setItem(slot, CraftUtils.QUICK_CRAFT_SLOT.build())

            if (totalSuggestions > i) {
                while (suggestionIterator.hasNext()) {
                    val suggestionEntry = suggestionIterator.next()

                    val ingredients = suggestionEntry.value
                    val craftable = CraftUtils.getAmountCraftable(player, ingredients)
                    if (craftable <= 0) {
                        --totalSuggestions
                        continue
                    }
                    val suggestionStackGui = craftSuggestionsMenu.formatItemSuggestion(player, suggestionEntry)
                    gui.updateItem(slot, suggestionStackGui)

                    break
                }
            }
        }

        if (totalSuggestions > 3) {
            val extraSuggestions = totalSuggestions - 3
            val firstLine: ComponentBuilder<TextComponent, TextComponent.Builder> = Component.text()

            if (extraSuggestions == 1) {
                firstLine.append(
                    Component.text("There is ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("1", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false),
                    Component.text(" more recipe that you", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
                )
            } else {
                firstLine.append(
                    Component.text("There are ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text(extraSuggestions, NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false),
                    Component.text(" more recipes that you", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
                )
            }

            CraftUtils.QUICK_CRAFT_MORE_SLOT.lore(
                listOf(
                    firstLine.build(),
                    Component.text("may craft!", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text(""),
                    Component.text("Click to view them!", NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false)
                )
            )
            gui.updateItem(
                CraftUtils.QUICK_CRAFT_EXTRA_SLOT,
                CraftUtils.QUICK_CRAFT_MORE_SLOT.asGuiItem { action -> craftSuggestionsMenu.build(player) })
        } else {
            gui.updateItem(CraftUtils.QUICK_CRAFT_EXTRA_SLOT, CraftUtils.FILL_BACKGROUND.asGuiItem())
        }
    }

    fun checkValidRecipes(world: World, holder: InventoryHolder) {
        if (holder is Gui) {
            object : BukkitRunnable() {
                override fun run() {
                    checkValidRecipes(world, holder)
                }
            }.runTaskAsynchronously(plugin)
        }
    }

    fun checkValidRecipes(world: World, gui: Gui) {
        val items: Array<ItemStack?> = Array(9) { i -> CraftUtils.CRAFT_SLOTS[i].let(gui.inventory::getItem) }
        val itemsNonNull: Array<ItemStack> = Array(9) { i -> items[i] ?: ItemStack.empty() }
        val recipe = plugin.server.getCraftingRecipe(itemsNonNull, world)

        if (recipe != null) {
            addCraftResult(gui, recipe.result)
        } else {
            clearResult(gui, false)
        }
    }

    fun addCraftResult(gui: Gui, result: ItemStack) {
        for (i in 0..9) {
            gui.updateItem(6, i, CraftUtils.READY_TO_CRAFT_ROW.asGuiItem())
        }
        gui.updateItem(CraftUtils.CLOSE_MENU_SLOT, CraftUtils.CLOSE_MENU.asGuiItem())

        val oldLore: MutableList<Component> = ArrayList(Objects.requireNonNullElse(result.lore(), arrayListOf()))
        oldLore.add(
            Component.text("------------", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)
                .decorate(TextDecoration.STRIKETHROUGH)
        )
        oldLore.add(
            Component.text("This is the item you are crafting.", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)
        )
        result.lore(oldLore)
        gui.updateItem(CraftUtils.RESULT_SLOT, result)
    }

    fun clearResult(gui: Gui, clearCraft: Boolean = true) {
        if (clearCraft) {
            for (slot in CraftUtils.CRAFT_SLOTS) {
                gui.removeItem(slot)
            }
        }

        for (i in 1..9) {
            gui.updateItem(6, i, CraftUtils.NO_RECIPE_ROW.asGuiItem())
        }
        gui.updateItem(CraftUtils.CLOSE_MENU_SLOT, CraftUtils.CLOSE_MENU.asGuiItem())
        gui.updateItem(CraftUtils.RESULT_SLOT, CraftUtils.NO_RESULT_READY.asGuiItem())
        gui.updateItem(CraftUtils.RESULT_SLOT - 1, CraftUtils.FILL_BACKGROUND.asGuiItem())
    }
}