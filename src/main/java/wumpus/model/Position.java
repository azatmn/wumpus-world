package wumpus.model;

import java.util.ArrayList;
import java.util.List;

/** Комната пещеры в нотации задания: (строка, столбец), (1,1) — левый верхний угол. */
public record Position(int row, int col) {

    /** Соседняя комната в направлении d (без проверки границ). */
    public Position step(Direction d) {
        return new Position(row + d.dRow(), col + d.dCol());
    }

    public boolean inside(int size) {
        return row >= 1 && row <= size && col >= 1 && col <= size;
    }

    /** Соседи по сторонам в порядке вверх, вправо, вниз, влево — только внутри поля. */
    public List<Position> neighbors(int size) {
        List<Position> result = new ArrayList<>(4);
        for (Direction d : Direction.values()) {
            Position n = step(d);
            if (n.inside(size)) {
                result.add(n);
            }
        }
        return result;
    }

    public boolean isNeighbor(Position other) {
        return Math.abs(row - other.row) + Math.abs(col - other.col) == 1;
    }

    @Override
    public String toString() {
        return row + "," + col;
    }
}
