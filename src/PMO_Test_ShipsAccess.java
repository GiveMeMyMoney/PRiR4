import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface PMO_Test_ShipsAccess extends Remote {
    public List<List<PMO_Test_Ship>> getShips( String password ) throws RemoteException;
}
