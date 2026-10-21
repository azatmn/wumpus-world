package wumpus.model;

import java.util.Objects;
import java.util.Set;

/**
 * Расстановка в пещере N×N: Вампус, золото, ямы. Не меняется; что убито и что взято, хранит {@link World}.
 * Охотник стартует в левом нижнем углу (N,1).
 */
public final class Cave {

    public static final int MIN_SIZE = 2;
    public static final int MAX_SIZE = 12;

    private final int size;
    private final Position wumpus;
    private final Position gold;
    private final Set<Position> pits;

    public Cave(int size, Position wumpus, Position gold, Set<Position> pits) {
        if (size < MIN_SIZE || size > MAX_SIZE) {
            throw new IllegalArgumentException("Размер пещеры от " + MIN_SIZE + " до " + MAX_SIZE + ", указано " + size);
        }
        this.size = size;
        this.wumpus = Objects.requireNonNull(wumpus);
        this.gold = Objects.requireNonNull(gold);
        this.pits = Set.copyOf(pits);
        Position start = start();
        requireInside(wumpus, "Вампус");
        requireInside(gold, "золото");
        for (Position pit : this.pits) {
            requireInside(pit, "яма");
        }
        if (wumpus.equals(start)) {
            throw new IllegalArgumentException("Вампус не может быть на старте");
        }
        if (!wumpus.isNeighbor(gold)) {
            throw new IllegalArgumentException("Золото должно лежать в соседней с Вампусом комнате");
        }
        if (this.pits.contains(start) || this.pits.contains(wumpus) || this.pits.contains(gold)) {
            throw new IllegalArgumentException("Яма не может быть на старте, под Вампусом или под золотом");
        }
    }

    /** Пещера 4×4 с картинки задания. */
    public static Cave pdfExample() {
        return new Cave(4, new Position(2, 1), new Position(2, 2),
                Set.of(new Position(1, 4), new Position(2, 3), new Position(4, 3)));
    }

    private void requireInside(Position p, String what) {
        if (!p.inside(size)) {
            throw new IllegalArgumentException(what + " вне пещеры: " + p);
        }
    }

    public int size() {
        return size;
    }

    public Position start() {
        return new Position(size, 1);
    }

    public Position wumpus() {
        return wumpus;
    }

    public Position gold() {
        return gold;
    }

    public Set<Position> pits() {
        return pits;
    }

    public boolean isPit(Position p) {
        return pits.contains(p);
    }

    /** Ветерок: яма в соседней комнате. */
    public boolean hasBreeze(Position p) {
        for (Position n : p.neighbors(size)) {
            if (pits.contains(n)) {
                return true;
            }
        }
        return false;
    }

    /** Запах: Вампус (живой или мёртвый) здесь или в соседней комнате. */
    public boolean hasStench(Position p) {
        return wumpus.equals(p) || wumpus.isNeighbor(p);
    }

    /** Золото здесь или в соседней комнате — там охотник видит блеск, пока золото не взято. */
    public boolean nearGold(Position p) {
        return gold.equals(p) || gold.isNeighbor(p);
    }
}
