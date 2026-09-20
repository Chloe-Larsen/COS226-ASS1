import java.util.concurrent.atomic.AtomicLong;
import java.util.Random;

/* Optional Helper Runner Class */
public class Runner {

    private static boolean DEBUG_PRINTS = false;

    public final int numberOfThreads;
    public final int iterations;
    public final Auction auction;
    public final Lock lock;

    // bid
    private final AtomicLong attempts = new AtomicLong(0);
    private final AtomicLong successes = new AtomicLong(0);
    private final AtomicLong[] bidsWon;
    private final AtomicLong[] waitNanos;

    private final AtomicLong totalWaitingTime = new AtomicLong(0);

    public Runner(int numberOfThreads, int iterations, Auction auction, Lock lock) {
        this.numberOfThreads = numberOfThreads;
        this.iterations = iterations;
        this.auction = auction;
        this.lock = lock;
        this.bidsWon = new AtomicLong[numberOfThreads];
        this.waitNanos = new AtomicLong[numberOfThreads];
        for (int i = 0; i < numberOfThreads; i++) {
            bidsWon[i] = new AtomicLong(0);
            waitNanos[i] = new AtomicLong(0);
        }

    }

    public void run() throws InterruptedException {
        Thread[] threads = new Thread[numberOfThreads];

        for (int i = 0; i < numberOfThreads; i++) {
            final int bidderId = i;

            threads[i] = new Thread(() -> {
                bidder(bidderId);
            });
        }

        long startTime = System.nanoTime();

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        long endTime = System.nanoTime();

        reportResults(endTime - startTime);
    }

    /*
     * Defines the behaviour of an individual bidder.
     * Note you have to decide how to incorporate your lock.
     */
    public void bidder(int bidderId) {

        Random rnd = new Random(bidderId * 31L + 17); // this makes a diffferent seed for each bidder avoiding two
                                                      // random objects having the same one

        for (int i = 0; i < iterations; i++) {
            double current = auction.getHighestBid();
            double newBid = current + 1 + rnd.nextInt(10);

            long t0 = System.nanoTime();

            if (DEBUG_PRINTS)
                System.out.println(Thread.currentThread().threadId() + " | AQUIRING LOCK");
            lock.lock();
            if (DEBUG_PRINTS)
                System.out.println(Thread.currentThread().threadId() + " | AQUIRED LOCK");

            try {
                long t1 = System.nanoTime();
                waitNanos[bidderId].addAndGet(t1 - t0);
                totalWaitingTime.addAndGet(t1 - t0);

                auction.placeBid(bidderId, newBid);

                if (auction.getHighestBidder() == bidderId) {
                    bidsWon[bidderId].incrementAndGet();
                    successes.incrementAndGet();
                }
                attempts.incrementAndGet();
            } finally {
                if (DEBUG_PRINTS)
                    System.out.println(Thread.currentThread().threadId() + " | RELEASING LOCK");
                lock.unlock();
            }
        }
    }

    /* Optional Helper: Records and reports the results of the experiment. */
    public void reportResults(long executionTime) {

        long expected = (long) numberOfThreads * iterations;

        System.out.println("Results:");
        System.out.println("Lock        : " + lock.getClass().getSimpleName());
        System.out.println("Item        : " + auction.getItemName());
        System.out.println("Threads     : " + numberOfThreads);
        System.out.println("Iterations  : " + iterations);
        System.out.println();

        System.out.printf("Execution time       : %.3f ms%n", executionTime / 1e6);
        System.out.printf("Total attempts       : %d (expected %d)%n", attempts.get(), expected);
        System.out.printf("Successful bids      : %d%n", successes.get());
        System.out.printf("Final highest bid    : %.2f%n", auction.getHighestBid());
        System.out.println("Highest bidder id    : " + auction.getHighestBidder());
        System.out.println();

        // fairness
        System.out.println("Bids won per bidder:");
        long min = Long.MAX_VALUE, max = Long.MIN_VALUE;
        for (int i = 0; i < numberOfThreads; i++) {
            long w = bidsWon[i].get();
            System.out.printf("     bidder %2d : %d%n", i, w);
            if (w < min)
                min = w;
            if (w > max)
                max = w;
        }
        System.out.printf("Fairness spread (max-min wins): %d%n", max - min);
        System.out.println();

        // average wait to acquire lock
        System.out.println("Average wait to acquire lock (ns):");
        long totalWait = 0;
        for (int i = 0; i < numberOfThreads; i++) {
            long avg = waitNanos[i].get() / iterations;
            totalWait += avg;
            System.out.printf("         bidder %2d : %d%n", i, avg);
        }
        System.out.printf("Overall average wait : %d ns%n", totalWait / numberOfThreads);
        System.out.println();

        long totalAcquisitions = (long) numberOfThreads * iterations;
        long avgWaitAcquisition = totalWaitingTime.get() / totalAcquisitions;

        System.out.println("Total waiting time           : " + totalWaitingTime.get() + " ns");
        System.out.println("Avg wait per lock acquisition: " + avgWaitAcquisition + " ns");
        System.out.println();

        if (attempts.get() != expected) {
            System.out.println("Workload not completed — expected "
                    + expected + " attempts, got " + attempts.get());
        }
    }
}