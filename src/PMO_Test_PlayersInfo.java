import java.util.ArrayList;
import java.util.List;

public class PMO_Test_PlayersInfo {
    private final List<PMO_Test_PlayerInfo> playerInfo = new ArrayList<>();

    {
        playerInfo.add(new PMO_Test_PlayerInfo());
        playerInfo.add(new PMO_Test_PlayerInfo());
    }

    public PMO_Test_PlayerInfo getInfoObject( int player ) {
        return playerInfo.get(player);
    }
}
