package wumpus.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Заказ на случайную пещеру. Проверяется при создании; обо всех проблемах сообщает сразу.
 *
 * @param pitPercent доля комнат с ямами, в процентах: ям тем больше, чем больше пещера
 * @param seed       зерно генератора: одинаковый seed — одинаковая пещера
 */
public record CaveSpec(int size, int pitPercent, long seed) {

    public static final int MIN_SIZE = 4;
    public static final int MAX_PIT_PERCENT = 40;

    public static CaveSpec defaults() {
        return new CaveSpec(6, 20, 1L);
    }

    public CaveSpec {
        List<InvalidSettingsException.Problem> problems = new ArrayList<>();
        if (size < MIN_SIZE || size > Cave.MAX_SIZE) {
            problems.add(new InvalidSettingsException.Problem("size",
                    "Размер пещеры: допустимо от " + MIN_SIZE + " до " + Cave.MAX_SIZE + ", указано " + size));
        }
        if (pitPercent < 0 || pitPercent > MAX_PIT_PERCENT) {
            problems.add(new InvalidSettingsException.Problem("pitPercent",
                    "Доля ям: допустимо от 0 до " + MAX_PIT_PERCENT + " %, указано " + pitPercent));
        }
        if (!problems.isEmpty()) {
            throw new InvalidSettingsException(problems);
        }
    }

    /** Сколько ям: доля от числа комнат N², с округлением. */
    public int pitCount() {
        return (int) Math.round(size * size * pitPercent / 100.0);
    }
}
