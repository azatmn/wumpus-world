package wumpus.model;

/** Действия охотника: три привода из задания и два, без которых игру не закончить. */
public enum Action {
    FORWARD("вперёд"),
    TURN_LEFT("налево"),
    TURN_RIGHT("направо"),
    SHOOT("выстрел"),
    GRAB("взять золото"),
    CLIMB("выбраться");

    private final String title;

    Action(String title) {
        this.title = title;
    }

    public String title() {
        return title;
    }
}
