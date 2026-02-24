package game.server.game;

import game.common.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CooldownManagerTest {

    private CooldownManager cooldownManager;
    private Board board;

    @BeforeEach
    void setUp() {
        cooldownManager = new CooldownManager();
        board = new Board(12);

        // Place one of each ship type for Strike mode
        board.placeShip(new Ship(ShipType.PATROL_BOAT, "PB-A", List.of(
                new Coordinate(0, 0), new Coordinate(0, 1))));
        board.placeShip(new Ship(ShipType.SUBMARINE, "SUB-A", List.of(
                new Coordinate(2, 0), new Coordinate(2, 1), new Coordinate(2, 2))));
        board.placeShip(new Ship(ShipType.DESTROYER, "DS-A", List.of(
                new Coordinate(4, 0), new Coordinate(4, 1), new Coordinate(4, 2))));
        board.placeShip(new Ship(ShipType.BATTLESHIP, "BATS-A", List.of(
                new Coordinate(6, 0), new Coordinate(6, 1), new Coordinate(6, 2), new Coordinate(6, 3))));
        board.placeShip(new Ship(ShipType.CARRIER, "CARR-A", List.of(
                new Coordinate(8, 0), new Coordinate(8, 1), new Coordinate(8, 2), new Coordinate(8, 3),
                new Coordinate(8, 4),
                new Coordinate(9, 0), new Coordinate(9, 1), new Coordinate(9, 2), new Coordinate(9, 3),
                new Coordinate(9, 4))));
    }

    @Test
    void testAllWeaponsReadyInitially() {
        for (WeaponType weapon : WeaponType.values()) {
            assertTrue(cooldownManager.isReady(weapon));
            assertEquals(0, cooldownManager.getRemainingCooldown(weapon));
        }
    }

    @Test
    void testActivateCooldown() {
        cooldownManager.activateCooldown(WeaponType.LINE_BARRAGE);

        assertFalse(cooldownManager.isReady(WeaponType.LINE_BARRAGE));
        assertEquals(2, cooldownManager.getRemainingCooldown(WeaponType.LINE_BARRAGE));

        // Other weapons unaffected
        assertTrue(cooldownManager.isReady(WeaponType.STANDARD_SHOT));
        assertTrue(cooldownManager.isReady(WeaponType.CROSS_BOMBER));
        assertTrue(cooldownManager.isReady(WeaponType.NUKE));
    }

    @Test
    void testTickCooldowns() {
        cooldownManager.activateCooldown(WeaponType.LINE_BARRAGE); // CD = 2
        cooldownManager.activateCooldown(WeaponType.NUKE); // CD = 5

        cooldownManager.tickCooldowns();
        assertEquals(1, cooldownManager.getRemainingCooldown(WeaponType.LINE_BARRAGE));
        assertEquals(4, cooldownManager.getRemainingCooldown(WeaponType.NUKE));

        cooldownManager.tickCooldowns();
        assertEquals(0, cooldownManager.getRemainingCooldown(WeaponType.LINE_BARRAGE));
        assertEquals(3, cooldownManager.getRemainingCooldown(WeaponType.NUKE));

        assertTrue(cooldownManager.isReady(WeaponType.LINE_BARRAGE));
        assertFalse(cooldownManager.isReady(WeaponType.NUKE));
    }

    @Test
    void testTickNeverGoesNegative() {
        cooldownManager.tickCooldowns();
        cooldownManager.tickCooldowns();
        cooldownManager.tickCooldowns();

        assertEquals(0, cooldownManager.getRemainingCooldown(WeaponType.STANDARD_SHOT));
    }

    @Test
    void testStandardShotAlwaysAvailable() {
        // Even with cooldown activated (it has CD=0 so it does nothing)
        cooldownManager.activateCooldown(WeaponType.STANDARD_SHOT);
        assertTrue(cooldownManager.isWeaponAvailable(WeaponType.STANDARD_SHOT, board));
    }

    @Test
    void testWeaponAvailability_shipAlive() {
        assertTrue(cooldownManager.isWeaponAvailable(WeaponType.LINE_BARRAGE, board));
        assertTrue(cooldownManager.isWeaponAvailable(WeaponType.CROSS_BOMBER, board));
        assertTrue(cooldownManager.isWeaponAvailable(WeaponType.NUKE, board));
    }

    @Test
    void testWeaponAvailability_shipSunk() {
        // Sink the destroyer
        Ship destroyer = board.getShips().stream()
                .filter(s -> s.getType() == ShipType.DESTROYER)
                .findFirst().orElseThrow();
        for (Coordinate c : destroyer.getOccupiedCells()) {
            board.attack(c);
        }

        assertFalse(cooldownManager.isWeaponAvailable(WeaponType.LINE_BARRAGE, board));
        // Other weapons still available
        assertTrue(cooldownManager.isWeaponAvailable(WeaponType.CROSS_BOMBER, board));
    }

    @Test
    void testWeaponAvailability_onCooldown() {
        cooldownManager.activateCooldown(WeaponType.CROSS_BOMBER);
        assertFalse(cooldownManager.isWeaponAvailable(WeaponType.CROSS_BOMBER, board));
    }

    @Test
    void testGetAvailableWeapons() {
        List<WeaponType> available = cooldownManager.getAvailableWeapons(board);
        assertEquals(4, available.size());
        assertTrue(available.contains(WeaponType.STANDARD_SHOT));
        assertTrue(available.contains(WeaponType.LINE_BARRAGE));
        assertTrue(available.contains(WeaponType.CROSS_BOMBER));
        assertTrue(available.contains(WeaponType.NUKE));

        // Put one on cooldown, sink another's ship
        cooldownManager.activateCooldown(WeaponType.NUKE);
        Ship battleship = board.getShips().stream()
                .filter(s -> s.getType() == ShipType.BATTLESHIP)
                .findFirst().orElseThrow();
        for (Coordinate c : battleship.getOccupiedCells()) {
            board.attack(c);
        }

        available = cooldownManager.getAvailableWeapons(board);
        assertEquals(2, available.size());
        assertTrue(available.contains(WeaponType.STANDARD_SHOT));
        assertTrue(available.contains(WeaponType.LINE_BARRAGE));
    }
}
