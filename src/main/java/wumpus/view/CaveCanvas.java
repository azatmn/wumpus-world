package wumpus.view;

import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import wumpus.agent.CellKnowledge;
import wumpus.agent.Hunter;
import wumpus.controller.Marks;
import wumpus.model.Cave;
import wumpus.model.Percept;
import wumpus.model.Position;
import wumpus.model.World;

import java.util.List;

/**
 * Карта пещеры: либо настоящая (что где лежит и что чувствуется), либо то, что знает охотник.
 * Растягивается под размер окна, клетки квадратные. В углу клетки — её координаты, как в таблицах задания.
 */
public class CaveCanvas extends Pane {

    public enum Mode {
        TRUTH,
        KNOWLEDGE
    }

    static final Color GRID = Color.web("#d3d1c7");
    static final Color CELL = Color.web("#ffffff");
    static final Color VISITED = Color.web("#e4e2dc");
    static final Color START = Color.web("#e6f1fb");
    static final Color MUTED = Color.web("#888780");
    static final Color OK = Color.web("#0f6e56");
    static final Color MAYBE = Color.web("#854f0b");
    static final Color SURE = Color.web("#a32d2d");
    static final Color HIDDEN = Color.web("#f1efe8");

    private static final double MARGIN = 4;

    private final Mode mode;
    private final Canvas canvas = new Canvas();
    private World world;
    private Hunter hunter;
    private boolean hidden;

    public CaveCanvas(Mode mode) {
        this.mode = mode;
        getChildren().add(canvas);
        setMinSize(160, 160);
        widthProperty().addListener((o, a, b) -> redraw());
        heightProperty().addListener((o, a, b) -> redraw());
    }

    public void show(World world, Hunter hunter, boolean hidden) {
        this.world = world;
        this.hunter = hunter;
        this.hidden = hidden;
        redraw();
    }

    @Override
    protected void layoutChildren() {
        canvas.setWidth(getWidth());
        canvas.setHeight(getHeight());
    }

    private void redraw() {
        canvas.setWidth(getWidth());
        canvas.setHeight(getHeight());
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        if (world == null) {
            return;
        }
        Cave cave = world.cave();
        int n = cave.size();
        double cell = Math.floor(Math.min(getWidth() - 2 * MARGIN, getHeight() - 2 * MARGIN) / n);
        if (cell <= 4) {
            return;
        }
        double left = Math.floor((getWidth() - cell * n) / 2);
        double top = MARGIN;
        if (hidden) {
            g.setFill(HIDDEN);
            g.fillRoundRect(left, top, cell * n, cell * n, 8, 8);
            g.setFill(MUTED);
            g.setFont(Font.font("PT Sans", 14));
            g.setTextAlign(TextAlignment.CENTER);
            g.setTextBaseline(VPos.CENTER);
            g.fillText("Пещера скрыта —\nохотник её тоже не видит", left + cell * n / 2, top + cell * n / 2);
            return;
        }
        for (int row = 1; row <= n; row++) {
            for (int col = 1; col <= n; col++) {
                Position p = new Position(row, col);
                double x = left + (col - 1) * cell;
                double y = top + (row - 1) * cell;
                if (mode == Mode.TRUTH) {
                    drawTruth(g, p, x, y, cell);
                } else {
                    drawKnowledge(g, p, x, y, cell);
                }
                g.setStroke(GRID);
                g.setLineWidth(1);
                g.strokeRect(x + 0.5, y + 0.5, cell - 1, cell - 1);
                g.setFill(MUTED);
                g.setFont(Font.font("PT Sans", Math.max(8, cell * 0.14)));
                g.setTextAlign(TextAlignment.LEFT);
                g.setTextBaseline(VPos.TOP);
                g.fillText(p.toString(), x + cell * 0.06, y + cell * 0.04);
            }
        }
        Position h = world.position();
        double hx = left + (h.col() - 0.5) * cell;
        double hy = top + (h.row() - 0.5) * cell;
        Icons.hunter(g, hx, hy, cell, world.facing());
    }

    private void drawTruth(GraphicsContext g, Position p, double x, double y, double cell) {
        Cave cave = world.cave();
        g.setFill(p.equals(cave.start()) ? START : CELL);
        g.fillRect(x, y, cell, cell);
        double cx = x + cell / 2;
        double cy = y + cell / 2;
        if (cave.isPit(p)) {
            Icons.pit(g, cx, cy, cell, Icons.PIT);
        }
        if (cave.wumpus().equals(p)) {
            Icons.wumpus(g, cx, cy, cell, world.wumpusAlive());
        }
        if (cave.gold().equals(p) && !world.hasGold()) {
            Icons.gold(g, cx, cy, cell);
        }
        sensors(g, x, y, cell, cave.hasStench(p), cave.hasBreeze(p), cave.nearGold(p) && !world.hasGold());
    }

    private void drawKnowledge(GraphicsContext g, Position p, double x, double y, double cell) {
        CellKnowledge k = hunter.knowledge().get(p);
        g.setFill(k.visited() ? VISITED : CELL);
        g.fillRect(x, y, cell, cell);
        if (k.visited()) {
            Percept seen = k.percept();
            sensors(g, x, y, cell, seen.stench(), seen.breeze(), seen.glitter());
        }
        List<String> marks = Marks.of(hunter, p);
        if (marks.isEmpty()) {
            return;
        }
        double fontSize = Math.max(9, cell * (marks.size() > 1 ? 0.2 : 0.26));
        g.setFont(Font.font("PT Sans", FontWeight.BOLD, fontSize));
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.CENTER);
        double lineHeight = fontSize * 1.1;
        double start = y + cell / 2 - (marks.size() - 1) * lineHeight / 2;
        for (int i = 0; i < marks.size(); i++) {
            String m = marks.get(i);
            g.setFill(m.equals("OK") ? OK : m.endsWith("?") ? MAYBE : m.startsWith("G") ? Icons.GOLD_EDGE : SURE);
            g.fillText(m, x + cell / 2, start + i * lineHeight);
        }
    }

    /** Значки датчиков в нижней полосе клетки. */
    private static void sensors(GraphicsContext g, double x, double y, double cell, boolean stench, boolean breeze, boolean glitter) {
        double s = cell * 0.2;
        double sx = x + cell * 0.08;
        double sy = y + cell - s - cell * 0.06;
        if (stench) {
            Icons.stench(g, sx, sy, s);
            sx += s * 1.3;
        }
        if (breeze) {
            Icons.breeze(g, sx, sy, s);
            sx += s * 1.3;
        }
        if (glitter) {
            Icons.glitter(g, sx, sy, s);
        }
    }
}
