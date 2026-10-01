package com.gmail.St3venAU.plugins.ArmorStandTools;

import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareInventoryResultEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
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
            if (removed) {
                event.setCancelled(true);
                return;
            }
        }
        if (BUNDLE_ACTIONS.contains(event.getAction())
                && (Utils.containsTool(event.getCurrentItem()) || Utils.containsTool(event.getCursor()))) {
            event.setCancelled(true);
        }
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
