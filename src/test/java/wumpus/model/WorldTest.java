package wumpus.model;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static wumpus.model.Action.*;

class WorldTest {

    private static Position p(int row, int col) {
        return new Position(row, col);
    }

    private static World pdf() {
        return new World(Cave.pdfExample());
    }

    /** Пещера 4×4 без ям: Вампус в 1,1, золото в 1,2. */
    private static World open() {
        return new World(new Cave(4, p(1, 1), p(1, 2), Set.of()));
    }

    /** В пещере open(): от старта до золота в обход Вампуса, 5 действий. */
    private static final Action[] TO_GOLD = {FORWARD, TURN_LEFT, FORWARD, FORWARD, FORWARD};

    /** Из open() после TO_GOLD: убить Вампуса и взять золото, 3 действия. */
    private static final Action[] KILL_AND_GRAB = {TURN_LEFT, SHOOT, GRAB};

    private static void run(World w, Action... actions) {
        for (Action a : actions) {
            w.execute(a);
        }
    }

    @Test
    void startState() {
        World w = pdf();
        assertEquals(p(4, 1), w.position());
        assertEquals(Direction.RIGHT, w.facing());
        assertTrue(w.hasArrow());
        assertTrue(w.alive());
        assertTrue(w.wumpusAlive());
        assertFalse(w.hasGold());
        assertFalse(w.over());
        assertFalse(w.won());
        assertEquals(0, w.score());
        assertEquals(0, w.actions());
        assertEquals(Percept.NONE, w.percept(), "первое измерение из задания");
    }

    @Test
    void firstPerceptAlreadyAtStart() {
        World w = new World(new Cave(4, p(1, 1), p(1, 2), Set.of(p(3, 1))));
        assertEquals(new Percept(false, true, false, false, false), w.percept());
    }

    @Test
    void stepsOfTaskExample() {
        World w = pdf();
        Percept second = w.execute(FORWARD);
        assertEquals(p(4, 2), w.position());
        assertEquals(new Percept(false, true, false, false, false), second, "шаг 2: ветерок");
        run(w, TURN_LEFT, TURN_LEFT, FORWARD, TURN_RIGHT);
        Percept third = w.execute(FORWARD);
        assertEquals(p(3, 1), w.position());
        assertEquals(new Percept(true, false, false, false, false), third, "шаг 3: запах");
        assertEquals(third, w.percept());
        assertEquals(-6, w.score());
        assertEquals(6, w.actions());
    }

    @Test
    void wallGivesBumpAndKeepsPlace() {
        World w = pdf();
        run(w, TURN_RIGHT);
        Percept bump = w.execute(FORWARD);
        assertTrue(bump.bump());
        assertEquals(p(4, 1), w.position());
        assertFalse(w.execute(TURN_RIGHT).bump(), "удар — только в тот ход");
    }

    @Test
    void pitKills() {
        World w = pdf();
        run(w, FORWARD, FORWARD);
        assertFalse(w.alive());
        assertTrue(w.over());
        assertFalse(w.won());
        assertEquals(p(4, 3), w.position());
        assertEquals(-2 - 1000, w.score());
    }

    @Test
    void livingWumpusKills() {
        World w = pdf();
        run(w, TURN_LEFT, FORWARD, FORWARD);
        assertFalse(w.alive());
        assertTrue(w.over());
    }

    @Test
    void noActionsAfterGameOver() {
        World w = pdf();
        run(w, TURN_LEFT, FORWARD, FORWARD);
        assertThrows(IllegalStateException.class, () -> w.execute(TURN_LEFT));
    }

    @Test
    void arrowKillsWumpusInLineAndScreamOnce() {
        World w = pdf();
        run(w, TURN_LEFT, FORWARD);
        Percept shot = w.execute(SHOOT);
        assertTrue(shot.scream());
        assertFalse(w.wumpusAlive());
        assertFalse(w.hasArrow());
        assertEquals(-3 - 10, w.score());
        assertFalse(w.execute(TURN_LEFT).scream(), "крик слышен только в ход выстрела");
    }

    @Test
    void arrowFliesFarUntilWall() {
        World w = pdf();
        run(w, TURN_LEFT);
        Percept shot = w.execute(SHOOT);
        assertTrue(shot.scream(), "Вампус через клетку от охотника — стрела долетает");
    }

    @Test
    void arrowMissesOutsideLine() {
        World w = pdf();
        Percept shot = w.execute(SHOOT);
        assertFalse(shot.scream());
        assertTrue(w.wumpusAlive());
        assertFalse(w.hasArrow());
        assertEquals(-1 - 10, w.score());
    }

    @Test
    void secondShotHasNoArrow() {
        World w = pdf();
        run(w, SHOOT, TURN_LEFT);
        Percept again = w.execute(SHOOT);
        assertFalse(again.scream(), "смотрит на Вампуса, но стрелы уже нет");
        assertTrue(w.wumpusAlive());
        assertEquals(-3 - 10, w.score(), "без стрелы — только цена действия");
    }

    @Test
    void deadWumpusRoomIsSafe() {
        World w = pdf();
        run(w, TURN_LEFT, SHOOT, FORWARD, FORWARD);
        assertTrue(w.alive());
        assertEquals(p(2, 1), w.position());
    }

    @Test
    void goldOnlyAfterWumpusKilled() {
        World w = open();
        run(w, TO_GOLD);
        assertEquals(p(1, 2), w.position());
        assertTrue(w.percept().glitter());
        w.execute(GRAB);
        assertFalse(w.hasGold(), "Вампус жив — золото не взять");
        run(w, TURN_LEFT, SHOOT);
        assertFalse(w.wumpusAlive());
        w.execute(GRAB);
        assertTrue(w.hasGold());
    }

    @Test
    void grabFarFromGoldDoesNothing() {
        World w = pdf();
        run(w, SHOOT, GRAB);
        assertFalse(w.hasGold());
    }

    @Test
    void glitterDisappearsWithGold() {
        World w = open();
        run(w, TO_GOLD);
        run(w, KILL_AND_GRAB);
        assertTrue(w.hasGold());
        assertFalse(w.execute(TURN_LEFT).glitter());
    }

    @Test
    void climbOnlyAtStart() {
        World w = pdf();
        run(w, FORWARD, CLIMB);
        assertFalse(w.over());
        run(w, TURN_LEFT, TURN_LEFT, FORWARD, CLIMB);
        assertTrue(w.over());
        assertTrue(w.alive());
        assertFalse(w.won(), "без золота — не победа");
        assertEquals(-6, w.score());
    }

    @Test
    void climbWithGoldWins() {
        World w = open();
        run(w, TO_GOLD);
        run(w, KILL_AND_GRAB);
        run(w, TURN_LEFT, FORWARD, FORWARD, FORWARD, TURN_RIGHT, FORWARD, CLIMB);
        assertTrue(w.over());
        assertTrue(w.won());
        assertEquals(p(4, 1), w.position());
        assertEquals(1000 - 15 - 10, w.score());
    }

    @Test
    void nullActionRejected() {
        assertThrows(NullPointerException.class, () -> pdf().execute(null));
    }
}
