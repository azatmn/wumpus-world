package wumpus.controller;

import wumpus.util.SafeText;

import java.util.regex.Pattern;

/**
 * Поле формы: ключ (имя параметра), подпись для человека и вид значения.
 * Умеет прочитать текст из поля ввода и превратить значение обратно в текст.
 */
public record FormField(String key, String label, Kind kind) {

    public enum Kind {
        INTEGER,
        LONG
    }

    /** Только ASCII-цифры, необязательный знак: «５» или «٣» числом не считаются. */
    private static final Pattern INTEGER = Pattern.compile("[+-]?[0-9]+");
    private static final int ECHO_LIMIT = 20;

    /** Прочитанное значение или текст ошибки. */
    public sealed interface Read {
        record Value(Object value) implements Read {
        }

        record Error(String message) implements Read {
        }
    }

    public Read read(String raw) {
        String text = raw == null ? "" : raw.strip();
        if (text.isEmpty()) {
            return error("не заполнено");
        }
        String echo = "\"" + SafeText.preview(text, ECHO_LIMIT) + "\"";
        if (!INTEGER.matcher(text).matches()) {
            return error("нужно целое число, а указано " + echo);
        }
        try {
            return new Read.Value(kind == Kind.LONG ? (Object) Long.parseLong(text) : Integer.parseInt(text));
        } catch (NumberFormatException e) {
            return error("слишком большое число " + echo);
        }
    }

    /** Значение как текст для поля ввода. */
    public static String text(Object value) {
        return String.valueOf(value);
    }

    private Read error(String what) {
        return new Read.Error(label + ": " + what);
    }
}
