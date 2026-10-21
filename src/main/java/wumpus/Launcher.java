package wumpus;

import javafx.application.Application;
import wumpus.view.WumpusApp;

/**
 * Точка входа.
 * Отдельный класс, а не сам WumpusApp: если главный класс наследует Application,
 * запуск без модулей JavaFX падает с ошибкой «JavaFX runtime components are missing».
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        Application.launch(WumpusApp.class, args);
    }
}
