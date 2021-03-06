package nars.time;

import jcog.Util;
import jcog.WTF;
import jcog.math.LongInterval;
import jcog.math.Longerval;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.task.util.TaskRegion;
import nars.term.Term;
import nars.util.Timed;
import org.jetbrains.annotations.Nullable;

import static java.lang.Long.MAX_VALUE;


/**
 * tense modes, and various Temporal utility functions and constants
 */
public enum Tense {

    Eternal(":-:"),
    Past(":\\:"),
    Present(":|:"),
    Future(":/:");

    /**
     * default for atemporal events
     * means "always" in Judgment/Question, but "current" in Goal/Quest
     */
    public static final long ETERNAL = LongInterval.ETERNAL;
    public static final long TIMELESS = LongInterval.TIMELESS;

    /**
     * integer version of long ETERNAL
     */
    public static final int DTERNAL = Integer.MIN_VALUE;

    /**
     * a dt placeholder value for preventing premature flattening during derivation target construction
     */
    public static final int XTERNAL = Integer.MAX_VALUE;


    private final String symbol;

    Tense(String string) {
        symbol = string;
    }

    static int order(float timeDiff, int durationCycles) {
        float halfDuration = durationCycles / 2.0f;
        if (timeDiff >= halfDuration) {
            return +1;
        } else if (timeDiff <= -halfDuration) {
            return -1;
        } else {
            return 0;
        }
    }

    /**
     * if (relative) event B after (stationary) event A then order=forward;
     * event B before       then order=backward
     * occur at the same time, relative to duration: order = concurrent
     */
    public static int order(long a, long b, int durationCycles) {
        if ((a == ETERNAL) || (b == ETERNAL))
            throw new RuntimeException("order() does not compare ETERNAL times");

        return order(b - a, durationCycles);
    }

    public static long getRelativeOccurrence(Tense tense, Timed m) {
        return getRelativeOccurrence(m.time(), tense, 1 /*m.duration()*/);
    }


    private static long getRelativeOccurrence(long creationTime, Tense tense, int duration) {
        switch (tense) {
            case Present:
                return creationTime;
            case Past:
                return creationTime - duration;
            case Future:
                return creationTime + duration;
            default:
                return ETERNAL;
        }
    }



    public static long dither(long t, int dither) {
        if (dither > 1) {
            //if (Param.DEBUG) {
                //HACK
//                if (t == DTERNAL || t==XTERNAL)
//                    throw new WTF("maybe you meant ETERNAL or TIMELESS");
            //}
            if (t == ETERNAL) return ETERNAL;
            else if (t == 0) return 0;
            else if (t == TIMELESS) return TIMELESS;
            else return _dither(t, false, dither);
        } else {
            return t;
        }
    }

    /** internal dither procedure */
    static long _dither(long t, boolean relative, int dither) {
        //return Util.round(t, dither);

        if (relative && Param.DT_DITHER_LOGARITHMICALLY && t > dither*dither) {
            //logarithmic dithering
            long r = (long) Util.round(Math.pow(dither, Util.round(Math.log( t )/Math.log((double)dither), 1f/dither)), dither);
            return r;
        } else return Util.round(t, dither);
    }

    public static int dither(int dt, NAR nar) {
        return dither(dt, nar.dtDither());
    }

    public static long dither(long t, NAR nar) {
        return dither(t, nar.dtDither());
    }


    /** modifies the input array, and returns it */
    public static long[] dither(long[] t, NAR nar) {
        return dither(t, nar.dtDither());
    }

    /** modifies the input array, and returns it */
    public static long[] dither(long[] t, int dt) {
        long s = t[0];
        if (s == ETERNAL) {
            assert(t[1] == ETERNAL);
            return t;
        }

        t[0] = dither(s, dt);
        t[1] = dither(t[1], dt);
        return t;
    }

    public static int dither(int dt, int dither) {
        if (dither > 1)
            switch (dt) {
                case DTERNAL:
                    return DTERNAL;
                case XTERNAL:
                    return XTERNAL;
                case 0:
                    return 0;
                default:
                    return (int) _dither(dt, true, dither);
        }

        return dt; 
    }

//    public static long[] union(TaskRegion... tt) {
//        switch (tt.length) {
//            case 0: throw new UnsupportedOperationException();
//            case 1: return new long[] { tt[0].start(), tt[0].end() };
//            //TODO will work if it handles eternal
//            case 2:
//                long as = tt[0].start();
//                long bs = tt[1].start();
//                if (as != ETERNAL && bs != ETERNAL) {
//                    return new long[]{Math.min(as, bs), Math.max(tt[0].end(), tt[1].end())};
//                }
//
//        }
//        return union(ArrayIterator.iterable(tt));
//    }

