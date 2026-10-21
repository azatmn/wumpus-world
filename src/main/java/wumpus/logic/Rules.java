package wumpus.logic;

import wumpus.model.Direction;
import wumpus.model.Position;

import java.util.ArrayList;
import java.util.List;

/**
 * Правила пещеры в виде предложений базы знаний.
 * Правило «ветерок в комнате ⇔ яма в одной из соседних» при известном измерении превращается
 * в одно предложение (ветерок есть: P(сосед1) ∨ P(сосед2) ∨ ...) или в отрицания (ветерка нет: ¬P у каждого соседа).
 */
public final class Rules {

    private Rules() {
    }

    /** Ветерок: яма в соседней комнате. */
    public static List<Clause> breeze(Position at, boolean felt, int size) {
        return sensor(Kind.PIT, at.neighbors(size), felt);
    }

    /** Запах: Вампус в этой или соседней комнате. */
    public static List<Clause> stench(Position at, boolean felt, int size) {
        return sensor(Kind.WUMPUS, withSelf(at, size), felt);
    }

    /** Блеск: золото в этой или соседней комнате. */
    public static List<Clause> glitter(Position at, boolean felt, int size) {
        return sensor(Kind.GOLD, withSelf(at, size), felt);
    }

    /** Охотник жив в комнате: ямы здесь нет, и живого Вампуса тоже. */
    public static List<Clause> alive(Position at, boolean wumpusAlive) {
        List<Clause> result = new ArrayList<>();
        result.add(new Clause(Literal.not(new Fact(Kind.PIT, at))));
        if (wumpusAlive) {
            result.add(new Clause(Literal.not(new Fact(Kind.WUMPUS, at))));
        }
        return result;
    }

    /** Выстрел из комнаты from: крик — Вампус на линии полёта, тишина — на линии его нет. */
    public static List<Clause> arrow(Position from, Direction direction, boolean scream, int size) {
        List<Position> line = new ArrayList<>();
        for (Position p = from.step(direction); p.inside(size); p = p.step(direction)) {
            line.add(p);
        }
        if (line.isEmpty()) {
            return List.of();
        }
        return sensor(Kind.WUMPUS, line, scream);
    }

    /** Ровно один: хотя бы в одной комнате и ни в каких двух сразу. */
    public static List<Clause> exactlyOne(Kind kind, int size) {
        List<Position> cells = cells(size);
        List<Clause> result = new ArrayList<>();
        result.add(new Clause(cells.stream().map(c -> Literal.of(new Fact(kind, c))).toList()));
        for (int i = 0; i < cells.size(); i++) {
            for (int j = i + 1; j < cells.size(); j++) {
                result.add(new Clause(Literal.not(new Fact(kind, cells.get(i))),
                        Literal.not(new Fact(kind, cells.get(j)))));
            }
        }
        return result;
    }

    /** Вампус сторожит золото: золото в комнате ⇒ Вампус в одной из соседних. */
    public static List<Clause> goldNextToWumpus(int size) {
        List<Clause> result = new ArrayList<>();
        for (Position c : cells(size)) {
            List<Literal> literals = new ArrayList<>();
            literals.add(Literal.not(new Fact(Kind.GOLD, c)));
            for (Position n : c.neighbors(size)) {
                literals.add(Literal.of(new Fact(Kind.WUMPUS, n)));
            }
            result.add(new Clause(literals));
        }
        return result;
    }

    /** Ни Вампус, ни золото не лежат в яме. */
    public static List<Clause> notInPit(int size) {
        List<Clause> result = new ArrayList<>();
        for (Position c : cells(size)) {
            Literal noPit = Literal.not(new Fact(Kind.PIT, c));
            result.add(new Clause(Literal.not(new Fact(Kind.WUMPUS, c)), noPit));
            result.add(new Clause(Literal.not(new Fact(Kind.GOLD, c)), noPit));
        }
        return result;
    }

    private static List<Clause> sensor(Kind kind, List<Position> cells, boolean felt) {
        if (felt) {
            return List.of(new Clause(cells.stream().map(c -> Literal.of(new Fact(kind, c))).toList()));
        }
        return cells.stream().map(c -> new Clause(Literal.not(new Fact(kind, c)))).toList();
    }

    private static List<Position> withSelf(Position at, int size) {
        List<Position> result = new ArrayList<>();
        result.add(at);
        result.addAll(at.neighbors(size));
        return result;
    }

    /** Все комнаты построчно. */
    static List<Position> cells(int size) {
        List<Position> result = new ArrayList<>(size * size);
        for (int row = 1; row <= size; row++) {
            for (int col = 1; col <= size; col++) {
                result.add(new Position(row, col));
            }
        }
        return result;
    }
}
