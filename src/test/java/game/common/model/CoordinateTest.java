package game.common.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CoordinateTest {

    @Test
    void testFromInput_simple() {
        Coordinate c = Coordinate.fromInput("E3");
        assertEquals(2, c.row()); // row 3 → 0-indexed = 2
        assertEquals(4, c.col()); // E → 0-indexed = 4
    }

    @Test
    void testFromInput_doubleDigitRow() {
        Coordinate c = Coordinate.fromInput("L12");
        assertEquals(11, c.row());
        assertEquals(11, c.col());
    }

    @Test
    void testFromInput_lowercase() {
        Coordinate c = Coordinate.fromInput("a1");
        assertEquals(0, c.row());
        assertEquals(0, c.col());
    }

    @Test
    void testFromInput_invalid() {
        assertThrows(IllegalArgumentException.class, () -> Coordinate.fromInput(""));
        assertThrows(IllegalArgumentException.class, () -> Coordinate.fromInput("Z"));
        assertThrows(IllegalArgumentException.class, () -> Coordinate.fromInput("Abc"));
    }

    @Test
    void testToDisplayString() {
        assertEquals("A1", new Coordinate(0, 0).toDisplayString());
        assertEquals("E3", new Coordinate(2, 4).toDisplayString());
        assertEquals("L12", new Coordinate(11, 11).toDisplayString());
    }

    @Test
    void testRoundTrip() {
        String input = "H8";
        Coordinate c = Coordinate.fromInput(input);
        assertEquals(input, c.toDisplayString());
    }

    @Test
    void testIsWithinBounds() {
        assertTrue(new Coordinate(0, 0).isWithinBounds(8));
        assertTrue(new Coordinate(7, 7).isWithinBounds(8));
        assertFalse(new Coordinate(8, 0).isWithinBounds(8));
        assertFalse(new Coordinate(-1, 0).isWithinBounds(8));
        assertFalse(new Coordinate(0, 8).isWithinBounds(8));
    }

    @Test
    void testEquality() {
        assertEquals(new Coordinate(3, 5), new Coordinate(3, 5));
        assertNotEquals(new Coordinate(3, 5), new Coordinate(5, 3));
    }
}
