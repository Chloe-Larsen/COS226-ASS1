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

                double current = auction.getHighestBid();
                double increment = 1 + rnd.nextInt(10);     //positive
                double newBid = current + increment;        //higher

                auction.placeBid(bidderId, newBid);
                totalBidsPlaced.incrementAndGet();

                if (auction.getHighestBidder() == bidderId)
                {
                    bidsWon[bidderId].incrementAndGet();
                }
            }finally {
                lock.unlock();
            }
        }
    }

    /* Optional Helper: Records and reports the results of the experiment. */
    public void reportResults(long executionTime) {
        // TODO
    }
}