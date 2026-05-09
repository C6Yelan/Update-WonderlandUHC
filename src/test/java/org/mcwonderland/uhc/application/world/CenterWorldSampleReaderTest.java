package org.mcwonderland.uhc.application.world;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class CenterWorldSampleReaderTest {

    @Test
    public void readerBuildsTerrainSampleFromSource() {
        FakeSource source = new FakeSource();
        source.height(0, 0, 80);
        source.height(8, 0, 86);
        source.height(-8, 0, 77);
        source.height(0, 8, 91);
        source.height(0, -8, 79);
        source.biomeKey = "minecraft:plains";
        source.surfaceMaterialKey = "minecraft:grass_block";
        source.standable = true;

        CenterWorldSampleReader reader = new CenterWorldSampleReader(source, 8);
        CenterTerrainSample sample = reader.sample(new CenterSamplePoint(0, 0));

        assertEquals("minecraft:plains", sample.getBiomeKey());
        assertEquals("minecraft:grass_block", sample.getSurfaceMaterialKey());
        assertEquals(80, sample.getSurfaceY());
        assertEquals(63, sample.getSeaLevel());
        assertEquals(-64, sample.getMinHeight());
        assertEquals(320, sample.getMaxHeight());
        assertEquals(11, sample.getMaxNeighborHeightDifference());
        assertTrue(sample.isStandable());
    }

    @Test
    public void readerCachesSamplesByCoordinate() {
        FakeSource source = new FakeSource();
        source.height(0, 0, 80);
        source.height(16, 0, 81);
        source.height(-16, 0, 82);
        source.height(0, 16, 83);
        source.height(0, -16, 84);

        CenterWorldSampleReader reader = new CenterWorldSampleReader(source, 16);
        CenterSamplePoint point = new CenterSamplePoint(0, 0);

        CenterTerrainSample first = reader.sample(point);
        CenterTerrainSample second = reader.sample(new CenterSamplePoint(0, 0));

        assertSame(first, second);
        assertEquals(1, reader.getCachedSampleCount());
        assertEquals(5, source.surfaceReads);
    }

    @Test
    public void cacheCanBeCleared() {
        FakeSource source = new FakeSource();
        source.height(0, 0, 80);
        source.height(16, 0, 81);
        source.height(-16, 0, 82);
        source.height(0, 16, 83);
        source.height(0, -16, 84);

        CenterWorldSampleReader reader = new CenterWorldSampleReader(source, 16);
        reader.sample(new CenterSamplePoint(0, 0));
        reader.clearCache();

        assertEquals(0, reader.getCachedSampleCount());
    }

    @Test(expected = IllegalArgumentException.class)
    public void readerRejectsInvalidNeighborDistance() {
        new CenterWorldSampleReader(new FakeSource(), 0);
    }

    private static final class FakeSource implements CenterSampleSource {
        private final Map<CenterSamplePoint, Integer> heights = new HashMap<>();
        private int surfaceReads;
        private String biomeKey = "minecraft:plains";
        private String surfaceMaterialKey = "minecraft:grass_block";
        private boolean standable;
        private boolean waterSurface;

        void height(int x, int z, int y) {
            heights.put(new CenterSamplePoint(x, z), y);
        }

        @Override
        public int getMinHeight() {
            return -64;
        }

        @Override
        public int getMaxHeight() {
            return 320;
        }

        @Override
        public int getSeaLevel() {
            return 63;
        }

        @Override
        public int getSurfaceY(int x, int z) {
            surfaceReads++;
            Integer height = heights.get(new CenterSamplePoint(x, z));
            return height == null ? 80 : height;
        }

        @Override
        public String getBiomeKey(int x, int z) {
            return biomeKey;
        }

        @Override
        public String getSurfaceMaterialKey(int x, int z) {
            return surfaceMaterialKey;
        }

        @Override
        public boolean isStandable(int x, int z) {
            return standable;
        }

        @Override
        public boolean isWaterSurface(int x, int z) {
            return waterSurface;
        }
    }
}
