package wumpus.logic;

import org.junit.jupiter.api.Test;
import wumpus.model.Direction;
import wumpus.model.Position;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static wumpus.logic.Kind.*;
import static wumpus.logic.Status.*;

class KnowledgeBaseTest {

    private static Position p(int row, int col) {
        return new Position(row, col);
    }

    private static Fact f(Kind kind, int row, int col) {
        return new Fact(kind, p(row, col));
    }

    /** Измерение в комнате, как его записывает охотник. */
    private static void sense(KnowledgeBase kb, int row, int col, boolean stench, boolean breeze, boolean glitter) {
        Position at = p(row, col);
        kb.tell(Rules.alive(at, true));
        kb.tell(Rules.stench(at, stench, kb.size()));
        kb.tell(Rules.breeze(at, breeze, kb.size()));
        kb.tell(Rules.glitter(at, glitter, kb.size()));
    }

    private static Status status(KnowledgeBase kb, Kind kind, int row, int col) {
        return kb.status(f(kind, row, col));
    }

    @Test
    void step1NeighborsOfStartAreSafe() {
        KnowledgeBase kb = new KnowledgeBase(4);
        sense(kb, 4, 1, false, false, false);
        assertEquals(NO, status(kb, PIT, 3, 1));
        assertEquals(NO, status(kb, PIT, 4, 2));
        assertEquals(NO, status(kb, WUMPUS, 3, 1));
        assertEquals(NO, status(kb, WUMPUS, 4, 2));
        assertEquals(MAYBE, status(kb, PIT, 3, 2), "дальше ничего не известно");
        assertEquals(MAYBE, status(kb, WUMPUS, 2, 1));
    }

    @Test
    void step2BreezeGivesPitMaybeInTwoRooms() {
        KnowledgeBase kb = new KnowledgeBase(4);
        sense(kb, 4, 1, false, false, false);
        sense(kb, 4, 2, false, true, false);
        assertEquals(MAYBE, status(kb, PIT, 3, 2));
        assertEquals(MAYBE, status(kb, PIT, 4, 3));
        assertEquals(NO, status(kb, WUMPUS, 3, 2));
        assertEquals(NO, status(kb, WUMPUS, 4, 3));
    }

    @Test
    void step3CombinesAllSteps() {
        KnowledgeBase kb = new KnowledgeBase(4);
        sense(kb, 4, 1, false, false, false);
        sense(kb, 4, 2, false, true, false);
        sense(kb, 3, 1, true, false, false);
        assertEquals(NO, status(kb, PIT, 3, 2), "в 3,2 нет ямы");
        assertEquals(YES, status(kb, PIT, 4, 3), "яма в 4,3");
        assertEquals(YES, status(kb, WUMPUS, 2, 1), "Вампус в 2,1");
        assertEquals(NO, status(kb, WUMPUS, 3, 2));
        assertEquals(NO, status(kb, WUMPUS, 1, 4), "Вампус один — остальные комнаты свободны от него");
        assertEquals(NO, status(kb, PIT, 2, 1), "Вампус не сидит в яме");
        assertEquals(MAYBE, status(kb, GOLD, 2, 2), "золото рядом с Вампусом: 1,1 или 2,2");
        assertEquals(MAYBE, status(kb, GOLD, 1, 1));
        assertEquals(NO, status(kb, GOLD, 1, 2), "1,2 не соседняя с Вампусом");
        assertEquals(NO, status(kb, GOLD, 3, 2), "рядом с 4,2 блеска не было");
    }

    @Test
    void glitterPinsGold() {
        KnowledgeBase kb = new KnowledgeBase(4);
        sense(kb, 4, 1, false, false, false);
        sense(kb, 3, 1, true, false, false);
        sense(kb, 4, 2, false, true, false);
        sense(kb, 3, 2, false, false, true);
        assertEquals(YES, status(kb, GOLD, 2, 2), "Вампус в 2,1, блеск в 3,2 — золото в 2,2");
    }

    @Test
    void arrowWithScreamLocatesWumpusOnLine() {
        KnowledgeBase kb = new KnowledgeBase(4);
        sense(kb, 4, 1, false, false, false);
        kb.tell(Rules.arrow(p(4, 1), Direction.UP, true, 4));
        assertEquals(NO, status(kb, WUMPUS, 3, 1));
        assertEquals(MAYBE, status(kb, WUMPUS, 2, 1));
        assertEquals(MAYBE, status(kb, WUMPUS, 1, 1));
        assertEquals(NO, status(kb, WUMPUS, 1, 2));
    }

    @Test
    void entailsBothWays() {
        KnowledgeBase kb = new KnowledgeBase(4);
        sense(kb, 4, 1, false, false, false);
        assertTrue(kb.entails(Literal.not(f(PIT, 3, 1))));
        assertFalse(kb.entails(Literal.of(f(PIT, 3, 1))));
        assertFalse(kb.entails(Literal.of(f(PIT, 2, 2))));
        assertFalse(kb.entails(Literal.not(f(PIT, 2, 2))));
    }

