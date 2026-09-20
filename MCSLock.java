import java.util.concurrent.atomic.AtomicReference;

public class MCSLock implements Lock {

    private final AtomicReference<Node> tail = new AtomicReference<>();
    private final ThreadLocal<Node> myNode = new ThreadLocal<>();

    @Override
    public void lock() {
        Node node = new Node();
        myNode.set(node);

        Node pred = tail.getAndSet(node);

        if (pred != null) {
            node.locked = true; // there is a predecessor so wait
            pred.next = node; // tell the pred I exist so it can tell me when it's done

            while (node.locked) {
                // wait until predecessor says I can continue
                Thread.onSpinWait();
            }
        }
    }

    @Override
    public void unlock() {
        Node node = myNode.get();

        if (node.next == null) {
            // there is no successor
            if (tail.compareAndSet(node, null))
                return; // there is actually no successor

            while (node.next == null) {
                // there is a successor, wait for it to set my node's next
                // Thread.onWaitSpin();
            }
        }

        node.next.locked = false; // tell successor I'm done and it can continue
    }

    class Node {
        boolean locked;
        Node next;
    }
}
