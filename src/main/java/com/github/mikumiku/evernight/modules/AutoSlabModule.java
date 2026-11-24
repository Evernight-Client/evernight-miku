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
import evernight.client.utils.inventory.InventoryUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SlabBlock;

/** Places slabs on the ground to stop mob spawning. Ported from {@code AutoSlab}. */
public final class AutoSlabModule extends MikuModule {
    private final Setting<Double> range;
    private final Setting<Integer> delay;
    private final Setting<Boolean> rotate;
    private final Setting<Boolean> onlyOnGround;

    private long lastPlace;

    public AutoSlabModule() {
        super("AutoSlabPlus", "Places slabs on the ground around you.", MikuCategories.BUILD, "mikuslab");
        this.range = this.add(new NumberSetting<>("Range", 4.0, 1.0, 6.0));
        this.delay = this.add(new NumberSetting<>("Delay", 50, 0, 500));
        this.rotate = this.add(new BooleanSetting("Rotate", true));
        this.onlyOnGround = this.add(new BooleanSetting("OnlyOnGround", true));
    }

    @Override
    public void onEnable() {
        this.lastPlace = 0L;
    }

    @EventHandler
    private void onTick(MinecraftTickEvent event) {
        if (!this.inGame()) {
            return;
        }
        if (this.onlyOnGround.getValue() && !mc().player.onGround()) {
            return;
        }
        if (System.currentTimeMillis() - this.lastPlace < this.delay.getValue()) {
            return;
        }
        InventoryUtils.SwapSlot slot = InventoryUtils.findSwapSlot(this::isSlab, true);
        if (!slot.isFound()) {
            return;
        }
        BlockPos target = this.findTarget();
        if (target == null) {
            return;
        }
        int previousSlot = mc().player.getInventory().getSelectedSlot();
        if (this.rotate.getValue()) {
            MikuUtils.rotateTo(target);
        }
        if (MikuUtils.placeWithSwap(target, slot, previousSlot)) {
            this.lastPlace = System.currentTimeMillis();
        }
    }

    private boolean isSlab(ItemStack stack) {
        return stack.getItem() instanceof net.minecraft.world.item.BlockItem blockItem
                && blockItem.getBlock() instanceof SlabBlock;
    }

    private BlockPos findTarget() {
        BlockPos origin = mc().player.blockPosition();
        int radius = (int) Math.ceil(this.range.getValue());
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                BlockPos candidate = origin.offset(x, -1, z);
                if (!MikuUtils.canReplace(candidate)) {
                    continue;
                }
                if (mc().player.position().distanceToSqr(candidate.getCenter())
                        > this.range.getValue() * this.range.getValue()) {
                    continue;
                }
                return candidate;
            }
        }
        return null;
    }
}
