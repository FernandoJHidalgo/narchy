package nars.time;

import static nars.time.Tense.*;

public final class TimeSpan {
    public final static TimeSpan TS_ZERO = new TimeSpan(0);


    public final static TimeSpan TS_ETERNAL = new TimeSpan(ETERNAL);
    public final long dt;

    private TimeSpan(long dt) {
        this.dt = dt;
    }

    public static TimeSpan the(long dt) {
        assert (dt != TIMELESS);
        assert (dt != XTERNAL) : "probably meant to use TIMELESS";
        assert (dt != DTERNAL) : "probably meant to use ETERNAL";

        if (dt == 0) {
            return TS_ZERO;
        } else if (dt == ETERNAL) {
            return TS_ETERNAL;
        } else {
            return new TimeSpan(dt);
        }
    }

    @Override
    public int hashCode() {
        return Long.hashCode(dt);
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || ((obj instanceof TimeSpan && dt == ((TimeSpan) obj).dt));
    }

    @Override
    public String toString() {
        return (dt == ETERNAL ? "~" : (dt >= 0 ? ("+" + dt) : ("-" + (-dt))));

    }
}
