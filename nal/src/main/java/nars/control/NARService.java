package nars.control;

import jcog.Service;
import jcog.event.Ons;
import nars.$;
import nars.NAR;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NARService extends Service<NAR> implements Termed {

    static final Logger logger = LoggerFactory.getLogger(NARService.class);

    public final Term id;
    protected Ons ons;

    protected volatile NAR nar;

    @Deprecated protected NARService(NAR nar) {
        this((Term)null);

        if (nar!=null) {
            (this.nar = nar).on(this);
        }
    }

    protected NARService(@Nullable Term id) {
        this.id = id!=null ? id :
                $.p($.quote(getClass().getName()), $.the(System.identityHashCode(this)) );
    }



    @Override
    protected final void start(NAR nar) {
        logger.debug("start {}", id);

        assert(this.nar == null || this.nar == nar);
        if (this.nar == null)
            this.nar = nar;

        ons = new Ons(nar.eventClear.on(n -> clear()));
        starting(nar);
    }

    @Override
    protected final void stop(NAR nar) {
        logger.debug("stop {}", id);

        this.ons.off();
        this.ons = null;

        stopping(nar);

        
    }

    public void clear() {
        
    }


    protected void starting(NAR nar) {

    }

    protected void stopping(NAR nar) {

    }

    public final void off() {
        NAR n = nar;
        if (n!=null) {
            n.services.remove(id);
        }
        
    }

    @Override
    public final Term term() {
        return id;
    }

}
