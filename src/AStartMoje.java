import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.*;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;

class AvailableEnemyPosition {
    Integer id;
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
        return positions.poll();
    }

    public AvailableEnemyPosition(Integer id, GameInterface.PositionAndCourse positionAndCourse) {
        this.id = id;
        this.positionAndCourse = positionAndCourse;

        initAllAvailablePositionToShot();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AvailableEnemyPosition that = (AvailableEnemyPosition) o;

        return id != null ? id.equals(that.id) : that.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "AvailableEnemyPosition{" +
                "id=" + id +
                ", positionAndCourse=" + positionAndCourse +
                ", positions=" + positions +
                '}';
    }
}


class Client {
    private Object prepareForShotHelper = new Object();
    private AtomicInteger enemyDetectedId = new AtomicInteger(1);
    //region stale
    private GameInterface GAME; //static?
    private long playerId;
    private static final String PLAYER_NAME = "MarcinStyczen";
    private int MAX_AVAILABLE_SHIPS_COUNT; //static?
    //endregion

    private ConcurrentHashMap<Integer, GameInterface.PositionAndCourse> warshipsCoordinateMap = new ConcurrentHashMap<>();
    //trzeba wsp�ldzieli� liste z pozycjami wszystkich statk�w - ograniczenie staranowania swojego
    private Set<GameInterface.Position> allWarshipsPositionSet = Collections.synchronizedSet(new HashSet<>()); //* It is imperative that the user manually synchronize on the returned set when iterating over it:
    //gdzie MA doleciec pocisk (co trwa jakis czas) - tam nie moge sie ruszyc
    private Set<GameInterface.Position> allFirePositionSet = Collections.synchronizedSet(new HashSet<>());
    //wszystkie waykryte pozycje wroga, najpierw bierzemy najabrdziej aktaulne i w glab
    private BlockingDeque<AvailableEnemyPosition> enemyDetectedWarshipDeque = new LinkedBlockingDeque<>();

    private List<Runnable> warshipGarrisionThreadList = new ArrayList<>();

    // ---- WarshipGarrision
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

        private void turn(GameInterface.Course actualCourse, boolean init) {
            switch (actualCourse) {
                case WEST:
                    changeCourse(this.warhsipId, EDirectionToMove.RIGHT);
                    break;
                case EAST:
                    changeCourse(this.warhsipId, EDirectionToMove.LEFT);
                    break;
                case NORTH:
                    if (!init) {
                        changeCourse(this.warhsipId, EDirectionToMove.LEFT);
                    }
                    break;
                default:
                    //obojetne bo i tak zmieni w dobra strone
                    changeCourse(this.warhsipId, EDirectionToMove.LEFT);
            }
        }

        private void moveAhead() {
            //MOVE
            try {
                GAME.move(playerId, warhsipId);
            } catch (RemoteException e) {
                System.out.println("prepareToMoveShip ex: " + e.getMessage());
            }
        }

