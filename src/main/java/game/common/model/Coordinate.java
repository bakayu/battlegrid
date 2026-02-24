package game.common.model;

/**
 * An (row, col) coordinate on the game board.
 * Display format: column letter + row number, e.g. "B5" = column B (1), row 5
 * (4).
 */
public class Coordinate {

    private final int row;
    private final int col;

    public Coordinate(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    /**
     * Record-style accessors for compatibility.
     */
    public int row() {
        return row;
    }

    public int col() {
        return col;
    }

    /**
     * Checks if this coordinate is within bounds of a square grid.
     *
     * @param gridSize the size of the grid (e.g. 8 for 8x8)
     * @return true if row and col are both in [0, gridSize)
     */
    public boolean isWithinBounds(int gridSize) {
        return row >= 0 && row < gridSize && col >= 0 && col < gridSize;
    }

    /**
     * Parses user input like "B5" into a Coordinate.
     * First character = column letter (A=0, B=1, ...).
     * Remaining characters = row number (1-indexed, so "5" → row index 4).
     *
     * @throws IllegalArgumentException if the format is invalid
     */
    public static Coordinate fromInput(String input) {
        if (input == null || input.length() < 2 || input.length() > 3) {
            throw new IllegalArgumentException("Invalid coordinate: " + input);
        }

        input = input.toUpperCase().trim();
        char colChar = input.charAt(0);
        if (colChar < 'A' || colChar > 'P') {
            throw new IllegalArgumentException("Invalid column letter: " + colChar);
        }

        int col = colChar - 'A';

        int rowNum;
        try {
            rowNum = Integer.parseInt(input.substring(1));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid row number: " + input.substring(1));
        }

        if (rowNum < 1 || rowNum > 16) {
            throw new IllegalArgumentException("Row number out of range: " + rowNum);
        }

        int row = rowNum - 1;
        return new Coordinate(row, col);
    }

    /**
     * Returns the display string, e.g. "B5" for column B (index 1), row 5 (index
     * 4).
     */
    public String toDisplayString() {
        char colChar = (char) ('A' + col);
        return "" + colChar + (row + 1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Coordinate that = (Coordinate) o;
        return row == that.row && col == that.col;
    }

    @Override
    public int hashCode() {
        return 31 * row + col;
    }

    @Override
    public String toString() {
        return toDisplayString();
    }
}
