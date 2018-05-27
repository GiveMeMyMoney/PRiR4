import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class PMO_Test_ShipsDetection implements PMO_LogSource {

    private final PMO_Test_Board board;
    private final static int SPOT_DISTANCE_IN_REST_SQ = (int)(GameInterface.SPOT_DISTANCE_IN_REST *
            GameInterface.SPOT_DISTANCE_IN_REST);
    private final static int SPOT_DISTANCE_IN_MOTION_SQ = (int)( GameInterface.SPOT_DISTANCE_IN_MOTION *
            GameInterface.SPOT_DISTANCE_IN_MOTION );
    private final static int SPOT_DISTANCE_SHOOTING_SQ = (int)( GameInterface.SPOT_DISTANCE_SHOOTING *
            GameInterface.SPOT_DISTANCE_SHOOTING );
    private final List<BlockingQueue<GameInterface.PositionAndCourse>> queues;
    private final AtomicBoolean continuationFlag;
    private final PMO_Test_JobsExecutor executor;

    public PMO_Test_ShipsDetection(PMO_Test_Board board,
                                   PMO_Test_JobsExecutor executor,
                                   AtomicBoolean continuationFlag ) {
        this.board = board;
        this.continuationFlag = continuationFlag;
        this.executor = executor;
        queues = new ArrayList<>(2);
        queues.add(0,new LinkedBlockingDeque<>());
        queues.add(1,new LinkedBlockingDeque<>());
    }

    public void normalSpot() {
        List<PMO_Test_Ship> ships0 = board.getAliveShips( 0 );
        List<PMO_Test_Ship> ships1 = board.getAliveShips( 1 );

        if ( ships0.size() == 0 || ships1.size() == 0 ) return;

        ships0.forEach( s -> {
            for ( PMO_Test_Ship ship : ships1 ) {
                if ( PMO_Test_Transformations.distanceSQ( s.getPosition(), ship.getVirtualPosition() ) <=
                        SPOT_DISTANCE_IN_REST_SQ ) {
                    s.spotted( queues.get(1));
                    ship.spotted( queues.get(0));
                }
            }
        });
    }

    private void spot(int player, PMO_Test_Ship ship, int distance ) {
        int opponent = PMO_Test_Transformations.otherPlayer(player);
        List<PMO_Test_Ship> ships = board.getAliveShips(opponent);
        if (ships.size() == 0) return;

        if (ships.stream().anyMatch(s ->
                PMO_Test_Transformations.distanceSQ(s.getPosition(), ship.getVirtualPosition()) <=
                        distance)) {
            // informacja o okręcie trafi do przeciwnika
            ship.spotted( queues.get(opponent));
        }
    }

    public void spotMove(int player, PMO_Test_Ship ship) {
        spot( player, ship, SPOT_DISTANCE_IN_MOTION_SQ );
    }

    public void spotFire(int player, PMO_Test_Ship ship) {
        spot( player, ship, SPOT_DISTANCE_SHOOTING_SQ );
    }

    public void startPeriodicalSpotCheck() {
        Thread th = PMO_ThreadsHelper.createThreadAndRegister( () -> {
            log( "Uruchomiono wątek odpowiedzialny za okresowe wprowadzania zadania detekcji okrętów");
            while ( continuationFlag.get() ) {
                PMO_TimeHelper.sleep( PMO_Test_Consts.ALL_TO_ALL_SPOT_DELAY );
                log( "--- spot ---");
                executor.supply( new PMO_Test_JobsExecutor.Job( "detekcja nieruchomych okrętów",
                        this::normalSpot ) );
            }
        });
        th.start();
    }

    public BlockingQueue<GameInterface.PositionAndCourse> getQueue( int player ) {
        return queues.get(player);
    }
}
