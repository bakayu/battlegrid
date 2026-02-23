package game.server.game;

import game.common.model.Board;
import game.common.model.CellState;
import game.common.model.GameMode;
import game.common.model.Ship;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ShipPlacerTest {

    @Test
    void testPlaceShips_blitz() {
        ShipPlacer placer = new ShipPlacer(42L);
        Board board = placer.placeShips(GameMode.BLITZ);

        assertEquals(3, board.getShips().size());
        assertEquals(8, board.getSize());

        // Count total ship tiles
        int shipTiles = 0;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if (board.getCell(r, c) == CellState.SHIP) {
                    shipTiles++;
                }
            }
        }
        // Patrol(2) + Sub(3) + Destroyer(3) = 8 tiles
        assertEquals(8, shipTiles);
    }

    @Test
    void testPlaceShips_strike() {
        ShipPlacer placer = new ShipPlacer(123L);
        Board board = placer.placeShips(GameMode.STRIKE);

        assertEquals(5, board.getShips().size());
        assertEquals(12, board.getSize());

        // Patrol(2) + Sub(3) + Destroyer(3) + Battleship(4) + Carrier(5*2=10) = 22
        // tiles
        int shipTiles = 0;
        for (int r = 0; r < 12; r++) {
            for (int c = 0; c < 12; c++) {
                if (board.getCell(r, c) == CellState.SHIP) {
                    shipTiles++;
                }
            }
        }
        assertEquals(22, shipTiles);
    }

    @Test
    void testPlaceShips_war() {
        ShipPlacer placer = new ShipPlacer(999L);
        Board board = placer.placeShips(GameMode.WAR);

        assertEquals(7, board.getShips().size());
        assertEquals(16, board.getSize());

        // 2*PB(2) + Sub(3) + 2*DS(3) + BS(4) + Carrier(10) = 4+3+6+4+10 = 27 tiles
        int shipTiles = 0;
        for (int r = 0; r < 16; r++) {
            for (int c = 0; c < 16; c++) {
                if (board.getCell(r, c) == CellState.SHIP) {
                    shipTiles++;
                }
            }
        }
        assertEquals(27, shipTiles);
    }

    @Test
    void testPlaceShips_noOverlap() {
        ShipPlacer placer = new ShipPlacer(77L);
        Board board = placer.placeShips(GameMode.WAR);

        // Verify no two ships share a coordinate
        for (int i = 0; i < board.getShips().size(); i++) {
            for (int j = i + 1; j < board.getShips().size(); j++) {
                Ship a = board.getShips().get(i);
                Ship b = board.getShips().get(j);
                for (var cellA : a.getOccupiedCells()) {
                    for (var cellB : b.getOccupiedCells()) {
                        assertNotEquals(cellA, cellB,
                                "Ships " + a.getId() + " and " + b.getId() + " overlap at " + cellA);
                    }
                }
            }
        }
    }

    @Test
    void testPlaceShips_gapEnforced() {
        ShipPlacer placer = new ShipPlacer(77L);
        Board board = placer.placeShips(GameMode.STRIKE);

        // For each ship, verify no adjacent cell belongs to another ship
        for (Ship ship : board.getShips()) {
            for (var cell : ship.getOccupiedCells()) {
                for (int dr = -1; dr <= 1; dr++) {
                    for (int dc = -1; dc <= 1; dc++) {
                        if (dr == 0 && dc == 0)
                            continue;
                        int nr = cell.row() + dr;
                        int nc = cell.col() + dc;
                        if (nr >= 0 && nr < board.getSize() && nc >= 0 && nc < board.getSize()) {
                            var neighbor = new game.common.model.Coordinate(nr, nc);
                            var neighborShip = board.getShipAt(neighbor);
                            if (neighborShip.isPresent() && neighborShip.get() != ship) {
                                fail("Ship " + ship.getId() + " at " + cell
                                        + " is adjacent to " + neighborShip.get().getId()
                                        + " at " + neighbor);
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    void testPlaceShips_shipIds() {
        ShipPlacer placer = new ShipPlacer(42L);
        Board board = placer.placeShips(GameMode.WAR);

        // War has 2 PB and 2 DS — check they have distinct IDs
        long distinctIds = board.getShips().stream()
                .map(Ship::getId)
                .distinct()
                .count();
        assertEquals(board.getShips().size(), distinctIds);
    }

    @Test
    void testPlaceShips_deterministic() {
        Board board1 = new ShipPlacer(42L).placeShips(GameMode.BLITZ);
        Board board2 = new ShipPlacer(42L).placeShips(GameMode.BLITZ);

        // Same seed should produce same placement
        assertEquals(board1.getShips().size(), board2.getShips().size());
        for (int i = 0; i < board1.getShips().size(); i++) {
            assertEquals(board1.getShips().get(i).getOccupiedCells(),
                    board2.getShips().get(i).getOccupiedCells());
        }
    }

    @Test
    void testPlaceShips_multipleSeeds() {
        // Run with many different seeds to ensure robustness
        for (long seed = 0; seed < 50; seed++) {
            ShipPlacer placer = new ShipPlacer(seed);
            assertDoesNotThrow(() -> placer.placeShips(GameMode.WAR),
                    "Failed with seed " + seed);
        }
    }
}
