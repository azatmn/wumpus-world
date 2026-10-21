package wumpus.logic;

import org.junit.jupiter.api.Test;
import wumpus.model.Direction;
import wumpus.model.Position;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RulesTest {

    private static Position p(int row, int col) {
        return new Position(row, col);
    }

    private static List<String> text(List<Clause> clauses) {
        return clauses.stream().map(Clause::toString).toList();
    }

    @Test
    void printedLikeFormulas() {
        assertEquals("P(3,2)", new Fact(Kind.PIT, p(3, 2)).toString());
        assertEquals("¬W(3,2)", Literal.not(new Fact(Kind.WUMPUS, p(3, 2))).toString());
        assertEquals("G(1,1)", Literal.of(new Fact(Kind.GOLD, p(1, 1))).toString());
        assertEquals("P(3,2) ∨ P(4,3)", new Clause(Literal.of(new Fact(Kind.PIT, p(3, 2))),
                Literal.of(new Fact(Kind.PIT, p(4, 3)))).toString());
    }

    @Test
    void negateTwiceIsSame() {
        Literal l = Literal.of(new Fact(Kind.PIT, p(1, 1)));
        assertFalse(l.negate().positive());
        assertEquals(l, l.negate().negate());
    }

    @Test
    void breezeFeltMeansSomeNeighborPit() {
        assertEquals(List.of("P(3,2) ∨ P(4,3) ∨ P(4,1)"), text(Rules.breeze(p(4, 2), true, 4)));
    }

    @Test
    void noBreezeMeansNoNeighborPits() {
        assertEquals(List.of("¬P(3,1)", "¬P(4,2)"), text(Rules.breeze(p(4, 1), false, 4)));
    }

    @Test
    void stenchIncludesOwnRoom() {
        assertEquals(List.of("W(3,1) ∨ W(2,1) ∨ W(3,2) ∨ W(4,1)"), text(Rules.stench(p(3, 1), true, 4)));
        assertEquals(List.of("¬W(4,1)", "¬W(3,1)", "¬W(4,2)"), text(Rules.stench(p(4, 1), false, 4)));
    }

    @Test
    void glitterIncludesOwnRoom() {
        assertEquals(List.of("G(1,1) ∨ G(1,2) ∨ G(2,1)"), text(Rules.glitter(p(1, 1), true, 4)));
        assertEquals(List.of("¬G(1,1)", "¬G(1,2)", "¬G(2,1)"), text(Rules.glitter(p(1, 1), false, 4)));
    }

    @Test
    void aliveInRoomMeansNoPitAndNoLivingWumpus() {
        assertEquals(List.of("¬P(2,2)", "¬W(2,2)"), text(Rules.alive(p(2, 2), true)));
        assertEquals(List.of("¬P(2,2)"), text(Rules.alive(p(2, 2), false)), "мёртвый Вампус может лежать здесь");
    }

    @Test
    void arrowLine() {
        assertEquals(List.of("W(3,1) ∨ W(2,1) ∨ W(1,1)"), text(Rules.arrow(p(4, 1), Direction.UP, true, 4)));
        assertEquals(List.of("¬W(4,2)", "¬W(4,3)", "¬W(4,4)"), text(Rules.arrow(p(4, 1), Direction.RIGHT, false, 4)));
        assertEquals(List.of(), text(Rules.arrow(p(4, 1), Direction.LEFT, false, 4)), "у стены стрела никуда не летит");
    }

    @Test
    void worldRulesForTwoByTwo() {
        assertEquals(List.of(
                "W(1,1) ∨ W(1,2) ∨ W(2,1) ∨ W(2,2)",
                "¬W(1,1) ∨ ¬W(1,2)", "¬W(1,1) ∨ ¬W(2,1)", "¬W(1,1) ∨ ¬W(2,2)",
                "¬W(1,2) ∨ ¬W(2,1)", "¬W(1,2) ∨ ¬W(2,2)", "¬W(2,1) ∨ ¬W(2,2)"),
                text(Rules.exactlyOne(Kind.WUMPUS, 2)));
        assertEquals(List.of(
                "¬G(1,1) ∨ W(1,2) ∨ W(2,1)", "¬G(1,2) ∨ W(2,2) ∨ W(1,1)",
                "¬G(2,1) ∨ W(1,1) ∨ W(2,2)", "¬G(2,2) ∨ W(1,2) ∨ W(2,1)"),
                text(Rules.goldNextToWumpus(2)));
        assertEquals(List.of("¬W(1,1) ∨ ¬P(1,1)", "¬G(1,1) ∨ ¬P(1,1)"), text(Rules.notInPit(2)).subList(0, 2));
        assertEquals(8, Rules.notInPit(2).size());
    }

    @Test
    void emptyClauseNotAllowed() {
        assertThrows(IllegalArgumentException.class, Clause::new);
    }
}
