package wumpus.controller;

import wumpus.agent.Hunter;
import wumpus.model.World;

/** Строки панели состояния. */
public final class Texts {

    private Texts() {
    }

    /** Что охотник знает о Вампусе. */
    public static String wumpus(Hunter h) {
        if (h.wumpusDead()) {
            return "убит";
        }
        return h.knownWumpus() == null ? "не найден" : "в " + h.knownWumpus() + ", жив";
    }

    /** Что охотник знает о золоте. */
    public static String gold(Hunter h) {
        if (h.hasGold()) {
            return "у охотника";
        }
        return h.knownGold() == null ? "не найдено" : "в " + h.knownGold();
    }

    public static String arrow(Hunter h) {
        return h.hasArrow() ? "есть" : "нет";
    }

    /** Итог игры; пока игра идёт — пустая строка. */
    public static String result(World w) {
        if (!w.over()) {
            return "";
        }
        if (w.won()) {
            return "Победа: охотник выбрался с золотом";
        }
        if (!w.alive()) {
            return w.cave().isPit(w.position())
                    ? "Охотник упал в яму в " + w.position()
                    : "Охотника съел Вампус в " + w.position();
        }
        return "Охотник выбрался без золота";
    }

    /** Счёт со знаком: «+974», «−7», «0». */
    public static String score(int v) {
        if (v == 0) {
            return "0";
        }
        return (v > 0 ? "+" : "−") + Math.abs(v);
    }
}
