package wumpus.controller;

/**
 * Перевод положения слайдера скорости в действия в секунду и обратно.
 * Шкала логарифмическая: медленные скорости, удобные для разбора, получают половину слайдера.
 */
public final class SpeedScale {

    public static final double SLIDER_MIN = 0;
    public static final double SLIDER_MAX = 100;

    private static final double LOG_MIN = Math.log(GameController.MIN_SPEED);
    private static final double LOG_MAX = Math.log(GameController.MAX_SPEED);

    private SpeedScale() {
    }

    /** Положение слайдера → скорость. */
    public static int toSpeed(double slider) {
        double t = (slider - SLIDER_MIN) / (SLIDER_MAX - SLIDER_MIN);
        long speed = Math.round(Math.exp(LOG_MIN + t * (LOG_MAX - LOG_MIN)));
        // За краями слайдера exp даёт почти 0 или бесконечность, NaN округляется в 0 — всё прижимается к диапазону
        return (int) Math.clamp(speed, GameController.MIN_SPEED, GameController.MAX_SPEED);
    }

    /** Скорость → положение слайдера. */
    public static double toSlider(int speed) {
        int clamped = Math.clamp(speed, GameController.MIN_SPEED, GameController.MAX_SPEED);
        return SLIDER_MIN + (Math.log(clamped) - LOG_MIN) / (LOG_MAX - LOG_MIN) * (SLIDER_MAX - SLIDER_MIN);
    }
}
