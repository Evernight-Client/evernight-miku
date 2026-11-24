package com.github.mikumiku.evernight.modules;

import com.github.mikumiku.evernight.MikuCategories;
import com.github.mikumiku.evernight.MikuModule;
import evernight.client.events.core.EventHandler;
import evernight.client.events.ui.MinecraftTickEvent;
import evernight.client.modules.ModuleCategory;
import evernight.client.settings.core.Setting;
import evernight.client.settings.types.NumberSetting;

/** Spins the player model for style. Ported from {@code HandsomeSpin}. */
public final class HandsomeSpinModule extends MikuModule {
    private final Setting<Double> speed;

    public HandsomeSpinModule() {
        super("HandsomeSpin", "Spins your character around.", MikuCategories.MIKU, "mikuspin");
        this.speed = this.add(new NumberSetting<>("Speed", 25.0, 1.0, 90.0));
    }

    @EventHandler
    private void onTick(MinecraftTickEvent event) {
        if (!this.inGame()) {
            return;
        }
        float yaw = mc().player.getYRot() + this.speed.getValue().floatValue();
        mc().player.setYRot(yaw);
        mc().player.setYHeadRot(yaw);
        mc().player.setYBodyRot(yaw);
    }
}
