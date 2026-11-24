package com.github.mikumiku.evernight.modules;

import com.github.mikumiku.evernight.MikuCategories;
import com.github.mikumiku.evernight.MikuModule;
import com.github.mikumiku.evernight.util.MikuUtils;
import evernight.client.events.core.EventHandler;
import evernight.client.events.ui.MinecraftTickEvent;
import evernight.client.modules.ModuleCategory;
import evernight.client.settings.core.NamedEntry;
import evernight.client.settings.core.Setting;
import evernight.client.settings.types.BooleanSetting;
import evernight.client.settings.types.EnumSetting;
import evernight.client.settings.types.NumberSetting;
import evernight.client.settings.types.StringSetting;
import evernight.client.utils.inventory.InventoryUtils;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Standalone schematic printer: reads a Litematica {@code .litematic} file and rebuilds it around
 * an anchor position. Ported from {@code BestPrinter}, but without a Litematica dependency so the
 * schematic world is not required.
 */
public final class PrinterModule extends MikuModule {
    public enum Anchor implements NamedEntry {
        Player("Player"),
        Fixed("Fixed");

        private final String name;

        Anchor(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return this.name;
        }
    }

    public enum Rotation implements NamedEntry {
        None("None"),
        Cw90("90"),
        Cw180("180"),
        Cw270("270");

        private final String name;

        Rotation(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return this.name;
        }
    }

    private record Target(BlockPos position, BlockState state) {
    }

    private final Setting<String> file;
    private final Setting<Boolean> reload;
    private final Setting<Anchor> anchor;
    private final Setting<Integer> originX;
    private final Setting<Integer> originY;
    private final Setting<Integer> originZ;
    private final Setting<Rotation> rotation;
    private final Setting<Boolean> mirrorX;
    private final Setting<Boolean> mirrorZ;
    private final Setting<Double> range;
    private final Setting<Integer> blocksPerTick;
    private final Setting<Integer> delayTicks;
    private final Setting<Boolean> rotate;
    private final Setting<Boolean> dirtForGrass;
    private final Setting<Boolean> debug;

    private final List<Target> targets = new ArrayList<>();
    private int cursor;
    private int delayCounter;
    private String loadedName = "";

    public PrinterModule() {
        super("Printer", "Rebuilds a .litematic schematic around you, no Litematica required.",
                MikuCategories.BUILD, "mikuprinter", "printer");
        this.file = this.add(new StringSetting("File", ""));
        this.reload = this.add(new BooleanSetting("Reload", false));
        this.anchor = this.add(new EnumSetting<>("Anchor", Anchor.Player));
        this.originX = this.add(new NumberSetting<>("OriginX", 0, -30000000, 30000000));
        this.originY = this.add(new NumberSetting<>("OriginY", 64, -64, 320));
        this.originZ = this.add(new NumberSetting<>("OriginZ", 0, -30000000, 30000000));
        this.rotation = this.add(new EnumSetting<>("Rotation", Rotation.None));
        this.mirrorX = this.add(new BooleanSetting("MirrorX", false));
        this.mirrorZ = this.add(new BooleanSetting("MirrorZ", false));
        this.range = this.add(new NumberSetting<>("Range", 4.5, 1.0, 6.0));
        this.blocksPerTick = this.add(new NumberSetting<>("BlocksPerTick", 2, 1, 9));
        this.delayTicks = this.add(new NumberSetting<>("DelayTicks", 0, 0, 10));
        this.rotate = this.add(new BooleanSetting("Rotate", true));
        this.dirtForGrass = this.add(new BooleanSetting("DirtForGrass", true));
        this.debug = this.add(new BooleanSetting("Debug", false));
    }

    @Override
    public void onEnable() {
        this.targets.clear();
        this.cursor = 0;
        this.delayCounter = 0;
        this.load();
    }

    @Override
    public void onDisable() {
        this.targets.clear();
    }

    @EventHandler
    private void onTick(MinecraftTickEvent event) {
        if (!this.inGame()) {
            return;
        }
        if (this.reload.getValue()) {
            this.reload.setValue(false);
            this.load();
        }
        if (this.targets.isEmpty()) {
            return;
        }
        if (this.delayCounter > 0) {
            this.delayCounter--;
            return;
        }
        int placed = 0;
        int attempts = 0;
        int total = this.targets.size();
        double maxDistance = this.range.getValue() * this.range.getValue();
        while (placed < this.blocksPerTick.getValue() && attempts < total) {
            attempts++;
            Target target = this.targets.get(this.cursor % total);
            this.cursor = (this.cursor + 1) % total;
            if (mc().player.position().distanceToSqr(target.position().getCenter()) > maxDistance) {
                continue;
            }
            if (this.isAlreadyPlaced(target)) {
                continue;
            }
            if (this.place(target)) {
                placed++;
            }
        }
        if (placed > 0 && this.delayTicks.getValue() > 0) {
            this.delayCounter = this.delayTicks.getValue();
        }
    }

    private boolean isAlreadyPlaced(Target target) {
        BlockState current = mc().level.getBlockState(target.position());
        return current.is(target.state().getBlock());
    }

