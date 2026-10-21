package wumpus.logic;

/** О чём бывает факт: яма, Вампус или золото в комнате. Буквы — как в задании (P, W) и G для золота. */
public enum Kind {
    PIT("P", "яма"),
    WUMPUS("W", "Вампус"),
    GOLD("G", "золото");

    private final String letter;
    private final String title;

    Kind(String letter, String title) {
        this.letter = letter;
        this.title = title;
    }

    public String letter() {
        return letter;
    }

    public String title() {
        return title;
    }
}
