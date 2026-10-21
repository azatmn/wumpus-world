package wumpus.agent;

import wumpus.model.Action;
import wumpus.model.Direction;
import wumpus.model.Position;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Маршруты охотника: поиск в ширину по разрешённым комнатам и перевод маршрута в повороты и шаги. */
public final class Planner {

    private Planner() {
    }

    /**
     * Кратчайший путь от from до to; промежуточные комнаты — только из allowed, сама to может быть любой.
     *
     * @return комнаты пути без from (пустой список, если from = to) или null, если пути нет
     */
    public static List<Position> path(Position from, Position to, Set<Position> allowed, int size) {
        if (from.equals(to)) {
            return List.of();
        }
        Map<Position, Position> cameFrom = new HashMap<>();
        Deque<Position> queue = new ArrayDeque<>();
        cameFrom.put(from, from);
        queue.add(from);
        while (!queue.isEmpty()) {
            Position p = queue.poll();
            for (Position n : p.neighbors(size)) {
                if (cameFrom.containsKey(n)) {
                    continue;
                }
                if (n.equals(to)) {
                    cameFrom.put(n, p);
                    return unwind(cameFrom, from, to);
                }
                if (allowed.contains(n)) {
                    cameFrom.put(n, p);
                    queue.add(n);
                }
            }
        }
        return null;
    }

    private static List<Position> unwind(Map<Position, Position> cameFrom, Position from, Position to) {
        List<Position> result = new ArrayList<>();
        for (Position p = to; !p.equals(from); p = cameFrom.get(p)) {
            result.add(p);
        }
        Collections.reverse(result);
        return result;
    }

    /**
     * Самая дешёвая по числу действий цель из targets: путь к ней идёт только по allowed.
     *
     * @return путь к выбранной цели или null, если ни одна не достижима
     */
    public static List<Position> cheapest(Position from, Direction facing, List<Position> targets,
                                          Set<Position> allowed, int size) {
        List<Position> best = null;
        int bestCost = Integer.MAX_VALUE;
        for (Position t : targets) {
            List<Position> p = path(from, t, allowed, size);
            if (p == null) {
                continue;
            }
            int cost = walk(from, facing, p).size();
            if (cost < bestCost) {
                best = p;
                bestCost = cost;
            }
        }
        return best;
    }

    /** Повороты, чтобы смотреть из from в to: меньшим числом поворотов. */
    public static List<Action> face(Direction from, Direction to) {
        int turns = (to.ordinal() - from.ordinal() + 4) % 4;
        return switch (turns) {
            case 0 -> List.of();
            case 1 -> List.of(Action.TURN_RIGHT);
            case 2 -> List.of(Action.TURN_RIGHT, Action.TURN_RIGHT);
            default -> List.of(Action.TURN_LEFT);
        };
    }

    /** Действия, чтобы пройти путь: перед каждым шагом — поворот в нужную сторону. */
    public static List<Action> walk(Position from, Direction facing, List<Position> path) {
        List<Action> result = new ArrayList<>();
        Position at = from;
        Direction looking = facing;
        for (Position next : path) {
            Direction d = direction(at, next);
            result.addAll(face(looking, d));
            result.add(Action.FORWARD);
            looking = d;
            at = next;
        }
        return result;
    }

    /** Куда охотник смотрит, пройдя путь. */
    public static Direction facingAfter(Position from, Direction facing, List<Position> path) {
        if (path.isEmpty()) {
            return facing;
        }
        Position before = path.size() > 1 ? path.get(path.size() - 2) : from;
        return direction(before, path.getLast());
    }

    /** Направление от комнаты к соседней. */
    public static Direction direction(Position from, Position to) {
        for (Direction d : Direction.values()) {
            if (from.step(d).equals(to)) {
                return d;
            }
        }
        throw new IllegalArgumentException(to + " не соседняя с " + from);
    }
}
