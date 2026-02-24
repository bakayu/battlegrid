package game.common.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Represents one player's game board.
 * Contains the grid state and all placed ships.
 */
public class Board {

    private final int size;
    private final CellState[][] grid;
    private final List<Ship> ships;

    public Board(int size) {
        this.size = size;
        this.grid = new CellState[size][size];
        this.ships = new ArrayList<>();

        // Initialize all cells to EMPTY
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                grid[r][c] = CellState.EMPTY;
            }
        }
    }

    public int getSize() {
        return size;
    }

    public CellState getCell(int row, int col) {
        return grid[row][col];
    }

    public CellState getCell(Coordinate coord) {
        return grid[coord.row()][coord.col()];
    }

    public void setCell(Coordinate coord, CellState state) {
        grid[coord.row()][coord.col()] = state;
    }

    public List<Ship> getShips() {
        return ships;
    }

    /**
     * Places a ship on the board. Marks all occupied cells as SHIP.
     */
    public void placeShip(Ship ship) {
        ships.add(ship);
        for (Coordinate cell : ship.getOccupiedCells()) {
            grid[cell.row()][cell.col()] = CellState.SHIP;
        }
    }

    /**
     * Returns the ship at the given coordinate, if any.
     */
    public Optional<Ship> getShipAt(Coordinate coord) {
        return ships.stream()
                .filter(s -> s.occupies(coord))
                .findFirst();
    }

    /**
     * Applies an attack at the given coordinate.
     * 
     * @return the resulting CellState (HIT, MISS, or SUNK)
     */
    public CellState attack(Coordinate coord) {
        CellState current = getCell(coord);

        switch (current) {
            case EMPTY -> {
                setCell(coord, CellState.MISS);
                return CellState.MISS;
            }
            case SHIP -> {
                setCell(coord, CellState.HIT);
                Optional<Ship> ship = getShipAt(coord);
                if (ship.isPresent()) {
                    ship.get().hit(coord);
                    if (ship.get().isSunk()) {
                        // Mark all cells of the sunk ship
                        for (Coordinate c : ship.get().getOccupiedCells()) {
                            setCell(c, CellState.SUNK);
                        }
                        return CellState.SUNK;
                    }
                }
                return CellState.HIT;
            }
            // Already attacked cells — return current state (no double-attack)
            case HIT, MISS, SUNK -> {
                return current;
            }
            default -> {
                return current;
            }
        }
    }

    /**
     * Returns true if all ships on this board have been sunk.
     */
    public boolean allShipsSunk() {
        return ships.stream().allMatch(Ship::isSunk);
    }

    /**
     * Checks if a ship can be placed at the given cells.
     * Validates bounds, no overlap, and 1-tile gap from existing ships.
     */
    public boolean canPlaceShip(List<Coordinate> cells) {
        for (Coordinate cell : cells) {
            // Check bounds
            if (!cell.isWithinBounds(size)) {
                return false;
            }
            // Check overlap
            if (grid[cell.row()][cell.col()] != CellState.EMPTY) {
                return false;
            }
            // Check 1-tile gap from existing ships
            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    int nr = cell.row() + dr;
                    int nc = cell.col() + dc;
                    if (nr >= 0 && nr < size && nc >= 0 && nc < size) {
                        if (grid[nr][nc] == CellState.SHIP) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    /**
     * Counts how many ships of a given type are still alive (not sunk).
     */
    public long aliveShipsOfType(ShipType type) {
        return ships.stream()
                .filter(s -> s.getType() == type && !s.isSunk())
                .count();
    }
}
