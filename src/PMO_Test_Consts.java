public class PMO_Test_Consts {
    // czas od spot do przekazania informacji przeciwnikowi
    public static final long BEFORE_SPOT_INFO_DELAY = (long)(GameInterface.MOVE_DELAY_HALF * 2 * 0.97);

    // czas pomiedzy testami zwykłego spotowania (okręt nie strzela i nie porusza się)
    public static final long ALL_TO_ALL_SPOT_DELAY = GameInterface.MOVE_DELAY_HALF * 2;

    public static final int SHIPS_INITIAL_ROW_MAX = 5;

    public static final int PARALLEL_CALL_EXPECTED = ( GameInterface.WIDTH * 2 ) / 3;

    // maksymalne opóźnienie w uruchomieniu zadania
    public static final long MAX_JOB_DELAY = 50;

    // maksymalna liczba salw w grze
    public static final int SALVO = 150;

    // maksymalny czas trawania gry
    public static final long MAX_GAME_TIME = GameInterface.THE_LULL_BEFORE_THE_STORM +
            GameInterface.FIRE_DELAY_HALF * 2 * SALVO;
}
