package com.github.mikumiku.evernight.modules;

import com.github.mikumiku.evernight.MikuCategories;
import com.github.mikumiku.evernight.MikuModule;
import com.github.mikumiku.evernight.util.MikuUtils;
import evernight.client.events.core.EventHandler;
import evernight.client.events.ui.MinecraftTickEvent;
import evernight.client.settings.core.Setting;
import evernight.client.settings.types.BooleanSetting;
import evernight.client.settings.types.NumberSetting;
import evernight.client.utils.inventory.InventoryUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Chops whole trees around the player. Ported from Tanghulu's {@code AutoTreeChop}. */
public final class AutoTreeChopModule extends MikuModule {
    private final Setting<Double> radius;
    private final Setting<Integer> height;
    private final Setting<Boolean> topDown;
    private final Setting<Boolean> autoTool;
    private final Setting<Boolean> rotate;
    private final Setting<Boolean> waitDrops;
    private final Setting<Integer> dropWaitTicks;

    private BlockPos target;
    private int waitTicks;
    private int scanCooldown;

    public AutoTreeChopModule() {
        super("AutoTreeChop", "Chops whole trees around you, top down.",
                MikuCategories.BUILD, "mikutreechop", "treechop");
        this.radius = this.add(new NumberSetting<>("Range", 4.5, 1.0, 8.0));
        this.height = this.add(new NumberSetting<>("Height", 16, 1, 64));
        this.topDown = this.add(new BooleanSetting("TopDown", true));
        this.autoTool = this.add(new BooleanSetting("AutoTool", true));
        this.rotate = this.add(new BooleanSetting("Rotate", true));
        this.waitDrops = this.add(new BooleanSetting("WaitDrops", true));
        this.dropWaitTicks = this.add(new NumberSetting<>("DropWaitTicks", 30, 0, 100));
    }

    @Override
    public void onEnable() {
        this.target = null;
        this.waitTicks = 0;
        this.scanCooldown = 0;
    }

    @EventHandler
    private void onTick(MinecraftTickEvent event) {
        if (!this.inGame()) {
            return;
        }
        if (this.waitTicks > 0) {
            this.waitTicks--;
            return;
        }
        if (this.target != null && !this.isLog(this.target)) {
            this.target = null;
            if (this.waitDrops.getValue()) {
                this.waitTicks = this.dropWaitTicks.getValue();
                return;
            }
        }
        if (this.target == null) {
            if (this.scanCooldown > 0) {
                this.scanCooldown--;
                return;
            }
            this.target = this.findTarget();
            this.scanCooldown = 5;
        }
        if (this.target == null) {
            return;
        }
        BlockPos position = this.target;
        if (this.autoTool.getValue()) {
            int slot = InventoryUtils.findBestToolSlot(position, true);
            if (slot >= 0 && slot < 9) {
                mc().player.getInventory().setSelectedSlot(slot);
            }
        }
        if (this.rotate.getValue()) {
            MikuUtils.rotateTo(position);
        }
        Direction direction = this.facing(position);
        if (!mc().gameMode.continueDestroyBlock(position, direction)) {
            mc().gameMode.startDestroyBlock(position, direction);
        }
        mc().player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
    }

    private Direction facing(BlockPos position) {
        if (mc().player == null) {
            return Direction.UP;
        }
        double dx = mc().player.getX() - (position.getX() + 0.5);
        double dz = mc().player.getZ() - (position.getZ() + 0.5);
        if (Math.abs(dx) > Math.abs(dz)) {
            return dx > 0 ? Direction.EAST : Direction.WEST;
        }
        return dz > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    private boolean isLog(BlockPos position) {
        if (mc().level == null) {
            return false;
        }
        BlockState state = mc().level.getBlockState(position);
        return state.is(BlockTags.LOGS);
    }

    private BlockPos findTarget() {
        if (mc().player == null || mc().level == null) {
            return null;
        }
        BlockPos origin = mc().player.blockPosition();
        int horizontal = (int) Math.ceil(this.radius.getValue());
        int top = this.height.getValue();
        List<BlockPos> logs = new ArrayList<>();
        for (int y = -2; y <= top; y++) {
            for (int x = -horizontal; x <= horizontal; x++) {
                for (int z = -horizontal; z <= horizontal; z++) {
                    if (x * x + z * z > horizontal * horizontal) {
                        continue;
                    }
                    BlockPos candidate = origin.offset(x, y, z);
                    if (this.isLog(candidate)) {
                        logs.add(candidate);
                    }
                }
            }
        }
        if (logs.isEmpty()) {
            return null;
        }
        Comparator<BlockPos> byHeight = Comparator.comparingInt(BlockPos::getY);
        if (this.topDown.getValue()) {
            byHeight = byHeight.reversed();
        }
        return logs.stream()
                .min(byHeight.thenComparingDouble(pos -> pos.distSqr(origin)))
                .orElse(null);
    }
}
