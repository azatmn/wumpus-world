package wumpus.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PositionTest {

    @Test
    void stepMovesByDirection() {
        Position p = new Position(3, 2);
        assertEquals(new Position(2, 2), p.step(Direction.UP));
        assertEquals(new Position(3, 3), p.step(Direction.RIGHT));
        assertEquals(new Position(4, 2), p.step(Direction.DOWN));
        assertEquals(new Position(3, 1), p.step(Direction.LEFT));
    }

    @Test
    void insideChecksAllFourBorders() {
        assertTrue(new Position(1, 1).inside(4));
        assertTrue(new Position(4, 4).inside(4));
        assertFalse(new Position(0, 1).inside(4));
        assertFalse(new Position(1, 0).inside(4));
        assertFalse(new Position(5, 1).inside(4));
        assertFalse(new Position(1, 5).inside(4));
    }

    @Test
    void neighborsInCornerAndCenter() {
        assertEquals(List.of(new Position(3, 1), new Position(4, 2)), new Position(4, 1).neighbors(4));
        assertEquals(List.of(new Position(1, 2), new Position(2, 3), new Position(3, 2), new Position(2, 1)),
                new Position(2, 2).neighbors(4));
    }

    @Test
    void isNeighborOnlyForSideCells() {
        Position p = new Position(2, 2);
        assertTrue(p.isNeighbor(new Position(1, 2)));
        assertTrue(p.isNeighbor(new Position(2, 1)));
        assertFalse(p.isNeighbor(p));
        assertFalse(p.isNeighbor(new Position(1, 1)));
        assertFalse(p.isNeighbor(new Position(2, 4)));
    }

    @Test
    void printedLikeInTask() {
        assertEquals("3,1", new Position(3, 1).toString());
    }
}
