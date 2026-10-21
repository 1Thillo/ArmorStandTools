package com.gmail.St3venAU.plugins.ArmorStandTools;

import org.bukkit.block.Shelf;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareInventoryResultEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.EnumSet;
import java.util.Set;

// Keeps the tools from leaving the tool inventory, every way out of it is a way to dupe them
class ToolProtectionListener implements Listener {

    private static final Set<InventoryAction> BUNDLE_ACTIONS = EnumSet.of(
            InventoryAction.PICKUP_FROM_BUNDLE,
            InventoryAction.PICKUP_ALL_INTO_BUNDLE,
            InventoryAction.PICKUP_SOME_INTO_BUNDLE,
            InventoryAction.PLACE_FROM_BUNDLE,
            InventoryAction.PLACE_ALL_INTO_BUNDLE,
            InventoryAction.PLACE_SOME_INTO_BUNDLE);

    private static boolean isEditing(HumanEntity p) {
        return AST.savedInventories.containsKey(p.getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        // Outside of tool mode a tool can only be a leftover from a dupe
        if (!isEditing(event.getWhoClicked())) {
            boolean removed = false;
            if (event.getClickedInventory() instanceof PlayerInventory && Utils.containsTool(event.getCurrentItem())) {
                event.setCurrentItem(Utils.withoutTools(event.getCurrentItem()));
                removed = true;
            }
            if (Utils.containsTool(event.getCursor())) {
                event.getView().setCursor(Utils.withoutTools(event.getCursor()));
                removed = true;
            }
            // Number keys and F move an item from the hotbar or off hand without it being clicked
            final PlayerInventory inventory = event.getWhoClicked().getInventory();
            final int swapSlot = event.getClick() == ClickType.SWAP_OFFHAND ? 40 : event.getHotbarButton();
            if (swapSlot >= 0 && Utils.containsTool(inventory.getItem(swapSlot))) {
                inventory.setItem(swapSlot, Utils.withoutTools(inventory.getItem(swapSlot)));
                removed = true;
            }
            if (removed) {
                event.setCancelled(true);
                return;
            }
        }
        if (BUNDLE_ACTIONS.contains(event.getAction())
                && (Utils.containsTool(event.getCurrentItem()) || Utils.containsTool(event.getCursor()))) {
            event.setCancelled(true);
            return;
        }
        // From the off hand or an armor slot a tool can be placed, worn or used outside the tool's own logic
        if (event.getClickedInventory() instanceof PlayerInventory && isEquipmentSlot(event.getSlot())
                && Utils.containsTool(event.getCursor())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        // Armor and off hand slots are only part of the player's own inventory view
        if (event.getView().getTopInventory().getType() != InventoryType.CRAFTING)
            return;
        if (!Utils.containsTool(event.getOldCursor()))
            return;
        for (int rawSlot : event.getRawSlots()) {
            if (isEquipmentSlot(event.getView().convertSlot(rawSlot)) && event.getView().getInventory(rawSlot) instanceof PlayerInventory) {
                event.setCancelled(true);
                return;
            }
        }
    }

    // Slots 36 to 39 hold the armor, 40 the off hand
    private static boolean isEquipmentSlot(int slot) {
        return slot >= 36 && slot <= 40;
    }

    // Item frames, allays and the like, with either hand. Armor stands get their own event
    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof ArmorStand)
            return;
        final Player p = event.getPlayer();
        if (Utils.containsTool(p.getInventory().getItem(event.getHand()))) {
            event.setCancelled(true);
            p.updateInventory();
        }
    }

    // The main hand is handled by MainListener, which turns the click into a tool action
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND && Utils.containsTool(event.getItem())) {
            event.setCancelled(true);
            event.getPlayer().updateInventory();
        }
    }

    // A shelf swaps its items with the player's hotbar on right click, no inventory involved
    @EventHandler(priority = EventPriority.LOWEST)
    public void onShelfInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null
                || !(event.getClickedBlock().getBlockData() instanceof org.bukkit.block.data.type.Shelf))
            return;
        if (isEditing(event.getPlayer())) {
            event.setCancelled(true);
            event.getPlayer().updateInventory();
            return;
        }
        // Tools left on a shelf by older versions would otherwise be swapped into the hotbar
        if (event.getClickedBlock().getState(false) instanceof final Shelf shelf) {
            removeTools(shelf.getInventory());
        }
    }

    // Hoppers and hopper minecarts, e.g. pulling from a shelf or a chest that still holds a tool
    @EventHandler(ignoreCancelled = true)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if (Utils.containsTool(event.getItem())) {
            event.setCancelled(true);
            removeTools(event.getSource());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryPickupItem(InventoryPickupItemEvent event) {
        if (removeTools(event.getItem()))
            event.setCancelled(true);
    }

    private static void removeTools(Inventory inventory) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            final ItemStack item = inventory.getItem(slot);
            if (Utils.containsTool(item)) {
                inventory.setItem(slot, Utils.withoutTools(item));
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        if (Utils.containsTool(event.getMainHandItem()) || Utils.containsTool(event.getOffHandItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof final Player p && isEditing(p))
            return;
        if (removeTools(event.getItem()))
            event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        if (removeTools(event.getEntity()))
            event.setCancelled(true);
    }

    // Returns true if nothing but tools was left in the item entity
    private static boolean removeTools(Item item) {
        final ItemStack stack = item.getItemStack();
        if (!Utils.containsTool(stack))
            return false;
        final ItemStack cleaned = Utils.withoutTools(stack);
        if (cleaned == null) {
            item.remove();
            return true;
        }
        item.setItemStack(cleaned);
        return false;
    }

    @EventHandler
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        for (ItemStack item : event.getInventory().getMatrix()) {
            if (Utils.containsTool(item)) {
                event.getInventory().setResult(null);
                return;
            }
        }
    }

    // Anvil, smithing table and grindstone
    @EventHandler
    public void onPrepareInventoryResult(PrepareInventoryResultEvent event) {
        for (ItemStack item : event.getInventory().getContents()) {
            if (Utils.containsTool(item)) {
                event.setResult(null);
                return;
            }
        }
    }

}
