package me.krzu.hsbmechanics.inventories.crafting;

import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.krzu.hsbmechanics.HSBMechanics;
import me.krzu.hsbmechanics.interfaces.AbstractMenu;
import me.krzu.hsbmechanics.utils.CraftUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentBuilder;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.Recipe;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

public class CraftMenu implements AbstractMenu {

    private final HSBMechanics plugin;
    private final CraftSuggestionsMenu craftSuggestionsMenu;

    public CraftMenu(HSBMechanics plugin) {
        this.plugin = plugin;
        this.craftSuggestionsMenu = new CraftSuggestionsMenu(plugin, this);
    }

    @Override
    public void build(Player player) {
        Gui gui = Gui.gui()
                .title(Component.text("Enhanced Crafting Table"))
                .rows(6)
                .create();

        gui.setOpenGuiAction(this::onOpen);
        gui.setCloseGuiAction(this::onClose);
        gui.setDefaultClickAction(this::onClick);
        gui.setDragAction(event -> checkValidRecipes(player.getWorld(), event.getInventory().getHolder()));

        gui.open(player);
    }

    private void onOpen(InventoryOpenEvent event) {
        Inventory inventory = event.getInventory();
        InventoryHolder holder = inventory.getHolder();
        if (!(holder instanceof Gui gui)) {
            return;
        }

        gui.getFiller().fillBetweenPoints(0, 0, 5, 9, CraftUtils.FILL_BACKGROUND.asGuiItem());
        gui.getFiller().fillBetweenPoints(
                CraftUtils.QUICK_CRAFT_FIRST_ROW, CraftUtils.QUICK_CRAFT_COLUMN,
                CraftUtils.QUICK_CRAFT_FIRST_ROW + CraftUtils.QUICK_CRAFT_SLOTS - 1,
                CraftUtils.QUICK_CRAFT_COLUMN, CraftUtils.QUICK_CRAFT_SLOT.asGuiItem()
        );
        clearResult(gui);
        gui.update();

        Player player = (Player) event.getPlayer();
        new BukkitRunnable() {
            @Override
            public void run() {
                checkRecipeSuggestions(player, gui);
            }
        }.runTaskAsynchronously(plugin);
    }

    private void onClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        InventoryHolder holder = inventory.getHolder();
        if (!(holder instanceof Gui)) {
            return;
        }

        Player player = (Player) event.getPlayer();
        PlayerInventory playerInv = player.getInventory();

        List<ItemStack> itemsFailedToAdd = new ArrayList<>();
        for (int slot : CraftUtils.CRAFT_SLOTS) {
            ItemStack item = inventory.getItem(slot);

            if (item != null && item.getType() != Material.AIR) {
                itemsFailedToAdd.addAll(playerInv.addItem(item).values());
            }
        }

