package wumpus.agent;

import org.junit.jupiter.api.Test;
import wumpus.model.Direction;
import wumpus.model.Position;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static wumpus.model.Action.*;

class PlannerTest {

    private static Position p(int row, int col) {
        return new Position(row, col);
    }

    @Test
    void pathGoesOnlyThroughAllowedRooms() {
        Set<Position> allowed = Set.of(p(4, 1), p(4, 2), p(4, 3), p(3, 3));
        assertEquals(List.of(p(4, 2), p(4, 3), p(3, 3), p(2, 3)), Planner.path(p(4, 1), p(2, 3), allowed, 4));
    }

    @Test
    void targetItselfNeedNotBeAllowed() {
        assertEquals(List.of(p(3, 1)), Planner.path(p(4, 1), p(3, 1), Set.of(p(4, 1)), 4));
    }

    @Test
    void noPathAndSameRoom() {
        assertNull(Planner.path(p(4, 1), p(1, 1), Set.of(p(4, 1)), 4));
        assertEquals(List.of(), Planner.path(p(4, 1), p(4, 1), Set.of(), 4));
    }

    @Test
    void pathIsShortest() {
        Set<Position> all = Set.of(p(1, 1), p(1, 2), p(2, 1), p(2, 2));
        assertEquals(2, Planner.path(p(2, 1), p(1, 2), all, 2).size());
    }

    @Test
    void faceWithFewestTurns() {
        assertEquals(List.of(), Planner.face(Direction.UP, Direction.UP));
        assertEquals(List.of(TURN_RIGHT), Planner.face(Direction.UP, Direction.RIGHT));
        assertEquals(List.of(TURN_LEFT), Planner.face(Direction.UP, Direction.LEFT));
        assertEquals(List.of(TURN_RIGHT, TURN_RIGHT), Planner.face(Direction.UP, Direction.DOWN));
        assertEquals(List.of(TURN_LEFT), Planner.face(Direction.RIGHT, Direction.UP));
    }

    @Test
    void walkTurnsBeforeEachStep() {
        assertEquals(List.of(FORWARD, TURN_LEFT, FORWARD),
                Planner.walk(p(4, 1), Direction.RIGHT, List.of(p(4, 2), p(3, 2))));
        assertEquals(List.of(TURN_RIGHT, TURN_RIGHT, FORWARD, TURN_RIGHT, FORWARD),
                Planner.walk(p(4, 2), Direction.RIGHT, List.of(p(4, 1), p(3, 1))), "путь из шага 2 задания");
    }

    @Test
    void facingAfterWalk() {
        assertEquals(Direction.UP, Planner.facingAfter(p(4, 2), Direction.RIGHT, List.of(p(4, 1), p(3, 1))));
        assertEquals(Direction.LEFT, Planner.facingAfter(p(4, 2), Direction.RIGHT, List.of(p(4, 1))));
        assertEquals(Direction.DOWN, Planner.facingAfter(p(4, 2), Direction.DOWN, List.of()));
    }

    @Test
    void cheapestCountsTurnsToo() {
        Set<Position> here = Set.of(p(4, 1));
        List<Position> both = List.of(p(3, 1), p(4, 2));
        assertEquals(List.of(p(4, 2)), Planner.cheapest(p(4, 1), Direction.RIGHT, both, here, 4));
        assertEquals(List.of(p(3, 1)), Planner.cheapest(p(4, 1), Direction.UP, both, here, 4));
        assertNull(Planner.cheapest(p(4, 1), Direction.UP, List.of(p(1, 1)), here, 4));
        assertNull(Planner.cheapest(p(4, 1), Direction.UP, List.of(), here, 4));
        assertEquals(List.of(p(2, 3)), Planner.cheapest(p(2, 2), Direction.UP, List.of(p(2, 3), p(2, 1)), Set.of(p(2, 2)), 4),
                "при равной цене — первая в списке");
        assertEquals(List.of(p(2, 1)), Planner.cheapest(p(2, 2), Direction.UP, List.of(p(2, 1), p(2, 3)), Set.of(p(2, 2)), 4));
    }

    @Test
    void directionOnlyForNeighbors() {
        assertEquals(Direction.DOWN, Planner.direction(p(3, 1), p(4, 1)));
        assertThrows(IllegalArgumentException.class, () -> Planner.direction(p(1, 1), p(3, 1)));
    }
}
