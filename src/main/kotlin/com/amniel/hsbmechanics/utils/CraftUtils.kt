package com.amniel.hsbmechanics.utils

import dev.triumphteam.gui.builder.item.ItemBuilder
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.Recipe
import org.bukkit.inventory.RecipeChoice
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.inventory.ShapelessRecipe
import java.util.Optional

class CraftUtils {
    companion object {
        val NO_RECIPE: TextComponent = Component.text("Recipe Required", NamedTextColor.RED)
            .decoration(TextDecoration.ITALIC, false)

        val CRAFT_SLOTS: List<Int> = listOf(10, 11, 12, 19, 20, 21, 28, 29, 30)
        const val RESULT_SLOT: Int = 23
        const val QUICK_CRAFT_COLUMN: Int = 8
        const val QUICK_CRAFT_FIRST_ROW: Int = 3
        const val QUICK_CRAFT_SLOTS: Int = 2
        const val QUICK_CRAFT_EXTRA_SLOT: Int = 26
        const val CLOSE_MENU_SLOT: Int = 49

        val NO_RECIPE_ROW: ItemBuilder = ItemBuilder.from(Material.RED_STAINED_GLASS_PANE)
            .name(Component.text(""))
        val READY_TO_CRAFT_ROW: ItemBuilder = ItemBuilder.from(Material.GREEN_STAINED_GLASS_PANE)
            .name(Component.text(""))
        val FILL_BACKGROUND: ItemBuilder = ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE)
            .name(Component.text(""))

        val CLOSE_MENU: ItemBuilder = ItemBuilder.from(Material.BARRIER)
            .name(Component.text("Close", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false))
        val BACK_MENU: ItemBuilder = ItemBuilder.from(Material.ARROW)
            .name(Component.text("Go Back", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false))
            .lore(Component.text("To Craft Item", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))

        val NO_RESULT_READY: ItemBuilder = ItemBuilder.from(Material.BARRIER)
            .name(NO_RECIPE)
            .lore(
                Component.text("Add the items for a valid recipe in", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false),
                Component.text("the crafting grid to the left!", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false)
            )
        val QUICK_CRAFT_SLOT: ItemBuilder = ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE)
            .name(Component.text("Quick Crafting Slot", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false))
            .lore(
                Component.text("Quick crafting allows you to craft", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false),
                Component.text("items without assembling the recipe.", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false)
            )
        val QUICK_CRAFT_MORE_SLOT: ItemBuilder =
            ItemBuilder.from(HSBUtils.createPlayerHead("b056bc1244fcff99344f12aba42ac23fee6ef6e3351d27d273c1572531f"))
                .name(
                    Component.text("More Quick Craft Options", NamedTextColor.GREEN)
                        .decoration(TextDecoration.ITALIC, false)
                )

        fun addSuggestionLore(item: ItemStack, craftable: Int, ingredients: Set<ItemStack>) {
            val lore: MutableList<Component> = mutableListOf(
                Component.text(""),
                Component.text("------------", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)
                    .decorate(TextDecoration.STRIKETHROUGH),
                Component.text("Craftable: ", NamedTextColor.BLUE).decoration(TextDecoration.ITALIC, false)
                    .append(
                        Component.text("${craftable}x", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false)
                    ),
                Component.text("Ingredients:", NamedTextColor.BLUE).decoration(TextDecoration.ITALIC, false)
            )

            for (ingredient in ingredients) {
                val name: Component =
                    ingredient.itemMeta.displayName() ?: Component.translatable(ingredient.type.translationKey())

                lore.add(
                    Component.text("${ingredient.amount}x", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
                        .append(name.color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                )
            }

            lore.add(Component.text(""))
            lore.add(Component.text("Click to craft!", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false))
            item.lore(lore)
        }

        fun stackSimilarItems(items: List<ItemStack?>): MutableSet<ItemStack> {
            val stackedItems: MutableSet<ItemStack> = HashSet()
            for (item in items) {
                if (item != null) {
                    val existing = stackedItems.stream().filter {
                        it.isSimilar(item)
                    }.findFirst()

                    if (existing.isPresent) {
                        val existingStack = existing.get()
                        stackedItems.remove(existingStack)
                        stackedItems.add(existingStack.add(item.amount))
                    } else {
                        stackedItems.add(item)
                    }
                }
            }
            return stackedItems
        }

        fun getAmountCraftable(player: Player, ingredients: Set<ItemStack>): Int {
            val clonedItems: List<ItemStack> = player.inventory.contents.filterNotNull()
                .map { it.clone() }
            val clonedIngredients: Set<ItemStack> = ingredients.toSet()
            val stackedInventory: MutableSet<ItemStack> = stackSimilarItems(clonedItems)

            var amount = 0
            var canKeepAdding = true

            while (canKeepAdding) {
                var allIngredientsAvailable = true
                for (ingredient in clonedIngredients) {
                    val invStack: Optional<ItemStack> =
                        stackedInventory.stream().filter { item -> item.isSimilar(ingredient) }.findFirst()
                    if (invStack.isPresent) {
                        val invStackData: ItemStack = invStack.get()

                        val ingredientAmount: Int = ingredient.amount
                        val invStackAmount: Int = invStackData.amount
                        if (ingredientAmount > invStackAmount) {
                            allIngredientsAvailable = false
                            canKeepAdding = false
                            break
                        } else {
                            invStackData.amount = invStackAmount - ingredientAmount
                            stackedInventory.remove(invStackData)
                            stackedInventory.add(invStackData)
                        }
                    } else {
                        allIngredientsAvailable = false
                        canKeepAdding = false
                        break
                    }
                }

                if (allIngredientsAvailable) {
                    ++amount
                }
            }
            return amount
        }

        fun getRequiredItems(recipe: Recipe): List<ItemStack> {
            val requiredItems: MutableList<ItemStack> = ArrayList()

            if (recipe is ShapelessRecipe) {
                for (choice in recipe.choiceList) {
                    if (choice is RecipeChoice.ExactChoice) {
                        requiredItems.addAll(choice.choices)
                    } else {
                        requiredItems.addAll(
                            (choice as RecipeChoice.MaterialChoice).choices.stream()
                                .map { t -> ItemStack(t) }.toList()
                        )
                    }
                }
            } else if (recipe is ShapedRecipe) {
                val choiceMap = recipe.choiceMap
                for (shape in recipe.shape) {
                    val shapes = shape.split("")
                    for (character in shapes) {
                        if (character.isEmpty()) continue

                        val choice = choiceMap[character[0]]
                        if (choice == null) continue

                        if (choice is RecipeChoice.ExactChoice) {
                            requiredItems.addAll(choice.choices.toTypedArray().toList())
                        } else {
                            requiredItems.addAll(
                                (choice as RecipeChoice.MaterialChoice).choices.stream()
                                    .map { t -> ItemStack(t) }.toList()
                            )
                        }
                    }
                }
            }
            return requiredItems
        }
    }
}