    @Test
    void statusesForManyFactsMatchSingleQuestions() {
        KnowledgeBase kb = new KnowledgeBase(4);
        sense(kb, 4, 1, false, false, false);
        sense(kb, 4, 2, false, true, false);
        sense(kb, 3, 1, true, false, false);
        List<Fact> all = new ArrayList<>();
        for (Kind kind : Kind.values()) {
            for (int row = 1; row <= 4; row++) {
                for (int col = 1; col <= 4; col++) {
                    all.add(f(kind, row, col));
                }
            }
        }
        Map<Fact, Status> many = kb.statuses(all);
        KnowledgeBase fresh = new KnowledgeBase(4);
        sense(fresh, 4, 1, false, false, false);
        sense(fresh, 4, 2, false, true, false);
        sense(fresh, 3, 1, true, false, false);
        for (Fact fact : all) {
            boolean yes = fresh.entails(Literal.of(fact));
            boolean no = fresh.entails(Literal.not(fact));
            Status expected = yes ? YES : no ? NO : MAYBE;
            assertEquals(expected, many.get(fact), fact.toString());
        }
    }

    @Test
    void knowledgeOnlyGrows() {
        KnowledgeBase kb = new KnowledgeBase(4);
        sense(kb, 4, 1, false, false, false);
        assertEquals(NO, status(kb, PIT, 3, 1));
        int before = kb.clauses().size();
        sense(kb, 4, 2, false, true, false);
        assertTrue(kb.clauses().size() > before);
        assertEquals(NO, status(kb, PIT, 3, 1));
    }

    @Test
    void maybeIsRecheckedAfterNewMeasurements() {
        KnowledgeBase kb = new KnowledgeBase(4);
        sense(kb, 4, 1, false, false, false);
        assertEquals(MAYBE, status(kb, PIT, 3, 2));
        assertEquals(MAYBE, status(kb, PIT, 4, 3));
        sense(kb, 4, 2, false, true, false);
        sense(kb, 3, 1, true, false, false);
        assertEquals(NO, status(kb, PIT, 3, 2));
        assertEquals(YES, status(kb, PIT, 4, 3));
    }

    @Test
    void wumpusAndGoldAreNeverInPit() {
        KnowledgeBase kb = new KnowledgeBase(4);
        kb.tell(new Clause(Literal.of(f(WUMPUS, 2, 2))));
        assertEquals(NO, status(kb, PIT, 2, 2));
        kb.tell(new Clause(Literal.of(f(GOLD, 1, 2))));
        assertEquals(NO, status(kb, PIT, 1, 2));
    }

    @Test
    void onlyOneGold() {
        KnowledgeBase kb = new KnowledgeBase(4);
        kb.tell(new Clause(Literal.of(f(GOLD, 2, 2))));
        assertEquals(NO, status(kb, GOLD, 1, 1), "по диагонали: один Вампус в 1,2 был бы рядом с обоими");
        assertEquals(NO, status(kb, GOLD, 3, 3));
    }

    @Test
    void foundModelsAreReused() {
        KnowledgeBase kb = new KnowledgeBase(4);
        sense(kb, 4, 1, false, false, false);
        List<Fact> all = new ArrayList<>();
        for (Kind kind : Kind.values()) {
            for (int row = 1; row <= 4; row++) {
                for (int col = 1; col <= 4; col++) {
                    all.add(f(kind, row, col));
                }
            }
        }
        kb.statuses(all);
        assertTrue(kb.solverCalls() < all.size(), "запусков DPLL: " + kb.solverCalls() + " на " + all.size() + " фактов");
    }

    @Test
    void badSizeRejected() {
        assertThrows(IllegalArgumentException.class, () -> new KnowledgeBase(1));
        assertThrows(IllegalArgumentException.class, () -> new KnowledgeBase(13));
        assertDoesNotThrow(() -> new KnowledgeBase(2));
        assertDoesNotThrow(() -> new KnowledgeBase(12));
    }

    @Test
    void contradictionIsReported() {
        KnowledgeBase kb = new KnowledgeBase(4);
        kb.tell(new Clause(Literal.of(f(PIT, 2, 2))));
        kb.tell(new Clause(Literal.not(f(PIT, 2, 2))));
        assertThrows(IllegalStateException.class, () -> kb.status(f(PIT, 1, 1)));
    }

    @Test
    void factOutsideCaveRejected() {
        KnowledgeBase kb = new KnowledgeBase(4);
        assertThrows(IllegalArgumentException.class, () -> kb.status(f(PIT, 5, 1)));
        assertThrows(IllegalArgumentException.class, () -> kb.tell(new Clause(Literal.of(f(PIT, 0, 1)))));
    }

    @Test
    void biggestCaveAnsweredQuickly() {
        KnowledgeBase kb = new KnowledgeBase(12);
        sense(kb, 12, 1, false, false, false);
        sense(kb, 11, 1, false, true, false);
        sense(kb, 12, 2, true, false, false);
        List<Fact> all = new ArrayList<>();
        for (Kind kind : Kind.values()) {
            for (int row = 1; row <= 12; row++) {
                for (int col = 1; col <= 12; col++) {
                    all.add(f(kind, row, col));
                }
            }
        }
        long start = System.nanoTime();
        Map<Fact, Status> result = kb.statuses(all);
        long ms = (System.nanoTime() - start) / 1_000_000;
        assertEquals(YES, result.get(f(WUMPUS, 12, 3)), "запах только в 12,2, а 11,2 и 12,1 исключены соседями без запаха");
        assertTrue(ms < 2000, "все вопросы по пещере 12×12 заняли " + ms + " мс");
    }
}
