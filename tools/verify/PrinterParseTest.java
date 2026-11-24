import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Feeds a synthetic .litematic through PrinterModule.readRegion to validate the parser. */
public class PrinterParseTest {
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        CompoundTag root = NbtIo.readCompressed(Path.of(args[0]), NbtAccounter.create(16L * 1024 * 1024));
        CompoundTag regions = root.getCompoundOrEmpty("Regions");
        CompoundTag region = regions.getCompoundOrEmpty("Main");

        HolderLookup<Block> lookup = BuiltInRegistries.BLOCK;
        Class<?> clazz = Class.forName("com.github.mikumiku.evernight.modules.PrinterModule");
        Object module = clazz.getDeclaredConstructor().newInstance();
        Method readRegion = clazz.getDeclaredMethod("readRegion", CompoundTag.class, HolderLookup.class,
                BlockPos.class, Map.class);
        readRegion.setAccessible(true);

        Map<BlockPos, BlockState> out = new HashMap<>();
        readRegion.invoke(module, region, lookup, new BlockPos(100, 64, 100), out);
        System.out.println("blocks parsed: " + out.size());
        out.entrySet().stream()
                .sorted(Map.Entry.comparingByKey((a, b) -> {
                    int cmp = Integer.compare(a.getY(), b.getY());
                    if (cmp != 0) return cmp;
                    cmp = Integer.compare(a.getZ(), b.getZ());
                    return cmp != 0 ? cmp : Integer.compare(a.getX(), b.getX());
                }))
                .forEach(entry -> System.out.println("  " + entry.getKey().toShortString()
                        + " -> " + entry.getValue()));
        System.out.println(out.size() == 8 ? "OK: parsed 8 non-air blocks" : "FAIL: unexpected block count");
    }
}
