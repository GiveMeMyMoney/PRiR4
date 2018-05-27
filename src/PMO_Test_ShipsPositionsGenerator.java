import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PMO_Test_ShipsPositionsGenerator {
    private boolean used[][] = new boolean[ GameInterface.WIDTH ]
            [ PMO_Test_Consts.SHIPS_INITIAL_ROW_MAX ];

    private List<GameInterface.PositionAndCourse> result =
            new ArrayList<>( GameInterface.WIDTH );

    private Random rnd = new Random();

    private boolean testOK( int col, int row, int course ) {
        if ( used[ col ][ row ] ) return false;

        // dodatkowo gwarantujemy, że pierwszy ruch w kierunku
        // wylosowanego kursu będzie bezpieczny
        GameInterface.Position nextPos =
            GameInterface.Course.values()[course].
                next( new GameInterface.Position(col,row) );

        if ( used[ nextPos.getCol() ][ nextPos.getRow() ] ) return false;

        return true;
    }

    private void mark( int col, int row ) {
        for ( int c = col-1; c < col + 2; c++ )
            for ( int r = row-1; r < row + 2; r++ )
                if ( ( c >= 0 ) && ( r >= 0 )
                        && ( c < GameInterface.WIDTH )
                        && ( r <= PMO_Test_Consts.SHIPS_INITIAL_ROW_MAX ) )
                    used[ c ][ r ] = true;
    }

    private void initUsed() {
        used = new boolean[ GameInterface.WIDTH ]
                [ PMO_Test_Consts.SHIPS_INITIAL_ROW_MAX + 2 ];
    }

    public List<GameInterface.PositionAndCourse> generate() {

        int col, row, course;

        result = new ArrayList<>( GameInterface.WIDTH );
        int counter = 0;
        int trials = 0;
        int trialsLimit = 500;
        initUsed();
        do {
            col = 1 + rnd.nextInt( GameInterface.WIDTH - 2 );
            row = 1 + rnd.nextInt( PMO_Test_Consts.SHIPS_INITIAL_ROW_MAX );
            course = rnd.nextInt( 4 );

//            System.out.println( "c " + col + " r " + row + " -> " + counter );
            if ( testOK( col, row, course ) ) {
                mark( col, row );
                result.add( new GameInterface.PositionAndCourse( new GameInterface.Position(col,row),
                        GameInterface.Course.values()[course] ) );
                counter ++;
            }

            trials++;
            if ( trials > trialsLimit ) {
                trials = 0;
                counter = 0;
                result.clear();
                initUsed();
                System.out.println( "Restart");
            }
        } while ( result.size() != GameInterface.WIDTH );

        return result;
    }

    public static void main(String[] args) {
        System.out.println( new PMO_Test_ShipsPositionsGenerator().generate() );
    }

}
