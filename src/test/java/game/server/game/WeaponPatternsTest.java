package game.server.game;

import game.common.model.Coordinate;
import game.common.model.Direction;
import game.common.model.WeaponType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WeaponPatternsTest {

    @Test
    void testStandardShot() {
        List<Coordinate> tiles = WeaponPatterns.getAffectedTiles(
                WeaponType.STANDARD_SHOT, new Coordinate(3, 3), Direction.HORIZONTAL, 8);
        assertEquals(1, tiles.size());
        assertEquals(new Coordinate(3, 3), tiles.get(0));
    }

    @Test
    void testLineBarrageHorizontal() {
        List<Coordinate> tiles = WeaponPatterns.getAffectedTiles(
                WeaponType.LINE_BARRAGE, new Coordinate(4, 4), Direction.HORIZONTAL, 8);
        assertEquals(3, tiles.size());
        assertTrue(tiles.contains(new Coordinate(4, 3)));
        assertTrue(tiles.contains(new Coordinate(4, 4)));
        assertTrue(tiles.contains(new Coordinate(4, 5)));
    }

    @Test
    void testLineBarrageVertical() {
        List<Coordinate> tiles = WeaponPatterns.getAffectedTiles(
                WeaponType.LINE_BARRAGE, new Coordinate(4, 4), Direction.VERTICAL, 8);
        assertEquals(3, tiles.size());
        assertTrue(tiles.contains(new Coordinate(3, 4)));
        assertTrue(tiles.contains(new Coordinate(4, 4)));
        assertTrue(tiles.contains(new Coordinate(5, 4)));
    }

    @Test
    void testLineBarrageClipsEdge() {
        // Fire at column 0 horizontally — left tile clips
        List<Coordinate> tiles = WeaponPatterns.getAffectedTiles(
                WeaponType.LINE_BARRAGE, new Coordinate(4, 0), Direction.HORIZONTAL, 8);
        assertEquals(2, tiles.size()); // only center + right
        assertTrue(tiles.contains(new Coordinate(4, 0)));
        assertTrue(tiles.contains(new Coordinate(4, 1)));
    }

    @Test
    void testCrossBomber() {
        List<Coordinate> tiles = WeaponPatterns.getAffectedTiles(
                WeaponType.CROSS_BOMBER, new Coordinate(4, 4), Direction.HORIZONTAL, 8);
        assertEquals(5, tiles.size());
        assertTrue(tiles.contains(new Coordinate(4, 4))); // center
        assertTrue(tiles.contains(new Coordinate(3, 4))); // up
        assertTrue(tiles.contains(new Coordinate(5, 4))); // down
        assertTrue(tiles.contains(new Coordinate(4, 3))); // left
        assertTrue(tiles.contains(new Coordinate(4, 5))); // right
    }

    @Test
    void testCrossBomberClipsCorner() {
        List<Coordinate> tiles = WeaponPatterns.getAffectedTiles(
                WeaponType.CROSS_BOMBER, new Coordinate(0, 0), Direction.HORIZONTAL, 8);
        assertEquals(3, tiles.size()); // center + right + down
        assertTrue(tiles.contains(new Coordinate(0, 0)));
        assertTrue(tiles.contains(new Coordinate(1, 0)));
        assertTrue(tiles.contains(new Coordinate(0, 1)));
    }

    @Test
    void testNuke() {
        List<Coordinate> tiles = WeaponPatterns.getAffectedTiles(
                WeaponType.NUKE, new Coordinate(4, 4), Direction.HORIZONTAL, 8);
        assertEquals(9, tiles.size());
        // Check all 9 tiles in 3x3
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                assertTrue(tiles.contains(new Coordinate(4 + dr, 4 + dc)));
            }
        }
    }

    @Test
    void testNukeClipsCorner() {
        List<Coordinate> tiles = WeaponPatterns.getAffectedTiles(
                WeaponType.NUKE, new Coordinate(0, 0), Direction.HORIZONTAL, 8);
        assertEquals(4, tiles.size()); // top-left 2x2
    }

    @Test
    void testNukeClipsEdge() {
        List<Coordinate> tiles = WeaponPatterns.getAffectedTiles(
                WeaponType.NUKE, new Coordinate(0, 4), Direction.HORIZONTAL, 8);
        assertEquals(6, tiles.size()); // top edge, 2×3
    }
}
