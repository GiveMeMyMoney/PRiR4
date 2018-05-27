import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class PMO_Test_CommandsFactory implements PMO_LogSource {
    private final PMO_Test_Board board;
    private final PMO_Test_PlayersInfo playersInfo;
    private final AtomicBoolean continuationFlag;
    private final PMO_Test_ShipsDetection detection;

    public PMO_Test_CommandsFactory(PMO_Test_Board board, PMO_Test_PlayersInfo playersInfo,
                                    PMO_Test_ShipsDetection detection,
                                    AtomicBoolean continuationFlag ) {
        this.playersInfo = playersInfo;
        this.board = board;
        this.detection = detection;
        this.continuationFlag = continuationFlag;
    }

    private void isAliveTest( PMO_Test_Ship ship ) {
        if ( ! ship.isAlive() )
            throw new RuntimeException( "Nie można zmienić położenia wraku " + ship );
    }

    private boolean insideBoardTest( GameInterface.Position position ) {
        int col = position.getCol();
        int row = position.getRow();

        return (col >= 0) && (row >= 0) &&
                (row != GameInterface.HIGHT) && (col != GameInterface.WIDTH);
    }

    private void testContinue() {
        if ( ! board.canContinue() ) {
            continuationFlag.set(false); // koniec gry !
            log( "Flaga kontynuacji gry ustawiona na false");
        }
    }

    public Runnable moveShipCommand( int player, int shipID )  {
        return () -> {
            PMO_Test_Ship ship = board.getShip( player, shipID );

            isAliveTest( ship );

            GameInterface.Position nextPosition = ship.getCourse().next( ship.getPosition() );

            if ( ! insideBoardTest( nextPosition ) ) {
                board.sink(ship);
                log( "Okręt " + ship + " poza planszą");
                testContinue();
                playersInfo.getInfoObject(player).shipOutOfBounds.set(true);
                throw new RuntimeException( "Okręt " + ship + " utracony - wyszedł poza planszę" );
            }

            // czy położenie jest wolne
            if ( ! board.isEmpty( player, nextPosition ) ) {
                // czym jest zajęte?
                PMO_Test_Ship otherShip = board.getShip( player, nextPosition );

                board.sink(ship);
                board.sink(otherShip);

                log( "Doszło do kolizji pomiędzy okrętami " + ship.getPosition() + " vs " +
                        otherShip.getVirtualPosition() );
                if ( otherShip.getOwner() == player ) {
                    // staranowanie wlasnej jednostki
                    log( "Doszło do kolizji z własną jednostką gracza " + player );
                    playersInfo.getInfoObject(player).collision.set(true);
                }
                testContinue();
            } else {
                board.move( ship );
                detection.spotMove(player, ship );
                log( "Okręt na nowej pozycji " + ship );
            }
        };
    }

    public Runnable turn( int player, int shipID, boolean left ) {
        return () -> {
            PMO_Test_Ship ship = board.getShip(player, shipID);

            isAliveTest(ship);

            if ( left ) {
                ship.turnLeft();
                log("Okręt po zmianie kursu w lewo " + ship );
            } else {
                ship.turnRight();
                log( "Okręt po zmianie kursu w prawo " + ship );
            }
        };
    }

    public Supplier<Boolean> fire(int player, int shipID, GameInterface.Position target ) {
        return () -> {
            PMO_Test_Ship ship = board.getShip(player, shipID);

            isAliveTest(ship);

            boolean result;
            PMO_Test_PlayerInfo info = playersInfo.getInfoObject(player);

            // czy cel znajduje się na planszy?
            if ( ! insideBoardTest( target ) ) {
                log( "Cel poza planszą " + target );
                info.targetOutOfBounds.set( true );
                return false;
            }

            // czy strzał był celny?
            if ( ! board.isEmpty( player, target ) ) {
                // co trafiono?
                PMO_Test_Ship otherShip = board.getShip( player, target );

                board.sink(otherShip);

                log( "Okręt " + ship + " zatopił " + otherShip  );
                // czyj był to okręt?
                if ( otherShip.getOwner() == player ) {
                    // zniszczenie wlasnej jednostki
                    log( "Zatopiono własną jednostką gracza " + player );
                    info.friendlyFire.set(true);
                }
                testContinue();
                result = true;
            } else {
                log( "Okręt " + ship + " oddał niecelny strzał na pozycję " + target );
                result = false;
            }
            detection.spotFire(player, ship );
            return result;
        };
    }
}
