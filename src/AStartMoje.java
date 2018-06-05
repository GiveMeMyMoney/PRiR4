import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingDeque;

class AvailableEnemyPosition {
    GameInterface.PositionAndCourse positionAndCourse;
    ConcurrentLinkedQueue<GameInterface.Position> positions = new ConcurrentLinkedQueue<>();

    private void initAllAvailablePositionToShot() {
        GameInterface.Position position = positionAndCourse.getPosition();
        GameInterface.Course course = positionAndCourse.getCourse();
        positions.add(position);
        if (course == GameInterface.Course.EAST) {
            GameInterface.Position position1 = new GameInterface.Position(position.getCol() - 1, position.getRow());
            if (position1.getCol() >= 0) positions.add(position1);
            GameInterface.Position position2 = new GameInterface.Position(position.getCol() - 2, position.getRow());
            if (position2.getCol() >= 0) positions.add(position2);
            GameInterface.Position position3 = new GameInterface.Position(position.getCol(), position.getRow() - 1);
            if (position3.getRow() >= 0) positions.add(position3);
            GameInterface.Position position4 = new GameInterface.Position(position.getCol(), position.getRow() + 1);
            if (position4.getRow() < GameInterface.HIGHT) positions.add(position4);

        } else if (course == GameInterface.Course.WEST) {
            GameInterface.Position position1 = new GameInterface.Position(position.getCol() + 1, position.getRow());
            if (position1.getCol() < GameInterface.WIDTH) positions.add(position1);
            GameInterface.Position position2 = new GameInterface.Position(position.getCol() + 2, position.getRow());
            if (position2.getCol() < GameInterface.WIDTH) positions.add(position2);
            GameInterface.Position position3 = new GameInterface.Position(position.getCol(), position.getRow() - 1);
            if (position3.getRow() >= 0) positions.add(position3);
            GameInterface.Position position4 = new GameInterface.Position(position.getCol(), position.getRow() + 1);
            if (position4.getRow() < GameInterface.HIGHT) positions.add(position4);

        } else if (course == GameInterface.Course.NORTH) {
            GameInterface.Position position1 = new GameInterface.Position(position.getCol(), position.getRow() - 1);
            if (position1.getRow() >= 0) positions.add(position1);
            GameInterface.Position position2 = new GameInterface.Position(position.getCol(), position.getRow() - 2);
            if (position2.getRow() >= 0) positions.add(position2);
            GameInterface.Position position3 = new GameInterface.Position(position.getCol() - 1, position.getRow());
            if (position3.getCol() >= 0) positions.add(position3);
            GameInterface.Position position4 = new GameInterface.Position(position.getCol() + 1, position.getRow());
            if (position4.getCol() < GameInterface.WIDTH) positions.add(position4);

        } else if (course == GameInterface.Course.SOUTH) {
            GameInterface.Position position1 = new GameInterface.Position(position.getCol(), position.getRow() + 1);
            if (position1.getRow() < GameInterface.HIGHT) positions.add(position1);
            GameInterface.Position position2 = new GameInterface.Position(position.getCol(), position.getRow() + 2);
            if (position2.getRow() < GameInterface.HIGHT) positions.add(position2);
            GameInterface.Position position3 = new GameInterface.Position(position.getCol() + 1, position.getRow());
            if (position3.getCol() < GameInterface.WIDTH) positions.add(position3);
            GameInterface.Position position4 = new GameInterface.Position(position.getCol() - 1, position.getRow());
            if (position4.getCol() >= 0) positions.add(position4);

        }
    }

    public GameInterface.Position pollNextPosition() {
        positions.poll();
    }

    public AvailableEnemyPosition(GameInterface.PositionAndCourse positionAndCourse) {
        this.positionAndCourse = positionAndCourse;

        initAllAvailablePositionToShot();
    }
}


class Client {
    private Object prepareForShotHelper = new Object();
    //region stale
    private GameInterface GAME; //static?
    private long playerId;
    private static final String PLAYER_NAME = "MarcinStyczen";
    private int MAX_AVAILABLE_SHIPS_COUNT; //static?
    //endregion

    private ConcurrentHashMap<Integer, GameInterface.PositionAndCourse> warshipsCoordinateMap = new ConcurrentHashMap<>();
    //trzeba wspóldzieliæ liste z pozycjami wszystkich statków - ograniczenie staranowania swojego
    private Set<GameInterface.Position> allWarshipsPositionSet = Collections.synchronizedSet(new HashSet<>()); //* It is imperative that the user manually synchronize on the returned set when iterating over it:
    //gdzie MA doleciec pocisk (co trwa jakis czas) - tam nie moge sie ruszyc
    private Set<GameInterface.Position> allFirePositionSet = Collections.synchronizedSet(new HashSet<>());
    //wszystkie waykryte pozycje wroga, najpierw bierzemy najabrdziej aktaulne i w glab
    private BlockingDeque<AvailableEnemyPosition> enemyDetectedWarshipDeque = new LinkedBlockingDeque<>();

    //private AtomicInteger warshipIdCounter = new AtomicInteger(0);

    private void prepareForStorm() throws RemoteException {
        for (int shipIdx = 0; shipIdx < MAX_AVAILABLE_SHIPS_COUNT; shipIdx++) {
            GameInterface.Position position = GAME.getPosition(playerId, shipIdx);
            //TODO
            warshipsCoordinateMap.put(shipIdx, new GameInterface.PositionAndCourse(position, GAME.getCourse(playerId, shipIdx)));
            allWarshipsPositionSet.add(position);

            //nowy watek nie odpalany
        }
    }

    class WarshipGarrision implements Runnable {
        int warhsipId;

