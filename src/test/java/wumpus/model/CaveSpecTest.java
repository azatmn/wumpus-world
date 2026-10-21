package wumpus.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CaveSpecTest {

    private static List<String> fields(int size, int pitPercent) {
        InvalidSettingsException e = assertThrows(InvalidSettingsException.class,
                () -> new CaveSpec(size, pitPercent, 1L));
        return e.problems().stream().map(InvalidSettingsException.Problem::field).toList();
    }

    @ParameterizedTest
    @CsvSource({"4, 0", "12, 40", "8, 20"})
    void boundaryValuesAllowed(int size, int pitPercent) {
        CaveSpec s = new CaveSpec(size, pitPercent, 7L);
        assertEquals(size, s.size());
        assertEquals(pitPercent, s.pitPercent());
        assertEquals(7L, s.seed());
    }

    @Test
    void badValuesNameTheirFields() {
        assertEquals(List.of("size"), fields(3, 20));
        assertEquals(List.of("size"), fields(13, 20));
        assertEquals(List.of("pitPercent"), fields(4, -1));
        assertEquals(List.of("pitPercent"), fields(4, 41));
        assertEquals(List.of("size", "pitPercent"), fields(0, 100));
    }

    @ParameterizedTest
    @CsvSource({"4, 20, 3", "4, 0, 0", "10, 20, 20", "12, 40, 58", "5, 10, 3"})
    void pitCountProportionalToRooms(int size, int pitPercent, int expected) {
        assertEquals(expected, new CaveSpec(size, pitPercent, 1L).pitCount());
    }

    @Test
    void defaultIsReasonable() {
        CaveSpec d = CaveSpec.defaults();
        assertEquals(20, d.pitPercent());
        assertTrue(d.size() >= 4 && d.size() <= Cave.MAX_SIZE);
    }
}