        if (!itemsFailedToAdd.isEmpty()) {
            Location playerLocation = player.getLocation();
            World playerWorld = playerLocation.getWorld();

            for (ItemStack item : itemsFailedToAdd) {
                playerWorld.dropItemNaturally(playerLocation, item);
            }
        }
    }

    private void onClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        InventoryHolder holder = inventory.getHolder();
        if (!(holder instanceof Gui gui)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();

        int slot = event.getSlot();
        if (slot == CraftUtils.CLOSE_MENU_SLOT && item != null && item.getType() == Material.BARRIER) {
            gui.close(player);
            event.setCancelled(true);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                checkRecipeSuggestions(player, gui);
            }
        }.runTaskAsynchronously(plugin);

        Inventory clickedInv = event.getClickedInventory();
        if (clickedInv == null) {
            return;
        }

        if (slot != CraftUtils.RESULT_SLOT && !CraftUtils.CRAFT_SLOTS.contains(slot) && clickedInv.equals(inventory)) {
            event.setCancelled(true);
            return;
        }

        World world = player.getWorld();

        if (slot == CraftUtils.RESULT_SLOT && clickedInv.equals(inventory)) {
            if (item != null && item.getType() != Material.BARRIER) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        craftRecipe(event, world, gui);
                    }
                }.runTaskAsynchronously(plugin);
            }

            event.setCancelled(true);
        } else {
            new BukkitRunnable() {
                @Override
                public void run() {
                    checkValidRecipes(world, gui);
                }
            }.runTaskAsynchronously(plugin);
        }
    }

    private void craftRecipe(InventoryClickEvent event, World world, Gui gui) {
        Inventory inventory = gui.getInventory();
        ItemStack[] items = CraftUtils.CRAFT_SLOTS.stream().map(inventory::getItem).toArray(ItemStack[]::new);
        Server server = plugin.getServer();

        Recipe recipe = server.getCraftingRecipe(items, world);
        if (recipe == null) {
            return;
        }

        ItemStack[] newMatrix = new ItemStack[9];
        List<ItemStack> requiredItems = CraftUtils.getRequiredItems(recipe);

        for (int i = 0; i < items.length; i++) {
            ItemStack item = items[i];
            if (item == null || item.getType() == Material.AIR) {
                newMatrix[i] = ItemStack.of(Material.AIR);
                continue;
            }

            Stream<ItemStack> itemStream = requiredItems.stream().filter(requiredItem -> requiredItem.isSimilar(item));
            if (itemStream.findFirst().isPresent()) {
                newMatrix[i] = item.asQuantity(item.getAmount() - 1);

                for (int j = 0; j < requiredItems.size(); j++) {
                    ItemStack requiredItem = requiredItems.get(j);
                    if (requiredItem.isSimilar(item)) {
                        requiredItems.remove(j);
                        break;
                    }
                }
            }
        }

        ItemStack currentCursor = event.getCursor();
        ItemStack result = recipe.getResult();
        if (currentCursor.isEmpty()) {
            event.setCursor(result);
        } else {
            if (!currentCursor.isSimilar(result)) return;
            if ((currentCursor.getAmount() + result.getAmount()) >= currentCursor.getMaxStackSize()) return;
            currentCursor.setAmount(currentCursor.getAmount() + result.getAmount());
        }
        clearResult(gui, false);

        int i2 = 0;
        for (int j : CraftUtils.CRAFT_SLOTS) {
            inventory.setItem(j, newMatrix[i2]);
            ++i2;
        }

        checkValidRecipes(world, gui);
    }

    private void checkRecipeSuggestions(Player player, Gui gui) {
        Inventory inventory = gui.getInventory();
        Map<ItemStack, Set<ItemStack>> suggestions = craftSuggestionsMenu.getCraftableItems(player);

        int totalSuggestions = suggestions.size();
        Iterator<Map.Entry<ItemStack, Set<ItemStack>>> suggestionIterator = suggestions.entrySet().iterator();
        for (int i = 0; i < CraftUtils.QUICK_CRAFT_SLOTS; i++) {
            int slot = (9 * (CraftUtils.QUICK_CRAFT_FIRST_ROW + i)) - (10 - CraftUtils.QUICK_CRAFT_COLUMN);
            inventory.setItem(slot, CraftUtils.QUICK_CRAFT_SLOT.build());

            if (totalSuggestions > i) {
                while (suggestionIterator.hasNext()) {
                    Map.Entry<ItemStack, Set<ItemStack>> suggestionEntry = suggestionIterator.next();

                    if (suggestionEntry != null && suggestionEntry.getKey() != null) {
                        Set<ItemStack> ingredients = suggestionEntry.getValue();
                        int craftable = CraftUtils.getAmountCraftable(player, ingredients);
                        if (craftable <= 0) {
                            --totalSuggestions;
                            continue;
                        }

                        GuiItem suggestionStackGui = craftSuggestionsMenu.formatItemSuggestion(player, suggestionEntry);
                        gui.updateItem(slot, suggestionStackGui);
                    }

                    break;
                }
            }
        }

        if (totalSuggestions > 3) {
            int extraSuggestions = totalSuggestions - 3;
            ComponentBuilder<TextComponent, TextComponent.Builder> firstLine = Component.text();

            if (extraSuggestions == 1) {
                firstLine.append(
                        Component.text("There is ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                        Component.text("1", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false),
                        Component.text(" more recipe that you", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                );
            } else {
                firstLine.append(
                        Component.text("There are ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                        Component.text(extraSuggestions, NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false),
                        Component.text(" more recipes that you", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                );
            }

            CraftUtils.QUICK_CRAFT_MORE_SLOT.lore(List.of(
                    firstLine.build(),
                    Component.text("may craft!", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text(""),
                    Component.text("Click to view them!", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
            ));
            gui.updateItem(CraftUtils.QUICK_CRAFT_EXTRA_SLOT, CraftUtils.QUICK_CRAFT_MORE_SLOT.asGuiItem(action -> craftSuggestionsMenu.build(player)));
        } else {
            gui.updateItem(CraftUtils.QUICK_CRAFT_EXTRA_SLOT, CraftUtils.FILL_BACKGROUND.asGuiItem());
        }
    }

    private void checkValidRecipes(World world, InventoryHolder holder) {
        if (!(holder instanceof Gui gui)) {
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                checkValidRecipes(world, gui);
            }
        }.runTaskAsynchronously(plugin);
    }

    private void checkValidRecipes(World world, Gui gui) {
        Inventory inventory = gui.getInventory();
        ItemStack[] items = CraftUtils.CRAFT_SLOTS.stream().map(inventory::getItem).toArray(ItemStack[]::new);
        Recipe recipe = plugin.getServer().getCraftingRecipe(items, world);

        if (recipe != null) {
            addCraftResult(gui, recipe.getResult());
        } else {
            clearResult(gui, false);
        }
    }

    private void addCraftResult(Gui gui, ItemStack result) {
        for (int i = 0; i < 9; i++) {
            gui.updateItem(6, i + 1, CraftUtils.READY_TO_CRAFT_ROW.asGuiItem());
        }
        gui.updateItem(CraftUtils.CLOSE_MENU_SLOT, CraftUtils.CLOSE_MENU.asGuiItem());

        List<Component> oldLore = new ArrayList<>(Objects.requireNonNullElse(result.lore(), new ArrayList<>()));
        oldLore.add(Component.text("------------", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false).decorate(TextDecoration.STRIKETHROUGH));
        oldLore.add(Component.text("This is the item you are crafting.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));

        result.lore(oldLore);
        gui.updateItem(CraftUtils.RESULT_SLOT, result);
    }

    private void clearResult(Gui gui) {
        clearResult(gui, true);
    }

    private void clearResult(Gui gui, boolean clearCraft) {
        if (clearCraft) {
            for (int slot : CraftUtils.CRAFT_SLOTS) {
                gui.removeItem(slot);
            }
        }

        for (int i = 0; i < 9; i++) {
            gui.updateItem(6, i + 1, CraftUtils.NO_RECIPE_ROW.asGuiItem());
        }

        gui.updateItem(CraftUtils.CLOSE_MENU_SLOT, CraftUtils.CLOSE_MENU.asGuiItem());
        gui.updateItem(CraftUtils.RESULT_SLOT, CraftUtils.NO_RESULT_READY.asGuiItem());
        gui.updateItem(CraftUtils.RESULT_SLOT - 1, CraftUtils.FILL_BACKGROUND.asGuiItem());
    }
}
