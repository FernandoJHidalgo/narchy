package nars.time;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by me on 7/2/15.
 */
public abstract class RealtimeClock implements Clock {

    long t, t0 = -1;
    private long start;

    long seed = Math.abs(UUID.randomUUID().getLeastSignificantBits() ) & 0xffff0000;
    final AtomicInteger nextStamp = new AtomicInteger(1);

    @Override
    public long newStampSerial() {
        return seed | nextStamp.getAndIncrement();
    }

    @Override
    public void clear() {
        tick();
        t = t0 = getRealTime();

        start = t;
    }


    @Override
    public final void tick() {
        long now = getRealTime();

        t0 = t;
        t = now;

        //        if (memory.resource!=null) {
//            final double frameTime = memory.resource.FRAME_DURATION.stop();
//
//            //in real-time mode, warn if frame consumed more time than reasoner duration
//            final int d = memory.duration();
//
//            if (frameTime > d) {
//                memory.eventError.emit(new Lag(d, frameTime));
//            }
//
//        }
    }

    static class Lag implements Serializable {

        private final double frameTime;
        private final int dur;

        public Lag(int duration, double frameTime) {
            dur = duration;
            this.frameTime = frameTime;
        }

        @NotNull
        public String toString() {
            return "Lag frameTime=" +
                    frameTime + ", duration=" + dur + " cycles)";
        }
    }


    @Override
    public final long time() {
        return t;
    }

    @Override
    public long elapsed() {
        return t0 - t;
    }

    protected abstract long getRealTime();

    float secondsSinceStart() {
        return unitsToSeconds(t - start);
    }

    protected abstract float unitsToSeconds(long l);

    @NotNull
    @Override
    public String toString() {
        return secondsSinceStart() + "s";
    }
}


//package nars.clock;
//
///**
// * hard realtime does not cache the value and will always update when time()
// * is called
// */
//public class HardRealtimeClock extends RealtimeClock {
//
//    private final boolean msOrNano;
//
//    public HardRealtimeClock(boolean msOrNano) {
//        super(false);
//        this.msOrNano = msOrNano;
//    }
//
//    /** default: ms resolution */
//    public HardRealtimeClock() {
//        this(true);
//    }
//
//
//    @Override
//    protected long getRealTime() {
//        if (msOrNano) {
//            return System.currentTimeMillis();
//        }
//        else {
//            return System.nanoTime();
//        }
//    }
//
//    @Override
//    protected float unitsToSeconds(long l) {
//        if (msOrNano) {
//            return (l / 1000f);
//        }
//        else {
//            return (l / 1e9f);
//        }
//    }
//
//    @Override
//    public long time() {
//        return getRealTime();
//    }
//
//}