package wumpus.logic;

import java.util.ArrayList;
import java.util.List;

/**
 * Проверка выполнимости формулы в КНФ: DPLL с обучением на конфликтах (CDCL).
 * <p>
 * Переменные — числа 1..n, литерал x — «переменная x истинна», −x — «ложна».
 * Предложение — массив литералов, соединённых ИЛИ; формула — список предложений, соединённых И.
 * <p>
 * Как обычный DPLL: выбирает значение переменной, выводит следствия из единичных предложений,
 * при противоречии откатывается. Отличие: из противоречия выводится новое предложение — его причина,
 * и откат идёт сразу к решению, которое к ней привело. Без этого перебор раз за разом
 * переставляет не относящиеся к делу переменные и на пещерах побольше не заканчивается.
 * Каждое предложение следит за двумя своими литералами и проверяется, только когда один из них стал ложным.
 */
public final class Dpll {

    private static final int NO_REASON = -1;

    private final List<int[]> clauses = new ArrayList<>();
    private final List<List<Integer>> watchers;
    private final int[] value;
    private final int[] level;
    private final int[] reason;
    private final int[] trail;
    private int trailSize;
    private int propagated;
    /** Где на следе начинается каждый уровень решений. */
    private final List<Integer> levelStart = new ArrayList<>();
    private final boolean[] seen;

    private Dpll(int varCount, List<int[]> formula) {
        value = new int[varCount + 1];
        level = new int[varCount + 1];
        reason = new int[varCount + 1];
        seen = new boolean[varCount + 1];
        trail = new int[varCount];
        watchers = new ArrayList<>(2 * varCount + 2);
        for (int i = 0; i < 2 * varCount + 2; i++) {
            watchers.add(new ArrayList<>());
        }
        for (int[] c : formula) {
            for (int lit : c) {
                if (lit == 0 || Math.abs(lit) > varCount) {
                    throw new IllegalArgumentException("Литерал " + lit + " вне переменных 1.." + varCount);
                }
            }
        }
        for (int[] c : formula) {
            clauses.add(c.clone());
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

    private int currentLevel() {
        return levelStart.size();
    }

    private void assign(int lit, int why) {
        int v = Math.abs(lit);
        value[v] = lit > 0 ? 1 : -1;
        level[v] = currentLevel();
        reason[v] = why;
        trail[trailSize++] = lit;
    }

    /** Откатить все решения выше уровня target. */
    private void backjump(int target) {
        int size = levelStart.get(target);
        while (trailSize > size) {
            value[Math.abs(trail[--trailSize])] = 0;
        }
        propagated = size;
        while (levelStart.size() > target) {
            levelStart.removeLast();
        }
    }

    private void watch(int ci) {
        int[] c = clauses.get(ci);
        watchers.get(index(c[0])).add(ci);
        watchers.get(index(c[1])).add(ci);
    }

    /** Подготовить слежение и единичные предложения. false — противоречие уже на старте. */
    private boolean init() {
        for (int i = 0; i < clauses.size(); i++) {
            int[] c = clauses.get(i);
            if (c.length == 0) {
                return false;
            }
            if (c.length == 1) {
                int v = valueOf(c[0]);
                if (v < 0) {
                    return false;
                }
                if (v == 0) {
                    assign(c[0], NO_REASON);
                }
                continue;
            }
            watch(i);
        }
        return true;
    }

    /** Распространение единичных предложений. Возвращает номер нарушенного предложения или −1. */
    private int propagate() {
        while (propagated < trailSize) {
            int falseLit = -trail[propagated++];
            List<Integer> list = watchers.get(index(falseLit));
            for (int w = 0; w < list.size(); ) {
                int ci = list.get(w);
                int[] c = clauses.get(ci);
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
                    return ci;
                }
                if (other == 0) {
                    assign(c[0], ci);
                }
                w++;
            }
        }
        return -1;
    }

    /**
     * Причина противоречия — новое предложение (схема «первой точки сочленения»):
     * разворачиваем следствия текущего уровня назад, пока на нём не останется один литерал.
     * Первым в результате стоит отрицание этого литерала.
     */
    private int[] analyze(int conflict) {
        List<Integer> learnt = new ArrayList<>();
        learnt.add(0);
        int pending = 0;
        int lit = 0;
        int ci = conflict;
        int t = trailSize - 1;
        do {
            for (int q : clauses.get(ci)) {
                int v = Math.abs(q);
                if (q == lit || seen[v] || level[v] == 0) {
                    continue;
                }
                seen[v] = true;
                if (level[v] == currentLevel()) {
                    pending++;
                } else {
                    learnt.add(q);
                }
            }
            while (!seen[Math.abs(trail[t])]) {
                t--;
            }
            lit = trail[t--];
            seen[Math.abs(lit)] = false;
            ci = reason[Math.abs(lit)];
            pending--;
        } while (pending > 0);
        learnt.set(0, -lit);
        int[] result = new int[learnt.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = learnt.get(i);
            seen[Math.abs(result[i])] = false;
        }
        return result;
    }

    private boolean[] search() {
        if (!init()) {
            return null;
        }
        int next = 1;
        while (true) {
            int conflict = propagate();
            if (conflict >= 0) {
                if (currentLevel() == 0) {
                    return null;
                }
                int[] learnt = analyze(conflict);
                // откат на самый высокий уровень среди остальных литералов причины
                int target = 0;
                int at = 1;
                for (int i = 1; i < learnt.length; i++) {
                    if (level[Math.abs(learnt[i])] > target) {
                        target = level[Math.abs(learnt[i])];
                        at = i;
                    }
                }
                backjump(target);
                if (learnt.length == 1) {
                    assign(learnt[0], NO_REASON);
                } else {
                    int swap = learnt[1];
                    learnt[1] = learnt[at];
                    learnt[at] = swap;
                    clauses.add(learnt);
                    watch(clauses.size() - 1);
                    assign(learnt[0], clauses.size() - 1);
                }
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
            levelStart.add(trailSize);
            assign(-next, NO_REASON);
        }
    }
}
