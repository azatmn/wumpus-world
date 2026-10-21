package wumpus.view;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import wumpus.agent.Hunter;
import wumpus.agent.Turn;
import wumpus.controller.CaveForm;
import wumpus.controller.GameController;
import wumpus.controller.SpeedScale;
import wumpus.controller.Texts;
import wumpus.model.Cave;
import wumpus.model.CaveSpec;
import wumpus.model.World;

import java.util.List;

/**
 * Главное окно: слева настоящая пещера, справа — что знает охотник, ещё правее состояние,
 * внизу журнал шагов «измерение → вывод → решение». Таймер JavaFX на каждом кадре даёт контроллеру
 * время, тот решает, сколько действий сделать, а окно перерисовывает изменившееся.
 */
public class WumpusApp extends Application {

    private final GameController controller = new GameController();
    private final CaveCanvas truth = new CaveCanvas(CaveCanvas.Mode.TRUTH);
    private final CaveCanvas knowledge = new CaveCanvas(CaveCanvas.Mode.KNOWLEDGE);
    private final Label caveInfo = new Label();
    private final Button startPause = new Button();
    private final Button step = new Button("Шаг");
    private final CheckBox showTruth = new CheckBox("показывать правду");
    private final CheckBox takeRisks = new CheckBox("рисковать, когда безопасных ходов нет");
    private final Slider speed = new Slider(SpeedScale.SLIDER_MIN, SpeedScale.SLIDER_MAX, 0);
    private final Label speedLabel = new Label();
    private final Label stepValue = new Label();
    private final Label actionsValue = new Label();
    private final Label scoreValue = new Label();
    private final Label arrowValue = new Label();
    private final Label wumpusValue = new Label();
    private final Label goldValue = new Label();
    private final Label clausesValue = new Label();
    private final Label result = new Label();
    private final TextArea journal = new TextArea();
    private AnimationTimer timer;

    /** Что уже нарисовано: перерисовываем, только когда что-то поменялось. */
    private World drawnWorld;
    private int drawnActions = -1;
    private boolean drawnTruth;
    private int drawnTurns = -1;

    static String stylesheet() {
        return WumpusApp.class.getResource("app.css").toExternalForm();
    }

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.setTop(buildToolbar());
        root.setCenter(buildMaps());
        root.setRight(buildPanel());
        root.setBottom(buildJournal());

