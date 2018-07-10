package nars;

import jcog.Util;
import nars.derive.Derivers;
import nars.derive.deriver.MatrixDeriver;
import nars.exe.BufferedExec;
import nars.index.concept.CaffeineIndex;
import nars.op.ArithmeticIntroduction;
import nars.op.language.NARHear;
import nars.op.language.NARSpeak;
import nars.op.stm.ConjClustering;
import nars.time.clock.RealTime;
import spacegraph.audio.speech.NativeSpeechDispatcher;

import static nars.Op.BELIEF;

public class NARchy extends NARS {

    //static final Logger logger = LoggerFactory.getLogger(NARchy.class);

    public static NAR core() {


        NAR nar = new DefaultNAR(0, true)

                .index(new CaffeineIndex(32*1024))
                //.index(new HijackConceptIndex(32*1024, 4))

                .exe(new BufferedExec.WorkerExec(Util.concurrency()))

                .time(new RealTime.MS(false ).durFPS(10f))
                
                .get();


        nar.dtDither.set(20);

        nar.beliefPriDefault.set(0.5f);
        nar.goalPriDefault.set(0.75f);
        nar.questionPriDefault.set(0.35f);
        nar.questPriDefault.set(0.35f);

        ConjClustering conjClusterB = new ConjClustering(nar, BELIEF,
                t -> t.isInput()
                , 16, 64);
        ConjClustering conjClusterBnonInput = new ConjClustering(nar, BELIEF,
                t -> !t.isInput()
                , 16, 64);

        new MatrixDeriver(Derivers.nal(nar, 1, 8, "curiosity.nal", "motivation.nal"));

        new ArithmeticIntroduction(32, nar);

        return nar;
    }

    public static NAR ui() {
        /** TODO differentiate this from UI, for use in embeddeds/servers without GUI */
        NAR nar = core();
        
        nar.runLater(()->{

            //User u = User.the();





            NARHear.readURL(nar);

            {
                NARSpeak s = new NARSpeak(nar);
                s.spoken.on(new NativeSpeechDispatcher()::speak);
                
            }





            InterNAR i = new InterNAR(nar, 8, 0);
            i.runFPS(2);


        });

        return nar;
    }


}