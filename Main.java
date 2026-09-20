public class Main {

    public static void main(String[] args) throws InterruptedException {
        int numberOfThreads = 16;
        int iterations = 200;

        Auction auction = new Auction(AuctionUtils.generateItemName());
        Lock lock = new MCSLock();
        Runner runner = new Runner(numberOfThreads, iterations, auction, lock);
        runner.run();
    }
}