        Scene scene = new Scene(root, 1180, 800);
        scene.getStylesheets().add(stylesheet());
        stage.setTitle("Мир Вампуса");
        stage.setMinWidth(900);
        stage.setMinHeight(620);
        stage.setScene(scene);

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                controller.tick(now);
                refresh();
            }
        };
        refresh();
        stage.show();
        timer.start();
    }

    @Override
    public void stop() {
        if (timer != null) {
            timer.stop();
        }
    }

    private HBox buildToolbar() {
        Label title = new Label("Мир Вампуса");
        title.getStyleClass().add("title");
        caveInfo.getStyleClass().add("muted");

        startPause.setId("startPause");
        startPause.setOnAction(e -> {
            controller.toggleRunning();
            refresh();
        });
        step.setId("step");
        step.setOnAction(e -> {
            controller.stepOnce();
            refresh();
        });
        Button restart = new Button("Сначала");
        restart.setId("restart");
        restart.setOnAction(e -> {
            controller.restart();
            refresh();
        });
        Button newCave = new Button("Новая пещера…");
        newCave.setId("newCave");
        newCave.setOnAction(e -> openNewCave());
        Button example = new Button("Пример из задания");
        example.setId("example");
        example.setOnAction(e -> {
            controller.pdfExample();
            refresh();
        });
        for (Button b : List.of(startPause, step, restart, newCave, example)) {
            b.setMinWidth(Region.USE_PREF_SIZE);
        }
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(8, title, caveInfo, spacer, startPause, step, restart, newCave, example);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("toolbar-row");
        return bar;
    }

    private HBox buildMaps() {
        VBox left = map("Пещера (правда)", truth);
        VBox right = map("Что знает охотник", knowledge);
        HBox maps = new HBox(14, left, right);
        HBox.setHgrow(left, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);
        maps.setPadding(new Insets(10, 12, 6, 12));
        return maps;
    }

    private static VBox map(String caption, CaveCanvas canvas) {
        Label label = new Label(caption);
        label.getStyleClass().add("muted");
        VBox box = new VBox(4, label, canvas);
        VBox.setVgrow(canvas, Priority.ALWAYS);
        box.setMinWidth(160);
        box.setPrefWidth(400);
        return box;
    }

    private VBox buildPanel() {
        Label state = new Label("Состояние");
        state.getStyleClass().add("section");
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(4);
        String[] names = {"Шаг вывода", "Действий", "Счёт", "Стрела", "Вампус", "Золото", "Предложений в базе"};
        Label[] values = {stepValue, actionsValue, scoreValue, arrowValue, wumpusValue, goldValue, clausesValue};
        for (int i = 0; i < names.length; i++) {
            Label name = new Label(names[i]);
            name.getStyleClass().add("muted");
            values[i].getStyleClass().add("value");
            grid.add(name, 0, i);
            grid.add(values[i], 1, i);
        }
        result.getStyleClass().add("status");
        result.setWrapText(true);

        showTruth.setId("showTruth");
        showTruth.setSelected(true);
        showTruth.setOnAction(e -> refresh());
        takeRisks.setId("takeRisks");
        takeRisks.setSelected(controller.isTakeRisks());
        takeRisks.setWrapText(true);
        takeRisks.setOnAction(e -> {
            controller.setTakeRisks(takeRisks.isSelected());
            refresh();
        });
        speed.setValue(SpeedScale.toSlider(controller.getSpeed()));
        speed.valueProperty().addListener((o, a, b) -> {
            controller.setSpeed(SpeedScale.toSpeed(b.doubleValue()));
            refresh();
        });

        Label legendTitle = new Label("Обозначения");
        legendTitle.getStyleClass().add("section");
        Label legend = new Label("""
                серая клетка — охотник там был
                OK — доказано, что безопасно
                P? / W? / G? — может быть яма / Вампус / золото
                P! / W! / G! — доказано
                волны — запах, линии — ветерок, звезда — блеск""");
        legend.getStyleClass().add("legend");
        legend.setWrapText(true);

        Label settings = new Label("Настройки");
        settings.getStyleClass().add("section");
        VBox panel = new VBox(6, state, grid, result, settings, showTruth, takeRisks,
                new Label("Скорость"), speed, speedLabel, legendTitle, legend);
        panel.getStyleClass().add("panel");
        panel.setPrefWidth(280);
        panel.setMinWidth(240);
        return panel;
    }

    private VBox buildJournal() {
        Label caption = new Label("Журнал: измерение → вывод → решение");
        caption.getStyleClass().add("muted");
        journal.setEditable(false);
        journal.setWrapText(true);
        journal.setPrefRowCount(7);
        journal.setId("journal");
        VBox box = new VBox(4, caption, journal);
        box.setPadding(new Insets(6, 12, 10, 12));
        return box;
    }

    private void openNewCave() {
        controller.pause();
        new FormDialog<>("Новая пещера",
                "Расстановка случайная по seed. Ям — указанная доля от числа комнат.\nСтарт и соседние с ним комнаты всегда без ям и Вампуса.",
                "Создать", CaveForm.FIELDS, CaveForm.toTexts(controller.spec()), CaveForm.toTexts(CaveSpec.defaults()),
                CaveForm::parse)
                .showAndWait().ifPresent(controller::newCave);
        refresh();
    }

    private void refresh() {
        World world = controller.world();
        Hunter hunter = controller.hunter();
        Cave cave = controller.cave();
        caveInfo.setText("пещера " + cave.size() + "×" + cave.size() + ", ям " + cave.pits().size());
        startPause.setText(controller.isRunning() ? "Пауза" : "Старт");
        startPause.setDisable(world.over());
        step.setDisable(world.over());
        speedLabel.setText(controller.getSpeed() + " действий в секунду");
        speedLabel.getStyleClass().setAll("label", "muted");

        List<Turn> turns = hunter.journal();
        boolean changed = world != drawnWorld || world.actions() != drawnActions
                || showTruth.isSelected() != drawnTruth || turns.size() != drawnTurns;
        if (!changed) {
            return;
        }
        drawnWorld = world;
        drawnActions = world.actions();
        drawnTruth = showTruth.isSelected();

        truth.show(world, hunter, !showTruth.isSelected());
        knowledge.show(world, hunter, false);
        stepValue.setText(Integer.toString(turns.size()));
        actionsValue.setText(Integer.toString(world.actions()));
        scoreValue.setText(Texts.score(world.score()));
        arrowValue.setText(Texts.arrow(hunter));
        wumpusValue.setText(Texts.wumpus(hunter));
        goldValue.setText(Texts.gold(hunter));
        clausesValue.setText(Integer.toString(hunter.clauseCount()));
        result.setText(Texts.result(world));
        result.getStyleClass().setAll("label", world.won() || !world.over() ? "status" : "message");

        if (turns.size() != drawnTurns) {
            StringBuilder text = new StringBuilder();
            for (Turn t : turns) {
                text.append(t).append('\n');
            }
            journal.setText(text.toString().strip());
            journal.positionCaret(journal.getLength());
            journal.setScrollTop(Double.MAX_VALUE);
            drawnTurns = turns.size();
        }
    }
}
