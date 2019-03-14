package nars.guifx.demo;

import nars.index.Indexes;
import nars.nar.Default;
import nars.op.mental.Abbreviation;
import nars.op.mental.Anticipate;
import nars.op.mental.Inperience;
import nars.time.RealtimeMSClock;
import nars.util.data.random.XorShift128PlusRandom;
import org.jetbrains.annotations.NotNull;


/**
 * Created by me on 9/7/15.
 */
public enum NARideRealtimeDefault {
    ;

    public static void main(String[] arg) {


        //Global.DEBUG = true;


        Default nar = newRealtimeNAR();



        /*nar.memory.conceptForgetDurations.set(10);
        nar.memory.termLinkForgetDurations.set(100);*/

        //nar.duration.set(750 /* ie, milliseconds */);
        //nar.spawnThread(1000/60);


        NARide.show(nar.loop(), (i) -> {
            /*try {
                i.nar.input(new File("/tmp/h.nal"));
            } catch (Throwable e) {
                i.nar.memory().eventError.emit(e);
                //e.printStackTrace();
            }*/
        });

    }

    @NotNull
    public static Default newRealtimeNAR() {
        XorShift128PlusRandom rng = new XorShift128PlusRandom(1);
//        Memory mem = new Memory(,

        //new MapCacheBag(
        //new WeakValueHashMap<>()

        //GuavaCacheBag.make(1024*1024)
                /*new InfiniCacheBag(
                    InfiniPeer.tmp().getCache()
                )*/
        //)
        //);

        Default nar = new Default(1024, 3, 2, 2, rng,
                new Indexes.WeakTermIndex(128*1024, new XorShift128PlusRandom(1)),
                new RealtimeMSClock());

        nar.with(
                Anticipate.class,
                Inperience.class
        );
        nar.with(new Abbreviation(nar, "is"));
        return nar;
    }
}