package me.reno.morningstar;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public final class MorningStar extends JavaPlugin implements Listener, CommandExecutor {

    private final String OWNER = "Yue_47";

    private final Map<UUID, Integer> combo = new HashMap<>();
    private final Map<UUID, Long> fury = new HashMap<>();

    private NamespacedKey key;

    @Override
    public void onEnable() {

        key = new NamespacedKey(this, "morningstar");

        getServer().getPluginManager().registerEvents(this, this);

        if (getCommand("morningstar") != null) {
            getCommand("morningstar").setExecutor(this);
        }

        getLogger().info("MorningStar enabled!");
        getLogger().info("Owner: " + OWNER);
    }

    // =========================
    // MORNINGSTAR ITEM
    // =========================

    private ItemStack createMorningStar() {

        ItemStack sword =
                new ItemStack(Material.GOLDEN_SWORD);

        ItemMeta meta =
                sword.getItemMeta();

        if (meta == null)
            return sword;

        meta.setDisplayName(
                ChatColor.GOLD + "" +
                ChatColor.BOLD +
                "MorningStar"
        );

        meta.setUnbreakable(true);

        // Sharpness V
        meta.addEnchant(
                Enchantment.SHARPNESS,
                5,
                true
        );

        meta.getPersistentDataContainer().set(
                key,
                PersistentDataType.BYTE,
                (byte) 1
        );

        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Legendary MorningStar",
                "",
                ChatColor.YELLOW + "4 Block Reach",
                ChatColor.YELLOW + "2x Attack Speed",
                ChatColor.YELLOW + "1.5x Damage",
                ChatColor.YELLOW + "Sharpness V",
                ChatColor.YELLOW + "Shield Break",
                ChatColor.YELLOW + "4 Crits = Critical Fury",
                ChatColor.YELLOW + "Critical Fury: 15 seconds",
                "",
                ChatColor.RED + "Owner: " + OWNER
        ));

        sword.setItemMeta(meta);

        return sword;
    }

    // =========================
    // CHECK WEAPON
    // =========================

    private boolean isMorningStar(ItemStack item) {

        if (item == null)
            return false;

        if (item.getType() != Material.GOLDEN_SWORD)
            return false;

        if (!item.hasItemMeta())
            return false;

        return item.getItemMeta()
                .getPersistentDataContainer()
                .has(
                        key,
                        PersistentDataType.BYTE
                );
    }

    private boolean isOwner(Player player) {

        return player.getName()
                .equalsIgnoreCase(OWNER);
    }

    private boolean holdingMorningStar(Player player) {

        return isOwner(player)
                && isMorningStar(
                player.getInventory()
                        .getItemInMainHand()
        );
    }

    // =========================
    // COMBAT
    // =========================

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onDamage(
            EntityDamageByEntityEvent event
    ) {

        if (!(event.getDamager()
                instanceof Player player))
            return;

        if (!holdingMorningStar(player))
            return;

        // =========================
        // CRITICAL FURY
        // =========================

        if (isFuryActive(player)) {

            event.setCritical(true);

            return;
        }

        // =========================
        // NORMAL CRITICAL
        // =========================

        if (isCritical(player)) {

            int hits =
                    combo.getOrDefault(
                            player.getUniqueId(),
                            0
                    ) + 1;

            if (hits >= 4) {

                combo.remove(
                        player.getUniqueId()
                );

                // 15 seconds
                fury.put(
                        player.getUniqueId(),
                        System.currentTimeMillis()
                                + 15000
                );

                // CHAT MESSAGE
                player.sendMessage(
                        ChatColor.GOLD +
                        "" + ChatColor.BOLD +
                        "✦ MORNINGSTAR ✦"
                );

                player.sendMessage(
                        ChatColor.YELLOW +
                        "" + ChatColor.BOLD +
                        "CRITICAL FURY ACTIVATED!"
                );

                player.sendMessage(
                        ChatColor.WHITE +
                        "All normal attacks are now CRITICAL!"
                );

                player.sendMessage(
                        ChatColor.GRAY +
                        "Duration: 15 seconds"
                );

                // PARTICLES
                player.getWorld().spawnParticle(
                        Particle.CRIT,
                        player.getLocation()
                                .add(0, 1, 0),
                        60,
                        0.6,
                        1.0,
                        0.6,
                        0.15
                );

                player.getWorld().spawnParticle(
                        Particle.ENCHANT,
                        player.getLocation()
                                .add(0, 1, 0),
                        40,
                        0.5,
                        1.0,
                        0.5,
                        1
                );

                // SOUND
                player.playSound(
                        player.getLocation(),
                        Sound.ENTITY_PLAYER_ATTACK_CRIT,
                        1.0f,
                        0.6f
                );

                player.playSound(
                        player.getLocation(),
                        Sound.BLOCK_BEACON_ACTIVATE,
                        1.0f,
                        1.5f
                );

            } else {

                combo.put(
                        player.getUniqueId(),
                        hits
                );

                player.sendActionBar(
                        ChatColor.GOLD +
                        "Critical Combo: " +
                        ChatColor.WHITE +
                        hits +
                        "/4"
                );
            }

        } else {

            // Failed critical resets combo
            combo.remove(
                    player.getUniqueId()
            );
        }

        // =========================
        // SHIELD BREAK
        // =========================

        if (event.getEntity()
                instanceof Player target) {

            if (target.isBlocking()) {

                // 3 seconds
                target.setCooldown(
                        Material.SHIELD,
                        60
                );

                target.sendMessage(
                        ChatColor.RED +
                        "Your shield was disabled by MorningStar!"
                );

                target.getWorld().spawnParticle(
                        Particle.CRIT,
                        target.getLocation()
                                .add(0, 1, 0),
                        20,
                        0.3,
                        0.5,
                        0.3,
                        0.1
                );
            }
        }
    }

    // =========================
    // CRITICAL DETECTION
    // =========================

    private boolean isCritical(Player player) {

        return !player.isOnGround()
                && player.getFallDistance() > 0
                && !player.isSprinting()
                && !player.isFlying()
                && !player.isSwimming()
                && !player.isClimbing()
                && !player.isInsideVehicle();
    }

    // =========================
    // CRITICAL FURY
    // =========================

    private boolean isFuryActive(Player player) {

        Long end =
                fury.get(player.getUniqueId());

        if (end == null)
            return false;

        if (System.currentTimeMillis() >= end) {

            fury.remove(
                    player.getUniqueId()
            );

            player.sendActionBar(
                    ChatColor.GRAY +
                    "Critical Fury ended."
            );

            return false;
        }

        return true;
    }

    // =========================
    // DROP PROTECTION
    // =========================

    @EventHandler(
            priority = EventPriority.HIGHEST
    )
    public void onDrop(
            PlayerDropItemEvent event
    ) {

        if (isMorningStar(
                event.getItemDrop()
                        .getItemStack()
        )) {

            event.setCancelled(true);

            event.getPlayer().sendMessage(
                    ChatColor.RED +
                    "MorningStar cannot be dropped!"
            );
        }
    }

    // =========================
    // PICKUP PROTECTION
    // =========================

    @EventHandler(
            priority = EventPriority.HIGHEST
    )
    public void onPickup(
            EntityPickupItemEvent event
    ) {

        if (!(event.getEntity()
                instanceof Player player))
            return;

        if (!isMorningStar(
                event.getItem()
                        .getItemStack()
        ))
            return;

        // Only Yue_47 can pick it up
        if (!isOwner(player)) {

            event.setCancelled(true);

            event.getItem().remove();

            return;
        }

        // Prevent duplicate copy
        for (ItemStack item :
                player.getInventory()
                        .getContents()) {

            if (isMorningStar(item)) {

                event.setCancelled(true);

                event.getItem().remove();

                player.sendMessage(
                        ChatColor.RED +
                        "Duplicate MorningStar removed!"
                );

                return;
            }
        }
    }

    // =========================
    // DEATH PROTECTION
    // =========================

    @EventHandler
    public void onDeath(
            PlayerDeathEvent event
    ) {

        if (!isOwner(event.getEntity()))
            return;

        event.getDrops().removeIf(
                this::isMorningStar
        );
    }

    // =========================
    // INVENTORY PROTECTION
    // =========================

    @EventHandler(
            priority = EventPriority.HIGHEST
    )
    public void onInventoryClick(
            InventoryClickEvent event
    ) {

        ItemStack current =
                event.getCurrentItem();

        ItemStack cursor =
                event.getCursor();

        if (!isMorningStar(current)
                && !isMorningStar(cursor))
            return;

        // Prevent putting it into containers
        if (event.getClickedInventory() != null
                && event.getClickedInventory()
                != event.getWhoClicked()
                .getInventory()) {

            event.setCancelled(true);
        }
    }

    // =========================
    // COMMAND
    // =========================

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        if (!(sender instanceof Player player))
            return true;

        // /morningstar
        if (args.length == 0) {

            sendHelp(player);

            return true;
        }

        // /morningstar give
        if (args[0].equalsIgnoreCase("give")) {

            if (!isOwner(player)) {

                player.sendMessage(
                        ChatColor.RED +
                        "You are not the owner!"
                );

                return true;
            }

            // Anti duplicate
            for (ItemStack item :
                    player.getInventory()
                            .getContents()) {

                if (isMorningStar(item)) {

                    player.sendMessage(
                            ChatColor.YELLOW +
                            "You already have MorningStar!"
                    );

                    return true;
                }
            }

            player.getInventory()
                    .addItem(
                            createMorningStar()
                    );

            player.sendMessage(
                    ChatColor.GOLD +
                    "MorningStar received!"
            );

            return true;
        }

        // /morningstar status
        if (args[0].equalsIgnoreCase("status")) {

            int hits =
                    combo.getOrDefault(
                            player.getUniqueId(),
                            0
                    );

            boolean active =
                    isFuryActive(player);

            player.sendMessage(
                    ChatColor.GOLD +
                    "===== MorningStar ====="
            );

            player.sendMessage(
                    ChatColor.YELLOW +
                    "Owner: " +
                    ChatColor.WHITE +
                    OWNER
            );

            player.sendMessage(
                    ChatColor.YELLOW +
                    "Critical Combo: " +
                    ChatColor.WHITE +
                    hits +
                    "/4"
            );

            player.sendMessage(
                    ChatColor.YELLOW +
                    "Critical Fury: " +
                    (active
                            ? ChatColor.GREEN +
                            "ACTIVE"
                            : ChatColor.RED +
                            "INACTIVE")
            );

            return true;
        }

        sendHelp(player);

        return true;
    }

    private void sendHelp(Player player) {

        player.sendMessage(
                ChatColor.GOLD +
                "===== MorningStar ====="
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "/morningstar give"
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "/morningstar status"
        );
    }
  }
