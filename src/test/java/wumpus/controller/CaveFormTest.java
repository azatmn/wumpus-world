package wumpus.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import wumpus.model.CaveSpec;
import wumpus.model.InvalidSettingsException;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CaveFormTest {

    private static final CaveSpec SPEC = new CaveSpec(8, 25, 42L);

    private static Map<String, String> with(String key, String text) {
        Map<String, String> texts = new HashMap<>(CaveForm.toTexts(SPEC));
        texts.put(key, text);
        return texts;
    }

    private static List<InvalidSettingsException.Problem> problems(Map<String, String> texts) {
        if (CaveForm.parse(texts) instanceof FormResult.Invalid<CaveSpec> invalid) {
            return invalid.problems();
        }
        return fail("форма должна быть отвергнута");
    }

    private static List<String> fields(Map<String, String> texts) {
        return problems(texts).stream().map(InvalidSettingsException.Problem::field).toList();
    }

    @Test
    void formHasFieldForEveryPartOfSpec() {
        List<String> components = Arrays.stream(CaveSpec.class.getRecordComponents()).map(RecordComponent::getName).toList();
        assertEquals(components, CaveForm.FIELDS.stream().map(FormField::key).toList());
        for (FormField f : CaveForm.FIELDS) {
            assertNotEquals(f.key(), f.label());
        }
    }

    @Test
    void roundTrip() {
        assertEquals(new FormResult.Valid<>(SPEC), CaveForm.parse(CaveForm.toTexts(SPEC)));
        CaveSpec odd = new CaveSpec(12, 0, Long.MIN_VALUE);
        assertEquals(new FormResult.Valid<>(odd), CaveForm.parse(CaveForm.toTexts(odd)));
    }

    @Test
    void spacesAndPlusAccepted() {
        assertEquals(new FormResult.Valid<>(new CaveSpec(5, 25, 42L)), CaveForm.parse(with("size", "  +5 ")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "abc", "5.5", "5,0", "1e3", "NaN", "Infinity", "٣", "５", "10%", "--5", "+"})
    void notAnIntegerReported(String text) {
        assertEquals(List.of("size"), fields(with("size", text)));
    }

    @Test
    void missingFieldReported() {
        Map<String, String> texts = new HashMap<>(CaveForm.toTexts(SPEC));
        texts.remove("seed");
        assertEquals(List.of("seed"), fields(texts));
        assertTrue(problems(texts).getFirst().message().contains("не заполнено"));
    }

    @Test
    void hugeNumbersReported() {
        assertEquals(List.of("pitPercent"), fields(with("pitPercent", "99999999999")));
        assertEquals(List.of("seed"), fields(with("seed", "99999999999999999999")));
        assertTrue(problems(with("seed", "99999999999999999999")).getFirst().message().contains("слишком большое"));
        assertEquals(new FormResult.Valid<>(new CaveSpec(8, 25, Long.MAX_VALUE)),
                CaveForm.parse(with("seed", Long.toString(Long.MAX_VALUE))));
    }

    @Test
    void specRulesReported() {
        assertEquals(List.of("size"), fields(with("size", "3")));
        assertEquals(List.of("pitPercent"), fields(with("pitPercent", "41")));
    }

    @Test
    void unreadableFieldsFirstThenSpecRules() {
        Map<String, String> t = with("size", "100");
        t.put("seed", "x");
        assertEquals(List.of("seed"), fields(t), "сначала — что не прочиталось; правила пещеры — потом");
    }

    @Test
    void messagesUseLabelAndSafeEcho() {
        String msg = problems(with("size", "a\u0000b" + "x".repeat(50))).getFirst().message();
        assertTrue(msg.startsWith("Размер N"), msg);
        assertTrue(msg.contains("a?b"), msg);
        assertTrue(msg.contains("..."), msg);
        assertFalse(msg.contains("\u0000"));
    }

    @Test
    void neverThrows() {
        for (FormField f : CaveForm.FIELDS) {
            for (String text : new String[]{null, "\uD800", "9".repeat(5000), "-", "\u0000"}) {
                Map<String, String> texts = with(f.key(), text);
                assertDoesNotThrow(() -> CaveForm.parse(texts));
            }
        }
    }
}
