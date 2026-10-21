package wumpus.controller;

import org.junit.jupiter.api.Test;
import wumpus.agent.Hunter;
import wumpus.model.Action;
import wumpus.model.Cave;
import wumpus.model.Position;
import wumpus.model.World;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MarksTest {

    private static Position p(int row, int col) {
        return new Position(row, col);
    }

    /** Охотник проходит пример задания до решения номер turns; само решение ещё не выполнено. */
    private static Hunter upToTurn(int turns) {
        World w = new World(Cave.pdfExample());
        Hunter h = new Hunter(4);
        while (true) {
            Action a = h.next(w.percept());
            if (h.journal().size() >= turns) {
                return h;
            }
            w.execute(a);
        }
    }

    @Test
    void nothingKnownBeforeFirstStep() {
        Hunter h = new Hunter(4);
        for (Position c : h.knowledge().keySet()) {
            assertEquals(List.of(), Marks.of(h, c), c.toString());
        }
    }

    @Test
    void step1NeighborsOk() {
        Hunter h = upToTurn(1);
        assertEquals(List.of("OK"), Marks.of(h, p(3, 1)));
        assertEquals(List.of("OK"), Marks.of(h, p(4, 2)));
        assertEquals(List.of(), Marks.of(h, p(3, 2)), "дальние комнаты — пусто, без «?»");
        assertEquals(List.of(), Marks.of(h, p(4, 1)), "сама комната старта — посещена");
    }

    @Test
    void step2PitMaybe() {
        Hunter h = upToTurn(2);
        assertEquals(List.of("P?"), Marks.of(h, p(3, 2)));
        assertEquals(List.of("P?"), Marks.of(h, p(4, 3)));
        assertEquals(List.of("OK"), Marks.of(h, p(3, 1)));
    }

    @Test
    void step3AsInTask() {
        Hunter h = upToTurn(3);
        assertEquals(List.of("OK"), Marks.of(h, p(3, 2)));
        assertEquals(List.of("P!"), Marks.of(h, p(4, 3)));
        assertEquals(List.of("W!"), Marks.of(h, p(2, 1)));
        assertEquals(List.of(), Marks.of(h, p(1, 1)), "золото может быть в 1,1, но блеска рядом не было");
    }

    @Test
    void wumpusMarksDisappearAfterKill() {
        Hunter h = upToTurn(4);
        assertTrue(h.wumpusDead());
        assertEquals(List.of("OK"), Marks.of(h, p(2, 1)), "мёртвый Вампус не опасен");
    }
}
