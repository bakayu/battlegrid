package game.common.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a placed ship on the board.
 * Tracks which cells it occupies and which have been hit.
 */
public class Ship {

    private final ShipType type;
    private final String id; // e.g., "PB-A", "DS-B" for multiple ships of same type
    private final List<Coordinate> occupiedCells;
    private final Set<Coordinate> hitCells;

    public Ship(ShipType type, String id, List<Coordinate> occupiedCells) {
        this.type = type;
        this.id = id;
        this.occupiedCells = new ArrayList<>(occupiedCells);
        this.hitCells = new HashSet<>();
    }

    public ShipType getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public List<Coordinate> getOccupiedCells() {
        return occupiedCells;
    }

    public Set<Coordinate> getHitCells() {
        return hitCells;
    }

    /**
     * Records a hit on this ship at the given coordinate.
     * 
     * @return true if the coordinate is part of this ship and wasn't already hit
     */
    public boolean hit(Coordinate coord) {
        if (occupiedCells.contains(coord) && !hitCells.contains(coord)) {
            hitCells.add(coord);
            return true;
        }
        return false;
    }

    /**
     * Returns true if every cell of this ship has been hit.
     */
    public boolean isSunk() {
        return hitCells.size() == occupiedCells.size();
    }

    /**
     * Returns true if this ship occupies the given coordinate.
     */
    public boolean occupies(Coordinate coord) {
        return occupiedCells.contains(coord);
    }

    /**
     * Returns the number of remaining (un-hit) cells.
     */
    public int remainingHealth() {
        return occupiedCells.size() - hitCells.size();
    }

    @Override
    public String toString() {
        return id + " [" + type.getDisplayName() + "] " +
                (isSunk() ? "SUNK" : remainingHealth() + "/" + occupiedCells.size());
    }
}
