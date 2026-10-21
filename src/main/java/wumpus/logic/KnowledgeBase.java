package wumpus.logic;

import wumpus.model.Cave;
import wumpus.model.Position;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * База знаний охотника: набор предложений. TELL — {@link #tell}, ASK — {@link #entails} и {@link #status}.
 * <p>
 * Факт выводим (KB ⊨ α), если «база знаний и не α» невыполнимо: ни одна расстановка, согласная с базой,
 * не делает α ложным. Выполнимость проверяет {@link Dpll}.
 * С самого начала в базе — правила мира: Вампус и золото ровно одни, золото рядом с Вампусом, оба не в яме.
 */
public final class KnowledgeBase {

    private final int size;
    private final List<Clause> clauses = new ArrayList<>();
    private final List<int[]> encoded = new ArrayList<>();
    /** Точные выводы не меняются: база только растёт. */
    private final Map<Fact, Status> settled = new HashMap<>();
    private int solverCalls;

    public KnowledgeBase(int size) {
        if (size < Cave.MIN_SIZE || size > Cave.MAX_SIZE) {
            throw new IllegalArgumentException("Размер пещеры от " + Cave.MIN_SIZE + " до " + Cave.MAX_SIZE + ", указано " + size);
        }
        this.size = size;
        tell(Rules.exactlyOne(Kind.WUMPUS, size));
        tell(Rules.exactlyOne(Kind.GOLD, size));
        tell(Rules.goldNextToWumpus(size));
        tell(Rules.notInPit(size));
    }

    public int size() {
        return size;
    }

    public void tell(Clause clause) {
        int[] c = new int[clause.literals().size()];
        for (int i = 0; i < c.length; i++) {
            c[i] = encode(clause.literals().get(i));
        }
        clauses.add(clause);
        encoded.add(c);
    }

    public void tell(Collection<Clause> list) {
        for (Clause c : list) {
            tell(c);
        }
    }

    public List<Clause> clauses() {
        return List.copyOf(clauses);
    }

    /** Следует ли литерал из базы: база вместе с его отрицанием невыполнима. */
    public boolean entails(Literal literal) {
        return solveWith(-encode(literal)) == null;
    }

    public Status status(Fact fact) {
        return statuses(List.of(fact)).get(fact);
    }

    /**
     * Статус каждого факта. Найденная расстановка показывает возможные значения сразу многих фактов,
     * поэтому DPLL запускается только для тех, чьё значение ещё ни разу не встретилось.
     */
    public Map<Fact, Status> statuses(Collection<Fact> facts) {
        Map<Fact, Status> result = new LinkedHashMap<>();
        List<boolean[]> models = new ArrayList<>();
        for (Fact fact : facts) {
            int v = variable(fact);
            Status known = settled.get(fact);
            if (known != null) {
                result.put(fact, known);
                continue;
            }
            boolean canBeTrue = possible(v, models);
            boolean canBeFalse = possible(-v, models);
            if (!canBeTrue && !canBeFalse) {
                throw new IllegalStateException("База знаний противоречива");
            }
            Status s = canBeTrue && canBeFalse ? Status.MAYBE : canBeTrue ? Status.YES : Status.NO;
            if (s != Status.MAYBE) {
                settled.put(fact, s);
            }
            result.put(fact, s);
        }
        return result;
    }

    /** Есть ли расстановка с литералом lit: сначала среди найденных, иначе спросить DPLL. */
    private boolean possible(int lit, List<boolean[]> models) {
        for (boolean[] m : models) {
            if (m[Math.abs(lit)] == lit > 0) {
                return true;
            }
        }
        boolean[] m = solveWith(lit);
        if (m == null) {
            return false;
        }
        models.add(m);
        return true;
    }

    private boolean[] solveWith(int lit) {
        solverCalls++;
        List<int[]> formula = new ArrayList<>(encoded.size() + 1);
        formula.addAll(encoded);
        formula.add(new int[]{lit});
        return Dpll.solve(3 * size * size, formula);
    }

    /** Сколько раз запускался DPLL — для проверки, что найденные расстановки переиспользуются. */
    int solverCalls() {
        return solverCalls;
    }

    private int encode(Literal literal) {
        int v = variable(literal.fact());
        return literal.positive() ? v : -v;
    }

    /** Номер переменной: сначала все ямы, потом все Вампусы, потом всё золото. */
    private int variable(Fact fact) {
        Position c = fact.cell();
        if (!c.inside(size)) {
            throw new IllegalArgumentException("Комната вне пещеры: " + c);
        }
        return fact.kind().ordinal() * size * size + (c.row() - 1) * size + c.col();
    }
}
