package game.common.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CoordinateTest {

    @Test
    void testFromInput_basic() {
        // B5 = column B (index 1), row 5 (index 4)
        Coordinate c = Coordinate.fromInput("B5");
        assertEquals(4, c.getRow());
        assertEquals(1, c.getCol());
    }

    @Test
    void testFromInput_topLeft() {
        Coordinate c = Coordinate.fromInput("A1");
        assertEquals(0, c.getRow());
        assertEquals(0, c.getCol());
    }

    @Test
    void testFromInput_lowercase() {
        Coordinate c = Coordinate.fromInput("c3");
        assertEquals(2, c.getRow());
        assertEquals(2, c.getCol());
    }

    @Test
    void testFromInput_doubleDigitRow() {
        Coordinate c = Coordinate.fromInput("D12");
        assertEquals(11, c.getRow());
        assertEquals(3, c.getCol());
    }

    @Test
    void testFromInput_invalidLetter() {
        assertThrows(IllegalArgumentException.class, () -> Coordinate.fromInput("Z5"));
    }

    @Test
    void testFromInput_invalidRow() {
        assertThrows(IllegalArgumentException.class, () -> Coordinate.fromInput("A0"));
        assertThrows(IllegalArgumentException.class, () -> Coordinate.fromInput("A17"));
    }

    @Test
    void testFromInput_nullOrShort() {
        assertThrows(IllegalArgumentException.class, () -> Coordinate.fromInput(null));
        assertThrows(IllegalArgumentException.class, () -> Coordinate.fromInput("A"));
    }

    @Test
    void testToDisplayString() {
        Coordinate c = new Coordinate(4, 1);
        assertEquals("B5", c.toDisplayString());
    }

    @Test
    void testToDisplayString_topLeft() {
        Coordinate c = new Coordinate(0, 0);
        assertEquals("A1", c.toDisplayString());
    }

    @Test
    void testRoundTrip() {
        String input = "E7";
        Coordinate c = Coordinate.fromInput(input);
        assertEquals(input, c.toDisplayString());
    }

    @Test
    void testEquals() {
        Coordinate a = new Coordinate(3, 5);
        Coordinate b = new Coordinate(3, 5);
        Coordinate c = new Coordinate(3, 6);

        assertEquals(a, b);
        assertNotEquals(a, c);
    }

    @Test
    void testHashCode() {
        Coordinate a = new Coordinate(3, 5);
        Coordinate b = new Coordinate(3, 5);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
