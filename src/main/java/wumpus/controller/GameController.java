package wumpus.controller;

import wumpus.agent.Hunter;
import wumpus.model.Cave;
import wumpus.model.CaveGenerator;
import wumpus.model.CaveSpec;
import wumpus.model.World;

import java.util.Objects;

/**
 * Управление игрой для окна: старт, пауза, шаг вывода, сначала, новая пещера, пример из задания.
 * Не знает ничего про JavaFX — окно только вызывает методы и рисует состояние,
 * поэтому всю логику кнопок можно проверить обычными тестами.
 * <p>
 * Время приходит снаружи через {@link #tick(long)} (окно вызывает его на каждом кадре),
 * а контроллер сам решает, сколько действий сделать, исходя из скорости.
 */
public class GameController {

    public static final int MIN_SPEED = 1;
    public static final int MAX_SPEED = 50;
    public static final int DEFAULT_SPEED = 4;

    private static final long NANOS_PER_SECOND = 1_000_000_000L;

    private CaveSpec spec = CaveSpec.defaults();
    private Cave cave;
    private World world;
    private Hunter hunter;
    private boolean takeRisks = true;
    private boolean running;
    private int speed = DEFAULT_SPEED;
    /** Время прошлого кадра; пока его нет (сразу после старта), первый кадр только запоминает время. */
    private boolean hasLastTick;
    private long lastTick;
    /** Накопленное время × скорость: целая часть от деления на секунду — сколько действий пора сделать. */
    private long actionDebt;

    /** Начинает с пещеры из задания. */
    public GameController() {
        this(Cave.pdfExample());
    }

    public GameController(Cave cave) {
        this.cave = Objects.requireNonNull(cave, "cave");
        restart();
    }

    /** Кнопка «Сначала»: та же пещера, охотник ничего не знает. */
    public void restart() {
        pause();
        world = new World(cave);
        hunter = new Hunter(cave.size(), takeRisks);
    }

    /** Окно «Новая пещера». */
    public void newCave(CaveSpec newSpec) {
        spec = Objects.requireNonNull(newSpec, "spec");
        cave = CaveGenerator.generate(newSpec);
        restart();
    }

    /** Кнопка «Пример из задания». */
    public void pdfExample() {
        cave = Cave.pdfExample();
        restart();
    }

    /** Галочка «рисковать»: меняет поведение охотника, поэтому игра начинается сначала. */
    public void setTakeRisks(boolean takeRisks) {
        this.takeRisks = takeRisks;
        restart();
    }

    public boolean isTakeRisks() {
        return takeRisks;
    }

    /** Одно действие охотника. */
    private void act() {
        world.execute(hunter.next(world.percept()));
        if (world.over()) {
            pause();
        }
    }

    /**
     * Кнопка «Шаг»: один шаг вывода, как в задании. Охотник измеряет, делает вывод, решает —
     * и выполняет решение целиком (доходит до цели, стреляет...). Ставит на паузу.
     */
    public void stepOnce() {
        pause();
        if (world.over()) {
            return;
        }
        do {
            act();
        } while (!world.over() && hunter.planned() > 0);
    }

    public void start() {
        if (running || world.over()) {
            return;
        }
        running = true;
        hasLastTick = false;
        actionDebt = 0;
    }

    public void pause() {
        running = false;
    }

    /** Одна кнопка «Старт/Пауза». */
    public void toggleRunning() {
        if (running) {
            pause();
        } else {
            start();
        }
    }

    public boolean isRunning() {
        return running;
    }

    /** Очередной кадр окна. nowNanos — текущее время в наносекундах. */
    public void tick(long nowNanos) {
        if (!running) {
            return;
        }
        if (!hasLastTick || nowNanos < lastTick) {
            hasLastTick = true;
            lastTick = nowNanos;
            return;
        }
        // больше секунды за кадр не учитываем: иначе после долгого «замерзания» набежит гигантский долг
        long elapsed = Math.min(nowNanos - lastTick, NANOS_PER_SECOND);
        lastTick = nowNanos;
        actionDebt += elapsed * speed;
        long due = actionDebt / NANOS_PER_SECOND;
        actionDebt %= NANOS_PER_SECOND;
        for (long i = 0; i < due && running; i++) {
            act();
        }
    }

    /** Скорость в действиях в секунду; значения вне диапазона прижимаются к границе. */
    public void setSpeed(int actionsPerSecond) {
        speed = Math.clamp(actionsPerSecond, MIN_SPEED, MAX_SPEED);
    }

    public int getSpeed() {
        return speed;
    }

    public CaveSpec spec() {
        return spec;
    }

    public Cave cave() {
        return cave;
    }

    public World world() {
        return world;
    }

    public Hunter hunter() {
        return hunter;
    }
}
