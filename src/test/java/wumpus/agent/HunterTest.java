package wumpus.agent;

import org.junit.jupiter.api.Test;
import wumpus.logic.Status;
import wumpus.model.Action;
import wumpus.model.Percept;
import wumpus.model.Cave;
import wumpus.model.CaveGenerator;
import wumpus.model.CaveSpec;
import wumpus.model.Position;
import wumpus.model.World;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class HunterTest {

    private static final int ACTION_LIMIT = 5000;

    private static Position p(int row, int col) {
        return new Position(row, col);
    }

    /** Охотник играет в пещере до конца игры. */
    private static Hunter play(World world) {
        return play(world, new Hunter(world.cave().size()));
    }

    private static Hunter play(World world, Hunter hunter) {
        while (!world.over()) {
            assertTrue(world.actions() < ACTION_LIMIT, "игра не закончилась");
            world.execute(hunter.next(world.percept()));
        }
        return hunter;
    }

    private static Status truth(boolean present) {
        return present ? Status.YES : Status.NO;
    }

    @Test
    void firstThreeStepsAsInTask() {
        World world = new World(Cave.pdfExample());
        List<Turn> j = play(world).journal();

        assertEquals(p(4, 1), j.get(0).at());
        assertEquals("(none, none, none, none, none)", j.get(0).percept().toString());
        assertEquals(List.of("безопасны: 3,1, 4,2"), j.get(0).conclusions());
        assertEquals("иду в 4,2", j.get(0).decision());

        assertEquals(p(4, 2), j.get(1).at());
        assertEquals("(none, Breeze, none, none, none)", j.get(1).percept().toString());
        assertEquals(List.of("рядом яма, может быть в 3,2, 4,3"), j.get(1).conclusions());
        assertEquals("иду в 3,1", j.get(1).decision());

        assertEquals(p(3, 1), j.get(2).at());
        assertEquals("(Stench, none, none, none, none)", j.get(2).percept().toString());
        assertEquals(List.of("безопасны: 3,2", "яма в 4,3", "Вампус в 2,1"), j.get(2).conclusions());
        assertEquals("Вампус в 2,1 — стреляю вверх", j.get(2).decision());
    }

    @Test
    void taskExampleIsWon() {
        World world = new World(Cave.pdfExample());
        Hunter hunter = play(world);
        assertTrue(world.won());
        assertTrue(hunter.finished());
        assertTrue(hunter.hasGold());
        assertTrue(hunter.wumpusDead());
        assertEquals(p(2, 2), hunter.knownGold());
        assertTrue(hunter.journal().stream().noneMatch(Turn::risky));
        assertTrue(hunter.journal().get(3).conclusions().contains("крик — Вампус убит"));
        assertFalse(hunter.journal().get(4).conclusions().contains("крик — Вампус убит"), "сообщение — один раз");
    }

    @Test
    void knowledgeIsAlwaysTrue() {
        for (int size = 4; size <= 7; size++) {
            for (long seed = 0; seed < 60; seed++) {
                World world = new World(CaveGenerator.generate(new CaveSpec(size, 20, seed)));
                Hunter hunter = play(world);
                Cave cave = world.cave();
                for (CellKnowledge k : hunter.knowledge().values()) {
                    String where = "size " + size + " seed " + seed + " " + k.cell();
                    Position c = k.cell();
                    if (k.pit() != Status.MAYBE) {
                        assertEquals(truth(cave.isPit(c)), k.pit(), where);
                    }
                    if (k.wumpus() != Status.MAYBE) {
                        assertEquals(truth(cave.wumpus().equals(c)), k.wumpus(), where);
                    }
                    if (k.gold() != Status.MAYBE) {
                        assertEquals(truth(cave.gold().equals(c)), k.gold(), where);
                    }
                }
            }
        }
    }

    @Test
    void diesOnlyAfterRisk() {
        int deaths = 0;
        for (long seed = 0; seed < 300; seed++) {
            World world = new World(CaveGenerator.generate(new CaveSpec(6, 20, seed)));
            Hunter hunter = play(world);
            if (!world.alive()) {
                deaths++;
                assertTrue(hunter.journal().getLast().risky(), "seed " + seed + ": погиб без риска");
                CellKnowledge where = hunter.knowledge().get(world.position());
                assertNotEquals(Status.YES, where.pit(), "seed " + seed + ": пошёл в доказанную яму");
                assertNotEquals(Status.YES, where.wumpus(), "seed " + seed + ": пошёл к доказанному Вампусу");
            }
        }
        assertTrue(deaths > 0, "в выборке должны быть смерти, иначе тест ничего не проверяет");
    }

    @Test
    void openCaveAlwaysWon() {
        for (int col = 2; col <= 4; col++) {
            Cave cave = new Cave(4, p(1, col), p(1, col - 1), Set.of());
            World world = new World(cave);
            play(world);
            assertTrue(world.won(), "Вампус в 1," + col);
        }
    }

    @Test
    void blindShotMissedMeansGoHome() {
        // ветерок прямо у старта: ямы в 3,1 и 4,2, Вампус далеко в 1,4
        Cave cave = new Cave(4, p(1, 4), p(1, 3), Set.of(p(3, 1), p(4, 2)));
        World world = new World(cave);
        Hunter hunter = play(world);
        List<Turn> j = hunter.journal();
        assertEquals("безопасных ходов нет — стреляю вправо наугад", j.get(0).decision());
        assertTrue(j.get(0).risky());
        assertTrue(j.get(1).conclusions().contains("крика нет — Вампуса вправо нет"));
        assertEquals("стрелы нет, а Вампус жив — золото не взять, возвращаюсь к выходу", j.get(1).decision());
        assertTrue(world.over());
        assertTrue(world.alive());
        assertEquals(2, j.size());
        assertEquals(Status.NO, hunter.knowledge().get(p(4, 4)).wumpus(), "тишина после выстрела — на линии Вампуса нет");
        assertEquals(Status.MAYBE, hunter.knowledge().get(p(1, 1)).wumpus());
    }

    @Test
    void perceiveTwiceChangesNothing() {
        World world = new World(Cave.pdfExample());
        Hunter hunter = new Hunter(4);
        world.execute(hunter.next(world.percept()));
        hunter.perceive(world.percept());
        int clauses = hunter.clauseCount();
        hunter.perceive(world.percept());
        assertEquals(clauses, hunter.clauseCount());
        assertEquals(p(4, 2), hunter.position(), "шаг вперёд учтён один раз");
        assertEquals(new Percept(false, true, false, false, false), hunter.perceptAt(p(4, 2)));
        assertNull(hunter.perceptAt(p(3, 1)));
        world.execute(hunter.next(world.percept()));
        assertEquals(p(4, 2), hunter.position(), "и next после perceive не сдвигает охотника ещё раз");
    }

    @Test
    void screamHeardRightAfterShot() {
        World world = new World(Cave.pdfExample());
        Hunter hunter = new Hunter(4);
        Action a = hunter.next(world.percept());
        while (a != Action.SHOOT) {
            world.execute(a);
            a = hunter.next(world.percept());
        }
        world.execute(a);
        assertFalse(hunter.wumpusDead());
        hunter.perceive(world.percept());
        assertTrue(hunter.wumpusDead());
    }

    @Test
    void bumpKeepsHunterInPlace() {
        Hunter hunter = new Hunter(4);
        assertEquals(Action.FORWARD, hunter.next(Percept.NONE));
        hunter.next(new Percept(false, false, false, true, false));
        assertEquals(p(4, 1), hunter.position(), "удар о стену — охотник остался на месте");
    }

    @Test
    void grabCountsOnlyWhenGlitterIsGone() {
        World world = new World(Cave.pdfExample());
        Hunter hunter = new Hunter(4);
        Action a = hunter.next(world.percept());
        while (a != Action.GRAB) {
            world.execute(a);
            a = hunter.next(world.percept());
        }
        Percept stillGlitter = world.percept();
        assertTrue(stillGlitter.glitter());
        hunter.next(stillGlitter);
        assertFalse(hunter.hasGold(), "блеск остался — золото не взято");
    }

    @Test
    void shotFromAnotherRoomSaysWhereHunterGoes() {
        World blind = new World(CaveGenerator.generate(new CaveSpec(6, 20, 1L)));
        List<Turn> j = play(blind).journal();
        assertEquals(p(5, 3), j.get(7).at());
        assertEquals("безопасных ходов нет — иду в 3,1 и стреляю вправо наугад", j.get(7).decision());
        assertEquals(p(3, 1), j.get(8).at());

        World aimed = new World(CaveGenerator.generate(new CaveSpec(12, 20, 3L)));
        j = play(aimed).journal();
        assertEquals(p(12, 7), j.get(6).at());
        assertEquals("Вампус в 10,6 — иду в 12,6 и стреляю вверх", j.get(6).decision());
    }

    @Test
    void cautiousHunterLeavesInsteadOfRisk() {
        Cave cave = new Cave(4, p(1, 4), p(1, 3), Set.of(p(3, 1), p(4, 2)));
        World world = new World(cave);
        Hunter hunter = play(world, new Hunter(4, false));
        assertEquals("идти некуда — возвращаюсь к выходу без золота", hunter.journal().getFirst().decision());
        assertTrue(hunter.hasArrow());
        assertTrue(world.alive());
        assertEquals(1, world.actions(), "сразу выбрался");
    }

    @Test
    void cautiousHunterNeverDies() {
        int wins = 0;
        for (long seed = 0; seed < 200; seed++) {
            World world = new World(CaveGenerator.generate(new CaveSpec(6, 20, seed)));
            Hunter hunter = play(world, new Hunter(6, false));
            assertTrue(world.alive(), "seed " + seed);
            assertTrue(hunter.journal().stream().noneMatch(Turn::risky), "seed " + seed);
            wins += world.won() ? 1 : 0;
        }
        assertTrue(wins > 40, "побед " + wins + " из 200");
    }

    @Test
    void riskWinsMoreOften() {
        int bold = 0;
        int cautious = 0;
        for (long seed = 0; seed < 200; seed++) {
            Cave cave = CaveGenerator.generate(new CaveSpec(6, 20, seed));
            World a = new World(cave);
            play(a, new Hunter(6, true));
            World b = new World(cave);
            play(b, new Hunter(6, false));
            bold += a.won() ? 1 : 0;
            cautious += b.won() ? 1 : 0;
        }
        assertTrue(bold > cautious, "с риском " + bold + ", осторожно " + cautious);
    }

    @Test
    void noMovesAfterClimb() {
        World world = new World(Cave.pdfExample());
        Hunter hunter = play(world);
        assertThrows(IllegalStateException.class, () -> hunter.next(world.percept()));
    }

    @Test
    void initialKnowledgeIsAllUnknown() {
        Hunter hunter = new Hunter(4);
        assertEquals(16, hunter.knowledge().size());
        assertTrue(hunter.knowledge().values().stream().noneMatch(CellKnowledge::visited));
        assertTrue(hunter.journal().isEmpty());
        assertEquals(p(4, 1), hunter.position());
        assertNull(hunter.knownWumpus());
        assertTrue(hunter.hasArrow());
    }
}
