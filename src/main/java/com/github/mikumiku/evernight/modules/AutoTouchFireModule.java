package com.github.mikumiku.evernight.modules;

import com.github.mikumiku.evernight.MikuCategories;
import com.github.mikumiku.evernight.MikuModule;
import com.github.mikumiku.evernight.util.MikuUtils;
import evernight.client.events.core.EventHandler;
import evernight.client.events.ui.MinecraftTickEvent;
import evernight.client.modules.ModuleCategory;
import evernight.client.settings.core.Setting;
import evernight.client.settings.types.BooleanSetting;
import evernight.client.settings.types.NumberSetting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;

/** Extinguishes fire around the player. Ported from {@code AutoTouchFire}. */
public final class AutoTouchFireModule extends MikuModule {
    private final Setting<Double> range;
    private final Setting<Boolean> rotate;
    private final Setting<Boolean> onlyOnGround;

    public AutoTouchFireModule() {
        super("AutoTouchFire", "Puts out fire around you.", MikuCategories.BUILD, "mikufire");
        this.range = this.add(new NumberSetting<>("Range", 4.0, 1.0, 6.0));
        this.rotate = this.add(new BooleanSetting("Rotate", true));
        this.onlyOnGround = this.add(new BooleanSetting("OnlyOnGround", false));
    }

    @EventHandler
    private void onTick(MinecraftTickEvent event) {
        if (!this.inGame()) {
            return;
        }
        if (this.onlyOnGround.getValue() && !mc().player.onGround()) {
            return;
        }
        BlockPos origin = mc().player.blockPosition();
        int radius = (int) Math.ceil(this.range.getValue());
        for (int x = -radius; x <= radius; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos candidate = origin.offset(x, y, z);
                    if (!mc().level.getBlockState(candidate).is(Blocks.FIRE)) {
                        continue;
                    }
                    if (mc().player.position().distanceToSqr(candidate.getCenter())
                            > this.range.getValue() * this.range.getValue()) {
                        continue;
                    }
                    if (this.rotate.getValue()) {
                        MikuUtils.rotateTo(candidate);
                    }
                    mc().gameMode.startDestroyBlock(candidate, Direction.UP);
                    return;
                }
            }
        }
    }
}
