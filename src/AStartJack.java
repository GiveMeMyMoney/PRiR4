import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

enum Direction {
    RIGHT,
    LEFT
}

class Start2 {
    private long playerID;
    private GameInterface game;
    private String PLAYER_NAME = "JacekZawodny";
    private int availableShips;
    private AtomicInteger warshipNum = new AtomicInteger(0);
    private Set<GameInterface.Position> currentPositions = Collections.synchronizedSet(new HashSet<>());
    private ConcurrentHashMap<Integer, GameInterface.PositionAndCourse> warshipDetails = new ConcurrentHashMap<>();
    private BlockingQueue<GameInterface.PositionAndCourse> foundedEnemyDetails = new LinkedBlockingQueue<>(400);
//    private ArrayList<Thread> threadArrayList = new ArrayList<>();

    {
        try {
            game = (GameInterface) LocateRegistry.getRegistry().lookup("GAME");
            playerID = game.register(PLAYER_NAME);
//            System.out.println("Gracz " + PLAYER_NAME + " zarejestrowany ID " + playerID );

            availableShips = game.getNumberOfAvaiablewarships(playerID);
            for (int warshipNumber = 0; warshipNumber < availableShips; warshipNumber++) {
                GameInterface.Position position = game.getPosition(playerID, warshipNumber);
                GameInterface.Course course = game.getCourse(playerID, warshipNumber);
                warshipDetails.put(warshipNumber, new GameInterface.PositionAndCourse(position, course));
                currentPositions.add(position);
//                System.out.println("Zarejestrowano statek nr " + warshipNumber);
            }
            game.waitForStart(playerID);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (NotBoundException e) {
            e.printStackTrace();
        }
    }

    private static boolean getRandomBoolean() {
        return Math.random() < 0.5;
    }

    private void turnInDirection(Direction direction, int warshipId) throws RemoteException {
        if (direction == Direction.LEFT) {
            GameInterface.PositionAndCourse warshipDet = warshipDetails.get(warshipId);
            GameInterface.PositionAndCourse newPosAndCou = new GameInterface.PositionAndCourse(warshipDet.getPosition(), warshipDet.getCourse().afterTurnToLeft());
            game.turnLeft(playerID, warshipId);
            warshipDetails.put(warshipId, newPosAndCou);
        } else if (direction == Direction.RIGHT) {
            GameInterface.PositionAndCourse warshipDet = warshipDetails.get(warshipId);
            GameInterface.PositionAndCourse newPosAndCou = new GameInterface.PositionAndCourse(warshipDet.getPosition(), warshipDet.getCourse().afterTurnToRight());
            game.turnRight(playerID, warshipId);
            warshipDetails.put(warshipId, newPosAndCou);
        }
    }

    private synchronized GameInterface.Position positionToMove(int warshipId, GameInterface.PositionAndCourse warshipPosAndCour) throws RemoteException {
        GameInterface.Position position = warshipPosAndCour.getPosition();
        GameInterface.Position newPosition;
        int nextCoordinate;
        switch (warshipPosAndCour.getCourse()) {
            case EAST:
                nextCoordinate = position.getCol() + 1;
                newPosition = new GameInterface.Position(nextCoordinate, position.getRow());
                if (nextCoordinate < GameInterface.WIDTH && !currentPositions.contains(newPosition)) {
                    currentPositions.add(newPosition);
                    warshipDetails.put(warshipId, new GameInterface.PositionAndCourse(newPosition, warshipPosAndCour.getCourse()));
                    return newPosition;
                }
                break;
            case WEST:
                nextCoordinate = position.getCol() - 1;
                newPosition = new GameInterface.Position(nextCoordinate, position.getRow());
                if (nextCoordinate >= 0 && !currentPositions.contains(newPosition)) {
                    currentPositions.add(newPosition);
                    warshipDetails.put(warshipId, new GameInterface.PositionAndCourse(newPosition, warshipPosAndCour.getCourse()));
                    return newPosition;
                }
                break;
            case NORTH:
                nextCoordinate = position.getRow() + 1;
                newPosition = new GameInterface.Position(position.getCol(), nextCoordinate);
                if (nextCoordinate < GameInterface.HIGHT && !currentPositions.contains(newPosition)) {
                    currentPositions.add(newPosition);
                    warshipDetails.put(warshipId, new GameInterface.PositionAndCourse(newPosition, warshipPosAndCour.getCourse()));
                    return newPosition;
                }
                break;
            case SOUTH:
                nextCoordinate = position.getRow() - 1;
                newPosition = new GameInterface.Position(position.getCol(), nextCoordinate);
                if (nextCoordinate >= 0 && !currentPositions.contains(newPosition)) {
                    currentPositions.add(newPosition);
                    warshipDetails.put(warshipId, new GameInterface.PositionAndCourse(newPosition, warshipPosAndCour.getCourse()));
                    return newPosition;
                }
                break;
            default:
                throw new IllegalStateException();
        }
        return null;
    }

    private void fire(int warshipId, GameInterface.Position enemyPosition) throws RemoteException {
        game.fire(playerID, warshipId, enemyPosition);
    }

    private boolean moveWarshipIntoCourse(int warshipId, GameInterface.PositionAndCourse warshipPosAndCour) throws RemoteException {
//        System.out.println("MOVE WARSHIP! #" + warshipId);
        if (positionToMove(warshipId, warshipPosAndCour) != null) {
            game.move(playerID, warshipId);
            currentPositions.remove(warshipPosAndCour.getPosition());
            return true;
        } else {
            return false;
        }
    }

    private void fight() throws RemoteException {
        // nasluchiwanie get message
        new Thread(() -> {
            while (true) {
                try {
                    GameInterface.PositionAndCourse enemyWarship = game.getMessage(playerID);
                    if (!currentPositions.contains(enemyWarship.getPosition())) {
//                        System.out.println("Enemy founded");
                        foundedEnemyDetails.add(new GameInterface.PositionAndCourse(enemyWarship.getPosition(), enemyWarship.getCourse()));
                    }
                } catch (RemoteException e) {
//                    System.out.println("Get Message exception!");
                }
            }
        }).start();

        for (int warshipNumber = 0; warshipNumber < availableShips; warshipNumber++) {
//            System.out.println("Utwórz wątek nr " + warshipNumber);
            new Thread(() -> {
                int localShipNumber = warshipNum.getAndIncrement();
                while (true) {
//                    System.out.println("FIGHT!");
                    try {
                        if (game.isAlive(playerID, localShipNumber)) {
//                            System.out.println("Ship number " + localShipNumber + " is Alive");
//                            System.out.println("Local Ship ID is: " + localShipNumber);
//                            System.out.println("Ship #" + localShipNumber + " should move");
                            if (foundedEnemyDetails.size() > 0) {
                                GameInterface.PositionAndCourse enemyDetails = foundedEnemyDetails.poll();
                                fire(localShipNumber, enemyDetails.getPosition());
                            }
                            if (!moveWarshipIntoCourse(localShipNumber, warshipDetails.get(localShipNumber))) {
//                                System.out.println("Ship #" + localShipNumber + " should turn");
                                if (getRandomBoolean()) {
                                    turnInDirection(Direction.LEFT, localShipNumber);
                                } else {
                                    turnInDirection(Direction.RIGHT, localShipNumber);
                                }
                            }
                        } else {
                            GameInterface.PositionAndCourse posAndCour = warshipDetails.get(localShipNumber);
                            currentPositions.remove(posAndCour.getPosition());
                            break;
                        }
                    } catch (RemoteException e) {

                    }
                }
            }).start();
        }
    }

    public static void main(String[] args) throws RemoteException {
        Start2 player = new Start2();
        player.fight();
    }
}