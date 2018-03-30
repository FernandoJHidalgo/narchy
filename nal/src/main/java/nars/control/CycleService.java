package nars.control;

import nars.NAR;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

abstract public class CycleService extends NARService implements Consumer<NAR> {

    static final Logger logger = LoggerFactory.getLogger(CycleService.class);

    protected final AtomicBoolean busy = new AtomicBoolean(false);

    protected CycleService(NAR nar) {
        super(nar);
    }

    @Override
    protected void starting(NAR nar)  {
        ons.add(nar.onCycle(this));
    }

    @Override public void accept(NAR nar) {
        if (busy.compareAndSet(false, true)) {
            try {
                run(nar);
            } catch (Exception e) {
                logger.error("{} {}", this, e);
            } finally {
                busy.set(false);
            }
        }
    }

    abstract protected void run(NAR nar);

}
