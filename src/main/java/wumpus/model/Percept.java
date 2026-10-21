package wumpus.model;

/** Одно измерение пяти датчиков, в порядке задания: (Stench, Breeze, Glitter, Bump, Scream). */
public record Percept(boolean stench, boolean breeze, boolean glitter, boolean bump, boolean scream) {

    public static final Percept NONE = new Percept(false, false, false, false, false);

    @Override
    public String toString() {
        return "(" + (stench ? "Stench" : "none")
                + ", " + (breeze ? "Breeze" : "none")
                + ", " + (glitter ? "Glitter" : "none")
                + ", " + (bump ? "Bump" : "none")
                + ", " + (scream ? "Scream" : "none") + ")";
    }
}