        @Override
        public void run() {
            //ustawWszystkie
            GameInterface.Course course = warshipsCoordinateMap.get(warhsipId).getCourse();
            turn(course, true);
            //course = warshipsCoordinateMap.get(warhsipId).getCourse();
            //turn(course, true);

            while (checkIsAlive()) {
                //jest jaki� statek to strzelaj!
                if (!enemyDetectedWarshipDeque.isEmpty()) {
                    //biore ostatnio wykryty
                    GameInterface.Position positionTarget = null;
                    //AvailableEnemyPosition availableEnemyPosition;
                    //synchronized (prepareForShotHelper) {
                    AvailableEnemyPosition availableEnemyPosition = enemyDetectedWarshipDeque.peekLast();
                    if (availableEnemyPosition != null) {
                        positionTarget = availableEnemyPosition.pollNextPosition();
                        //jezeli pozycje wszystkie ostrzelane to usun
                        if (positionTarget == null || availableEnemyPosition.positions.isEmpty()) {
                            //TODO sprawdzic
                            //usuwam jesli to ostatni mozliwy strzal
                            enemyDetectedWarshipDeque.removeLastOccurrence(availableEnemyPosition);
                        }
                    }
                    //}
                    //strzelam jesli && nie moge strzelic jezeli tam jest moj statek...
                    if (availableEnemyPosition != null && positionTarget != null && !allWarshipsPositionSet.contains(positionTarget)) {
                        try {
                            allFirePositionSet.add(positionTarget);
                            boolean hitHurra = GAME.fire(playerId, warhsipId, positionTarget);
                            //jezeli trafiony to usun z detected
                            if (hitHurra) {
                                //System.out.println("SHIPid: " + warhsipId + " enemyDetectedWarshipDeque: " + availableEnemyPosition.toString() + " \n +++++" + " enemyDetectedWarshipDeque: " + enemyDetectedWarshipDeque.toString());
                                enemyDetectedWarshipDeque.removeLastOccurrence(availableEnemyPosition);
                                //System.out.println("SHIPid: " + warhsipId + "enemyDetectedWarshipDeque AFTER: " + availableEnemyPosition.toString() + " \n +++++" + " enemyDetectedWarshipDeque: " + enemyDetectedWarshipDeque.toString());
                            }
                            allFirePositionSet.remove(positionTarget);
                        } catch (RemoteException e) {
                            System.out.println("fire ex: " + e.getMessage());
                        }
                    }
                } //poruszam sie
                //else {
                    /*if (warhsipId == 1 || warhsipId == 2 || warhsipId == 3 || warhsipId == 4 || warhsipId == 5) {
                        GameInterface.Course course = warshipsCoordinateMap.get(warhsipId).getCourse();
                        if (GameInterface.Course.NORTH == course) {
                            boolean canMove = canMoveWarshipToPosition(warhsipId, null, true);
                            if (canMove) {
                                moveAhead();
                            } else {
                                turn();
                            }
                        }
                    }*/
                //if (getRandomForMove()) {
                //rusz jezeli mozesz
                boolean canMove = canMoveWarshipToPosition(warhsipId, null, true);
                //statek nie moze ruszyc sprobuj obrocic w poprawna strone.
                if (canMove) {
                    moveAhead();
                } else {
                    GameInterface.Course course2 = warshipsCoordinateMap.get(warhsipId).getCourse();
                    turn(course2, false);
                }
                    /*} else {
                        turn();
                    }*/
                //}
            }

            //System.out.println("enemyDetectedWarshipDeque: " + enemyDetectedWarshipDeque.toString());
            //statek wrak :(
            cleanWreck();
        }
    }

    // ---- Radar
    class Radar implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {

                    GameInterface.PositionAndCourse enemyDetectedWarship = GAME.getMessage(playerId);
                    //if (enemyDetectedWarship != null) {
                    AvailableEnemyPosition availableEnemyPosition = new AvailableEnemyPosition(enemyDetectedId.getAndIncrement(), enemyDetectedWarship);
                    enemyDetectedWarshipDeque.add(availableEnemyPosition);
                    //}
                } catch (RemoteException e) {
                    System.out.println("Radar ex: " + e.getMessage());
                }
            }
        }
    }

    private void prepareForStorm() throws RemoteException {
        for (int shipId = 0; shipId < MAX_AVAILABLE_SHIPS_COUNT; shipId++) {
            GameInterface.Position position = GAME.getPosition(playerId, shipId);
            //TODO
            warshipsCoordinateMap.put(shipId, new GameInterface.PositionAndCourse(position, GAME.getCourse(playerId, shipId)));
            allWarshipsPositionSet.add(position);

            //nowy watek nie odpalany - WarshipGarrision
            warshipGarrisionThreadList.add(new WarshipGarrision(shipId));
        }
    }

    //region RANDOM
    private static boolean getRandomForMove() {
        return Math.random() < 0.70; //wieksze prawodpodobienstwo
    }

    //endregion

    private void init(String IP) throws RemoteException, NotBoundException {
        //"11.11.11.11" -> to IP w ARGS ?
        GAME = (GameInterface) LocateRegistry.getRegistry(IP).lookup("GAME");
        playerId = GAME.register(PLAYER_NAME);
        MAX_AVAILABLE_SHIPS_COUNT = GAME.getNumberOfAvaiablewarships(playerId);
    }

    public Client(String IP) {
        try {
            init(IP);
            prepareForStorm();
            GAME.waitForStart(playerId);

            //start Radaru
            new Thread(new Radar()).start();
            //startStorm wystartuj wszystkei watki
            warshipGarrisionThreadList.forEach(runnable -> new Thread(runnable).start());
        } catch (RemoteException | NotBoundException e) {
            System.out.println("Client() ex: " + e.getMessage());
        }
    }

    //region MOVE
    private void prepareToMoveShip(int shipId, GameInterface.Position positionFrom, GameInterface.Position positionTo, GameInterface.Course course) {
        //TODO czy trzeba to syncrhonized? nie zapetli sie?
        allWarshipsPositionSet.add(positionTo);
        allWarshipsPositionSet.remove(positionFrom);

        //aktualizacja danych dla statku
        warshipsCoordinateMap.put(shipId, new GameInterface.PositionAndCourse(positionTo, course));
    }

    private synchronized boolean canMoveWarshipToPosition(int shipId, GameInterface.Course course, boolean moveIfCan) {
        GameInterface.PositionAndCourse positionAndCourse = warshipsCoordinateMap.get(shipId);
        GameInterface.Position positionFrom = positionAndCourse.getPosition();
        if (course == null) {
            course = positionAndCourse.getCourse();
        }

        if (GameInterface.Course.WEST == course) {
            GameInterface.Position positionTo = new GameInterface.Position(positionFrom.getCol() - 1, positionFrom.getRow());
            //nie wyjdzie poza mape ani nie zderzy sie z innym swoim statkiem ani nie wejdzie w pole do strzelania
            if (positionTo.getCol() >= 0 && !allWarshipsPositionSet.contains(positionTo) && !allFirePositionSet.contains(positionTo)) {
                if (moveIfCan) {
                    prepareToMoveShip(shipId, positionFrom, positionTo, course);
                }
                return true;
            }
        } else if (GameInterface.Course.EAST == course) {
            GameInterface.Position positionTo = new GameInterface.Position(positionFrom.getCol() + 1, positionFrom.getRow());
            //nie wyjdzie poza mape ani nie zderzy sie z innym swoim statkiem
            if (positionTo.getCol() < GameInterface.WIDTH && !allWarshipsPositionSet.contains(positionTo) && !allFirePositionSet.contains(positionTo)) {
                if (moveIfCan) {
                    prepareToMoveShip(shipId, positionFrom, positionTo, course);
                }
                return true;
            }
        } else if (GameInterface.Course.SOUTH == course) {
            GameInterface.Position positionTo = new GameInterface.Position(positionFrom.getCol(), positionFrom.getRow() - 1);
            //nie wyjdzie poza mape ani nie zderzy sie z innym swoim statkiem
            if (positionTo.getRow() >= 0 && !allWarshipsPositionSet.contains(positionTo) && !allFirePositionSet.contains(positionTo)) {
                if (moveIfCan) {
                    prepareToMoveShip(shipId, positionFrom, positionTo, course);
                }
                return true;
            }
        } else if (GameInterface.Course.NORTH == course) {
            GameInterface.Position positionTo = new GameInterface.Position(positionFrom.getCol(), positionFrom.getRow() + 1);
            //nie wyjdzie poza mape ani nie zderzy sie z innym swoim statkiem
            if (positionTo.getRow() < GameInterface.HIGHT && !allWarshipsPositionSet.contains(positionTo) && !allFirePositionSet.contains(positionTo)) {
                if (moveIfCan) {
                    prepareToMoveShip(shipId, positionFrom, positionTo, course);
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

class Start {

    public static void main(String[] args) {
        Client client = new Client(args[0]);
    }
}
