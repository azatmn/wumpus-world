package wumpus.view;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;
import wumpus.model.Direction;

/** Значки, нарисованные фигурами. (cx, cy) — центр, size — размер клетки. */
final class Icons {

    static final Color HUNTER = Color.web("#185fa5");
    static final Color ARROW = Color.web("#e24b4a");
    static final Color WUMPUS = Color.web("#a32d2d");
    static final Color DEAD = Color.web("#b4b2a9");
    static final Color PIT = Color.web("#2c2c2a");
    static final Color GOLD = Color.web("#ef9f27");
    static final Color GOLD_EDGE = Color.web("#ba7517");
    static final Color STENCH = Color.web("#3b6d11");
    static final Color BREEZE = Color.web("#888780");

    private Icons() {
    }

    /** Охотник: голова и плечи; красная стрелка у края клетки — куда он смотрит (как на картинке задания). */
    static void hunter(GraphicsContext g, double cx, double cy, double size, Direction facing) {
        double r = size * 0.1;
        g.setFill(HUNTER);
        g.fillOval(cx - r, cy - size * 0.16 - r, 2 * r, 2 * r);
        g.fillArc(cx - size * 0.17, cy - size * 0.03, size * 0.34, size * 0.3, 0, 180, ArcType.ROUND);
        double from = size * 0.24;
        double tip = size * 0.44;
        double head = size * 0.08;
        double sx = cx + facing.dCol() * from;
        double sy = cy + facing.dRow() * from;
        double px = cx + facing.dCol() * tip;
        double py = cy + facing.dRow() * tip;
        double bx = px - facing.dCol() * head * 1.4;
        double by = py - facing.dRow() * head * 1.4;
        double ox = facing.dRow() * head;
        double oy = facing.dCol() * head;
        g.setStroke(ARROW);
        g.setLineWidth(Math.max(1.5, size * 0.035));
        g.setLineCap(StrokeLineCap.ROUND);
        g.strokeLine(sx, sy, bx, by);
        g.setFill(ARROW);
        g.fillPolygon(new double[]{px, bx + ox, bx - ox}, new double[]{py, by + oy, by - oy}, 3);
    }

    /** Вампус: круглое тело, глаза и зубы. */
    static void wumpus(GraphicsContext g, double cx, double cy, double size, boolean alive) {
        double r = size * 0.28;
        g.setFill(alive ? WUMPUS : DEAD);
        g.fillOval(cx - r, cy - r, 2 * r, 2 * r);
        g.setFill(Color.WHITE);
        double e = size * 0.06;
        g.fillOval(cx - size * 0.12 - e, cy - size * 0.08 - e, 2 * e, 2 * e);
        g.fillOval(cx + size * 0.12 - e, cy - size * 0.08 - e, 2 * e, 2 * e);
        double ty = cy + size * 0.1;
        for (int i = -1; i <= 1; i++) {
            double tx = cx + i * size * 0.08;
            g.fillPolygon(new double[]{tx - size * 0.035, tx + size * 0.035, tx},
                    new double[]{ty, ty, ty + size * 0.07}, 3);
        }
        if (!alive) {
            g.setStroke(Color.WHITE);
            g.setLineWidth(Math.max(1, size * 0.03));
            for (int s : new int[]{-1, 1}) {
                double ex = cx + s * size * 0.12;
                double ey = cy - size * 0.08;
                g.strokeLine(ex - e, ey - e, ex + e, ey + e);
                g.strokeLine(ex - e, ey + e, ex + e, ey - e);
            }
        }
    }

    static void pit(GraphicsContext g, double cx, double cy, double size, Color color) {
        double rx = size * 0.32;
        double ry = size * 0.26;
        g.setFill(color);
        g.fillOval(cx - rx, cy - ry, 2 * rx, 2 * ry);
    }

    static void gold(GraphicsContext g, double cx, double cy, double size) {
        double r = size * 0.2;
        g.setFill(GOLD);
        g.fillOval(cx - r, cy - r, 2 * r, 2 * r);
        g.setStroke(GOLD_EDGE);
        g.setLineWidth(Math.max(1, size * 0.03));
        g.strokeOval(cx - r * 0.6, cy - r * 0.6, 1.2 * r, 1.2 * r);
    }

    /** Запах: три волнистые линии вверх. */
    static void stench(GraphicsContext g, double x, double y, double s) {
        g.setStroke(STENCH);
        g.setLineWidth(Math.max(1, s * 0.09));
        g.setLineCap(StrokeLineCap.ROUND);
        for (int i = 0; i < 3; i++) {
            double lx = x + s * (0.2 + 0.3 * i);
            g.beginPath();
            g.moveTo(lx, y + s);
            g.bezierCurveTo(lx - s * 0.15, y + s * 0.66, lx + s * 0.15, y + s * 0.33, lx, y);
            g.stroke();
        }
    }

    /** Ветерок: две изогнутые линии. */
    static void breeze(GraphicsContext g, double x, double y, double s) {
        g.setStroke(BREEZE);
        g.setLineWidth(Math.max(1, s * 0.09));
        g.setLineCap(StrokeLineCap.ROUND);
        for (int i = 0; i < 2; i++) {
            double ly = y + s * (0.3 + 0.4 * i);
            g.beginPath();
            g.moveTo(x, ly);
            g.bezierCurveTo(x + s * 0.4, ly - s * 0.2, x + s * 0.6, ly + s * 0.2, x + s, ly - s * 0.1);
            g.stroke();
        }
    }

    /** Блеск: четырёхлучевая звёздочка. */
    static void glitter(GraphicsContext g, double x, double y, double s) {
        double cx = x + s / 2;
        double cy = y + s / 2;
        double a = s / 2;
        double b = s * 0.12;
        g.setFill(GOLD);
        g.fillPolygon(new double[]{cx, cx + b, cx + a, cx + b, cx, cx - b, cx - a, cx - b},
                new double[]{cy - a, cy - b, cy, cy + b, cy + a, cy + b, cy, cy - b}, 8);
    }
}
