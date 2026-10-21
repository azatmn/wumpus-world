package wumpus.agent;

import wumpus.logic.Fact;
import wumpus.logic.Kind;
import wumpus.logic.KnowledgeBase;
import wumpus.logic.Rules;
import wumpus.logic.Status;
import wumpus.model.Action;
import wumpus.model.Direction;
import wumpus.model.Percept;
import wumpus.model.Position;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Охотник — агент, основанный на знаниях. Карты не видит; знает правила, свои координаты и куда смотрит.
 * Каждое новое измерение записывает в базу знаний (TELL), спрашивает её о комнатах (ASK) и решает, что делать.
 * <p>
 * Порядок целей: с золотом — к выходу; Вампус убит и золото найдено — за золотом; Вампус найден — застрелить;
 * иначе — в ближайшую безопасную комнату. Безопасных нет — выстрел наугад, затем риск, затем выход.
 */
public final class Hunter {

    private final int size;
    private final boolean takeRisks;
    private final Position start;
    private final KnowledgeBase kb;
    private Position position;
    private Direction facing = Direction.RIGHT;
    private boolean hasArrow = true;
    private boolean wumpusDead;
    private boolean hasGold;
    private boolean finished;
    private Action last;
    private final Map<Position, Percept> visited = new LinkedHashMap<>();
    private final Deque<Action> plan = new ArrayDeque<>();
    private final List<Turn> journal = new ArrayList<>();
    private final List<String> pending = new ArrayList<>();
    private Map<Position, CellKnowledge> knowledge = new LinkedHashMap<>();
    /** Куда ведёт текущий план — для текста решения. */
    private Position walkTarget;
    private Direction shotDir;

    public Hunter(int size) {
        this(size, true);
    }

    /** @param takeRisks идти в комнату с недоказанной опасностью, когда безопасных не осталось; иначе — к выходу */
    public Hunter(int size, boolean takeRisks) {
        this.size = size;
        this.takeRisks = takeRisks;
        this.kb = new KnowledgeBase(size);
        this.start = new Position(size, 1);
        this.position = start;
        refresh();
    }

    /** Получить измерение после прошлого действия и выбрать следующее. */
    public Action next(Percept percept) {
        if (finished) {
            throw new IllegalStateException("Охотник уже выбрался из пещеры");
        }
        observe(percept);
        if (plan.isEmpty()) {
            decide(percept);
        }
        Action action = plan.poll();
        switch (action) {
            case TURN_LEFT -> facing = facing.left();
            case TURN_RIGHT -> facing = facing.right();
            case CLIMB -> finished = true;
            default -> {
            }
        }
        last = action;
        return action;
    }

    /** Последствия прошлого действия и новое измерение — в базу знаний. */
    private void observe(Percept percept) {
        if (last == Action.FORWARD && !percept.bump()) {
            position = position.step(facing);
        }
        if (percept.bump()) {
            plan.clear();
        }
        if (last == Action.SHOOT) {
            hasArrow = false;
            kb.tell(Rules.arrow(position, facing, percept.scream(), size));
            if (percept.scream()) {
                wumpusDead = true;
                pending.add("крик — Вампус убит");
            } else {
                pending.add("крика нет — Вампуса " + facing.title() + " нет");
            }
        }
        if (last == Action.GRAB && !percept.glitter()) {
            hasGold = true;
            pending.add("золото у охотника");
        }
        if (!visited.containsKey(position)) {
            visited.put(position, percept);
            kb.tell(Rules.alive(position, !wumpusDead));
            kb.tell(Rules.stench(position, percept.stench(), size));
            kb.tell(Rules.breeze(position, percept.breeze(), size));
            if (!hasGold) {
                kb.tell(Rules.glitter(position, percept.glitter(), size));
            }
        }
    }

