package game.server.game;

import game.common.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AttackResolverTest {

    private Board board;
    private AttackResolver resolver;

    @BeforeEach
    void setUp() {
        board = new Board(8);
        resolver = new AttackResolver();

        // Place a patrol boat at (2,2)-(2,3)
        Ship patrol = new Ship(ShipType.PATROL_BOAT, "PB-A", List.of(
                new Coordinate(2, 2),
                new Coordinate(2, 3)));
        board.placeShip(patrol);

        // Place a destroyer at (5,1)-(5,2)-(5,3)
        Ship destroyer = new Ship(ShipType.DESTROYER, "DS-A", List.of(
                new Coordinate(5, 1),
                new Coordinate(5, 2),
                new Coordinate(5, 3)));
        board.placeShip(destroyer);
    }

    @Test
    void testStandardShot_miss() {
        AttackResult result = resolver.resolve(
                board, WeaponType.STANDARD_SHOT, new Coordinate(0, 0), Direction.HORIZONTAL);

        assertEquals(1, result.getTileResults().size());
        assertEquals(CellState.MISS, result.getTileResults().get(0).outcome());
        assertEquals(0, result.hitCount());
        assertEquals(1, result.missCount());
        assertTrue(result.getSunkShips().isEmpty());
        assertFalse(result.isGameOver());
    }

    @Test
    void testStandardShot_hit() {
        AttackResult result = resolver.resolve(
                board, WeaponType.STANDARD_SHOT, new Coordinate(2, 2), Direction.HORIZONTAL);

        assertEquals(1, result.getTileResults().size());
        assertEquals(CellState.HIT, result.getTileResults().get(0).outcome());
        assertEquals(1, result.hitCount());
        assertTrue(result.getSunkShips().isEmpty());
    }

    @Test
    void testStandardShot_sinks() {
        // Hit first cell
        resolver.resolve(board, WeaponType.STANDARD_SHOT, new Coordinate(2, 2), Direction.HORIZONTAL);
        // Hit second cell — should sink
        AttackResult result = resolver.resolve(
                board, WeaponType.STANDARD_SHOT, new Coordinate(2, 3), Direction.HORIZONTAL);

        assertEquals(CellState.SUNK, result.getTileResults().get(0).outcome());
        assertEquals(1, result.getSunkShips().size());
        assertEquals("PB-A", result.getSunkShips().get(0).getId());
    }

    @Test
    void testLineBarrage_horizontal() {
        // Fire line barrage centered at (5,2) — should hit (5,1), (5,2), (5,3)
        AttackResult result = resolver.resolve(
                board, WeaponType.LINE_BARRAGE, new Coordinate(5, 2), Direction.HORIZONTAL);

        assertEquals(3, result.getTileResults().size());
        assertEquals(3, result.hitCount());
        // All 3 hits sink the destroyer
        assertEquals(1, result.getSunkShips().size());
        assertEquals("DS-A", result.getSunkShips().get(0).getId());
    }

    @Test
    void testCrossBomber_partialHit() {
        // Cross bomber centered at (2,3) — center hits patrol at (2,3),
        // up(1,3) miss, down(3,3) miss, left(2,2) hits patrol, right(2,4) miss
        AttackResult result = resolver.resolve(
                board, WeaponType.CROSS_BOMBER, new Coordinate(2, 3), Direction.HORIZONTAL);

        assertEquals(5, result.getTileResults().size());
        assertEquals(2, result.hitCount()); // (2,3) and (2,2) — but sinks, so SUNK counts as hit
        assertEquals(1, result.getSunkShips().size());
    }

    @Test
    void testNuke_multipleShips() {
        // Place ships close enough that a nuke can hit both
        Board smallBoard = new Board(8);
        Ship pb = new Ship(ShipType.PATROL_BOAT, "PB-A", List.of(
                new Coordinate(3, 3),
                new Coordinate(3, 4)));
        Ship sub = new Ship(ShipType.SUBMARINE, "SUB-A", List.of(
                new Coordinate(5, 3),
                new Coordinate(5, 4),
                new Coordinate(5, 5)));
        smallBoard.placeShip(pb);
        smallBoard.placeShip(sub);

        // Nuke at (4,4) — 3x3 covers (3,3)-(5,5)
        AttackResult result = resolver.resolve(
                smallBoard, WeaponType.NUKE, new Coordinate(4, 4), Direction.HORIZONTAL);

        assertEquals(9, result.getTileResults().size());
        assertTrue(result.hitCount() > 0);
        // PB at (3,3)(3,4) — both in range → sunk
        // SUB at (5,3)(5,4)(5,5) — all in range → sunk
        assertEquals(2, result.getSunkShips().size());
        assertTrue(smallBoard.allShipsSunk());
        assertTrue(result.isGameOver());
    }

    @Test
    void testGameOver_allShipsSunk() {
        // Sink patrol boat
        resolver.resolve(board, WeaponType.STANDARD_SHOT, new Coordinate(2, 2), Direction.HORIZONTAL);
        resolver.resolve(board, WeaponType.STANDARD_SHOT, new Coordinate(2, 3), Direction.HORIZONTAL);

        // Sink destroyer
        resolver.resolve(board, WeaponType.STANDARD_SHOT, new Coordinate(5, 1), Direction.HORIZONTAL);
        resolver.resolve(board, WeaponType.STANDARD_SHOT, new Coordinate(5, 2), Direction.HORIZONTAL);
        AttackResult result = resolver.resolve(
                board, WeaponType.STANDARD_SHOT, new Coordinate(5, 3), Direction.HORIZONTAL);

        assertTrue(result.isGameOver());
    }
}
