import java.util.concurrent.atomic.AtomicLong;

/* Optional Helper Runner Class */
public class Runner {

    public final int numberOfThreads;
    public final int iterations;
    public final Auction auction;
    public final Lock lock;

    //bid 
    private final AtomicLong totalBidsPlaced = new AtomicLong(0);
    private final AtomicLong[] bidsWon; //per bidder
    private final AtomicLong[] waitNanos;//time waiting for lock

    private final AtomicLong totalWaitingTime = new AtomicLong(0);

    public Runner(int numberOfThreads, int iterations, Auction auction, Lock lock) {
        this.numberOfThreads = numberOfThreads;
        this.iterations = iterations;
        this.auction = auction;
        this.lock = lock;
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
        
        Random rnd = new Random(bidderId * 31L + 17);   //this makes a diffferent seed for each bidder avoiding two random objects having the same one 

        for (int i = 0; i < iterations; i++)
        {
            long t0 = System.nanoTime();

            lock.lock();
            try {
                long t1 = System.nanoTime();
                waitNanos[bidderId].addAndGet(t1-t0);

                //crit
                double current = auction.getHighestBid();
                double increment = 1 + rnd.nextInt(10);     //positive
                double newBid = current + increment;        //higher

                auction.placeBid(bidderId, newBid);
                totalBidsPlaced.incrementAndGet();

                if (auction.getHighestBidder() == bidderId)
                {
                    bidsWon[bidderId].incrementAndGet();
                }
                //eo crit
            }finally {
                lock.unlock();
            }
        }
    }

    /* Optional Helper: Records and reports the results of the experiment. */
    public void reportResults(long executionTime) {
        
        long expected = (long) numberOfThreads * iterations;    //brackets are casting
        long actual = totalBidsPlaced.get()

        System.out.println("Results: ");

        System.out.println("Lock        : " + lock.getClass().getSimpleName());
        System.out.println("Item        : " + auction.getItemName());
        System.out.println("Threads     : " + numberOfThreads);
        System.out.println("Iterations  : " + iterations);
        
        System.out.println();
        
        System.out.printf("Execution time       : %.3f ms%n", executionTimeNanos / 1e6);    //%. is a placeholder, %n newline, the conversion to miliseconds
        System.out.printf("Total bids placed    : %sd (expected %d)%n" actual, expected);   //%d decimal integer
        System.out.println("Highest bidder id : " + auction.getHighestBidder());

        System.out.println();
        //fairness test

        System.out.println("Bids won per bidder: ");
        long min Long.MAX_VALUE, max = Long.MIN_VALUE;
        for (int i = 0; i < numberOfThreads; i++) {
            long w = bidsWon[i].get();
            System.out.printf("     bidder %2d : %d%n", i,w);
            if (w < min)
            {
                min = w;
            }
            if (w > max)
            {
                max = w;
            }

        }
        System.out.printf("Fairness spread (max-min wins): %d%n", (max-min));

        System.out.println();
        //extra metric: 

        long totalWait = 0; 

        System.out.println("Average wait to acquire lock in nanoseconds:");
        for (int i = 0; i < numberOfThreads; i++){
            long avg = waitNanos[i].get() / iterations;
            totalWait += avg;
            System.out.printf("         bidder %2d : %d%n", i, avg);
        }
        System.out.printf("Overall average wait : %d ns%n", totalWait/numberOfThreads);

        System.out.println();

        if (actual != expected)
        {
            System.out.println("Mutual exclusion has been broken as the actual waiting time was not what was expected")
        }
    }
}