    private boolean place(Target target) {
        Block block = target.state().getBlock();
        InventoryUtils.SwapSlot slot = InventoryUtils.findSwapSlot(
                stack -> this.matchesBlock(stack, block), true);
        if (!slot.isFound()) {
            if (this.debug.getValue()) {
                this.info("Missing item for " + block.getName().getString());
            }
            return false;
        }
        BlockPos position = target.position();
        if (!MikuUtils.canReplace(position)) {
            return false;
        }
        Direction support = evernight.client.utils.world.BlockInteractionUtils.findSupportDirection(position);
        if (support == null) {
            return false;
        }
        int previousSlot = mc().player.getInventory().getSelectedSlot();
        InventoryUtils.swapTo(slot);
        try {
            this.applyRotation(target.state());
            if (this.rotate.getValue()) {
                MikuUtils.rotateTo(position);
            }
            boolean placed = this.placeWithState(position, target.state(), support);
            if (placed) {
                this.delayCounter = 0;
            }
            return placed;
        } finally {
            InventoryUtils.swapBack(previousSlot, slot);
        }
    }

    /** Turns the player so vanilla derives the same facing as the schematic state. */
    private void applyRotation(BlockState state) {
        Property<?> facing = findProperty(state, "facing");
        if (facing == null) {
            return;
        }
        Object value = valueOf(state, facing);
        if (!(value instanceof Direction direction)) {
            return;
        }
        float yaw = direction.toYRot();
        mc().player.setYRot(yaw);
        mc().player.setYHeadRot(yaw);
        mc().player.setYBodyRot(yaw);
    }

    private boolean placeWithState(BlockPos position, BlockState state, Direction support) {
        // Slabs: a top slab must be placed against the bottom face of the block above.
        if (state.getBlock() instanceof SlabBlock) {
            Property<?> type = findProperty(state, "type");
            if (type != null && "top".equals(String.valueOf(valueOf(state, type)))) {
                BlockPos above = position.above();
                if (!mc().level.getBlockState(above).canBeReplaced()) {
                    return evernight.client.utils.world.BlockInteractionUtils.placeBlock(
                            position, null, support, false, net.minecraft.world.InteractionHand.MAIN_HAND);
                }
                return evernight.client.utils.world.BlockInteractionUtils.placeBlock(
                        position, null, Direction.UP, false, net.minecraft.world.InteractionHand.MAIN_HAND);
            }
        }
        if (state.getBlock() instanceof StairBlock) {
            return evernight.client.utils.world.BlockInteractionUtils.placeBlock(
                    position, null, Direction.UP, false, net.minecraft.world.InteractionHand.MAIN_HAND);
        }
        return MikuUtils.place(position);
    }

