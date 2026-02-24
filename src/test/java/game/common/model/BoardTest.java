package game.common.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BoardTest {

    private Board board;

    @BeforeEach
    void setUp() {
        board = new Board(8);
    }

    @Test
    void testNewBoardIsEmpty() {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                assertEquals(CellState.EMPTY, board.getCell(r, c));
            }
        }
    }

    @Test
    void testPlaceShip() {
        List<Coordinate> cells = List.of(
                new Coordinate(0, 0),
                new Coordinate(0, 1));
        Ship ship = new Ship(ShipType.PATROL_BOAT, "PB-A", cells);
        board.placeShip(ship);

        assertEquals(CellState.SHIP, board.getCell(0, 0));
        assertEquals(CellState.SHIP, board.getCell(0, 1));
        assertEquals(CellState.EMPTY, board.getCell(0, 2));
        assertEquals(1, board.getShips().size());
    }

    @Test
    void testAttackMiss() {
        CellState result = board.attack(new Coordinate(3, 3));
        assertEquals(CellState.MISS, result);
        assertEquals(CellState.MISS, board.getCell(3, 3));
    }

    @Test
    void testAttackHit() {
        Ship ship = new Ship(ShipType.PATROL_BOAT, "PB-A", List.of(
                new Coordinate(2, 2),
                new Coordinate(2, 3)));
        board.placeShip(ship);

        CellState result = board.attack(new Coordinate(2, 2));
        assertEquals(CellState.HIT, result);
    }

    @Test
    void testAttackSinksShip() {
        Ship ship = new Ship(ShipType.PATROL_BOAT, "PB-A", List.of(
                new Coordinate(2, 2),
                new Coordinate(2, 3)));
        board.placeShip(ship);

        board.attack(new Coordinate(2, 2));
        CellState result = board.attack(new Coordinate(2, 3));
        assertEquals(CellState.SUNK, result);
        assertTrue(ship.isSunk());

        // Both cells should be SUNK
        assertEquals(CellState.SUNK, board.getCell(2, 2));
        assertEquals(CellState.SUNK, board.getCell(2, 3));
    }

    @Test
    void testAllShipsSunk() {
        Ship pb = new Ship(ShipType.PATROL_BOAT, "PB-A", List.of(
                new Coordinate(0, 0),
                new Coordinate(0, 1)));
        Ship sub = new Ship(ShipType.SUBMARINE, "SUB-A", List.of(
                new Coordinate(4, 4),
                new Coordinate(4, 5),
                new Coordinate(4, 6)));
        board.placeShip(pb);
        board.placeShip(sub);

        assertFalse(board.allShipsSunk());

        // Sink patrol boat
        board.attack(new Coordinate(0, 0));
        board.attack(new Coordinate(0, 1));
        assertFalse(board.allShipsSunk());

        // Sink submarine
        board.attack(new Coordinate(4, 4));
        board.attack(new Coordinate(4, 5));
        board.attack(new Coordinate(4, 6));
        assertTrue(board.allShipsSunk());
    }

    @Test
    void testCanPlaceShip_valid() {
        List<Coordinate> cells = List.of(
                new Coordinate(3, 3),
                new Coordinate(3, 4),
                new Coordinate(3, 5));
        assertTrue(board.canPlaceShip(cells));
    }

    @Test
    void testCanPlaceShip_outOfBounds() {
        List<Coordinate> cells = List.of(
                new Coordinate(7, 6),
                new Coordinate(7, 7),
                new Coordinate(7, 8) // out of bounds
        );
        assertFalse(board.canPlaceShip(cells));
    }

    @Test
    void testCanPlaceShip_overlap() {
        Ship existing = new Ship(ShipType.PATROL_BOAT, "PB-A", List.of(
                new Coordinate(3, 3),
                new Coordinate(3, 4)));
        board.placeShip(existing);

        List<Coordinate> cells = List.of(
                new Coordinate(3, 4),
                new Coordinate(3, 5));
        assertFalse(board.canPlaceShip(cells));
    }

    @Test
    void testCanPlaceShip_tooCloseGap() {
        Ship existing = new Ship(ShipType.PATROL_BOAT, "PB-A", List.of(
                new Coordinate(3, 3),
                new Coordinate(3, 4)));
        board.placeShip(existing);

        // Adjacent (diagonal) — should fail due to 1-tile gap rule
        List<Coordinate> cells = List.of(
                new Coordinate(4, 5),
                new Coordinate(4, 6));
        assertFalse(board.canPlaceShip(cells));
    }

    @Test
    void testCanPlaceShip_respectsGap() {
        Ship existing = new Ship(ShipType.PATROL_BOAT, "PB-A", List.of(
                new Coordinate(3, 3),
                new Coordinate(3, 4)));
        board.placeShip(existing);

        // 2 rows away — should succeed
        List<Coordinate> cells = List.of(
                new Coordinate(5, 3),
                new Coordinate(5, 4));
        assertTrue(board.canPlaceShip(cells));
    }

    @Test
    void testAliveShipsOfType() {
        Ship pb = new Ship(ShipType.PATROL_BOAT, "PB-A", List.of(
                new Coordinate(0, 0),
                new Coordinate(0, 1)));
        board.placeShip(pb);

        assertEquals(1, board.aliveShipsOfType(ShipType.PATROL_BOAT));
        assertEquals(0, board.aliveShipsOfType(ShipType.DESTROYER));

        // Sink it
        board.attack(new Coordinate(0, 0));
        board.attack(new Coordinate(0, 1));
        assertEquals(0, board.aliveShipsOfType(ShipType.PATROL_BOAT));
    }

    @Test
    void testDoubleAttackSameCell() {
        CellState first = board.attack(new Coordinate(5, 5));
        assertEquals(CellState.MISS, first);

        CellState second = board.attack(new Coordinate(5, 5));
        assertEquals(CellState.MISS, second); // returns current state, no change
    }
}
