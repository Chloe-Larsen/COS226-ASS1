import java.util.concurrent.atomic.AtomicReference;

public class CLHLock implements Lock {

    private final AtomicReference<Node> tail = new AtomicReference<>(new Node());
    // create new node for each thread
    private final ThreadLocal<Node> myNode = ThreadLocal.withInitial(Node::new);
    private final ThreadLocal<Node> myPred = new ThreadLocal<>();

    @Override
    public void lock() {
        Node node = myNode.get();
        node.locked = true;

        Node pred = tail.getAndSet(node);
        myPred.set(pred);

        while (pred.locked) {
            Thread.onSpinWait();
        }
    }

    @Override
    public void unlock() {
        // signal to successor that I am no longer in critical section
        myNode.get().locked = false;

        // take ownership of precesessor node to reuse it and pass ownership of current
        // node onto successor
        Node pred = myPred.get();
        myNode.set(pred);
    }

    class Node {
        volatile boolean locked;
    }
}
