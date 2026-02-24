package game.client.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BoardRendererTest {

    @Test
    void testRenderBoard_empty() {
        JsonObject board = createBoard(4, "EMPTY");
        String rendered = BoardRenderer.renderBoard("Test Board", board, true);

        assertNotNull(rendered);
        assertTrue(rendered.contains("Test Board"));
        // Column headers should be letters A-D
        assertTrue(rendered.contains("A"));
        assertTrue(rendered.contains("D"));
        // Row labels should be numbers 1-4
        assertTrue(rendered.contains("1"));
        assertTrue(rendered.contains("4"));
    }

    @Test
    void testRenderBoard_axisLabeling() {
        JsonObject board = createBoard(8, "EMPTY");
        String rendered = BoardRenderer.renderBoard("Board", board, true);
        String stripped = BoardRenderer.stripAnsi(rendered);

        // First content line after title should have column letters
        String[] lines = stripped.split("\n");
        // Find the header line (contains A B C ...)
        boolean foundColumnHeaders = false;
        for (String line : lines) {
            if (line.contains("A") && line.contains("B") && line.contains("H")) {
                foundColumnHeaders = true;
                break;
            }
        }
        assertTrue(foundColumnHeaders, "Column headers (A-H) should be at the top");

        // Row labels should be numbers on the left side
        boolean foundRow1 = false;
        boolean foundRow8 = false;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("1") || trimmed.startsWith("1 "))
                foundRow1 = true;
            if (trimmed.startsWith("8") || trimmed.startsWith("8 "))
                foundRow8 = true;
        }
        assertTrue(foundRow1, "Row 1 should be labeled on the left");
        assertTrue(foundRow8, "Row 8 should be labeled on the left");
    }

    @Test
    void testRenderBoard_withShips() {
        JsonObject board = createBoard(4, "EMPTY");
        JsonArray grid = board.getAsJsonArray("grid");
        grid.get(0).getAsJsonArray().set(0, new JsonPrimitive("SHIP"));

        String rendered = BoardRenderer.renderBoard("Own Board", board, true);
        assertTrue(rendered.contains("■"));
    }

    @Test
    void testRenderBoard_fogOfWar() {
        JsonObject board = createBoard(4, "EMPTY");
        JsonArray grid = board.getAsJsonArray("grid");
        grid.get(0).getAsJsonArray().set(0, new JsonPrimitive("SHIP"));

        String rendered = BoardRenderer.renderBoard("Enemy Board", board, false);
        assertFalse(rendered.contains("■"));
    }

    @Test
    void testRenderBoard_withHitsAndMisses() {
        JsonObject board = createBoard(4, "EMPTY");
        JsonArray grid = board.getAsJsonArray("grid");
        grid.get(0).getAsJsonArray().set(0, new JsonPrimitive("HIT"));
        grid.get(1).getAsJsonArray().set(1, new JsonPrimitive("MISS"));
        grid.get(2).getAsJsonArray().set(2, new JsonPrimitive("SUNK"));

        String rendered = BoardRenderer.renderBoard("Board", board, true);
        assertTrue(rendered.contains("✖"));
        assertTrue(rendered.contains("○"));
    }

    @Test
    void testRenderSideBySide() {
        JsonObject left = createBoard(4, "EMPTY");
        JsonObject right = createBoard(4, "EMPTY");

        String rendered = BoardRenderer.renderSideBySide(
                "Your Fleet", left, true,
                "Enemy Waters", right, false);

        assertNotNull(rendered);
        assertTrue(rendered.contains("Your Fleet"));
        assertTrue(rendered.contains("Enemy Waters"));
    }

    @Test
    void testStripAnsi() {
        String colored = "\033[1m\033[31mHello\033[0m";
        assertEquals("Hello", BoardRenderer.stripAnsi(colored));
    }

    @Test
    void testRenderBoard_largeGrid() {
        JsonObject board = createBoard(16, "EMPTY");
        String rendered = BoardRenderer.renderBoard("War Board", board, true);

        assertNotNull(rendered);
        assertTrue(rendered.contains("P")); // Column P = 16th column
        assertTrue(rendered.contains("16")); // Row 16
    }

    private JsonObject createBoard(int size, String defaultState) {
        JsonObject board = new JsonObject();
        board.addProperty("size", size);
        JsonArray grid = new JsonArray();
        for (int r = 0; r < size; r++) {
            JsonArray row = new JsonArray();
            for (int c = 0; c < size; c++) {
                row.add(defaultState);
            }
            grid.add(row);
        }
        board.add("grid", grid);
        return board;
    }
}
