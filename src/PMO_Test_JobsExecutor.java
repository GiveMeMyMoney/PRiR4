import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class PMO_Test_JobsExecutor implements PMO_LogSource {

    private BlockingQueue<Runnable> queue = new LinkedBlockingDeque<>();
    private AtomicBoolean continuationFlag;

    public static class Job<T> implements Runnable, PMO_LogSource {
        private final String name;
        private final long createdAt = System.currentTimeMillis();
        private Runnable job;
        private Supplier<T> resultSupplier;
        private AtomicReference<T> resultHolder;
        private final AtomicBoolean done = new AtomicBoolean(false);
        private final AtomicReference<Exception> exceptionHoler;

        {
            exceptionHoler = new AtomicReference<>();
        }

        public Job( String name, Runnable job ) {
            this.job = job;
            this.name = name;
        }

        public Job( String name, Supplier<T> job, AtomicReference<T> resultHolder ) {
            this.resultSupplier = job;
            this.resultHolder = resultHolder;
            this.name = name;
        }

        @Override
        public void run() {
            long startedAt = System.currentTimeMillis();
            long delay = startedAt - createdAt;
            log( "Uruchomiono zadanie " + name + " czas oczekiwania na start " + delay );
            if ( delay > PMO_Test_Consts.MAX_JOB_DELAY ) {
                error( "System działa zbyt wolno. Zadanie " + name );
            }
            try {
                if ( job != null ) {
                    job.run();
                } else {
                    resultHolder.set( resultSupplier.get() );
                }
            } catch ( Exception e ) {
                exceptionHoler.set(e);
            }
            done.set(true);
            log( "Koniec zadania " + name );
        }

        public boolean done() {
            return done.get();
        }

        public Optional<T> getResult() {
            if ( job == null ) return Optional.empty();
            if ( ! done.get() ) return  Optional.empty();
            return Optional.of( resultHolder.get() );
        }

        public Optional<Exception> getException() {
            if ( ! done.get() ) return  Optional.empty();
            if ( exceptionHoler.get() != null ) return Optional.empty();
            return Optional.ofNullable( exceptionHoler.get() );
        }
    }

    public void supply( Job<?> job ) {
        queue.add( job );
    }

    public void setContinuationFlag( AtomicBoolean continuationFlag ) {
        this.continuationFlag = continuationFlag;
    }

    public void startJobsExecutor() {
        assert continuationFlag != null;

        Thread th = PMO_ThreadsHelper.createThreadAndRegister( () -> {
            Runnable job = null;
            while ( continuationFlag.get() ) {
                try {
                    job = queue.take();
                } catch (InterruptedException e) {
                    error( "W trakcie oczekiwania na zadanie doszło do wyjątku InterruptedException");
                }
                job.run();
            }
        });
        th.start();
    }
}
