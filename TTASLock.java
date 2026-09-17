import java.util.concurrent.atomic.AtomicBoolean;

public class TTASLock implements Lock {

    private final AtomicBoolean locked = new AtomicBoolean(false);

    @Override
    public void lock() {
        while(true){
            while(locked.get()){
                Thread.onSpinWait();
            }

            if(locked.compareAndSet(false, true)){
                return;
            }
        }
    }

    @Override
    public void unlock() {
        locked.set(false);
    }
}
