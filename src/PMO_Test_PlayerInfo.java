import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class PMO_Test_PlayerInfo {
    public final AtomicBoolean collision = new AtomicBoolean( false );
    public final AtomicBoolean friendlyFire = new AtomicBoolean( false );
    public final AtomicBoolean shipOutOfBounds = new AtomicBoolean( false );
    public final AtomicBoolean targetOutOfBounds = new AtomicBoolean( false );
    public final PMO_AtomicCounter parallelOperationsMax = PMO_CountersFactory.createCommonMaxStorageCounter();
    public final PMO_AtomicCounter parallelOperations = PMO_CountersFactory.createCounterWithMaxStorageSet();
    public final AtomicInteger totalMethodCalls = new AtomicInteger(0 );

    private boolean singleTest( AtomicBoolean flag, String txt ) {
        PMO_SystemOutRedirect.print( "> " +  txt + " : " );
        if ( flag.get() ) {
            PMO_SystemOutRedirect.println( "BŁĄD");
            return false;
        } else {
            PMO_SystemOutRedirect.println( "OK");
            return true;
        }
    }

    public boolean test( String playerName ) {
        PMO_SystemOutRedirect.println( "Raport dla gracza: " + playerName );
        boolean result = true;

        result &= singleTest( collision, "kolizje własnych jednostek    ");
        result &= singleTest( friendlyFire, "ostrzelanie własnych jednostek");
        result &= singleTest( shipOutOfBounds, "wyjście poza planszę          ");
        result &= singleTest( targetOutOfBounds, "ostrzał pozycji spoza planszy ");

        PMO_SystemOutRedirect.print( "> liczba jednoczesnych operacji " +
                parallelOperationsMax.get() + " ");
        if ( parallelOperationsMax.get() < PMO_Test_Consts.PARALLEL_CALL_EXPECTED ) {
            PMO_SystemOutRedirect.println( "BŁĄD");
            result = false;
        } else {
            PMO_SystemOutRedirect.println( "OK");
        }

        PMO_SystemOutRedirect.println( "> całkowita liczba operacji " +
                totalMethodCalls.get() + " ");

        return result;
    }

}
