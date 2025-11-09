package kaptainwutax.seedcrackerX.finder.decorator;

import com.seedfinding.mccore.version.MCVersion;
import kaptainwutax.seedcrackerX.SeedCracker;
import kaptainwutax.seedcrackerX.config.Config;
import kaptainwutax.seedcrackerX.cracker.DataAddedEvent;
import kaptainwutax.seedcrackerX.cracker.PillarData;
import kaptainwutax.seedcrackerX.finder.BlockFinder;
import kaptainwutax.seedcrackerX.finder.Finder;
import kaptainwutax.seedcrackerX.render.Cuboid;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.DimensionType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EndPillarsFinder extends Finder {

    private final boolean alreadyFound;
    protected BedrockMarkerFinder[] bedrockMarkers = new BedrockMarkerFinder[10];

    public EndPillarsFinder(Level world, ChunkPos chunkPos) {
        super(world, chunkPos);

        this.alreadyFound = !SeedCracker.get().getDataStorage().addPillarData(null, DataAddedEvent.POKE_PILLARS);
        if (this.alreadyFound) return;

        for (int i = 0; i < this.bedrockMarkers.length; i++) {
            double x = 42.0D * Math.cos(2.0D * (-Math.PI + (Math.PI / 10.0D) * (double) i));
            double z = 42.0D * Math.sin(2.0D * (-Math.PI + (Math.PI / 10.0D) * (double) i));
            if (Config.get().getVersion().isOlderThan(MCVersion.v1_14)) {
                x = Math.round(x);
                z = Math.round(z);
            }
            this.bedrockMarkers[i] = new BedrockMarkerFinder(this.world, new ChunkPos(BlockPos.containing(x, 0, z)), BlockPos.containing(x, 0, z));
        }
    }

    public static List<Finder> create(Level world, ChunkPos chunkPos) {
        List<Finder> finders = new ArrayList<>();
        finders.add(new EndPillarsFinder(world, chunkPos));
        return finders;
    }

    @Override
    public List<BlockPos> findInChunk() {
        List<BlockPos> result = new ArrayList<>();

        for (BedrockMarkerFinder bedrockMarker : this.bedrockMarkers) {
            if (bedrockMarker == null) continue;
            result.addAll(bedrockMarker.findInChunk());
        }

        if (result.size() == this.bedrockMarkers.length) {
            PillarData pillarData = new PillarData(result.stream().map(Vec3i::getY).collect(Collectors.toList()));

            if (SeedCracker.get().getDataStorage().addPillarData(pillarData, DataAddedEvent.POKE_PILLARS)) {
                result.forEach(pos -> this.cuboids.add(new Cuboid(pos, ARGB.color(128, 0, 128))));
            }

        }

        return result;
    }

    @Override
    public boolean isValidDimension(DimensionType dimension) {
        return this.isEnd(dimension);
    }

    public static class BedrockMarkerFinder extends BlockFinder {

        protected static List<BlockPos> SEARCH_POSITIONS;
        // 新增：定义需要校验的上方方块（黑曜石）
        private static final BlockPos ABOVE_OFFSET = new BlockPos(0, 1, 0);

        public BedrockMarkerFinder(Level world, ChunkPos chunkPos, BlockPos xz) {
            super(world, chunkPos, Blocks.BEDROCK);
            this.searchPositions = SEARCH_POSITIONS;
        }

        public static void reloadSearchPositions() {
            SEARCH_POSITIONS = buildSearchPositions(CHUNK_POSITIONS, pos -> {
                if (pos.getY() < 76) return true;
                return pos.getY() > 76 + 3 * 10;
            });
        }

        @Override
        public List<BlockPos> findInChunk() {
            // 1. 先调用父类方法找到所有基岩位置
            List<BlockPos> baseBedrocks = super.findInChunk();
            // 2. 过滤出“上方一格为黑曜石”的基岩位置
            return baseBedrocks.stream()
                    .filter(pos -> {
                        // 计算上方一格的坐标
                        BlockPos abovePos = pos.offset(ABOVE_OFFSET);
                        // 校验上方方块是否为黑曜石
                        return world.getBlockState(abovePos).getBlock() == Blocks.OBSIDIAN;
                    })
                    .collect(Collectors.toList());
        }

        @Override
        public boolean isValidDimension(DimensionType dimension) {
            return true;
        }

    }

}
