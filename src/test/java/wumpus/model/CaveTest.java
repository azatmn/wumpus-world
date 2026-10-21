package wumpus.model;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CaveTest {

    private static Position p(int row, int col) {
        return new Position(row, col);
    }

    @Test
    void pdfExampleMatchesPicture() {
        Cave c = Cave.pdfExample();
        assertEquals(4, c.size());
        assertEquals(p(4, 1), c.start());
        assertEquals(p(2, 1), c.wumpus());
        assertEquals(p(2, 2), c.gold());
        assertEquals(Set.of(p(1, 4), p(2, 3), p(4, 3)), c.pits());
    }

    @Test
    void breezeNextToPitOnly() {
        Cave c = Cave.pdfExample();
        assertTrue(c.hasBreeze(p(4, 2)));
        assertTrue(c.hasBreeze(p(1, 3)));
        assertTrue(c.hasBreeze(p(3, 3)));
        assertFalse(c.hasBreeze(p(4, 1)));
        assertFalse(c.hasBreeze(p(3, 1)));
        assertFalse(c.hasBreeze(p(1, 1)));
        assertFalse(c.hasBreeze(p(4, 3)), "в самой яме ветерка нет — только у соседей");
    }

    @Test
    void stenchNextToWumpusAndInItsRoom() {
        Cave c = Cave.pdfExample();
        assertTrue(c.hasStench(p(1, 1)));
        assertTrue(c.hasStench(p(3, 1)));
        assertTrue(c.hasStench(p(2, 2)));
        assertTrue(c.hasStench(p(2, 1)));
        assertFalse(c.hasStench(p(4, 1)));
        assertFalse(c.hasStench(p(3, 2)));
        assertFalse(c.hasStench(p(1, 2)));
    }

    @Test
    void goldNearInItsRoomAndNeighbors() {
        Cave c = Cave.pdfExample();
        assertTrue(c.nearGold(p(2, 2)));
        assertTrue(c.nearGold(p(1, 2)));
        assertTrue(c.nearGold(p(3, 2)));
        assertTrue(c.nearGold(p(2, 1)));
        assertTrue(c.nearGold(p(2, 3)));
        assertFalse(c.nearGold(p(1, 3)));
        assertFalse(c.nearGold(p(3, 1)));
    }

    @Test
    void isPit() {
        Cave c = Cave.pdfExample();
        assertTrue(c.isPit(p(2, 3)));
        assertFalse(c.isPit(p(2, 2)));
    }

    @Test
    void pitsCannotBeChangedFromOutside() {
        assertThrows(UnsupportedOperationException.class, () -> Cave.pdfExample().pits().add(p(1, 1)));
    }

    @Test
    void validCaveAccepted() {
        assertDoesNotThrow(() -> new Cave(4, p(1, 1), p(1, 2), Set.of()));
        assertDoesNotThrow(() -> new Cave(Cave.MAX_SIZE, p(1, 1), p(2, 1), Set.of(p(5, 5))));
    }

    @Test
    void badCavesRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Cave(Cave.MIN_SIZE - 1, p(1, 1), p(1, 2), Set.of()));
        assertThrows(IllegalArgumentException.class, () -> new Cave(Cave.MAX_SIZE + 1, p(1, 1), p(1, 2), Set.of()));
        assertThrows(IllegalArgumentException.class, () -> new Cave(4, p(0, 1), p(1, 1), Set.of()), "Вампус вне поля");
        assertThrows(IllegalArgumentException.class, () -> new Cave(4, p(1, 4), p(1, 5), Set.of()), "золото вне поля, хотя рядом с Вампусом");
        assertThrows(IllegalArgumentException.class, () -> new Cave(4, p(1, 1), p(1, 2), Set.of(p(9, 9))), "яма вне поля");
        assertThrows(IllegalArgumentException.class, () -> new Cave(4, p(4, 1), p(3, 1), Set.of()), "Вампус на старте");
        assertThrows(IllegalArgumentException.class, () -> new Cave(4, p(1, 1), p(1, 3), Set.of()), "золото не рядом с Вампусом");
        assertThrows(IllegalArgumentException.class, () -> new Cave(4, p(1, 1), p(1, 1), Set.of()), "золото в комнате Вампуса");
        assertThrows(IllegalArgumentException.class, () -> new Cave(4, p(1, 1), p(1, 2), Set.of(p(4, 1))), "яма на старте");
        assertThrows(IllegalArgumentException.class, () -> new Cave(4, p(1, 1), p(1, 2), Set.of(p(1, 1))), "яма под Вампусом");
        assertThrows(IllegalArgumentException.class, () -> new Cave(4, p(1, 1), p(1, 2), Set.of(p(1, 2))), "золото в яме");
        assertThrows(NullPointerException.class, () -> new Cave(4, null, p(1, 2), Set.of()));
    }
}
