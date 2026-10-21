package wumpus.agent;

import wumpus.model.Percept;
import wumpus.model.Position;

import java.util.List;

/**
 * Один шаг вывода для журнала, как в задании: измерение → вывод → решение.
 *
 * @param risky решение принято без доказанно безопасного хода
 */
public record Turn(int number, Position at, Percept percept, List<String> conclusions, String decision, boolean risky) {

    public Turn {
        conclusions = List.copyOf(conclusions);
    }

    @Override
    public String toString() {
        return "Шаг " + number + " · (" + at + ") · измерение " + percept
                + " · вывод: " + (conclusions.isEmpty() ? "нового нет" : String.join("; ", conclusions))
                + " · решение: " + decision;
    }
}
