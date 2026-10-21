package wumpus.model;

import java.util.Objects;

/**
 * Среда: выполняет действия охотника по правилам задания и возвращает измерение датчиков.
 * Счёт: −1 за действие, −10 за выстрел, −1000 за смерть, +1000 за выход с золотом.
 */
public final class World {

    public static final int ACTION_COST = 1;
    public static final int ARROW_COST = 10;
    public static final int DEATH_PENALTY = 1000;
    public static final int GOLD_REWARD = 1000;

    private final Cave cave;
    private Position position;
    private Direction facing = Direction.UP;
    private boolean hasArrow = true;
    private boolean wumpusAlive = true;
    private boolean hasGold;
    private boolean alive = true;
    private boolean over;
    private int score;
    private int actions;
    private Percept percept;

    public World(Cave cave) {
        this.cave = Objects.requireNonNull(cave);
        this.position = cave.start();
        this.percept = sense(false, false);
    }

    /** Выполнить действие; вернуть измерение после него. */
    public Percept execute(Action action) {
        Objects.requireNonNull(action);
        if (over) {
            throw new IllegalStateException("Игра окончена");
        }
        actions++;
        score -= ACTION_COST;
        boolean bump = false;
        boolean scream = false;
        switch (action) {
            case FORWARD -> bump = forward();
            case TURN_LEFT -> facing = facing.left();
            case TURN_RIGHT -> facing = facing.right();
            case SHOOT -> scream = shoot();
            case GRAB -> grab();
            case CLIMB -> climb();
        }
        percept = sense(bump, scream);
        return percept;
    }

    /** true — удар о стену. */
    private boolean forward() {
        Position next = position.step(facing);
        if (!next.inside(cave.size())) {
            return true;
        }
        position = next;
        if (cave.isPit(position) || (wumpusAlive && cave.wumpus().equals(position))) {
            alive = false;
            over = true;
            score -= DEATH_PENALTY;
        }
        return false;
    }

    /** Стрела летит по прямой до стены. true — Вампус убит (крик). */
    private boolean shoot() {
        if (!hasArrow) {
            return false;
        }
        hasArrow = false;
        score -= ARROW_COST;
        for (Position p = position.step(facing); p.inside(cave.size()); p = p.step(facing)) {
            if (wumpusAlive && cave.wumpus().equals(p)) {
                wumpusAlive = false;
                return true;
            }
        }
        return false;
    }

    /** Золото берётся только после убийства Вампуса — так в задании. */
    private void grab() {
        if (!hasGold && !wumpusAlive && cave.gold().equals(position)) {
            hasGold = true;
        }
    }

    private void climb() {
        if (!position.equals(cave.start())) {
            return;
        }
        over = true;
        if (hasGold) {
            score += GOLD_REWARD;
        }
    }

    private Percept sense(boolean bump, boolean scream) {
        return new Percept(cave.hasStench(position), cave.hasBreeze(position),
                !hasGold && cave.nearGold(position), bump, scream);
    }

    public Cave cave() {
        return cave;
    }

    public Position position() {
        return position;
    }

    public Direction facing() {
        return facing;
    }

    public boolean hasArrow() {
        return hasArrow;
    }

    public boolean wumpusAlive() {
        return wumpusAlive;
    }

    public boolean hasGold() {
        return hasGold;
    }

    public boolean alive() {
        return alive;
    }

    public boolean over() {
        return over;
    }

    /** Победа: выбрался из пещеры с золотом. */
    public boolean won() {
        return over && alive && hasGold;
    }

    public int score() {
        return score;
    }

    public int actions() {
        return actions;
    }

    /** Последнее измерение. */
    public Percept percept() {
        return percept;
    }
}
