package github.nyeptos.packingcube.gui;

import java.util.Objects;

/** Per-host, one-frame popup queue. Keeps Screen's first-request/explicit-override semantics. */
public final class TooltipQueue<T> {
    private T pending;

    public void offer(T request, boolean override) {
        Objects.requireNonNull(request);
        if (pending == null || override) pending = request;
    }

    public T take() {
        T result = pending;
        pending = null;
        return result;
    }

    public void clear() {
        pending = null;
    }
}
