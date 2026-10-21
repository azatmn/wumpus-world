package wumpus.model;

/** Куда смотрит охотник. Порядок — по часовой стрелке от «вверх»; строки нумеруются сверху вниз. */
public enum Direction {
    UP(-1, 0, "вверх"),
    RIGHT(0, 1, "вправо"),
    DOWN(1, 0, "вниз"),
    LEFT(0, -1, "влево");

    private final int dRow;
    private final int dCol;
    private final String title;

    Direction(int dRow, int dCol, String title) {
        this.dRow = dRow;
        this.dCol = dCol;
        this.title = title;
    }

    public int dRow() {
        return dRow;
    }

    public int dCol() {
        return dCol;
    }

    /** Название для журнала: «вверх», «вправо»... */
    public String title() {
        return title;
    }

    /** Поворот на 90° по часовой стрелке. */
    public Direction right() {
        return values()[(ordinal() + 1) % 4];
    }

    /** Поворот на 90° против часовой стрелки. */
    public Direction left() {
        return values()[(ordinal() + 3) % 4];
    }
}
