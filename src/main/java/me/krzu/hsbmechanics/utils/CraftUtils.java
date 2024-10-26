package me.krzu.hsbmechanics.utils;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class CraftUtils {

    public final static TextComponent NO_RECIPE = Component.text("Recipe Required", NamedTextColor.RED)
            .decoration(TextDecoration.ITALIC, false);

    public final static List<Integer> CRAFT_SLOTS = List.of(10, 11, 12, 19, 20, 21, 28, 29, 30);
    public final static int RESULT_SLOT = 23;
    public final static int QUICK_CRAFT_COLUMN = 8;
    public final static int QUICK_CRAFT_FIRST_ROW = 2;
    public final static int QUICK_CRAFT_SLOTS = 3;
    public final static int QUICK_CRAFT_EXTRA_SLOT = 26;
    public final static int CLOSE_MENU_SLOT = 49;

    public final static ItemBuilder NO_RECIPE_ROW = ItemBuilder.from(Material.RED_STAINED_GLASS_PANE)
            .name(Component.text(""));
    public final static ItemBuilder READY_TO_CRAFT_ROW = ItemBuilder.from(Material.GREEN_STAINED_GLASS_PANE)
            .name(Component.text(""));
    public final static ItemBuilder FILL_BACKGROUND = ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE)
            .name(Component.text(""));
    public final static ItemBuilder CLOSE_MENU = ItemBuilder.from(Material.BARRIER)
            .name(Component.text("Close", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
    public final static ItemBuilder BACK_MENU = ItemBuilder.from(Material.ARROW)
            .name(Component.text("Go Back", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false))
            .lore(Component.text("To Craft Item", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));

    public final static ItemBuilder NO_RESULT_READY = ItemBuilder.from(Material.BARRIER)
            .name(NO_RECIPE)
            .lore(
                    Component.text("Add the items for a valid recipe in", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.text("the crafting grid to the left!", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            );
    public final static ItemBuilder QUICK_CRAFT_SLOT = ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE)
            .name(Component.text("Quick Crafting Slot", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false))
            .lore(
                    Component.text("Quick crafting allows you to craft", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.text("items without assembling the recipe.", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            );
    public final static ItemBuilder QUICK_CRAFT_MORE_SLOT = ItemBuilder.from(HSBUtils.createPlayerHead("b056bc1244fcff99344f12aba42ac23fee6ef6e3351d27d273c1572531f"))
            .name(Component.text("More Quick Craft Options", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));

    public static void addSuggestionLore (ItemStack item, int craftable, Set<ItemStack> ingredients) {
        List<Component> lore = new ArrayList<>(List.of(
                Component.text(""),
                Component.text("------------", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false).decorate(TextDecoration.STRIKETHROUGH),
                Component.text("Craftable: ", NamedTextColor.BLUE).decoration(TextDecoration.ITALIC, false)
                        .append(Component.text(craftable + "x", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false)),
                Component.text("Ingredients:", NamedTextColor.BLUE).decoration(TextDecoration.ITALIC, false)
        ));

        for (ItemStack ingredient : ingredients) {
            TranslatableComponent name = (TranslatableComponent) ingredient.getItemMeta().displayName();
            if (name == null) {
                name = Component.translatable(ingredient.getType().translationKey());
            }

            lore.add(
                    Component.text(ingredient.getAmount() + "x ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                            .append(name.color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
            );
        }

        lore.add(Component.text(""));
        lore.add(Component.text("Click to craft!", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        item.lore(lore);

    }

    public static Set<ItemStack> stackSimilarItems (List<ItemStack> items) {
        Set<ItemStack> stackedItems = new HashSet<>();

        for (ItemStack item : items) {
            if (item == null) continue;
            Optional<ItemStack> existing = stackedItems.stream().filter(stack -> stack.isSimilar(item)).findFirst();

            if (existing.isPresent()) {
                ItemStack existingStack = existing.get();
                stackedItems.remove(existingStack);
                stackedItems.add(existingStack.add(item.getAmount()));
            } else {
                stackedItems.add(item);
            }
        }

        return stackedItems;
    }

    public static int getAmountCraftable(Player player, Set<ItemStack> ingredients) {
        List<ItemStack> clonedItems = Arrays.stream(player.getInventory().getContents()).filter(Objects::nonNull).map(ItemStack::clone).toList();
        Set<ItemStack> clonedIngredients = new HashSet<>(ingredients);

        Set<ItemStack> stackedInventory = CraftUtils.stackSimilarItems(clonedItems);

        int amount = 0;
        boolean canKeepAdding = true;

        while (canKeepAdding) {
            boolean allIngredientsAvailable = true;

            for (ItemStack ingredient : clonedIngredients) {
                Optional<ItemStack> invStack = stackedInventory.stream().filter(item -> item.isSimilar(ingredient)).findFirst();
                if (invStack.isPresent()) {
                    ItemStack invStackData = invStack.get();

                    int ingredientAmount = ingredient.getAmount();
                    int invStackAmount = invStackData.getAmount();
                    if (ingredientAmount > invStackAmount) {
                        allIngredientsAvailable = false;
                        canKeepAdding = false;
                        break;
                    } else {
                        invStackData.setAmount(invStackAmount - ingredientAmount);
                        stackedInventory.remove(invStackData);
                        stackedInventory.add(invStackData);
                    }
                } else {
                    allIngredientsAvailable = false;
                    canKeepAdding = false;
                    break;
                }
            }

            if (allIngredientsAvailable) {
                ++amount;
            }
        }

        return amount;
    }

    public static List<ItemStack> getRequiredItems(Recipe recipe) {
        List<ItemStack> requiredItems = new ArrayList<>();

        if (recipe instanceof ShapelessRecipe) {
            for (RecipeChoice choice : ((ShapelessRecipe) recipe).getChoiceList()) {
                if (choice instanceof RecipeChoice.ExactChoice) {
                    requiredItems.addAll(((RecipeChoice.ExactChoice) choice).getChoices());
                } else {
                    requiredItems.addAll(((RecipeChoice.MaterialChoice) choice).getChoices().stream().map(ItemStack::new).toList());
                }
            }
        } else if (recipe instanceof ShapedRecipe shapedRecipe) {
            Map<Character, RecipeChoice> choiceMap = shapedRecipe.getChoiceMap();
            for (String shape : shapedRecipe.getShape()) {
                String[] shapes = shape.split("");
                for (String character : shapes) {
                    RecipeChoice choice = choiceMap.get(character.charAt(0));
                    if (choice == null) continue;

                    if (choice instanceof RecipeChoice.ExactChoice) {
                        requiredItems.addAll(List.of(((RecipeChoice.ExactChoice) choice).getChoices().toArray(ItemStack[]::new)));
                    } else {
                        requiredItems.addAll(((RecipeChoice.MaterialChoice) choice).getChoices().stream().map(ItemStack::new).toList());
                    }
                }
            }
        }

        return requiredItems;
    }
}
