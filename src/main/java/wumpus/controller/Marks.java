package wumpus.controller;

import wumpus.agent.CellKnowledge;
import wumpus.agent.Hunter;
import wumpus.logic.Status;
import wumpus.model.Percept;
import wumpus.model.Position;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Пометки на карте охотника, как в таблицах задания: OK, P?, P!, W?, W!, G?, G!.
 * «!» — доказано. «?» ставится только рядом с комнатой, где почувствовали ветерок, запах или блеск:
 * иначе «может быть» стояло бы почти в каждой неизвестной комнате и карта превратилась бы в шум.
 */
public final class Marks {

    private Marks() {
    }

    public static List<String> of(Hunter hunter, Position cell) {
        CellKnowledge k = hunter.knowledge().get(cell);
        List<String> result = new ArrayList<>();
        if (k.safe() && !k.visited()) {
            result.add("OK");
        }
        add(result, "P", k.pit(), hunter, cell, Percept::breeze);
        if (!hunter.wumpusDead()) {
            add(result, "W", k.wumpus(), hunter, cell, Percept::stench);
        }
        if (!hunter.hasGold()) {
            add(result, "G", k.gold(), hunter, cell, Percept::glitter);
        }
        return result;
    }

    private static void add(List<String> result, String letter, Status status, Hunter hunter, Position cell,
                            Predicate<Percept> sensor) {
        if (status == Status.YES) {
            result.add(letter + "!");
        } else if (status == Status.MAYBE && sensedNear(hunter, cell, sensor)) {
            result.add(letter + "?");
        }
    }

    /** Почувствовали ли в самой комнате или рядом. */
    private static boolean sensedNear(Hunter hunter, Position cell, Predicate<Percept> sensor) {
        int size = (int) Math.round(Math.sqrt(hunter.knowledge().size()));
        List<Position> around = new ArrayList<>(cell.neighbors(size));
        around.add(cell);
        for (Position p : around) {
            Percept seen = hunter.knowledge().get(p).percept();
            if (seen != null && sensor.test(seen)) {
                return true;
            }
        }
        return false;
    }
}
