package wumpus.logic;

import java.util.ArrayList;
import java.util.List;

/**
 * Проверка выполнимости формулы в КНФ алгоритмом DPLL: перебор значений переменных
 * с распространением единичных предложений и возвратом при противоречии.
 * <p>
 * Переменные — числа 1..n, литерал x — «переменная x истинна», −x — «ложна».
 * Предложение — массив литералов, соединённых ИЛИ; формула — список предложений, соединённых И.
 * Каждое предложение следит за двумя своими литералами и проверяется, только когда один из них стал ложным.
 */
public final class Dpll {

    private final int[][] clauses;
    private final List<List<Integer>> watchers;
    private final int[] value;
    private final int[] trail;
    private int trailSize;
    private int propagated;

    private Dpll(int varCount, List<int[]> formula) {
        value = new int[varCount + 1];
        trail = new int[varCount];
        watchers = new ArrayList<>(2 * varCount + 2);
        for (int i = 0; i < 2 * varCount + 2; i++) {
            watchers.add(new ArrayList<>());
        }
        clauses = new int[formula.size()][];
        for (int i = 0; i < formula.size(); i++) {
            int[] c = formula.get(i);
            for (int lit : c) {
                if (lit == 0 || Math.abs(lit) > varCount) {
                    throw new IllegalArgumentException("Литерал " + lit + " вне переменных 1.." + varCount);
                }
            }
            clauses[i] = c.clone();
        }
    }

    /**
     * Найти набор значений, при котором верны все предложения.
     *
     * @return model[x] — значение переменной x (model[0] не используется) или null, если формула невыполнима
     */
    public static boolean[] solve(int varCount, List<int[]> formula) {
        return new Dpll(varCount, formula).search();
    }

    private static int index(int lit) {
        return lit > 0 ? 2 * lit : -2 * lit + 1;
    }

    /** 1 — литерал истинен, −1 — ложен, 0 — переменная ещё не задана. */
    private int valueOf(int lit) {
        int v = value[Math.abs(lit)];
        return lit > 0 ? v : -v;
    }

    private void assign(int lit) {
        value[Math.abs(lit)] = lit > 0 ? 1 : -1;
        trail[trailSize++] = lit;
    }

    /** Откатить назначения до длины следа size. */
    private void undoTo(int size) {
        while (trailSize > size) {
            value[Math.abs(trail[--trailSize])] = 0;
        }
        propagated = size;
    }

    /** Подготовить слежение и единичные предложения. false — противоречие уже на старте. */
    private boolean init() {
        for (int i = 0; i < clauses.length; i++) {
            int[] c = clauses[i];
            if (c.length == 0) {
                return false;
            }
            if (c.length == 1) {
                int v = valueOf(c[0]);
                if (v < 0) {
                    return false;
                }
                if (v == 0) {
                    assign(c[0]);
                }
                continue;
            }
            watchers.get(index(c[0])).add(i);
            watchers.get(index(c[1])).add(i);
        }
        return true;
    }

    /** Распространение единичных предложений. false — противоречие. */
    private boolean propagate() {
        while (propagated < trailSize) {
            int falseLit = -trail[propagated++];
            List<Integer> list = watchers.get(index(falseLit));
            for (int w = 0; w < list.size(); ) {
                int ci = list.get(w);
                int[] c = clauses[ci];
                if (c[0] == falseLit) {
                    c[0] = c[1];
                    c[1] = falseLit;
                }
                if (valueOf(c[0]) > 0) {
                    w++;
                    continue;
                }
                boolean moved = false;
                for (int k = 2; k < c.length; k++) {
                    if (valueOf(c[k]) >= 0) {
                        c[1] = c[k];
                        c[k] = falseLit;
                        watchers.get(index(c[1])).add(ci);
                        list.set(w, list.getLast());
                        list.removeLast();
                        moved = true;
                        break;
                    }
                }
                if (moved) {
                    continue;
                }
                int other = valueOf(c[0]);
                if (other < 0) {
                    return false;
                }
                if (other == 0) {
                    assign(c[0]);
                }
                w++;
            }
        }
        return true;
    }

    private boolean[] search() {
        if (!init()) {
            return null;
        }
        // решения: литерал выбора и длина следа до него; flipped — уже пробовали обратное значение
        int[] decision = new int[value.length];
        int[] trailBefore = new int[value.length];
        boolean[] flipped = new boolean[value.length];
        int level = 0;
        int next = 1;
        while (true) {
            if (!propagate()) {
                while (level > 0 && flipped[level - 1]) {
                    level--;
                }
                if (level == 0) {
                    return null;
                }
                undoTo(trailBefore[level - 1]);
                decision[level - 1] = -decision[level - 1];
                flipped[level - 1] = true;
                assign(decision[level - 1]);
                next = 1;
                continue;
            }
            while (next < value.length && value[next] != 0) {
                next++;
            }
            if (next == value.length) {
                boolean[] model = new boolean[value.length];
                for (int v = 1; v < value.length; v++) {
                    model[v] = value[v] > 0;
                }
                return model;
            }
            // сначала «ложь»: в пещере почти все факты ложны (ям мало, Вампус один)
            decision[level] = -next;
            trailBefore[level] = trailSize;
            flipped[level] = false;
            level++;
            assign(-next);
        }
    }
}