    private void decide(Percept percept) {
        Map<Position, CellKnowledge> before = knowledge;
        refresh();
        List<String> conclusions = new ArrayList<>(pending);
        pending.clear();
        conclusions.addAll(conclusions(before, percept));
        String decision;
        boolean risky = false;
        Position gold = located(Kind.GOLD);
        Position wumpus = located(Kind.WUMPUS);
        if (hasGold) {
            decision = goTo(start, "золото у меня — возвращаюсь к выходу");
            plan.add(Action.CLIMB);
        } else if (wumpusDead && gold != null && pathTo(gold) != null) {
            decision = position.equals(gold) ? "беру золото" : goTo(gold, "иду за золотом в " + gold);
            plan.add(Action.GRAB);
        } else if (!wumpusDead && !hasArrow) {
            decision = goTo(start, "стрелы нет, а Вампус жив — золото не взять, возвращаюсь к выходу");
            plan.add(Action.CLIMB);
        } else if (!wumpusDead && wumpus != null && planShot(List.of(wumpus))) {
            decision = "Вампус в " + wumpus + " — стреляю " + shotDirection();
        } else if (planExplore()) {
            decision = "иду в " + target();
        } else if (takeRisks && !wumpusDead && planShot(maybe(Kind.WUMPUS))) {
            decision = "безопасных ходов нет — стреляю " + shotDirection() + " наугад";
            risky = true;
        } else if (takeRisks && planRisk()) {
            decision = "безопасных ходов нет — рискую: иду в " + target();
            risky = true;
        } else {
            decision = goTo(start, "идти некуда — возвращаюсь к выходу без золота");
            plan.add(Action.CLIMB);
        }
        journal.add(new Turn(journal.size() + 1, position, percept, conclusions, decision, risky));
    }

    /** Пересчитать знания о всех комнатах: один запрос к базе на все факты. */
    private void refresh() {
        List<Fact> facts = new ArrayList<>();
        for (Position c : cells()) {
            for (Kind kind : Kind.values()) {
                facts.add(new Fact(kind, c));
            }
        }
        Map<Fact, Status> s = kb.statuses(facts);
        Map<Position, CellKnowledge> result = new LinkedHashMap<>();
        for (Position c : cells()) {
            Status pit = s.get(new Fact(Kind.PIT, c));
            Status w = s.get(new Fact(Kind.WUMPUS, c));
            Status g = s.get(new Fact(Kind.GOLD, c));
            boolean safe = pit == Status.NO && (w == Status.NO || wumpusDead);
            result.put(c, new CellKnowledge(c, visited.get(c), pit, w, g, safe));
        }
        knowledge = result;
    }

    /** Что нового выведено с прошлого шага — словами, как в задании. */
    private List<String> conclusions(Map<Position, CellKnowledge> before, Percept percept) {
        List<String> result = new ArrayList<>();
        List<Position> newlySafe = new ArrayList<>();
        for (CellKnowledge k : knowledge.values()) {
            CellKnowledge was = before.get(k.cell());
            if (k.safe() && !k.visited() && !was.safe()) {
                newlySafe.add(k.cell());
            }
        }
        if (!newlySafe.isEmpty()) {
            result.add("безопасны: " + list(newlySafe));
        }
        for (Kind kind : Kind.values()) {
            for (CellKnowledge k : knowledge.values()) {
                if (status(k, kind) == Status.YES && status(before.get(k.cell()), kind) != Status.YES) {
                    result.add(kind.title() + " в " + k.cell());
                }
            }
        }
        if (percept.breeze()) {
            sensed(result, "рядом яма, может быть в ", Kind.PIT);
        }
        if (percept.stench() && !wumpusDead) {
            sensed(result, "рядом Вампус, может быть в ", Kind.WUMPUS);
        }
        if (percept.glitter() && !hasGold) {
            sensed(result, "рядом золото, может быть в ", Kind.GOLD);
        }
        return result;
    }

    private void sensed(List<String> result, String text, Kind kind) {
        List<Position> around = new ArrayList<>();
        around.add(position);
        around.addAll(position.neighbors(size));
        List<Position> maybe = around.stream().filter(c -> status(knowledge.get(c), kind) == Status.MAYBE).toList();
        if (!maybe.isEmpty()) {
            result.add(text + list(maybe));
        }
    }

    private static Status status(CellKnowledge k, Kind kind) {
        return switch (kind) {
            case PIT -> k.pit();
            case WUMPUS -> k.wumpus();
            case GOLD -> k.gold();
        };
    }

    /** Комната, где факт точно верен, или null. */
    private Position located(Kind kind) {
        for (CellKnowledge k : knowledge.values()) {
            if (status(k, kind) == Status.YES) {
                return k.cell();
            }
        }
        return null;
    }

    private List<Position> maybe(Kind kind) {
        return knowledge.values().stream().filter(k -> status(k, kind) == Status.MAYBE).map(CellKnowledge::cell).toList();
    }

    private List<Position> pathTo(Position to) {
        return Planner.path(position, to, visited.keySet(), size);
    }

