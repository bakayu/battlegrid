package game.common.model;

import java.util.Objects;

/**
 * Represents a position on the game grid.
 * Row and column are 0-indexed internally.
 */
public record Coordinate(int row, int col) {

    /**
     * Parses user input like "E3" or "L12" into a Coordinate.
     * Column letter is converted to 0-indexed col, row number to 0-indexed row.
     */
    public static Coordinate fromInput(String input) {
        if (input == null || input.length() < 2) {
            throw new IllegalArgumentException("Invalid coordinate: " + input);
        }
        input = input.trim().toUpperCase();

        char colChar = input.charAt(0);
        if (colChar < 'A' || colChar > 'P') {
            throw new IllegalArgumentException("Column must be A-P, got: " + colChar);
        }

        int col = colChar - 'A';
        int row;
        try {
            row = Integer.parseInt(input.substring(1)) - 1; // convert to 0-indexed
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid row number in: " + input);
        }

        return new Coordinate(row, col);
    }

    /**
     * Converts back to display format, e.g., (0,4) → "E1"
     */
    public String toDisplayString() {
        return "" + (char) ('A' + col) + (row + 1);
    }

    /**
     * Returns true if this coordinate is within a grid of the given size.
     */
    public boolean isWithinBounds(int gridSize) {
        return row >= 0 && row < gridSize && col >= 0 && col < gridSize;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Coordinate c))
            return false;
        return row == c.row && col == c.col;
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, col);
    }
}
