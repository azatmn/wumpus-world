package wumpus.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PerceptTest {

    @Test
    void printedInTaskOrder() {
        assertEquals("(none, none, none, none, none)", Percept.NONE.toString());
        assertEquals("(none, Breeze, none, none, none)", new Percept(false, true, false, false, false).toString());
        assertEquals("(Stench, Breeze, Glitter, Bump, Scream)", new Percept(true, true, true, true, true).toString());
        assertEquals("(Stench, none, none, none, Scream)", new Percept(true, false, false, false, true).toString());
    }
}
