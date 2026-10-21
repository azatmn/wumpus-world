package wumpus.logic;

import wumpus.model.Position;

import java.util.Objects;

/** Высказывание «в комнате cell есть kind», например P(3,2) — «в 3,2 яма». */
public record Fact(Kind kind, Position cell) {

    public Fact {
        Objects.requireNonNull(kind);
        Objects.requireNonNull(cell);
    }

    @Override
    public String toString() {
        return kind.letter() + "(" + cell + ")";
    }
}