    private boolean matchesBlock(ItemStack stack, Block block) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return false;
        }
        if (blockItem.getBlock() == block) {
            return true;
        }
        if (!this.dirtForGrass.getValue()) {
            return false;
        }
        Block held = blockItem.getBlock();
        if (block == Blocks.GRASS_BLOCK || block == Blocks.MYCELIUM || block == Blocks.PODZOL) {
            return held == Blocks.DIRT;
        }
        return false;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Comparable<?> valueOf(BlockState state, Property property) {
        return (Comparable<?>) state.getValue(property);
    }

    private static Property<?> findProperty(BlockState state, String name) {
        for (Property<?> property : state.getProperties()) {
            if (property.getName().equals(name)) {
                return property;
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- loading

    private void load() {
        this.targets.clear();
        this.cursor = 0;
        String pathText = this.file.getValue();
        if (pathText == null || pathText.isBlank()) {
            return;
        }
        Path path = Path.of(pathText.trim());
        if (!Files.isRegularFile(path)) {
            this.warn("Schematic not found: " + path);
            return;
        }
        if (mc().level == null) {
            return;
        }
        try {
            CompoundTag root = NbtIo.readCompressed(path, NbtAccounter.create(512L * 1024 * 1024));
            this.loadedName = root.getCompound("Metadata")
                    .map(metadata -> metadata.getStringOr("Name", path.getFileName().toString()))
                    .orElse(path.getFileName().toString());
            BlockPos anchorPos = this.resolveAnchor();
            HolderLookup<Block> lookup = mc().level.holderLookup(Registries.BLOCK);
            Map<BlockPos, BlockState> merged = new HashMap<>();
            CompoundTag regions = root.getCompoundOrEmpty("Regions");
            for (String regionName : regions.keySet()) {
                this.readRegion(regions.getCompoundOrEmpty(regionName), lookup, anchorPos, merged);
            }
            merged.forEach((position, state) -> this.targets.add(new Target(position, state)));
            this.targets.sort(Comparator.comparingDouble(
                    target -> mc().player == null ? 0.0
                            : mc().player.distanceToSqr(target.position().getCenter())));
            this.info("Loaded " + this.loadedName + ": " + this.targets.size() + " blocks");
        } catch (Exception exception) {
            this.warn("Failed to read schematic: " + exception.getMessage());
        }
    }

    private BlockPos resolveAnchor() {
        if (this.anchor.getValue() == Anchor.Player && mc().player != null) {
            BlockPos player = mc().player.blockPosition();
            return player.offset(this.originX.getValue(), this.originY.getValue(), this.originZ.getValue());
        }
        return new BlockPos(this.originX.getValue(), this.originY.getValue(), this.originZ.getValue());
    }

    private void readRegion(CompoundTag region, HolderLookup<Block> lookup, BlockPos anchor,
                            Map<BlockPos, BlockState> out) {
        ListTag paletteTag = region.getListOrEmpty("BlockStatePalette");
        long[] data = region.getLongArray("BlockStates").orElse(null);
        if (data == null || paletteTag.isEmpty()) {
            return;
        }
        BlockState[] palette = new BlockState[paletteTag.size()];
        for (int index = 0; index < paletteTag.size(); index++) {
            palette[index] = this.parseState(paletteTag.getCompoundOrEmpty(index), lookup);
        }
        Vec3i position = readVec(region.getCompoundOrEmpty("Position"));
        Vec3i size = readVec(region.getCompoundOrEmpty("Size"));
        int sizeX = Math.abs(size.getX());
        int sizeY = Math.abs(size.getY());
        int sizeZ = Math.abs(size.getZ());
        if (sizeX == 0 || sizeY == 0 || sizeZ == 0) {
            return;
        }
        int originX = size.getX() >= 0 ? position.getX() : position.getX() + size.getX() + 1;
        int originY = size.getY() >= 0 ? position.getY() : position.getY() + size.getY() + 1;
        int originZ = size.getZ() >= 0 ? position.getZ() : position.getZ() + size.getZ() + 1;

        int bits = Math.max(2, 32 - Integer.numberOfLeadingZeros(Math.max(1, palette.length - 1)));
        double centerX = (sizeX - 1) / 2.0;
        double centerZ = (sizeZ - 1) / 2.0;

        for (int y = 0; y < sizeY; y++) {
            for (int z = 0; z < sizeZ; z++) {
                for (int x = 0; x < sizeX; x++) {
                    int index = (y * sizeZ + z) * sizeX + x;
                    int paletteIndex = readBits(data, bits, index);
                    if (paletteIndex < 0 || paletteIndex >= palette.length) {
                        continue;
                    }
                    BlockState state = palette[paletteIndex];
                    if (state.isAir()) {
                        continue;
                    }
                    double localX = this.mirrorX.getValue() ? sizeX - 1 - x : x;
                    double localZ = this.mirrorZ.getValue() ? sizeZ - 1 - z : z;
                    double dx = localX - centerX;
                    double dz = localZ - centerZ;
                    double rotatedX;
                    double rotatedZ;
                    switch (this.rotation.getValue()) {
                        case Cw90 -> {
                            rotatedX = -dz;
                            rotatedZ = dx;
                        }
                        case Cw180 -> {
                            rotatedX = -dx;
                            rotatedZ = -dz;
                        }
                        case Cw270 -> {
                            rotatedX = dz;
                            rotatedZ = -dx;
                        }
                        default -> {
                            rotatedX = dx;
                            rotatedZ = dz;
                        }
                    }
                    int worldX = (int) Math.round(centerX + rotatedX);
                    int worldZ = (int) Math.round(centerZ + rotatedZ);
                    BlockPos world = new BlockPos(
                            anchor.getX() + worldX,
                            anchor.getY() + originY + y,
                            anchor.getZ() + worldZ);
                    out.put(world, state);
                }
            }
        }
    }

    private BlockState parseState(CompoundTag entry, HolderLookup<Block> lookup) {
        String name = entry.getStringOr("Name", "minecraft:air");
        StringBuilder text = new StringBuilder(name);
        Optional<CompoundTag> properties = entry.getCompound("Properties");
        if (properties.isPresent() && !properties.get().isEmpty()) {
            text.append('[');
            boolean first = true;
            for (String key : properties.get().keySet()) {
                if (!first) {
                    text.append(',');
                }
                text.append(key).append('=').append(properties.get().getStringOr(key, ""));
                first = false;
            }
            text.append(']');
        }
        try {
            return BlockStateParser.parseForBlock(lookup, text.toString(), false).blockState();
        } catch (Exception exception) {
            return Blocks.AIR.defaultBlockState();
        }
    }

    private static Vec3i readVec(CompoundTag tag) {
        return new Vec3i(tag.getInt("x").orElse(0), tag.getInt("y").orElse(0), tag.getInt("z").orElse(0));
    }

    private static int readBits(long[] data, int bits, int index) {
        long startOffset = (long) index * bits;
        int startIndex = (int) (startOffset >> 6);
        int endIndex = (int) (((index + 1L) * bits - 1) >> 6);
        if (startIndex < 0 || startIndex >= data.length) {
            return 0;
        }
        int startBit = (int) (startOffset & 63);
        long mask = (1L << bits) - 1L;
        if (startIndex == endIndex || endIndex >= data.length) {
            return (int) ((data[startIndex] >>> startBit) & mask);
        }
        int endOffset = 64 - startBit;
        long value = (data[startIndex] >>> startBit) | (data[endIndex] << endOffset);
        return (int) (value & mask);
    }
}
