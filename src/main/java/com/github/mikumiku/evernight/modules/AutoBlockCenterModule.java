package com.github.mikumiku.evernight.modules;

import com.github.mikumiku.evernight.MikuCategories;
import com.github.mikumiku.evernight.MikuModule;
import evernight.client.events.core.EventHandler;
import evernight.client.events.ui.MinecraftTickEvent;
import evernight.client.modules.ModuleCategory;
import evernight.client.settings.core.Setting;
import evernight.client.settings.types.BooleanSetting;

/** Centers the player on the current block. Ported from {@code AutoBlockCenter}. */
public final class AutoBlockCenterModule extends MikuModule {
    private final Setting<Boolean> onlyOnGround;
    private final Setting<Boolean> stopMoving;

    public AutoBlockCenterModule() {
        super("AutoBlockCenter", "Centers you on the block you stand on.", MikuCategories.TWEAK, "mikucenter");
        this.onlyOnGround = this.add(new BooleanSetting("OnlyOnGround", true));
        this.stopMoving = this.add(new BooleanSetting("StopMoving", true));
    }

    @EventHandler
    private void onTick(MinecraftTickEvent event) {
        if (!this.inGame()) {
            return;
        }
        if (this.onlyOnGround.getValue() && !mc().player.onGround()) {
            return;
        }
        if (this.stopMoving.getValue() && mc().options.keyUp.isDown()) {
            return;
        }
        double x = Math.floor(mc().player.getX()) + 0.5;
        double z = Math.floor(mc().player.getZ()) + 0.5;
        if (Math.abs(mc().player.getX() - x) < 0.05 && Math.abs(mc().player.getZ() - z) < 0.05) {
            return;
        }
        mc().player.setPos(x, mc().player.getY(), z);
    }
}