    /** Дойти по посещённым комнатам; текст решения — если идти не надо, то «здесь». */
    private String goTo(Position to, String text) {
        plan.addAll(Planner.walk(position, facing, pathTo(to)));
        return text;
    }

    /** Ближайшая безопасная непосещённая комната. */
    private boolean planExplore() {
        List<Position> targets = knowledge.values().stream()
                .filter(k -> k.safe() && !k.visited()).map(CellKnowledge::cell).toList();
        return planWalk(targets);
    }

    /** Наименее опасная непосещённая комната рядом с посещёнными: где опасность не доказана. */
    private boolean planRisk() {
        for (int risk = 1; risk <= 2; risk++) {
            List<Position> targets = new ArrayList<>();
            for (CellKnowledge k : knowledge.values()) {
                if (k.visited() || k.pit() == Status.YES || (!wumpusDead && k.wumpus() == Status.YES)) {
                    continue;
                }
                int unknown = (k.pit() == Status.MAYBE ? 1 : 0) + (!wumpusDead && k.wumpus() == Status.MAYBE ? 1 : 0);
                if (unknown == risk) {
                    targets.add(k.cell());
                }
            }
            if (planWalk(targets)) {
                return true;
            }
        }
        return false;
    }

    private boolean planWalk(List<Position> targets) {
        List<Position> path = Planner.cheapest(position, facing, targets, visited.keySet(), size);
        if (path == null || path.isEmpty()) {
            return false;
        }
        walkTarget = path.getLast();
        plan.addAll(Planner.walk(position, facing, path));
        return true;
    }

    private String target() {
        return walkTarget.toString();
    }

    /**
     * Выстрел по одной из комнат targets: из ближайшей посещённой комнаты на одной линии с ней.
     * Из нескольких направлений — то, где на линии больше всего целей.
     */
    private boolean planShot(List<Position> targets) {
        if (targets.isEmpty()) {
            return false;
        }
        Set<Position> goals = new LinkedHashSet<>(targets);
        List<Position> spots = new ArrayList<>(visited.keySet());
        Position bestSpot = null;
        Direction bestDir = null;
        int bestHits = 0;
        int bestCost = Integer.MAX_VALUE;
        for (Position spot : spots) {
            List<Position> path = pathTo(spot);
            if (path == null) {
                continue;
            }
            for (Direction d : Direction.values()) {
                int hits = 0;
                for (Position p = spot.step(d); p.inside(size); p = p.step(d)) {
                    if (goals.contains(p)) {
                        hits++;
                    }
                }
                if (hits == 0) {
                    continue;
                }
                Direction arrive = Planner.facingAfter(position, facing, path);
                int cost = Planner.walk(position, facing, path).size() + Planner.face(arrive, d).size();
                if (hits > bestHits || (hits == bestHits && cost < bestCost)) {
                    bestSpot = spot;
                    bestDir = d;
                    bestHits = hits;
                    bestCost = cost;
                }
            }
        }
        if (bestSpot == null) {
            return false;
        }
        List<Position> path = pathTo(bestSpot);
        plan.addAll(Planner.walk(position, facing, path));
        plan.addAll(Planner.face(Planner.facingAfter(position, facing, path), bestDir));
        plan.add(Action.SHOOT);
        shotDir = bestDir;
        return true;
    }

    private String shotDirection() {
        return shotDir.title();
    }

    private List<Position> cells() {
        List<Position> result = new ArrayList<>(size * size);
        for (int row = 1; row <= size; row++) {
            for (int col = 1; col <= size; col++) {
                result.add(new Position(row, col));
            }
        }
        return result;
    }

    private static String list(List<Position> cells) {
        return cells.stream().map(Position::toString).collect(Collectors.joining(", "));
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

    public boolean wumpusDead() {
        return wumpusDead;
    }

    public boolean hasGold() {
        return hasGold;
    }

    public boolean finished() {
        return finished;
    }

    /** Где охотник точно знает Вампуса / золото; null — не знает. */
    public Position knownWumpus() {
        return located(Kind.WUMPUS);
    }

    public Position knownGold() {
        return located(Kind.GOLD);
    }

    public Map<Position, CellKnowledge> knowledge() {
        return Collections.unmodifiableMap(knowledge);
    }

    public List<Turn> journal() {
        return List.copyOf(journal);
    }

    public int clauseCount() {
        return kb.clauses().size();
    }
}
