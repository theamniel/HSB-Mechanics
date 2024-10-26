package me.krzu.hsbmechanics.inventories.crafting;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import me.krzu.hsbmechanics.HSBMechanics;
import me.krzu.hsbmechanics.interfaces.AbstractMenu;
import me.krzu.hsbmechanics.utils.CraftUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class CraftSuggestionsMenu implements AbstractMenu {

    private final static ItemBuilder PREVIOUS_PAGE = ItemBuilder.from(Material.ARROW).name(Component.text("Previous Page", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
    private final static ItemBuilder NEXT_PAGE = ItemBuilder.from(Material.ARROW).name(Component.text("Next Page", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));

    private final HSBMechanics plugin;
    private final CraftMenu craftMenu;

    public CraftSuggestionsMenu(HSBMechanics plugin, CraftMenu craftMenu) {
        this.plugin = plugin;
        this.craftMenu = craftMenu;
    }

    @Override
    public void build(Player player) {
        PaginatedGui gui = Gui.paginated()
                .title(Component.text("Quick Crafting"))
                .rows(6)
                .pageSize(28)
                .create();

        gui.setDefaultClickAction(this::onClick);
        gui.setOpenGuiAction(this::onOpen);

        gui.open(player);
    }

    private void onClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        InventoryHolder holder = inventory.getHolder();
        if (!(holder instanceof PaginatedGui gui)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        new BukkitRunnable() {
            @Override
            public void run() {
                updateSuggestions(player, gui);
            }
        }.runTaskAsynchronously(plugin);

        Inventory clickedInv = event.getClickedInventory();
        if (clickedInv == null) {
            return;
        }

        if (clickedInv.equals(inventory)) {
            event.setCancelled(true);
        }
    }

    private void onOpen(InventoryOpenEvent event) {
        Inventory inventory = event.getInventory();
        InventoryHolder holder = inventory.getHolder();
        if (!(holder instanceof PaginatedGui gui)) {
            return;
        }

        gui.getFiller().fillBorder(CraftUtils.FILL_BACKGROUND.asGuiItem());
        gui.setItem(49, CraftUtils.BACK_MENU.asGuiItem(action -> craftMenu.build((Player) action.getWhoClicked())));

        new BukkitRunnable() {
            @Override
            public void run() {
                Player player = (Player) event.getPlayer();
                updateSuggestions(player, gui);
            }
        }.runTask(plugin);
    }

    private void updateSuggestions(Player player, PaginatedGui gui) {
        gui.clearPageItems(false);
        Map<ItemStack, Set<ItemStack>> suggestions = getCraftableItems(player);

        gui.addItem(suggestions.entrySet().stream().map(item -> formatItemSuggestion(player, item)).toArray(GuiItem[]::new));

        int currentPage = gui.getCurrentPageNum();
        int pages = gui.getPagesNum();
        if (currentPage > pages) {
            gui.setPageNum(pages);
        }

        if (currentPage > 1 && pages > 1) {
            gui.setItem(6, 1, PREVIOUS_PAGE.lore(
                    Component.text("Page " + (currentPage - 1), NamedTextColor.YELLOW)
                            .decoration(TextDecoration.ITALIC, false)).asGuiItem(ignored -> gui.previous()));
        } else {
            gui.setItem(6, 1, CraftUtils.FILL_BACKGROUND.asGuiItem());
        }

        if (currentPage < pages && pages > 1) {
            gui.setItem(6, 9, NEXT_PAGE.lore(
                    Component.text("Page " + (currentPage + 1), NamedTextColor.YELLOW)
                            .decoration(TextDecoration.ITALIC, false)).asGuiItem(ignored -> gui.next()
            ));
        } else {
            gui.setItem(6, 9, CraftUtils.FILL_BACKGROUND.asGuiItem());
        }

        gui.update();
    }

    public GuiItem formatItemSuggestion(Player player, Map.Entry<ItemStack, Set<ItemStack>> suggestion) {
        ItemStack item = suggestion.getKey();
        Set<ItemStack> ingredients = suggestion.getValue();
        int craftable = CraftUtils.getAmountCraftable(player, ingredients);

        ItemStack clonedItem = item.clone();
        CraftUtils.addSuggestionLore(item, craftable, ingredients);
        GuiItem guiItem = ItemBuilder.from(item).asGuiItem();
        guiItem.setAction(action -> this.craftSuggestion(action, player, clonedItem, ingredients));

        return guiItem;
    }

    public void craftSuggestion(InventoryClickEvent action, Player player, ItemStack suggestion, Set<ItemStack> ingredients) {
        PlayerInventory inventory = player.getInventory();
        Set<ItemStack> inventoryContent = CraftUtils.stackSimilarItems(Arrays.stream(inventory.getContents()).filter(Objects::nonNull).map(ItemStack::clone).toList());
        action.setCancelled(true);

        boolean canGiveItem = true;
        boolean canStackOnCursor = true;

        ItemStack currentCursor = action.getCursor();
        if (!currentCursor.isEmpty()) {
            if (!currentCursor.isSimilar(suggestion)) {
                canStackOnCursor = false;
            } else if ((currentCursor.getAmount() + suggestion.getAmount()) > currentCursor.getMaxStackSize()) {
                canStackOnCursor = false;
            }
        }

        if (!canStackOnCursor) {
            return;
        }

        for (ItemStack ingredient : ingredients) {
            ItemStack inventoryItem = inventoryContent.stream().filter(item -> item != null && item.isSimilar(ingredient)).findFirst().orElse(null);

            if (inventoryItem == null || inventoryItem.getAmount() < ingredient.getAmount()) {
                canGiveItem = false;
                break;
            }

            inventory.removeItem(ingredient);
        }

        if (!canGiveItem) {
            return;
        }

        if (currentCursor.isEmpty()) {
            action.setCursor(suggestion);
        } else {
            action.setCursor(currentCursor.add(suggestion.getAmount()));
        }
    }

    public Map<ItemStack, Set<ItemStack>> getCraftableItems(Player player) {
        PlayerInventory playerInventory = player.getInventory();

        Iterator<Recipe> iterator = plugin.getServer().recipeIterator();
        Map<ItemStack, Set<ItemStack>> suggestions = new HashMap<>();

        while (iterator.hasNext()) {
            Recipe recipe = iterator.next();
            ItemStack result = recipe.getResult();
            if (suggestions.keySet().stream().anyMatch(i -> i.isSimilar(result))) {
                continue;
            }

            if (!(recipe instanceof ShapedRecipe) && !(recipe instanceof ShapelessRecipe)) continue;

            boolean canCraft = true;
            List<ItemStack> requiredItems = CraftUtils.getRequiredItems(recipe);
            for (ItemStack item : requiredItems) {
                if (!playerInventory.containsAtLeast(item, item.getAmount())) {
                    canCraft = false;
                }
            }

            if (canCraft) {
                suggestions.put(result, CraftUtils.stackSimilarItems(requiredItems));
            }
        }

        return suggestions;
    }
}
