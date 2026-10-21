package wumpus.model;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CaveGeneratorTest {

    @Test
    void sameSeedSameCave() {
        CaveSpec spec = new CaveSpec(8, 20, 42L);
        Cave a = CaveGenerator.generate(spec);
        Cave b = CaveGenerator.generate(spec);
        assertEquals(a.wumpus(), b.wumpus());
        assertEquals(a.gold(), b.gold());
        assertEquals(a.pits(), b.pits());
    }

    @Test
    void differentSeedsGiveDifferentCaves() {
        Set<Set<Position>> layouts = new HashSet<>();
        for (long seed = 0; seed < 20; seed++) {
            layouts.add(CaveGenerator.generate(new CaveSpec(8, 20, seed)).pits());
        }
        assertTrue(layouts.size() > 15, "разных расстановок: " + layouts.size());
    }

    @Test
    void rulesHoldForManyCaves() {
        for (int size = 4; size <= Cave.MAX_SIZE; size++) {
            for (int percent : new int[]{0, 20, 40}) {
                for (long seed = 0; seed < 30; seed++) {
                    CaveSpec spec = new CaveSpec(size, percent, seed);
                    Cave c = CaveGenerator.generate(spec);
                    String where = spec.toString();
                    assertEquals(size, c.size(), where);
                    assertEquals(spec.pitCount(), c.pits().size(), where);
                    Set<Position> startZone = new HashSet<>(c.start().neighbors(size));
                    startZone.add(c.start());
                    assertFalse(startZone.contains(c.wumpus()), where);
                    for (Position pit : c.pits()) {
                        assertFalse(startZone.contains(pit), where);
                    }
                    assertTrue(c.wumpus().isNeighbor(c.gold()), where);
                }
            }
        }
    }

    @Test
    void everythingCanAppearAnywhereOutsideStartZone() {
        Set<Position> wumpusSeen = new HashSet<>();
        Set<Position> pitSeen = new HashSet<>();
        Set<Position> goldSeen = new HashSet<>();
        for (long seed = 0; seed < 2000; seed++) {
            Cave c = CaveGenerator.generate(new CaveSpec(4, 20, seed));
            wumpusSeen.add(c.wumpus());
            pitSeen.addAll(c.pits());
            goldSeen.add(c.gold());
        }
        assertEquals(13, wumpusSeen.size(), "16 клеток минус старт и два соседа");
        assertEquals(13, pitSeen.size());
        assertEquals(15, goldSeen.size(), "золото может лежать и рядом со стартом, но не на нём");
    }
}
