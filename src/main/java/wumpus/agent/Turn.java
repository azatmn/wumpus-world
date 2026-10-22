package wumpus.agent;

import wumpus.model.Action;
import wumpus.model.Percept;
import wumpus.model.Position;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Один шаг вывода для журнала, как в задании: измерение → вывод → решение.
 *
 * @param actions действия, которыми решение выполняется: повороты, шаги, выстрел
 * @param risky   решение принято без доказанно безопасного хода
 */
public record Turn(int number, Position at, Percept percept, List<String> conclusions, String decision,
                   List<Action> actions, boolean risky) {

    public Turn {
        conclusions = List.copyOf(conclusions);
        actions = List.copyOf(actions);
    }

    @Override
    public String toString() {
        return "Шаг " + number + " · (" + at + ") · измерение " + percept
                + " · вывод: " + (conclusions.isEmpty() ? "нового нет" : String.join("; ", conclusions))
                + " · решение: " + decision
                + " · действия: " + actions.stream().map(Action::title).collect(Collectors.joining(", "));
    }
}
