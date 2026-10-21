module wumpus {
    requires javafx.controls;
    exports wumpus;
    exports wumpus.model;
    exports wumpus.logic;
    exports wumpus.agent;
    exports wumpus.controller;
    exports wumpus.util;
    exports wumpus.view to javafx.graphics;
    opens wumpus.view to javafx.graphics;
}
