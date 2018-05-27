public class PMO_Test_Transformations {
    public static GameInterface.Position real2virtual(GameInterface.Position real) {

        return new GameInterface.Position( GameInterface.WIDTH - real.getCol() - 1,
                GameInterface.HIGHT - real.getRow() - 1 );
    }

    public static GameInterface.Course real2virtual( GameInterface.Course real ) {
        switch ( real ) {
            case NORTH: return GameInterface.Course.SOUTH;
            case SOUTH: return GameInterface.Course.NORTH;
            case EAST: return GameInterface.Course.WEST;
            case WEST: return GameInterface.Course.EAST;
        }
        return null;
    }

    public static int distanceSQ( GameInterface.Position p1, GameInterface.Position p2 ) {
        int dx = p1.getCol() - p2.getCol();
        int dy = p1.getRow() - p2.getRow();

        return dx * dx + dy * dy;
    }

    public static int otherPlayer(int player) {
        return ( player + 1 ) % 2;
    }
}