    public static long[] union(Iterable<? extends TaskRegion> t) {
        long start = MAX_VALUE, end = Long.MIN_VALUE;
        
        for (TaskRegion x : t) {
//            if (x == null) continue;
            long xs = x.start();
            if (xs != ETERNAL) {
                start = Math.min(xs, start);
                end = Math.max(x.end(), end);
            }
        }

        if (start == MAX_VALUE)
            start = end = ETERNAL;

        return new long[] { start, end };
    }

    @Nullable private static long[] intersect(Iterable<? extends TaskRegion> t) {
        long start = MAX_VALUE, end = Long.MIN_VALUE;

        for (TaskRegion x : t) {

            long xs = x.start();
            if (xs != ETERNAL) {
                long xe = x.end();
                if (start==MAX_VALUE) {
                    //first
                    start = xs;
                    end = xe;
                } else {
                    @Nullable Longerval l = Longerval.intersection(start, end, xs, xe);
                    if (l==null)
                        return null;

                    start = Math.max(xs, start);
                    end = Math.min(xe, end);
                }
            }
        }

        if (start == MAX_VALUE)
            start = end = ETERNAL;

        return new long[] { start, end };
    }


    public static boolean dtSpecial(int dt) {
        switch (dt) {
            case 0:
            case DTERNAL:
            case XTERNAL:
                return true;

            default:
                return false; //sequence
        }
    }

    /** safely transform occ (64-bit) to dt (32-bit) */
    public static int occToDT(long occ) {
        if (occ == ETERNAL)
            return DTERNAL; //HACK
        if (occ == TIMELESS)
            return XTERNAL; //HACK
        if (occ > Integer.MAX_VALUE-1 || occ < Integer.MIN_VALUE+1)
            throw new WTF(occ + " can not be DT");
        return (int)occ;
    }

    /** computes an ideal range of time for a merge or revision of tasks.
     * assumes that at least one of the items is non-eternal.
     * */
    @Nullable
    public static long[] merge(Iterable<? extends TaskRegion> tasks) {
        long[] u = Tense.union(tasks);
        long unionRange = u[1] - u[0];
        if (unionRange > Math.ceil(Param.TASK_REVISION_STRETCH_LIMIT_PROPORTION * Util.max(t -> t.start()==ETERNAL ?  0 : t.range(), tasks))) {

            //too sparse: settle for more potent intersection if exists

            long[] i = Tense.intersect(tasks);
            if (i!=null)
                return i;

            if (!Param.TASK_REVISION_ALLOW_DILUTE_UNION)
                return null;
            //else: resort to dilute union
        }

        //TODO handle cases where N>2 and a mix of union/intersect is valid

        return u;
    }

    public static void assertDithered(TaskRegion x, int d) {
        if (d < 1)
            throw new WTF("dtDither < 1");

        long s = x.start();
        long e = x.end();
        if (s != ETERNAL) {
            if (s == TIMELESS)
                throw WTF.WTF(x + " has start=TIMELESS");

            if (d > 1) {
                if (Tense.dither(s, d) != s)
                    throw WTF.WTF(x + " has non-dithered start occurrence");

                if (e != s && Tense.dither(e, d) != e)
                    throw WTF.WTF(x + " has non-dithered end occurrence");
            }
        } else {
            if (e!=ETERNAL)
                throw WTF.WTF(x + " start=ETERNAL but end!=ETERNAL");
        }
    }

    public static void assertDithered(Term t, int d) {
        if (d > 1 && t.hasAny(Op.Temporal)) {
            t.recurseTerms((Term z) -> z.hasAny(Op.Temporal), xx -> {
                int zdt = xx.dt();
                if (!Tense.dtSpecial(zdt)) {
                    if (zdt != Tense.dither(zdt, d))
                        throw WTF.WTF(t + " contains non-dithered DT in subterm " + xx);
                }
                return true;
            }, null);
        }
    }


//    public static long[] intersection(TaskRegion[] t) {
//        //HACK
//        long[] u = Tense.union(t);
//        if (u[0] == ETERNAL)
//            return u;
//
//
//        //find the min range
//        long minRange = Long.MAX_VALUE;
//        for(TaskRegion x : t) {
//            if (x == null || x.task().isEternal())
//                continue;
//            long r = x.range();
//            if (r < minRange)
//                minRange = r;
//        }
//        long range = minRange-1;
//        if (u[1]-u[0] > range) {
//            //shrink range around estimated center point
//            long mid = (u[1] + u[0])/2L;
//            return new long[] { mid - range/2L, mid + range/2L };
//        }
//        return u;
//    }


    @Override
    public String toString() {
        return symbol;
    }


}
