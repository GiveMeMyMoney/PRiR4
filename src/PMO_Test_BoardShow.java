import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.List;
import java.util.stream.IntStream;

public class PMO_Test_BoardShow {

    private List<List<PMO_Test_Ship>> ships;
    private final static int MARGIN = 5;
    private final static int SEPARATION = 12;
    private final static String SEA = ".";
    private PMO_Test_Ship[][][] shipsTable;
    private StringBuilder sb ;

    private void playerShips2table( List<PMO_Test_Ship> ships, PMO_Test_Ship[][] shipsTable ) {
        ships.stream().filter( s -> s.isAlive()).forEach( s ->
                shipsTable
                        [ s.getPosition().getCol() ]
                        [ s.getPosition().getRow() ] = s );
    }

    private void virtualPlayerShips2table(List<PMO_Test_Ship> ships, PMO_Test_Ship[][] shipsTable ) {
        ships.stream().filter( s -> s.isAlive()).forEach( s ->
                shipsTable
                        [ s.getVirtualPosition().getCol() ]
                        [ s.getVirtualPosition().getRow() ] = s );
    }

    private void ships2table() {
        shipsTable = new PMO_Test_Ship[2][GameInterface.WIDTH][GameInterface.HIGHT];
        playerShips2table(ships.get(0), shipsTable[0]);
        playerShips2table(ships.get(1), shipsTable[1]);
        virtualPlayerShips2table(ships.get(0), shipsTable[1]);
        virtualPlayerShips2table(ships.get(1), shipsTable[0]);
    }

    private void addSpace( int marginSize ) {
        IntStream.range(0,marginSize).forEach(i->sb.append(" "));
    }

    private void addPlayerRow( int player, int row ) {
        IntStream.range(0,GameInterface.WIDTH).forEach( i -> {
                    if (shipsTable[player][i][row] == null) {
                        sb.append( SEA );
                    } else {
                        PMO_Test_Ship ship = shipsTable[player][i][row];

                        if ( ship.getOwner() == player )
                            sb.append( ship.getCourse() );
                        else
                            sb.append( ship.getVirtualCourse() );
                    }
                }
        );
    }

    public void setShips( List<List<PMO_Test_Ship>> ships ) {
        this.ships = ships;
        ships2table();
    }

    public String generate() {
        sb = new StringBuilder();

        IntStream.range(0,GameInterface.HIGHT).forEach( i -> {
                    int row = GameInterface.HIGHT - i - 1;
                    addSpace( MARGIN );
                    addPlayerRow(0,row);
                    addSpace( SEPARATION );
                    addPlayerRow(1,row);
                    sb.append('\n');
                }
        );

        return sb.toString();
    }

    public static void main(String[] args) throws RemoteException, NotBoundException {
        PMO_Test_ShipsAccess access = (PMO_Test_ShipsAccess)
                LocateRegistry.getRegistry().lookup("SHIPS");

        PMO_SystemOutRedirect.println( "access = " + access );
        PMO_Test_BoardShow show = new PMO_Test_BoardShow();

        Thread th = new Thread( () -> {
           do {
               try {
                   show.setShips( access.getShips( "superTajneHaslo"));
                   PMO_SystemOutRedirect.println( show.generate() );
               } catch (RemoteException e) {
                   Runtime.getRuntime().halt(0);
               }
               PMO_TimeHelper.sleep( GameInterface.DELAY );
           } while ( true );
        });
        th.start();
    }

}
