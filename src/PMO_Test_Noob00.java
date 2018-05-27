import javax.xml.ws.Holder;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.stream.IntStream;

public class PMO_Test_Noob00 {
    private long playerID;
    private GameInterface game;
    private String PLAYER_NAME = "Noob00";

    {
        try {
            game = (GameInterface)LocateRegistry.getRegistry().lookup("GAME");
            playerID = game.register(PLAYER_NAME);

            System.out.println("Gracz " + PLAYER_NAME + " zarejestrowany ID " + playerID );

            game.waitForStart(playerID);

        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (NotBoundException e) {
            e.printStackTrace();
        }
    }

    private void doNothing() throws RemoteException {
        int ships = game.getNumberOfAvaiablewarships(playerID);
        Holder<Boolean> canContinue = new Holder<>();
        do {

            canContinue.value = false;
            IntStream.range(0, ships).forEach( s -> {
                try {
                    boolean alive = game.isAlive(playerID,s);
                    if ( alive ) canContinue.value = true;
                    System.out.println( "Okręt #" + s + " stan " + alive );
                } catch (RemoteException e) {
                    Runtime.getRuntime().halt(0);
                }
            });
            PMO_TimeHelper.sleep( 500 );

        } while ( canContinue.value );
    }

    public static void main(String[] args) throws RemoteException {
        PMO_Test_Noob00 pl = new PMO_Test_Noob00();
        pl.doNothing();
    }
}
