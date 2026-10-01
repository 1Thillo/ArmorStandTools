package com.gmail.St3venAU.plugins.ArmorStandTools;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;

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

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (BUNDLE_ACTIONS.contains(event.getAction())
                && (Utils.containsTool(event.getCurrentItem()) || Utils.containsTool(event.getCursor()))) {
            event.setCancelled(true);
        }
    }

}
