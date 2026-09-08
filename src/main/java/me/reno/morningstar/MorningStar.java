package me.reno.morningstar;

import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MorningStar extends JavaPlugin implements Listener {

    // =========================
    // SETTINGS
    // =========================

    private static final String OWNER_NAME = "Yue_47";

    private static final double NORMAL_DAMAGE = 23.0;
    private static final double CRITICAL_FURY_DAMAGE = 30.0;

    private static final double REACH = 4.0;

    private static final long CRITICAL_FURY_COOLDOWN = 30_000L;
    private static final long CRITICAL_FURY_DURATION = 10_000L;

    private static final int STUN_HITS = 5;

    // =========================
    // DATA
    // =========================

    private final Map<UUID, Integer> hitCounter = new HashMap<>();
    private final Map<UUID, Long> furyCooldown = new HashMap<>();
    private final Map<UUID, Long> furyActive = new HashMap<>();

    private NamespacedKey morningstarKey;

    // =========================
    // ENABLE
    // =========================

    @Override
    public void onEnable() {

        morningstarKey = new NamespacedKey(this, "morningstar");

        getServer().getPluginManager().registerEvents(this, this);

        // Speed II + Fury particles
        getServer().getScheduler().runTaskTimer(this, () -> {

            long now = System.currentTimeMillis();

            for (Player player : Bukkit.getOnlinePlayers()) {

                if (!isOwner(player)) {
                    continue;
                }

                // =========================
                // SPEED II
                // =========================

                player.addPotionEffect(
                        new PotionEffect(
                                PotionEffectType.SPEED,
                                40,
                                1,
                                false,
                                false,
                                true
                        )
                );

                // =========================
                // CRITICAL FURY PARTICLES
                // =========================

                if (isFuryActive(player)) {

                    Location loc =
                            player.getLocation().add(0, 1.0, 0);

                    for (int i = 0; i < 8; i++) {

                        double angle =
                                Math.random() * Math.PI * 2;

                        double radius =
                                0.6 + Math.random() * 0.5;

                        double x =
                                Math.cos(angle) * radius;

                        double z =
                                Math.sin(angle) * radius;

                        Location particleLoc =
                                loc.clone().add(x, 0, z);

                        player.getWorld().spawnParticle(
                                Particle.DUST,
                                particleLoc,
                                1,
                                new Particle.DustOptions(
                                        Color.fromRGB(120, 0, 0),
                                        1.8f
                                )
                        );
                    }
                }
            }

            // Remove expired Fury
            furyActive.entrySet()
                    .removeIf(entry -> now >= entry.getValue());

        }, 0L, 10L);
    }

    // =========================================================
    // CREATE MORNINGSTAR
    // =========================================================

    public ItemStack createMorningstar() {

        ItemStack item =
                new ItemStack(Material.NETHERITE_SWORD);

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        meta.setDisplayName(
                ChatColor.DARK_RED +
                "" +
                ChatColor.BOLD +
                "Morningstar"
        );

        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "The weapon of Yue_47",
                "",
                ChatColor.RED +
                        "⚔ Physical Attack: " +
                        NORMAL_DAMAGE,

                ChatColor.RED +
                        "⚔ Reach: " +
                        REACH +
                        " blocks",

                "",
                ChatColor.DARK_RED +
                        "" +
                        ChatColor.BOLD +
                        "Critical Fury",

                ChatColor.GRAY +
                        "Right Click to activate",

                ChatColor.GRAY +
                        "Cooldown: 30 seconds"
        ));

        meta.setUnbreakable(true);

        // Identifier only.
        // THIS DOES NOT prevent dropping or storing.
        meta.getPersistentDataContainer().set(
                morningstarKey,
                PersistentDataType.BYTE,
                (byte) 1
        );

        item.setItemMeta(meta);

        return item;
    }

    // =========================================================
    // CHECK MORNINGSTAR
    // =========================================================

    private boolean isMorningstar(ItemStack item) {

        if (item == null) {
            return false;
        }

        if (!item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return false;
        }

        Byte value =
                meta.getPersistentDataContainer().get(
                        morningstarKey,
                        PersistentDataType.BYTE
                );

        return value != null &&
                value == (byte) 1;
    }

    // =========================================================
    // OWNER
    // =========================================================

    private boolean isOwner(Player player) {

        return player.getName()
                .equalsIgnoreCase(OWNER_NAME);
    }

    // =========================================================
    // RIGHT CLICK
    // =========================================================

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {

        Player player = event.getPlayer();

        if (!isOwner(player)) {
            return;
        }

        ItemStack item =
                player.getInventory().getItemInMainHand();

        if (!isMorningstar(item)) {
            return;
        }

        Action action = event.getAction();

        if (action != Action.RIGHT_CLICK_AIR &&
                action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        event.setCancelled(true);

        activateCriticalFury(player);
    }

    // =========================================================
    // CRITICAL FURY
    // =========================================================

    private void activateCriticalFury(Player player) {

        UUID uuid = player.getUniqueId();

        long now = System.currentTimeMillis();

        // =========================
        // COOLDOWN
        // =========================

        Long cooldownEnd =
                furyCooldown.get(uuid);

        if (cooldownEnd != null) {

            long remaining =
                    cooldownEnd - now;

            if (remaining > 0) {

                long seconds =
                        (remaining + 999) / 1000;

                player.sendMessage(
                        ChatColor.RED +
                                "Critical Fury is on cooldown! " +
                                ChatColor.GRAY +
                                "(" +
                                seconds +
                                "s)"
                );

                return;
            }
        }

        // =========================
        // ACTIVATE
        // =========================

        furyCooldown.put(
                uuid,
                now + CRITICAL_FURY_COOLDOWN
        );

        furyActive.put(
                uuid,
                now + CRITICAL_FURY_DURATION
        );

        player.sendMessage(
                ChatColor.DARK_RED +
                        "" +
                        ChatColor.BOLD +
                        "CRITICAL FURY ACTIVATED!"
        );

        player.playSound(
                player.getLocation(),
                Sound.ENTITY_WITHER_SPAWN,
                0.7f,
                1.5f
        );

        player.getWorld().spawnParticle(
                Particle.DUST,
                player.getLocation().add(0, 1, 0),
                80,
                1.0,
                1.0,
                1.0,
                new Particle.DustOptions(
                        Color.fromRGB(120, 0, 0),
                        2.5f
                )
        );
    }

    // =========================================================
    // FURY CHECK
    // =========================================================

    private boolean isFuryActive(Player player) {

        Long end =
                furyActive.get(player.getUniqueId());

        if (end == null) {
            return false;
        }

        if (System.currentTimeMillis() >= end) {

            furyActive.remove(
                    player.getUniqueId()
            );

            return false;
        }

        return true;
    }

    // =========================================================
    // NORMAL ATTACK
    // =========================================================

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {

        if (!(event.getDamager() instanceof Player)) {
            return;
        }

        Player player =
                (Player) event.getDamager();

        if (!isOwner(player)) {
            return;
        }

        ItemStack item =
                player.getInventory().getItemInMainHand();

        if (!isMorningstar(item)) {
            return;
        }

        if (!(event.getEntity() instanceof LivingEntity)) {
            return;
        }

        LivingEntity target =
                (LivingEntity) event.getEntity();

        // =========================
        // DAMAGE
        // =========================

        if (isFuryActive(player)) {

            event.setDamage(
                    CRITICAL_FURY_DAMAGE
            );

        } else {

            event.setDamage(
                    NORMAL_DAMAGE
            );
        }

        // =========================
        // HIT COUNTER
        // =========================

        UUID uuid =
                player.getUniqueId();

        int hits =
                hitCounter.getOrDefault(
                        uuid,
                        0
                ) + 1;

        if (hits >= STUN_HITS) {

            stun(target);

            hitCounter.put(
                    uuid,
                    0
            );

        } else {

            hitCounter.put(
                    uuid,
                    hits
            );
        }
    }

    // =========================================================
    // STUN
    // =========================================================

    private void stun(LivingEntity target) {

        target.addPotionEffect(
                new PotionEffect(
                        PotionEffectType.SLOWNESS,
                        60,
                        254,
                        false,
                        true,
                        true
                )
        );

        target.getWorld().spawnParticle(
                Particle.DUST,
                target.getLocation().add(0, 1, 0),
                40,
                0.5,
                1.0,
                0.5,
                new Particle.DustOptions(
                        Color.fromRGB(120, 0, 0),
                        2.0f
                )
        );

        target.getWorld().playSound(
                target.getLocation(),
                Sound.BLOCK_ANVIL_LAND,
                0.7f,
                1.8f
        );
    }

    // =========================================================
    // EXTENDED REACH
    // =========================================================

    @EventHandler
    public void onSwing(PlayerAnimationEvent event) {

        Player player =
                event.getPlayer();

        if (!isOwner(player)) {
            return;
        }

        ItemStack item =
                player.getInventory().getItemInMainHand();

        if (!isMorningstar(item)) {
            return;
        }

        RayTraceResult result =
                player.getWorld().rayTraceEntities(
                        player.getEyeLocation(),
                        player.getEyeLocation().getDirection(),
                        REACH,
                        0.35,
                        entity -> {

                            if (!(entity instanceof LivingEntity)) {
                                return false;
                            }

                            return entity != player;
                        }
                );

        if (result == null) {
            return;
        }

        Entity hit =
                result.getHitEntity();

        if (!(hit instanceof LivingEntity)) {
            return;
        }

        LivingEntity target =
                (LivingEntity) hit;

        double distance =
                player.getEyeLocation()
                        .distance(
                                target.getLocation()
                        );

        if (distance <= 3.0) {
            return;
        }

        double damage =
                isFuryActive(player)
                        ? CRITICAL_FURY_DAMAGE
                        : NORMAL_DAMAGE;

        EntityDamageByEntityEvent damageEvent =
                new EntityDamageByEntityEvent(
                        player,
                        target,
                        EntityDamageByEntityEvent.DamageCause.ENTITY_ATTACK,
                        damage
                );

        Bukkit.getPluginManager()
                .callEvent(damageEvent);

        if (damageEvent.isCancelled()) {
            return;
        }

        target.damage(
                damageEvent.getFinalDamage(),
                player
        );

        // =========================
        // HIT COUNTER
        // =========================

        UUID uuid =
                player.getUniqueId();

        int hits =
                hitCounter.getOrDefault(
                        uuid,
                        0
                ) + 1;

        if (hits >= STUN_HITS) {

            stun(target);

            hitCounter.put(
                    uuid,
                    0
            );

        } else {

            hitCounter.put(
                    uuid,
                    hits
            );
        }
    }

    // =========================================================
    // QUIT CLEANUP
    // =========================================================

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {

        UUID uuid =
                event.getPlayer().getUniqueId();

        hitCounter.remove(uuid);
        furyActive.remove(uuid);
        furyCooldown.remove(uuid);
    }

    // =========================================================
    // GIVE MORNINGSTAR
    // =========================================================

    public boolean giveMorningstar(Player player) {

        if (!isOwner(player)) {
            return false;
        }

        // Normal inventory item.
        // It CAN be moved, stored and dropped.
        player.getInventory().addItem(
                createMorningstar()
        );

        return true;
    }
}