        public WarshipGarrision(int warhsipId) {
            this.warhsipId = warhsipId;
        }

        private boolean checkIsAlive() {
            try {
                return GAME.isAlive(playerId, warhsipId);
            } catch (RemoteException e) {
                System.out.println("checkIsAlive ex: " + e.getMessage());
                return false;
            }
        }

        private void cleanWreck() {
            GameInterface.PositionAndCourse positionAndCourseOfWreck = warshipsCoordinateMap.get(warhsipId);
            allWarshipsPositionSet.remove(positionAndCourseOfWreck);
            warshipsCoordinateMap.remove(warhsipId);
        }

        @Override
        public void run() {
            while (checkIsAlive()) {
                //jest jakiœ statek to strzelaj!
                if (!enemyDetectedWarshipDeque.isEmpty()) {
                    //biore ostatnio wykryty
                    synchronized (prepareForShotHelper) {
                        AvailableEnemyPosition availableEnemyPosition = enemyDetectedWarshipDeque.peekLast();
                        GameInterface.Position position = availableEnemyPosition.pollNextPosition();

                    }
                }

            }

            //statek wrak
            cleanWreck();
        }
    }

    private void init() throws RemoteException, NotBoundException {
        GAME = (GameInterface) LocateRegistry.getRegistry().lookup("GAME");
        playerId = GAME.register(PLAYER_NAME);
        MAX_AVAILABLE_SHIPS_COUNT = GAME.getNumberOfAvaiablewarships(playerId);
    }

    public Client() throws RemoteException, NotBoundException {
        init();
        prepareForStorm();
        GAME.waitForStart(playerId);

        //startStrom()
    }

    //region MOVE
    private void moveShip(int shipId, GameInterface.Position positionFrom, GameInterface.Position positionTo, GameInterface.Course course) throws RemoteException {
        //TODO czy trzeba to syncrhonized? nie zapetli sie?
        synchronized (allWarshipsPositionSet) {
            allWarshipsPositionSet.add(positionTo);
            allWarshipsPositionSet.remove(positionFrom);
        }
        //aktualizacja danych dla statku
        warshipsCoordinateMap.put(shipId, new GameInterface.PositionAndCourse(positionTo, course));
        //MOVE
        GAME.move(playerId, shipId);
    }

    private void moveWarshipToPosition(int shipId) throws RemoteException {
        GameInterface.PositionAndCourse positionAndCourse = warshipsCoordinateMap.get(shipId);
        GameInterface.Position positionFrom = positionAndCourse.getPosition();
        GameInterface.Course course = positionAndCourse.getCourse();

        //TODO w IFACH jeszcze posytionFire!!!!!!!!!!!!!!!!!!!
        if (GameInterface.Course.WEST == course) {
            GameInterface.Position positionTo = new GameInterface.Position(positionFrom.getCol() - 1, positionFrom.getRow());
            //nie wyjdzie poza mape ani nie zderzy sie z innym swoim statkiem
            if (positionTo.getCol() >= 0 && !allWarshipsPositionSet.contains(positionTo)) {
                moveShip(shipId, positionFrom, positionTo, course);
            }
        } else if (GameInterface.Course.EAST == course) {
            GameInterface.Position positionTo = new GameInterface.Position(positionFrom.getCol() + 1, positionFrom.getRow());
            //nie wyjdzie poza mape ani nie zderzy sie z innym swoim statkiem
            if (positionTo.getCol() < GameInterface.WIDTH && !allWarshipsPositionSet.contains(positionTo)) {
                moveShip(shipId, positionFrom, positionTo, course);
            }
        } else if (GameInterface.Course.SOUTH == course) {
            GameInterface.Position positionTo = new GameInterface.Position(positionFrom.getCol(), positionFrom.getRow() - 1);
            //nie wyjdzie poza mape ani nie zderzy sie z innym swoim statkiem
            if (positionTo.getRow() >= 0 && !allWarshipsPositionSet.contains(positionTo)) {
                moveShip(shipId, positionFrom, positionTo, course);
            }
        } else if (GameInterface.Course.NORTH == course) {
            GameInterface.Position positionTo = new GameInterface.Position(positionFrom.getCol(), positionFrom.getRow() + 1);
            //nie wyjdzie poza mape ani nie zderzy sie z innym swoim statkiem
            if (positionTo.getRow() < GameInterface.HIGHT && !allWarshipsPositionSet.contains(positionTo)) {
                moveShip(shipId, positionFrom, positionTo, course);
            }
        }
    }
    //endregion

    //region ChangeCOURSE
    enum EDirectionToMove {
        RIGHT, LEFT;
    }

    private void changeCourse(int shipId, EDirectionToMove directionToMove) throws RemoteException {
        GameInterface.PositionAndCourse actualPositionAndCourse = warshipsCoordinateMap.get(shipId);
        if (EDirectionToMove.LEFT.equals(directionToMove)) {
            GameInterface.Course newCourse = actualPositionAndCourse.getCourse().afterTurnToLeft();
            warshipsCoordinateMap.put(shipId, new GameInterface.PositionAndCourse(actualPositionAndCourse.getPosition(), newCourse));
            GAME.turnLeft(playerId, shipId);
        } else if (EDirectionToMove.RIGHT.equals(directionToMove)) {
            GameInterface.Course newCourse = actualPositionAndCourse.getCourse().afterTurnToRight();
            warshipsCoordinateMap.put(shipId, new GameInterface.PositionAndCourse(actualPositionAndCourse.getPosition(), newCourse));
            GAME.turnRight(playerId, shipId);
        }
    }

    //endregion


}


class StartMoje {


    public static void main(String[] args) {
        // write your code here
    }
}
