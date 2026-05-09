package org.mcwonderland.uhc.application.world;

interface CenterSampleSource {

    int getMinHeight();

    int getMaxHeight();

    int getSeaLevel();

    int getSurfaceY(int x, int z);

    String getBiomeKey(int x, int z);

    String getSurfaceMaterialKey(int x, int z);

    boolean isStandable(int x, int z);

    boolean isWaterSurface(int x, int z);
}
