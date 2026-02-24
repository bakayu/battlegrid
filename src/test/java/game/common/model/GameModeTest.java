package game.common.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameModeTest {

    @Test
    void testBlitzConfig() {
        assertEquals(8, GameMode.BLITZ.getGridSize());
        assertEquals(3, GameMode.BLITZ.getShipCount());
    }

    @Test
    void testStrikeConfig() {
        assertEquals(12, GameMode.STRIKE.getGridSize());
        assertEquals(5, GameMode.STRIKE.getShipCount());
    }

    @Test
    void testWarConfig() {
        assertEquals(16, GameMode.WAR.getGridSize());
        assertEquals(7, GameMode.WAR.getShipCount());
        // War should have 2 patrol boats and 2 destroyers
        long pbCount = GameMode.WAR.getFleet().stream()
                .filter(s -> s == ShipType.PATROL_BOAT).count();
        long dsCount = GameMode.WAR.getFleet().stream()
                .filter(s -> s == ShipType.DESTROYER).count();
        assertEquals(2, pbCount);
        assertEquals(2, dsCount);
    }
}
