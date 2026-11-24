package com.github.mikumiku.evernight.util;

import evernight.client.managers.player.RotationManager;
import evernight.client.utils.inventory.InventoryUtils;
import evernight.client.utils.world.BlockInteractionUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/** Small helpers shared by the ported Miku modules. */
public final class MikuUtils {
    public static final Set<Block> DEFAULT_FILL_BLOCKS = Set.of(
            Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN, Blocks.ENDER_CHEST, Blocks.NETHERITE_BLOCK,
            Blocks.ANCIENT_DEBRIS, Blocks.RESPAWN_ANCHOR, Blocks.ENCHANTING_TABLE);

    private MikuUtils() {
    }

    public static Minecraft mc() {
        return Minecraft.getInstance();
    }

    public static boolean isMoving() {
        Minecraft client = mc();
        return client.options.keyUp.isDown() || client.options.keyDown.isDown()
                || client.options.keyLeft.isDown() || client.options.keyRight.isDown();
    }

    public static Vec3 playerPos() {
        LocalPlayer player = mc().player;
        return player == null ? Vec3.ZERO : player.position();
    }

    public static BlockPos playerBlockPos() {
        LocalPlayer player = mc().player;
        return player == null ? BlockPos.ZERO : player.blockPosition();
    }

    public static boolean canReplace(BlockPos position) {
        return mc().level != null && mc().level.getBlockState(position).canBeReplaced();
    }

    public static InventoryUtils.SwapSlot findBlockSlot(boolean includeInventory) {
        return findBlockSlot(includeInventory, MikuUtils::isFillBlock);
    }

    public static InventoryUtils.SwapSlot findBlockSlot(boolean includeInventory, Predicate<ItemStack> predicate) {
        return InventoryUtils.findSwapSlot(predicate, includeInventory);
    }

    public static boolean isFillBlock(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return false;
        }
        return DEFAULT_FILL_BLOCKS.contains(blockItem.getBlock());
    }

    /** Places the currently held block at {@code position}, returning whether a placement was sent. */
    public static boolean place(BlockPos position) {
        Direction direction = BlockInteractionUtils.findSupportDirection(position);
        if (direction == null) {
            return false;
        }
        return BlockInteractionUtils.placeBlock(position, null, direction, false, InteractionHand.MAIN_HAND);
    }

    /** Places a block after swapping to it, restoring the previous slot afterwards. */
    public static boolean placeWithSwap(BlockPos position, InventoryUtils.SwapSlot slot, int previousSlot) {
        if (!slot.isFound()) {
            return false;
        }
        InventoryUtils.swapTo(slot);
        try {
            return place(position);
        } finally {
            InventoryUtils.swapBack(previousSlot, slot);
        }
    }

    public static void rotateTo(BlockPos position) {
        if (mc().player == null) {
            return;
        }
        float[] angles = RotationManager.getRotationsToBlock(position);
        if (angles != null) {
            evernight.client.ClientContext.rotationManager.requestRotation(angles, 50);
        }
    }

    public static void rotateTo(Vec3 position) {
        if (mc().player == null) {
            return;
        }
        float[] angles = RotationManager.getRotationsToPosition(position);
        if (angles != null) {
            evernight.client.ClientContext.rotationManager.requestRotation(angles, 50);
        }
    }

    public static boolean isAir(BlockPos position) {
        return mc().level != null && mc().level.getBlockState(position).isAir();
    }

    public static boolean isHole(BlockPos position) {
        if (mc().level == null || !isAir(position) || !isAir(position.above())) {
            return false;
        }
        BlockPos below = position.below();
        return !canReplace(below) && !canReplace(position.north()) && !canReplace(position.south())
                && !canReplace(position.east()) && !canReplace(position.west());
    }

    public static List<BlockPos> surroundPositions(BlockPos feet) {
        List<BlockPos> positions = new ArrayList<>(6);
        positions.add(feet.north());
        positions.add(feet.south());
        positions.add(feet.east());
        positions.add(feet.west());
        positions.add(feet.above());
        return positions;
    }

    public static boolean hasEntity(BlockPos position) {
        if (mc().level == null) {
            return false;
        }
        AABB box = new AABB(position);
        for (Entity entity : mc().level.getEntities((Entity) null, box)) {
            if (entity != mc().player) {
                return true;
            }
        }
        return false;
    }

    public static Entity nearestEntity(double range) {
        LocalPlayer player = mc().player;
        if (player == null || mc().level == null) {
            return null;
        }
        Entity best = null;
        double bestDistance = range * range;
        for (Entity entity : mc().level.entitiesForRendering()) {
            if (entity == player || !entity.isAlive() || entity.isRemoved()) {
                continue;
            }
            double distance = player.distanceToSqr(entity);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = entity;
            }
        }
        return best;
    }
}
