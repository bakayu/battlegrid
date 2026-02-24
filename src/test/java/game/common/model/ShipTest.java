package game.common.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ShipTest {

    @Test
    void testNewShipIsNotSunk() {
        Ship ship = new Ship(ShipType.DESTROYER, "DS-A", List.of(
                new Coordinate(0, 0),
                new Coordinate(0, 1),
                new Coordinate(0, 2)));
        assertFalse(ship.isSunk());
        assertEquals(3, ship.remainingHealth());
    }

    @Test
    void testHitReducesHealth() {
        Ship ship = new Ship(ShipType.PATROL_BOAT, "PB-A", List.of(
                new Coordinate(0, 0),
                new Coordinate(0, 1)));
        assertTrue(ship.hit(new Coordinate(0, 0)));
        assertEquals(1, ship.remainingHealth());
        assertFalse(ship.isSunk());
    }

    @Test
    void testShipSinksWhenAllHit() {
        Ship ship = new Ship(ShipType.PATROL_BOAT, "PB-A", List.of(
                new Coordinate(0, 0),
                new Coordinate(0, 1)));
        ship.hit(new Coordinate(0, 0));
        ship.hit(new Coordinate(0, 1));
        assertTrue(ship.isSunk());
        assertEquals(0, ship.remainingHealth());
    }

    @Test
    void testHitOnWrongCoordinateReturnsFalse() {
        Ship ship = new Ship(ShipType.SUBMARINE, "SUB-A", List.of(
                new Coordinate(1, 1),
                new Coordinate(1, 2),
                new Coordinate(1, 3)));
        assertFalse(ship.hit(new Coordinate(5, 5)));
    }

    @Test
    void testDoubleHitReturnsFalse() {
        Ship ship = new Ship(ShipType.PATROL_BOAT, "PB-A", List.of(
                new Coordinate(0, 0),
                new Coordinate(0, 1)));
        assertTrue(ship.hit(new Coordinate(0, 0)));
        assertFalse(ship.hit(new Coordinate(0, 0)));
        assertEquals(1, ship.remainingHealth());
    }

    @Test
    void testOccupies() {
        Ship ship = new Ship(ShipType.DESTROYER, "DS-A", List.of(
                new Coordinate(3, 3),
                new Coordinate(3, 4),
                new Coordinate(3, 5)));
        assertTrue(ship.occupies(new Coordinate(3, 4)));
        assertFalse(ship.occupies(new Coordinate(0, 0)));
    }
}
