package game.client.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Renders game boards as ASCII art for the terminal.
 * 
 * Layout:
 * Columns = letters (A, B, C, ...) across the top
 * Rows = numbers (1, 2, 3, ...) down the left side
 */
public class BoardRenderer {

    // ANSI color codes
    private static final String RESET = "\033[0m";
    private static final String BOLD = "\033[1m";
    private static final String DIM = "\033[2m";

    private static final String RED = "\033[31m";
    private static final String GREEN = "\033[32m";
    private static final String BLUE = "\033[34m";
    private static final String CYAN = "\033[36m";
    private static final String WHITE = "\033[37m";

    private static final String BG_RED = "\033[41m";

    /**
     * Renders a single board.
     *
     * @param title title above the board
     * @param board the board JSON object with "size" and "grid"
     * @param isOwn true = show ships; false = tracking view
     */
    public static String renderBoard(String title, JsonObject board, boolean isOwn) {
        int size = board.get("size").getAsInt();
        JsonArray grid = board.getAsJsonArray("grid");

        StringBuilder sb = new StringBuilder();

        // Title centered above the board
        int boardWidth = size * 4 + 4;
        int titlePad = Math.max(0, (boardWidth - title.length()) / 2);
        sb.append(BOLD).append(CYAN)
                .append(" ".repeat(titlePad)).append(title)
                .append(RESET).append("\n");

        // Column headers (letters: A, B, C, ...)
        sb.append("      ");
        for (int c = 0; c < size; c++) {
            char colLabel = (char) ('A' + c);
            sb.append(BOLD).append(WHITE).append(colLabel).append("   ").append(RESET);
        }
        sb.append("\n");

        // Top border
        sb.append("    ┌");
        for (int c = 0; c < size; c++) {
            sb.append("───");
            if (c < size - 1)
                sb.append("┬");
        }
        sb.append("┐\n");

        // Rows
        for (int r = 0; r < size; r++) {
            // Row label (numbers: 1, 2, 3, ...)
            sb.append(BOLD).append(WHITE)
                    .append(String.format(" %2d ", r + 1))
                    .append(RESET);
            sb.append("│");

            JsonArray row = grid.get(r).getAsJsonArray();
            for (int c = 0; c < size; c++) {
                String state = row.get(c).getAsString();
                sb.append(renderCell(state, isOwn));
                if (c < size - 1)
                    sb.append("│");
            }
            sb.append("│\n");

            // Row separator
            if (r < size - 1) {
                sb.append("    ├");
                for (int c = 0; c < size; c++) {
                    sb.append("───");
                    if (c < size - 1)
                        sb.append("┼");
                }
                sb.append("┤\n");
            }
        }

        // Bottom border
        sb.append("    └");
        for (int c = 0; c < size; c++) {
            sb.append("───");
            if (c < size - 1)
                sb.append("┴");
        }
        sb.append("┘\n");

        return sb.toString();
    }

    /**
     * Renders two boards side by side.
     */
    public static String renderSideBySide(String leftTitle, JsonObject leftBoard, boolean leftIsOwn,
            String rightTitle, JsonObject rightBoard, boolean rightIsOwn) {
        String leftStr = renderBoard(leftTitle, leftBoard, leftIsOwn);
        String rightStr = renderBoard(rightTitle, rightBoard, rightIsOwn);

        String[] leftLines = leftStr.split("\n");
        String[] rightLines = rightStr.split("\n");

        int maxLeft = 0;
        for (String line : leftLines) {
            int visible = stripAnsi(line).length();
            if (visible > maxLeft)
                maxLeft = visible;
        }

        StringBuilder sb = new StringBuilder();
        int maxLines = Math.max(leftLines.length, rightLines.length);

        for (int i = 0; i < maxLines; i++) {
            String left = i < leftLines.length ? leftLines[i] : "";
            String right = i < rightLines.length ? rightLines[i] : "";

            sb.append(left);

            // Pad left side to align right board
            int visibleLen = stripAnsi(left).length();
            int padding = maxLeft - visibleLen + 6;
            sb.append(" ".repeat(Math.max(1, padding)));

            sb.append(right);
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Renders a cell based on its state.
     */
    private static String renderCell(String state, boolean isOwn) {
        return switch (state) {
            case "EMPTY" -> DIM + " · " + RESET;
            case "SHIP" -> isOwn ? (GREEN + " ■ " + RESET) : (DIM + " · " + RESET);
            case "HIT" -> RED + BOLD + " ✖ " + RESET;
            case "MISS" -> BLUE + " ○ " + RESET;
            case "SUNK" -> BG_RED + WHITE + BOLD + " ✖ " + RESET;
            default -> " ? ";
        };
    }

    /**
     * Strips ANSI escape codes for length calculation.
     */
    public static String stripAnsi(String str) {
        return str.replaceAll("\033\\[[0-9;]*m", "");
    }
}
