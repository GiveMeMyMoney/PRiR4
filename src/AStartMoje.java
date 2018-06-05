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

            //nowy watek nie odpalany - WarshipGarrision
        }
    }

    //region RANDOM
    private static boolean getRandomForMove() {
        return Math.random() < 0.7; //wieksze prawodpodobienstwo
    }

    //endregion

    //TODO osobny watek do getMesseage!!!!!!!!!!!!!!!

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

        private void turn() {
            if (Math.random() < 0.5) {
                changeCourse(this.warhsipId, EDirectionToMove.LEFT);
            } else {
                changeCourse(this.warhsipId, EDirectionToMove.RIGHT);
            }
        }

        @Override
        public void run() {
            while (checkIsAlive()) {
                //jest jakiœ statek to strzelaj!
                if (!enemyDetectedWarshipDeque.isEmpty()) {
                    //biore ostatnio wykryty
                    GameInterface.Position positionTarget;
                    synchronized (prepareForShotHelper) {
                        AvailableEnemyPosition availableEnemyPosition = enemyDetectedWarshipDeque.peekLast();
                        positionTarget = availableEnemyPosition.pollNextPosition();
                        //jezeli pozycje wszystkie ostrzelane to usun
                        if (positionTarget == null || availableEnemyPosition.positions.isEmpty()) {
                            //TODO sprawdzic
                            //usuwam jesli to ostatni mozliwy strzal
                            enemyDetectedWarshipDeque.removeLastOccurrence(availableEnemyPosition);
                        }
                    }
                    //strzelam jesli && nie moge strzelic jezeli tam jest moj statek...
                    if (positionTarget != null && !allWarshipsPositionSet.contains(positionTarget)) {
                        try {
                            allFirePositionSet.add(positionTarget);
                            GAME.fire(playerId, warhsipId, positionTarget);
                            allFirePositionSet.remove(positionTarget);
                        } catch (RemoteException e) {
                            System.out.println("fire ex: " + e.getMessage());
                        }
                    }
                } //poruszam sie
                else {
                    if (getRandomForMove()) {
                        //rusz jezeli mozesz
                        boolean canMove = canMoveWarshipToPosition(warhsipId, null, true);
                        //statek nie moze ruszyc sprobuj obrocic
                        if (!canMove) {
                            turn();
                        }
                    } else {
                        turn();
                    }
                }
            }

            //statek wrak :(
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

        //startStrom() - wystartowac wszystkie przygotowane watki...
    }

    //region MOVE
    private void moveShip(int shipId, GameInterface.Position positionFrom, GameInterface.Position positionTo, GameInterface.Course course) {
        //TODO czy trzeba to syncrhonized? nie zapetli sie?
        synchronized (allWarshipsPositionSet) {
            allWarshipsPositionSet.add(positionTo);
            allWarshipsPositionSet.remove(positionFrom);
        }
        //aktualizacja danych dla statku
        warshipsCoordinateMap.put(shipId, new GameInterface.PositionAndCourse(positionTo, course));
        //MOVE
        try {
            GAME.move(playerId, shipId);
        } catch (RemoteException e) {
            System.out.println("moveShip ex: " + e.getMessage());
        }
    }

    private boolean canMoveWarshipToPosition(int shipId, GameInterface.Course course, boolean moveIfCan) {
        GameInterface.PositionAndCourse positionAndCourse = warshipsCoordinateMap.get(shipId);
        GameInterface.Position positionFrom = positionAndCourse.getPosition();
        if (course == null) {
            course = positionAndCourse.getCourse();
        }

        //TODO w IFACH jeszcze posytionFire!!!!!!!!!!!!!!!!!!!
        if (GameInterface.Course.WEST == course) {
            GameInterface.Position positionTo = new GameInterface.Position(positionFrom.getCol() - 1, positionFrom.getRow());
            //nie wyjdzie poza mape ani nie zderzy sie z innym swoim statkiem ani nie wejdzie w pole do strzelania
            if (positionTo.getCol() >= 0 && !allWarshipsPositionSet.contains(positionTo) && !allFirePositionSet.contains(positionTo)) {
                if (moveIfCan) {
                    moveShip(shipId, positionFrom, positionTo, course);
                }
                return true;
            }
        } else if (GameInterface.Course.EAST == course) {
            GameInterface.Position positionTo = new GameInterface.Position(positionFrom.getCol() + 1, positionFrom.getRow());
            //nie wyjdzie poza mape ani nie zderzy sie z innym swoim statkiem
            if (positionTo.getCol() < GameInterface.WIDTH && !allWarshipsPositionSet.contains(positionTo) && !allFirePositionSet.contains(positionTo)) {
                if (moveIfCan) {
                    moveShip(shipId, positionFrom, positionTo, course);
                }
                return true;
            }
        } else if (GameInterface.Course.SOUTH == course) {
            GameInterface.Position positionTo = new GameInterface.Position(positionFrom.getCol(), positionFrom.getRow() - 1);
            //nie wyjdzie poza mape ani nie zderzy sie z innym swoim statkiem
            if (positionTo.getRow() >= 0 && !allWarshipsPositionSet.contains(positionTo) && !allFirePositionSet.contains(positionTo)) {
                if (moveIfCan) {
                    moveShip(shipId, positionFrom, positionTo, course);
                }
                return true;
            }
        } else if (GameInterface.Course.NORTH == course) {
            GameInterface.Position positionTo = new GameInterface.Position(positionFrom.getCol(), positionFrom.getRow() + 1);
            //nie wyjdzie poza mape ani nie zderzy sie z innym swoim statkiem
            if (positionTo.getRow() < GameInterface.HIGHT && !allWarshipsPositionSet.contains(positionTo) && !allFirePositionSet.contains(positionTo)) {
                if (moveIfCan) {
                    moveShip(shipId, positionFrom, positionTo, course);
                }
                return true;
            }
        }
        return false;
    }
    //endregion

    //region ChangeCOURSE
    enum EDirectionToMove {
        RIGHT, LEFT;
    }

    //bez sensu zeby np. skrecal w lewo jak jest przy scianie
    private void changeCourse(int shipId, EDirectionToMove directionToMove) {
        GameInterface.PositionAndCourse actualPositionAndCourse = warshipsCoordinateMap.get(shipId);
        try {
            if (EDirectionToMove.LEFT.equals(directionToMove)) {
                GameInterface.Course newCourse = actualPositionAndCourse.getCourse().afterTurnToLeft();
                boolean canMoveAfterTurn = canMoveWarshipToPosition(shipId, newCourse, false);
                if (canMoveAfterTurn) {
                    warshipsCoordinateMap.put(shipId, new GameInterface.PositionAndCourse(actualPositionAndCourse.getPosition(), newCourse));
                    GAME.turnLeft(playerId, shipId);
                } else {
                    newCourse = actualPositionAndCourse.getCourse().afterTurnToRight();
                    warshipsCoordinateMap.put(shipId, new GameInterface.PositionAndCourse(actualPositionAndCourse.getPosition(), newCourse));
                    GAME.turnRight(playerId, shipId);
                }

            } else if (EDirectionToMove.RIGHT.equals(directionToMove)) {
                GameInterface.Course newCourse = actualPositionAndCourse.getCourse().afterTurnToRight();
                boolean canMoveAfterTurn = canMoveWarshipToPosition(shipId, newCourse, false);
                if (canMoveAfterTurn) {
                    warshipsCoordinateMap.put(shipId, new GameInterface.PositionAndCourse(actualPositionAndCourse.getPosition(), newCourse));
                    GAME.turnRight(playerId, shipId);
                } else {
                    newCourse = actualPositionAndCourse.getCourse().afterTurnToLeft();
                    warshipsCoordinateMap.put(shipId, new GameInterface.PositionAndCourse(actualPositionAndCourse.getPosition(), newCourse));
                    GAME.turnLeft(playerId, shipId);
                }
            }
        } catch (RemoteException e) {
            System.out.println("changeCourse ex: " + e.getMessage());
        }
    }

    //endregion
}


class StartMoje {


    public static void main(String[] args) {
        // write your code here
    }
}
