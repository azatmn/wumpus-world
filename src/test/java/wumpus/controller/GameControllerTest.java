package wumpus.controller;

import org.junit.jupiter.api.Test;
import wumpus.model.Cave;
import wumpus.model.CaveSpec;
import wumpus.model.Position;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GameControllerTest {

    private static final long SECOND = 1_000_000_000L;

    private static Position p(int row, int col) {
        return new Position(row, col);
    }

    @Test
    void startsWithTaskExample() {
        GameController c = new GameController();
        assertEquals(p(2, 1), c.cave().wumpus());
        assertEquals(p(4, 1), c.world().position());
        assertTrue(c.hunter().journal().isEmpty());
        assertFalse(c.isRunning());
        assertTrue(c.isTakeRisks());
    }

    @Test
    void stepIsOneInferenceStepOfTask() {
        GameController c = new GameController();
        c.stepOnce();
        assertEquals(1, c.hunter().journal().size());
        assertEquals(p(4, 2), c.world().position(), "решение шага 1 выполнено: охотник в 4,2");
        c.stepOnce();
        assertEquals(2, c.hunter().journal().size());
        assertEquals(p(3, 1), c.world().position());
        c.stepOnce();
        assertEquals(3, c.hunter().journal().size());
        assertFalse(c.world().wumpusAlive(), "шаг 3: выстрел по Вампусу в 2,1");
    }

    @Test
    void stepsPlayWholeGameAndThenDoNothing() {
        GameController c = new GameController();
        for (int i = 0; i < 100 && !c.world().over(); i++) {
            c.stepOnce();
        }
        assertTrue(c.world().won());
        int actions = c.world().actions();
        c.stepOnce();
        assertEquals(actions, c.world().actions());
    }

    @Test
    void stepPausesRunning() {
        GameController c = new GameController();
        c.start();
        c.stepOnce();
        assertFalse(c.isRunning());
    }

    @Test
    void runningMakesActionsBySpeed() {
        GameController c = new GameController();
        c.setSpeed(4);
        c.start();
        c.tick(0);
        assertEquals(0, c.world().actions(), "первый кадр только запоминает время");
        c.tick(SECOND);
        assertEquals(4, c.world().actions());
        c.tick(SECOND + SECOND / 2);
        assertEquals(6, c.world().actions());
    }

    @Test
    void pausedOrNotStartedDoesNothing() {
        GameController c = new GameController();
        c.tick(0);
        c.tick(SECOND);
        assertEquals(0, c.world().actions());
        c.start();
        c.tick(2 * SECOND);
        c.pause();
        c.tick(5 * SECOND);
        assertEquals(0, c.world().actions());
    }

    @Test
    void longFreezeCountsAsOneSecond() {
        GameController c = new GameController();
        c.setSpeed(4);
        c.start();
        c.tick(0);
        c.tick(60 * SECOND);
        assertEquals(4, c.world().actions());
    }

    @Test
    void timeGoingBackIsIgnored() {
        GameController c = new GameController();
        c.setSpeed(4);
        c.start();
        c.tick(10 * SECOND);
        c.tick(5_300_000_000L);
        c.tick(5_800_000_000L);
        assertEquals(2, c.world().actions(), "время назад — как новый отсчёт, без долга");
    }

    @Test
    void pauseTimeIsNotTurnedIntoActions() {
        GameController c = new GameController();
        c.setSpeed(4);
        c.start();
        c.tick(0);
        c.tick(SECOND / 2);
        c.pause();
        c.start();
        c.tick(10 * SECOND);
        c.tick(10 * SECOND + SECOND / 4);
        assertEquals(3, c.world().actions());
    }

    @Test
    void runningStopsWhenGameEnds() {
        GameController c = new GameController();
        c.setSpeed(GameController.MAX_SPEED);
        c.start();
        long t = 0;
        for (int i = 0; i < 100 && !c.world().over(); i++) {
            c.tick(t);
            t += SECOND;
        }
        assertTrue(c.world().over());
        assertFalse(c.isRunning());
        c.start();
        assertFalse(c.isRunning(), "после конца игры старт ничего не делает");
    }

    @Test
    void toggle() {
        GameController c = new GameController();
        c.toggleRunning();
        assertTrue(c.isRunning());
        c.toggleRunning();
        assertFalse(c.isRunning());
    }

    @Test
    void speedClamped() {
        GameController c = new GameController();
        c.setSpeed(0);
        assertEquals(GameController.MIN_SPEED, c.getSpeed());
        c.setSpeed(Integer.MAX_VALUE);
        assertEquals(GameController.MAX_SPEED, c.getSpeed());
        c.setSpeed(7);
        assertEquals(7, c.getSpeed());
    }

    @Test
    void restartKeepsCaveAndForgetsKnowledge() {
        GameController c = new GameController();
        c.newCave(new CaveSpec(8, 20, 5L));
        Cave cave = c.cave();
        c.stepOnce();
        c.start();
        c.restart();
        assertSame(cave, c.cave());
        assertEquals(0, c.world().actions());
        assertTrue(c.hunter().journal().isEmpty());
        assertFalse(c.isRunning());
    }

    @Test
    void newCaveFollowsSpec() {
        GameController c = new GameController();
        CaveSpec spec = new CaveSpec(9, 30, 77L);
        c.newCave(spec);
        assertEquals(spec, c.spec());
        assertEquals(9, c.cave().size());
        assertEquals(spec.pitCount(), c.cave().pits().size());
        assertEquals(9, c.hunter().knowledge().size() / 9);
        c.pdfExample();
        assertEquals(4, c.cave().size());
        assertEquals(spec, c.spec(), "форма помнит последний заказ");
    }

    @Test
    void deadHunterLearnsNothingFromDeathRoom() {
        GameController c = new GameController();
        c.newCave(new CaveSpec(6, 20, 1L));
        while (!c.world().over()) {
            c.stepOnce();
        }
        assertFalse(c.world().alive());
        assertNull(c.hunter().perceptAt(c.world().position()), "в яме охотник уже не измеряет и не пишет «жив здесь»");
    }

    @Test
    void riskSwitchRestartsWithNewHunter() {
        // ветерок у старта: с риском охотник стреляет наугад, без риска сразу уходит
        GameController c = new GameController(new Cave(4, p(1, 4), p(1, 3), Set.of(p(3, 1), p(4, 2))));
        c.stepOnce();
        assertTrue(c.hunter().journal().getFirst().risky());
        c.setTakeRisks(false);
        assertFalse(c.isTakeRisks());
        assertEquals(0, c.world().actions());
        c.stepOnce();
        assertEquals("идти некуда — возвращаюсь к выходу без золота", c.hunter().journal().getFirst().decision());
        assertTrue(c.world().over());
    }
}
