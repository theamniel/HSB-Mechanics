package com.amniel.hsbmechanics.inventories.crafting

import com.amniel.hsbmechanics.HSBMechanics
import com.amniel.hsbmechanics.interfaces.AbstractMenu
import com.amniel.hsbmechanics.utils.CraftUtils
import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.guis.Gui
import dev.triumphteam.gui.guis.GuiItem
import dev.triumphteam.gui.guis.PaginatedGui
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.inventory.ShapelessRecipe
import org.bukkit.scheduler.BukkitRunnable

class CraftSuggestionsMenu(val plugin: HSBMechanics, private val craftMenu: CraftMenu) : AbstractMenu {

    companion object {
        val PREVIOUS_PAGE: ItemBuilder = ItemBuilder.from(Material.ARROW).name(
            Component.text(
                "Previous Page",
                NamedTextColor.GREEN
            ).decoration(TextDecoration.ITALIC, false)
        )

        val NEXT_PAGE: ItemBuilder = ItemBuilder.from(Material.ARROW).name(
            Component.text(
                "Next Page",
                NamedTextColor.GREEN
            ).decoration(TextDecoration.ITALIC, false)
        )
    }

    override fun build(player: Player) {
        val gui = Gui.paginated()
            .title(Component.text("Quick Crafting"))
            .rows(6)
            .pageSize(28)
            .create()

        gui.setDefaultClickAction(::onClick)
        gui.setOpenGuiAction(::onOpen)
        gui.open(player)
    }

    fun onClick(event: InventoryClickEvent) {
        val inventory = event.inventory
        val gui = inventory.holder

        if (gui is PaginatedGui) {
            val player = event.whoClicked as Player

            object : BukkitRunnable() {
                override fun run() {
                    updateSuggestions(player, gui)
                }
            }.runTaskAsynchronously(plugin)

            val clickedInv = event.clickedInventory
            if (clickedInv == inventory) {
                event.isCancelled = true
            }
        }
    }

    fun onOpen(event: InventoryOpenEvent) {
        val inventory = event.inventory
        val gui = inventory.holder
        if (gui is PaginatedGui) {
            gui.filler.fillBorder(CraftUtils.FILL_BACKGROUND.asGuiItem())
            gui.setItem(49, CraftUtils.BACK_MENU.asGuiItem { action -> craftMenu.build(action.whoClicked as Player) })
            object : BukkitRunnable() {
                override fun run() {
                    val player = event.player as Player
                    updateSuggestions(player, gui)
                }
            }.runTask(plugin)
        }
    }

    fun updateSuggestions(player: Player, gui: PaginatedGui) {
        gui.clearPageItems(false)
        val suggestions = getCraftableItems(player)
        gui.addItem(*suggestions.entries.map { formatItemSuggestion(player, it) }.toTypedArray())

        val currentPage = gui.currentPageNum
        val pages = gui.pagesNum
        if (currentPage > pages) {
            @Suppress("UsePropertyAccessSyntax") // TODO
            gui.setPageNum(pages)
        }

        if (currentPage > 1 && pages > 1) {
            gui.setItem(
                6, 1, PREVIOUS_PAGE.lore(
                    Component.text("Page ${currentPage - 1}", NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false)
                ).asGuiItem { gui.previous() }
            )
        } else {
            gui.setItem(6, 1, CraftUtils.FILL_BACKGROUND.asGuiItem())
        }

        if (currentPage < pages && pages > 1) {
            gui.setItem(
                6, 9, NEXT_PAGE.lore(
                    Component.text("Page ${currentPage + 1}", NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false)
                ).asGuiItem { gui.next() })
        } else {
            gui.setItem(6, 9, CraftUtils.FILL_BACKGROUND.asGuiItem())
        }
        gui.update()
    }


    fun formatItemSuggestion(player: Player, suggestion: Map.Entry<ItemStack, Set<ItemStack>>): GuiItem {
        val item = suggestion.key
        val ingredients = suggestion.value
        val craftable = CraftUtils.getAmountCraftable(player, ingredients)
        val clonedItem = item.clone()
        CraftUtils.addSuggestionLore(item, craftable, ingredients)
        val guiItem = ItemBuilder.from(item).asGuiItem()
        guiItem.setAction { action -> craftSuggestion(action, player, clonedItem, ingredients) }
        return guiItem
    }

    fun craftSuggestion(
        action: InventoryClickEvent,
        player: Player,
        suggestion: ItemStack,
        ingredients: Set<ItemStack>
    ) {
        val inventory = player.inventory
        val inventoryContent = CraftUtils.stackSimilarItems(inventory.contents.mapNotNull { it?.clone() }.toList())

        action.isCancelled = true
        var canGiveItem = true
        var canStackOnCursor = true

        val currentCursor = action.cursor
        if (!currentCursor.isEmpty) {
            if (!currentCursor.isSimilar(suggestion)) {
                canStackOnCursor = false
            } else if ((currentCursor.amount + suggestion.amount) > currentCursor.maxStackSize) {
                canStackOnCursor = false
            }
        }

        if (!canStackOnCursor) {
            return
        }

        for (ingredient in ingredients) {
            val inventoryItem =
                inventoryContent.stream().filter { item -> item != null && item.isSimilar(ingredient) }.findFirst()
                    .orElse(null)

            if (inventoryItem == null || inventoryItem.amount < ingredient.amount) {
                canGiveItem = false
                break
            }
            inventory.removeItem(ingredient)
        }

        if (!canGiveItem) {
            return
        }

        if (currentCursor.isEmpty) {
            @Suppress("DEPRECATION") // TODO
            action.setCursor(suggestion)
        } else {
            @Suppress("DEPRECATION") // TODO
            action.setCursor(currentCursor.add(suggestion.amount))
        }
    }

    fun getCraftableItems(player: Player): MutableMap<ItemStack, MutableSet<ItemStack>> {
        val inventory = player.inventory
        val iterator = plugin.server.recipeIterator()
        val suggestions: MutableMap<ItemStack, MutableSet<ItemStack>> = HashMap()

        while (iterator.hasNext()) {
            val recipe = iterator.next()
            val result = recipe.result

            when {
                suggestions.keys.stream().anyMatch { i -> i.isSimilar(result) } -> continue
                recipe is ShapedRecipe || recipe is ShapelessRecipe -> {
                    var canCraft = true
                    val requiredItems = CraftUtils.getRequiredItems(recipe)
                    for (item in requiredItems) {
                        if (!inventory.containsAtLeast(item, item.amount)) {
                            canCraft = false
                        }
                    }
                    if (canCraft) {
                        suggestions.put(result, CraftUtils.stackSimilarItems(requiredItems))
                    }
                }
            }
        }
        return suggestions
    }
}