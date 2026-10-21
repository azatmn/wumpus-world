package wumpus.logic;

import java.util.List;
import java.util.stream.Collectors;

/** Предложение базы знаний: литералы через ИЛИ. Верно, если верен хотя бы один литерал. */
public record Clause(List<Literal> literals) {

    public Clause {
        literals = List.copyOf(literals);
        if (literals.isEmpty()) {
            throw new IllegalArgumentException("Пустое предложение всегда ложно");
        }
    }

    public Clause(Literal... literals) {
        this(List.of(literals));
    }

    @Override
    public String toString() {
        return literals.stream().map(Literal::toString).collect(Collectors.joining(" ∨ "));
    }
}
