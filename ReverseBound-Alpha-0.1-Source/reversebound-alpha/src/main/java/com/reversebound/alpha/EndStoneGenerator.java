package com.reversebound.alpha;

import org.bukkit.Material;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import java.util.Random;

public final class EndStoneGenerator extends ChunkGenerator {
    @Override
    public void generateNoise(WorldInfo info, Random random, int chunkX, int chunkZ, ChunkData data) {
        final int minY = data.getMinHeight();
        final int maxY = data.getMaxHeight();
        // Fixed roof and floor: the test world is an enclosed stone mass.
        final int roof = Math.min(128, maxY - 5);
        data.setRegion(0, minY, 0, 16, minY + 4, 16, Material.BEDROCK);
        data.setRegion(0, minY + 4, 0, 16, roof, 16, Material.END_STONE);
        data.setRegion(0, roof, 0, 16, roof + 4, 16, Material.BEDROCK);
    }
}
