package wumpus.view;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import wumpus.controller.FormField;
import wumpus.controller.FormResult;
import wumpus.model.InvalidSettingsException.Problem;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;

/**
 * Окно с формой. Проверка введённого — в контроллере (CaveForm);
 * здесь только поля, подсветка ошибок и кнопки. С ошибками окно не закрывается.
 */
class FormDialog<T> extends Dialog<T> {

    private final Map<String, TextField> inputs = new LinkedHashMap<>();
    private final Label errors = new Label();
    private final Function<Map<String, String>, FormResult<T>> parser;
    private T accepted;

    /** @param defaults тексты для кнопки «По умолчанию» */
    FormDialog(String title, String header, String applyText, List<FormField> fields,
               Map<String, String> current, Map<String, String> defaults,
               Function<Map<String, String>, FormResult<T>> parser) {
        this.parser = parser;
        setTitle(title);
        setHeaderText(header);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(6);
        grid.setPadding(new Insets(4, 4, 4, 4));
        // подписи никогда не обрезаются многоточием: столбец не уже самой длинной подписи
        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setMinWidth(Region.USE_PREF_SIZE);
        grid.getColumnConstraints().addAll(labelColumn, new ColumnConstraints(), new ColumnConstraints());
        int row = 0;
        for (FormField field : fields) {
            TextField input = new TextField(current.get(field.key()));
            input.setPrefColumnCount(10);
            input.setId("field-" + field.key());
            inputs.put(field.key(), input);
            grid.add(new Label(field.label()), 0, row);
            grid.add(input, 1, row);
            if (field.key().equals("seed")) {
                Button random = new Button("Случайный");
                random.setMinWidth(Region.USE_PREF_SIZE);
                random.setOnAction(e -> input.setText(Long.toString(new Random().nextLong())));
                grid.add(random, 2, row);
            }
            row++;
        }

        Button reset = new Button("По умолчанию");
        reset.setOnAction(e -> defaults.forEach((key, text) -> inputs.get(key).setText(text)));

        errors.getStyleClass().add("error-text");
        errors.setWrapText(true);
        errors.setMaxWidth(520);
        // блок ошибок растёт по высоте под все строки, а не обрезается
        errors.setMinHeight(Region.USE_PREF_SIZE);

        getDialogPane().setContent(new VBox(10, grid, new HBox(reset), errors));
        ButtonType apply = new ButtonType(applyText, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(apply, cancel);
        getDialogPane().getStylesheets().add(WumpusApp.stylesheet());

        Button applyButton = (Button) getDialogPane().lookupButton(apply);
        applyButton.setId("apply");
        applyButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (!tryAccept()) {
                event.consume(); // есть ошибки — окно остаётся открытым
            }
        });
        setResultConverter(button -> button == apply ? accepted : null);
    }

    private boolean tryAccept() {
        Map<String, String> texts = new HashMap<>();
        inputs.forEach((key, input) -> texts.put(key, input.getText()));
        inputs.values().forEach(input -> input.getStyleClass().remove("invalid"));
        switch (parser.apply(texts)) {
            case FormResult.Valid<T>(T value) -> {
                accepted = value;
                return true;
            }
            case FormResult.Invalid<T>(List<Problem> problems) -> {
                StringBuilder text = new StringBuilder();
                for (Problem problem : problems) {
                    text.append("• ").append(problem.message()).append('\n');
                    TextField input = inputs.get(problem.field());
                    if (input != null && !input.getStyleClass().contains("invalid")) {
                        input.getStyleClass().add("invalid");
                    }
                }
                errors.setText(text.toString().strip());
                return false;
            }
        }
    }
}
