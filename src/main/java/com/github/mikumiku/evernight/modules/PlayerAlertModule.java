package com.github.mikumiku.evernight.modules;

import com.github.mikumiku.evernight.MikuCategories;
import com.github.mikumiku.evernight.MikuModule;
import evernight.client.events.core.EventHandler;
import evernight.client.events.ui.MinecraftTickEvent;
import evernight.client.modules.ModuleCategory;
import evernight.client.settings.core.Setting;
import evernight.client.settings.types.BooleanSetting;
import evernight.client.settings.types.NumberSetting;
import evernight.client.settings.types.StringSetting;
import net.minecraft.world.entity.player.Player;

/** Alerts when a watched player is nearby. Ported from {@code PlayerAlert}. */
public final class PlayerAlertModule extends MikuModule {
    private final Setting<String> playerName;
    private final Setting<Double> range;
    private final Setting<Boolean> onlyOnce;

    private boolean alerted;

    public PlayerAlertModule() {
        super("PlayerAlertPlus", "Warns you when a specific player is nearby.", MikuCategories.MIKU, "mikualert");
        this.playerName = this.add(new StringSetting("Player", ""));
        this.range = this.add(new NumberSetting<>("Range", 64.0, 8.0, 256.0));
        this.onlyOnce = this.add(new BooleanSetting("OnlyOnce", false));
    }

    @Override
    public void onEnable() {
        this.alerted = false;
    }

    @EventHandler
    private void onTick(MinecraftTickEvent event) {
        if (!this.inGame()) {
            return;
        }
        String name = this.playerName.getValue();
        if (name == null || name.isBlank()) {
            return;
        }
        boolean nearby = mc().level.players().stream()
                .filter(player -> player != mc().player)
                .map(Player::getName)
                .anyMatch(component -> component.getString().equalsIgnoreCase(name))
                && mc().level.players().stream()
                .filter(player -> player != mc().player)
                .anyMatch(player -> player.getName().getString().equalsIgnoreCase(name)
                        && mc().player.distanceToSqr(player) <= this.range.getValue() * this.range.getValue());
        if (!nearby) {
            if (!this.onlyOnce.getValue()) {
                this.alerted = false;
            }
            return;
        }
        if (this.alerted) {
            return;
        }
        this.alerted = true;
        this.info("Player " + name + " is nearby!");
        mc().player.playSound(net.minecraft.sounds.SoundEvents.NOTE_BLOCK_PLING.value(), 1.0F, 1.0F);
    }
}
