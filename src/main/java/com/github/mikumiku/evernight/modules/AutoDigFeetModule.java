package com.github.mikumiku.evernight.modules;

import com.github.mikumiku.evernight.MikuCategories;
import com.github.mikumiku.evernight.MikuModule;
import com.github.mikumiku.evernight.util.MikuUtils;
import evernight.client.events.core.EventHandler;
import evernight.client.events.ui.MinecraftTickEvent;
import evernight.client.modules.ModuleCategory;
import evernight.client.settings.core.Setting;
import evernight.client.settings.types.BooleanSetting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** Mines the blocks blocking your feet. Ported from {@code AutoDigFeet}. */
public final class AutoDigFeetModule extends MikuModule {
    private final Setting<Boolean> rotate;
    private final Setting<Boolean> onlyWhenMoving;

    public AutoDigFeetModule() {
        super("AutoDigFeet", "Digs out the blocks in front of your feet.", MikuCategories.BUILD, "mikudigfeet");
        this.rotate = this.add(new BooleanSetting("Rotate", true));
        this.onlyWhenMoving = this.add(new BooleanSetting("OnlyWhenMoving", true));
    }

    @EventHandler
    private void onTick(MinecraftTickEvent event) {
        if (!this.inGame()) {
            return;
        }
        if (this.onlyWhenMoving.getValue() && !MikuUtils.isMoving()) {
            return;
        }
        BlockPos feet = mc().player.blockPosition();
        Direction facing = mc().player.getDirection();
        BlockPos front = feet.relative(facing);
        if (mc().level.getBlockState(front).isAir() && mc().level.getBlockState(front.above()).isAir()) {
            return;
        }
        if (this.rotate.getValue()) {
            MikuUtils.rotateTo(front);
        }
        if (!mc().gameMode.continueDestroyBlock(front, facing.getOpposite())) {
            mc().gameMode.startDestroyBlock(front, facing.getOpposite());
        }
    }
}
