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
import evernight.client.utils.network.PacketSender;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;

/** Automatically opens nearby chests. Ported from {@code ChestAura}. */
public final class ChestAuraModule extends MikuModule {
    private final Setting<Double> range;
    private final Setting<Integer> delay;
    private final Setting<Boolean> rotate;
    private final Setting<Boolean> closeAfterOpen;

    private long lastOpen;

    public ChestAuraModule() {
        super("ChestAuraPlus", "Opens chests around you automatically.", MikuCategories.MIKU, "mikuchestaura");
        this.range = this.add(new NumberSetting<>("Range", 4.0, 1.0, 6.0));
        this.delay = this.add(new NumberSetting<>("Delay", 200, 0, 2000));
        this.rotate = this.add(new BooleanSetting("Rotate", true));
        this.closeAfterOpen = this.add(new BooleanSetting("CloseAfterOpen", false));
    }

    @Override
    public void onEnable() {
        this.lastOpen = 0L;
    }

    @EventHandler
    private void onTick(MinecraftTickEvent event) {
        if (!this.inGame()) {
            return;
        }
        if (mc().screen != null) {
            if (this.closeAfterOpen.getValue()) {
                mc().player.closeContainer();
            }
            return;
        }
        if (System.currentTimeMillis() - this.lastOpen < this.delay.getValue()) {
            return;
        }
        BlockPos chest = this.findChest();
        if (chest == null) {
            return;
        }
        if (this.rotate.getValue()) {
            MikuUtils.rotateTo(chest);
        }
        Direction direction = Direction.UP;
        BlockHitResult hit = new BlockHitResult(chest.getCenter(), direction, chest, false);
        PacketSender.sendUseItemOn(InteractionHand.MAIN_HAND, hit);
        mc().player.swing(InteractionHand.MAIN_HAND);
        this.lastOpen = System.currentTimeMillis();
    }

    private BlockPos findChest() {
        BlockPos origin = mc().player.blockPosition();
        int radius = (int) Math.ceil(this.range.getValue());
        for (int x = -radius; x <= radius; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos candidate = origin.offset(x, y, z);
                    Block block = mc().level.getBlockState(candidate).getBlock();
                    if (block != Blocks.CHEST && block != Blocks.TRAPPED_CHEST && block != Blocks.BARREL
                            && block != Blocks.ENDER_CHEST) {
                        continue;
                    }
                    if (mc().player.position().distanceToSqr(candidate.getCenter())
                            > this.range.getValue() * this.range.getValue()) {
                        continue;
                    }
                    return candidate;
                }
            }
        }
        return null;
    }
}
