import java.rmi.RemoteException;
import java.util.*;
import java.util.stream.Collectors;

public class PMO_Test_Board implements PMO_LogSource, PMO_Test_ShipsAccess {

    // Mapa numer gracza -> flota, po zainicjowaniu read-only
    private final List<List<PMO_Test_Ship>> ships;

    // tablica referencji do statków - pozwala szybko sprawdzić, czy dana pozycja
    // jest zajęta i czym
    private final PMO_Test_Ship[][][] shipsTable;

    private final PMO_Test_ShipsPositionsGenerator generator =
            new PMO_Test_ShipsPositionsGenerator();

    public PMO_Test_Board() {
        ships = new ArrayList<>(2);
        ships.add(0, new ArrayList<>( GameInterface.WIDTH ));
        ships.add(1, new ArrayList<>( GameInterface.WIDTH ));
        shipsTable = new PMO_Test_Ship[2][GameInterface.WIDTH][GameInterface.HIGHT];
        generateShips();
    }

    private void generateShips( int player ) {
        List<GameInterface.PositionAndCourse> pc = generator.generate();

        pc.forEach( s -> {
            PMO_Test_Ship ship = new PMO_Test_Ship(s.getPosition(),s.getCourse(), player );
            log( "Utworzono nowy okręt: " + ship );
            ships.get(player).add( ship );
            setShipOnTables(player,ship);
        });
    }

    private void generateShips() {
        generateShips(0);
        generateShips(1);
    }

    public boolean isEmpty( int player, GameInterface.Position position ) {
        return isEmpty( player, position.getCol(), position.getRow() );
    }

    public boolean isEmpty( int player, int col, int row ) {
        return shipsTable[player][ col][ row ] == null;
    }

    public PMO_Test_Ship getShip( int player, GameInterface.Position position ) {
        return getShip( player, position.getCol(), position.getRow() );
    }

    public PMO_Test_Ship getShip( int player, int col, int row ) {
        return shipsTable[player][ col][ row ];
    }

    public PMO_Test_Ship getShip( int player, int shipID ) {
        return ships.get(player).get(shipID);
    }

    public List<PMO_Test_Ship> getShips( int player ) {
        return ships.get(player);
    }

    @Override
    public List<List<PMO_Test_Ship>> getShips(String password) throws RemoteException {
        if ( ! password.equals( "superTajneHaslo" ) ) return null;
        return ships;
    }

    public int getNumberOfShips(int player ) {
        return (int)ships.get(player).stream().filter( s -> s.isAlive() ).count();
    }

    public boolean canContinue() {
        return ( getNumberOfShips(0) > 0 ) && ( getNumberOfShips( 1 ) > 0 );
    }

    public List<PMO_Test_Ship> getAliveShips( int player ) {
        return ships.get(player).stream().filter( s -> s.isAlive() ).collect(Collectors.toList());
    }

    private void setShipOnTables( int player, PMO_Test_Ship ship ) {
        shipsTable[player][ship.getPosition().getCol()][ship.getPosition().getRow()]
                = ship;
        shipsTable[PMO_Test_Transformations.otherPlayer(player)]
                [ship.getVirtualPosition().getCol()]
                [ship.getVirtualPosition().getRow()]
                = ship;
    }

    private void clearShipOnTable( int player, GameInterface.Position position ) {
        shipsTable[player][position.getCol()][position.getRow()] = null;
    }

    private void clearShipOnTables( int player, GameInterface.Position position ) {
        clearShipOnTable( player, position );
        clearShipOnTable( PMO_Test_Transformations.otherPlayer(player),
                PMO_Test_Transformations.real2virtual(position) );
    }

    private void clearShipOnTables( int player, PMO_Test_Ship ship ) {
        clearShipOnTable( player, ship.getPosition() );
        clearShipOnTable( PMO_Test_Transformations.otherPlayer(player),
                ship.getVirtualPosition() );
    }

    public void move( PMO_Test_Ship ship ) {
        clearShipOnTables(ship.getOwner(), ship.getPosition());
        ship.move();
        setShipOnTables(ship.getOwner(), ship);
    }

    public void sink( PMO_Test_Ship ship ) {
        clearShipOnTables(ship.getOwner(), ship );
        ship.destroy();
    }

}
