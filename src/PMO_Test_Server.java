import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class PMO_Test_Server implements GameInterface, PMO_LogSource {

    private final Map<Long,Integer> playerID2localID = new HashMap<>();
    private final Map<Integer,Long> localID2playerID = new HashMap<>();
    private final Map<Integer,String> localID2PlayerName = new HashMap<>();
    private final PMO_Test_PlayersInfo playersInfo = new PMO_Test_PlayersInfo();
    private final PMO_Test_Board board = new PMO_Test_Board();
    private final PMO_Test_JobsExecutor executor = new PMO_Test_JobsExecutor();
    private final AtomicBoolean continuationFlag = new AtomicBoolean( true );
    private final PMO_Test_ShipsDetection detector =
            new PMO_Test_ShipsDetection(board,executor,continuationFlag);
    private final PMO_Test_CommandsFactory factory =
            new PMO_Test_CommandsFactory(board,playersInfo,detector,continuationFlag);
    private final AtomicBoolean canFire = new AtomicBoolean( false );
    private final Random rnd = new Random();
    private final CyclicBarrier cb;

    {
        long id = rnd.nextLong();
        long id2;

        do {
            id2 = rnd.nextLong();
        } while ( id2 == id );

        playerID2localID.put(id,0);
        playerID2localID.put(id2,1);

        localID2playerID.put(0,id);
        localID2playerID.put(1,id2);

        executor.setContinuationFlag( continuationFlag );

        cb = new CyclicBarrier(3, () -> {
            log( "Obaj gracze wykonali waitForStart. Rozpoczęto odliczanie THE_LULL_BEFORE_THE_STORM");
            detector.startPeriodicalSpotCheck();
            executor.startJobsExecutor();
        });
        PMO_ThreadsHelper.createThreadAndRegister( new GameTimeout() ).start();
    }

    private class GameTimeout implements Runnable {
        @Override
        public void run() {
            log( "Uruchomiono wątek kontrolujący czas gry");
            PMO_ThreadsHelper.wait(cb);
            log( "Do rozpoczęcia ostrzału pozostaje: " + GameInterface.THE_LULL_BEFORE_THE_STORM
                    + " do końca gry: " + PMO_Test_Consts.MAX_GAME_TIME );
            PMO_TimeHelper.sleep( GameInterface.THE_LULL_BEFORE_THE_STORM );
            long end = System.currentTimeMillis() + PMO_Test_Consts.MAX_GAME_TIME;
            canFire.set(true);
            log( "Można strzelać" );
            do {
                PMO_TimeHelper.sleep( 1000 );
                log("-- MARK --");
            } while ( ( end > System.currentTimeMillis() ) && continuationFlag.get() );
            continuationFlag.set(false);
            PMO_TimeHelper.sleep(500);

            PMO_SystemOutRedirect.println( "---------------------------------");

            String name0 = localID2PlayerName.get( 0 );
            String name1 = localID2PlayerName.get( 1 );

            boolean test0 = playersInfo.getInfoObject(0 ).test( name0 );
            boolean test1 = playersInfo.getInfoObject(1 ).test( name1 );

            int r0 = board.getNumberOfShips(0);
            int r1 = board.getNumberOfShips(1);

            PMO_SystemOutRedirect.println( "Player 0 " + name0 + " pozostało " + r0 + " jednostek");
            PMO_SystemOutRedirect.println( "Player 1 " + name1 + " pozostało " + r1 + " jednostek");

            if ( r0 == r1 ) {
                PMO_SystemOutRedirect.println( "REMIS");
            }
            if ( r0 > r1 ) {
                PMO_SystemOutRedirect.println( "WYGRANA gracza " + name0 );
                if ( ! test0 ) {
                    PMO_SystemOutRedirect.println( "UWAGA GRACZ NIE ZALICZYŁ TESTU" );
                }
            }
            if ( r0 < r1 ) {
                PMO_SystemOutRedirect.println( "WYGRANA gracza " + name1 );
                if ( ! test1 ) {
                    PMO_SystemOutRedirect.println( "UWAGA GRACZ NIE ZALICZYŁ TESTU" );
                }
            }

            PMO_ThreadsHelper.shutdown();
        }
    }

    private Optional<Integer> getLocalID(long playerID ) {

        if ( ! playerID2localID.containsKey( playerID )) {
            log( "Zarejestrowano użycie błędnego playerID " + playerID );
            return Optional.empty();
        }

        int localID = playerID2localID.get( playerID );

        playersInfo.getInfoObject(localID).totalMethodCalls.incrementAndGet();

        return Optional.of( localID );
    }

    private int getLocalIDOrException( long playerID ) throws RemoteException {
        Optional<Integer> localID = getLocalID( playerID );
        if ( ! localID.isPresent() ) {
            throw new RemoteException( "Blad: nieznane playerID");
        }
        return localID.get();
    }

    private String getPlayerName( int localID ) {
        return localID2PlayerName.get( localID );
    }

    private PMO_Test_Ship getShipOrExceptionIfWreck( int localID, int warshipID ) throws RemoteException {
        PMO_Test_Ship ship = getShip(localID,warshipID);

        if ( ! ship.isAlive() ) {
            throw new RemoteException( "Okręt zatopiony");
        }

        return ship;
    }

    private PMO_Test_Ship getShip( int localID, int warshipID ) throws RemoteException {
        PMO_Test_Ship ship = board.getShip(localID, warshipID);

        if ( ship == null ) {
            log( "Gracz " + getPlayerName( localID ) + " użył błędnego warshipID " + warshipID );
            throw new RemoteException( "Blędny identyfikator okrętu" );
        }

        return ship;
    }


    private PMO_Test_PlayerInfo getPlayerInfoAndIncrementCallsCounter( int localID ) {
        PMO_Test_PlayerInfo info = playersInfo.getInfoObject(localID);
        info.totalMethodCalls.incrementAndGet();
        return info;
    }

    @Override
    public long register(String playerName) throws RemoteException {

        long result;
        synchronized (this) {
            int localid = localID2PlayerName.size();
            result = localID2playerID.get(localid);
            localID2PlayerName.put(localid,playerName);
            log( "Zarejestrowano gracza " + playerName + " jako player " + localid );
        }

        return result;
    }

    @Override
    public void waitForStart(long playerID) throws RemoteException {

        Optional<Integer> localID = getLocalID( playerID );

        if ( ! localID.isPresent() ) return;

        String name = getPlayerName( localID.get() );
        log( "Gracz " + name + " uruchomił waitForStart");

        PMO_ThreadsHelper.wait( cb );

        log( "Gracz " + name + " zakończył waitForStart");
    }

    @Override
    public int getNumberOfAvaiablewarships(long playerID) throws RemoteException {
        int localID = getLocalIDOrException( playerID );
        getPlayerInfoAndIncrementCallsCounter( localID );
        return board.getNumberOfShips( localID );
    }

    @Override
    public Course getCourse(long playerID, int warshipID) throws RemoteException {
        int localID = getLocalIDOrException( playerID );
        getPlayerInfoAndIncrementCallsCounter( localID );
        return getShipOrExceptionIfWreck(localID,warshipID).getCourse();
    }

    @Override
    public Position getPosition(long playerID, int warshipID) throws RemoteException {
        int localID = getLocalIDOrException( playerID );
        getPlayerInfoAndIncrementCallsCounter( localID );
        return getShipOrExceptionIfWreck(localID,warshipID).getPosition();
    }

    @Override
    public boolean isAlive(long playerID, int warshipID) throws RemoteException {
        int localID = getLocalIDOrException( playerID );
        getPlayerInfoAndIncrementCallsCounter( localID );
        return getShip(localID,warshipID).isAlive();
    }

    private void rethrowException( Optional<Exception> object2check ) throws RemoteException {
        if ( object2check.isPresent() ) {
            log( "rethrowException wygeneruje wyjątek " + object2check.get().toString() );
            throw new RuntimeException( object2check.get() );
        }
    }

    private void turn( long playerID, int warshipID, boolean left ) throws RemoteException {
        int localID = getLocalIDOrException( playerID );
        PMO_Test_Ship ship = getShipOrExceptionIfWreck(localID,warshipID);
        PMO_Test_PlayerInfo info = getPlayerInfoAndIncrementCallsCounter(localID);

        try {
            ship.getLock().lock();
            int parallelOps = info.parallelOperations.incAndStoreMax();
            log( "Gracz " + localID + " zlecił turn jako " + parallelOps + " współbieżną operację");

            PMO_TimeHelper.sleep( GameInterface.TURN_DELAY_HALF );
            PMO_Test_JobsExecutor.Job<?> job;
            if ( left ) {
                job = new PMO_Test_JobsExecutor.Job( "turnLeft", factory.turn(localID,warshipID,true));
            } else {
                job = new PMO_Test_JobsExecutor.Job( "turnRight", factory.turn(localID,warshipID,false));
            }
            executor.supply( job );

            PMO_TimeHelper.sleep( GameInterface.TURN_DELAY_HALF );

            rethrowException( job.getException() );
        } finally {
            ship.getLock().unlock();
            info.parallelOperations.dec();
        }
    }

    @Override
    public void turnLeft(long playerID, int warshipID) throws RemoteException {
        turn( playerID, warshipID, true );
    }

    @Override
    public void turnRight(long playerID, int warshipID) throws RemoteException {
        turn( playerID, warshipID, false );
    }

    @Override
    public void move(long playerID, int warshipID) throws RemoteException {
        int localID = getLocalIDOrException( playerID );
        PMO_Test_Ship ship = getShipOrExceptionIfWreck(localID,warshipID);
        PMO_Test_PlayerInfo info = getPlayerInfoAndIncrementCallsCounter(localID);

        try {
            ship.getLock().lock();

            int parallelOps = info.parallelOperations.incAndStoreMax();
            log( "Gracz " + localID + " zlecił move jako " + parallelOps + " współbieżną operację");

            PMO_TimeHelper.sleep( GameInterface.MOVE_DELAY_HALF );

            PMO_Test_JobsExecutor.Job<?> job = new PMO_Test_JobsExecutor.Job( "move",
                    factory.moveShipCommand(localID,warshipID) );
            executor.supply( job );

            PMO_TimeHelper.sleep( GameInterface.MOVE_DELAY_HALF );

            rethrowException( job.getException() );
        } finally {
            ship.getLock().unlock();
            info.parallelOperations.dec();
        }
    }

    @Override
    public boolean fire(long playerID, int warshipID, Position target) throws RemoteException {
        if ( ! canFire.get() ) return false;
        int localID = getLocalIDOrException( playerID );
        PMO_Test_Ship ship = getShipOrExceptionIfWreck(localID,warshipID);
        PMO_Test_PlayerInfo info = getPlayerInfoAndIncrementCallsCounter(localID);
        log( "Gracz " + localID + " strzał na pozycję " + target );

        AtomicReference<Boolean> result = new AtomicReference<>();
        result.set(false);

        try {
            ship.getLock().lock();

            int parallelOps = info.parallelOperations.incAndStoreMax();
            log( "Gracz " + localID + " zlecił fire jako " + parallelOps + " współbieżną operację");

            PMO_TimeHelper.sleep( GameInterface.MOVE_DELAY_HALF );

            PMO_Test_JobsExecutor.Job<Boolean> job = new PMO_Test_JobsExecutor.Job( "fire",
                    factory.fire(localID,warshipID,target ), result );
            executor.supply( job );

            PMO_TimeHelper.sleep( GameInterface.MOVE_DELAY_HALF );

            while ( ! job.done() ) {
                PMO_TimeHelper.sleep( GameInterface.MOVE_DELAY_HALF / 20 );
            }

            rethrowException( job.getException() );
        } finally {
            ship.getLock().unlock();
            info.parallelOperations.dec();
        }

        return result.get();
    }

    @Override
    public PositionAndCourse getMessage(long playerID) throws RemoteException {
        int localID = getLocalIDOrException( playerID );
        getPlayerInfoAndIncrementCallsCounter(localID);

        try {
            return detector.getQueue(localID).take();
        } catch (InterruptedException e) {
            throw new RemoteException( "Przechwycono InterruptedException w trakcie take() - user " +
                    localID);
        }
    }

    public static void main(String[] args) throws RemoteException {
        PMO_Test_Server server = new PMO_Test_Server();

        Registry registry = LocateRegistry.createRegistry(Registry.REGISTRY_PORT);

        PMO_SystemOutRedirect.println( "registry");

        registry.rebind( "GAME", UnicastRemoteObject.exportObject(server,0) );
        PMO_SystemOutRedirect.println( "Zarejestrowano GAME");

        registry.rebind( "SHIPS", UnicastRemoteObject.exportObject(server.board, 0 ));
        PMO_SystemOutRedirect.println( "Zarejestrowano SHIPS");
    }
}
