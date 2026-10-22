package wumpus.controller;

import org.junit.jupiter.api.Test;
import wumpus.agent.Hunter;
import wumpus.model.Action;
import wumpus.model.Cave;
import wumpus.model.Position;
import wumpus.model.World;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TextsTest {

    private static Position p(int row, int col) {
        return new Position(row, col);
    }

    @Test
    void statusAlongTaskExample() {
        GameController c = new GameController();
        Hunter h = c.hunter();
        assertEquals("не найден", Texts.wumpus(h));
        assertEquals("не найдено", Texts.gold(h));
        assertEquals("есть", Texts.arrow(h));
        assertEquals("", Texts.result(c.world()));
        c.stepOnce();
        c.stepOnce();
        assertEquals("в 2,1, жив", Texts.wumpus(c.hunter()), "в 3,1 вывел Вампуса, выстрел — следующим шагом");
        assertEquals("есть", Texts.arrow(c.hunter()));
        c.stepOnce();
        assertEquals("убит", Texts.wumpus(c.hunter()));
        assertEquals("нет", Texts.arrow(c.hunter()));
        while (!c.world().over()) {
            c.stepOnce();
            if (c.hunter().knownGold() != null && !c.hunter().hasGold()) {
                assertEquals("в 2,2", Texts.gold(c.hunter()));
            }
        }
        assertEquals("у охотника", Texts.gold(c.hunter()));
        assertEquals("Победа: охотник выбрался с золотом", Texts.result(c.world()));
    }

    @Test
    void wumpusFoundButAlive() {
        World w = new World(Cave.pdfExample());
        Hunter h = new Hunter(4);
        // до выстрела: охотник уже вывел, где Вампус, но стрела ещё не выпущена
        for (Action a = h.next(w.percept()); a != Action.SHOOT; a = h.next(w.percept())) {
            w.execute(a);
        }
        assertEquals("в 2,1, жив", Texts.wumpus(h));
        assertEquals(p(3, 1), w.position());
    }

    @Test
    void deathTexts() {
        World pit = new World(Cave.pdfExample());
        pit.execute(Action.FORWARD);
        pit.execute(Action.FORWARD);
        assertEquals("Охотник упал в яму в 4,3", Texts.result(pit));
        World eaten = new World(Cave.pdfExample());
        eaten.execute(Action.TURN_LEFT);
        eaten.execute(Action.FORWARD);
        eaten.execute(Action.FORWARD);
        assertEquals("Охотника съел Вампус в 2,1", Texts.result(eaten));
    }

    @Test
    void leftWithoutGold() {
        World w = new World(new Cave(4, p(1, 4), p(1, 3), Set.of()));
        w.execute(Action.CLIMB);
        assertEquals("Охотник выбрался без золота", Texts.result(w));
    }

    @Test
    void scoreWithSign() {
        assertEquals("+974", Texts.score(974));
        assertEquals("−7", Texts.score(-7));
        assertEquals("0", Texts.score(0));
    }
}
