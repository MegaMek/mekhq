package mekhq.event;

import java.util.Objects;

/**
 * Base class for all events
 */
public abstract class HQEvent<T> {
    protected T source;
    protected boolean cancelled = false;
    
    public HQEvent(T source) {
        this.source = Objects.requireNonNull(source);
    }
    
    /** @return true if the event can be cancelled (aborted) */
    public boolean isCancellable() {
        return false;
    }
    
    /** @return true if the event is cancelled */
    public boolean isCancelled() {
        return cancelled;
    }

    public void cancel() {
        if(isCancellable()) {
            cancelled = true;
        }
    }
}
