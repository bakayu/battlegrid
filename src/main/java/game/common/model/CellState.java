package game.common.model;

/**
 * Represents the state of a single cell on the game grid.
 */
public enum CellState {
    EMPTY, // Water, not attacked
    SHIP, // Ship segment, not attacked
    MISS, // Water, attacked
    HIT, // Ship segment, attacked
    SUNK // Ship segment, ship fully destroyed
}
