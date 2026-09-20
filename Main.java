public class Main {

    private static final int THREAD_COUNT = 2;
    private static final int ITERATIONS = 200;
    private static final Lock LOCK = new TTASLock();

    public static void main(String[] args) throws InterruptedException {
        Auction auction = new Auction(AuctionUtils.generateItemName());
        Runner runner = new Runner(THREAD_COUNT, ITERATIONS, auction, LOCK);
        runner.run();
    }
}