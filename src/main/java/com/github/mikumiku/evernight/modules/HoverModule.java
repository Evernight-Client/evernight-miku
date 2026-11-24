package com.github.mikumiku.evernight.modules;

import com.github.mikumiku.evernight.MikuCategories;
import com.github.mikumiku.evernight.MikuModule;
import evernight.client.events.core.EventHandler;
import evernight.client.events.ui.MinecraftTickEvent;
import evernight.client.modules.ModuleCategory;
import evernight.client.settings.core.Setting;
import evernight.client.settings.types.BooleanSetting;
import evernight.client.settings.types.NumberSetting;
import net.minecraft.world.phys.Vec3;

/** Keeps the player hovering in place while airborne. Ported from {@code Hover}. */
public final class HoverModule extends MikuModule {
    private final Setting<Double> fallSpeed;
    private final Setting<Boolean> onlyWhenJumping;
    private final Setting<Boolean> stopHorizontal;

    public HoverModule() {
        super("HoverPlus", "Holds your position in the air.", MikuCategories.TWEAK, "mikuhover");
        this.fallSpeed = this.add(new NumberSetting<>("FallSpeed", 0.0, -0.5, 0.5));
        this.onlyWhenJumping = this.add(new BooleanSetting("OnlyWhenJumping", true));
        this.stopHorizontal = this.add(new BooleanSetting("StopHorizontal", false));
    }

    @EventHandler
    private void onTick(MinecraftTickEvent event) {
        if (!this.inGame() || mc().player.onGround() || mc().player.isFallFlying()
                || mc().player.isInWater() || mc().player.isInLava()) {
            return;
        }
        if (this.onlyWhenJumping.getValue() && !mc().options.keyJump.isDown()) {
            return;
        }
        Vec3 motion = mc().player.getDeltaMovement();
        double x = this.stopHorizontal.getValue() ? 0.0 : motion.x;
        double z = this.stopHorizontal.getValue() ? 0.0 : motion.z;
        mc().player.setDeltaMovement(x, this.fallSpeed.getValue(), z);
        mc().player.fallDistance = 0.0F;
    }
}
