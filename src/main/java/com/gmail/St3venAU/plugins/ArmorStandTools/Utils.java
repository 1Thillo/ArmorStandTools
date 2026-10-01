package com.gmail.St3venAU.plugins.ArmorStandTools;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CommandBlock;
import org.bukkit.block.ShulkerBox;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntitySnapshot;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.util.Vector;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Utils {

    private static DecimalFormat twoDec;

    static boolean hasDisabledSlots(ArmorStand as) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            for (ArmorStand.LockType lockType : ArmorStand.LockType.values()) {
                if (!as.hasEquipmentLock(slot, lockType)) {
                    return false;
                }
            }
        }
        return true;
    }

    static void setSlotsDisabled(ArmorStand as, boolean slotsDisabled) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            for (ArmorStand.LockType lockType : ArmorStand.LockType.values()) {
                if (slotsDisabled) {
                    as.addEquipmentLock(slot, lockType);
                } else {
                    as.removeEquipmentLock(slot, lockType);
                }
            }
        }
    }

    static boolean toggleSlotsDisabled(ArmorStand as) {
        final boolean slotsDisabled = !hasDisabledSlots(as);
        setSlotsDisabled(as, slotsDisabled);
        return slotsDisabled;
    }

    static boolean hasPermissionNode(Player p, String perm) {
        if ((p == null) || p.isOp()) {
            return true;
        }
        if (p.hasPermission(perm)) {
            return true;
        }
        final String[] nodes = perm.split("\\.");
        final StringBuilder n = new StringBuilder();
        for (int i = 0; i < (nodes.length - 1); i++) {
            n.append(nodes[i]).append(".");
            if (p.hasPermission(n + "*")) {
                return true;
            }
        }
        return false;
    }

    static void title(Player p, String msg) {
        p.sendTitle(" ", msg, 0, 70, 0);
    }

    public static Location getLocationFacing(Location loc) {
        final Location l = loc.clone();
        final Vector v = l.getDirection();
        v.setY(0);
        v.multiply(3);
        l.add(v);
        l.setYaw(l.getYaw() + 180);
        boolean ok = false;
        for (int n = 0; n < 5; n++) {
            if (l.getBlock().getType().isSolid()) {
                l.add(0, 1, 0);
            } else {
                ok = true;
                break;
            }
        }
        if (!ok) {
            l.subtract(0, 5, 0);
        }
        return l;
    }

    static boolean containsItems(Collection<ItemStack> items) {
        for (ItemStack i : items) {
            if (ArmorStandTool.get(i) != null) {
                return true;
            }
        }
        return false;
    }

    static String createSummonCommand(ArmorStand as) {
        final Location asLocation = as.getLocation();
        return "summon minecraft:armor_stand " +
                twoDec(asLocation.getX()) + " " +
                twoDec(asLocation.getY()) + " " +
                twoDec(asLocation.getZ()) + " " +
                createEntityTag(as);
    }

    static ItemStack createArmorStandItem(ArmorStand as) {
        final EntityEquipment equipment = as.getEquipment();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!canArmorStandItemContain(equipment.getItem(slot)))
                return null;
        }
        final ItemStack armorStand = Bukkit.getItemFactory().createItemStack("minecraft:armor_stand[minecraft:entity_data=" + createEntityTag(as) + "]");
        final ItemMeta meta = armorStand.getItemMeta();
        if (meta != null) {
            meta.setLore(createItemLore(as));
            meta.setDisplayName(Config.configuredArmorStand);
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        armorStand.setItemMeta(meta);
        return armorStand;
    }

    // Server-internal bookkeeping such as Paper.Origin, which would only point the copy at the original's position
    private static final Pattern SERVER_INTERNAL_TAGS = Pattern.compile("(?<=[{,])(?:Paper|Spigot|Bukkit)\\.\\w+:(?:\\[[^\\[\\]]*]|\"[^\"]*\"|[^,{}\\[\\]]+),?");

    // The server writes the entity data itself, so the tag always matches the running Minecraft version
    static String createEntityTag(ArmorStand as) {
        final EntitySnapshot snapshot = as.createSnapshot();
        if (snapshot == null)
            return "{id:\"minecraft:armor_stand\"}";
        return SERVER_INTERNAL_TAGS.matcher(snapshot.getAsString()).replaceAll("").replace(",}", "}");
    }

    static List<String> createItemLore(ArmorStand as) {
        final EntityEquipment e = as.getEquipment();
        final List<String> lore = new ArrayList<>();
        final String name = as.getCustomName();
        if (name != null && name.length() > 0) {
            lore.add(Config.name + ": " + name);
        }
        int stacks = 0;
        int items = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            final ItemStack item = e.getItem(slot);
            if (item.getType().isAir())
                continue;
            stacks++;
            items += item.getAmount();
        }
        if (stacks > 0) {
            lore.add(Config.inventory + ": " + ChatColor.YELLOW + items + " " + Config.items + " (" + stacks + " " + Config.stacks + ")");
        }
        final List<String> attribs = new ArrayList<>();
        if (hasDisabledSlots(as))
            attribs.add(Config.equip + ": " + Config.locked);
        if (!as.hasGravity())
            attribs.add(Config.gravity + ": " + Config.isOff);
        if (!as.isVisible())
            attribs.add(Config.invisible + ": " + Config.isOff);
        if (as.hasArms())
            attribs.add(Config.arms + ": " + Config.isOn);
        if (as.isSmall())
            attribs.add(Config.size + ": " + Config.small);
        if (as.isInvulnerable())
            attribs.add(Config.invuln + ": " + Config.isOn);
        if (as.isGlowing())
            attribs.add(Config.glowing + ": " + Config.isOn);
        if (attribs.size() > 0) {
            StringBuilder sb = new StringBuilder(Config.attributes + ": " + ChatColor.YELLOW);
            for (Iterator<String> iterator = attribs.iterator(); iterator.hasNext(); ) {
                final String attrib = iterator.next();
                sb.append(attrib);
                if (iterator.hasNext()) {
                    sb.append(ChatColor.GRAY).append(", ");
                }
                if (sb.length() >= 40) {
                    lore.add(sb.toString());
                    sb = new StringBuilder(ChatColor.YELLOW.toString());
                }
            }
            lore.add(sb.toString());
        }
        return lore;
    }

    static void generateCmdBlock(Location l, String command) {
        final Block b = l.getBlock();
        b.setType(Material.COMMAND_BLOCK);
        final CommandBlock cb = (CommandBlock) b.getState();
        cb.setCommand(command);
        cb.update();
    }

    static String twoDec(double d) {
        if (twoDec == null) {
            twoDec = new DecimalFormat("0.0#");
            final DecimalFormatSymbols symbols = new DecimalFormatSymbols();
            symbols.setDecimalSeparator('.');
            twoDec.setDecimalFormatSymbols(symbols);
        }
        return twoDec.format(d);
    }

    static boolean hasAnyTools(Player p) {
        for (ItemStack i : p.getInventory()) {
            if (ArmorStandTool.isTool(i)) {
                return true;
            }
        }
        return false;
    }

    private static boolean onCooldown(Entity e) {
        for (MetadataValue meta : e.getMetadata("lastEvent")) {
            if (AST.plugin.equals(meta.getOwningPlugin())) {
                return System.currentTimeMillis() - meta.asLong() < 100;
            }
        }
        return false;
    }

    static void cycleInventory(Player p) {
        if (onCooldown(p))
            return;
        final Inventory i = p.getInventory();
        for (int n = 0; n < 9; n++) {
            final ItemStack temp = i.getItem(n);
            i.setItem(n, i.getItem(27 + n));
            i.setItem(27 + n, i.getItem(18 + n));
            i.setItem(18 + n, i.getItem(9 + n));
            i.setItem(9 + n, temp);
        }
        p.updateInventory();
        p.setMetadata("lastEvent", new FixedMetadataValue(AST.plugin, System.currentTimeMillis()));
    }

    static ArmorStand cloneArmorStand(Player p, ArmorStand as) {
        final Inventory inventory = p.getInventory();
        final EntityEquipment asEquipment = as.getEquipment();
        final GameMode gameMode = p.getGameMode();
        if (gameMode == GameMode.ADVENTURE || gameMode == GameMode.SURVIVAL) {
            final List<ItemStack> required = Stream.of(
                            new ItemStack(Material.ARMOR_STAND),
                            asEquipment.getHelmet(),
                            asEquipment.getChestplate(),
                            asEquipment.getLeggings(),
                            asEquipment.getBoots(),
                            asEquipment.getItemInMainHand(),
                            asEquipment.getItemInOffHand()
                    )
                    .filter(itemStack -> itemStack != null && !itemStack.getType().isAir())
                    .toList();

            for (ItemStack itemStack : required) {
                if (!inventory.containsAtLeast(itemStack, itemStack.getAmount())) {
                    p.sendMessage(ChatColor.RED + Config.generalNoPerm);
                    return null;
                }
            }
            for (ItemStack itemStack : required) {
                inventory.removeItemAnySlot(itemStack);
            }
        }

        final ArmorStand clone = (ArmorStand) as.getWorld().spawnEntity(as.getLocation().add(1, 0, 0), EntityType.ARMOR_STAND);
        final EntityEquipment cloneEquipment = clone.getEquipment();
        cloneEquipment.setHelmet(asEquipment.getHelmet());
        cloneEquipment.setChestplate(asEquipment.getChestplate());
        cloneEquipment.setLeggings(asEquipment.getLeggings());
        cloneEquipment.setBoots(asEquipment.getBoots());
        cloneEquipment.setItemInMainHand(asEquipment.getItemInMainHand());
        cloneEquipment.setItemInOffHand(asEquipment.getItemInOffHand());
        clone.setHeadPose(as.getHeadPose());
        clone.setBodyPose(as.getBodyPose());
        clone.setLeftArmPose(as.getLeftArmPose());
        clone.setRightArmPose(as.getRightArmPose());
        clone.setLeftLegPose(as.getLeftLegPose());
        clone.setRightLegPose(as.getRightLegPose());
        clone.setGravity(as.hasGravity());
        clone.setVisible(as.isVisible());
        clone.setBasePlate(as.hasBasePlate());
        clone.setArms(as.hasArms());
        clone.setCustomName(as.getCustomName());
        clone.setCustomNameVisible(as.isCustomNameVisible());
        clone.setSmall(as.isSmall());
        clone.setInvulnerable(as.isInvulnerable());
        clone.setGlowing(as.isGlowing());
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            for (ArmorStand.LockType lockType : ArmorStand.LockType.values()) {
                if (as.hasEquipmentLock(slot, lockType)) {
                    clone.addEquipmentLock(slot, lockType);
                }
            }
        }
        final ArmorStandCmdManager cmdMgrAs = new ArmorStandCmdManager(as);
        final ArmorStandCmdManager cmdMgrClone = new ArmorStandCmdManager(clone);
        for (ArmorStandCmd asCmd : cmdMgrAs.getCommands()) {
            cmdMgrClone.addCommand(asCmd, true);
        }
        int cooldown = cmdMgrAs.getCooldownTime();
        if (cooldown > 0) {
            cmdMgrClone.setCooldownTime(cooldown);
        }
        clone.setMetadata("clone", new FixedMetadataValue(AST.plugin, true));
        return clone;
    }

    static boolean isConfiguredArmorStandItem(ItemStack item) {
        if (item.getType() != Material.ARMOR_STAND || !item.hasItemMeta())
            return false;
        return item.getItemMeta().getAsComponentString().contains("minecraft:entity_data=");
    }

    static boolean canArmorStandItemContain(ItemStack item) {
        return !isConfiguredArmorStandItem(item) &&
                !(item.getItemMeta() instanceof final BlockStateMeta meta && meta.getBlockState() instanceof ShulkerBox);
    }

}
