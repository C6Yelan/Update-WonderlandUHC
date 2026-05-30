package org.mcwonderland.uhc.application.border;

import org.junit.Test;
import org.mcwonderland.uhc.application.world.MatchCenter;
import org.mcwonderland.uhc.port.WorldBorderPort;

import static org.junit.Assert.assertEquals;

public class BorderServiceTest {

    @Test
    public void shrinkBorderKeepsLegacySecondsInputAndCallsPaperWithTicks() {
        CapturingWorldBorderPort port = new CapturingWorldBorderPort();
        BorderService service = new BorderService(port);

        service.shrinkBorder("uhc_world", 500, 10L, new MatchCenter(12, -34, 500));

        assertEquals("uhc_world", port.changedWorldName);
        assertEquals(500D, port.changedSize, 0.00001D);
        assertEquals(200L, port.changedTicks);
        assertEquals(12D, port.centerX, 0.00001D);
        assertEquals(-34D, port.centerZ, 0.00001D);
    }

    private static final class CapturingWorldBorderPort implements WorldBorderPort {
        private String changedWorldName;
        private double changedSize;
        private long changedTicks;
        private double centerX;
        private double centerZ;

        @Override
        public void reset(String worldName) {
        }

        @Override
        public void setCenter(String worldName, double x, double z) {
            centerX = x;
            centerZ = z;
        }

        @Override
        public void setSize(String worldName, double size) {
        }

        @Override
        public void changeSize(String worldName, double size, long ticks) {
            changedWorldName = worldName;
            changedSize = size;
            changedTicks = ticks;
        }

        @Override
        public void setWarningDistance(String worldName, int blocks) {
        }

        @Override
        public void setWarningTimeTicks(String worldName, int ticks) {
        }

        @Override
        public void setDamageBuffer(String worldName, double blocks) {
        }

        @Override
        public double getSize(String worldName) {
            return 0D;
        }
    }
}
