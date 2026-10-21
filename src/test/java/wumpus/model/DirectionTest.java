package wumpus.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DirectionTest {

    @Test
    void turnsGoClockwiseAndBack() {
        assertEquals(Direction.RIGHT, Direction.UP.right());
        assertEquals(Direction.DOWN, Direction.RIGHT.right());
        assertEquals(Direction.LEFT, Direction.DOWN.right());
        assertEquals(Direction.UP, Direction.LEFT.right());
        for (Direction d : Direction.values()) {
            assertEquals(d, d.right().left());
            assertEquals(d, d.left().left().left().left());
        }
    }

    @Test
    void namesForJournal() {
        assertEquals("вверх", Direction.UP.title());
        assertEquals("вправо", Direction.RIGHT.title());
        assertEquals("вниз", Direction.DOWN.title());
        assertEquals("влево", Direction.LEFT.title());
    }
}
