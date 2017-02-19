package nars.io;

import nars.NAR;
import nars.storage.Memory;
import nars.util.AbstractObserver;
import nars.util.EventEmitter;
import nars.util.Events.Answer;

/**
 * Output Channel: Implements this and NAR.addOutput(..) to receive output signals on various channels
 */
public abstract class Output extends AbstractObserver {
    
    
    /** implicitly repeated input (a repetition of all input) */
    public interface IN  { }
    
    /** conversational (judgments, questions, etc...) output */
    public interface OUT  { }
    
    /** warnings, errors & exceptions */
    public interface ERR { }
    
    /** explicitly repeated input (repetition of the content of input ECHO commands) */
    public interface ECHO  { }
    
    /** operation execution */
    public interface EXE  { }
    
        
    public static class ANTICIPATE {}
    
    public static class CONFIRM {}
    
    public static class DISAPPOINT {}

    public static final Class[] DefaultOutputEvents = { IN.class, EXE.class, OUT.class, ERR.class, ECHO.class, Answer.class, ANTICIPATE.class, CONFIRM.class, DISAPPOINT.class };
            
    public Output(EventEmitter source, boolean active) {
        super(source, active, DefaultOutputEvents );
    }
    
    public Output(Memory m, boolean active) {
        this(m.event, active);
    }

    public Output(NAR n, boolean active) {
        this(n.memory.event, active);
    }

    public Output(NAR n) {
        this(n, true);
    }

}
