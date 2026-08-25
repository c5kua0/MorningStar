package me.reno.morningstar;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public final class MorningStar extends JavaPlugin
        implements Listener, CommandExecutor {

    private static final String OWNER = "Yue_47";

    private final Map<UUID, Integer> combo = new HashMap<>();
    private final Map<UUID, Long> fury = new HashMap<>();

    private NamespacedKey key;
    private BukkitTask auraTask;

    @Override
    public void onEnable() {

        key = new NamespacedKey(this, "morningstar");

        getServer()
                .getPluginManager()
                .registerEvents(this, this);

        if (getCommand("morningstar") != null) {
            getCommand("morningstar")
                    .setExecutor(this);
        }

        getLogger().info("================================");
        getLogger().info("       MORNINGSTAR ENABLED");
        getLogger().info("       Owner: " + OWNER);
        getLogger().info("================================");
    }

    @Override
    public void onDisable() {

        if (auraTask != null) {
            auraTask.cancel();
            auraTask = null;
        }
    }

    // =========================
    // CREATE MORNINGSTAR
    // =========================

    private ItemStack createMorningStar() {

        ItemStack sword =
                new ItemStack(Material.GOLDEN_SWORD);

        ItemMeta meta = sword.getItemMeta();

        if (meta == null) {
            return sword;
        }

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

        // Unique ID
        meta.getPersistentDataContainer().set(
                key,
                PersistentDataType.BYTE,
                (byte) 1
        );

        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "The legendary MorningStar.",
                "",
                ChatColor.YELLOW + "4 Block Reach",
                ChatColor.YELLOW + "2x Attack Speed",
                ChatColor.YELLOW + "1.5x Damage",
                ChatColor.YELLOW + "Sharpness V",
                ChatColor.YELLOW + "Shield Break",
                ChatColor.YELLOW + "4 Critical Hits",
                ChatColor.YELLOW + "Critical Fury",
                ChatColor.YELLOW + "Duration: 15 seconds",
                "",
                ChatColor.RED + "Owner: " + OWNER
        ));

        sword.setItemMeta(meta);

        return sword;
    }

    // =========================
    // CHECK MORNINGSTAR
    // =========================

    private boolean isMorningStar(ItemStack item) {

        if (item == null) {
            return false;
        }

        if (item.getType() != Material.GOLDEN_SWORD) {
            return false;
        }

        if (!item.hasItemMeta()) {
            return false;
        }

        return item.getItemMeta()
                .getPersistentDataContainer()
                .has(
                        key,
                        PersistentDataType.BYTE
                );
    }

    // =========================
    // OWNER
    // =========================

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
    // DAMAGE
    // =========================

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onDamage(
            EntityDamageByEntityEvent event
    ) {

        if (!(event.getDamager()
                instanceof Player player)) {
            return;
        }

        if (!holdingMorningStar(player)) {
            return;
        }

        // =========================
        // CRITICAL FURY
        // =========================

        if (isFuryActive(player)) {

            /*
             * Paper 26.1.2 does not have
             * event.setCritical().
             *
             * 1.5x damage simulates
             * guaranteed critical damage.
             */

            event.setDamage(
                    event.getDamage() * 1.5
            );

        } else {

            // =========================
            // NORMAL CRITICAL
            // =========================

            if (isCritical(player)) {

                int hits =
                        combo.getOrDefault(
                                player.getUniqueId(),
                                0
                        ) + 1;

                // =========================
                // FOURTH CRIT
                // =========================

                if (hits >= 4) {

                    combo.remove(
                            player.getUniqueId()
                    );

                    activateFury(player);

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
        }

        // =========================
        // SHIELD BREAK
        // =========================

        if (event.getEntity()
                instanceof Player target) {

            if (target.isBlocking()) {

                disableShield(target);
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
    // ACTIVATE FURY
    // =========================

    private void activateFury(Player player) {

        // 15 seconds
        fury.put(
                player.getUniqueId(),
                System.currentTimeMillis() + 15000
        );

        // =========================
        // CHAT
        // =========================

        player.sendMessage("");

        player.sendMessage(
                ChatColor.DARK_RED +
                "" + ChatColor.BOLD +
                "✦ MORNINGSTAR ✦"
        );

        player.sendMessage(
                ChatColor.RED +
                "" + ChatColor.BOLD +
                "CRITICAL FURY ACTIVATED!"
        );

        player.sendMessage(
                ChatColor.WHITE +
                "All normal attacks are now CRITICAL!"
        );

        player.sendMessage(
                ChatColor.GRAY +
                "Duration: " +
                ChatColor.WHITE +
                "15 seconds"
        );

        player.sendMessage("");

        // =========================
        // ACTIVATION PARTICLES
        // =========================

        Location location =
                player.getLocation()
                        .add(0, 1, 0);

        player.getWorld().spawnParticle(
                Particle.CRIT,
                location,
                80,
                0.7,
                1.0,
                0.7,
                0.15
        );

        player.getWorld().spawnParticle(
                Particle.ENCHANT,
                location,
                60,
                0.7,
                1.0,
                0.7,
                1
        );

        // =========================
        // SOUND
        // =========================

        player.playSound(
                location,
                Sound.ENTITY_PLAYER_ATTACK_CRIT,
                1.0f,
                0.6f
        );

        player.playSound(
                location,
                Sound.BLOCK_BEACON_ACTIVATE,
                1.0f,
                1.5f
        );

        // =========================
        // DARK RED AURA
        // =========================

        startDarkRedAura(player);
    }

    // =========================
    // DARK RED AURA
    // =========================

    private void startDarkRedAura(Player player) {

        if (auraTask != null) {
            auraTask.cancel();
        }

        auraTask =
                getServer()
                        .getScheduler()
                        .runTaskTimer(
                                this,
                                () -> {

            // Stop when Fury ends
            if (!isFuryActive(player)) {

                if (auraTask != null) {
                    auraTask.cancel();
                    auraTask = null;
                }

                return;
            }

            Location loc =
                    player.getLocation();

            // =========================
            // DARK RED BODY AURA
            // =========================

            for (int i = 0; i < 20; i++) {

                double angle =
                        (Math.PI * 2 / 20) * i;

                double radius = 0.75;

                double x =
                        Math.cos(angle) * radius;

                double z =
                        Math.sin(angle) * radius;

                double y =
                        0.2 +
                        (Math.random() * 1.8);

                Location particle =
                        loc.clone().add(
                                x,
                                y,
                                z
                        );

                player.getWorld().spawnParticle(
                        Particle.DUST,
                        particle,
                        1,
                        new Particle.DustOptions(
                                Color.fromRGB(
                                        80,
                                        0,
                                        0
                                ),
                                2.0f
                        )
                );
            }

            // =========================
            // DARK RED FOOT RING
            // =========================

            for (int i = 0; i < 24; i++) {

                double angle =
                        (Math.PI * 2 / 24) * i;

                double radius = 0.9;

                double x =
                        Math.cos(angle) * radius;

                double z =
                        Math.sin(angle) * radius;

                Location ring =
                        loc.clone().add(
                                x,
                                0.05,
                                z
                        );

                player.getWorld().spawnParticle(
                        Particle.DUST,
                        ring,
                        1,
                        new Particle.DustOptions(
                                Color.fromRGB(
                                        110,
                                        0,
                                        0
                                ),
                                1.5f
                        )
                );
            }

            // =========================
            // DARK RED RISING PARTICLES
            // =========================

            for (int i = 0; i < 5; i++) {

                double x =
                        (Math.random() - 0.5) * 1.2;

                double z =
                        (Math.random() - 0.5) * 1.2;

                double y =
                        Math.random() * 2.0;

                Location rising =
                        loc.clone().add(
                                x,
                                y,
                                z
                        );

                player.getWorld().spawnParticle(
                        Particle.DUST,
                        rising,
                        1,
                        new Particle.DustOptions(
                                Color.fromRGB(
                                        60,
                                        0,
                                        0
                                ),
                                1.8f
                        )
                );
            }

        },
        0L,
        2L
        );
    }

    // =========================
    // FURY CHECK
    // =========================

    private boolean isFuryActive(Player player) {

        Long end =
                fury.get(
                        player.getUniqueId()
                );

        if (end == null) {
            return false;
        }

        if (System.currentTimeMillis() >= end) {

            fury.remove(
                    player.getUniqueId()
            );

            player.sendActionBar(
                    ChatColor.DARK_RED +
                    "Critical Fury ended."
            );

            return false;
        }

        return true;
    }

    // =========================
    // SHIELD BREAK
    // =========================

    private void disableShield(Player target) {

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
                25,
                0.3,
                0.5,
                0.3,
                0.1
        );
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
                instanceof Player player)) {
            return;
        }

        ItemStack item =
                event.getItem()
                        .getItemStack();

        if (!isMorningStar(item)) {
            return;
        }

        // Only Yue_47
        if (!isOwner(player)) {

            event.setCancelled(true);
            event.getItem().remove();

            return;
        }

        // Anti duplicate
        for (ItemStack inventoryItem :
                player.getInventory()
                        .getContents()) {

            if (isMorningStar(inventoryItem)) {

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

        if (!isOwner(event.getEntity())) {
            return;
        }

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
                && !isMorningStar(cursor)) {
            return;
        }

        if (event.getClickedInventory()
                != null
                &&
                event.getClickedInventory()
                != event.getWhoClicked()
                .getInventory()) {

            event.setCancelled(true);

            event.getWhoClicked().sendMessage(
                    ChatColor.RED +
                    "MorningStar cannot be stored!"
            );
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

        if (!(sender instanceof Player player)) {

            sender.sendMessage(
                    "Players only."
            );

            return true;
        }

        // =========================
        // HELP
        // =========================

        if (args.length == 0
                || args[0].equalsIgnoreCase(
                "help"
        )) {

            sendHelp(player);

            return true;
        }

        // =========================
        // GIVE
        // =========================

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

        // =========================
        // STATUS
        // =========================

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
                    (
                        active
                        ? ChatColor.GREEN + "ACTIVE"
                        : ChatColor.RED + "INACTIVE"
                    )
            );

            if (active) {

                Long end =
                        fury.get(
                                player.getUniqueId()
                        );

                long seconds =
                        Math.max(
                                0,
                                (end -
                                System.currentTimeMillis())
                                / 1000
                        );

                player.sendMessage(
                        ChatColor.YELLOW +
                        "Time left: " +
                        ChatColor.WHITE +
                        seconds +
                        "s"
                );
            }

            return true;
        }

        sendHelp(player);

        return true;
    }

    // =========================
    // HELP
    // =========================

    private void sendHelp(Player player) {

        player.sendMessage(
                ChatColor.GOLD +
                "===== MorningStar ====="
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "/morningstar give" +
                ChatColor.GRAY +
                " - Get MorningStar"
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "/morningstar status" +
                ChatColor.GRAY +
                " - View status"
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "/morningstar help" +
                ChatColor.GRAY +
                " - Show help"
        );
    }
}