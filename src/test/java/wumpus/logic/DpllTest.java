package wumpus.logic;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class DpllTest {

    private static List<int[]> cnf(int[]... clauses) {
        return List.of(clauses);
    }

    private static boolean satisfies(boolean[] model, List<int[]> clauses) {
        for (int[] c : clauses) {
            boolean any = false;
            for (int lit : c) {
                any |= model[Math.abs(lit)] == (lit > 0);
            }
            if (!any) {
                return false;
            }
        }
        return true;
    }

    /** Перебор всех 2^n наборов — эталон для сравнения. */
    private static boolean bruteForce(int vars, List<int[]> clauses) {
        boolean[] model = new boolean[vars + 1];
        for (int mask = 0; mask < (1 << vars); mask++) {
            for (int v = 1; v <= vars; v++) {
                model[v] = (mask >> (v - 1) & 1) == 1;
            }
            if (satisfies(model, clauses)) {
                return true;
            }
        }
        return false;
    }

    @Test
    void emptyFormulaIsSatisfiable() {
        assertNotNull(Dpll.solve(3, List.of()));
    }

    @Test
    void emptyClauseIsUnsatisfiable() {
        assertNull(Dpll.solve(2, cnf(new int[]{})));
    }

    @Test
    void contradictionOfUnits() {
        assertNull(Dpll.solve(1, cnf(new int[]{1}, new int[]{-1})));
    }

    @Test
    void unitsAndImplicationsGiveTheOnlyModel() {
        // x1, x1 → x2, x2 → x3, ¬x3 ∨ ¬x4
        List<int[]> f = cnf(new int[]{1}, new int[]{-1, 2}, new int[]{-2, 3}, new int[]{-3, -4});
        boolean[] m = Dpll.solve(4, f);
        assertNotNull(m);
        assertTrue(m[1] && m[2] && m[3]);
        assertFalse(m[4]);
    }

    @Test
    void needsBacktracking() {
        // решение только x1 = x2 = true, а перебор начинает с false
        List<int[]> f = cnf(new int[]{1, 2}, new int[]{1, -2}, new int[]{-1, 2});
        boolean[] m = Dpll.solve(2, f);
        assertNotNull(m);
        assertTrue(m[1] && m[2]);
    }

    @Test
    void pigeonholeThreeIntoTwoIsUnsatisfiable() {
        // голубь i в норе j: переменная 2(i−1)+j
        List<int[]> f = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            f.add(new int[]{2 * (i - 1) + 1, 2 * (i - 1) + 2});
        }
        for (int j = 1; j <= 2; j++) {
            for (int a = 1; a <= 3; a++) {
                for (int b = a + 1; b <= 3; b++) {
                    f.add(new int[]{-(2 * (a - 1) + j), -(2 * (b - 1) + j)});
                }
            }
        }
        assertNull(Dpll.solve(6, f));
    }

    @Test
    void duplicateLiteralsAndTautologies() {
        assertNotNull(Dpll.solve(2, cnf(new int[]{1, 1}, new int[]{2, -2}, new int[]{-1, -1, 2})));
        assertNull(Dpll.solve(1, cnf(new int[]{1, 1}, new int[]{-1, -1})));
    }

    @Test
    void agreesWithBruteForceOnRandomFormulas() {
        Random random = new Random(7);
        int sat = 0;
        for (int round = 0; round < 3000; round++) {
            int vars = 1 + random.nextInt(9);
            int count = random.nextInt(4 * vars + 2);
            List<int[]> f = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                int[] c = new int[1 + random.nextInt(3)];
                for (int k = 0; k < c.length; k++) {
                    c[k] = (1 + random.nextInt(vars)) * (random.nextBoolean() ? 1 : -1);
                }
                f.add(c);
            }
            boolean[] m = Dpll.solve(vars, f);
            assertEquals(bruteForce(vars, f), m != null, "раунд " + round);
            if (m != null) {
                assertTrue(satisfies(m, f), "найденный набор должен подходить, раунд " + round);
                sat++;
            }
        }
        assertTrue(sat > 500 && sat < 2500, "смесь выполнимых и невыполнимых: " + sat);
    }

    @Test
    void agreesWithBruteForceOnHardRandom3Sat() {
        // около 4.3 предложений на переменную — самые трудные формулы: много откатов и выученных причин
        Random random = new Random(11);
        int sat = 0;
        for (int round = 0; round < 1500; round++) {
            int vars = 8 + random.nextInt(7);
            int count = (int) Math.round(vars * (3.8 + random.nextDouble()));
            List<int[]> f = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                int[] c = new int[3];
                for (int k = 0; k < 3; k++) {
                    c[k] = (1 + random.nextInt(vars)) * (random.nextBoolean() ? 1 : -1);
                }
                f.add(c);
            }
            boolean[] m = Dpll.solve(vars, f);
            assertEquals(bruteForce(vars, f), m != null, "раунд " + round);
            if (m != null) {
                assertTrue(satisfies(m, f), "раунд " + round);
                sat++;
            }
        }
        assertTrue(sat > 300 && sat < 1200, "смесь выполнимых и невыполнимых: " + sat);
    }

    @Test
    void literalOutsideVariablesRejected() {
        assertThrows(IllegalArgumentException.class, () -> Dpll.solve(2, cnf(new int[]{3})));
        assertThrows(IllegalArgumentException.class, () -> Dpll.solve(2, cnf(new int[]{0})));
    }

    @Test
    void largeExactlyOneIsFast() {
        // ровно одна из 144 переменных — как «Вампус ровно один» в пещере 12×12
        int n = 144;
        List<int[]> f = new ArrayList<>();
        int[] atLeast = new int[n];
        for (int i = 0; i < n; i++) {
            atLeast[i] = i + 1;
            for (int j = i + 1; j < n; j++) {
                f.add(new int[]{-(i + 1), -(j + 1)});
            }
        }
        f.add(atLeast);
        for (int i = 1; i < n; i++) {
            f.add(new int[]{-i});
        }
        long start = System.nanoTime();
        boolean[] m = Dpll.solve(n, f);
        long ms = (System.nanoTime() - start) / 1_000_000;
        assertNotNull(m);
        assertTrue(m[n]);
        assertTrue(ms < 500, "решалось " + ms + " мс");
    }
}
