import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Objects;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class PMO_Test_Ship implements PMO_LogSource, Serializable {
    private final AtomicReference<GameInterface.Position> realPosition;
    private final AtomicReference<GameInterface.Position> virtualPosition;
    private final AtomicReference<GameInterface.Position> lastSpottedPosition;
    private final AtomicReference<GameInterface.Course> realCourse;
    private final AtomicReference<GameInterface.Course> virtualCourse;
    private final AtomicReference<GameInterface.Course> lastSpottedCourse;
    private final AtomicBoolean isAlive;
    private final static AtomicInteger counter = new AtomicInteger( 1 );
    private final int id;
    private final int owner;
    private final AtomicInteger simultaneousDetections  = new AtomicInteger( 0 );
    private final Lock shipLock = new ReentrantLock();

    public PMO_Test_Ship(GameInterface.Position position,
                         GameInterface.Course course,
                         int owner ) {
        realPosition = new AtomicReference<>();
        virtualPosition = new AtomicReference<>();
        lastSpottedPosition = new AtomicReference<>();
        realCourse = new AtomicReference<>();
        virtualCourse = new AtomicReference<>();
        lastSpottedCourse = new AtomicReference<>();

        updateCourse(course);
        updatePosition(position);
        isAlive = new AtomicBoolean(true);
        id = counter.getAndIncrement();
        this.owner = owner;
    }

    private void updatePosition(GameInterface.Position position) {
        GameInterface.Position tmp = PMO_Test_Transformations.real2virtual(position);
        realPosition.set(position);
        virtualPosition.set(tmp);
    }

    private void updateCourse( GameInterface.Course course ) {
        GameInterface.Course tmp = PMO_Test_Transformations.real2virtual( course );
        realCourse.set(course);
        virtualCourse.set(tmp);
    }

    public void destroy() {
        isAlive.set( false );
    }

    public boolean isAlive() {
        return isAlive.get();
    }

    public Lock getLock() {
        return shipLock;
    }

    public int getOwner() {
        return owner;
    }

    public GameInterface.Position getPosition() {
        return realPosition.get();
    }

    public GameInterface.Course getCourse() {
        return realCourse.get();
    }

    public GameInterface.Position getVirtualPosition() {
        return virtualPosition.get();
    }

    public GameInterface.Course getVirtualCourse() {
        return virtualCourse.get();
    }

    public void move() {
        updatePosition( realCourse.get().next( realPosition.get() ));
    }

    public void turnLeft(){
        updateCourse( realCourse.get().afterTurnToLeft() );
    }

    public void turnRight(){
        updateCourse( realCourse.get().afterTurnToRight() );
    }

    public void spotted(BlockingQueue<GameInterface.PositionAndCourse> deque) {
        int detections = simultaneousDetections.incrementAndGet();

        if ( detections > 1 ) {
            // okręt był już wykryty, ale informacja jeszcze nie została
            // przekazana przeciwnikowi. Wygenerujemy nową jeśli tylko
            // okręt zmienił pozycję lub kurs.

            if ( realPosition.get().equals( lastSpottedPosition.get() ) &&
                    realCourse.get().equals( lastSpottedCourse.get() ) ) {
                simultaneousDetections.decrementAndGet();
                return;
            }
        }

        class DelayedMessage implements Runnable {
            @Override
            public void run() {
                GameInterface.PositionAndCourse pc = new GameInterface.PositionAndCourse(
                        virtualPosition.get(), virtualCourse.get() );
                String shipInfo = PMO_Test_Ship.this.toString();
                log( "Wykryto okręt " + shipInfo );
                PMO_TimeHelper.sleep( PMO_Test_Consts.BEFORE_SPOT_INFO_DELAY);
                deque.add( pc );
                log( "Informacja o wykryciu okrętu " + shipInfo + " przekazana przeciwnikowi");
                simultaneousDetections.decrementAndGet();
            }
        }

        PMO_ThreadsHelper.createThreadAndRegister( new DelayedMessage() ).start();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PMO_Test_Ship that = (PMO_Test_Ship) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "PMO_Test_Ship{" +
                "position=" + realPosition +
                ", course=" + realCourse +
                ", alive=" + isAlive +
                ", id=" + id +
                ", owner=" + owner +
                '}';
    }
}
