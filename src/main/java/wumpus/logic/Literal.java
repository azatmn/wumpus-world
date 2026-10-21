package wumpus.logic;

import java.util.Objects;

/** Факт или его отрицание: P(3,2) или ¬P(3,2). */
public record Literal(Fact fact, boolean positive) {

    public Literal {
        Objects.requireNonNull(fact);
    }

    public static Literal of(Fact fact) {
        return new Literal(fact, true);
    }

    public static Literal not(Fact fact) {
        return new Literal(fact, false);
    }

    public Literal negate() {
        return new Literal(fact, !positive);
    }

    @Override
    public String toString() {
        return (positive ? "" : "¬") + fact;
    }
}
