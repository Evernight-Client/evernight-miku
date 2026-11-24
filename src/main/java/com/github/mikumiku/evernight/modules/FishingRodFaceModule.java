package com.github.mikumiku.evernight.modules;

import com.github.mikumiku.evernight.MikuCategories;
import com.github.mikumiku.evernight.MikuModule;
import com.github.mikumiku.evernight.util.MikuUtils;
import evernight.client.events.core.EventHandler;
import evernight.client.events.ui.MinecraftTickEvent;
import evernight.client.modules.ModuleCategory;
import evernight.client.settings.core.Setting;
import evernight.client.settings.types.NumberSetting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

/** Flicks a fishing rod at nearby enemies. Ported from {@code FishingRodFace}. */
public final class FishingRodFaceModule extends MikuModule {
    private final Setting<Double> range;
    private final Setting<Integer> delay;

    private long lastCast;

    public FishingRodFaceModule() {
        super("FishingRodFace", "Casts a fishing rod at nearby enemies.", MikuCategories.MIKU, "mikurod");
        this.range = this.add(new NumberSetting<>("Range", 4.0, 1.0, 8.0));
        this.delay = this.add(new NumberSetting<>("Delay", 500, 0, 2000));
    }

    @Override
    public void onEnable() {
        this.lastCast = 0L;
    }

    @EventHandler
    private void onTick(MinecraftTickEvent event) {
        if (!this.inGame() || System.currentTimeMillis() - this.lastCast < this.delay.getValue()) {
            return;
        }
        if (!mc().player.getMainHandItem().is(Items.FISHING_ROD)) {
            return;
        }
        LivingEntity target = this.findTarget();
        if (target == null) {
            return;
        }
        MikuUtils.rotateTo(target.position());
        mc().gameMode.useItem(mc().player, InteractionHand.MAIN_HAND);
        mc().player.swing(InteractionHand.MAIN_HAND);
        this.lastCast = System.currentTimeMillis();
    }

    private LivingEntity findTarget() {
        for (Entity entity : mc().level.entitiesForRendering()) {
            if (!(entity instanceof Player player) || player == mc().player || !player.isAlive()) {
                continue;
            }
            if (mc().player.distanceToSqr(player) <= this.range.getValue() * this.range.getValue()) {
                return player;
            }
        }
        return null;
    }
}
