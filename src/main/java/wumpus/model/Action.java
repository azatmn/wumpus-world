package wumpus.model;

/** Действия охотника: три привода из задания и два, без которых игру не закончить. */
public enum Action {
    FORWARD("шаг вперёд"),
    TURN_LEFT("поворот налево"),
    TURN_RIGHT("поворот направо"),
    SHOOT("выстрел"),
    GRAB("взять золото"),
    CLIMB("выбраться из пещеры");

    private final String title;

    Action(String title) {
        this.title = title;
    }

    public String title() {
        return title;
    }
}
