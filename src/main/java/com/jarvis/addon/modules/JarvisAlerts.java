package com.jarvis.addon.modules;

import com.jarvis.addon.JarvisAddon;
import com.jarvis.addon.audio.JarvisSound;
import com.jarvis.addon.audio.SoundPlayer;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class JarvisAlerts extends Module {
    // Entity status ids sent by the server (see net.minecraft.entity.EntityStatuses)
    private static final byte STATUS_TOTEM = 35;
    private static final byte STATUS_BREAK_HELMET = 49;
    private static final byte STATUS_BREAK_CHESTPLATE = 50;
    private static final byte STATUS_BREAK_LEGGINGS = 51;
    private static final byte STATUS_BREAK_BOOTS = 52;

    private final SettingGroup sgEvents = settings.createGroup("Events");
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // --- Events ---
    private final Setting<Boolean> totem = sgEvents.add(new BoolSetting.Builder()
        .name("totem-pop").description("Play a sound when a player's totem pops.")
        .defaultValue(true).build());

    private final Setting<Boolean> helmet = sgEvents.add(new BoolSetting.Builder()
        .name("helmet-break").description("Play a sound when a player's helmet breaks.")
        .defaultValue(true).build());

    private final Setting<Boolean> chestplate = sgEvents.add(new BoolSetting.Builder()
        .name("chestplate-break").description("Play a sound when a player's chestplate breaks.")
        .defaultValue(true).build());

    private final Setting<Boolean> leggings = sgEvents.add(new BoolSetting.Builder()
        .name("leggings-break").description("Play a sound when a player's leggings break.")
        .defaultValue(true).build());

    private final Setting<Boolean> boots = sgEvents.add(new BoolSetting.Builder()
        .name("boots-break").description("Play a sound when a player's boots break.")
        .defaultValue(true).build());

    private final Setting<Boolean> lowHp = sgEvents.add(new BoolSetting.Builder()
        .name("low-hp").description("Play a sound when a player's health is low.")
        .defaultValue(true).build());

    private final Setting<Double> lowHpThreshold = sgEvents.add(new DoubleSetting.Builder()
        .name("low-hp-threshold").description("Health points at or below which a player counts as low (2 HP = 1 heart).")
        .defaultValue(6).min(1).sliderRange(1, 20).visible(lowHp::get).build());

    // --- General ---
    private final Setting<Integer> volume = sgGeneral.add(new IntSetting.Builder()
        .name("volume").description("Alert volume in percent.")
        .defaultValue(100).range(0, 100).sliderRange(0, 100).build());

    private final Setting<Integer> cooldown = sgGeneral.add(new IntSetting.Builder()
        .name("cooldown-ms").description("Minimum delay before the same sound can play again.")
        .defaultValue(1000).min(0).sliderMax(5000).build());

    private final Setting<Boolean> ignoreFriends = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-friends").description("Don't play sounds for players on your friends list.")
        .defaultValue(false).build());

    private final Map<JarvisSound, Long> lastPlayed = new EnumMap<>(JarvisSound.class);
    private final Set<UUID> lowHpAnnounced = new HashSet<>();

    public JarvisAlerts() {
        super(JarvisAddon.CATEGORY, "jarvis-alerts", "Voice alerts for totem pops, armor breaks and low HP of other players.");
    }

    @Override
    public void onActivate() {
        lowHpAnnounced.clear();
        lastPlayed.clear();
    }

    @Override
    public void onDeactivate() {
        lowHpAnnounced.clear();
    }

    @EventHandler
    private void onPacket(PacketEvent.Receive event) {
        if (mc.world == null || !(event.packet instanceof EntityStatusS2CPacket packet)) return;

        JarvisSound sound = switch (packet.getStatus()) {
            case STATUS_TOTEM -> totem.get() ? JarvisSound.TOTEM_POP : null;
            case STATUS_BREAK_HELMET -> helmet.get() ? JarvisSound.HELMET_BREAK : null;
            case STATUS_BREAK_CHESTPLATE -> chestplate.get() ? JarvisSound.CHESTPLATE_BREAK : null;
            case STATUS_BREAK_LEGGINGS -> leggings.get() ? JarvisSound.LEGGINGS_BREAK : null;
            case STATUS_BREAK_BOOTS -> boots.get() ? JarvisSound.BOOTS_BREAK : null;
            default -> null;
        };
        if (sound == null) return;

        Entity entity = packet.getEntity(mc.world);
        if (!(entity instanceof PlayerEntity player) || !isTarget(player)) return;

        play(sound);
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!lowHp.get() || mc.world == null || mc.player == null) return;

        Set<UUID> present = new HashSet<>();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (!isTarget(player)) continue;

            UUID id = player.getUuid();
            present.add(id);

            float health = player.getHealth();
            if (health > 0 && health <= lowHpThreshold.get()) {
                // add() returns true only the first time -> one announcement per "low HP period"
                if (lowHpAnnounced.add(id)) play(JarvisSound.LOW_HP);
            } else if (health > lowHpThreshold.get()) {
                lowHpAnnounced.remove(id);
            }
        }

        // Forget players who left render range / disconnected
        lowHpAnnounced.retainAll(present);
    }

    /** Other players only (never yourself), optionally skipping friends. */
    private boolean isTarget(PlayerEntity player) {
        if (player == mc.player) return false;
        return !(ignoreFriends.get() && Friends.get().isFriend(player));
    }

    private void play(JarvisSound sound) {
        long now = System.currentTimeMillis();
        Long last = lastPlayed.get(sound);
        if (last != null && now - last < cooldown.get()) return;
        lastPlayed.put(sound, now);

        SoundPlayer.play(sound, volume.get() / 100f);
    }
}
