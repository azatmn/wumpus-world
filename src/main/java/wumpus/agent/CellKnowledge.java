package wumpus.agent;

import wumpus.logic.Status;
import wumpus.model.Percept;
import wumpus.model.Position;

/**
 * Что охотник знает о комнате — то, что рисуется на его карте.
 *
 * @param percept измерение в комнате или null, если охотник там не был
 * @param safe    комната безопасна: ямы точно нет и живого Вампуса точно нет (пометка OK)
 */
public record CellKnowledge(Position cell, Percept percept, Status pit, Status wumpus, Status gold, boolean safe) {

    public boolean visited() {
        return percept != null;
    }
}
