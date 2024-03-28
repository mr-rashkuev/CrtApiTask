package project;

import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {

    public static class RequestSubmitter implements Runnable {
        private final BlockingQueue<Request> q;

        public RequestSubmitter(final BlockingQueue<Request> q) {
            this.q = Objects.requireNonNull(q);
        }

        @Override
        public void run() {
            try {
                q.put(new Request()); //Will block until available capacity.
            }
            catch (final InterruptedException ix) {
                System.err.println("Interrupted!"); //Not expected to happen under normal use.
            }
        }
    }

    public static class Request {
        public void make() {
            try {
                //Let's simulate the communication with the external API:
                TimeUnit.MILLISECONDS.sleep((long) (Math.random() * 100));
            }
            catch (final InterruptedException ix) {
                //Let's say here we failed to communicate with the external API...
            }
        }
    }

    public static class RequestImplementor implements Runnable {
        private final BlockingQueue<Request> q;

        public RequestImplementor(final BlockingQueue<Request> q) {
            this.q = Objects.requireNonNull(q);
        }

        @Override
        public void run() {
            try {
                q.take().make(); //Will block until there is at least one element to take.
                System.out.println("Request made.");
            }
            catch (final InterruptedException ix) {
                //Here the 'taking' from the 'q' is interrupted.
            }
        }
    }

    public static void main(final String[] args) throws InterruptedException {
        
        /*The following initialization parameters specify that we
        can communicate with the external API 60 times per 1 minute.*/
        final int maxRequestsPerTime = 60;
        final TimeUnit timeUnit = TimeUnit.MINUTES;
        final long timeAmount = 1;

        final BlockingQueue<Request> q = new ArrayBlockingQueue<>(maxRequestsPerTime, true);
        //final BlockingQueue<Request> q = new LinkedBlockingQueue<>(maxRequestsPerTime);

        //Submit some RequestSubmitters to the pool...
        final ExecutorService pool = Executors.newFixedThreadPool(100);
        for (int i = 0; i < 50_000; ++i)
            pool.submit(new RequestSubmitter(q));

        System.out.println("Serving...");

        //Find out the period between communications with the external API:
        final long delayMicroseconds = TimeUnit.MICROSECONDS.convert(timeAmount, timeUnit) / maxRequestsPerTime;
        //We could do the same with NANOSECONDS for more accuracy, but that would be overkill I think.

        //The most important line probably:
        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(new RequestImplementor(q), 0L, delayMicroseconds, TimeUnit.MICROSECONDS);
    }
}