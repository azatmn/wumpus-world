package wumpus.controller;

import wumpus.model.CaveSpec;

import java.util.List;
import java.util.Map;

import static wumpus.controller.FormField.Kind.INTEGER;
import static wumpus.controller.FormField.Kind.LONG;

/** Форма «Новая пещера» без JavaFX: тексты полей → {@link CaveSpec} или список проблем. */
public final class CaveForm {

    /** Поля в том же порядке, что и части {@link CaveSpec}. */
    public static final List<FormField> FIELDS = List.of(
            new FormField("size", "Размер N (пещера N×N)", INTEGER),
            new FormField("pitPercent", "Доля ям, %", INTEGER),
            new FormField("seed", "Зерно случайности (seed)", LONG));

    private CaveForm() {
    }

    public static Map<String, String> toTexts(CaveSpec s) {
        return Forms.texts(FIELDS, s.size(), s.pitPercent(), s.seed());
    }

    public static FormResult<CaveSpec> parse(Map<String, String> texts) {
        return Forms.parse(FIELDS, texts, v -> new CaveSpec((int) v[0], (int) v[1], (long) v[2]));
    }
}
