package game.server.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import game.common.model.Board;
import game.common.model.Coordinate;
import game.common.model.Direction;
import game.common.model.GameMode;
import game.common.model.Ship;
import game.common.model.ShipType;

/**
 * Handles randomized ship placement on a board.
 * Ships are placed one at a time in a random order, with random positions
 * and orientations. A 1-tile gap is enforced between all ships.
 *
 * Uses retry-based approach: if a placement attempt fails after MAX_RETRIES
 * for any single ship, the entire board is cleared and placement restarts.
 */
public class ShipPlacer {

    private static final int MAX_RETRIES_PER_SHIP = 100;
    private static final int MAX_FULL_RETRIES = 50;

    private final Random random;

    public ShipPlacer() {
        this.random = new Random();
    }

    public ShipPlacer(long seed) {
        this.random = new Random(seed);
    }

    /**
     * Places all ships for the given game mode onto a new board.
     *
     * @param mode the game mode defining grid size and fleet composition
     * @return a Board with all ships placed randomly
     * @throws IllegalStateException if placement fails after all retries
     */
    public Board placeShips(GameMode mode) {
        for (int fullRetry = 0; fullRetry < MAX_FULL_RETRIES; fullRetry++) {
            Board board = new Board(mode.getGridSize());
            List<ShipType> fleet = new ArrayList<>(mode.getFleet());

            // Shuffle to randomize placement order — larger ships placed first
            // tend to succeed more often, but shuffling adds variety
            Collections.shuffle(fleet, random);

            // Sort descending by tile count so bigger ships go first (better success rate)
            fleet.sort((a, b) -> Integer.compare(b.getTileCount(), a.getTileCount()));

            boolean allPlaced = true;
            // Track ship instance counters per type for ID generation
            int[] typeCounts = new int[ShipType.values().length];

            for (ShipType type : fleet) {
                typeCounts[type.ordinal()]++;
                String shipId = generateShipId(type, typeCounts[type.ordinal()]);

                boolean placed = false;
                for (int retry = 0; retry < MAX_RETRIES_PER_SHIP; retry++) {
                    List<Coordinate> cells = generateRandomCells(type, board.getSize());
                    if (cells != null && board.canPlaceShip(cells)) {
                        Ship ship = new Ship(type, shipId, cells);
                        board.placeShip(ship);
                        placed = true;
                        break;
                    }
                }

                if (!placed) {
                    allPlaced = false;
                    break;
                }
            }

            if (allPlaced) {
                return board;
            }
        }

        throw new IllegalStateException(
                "Failed to place all ships after " + MAX_FULL_RETRIES + " full retries.");
    }

    /**
     * Generates a list of coordinates for a ship placed at a random position
     * and orientation on a grid of the given size.
     *
     * @return list of coordinates, or null if the random position is out of bounds
     */
    private List<Coordinate> generateRandomCells(ShipType type, int gridSize) {
        Direction dir = random.nextBoolean() ? Direction.HORIZONTAL : Direction.VERTICAL;

        int length = type.getLength();
        int width = type.getWidth();

        // For HORIZONTAL: ship extends right (length) and down (width)
        // For VERTICAL: ship extends down (length) and right (width)
        int maxRow, maxCol;
        if (dir == Direction.HORIZONTAL) {
            maxCol = gridSize - length;
            maxRow = gridSize - width;
        } else {
            maxRow = gridSize - length;
            maxCol = gridSize - width;
        }

        if (maxRow < 0 || maxCol < 0) {
            return null;
        }

        int startRow = random.nextInt(maxRow + 1);
        int startCol = random.nextInt(maxCol + 1);

        List<Coordinate> cells = new ArrayList<>();

        if (dir == Direction.HORIZONTAL) {
            for (int l = 0; l < length; l++) {
                for (int w = 0; w < width; w++) {
                    cells.add(new Coordinate(startRow + w, startCol + l));
                }
            }
        } else {
            for (int l = 0; l < length; l++) {
                for (int w = 0; w < width; w++) {
                    cells.add(new Coordinate(startRow + l, startCol + w));
                }
            }
        }

        return cells;
    }

    /**
     * Generates a ship ID like "PB-A", "DS-B", etc.
     */
    private String generateShipId(ShipType type, int instanceNumber) {
        char suffix = (char) ('A' + instanceNumber - 1);
        return type.getCode() + "-" + suffix;
    }
}
