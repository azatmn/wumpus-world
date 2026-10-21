package wumpus.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Случайная пещера по заказу. Старт и соседние с ним комнаты всегда чистые — иначе часть игр
 * заканчивалась бы на первом шаге; золото кладётся рядом с Вампусом, ямы — в оставшиеся комнаты.
 */
public final class CaveGenerator {

    private CaveGenerator() {
    }

    public static Cave generate(CaveSpec spec) {
        int size = spec.size();
        Random random = new Random(spec.seed());
        Position start = new Position(size, 1);
        Set<Position> startZone = new HashSet<>(start.neighbors(size));
        startZone.add(start);

        List<Position> free = new ArrayList<>();
        for (int row = 1; row <= size; row++) {
            for (int col = 1; col <= size; col++) {
                Position p = new Position(row, col);
                if (!startZone.contains(p)) {
                    free.add(p);
                }
            }
        }
        Position wumpus = free.get(random.nextInt(free.size()));
        List<Position> goldPlaces = wumpus.neighbors(size);
        Position gold = goldPlaces.get(random.nextInt(goldPlaces.size()));

        free.remove(wumpus);
        free.remove(gold);
        Collections.shuffle(free, random);
        Set<Position> pits = new HashSet<>(free.subList(0, spec.pitCount()));
        return new Cave(size, wumpus, gold, pits);
    }
}
