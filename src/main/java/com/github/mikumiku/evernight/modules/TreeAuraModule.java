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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;

/** Plants and grows trees around the player. Ported from {@code TreeAura}. */
public final class TreeAuraModule extends MikuModule {
    private final Setting<Double> range;
    private final Setting<Integer> delay;
    private final Setting<Boolean> bonemeal;

    private long lastAction;

    public TreeAuraModule() {
        super("TreeAuraPlus", "Plants saplings and grows them.", MikuCategories.BUILD, "mikutree");
        this.range = this.add(new NumberSetting<>("Range", 4.0, 1.0, 6.0));
        this.delay = this.add(new NumberSetting<>("Delay", 200, 0, 1000));
        this.bonemeal = this.add(new BooleanSetting("Bonemeal", true));
    }

    @Override
    public void onEnable() {
        this.lastAction = 0L;
    }

    @EventHandler
    private void onTick(MinecraftTickEvent event) {
        if (!this.inGame() || System.currentTimeMillis() - this.lastAction < this.delay.getValue()) {
            return;
        }
        BlockPos target = this.findPlantPosition();
        if (target == null) {
            return;
        }
        InventoryUtils.SwapSlot slot = InventoryUtils.findSwapSlot(this::isSapling, true);
        if (!slot.isFound()) {
            return;
        }
        int previousSlot = mc().player.getInventory().getSelectedSlot();
        InventoryUtils.swapTo(slot);
        try {
            MikuUtils.rotateTo(target);
            MikuUtils.place(target);
            this.lastAction = System.currentTimeMillis();
        } finally {
            InventoryUtils.swapBack(previousSlot, slot);
        }
    }

    private boolean isSapling(ItemStack stack) {
        return stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof SaplingBlock;
    }

    private BlockPos findPlantPosition() {
        BlockPos origin = mc().player.blockPosition();
        int radius = (int) Math.ceil(this.range.getValue());
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                BlockPos ground = origin.offset(x, -1, z);
                if (!mc().level.getBlockState(ground).is(Blocks.GRASS_BLOCK)
                        && !mc().level.getBlockState(ground).is(Blocks.DIRT)) {
                    continue;
                }
                BlockPos above = ground.above();
                if (!MikuUtils.canReplace(above) || MikuUtils.hasEntity(above)) {
                    continue;
                }
                if (mc().player.position().distanceToSqr(above.getCenter())
                        > this.range.getValue() * this.range.getValue()) {
                    continue;
                }
                return above;
            }
        }
        return null;
    }
}
