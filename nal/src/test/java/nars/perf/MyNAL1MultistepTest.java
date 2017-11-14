package nars.perf;

import jcog.math.FloatSupplier;
import nars.nal.nal1.NAL1MultistepTest;

public class MyNAL1MultistepTest implements FloatSupplier {
    @Override
    public float asFloat() {
        try {
//                Class c = Thread.currentThread().getContextClassLoader().loadClass("nars.nal.nal1.NAL1MultistepTest");
//                Object n0 = c.newInstance();
            //n.getClass().getMethod

            NAL1MultistepTest n = new NAL1MultistepTest();

            System.out.println("TTL_MUTATE=" + n.nar.TTL_MUTATE);
            System.out.println("TTL_MUTATE=" + n.nar.TTL_DERIVE_TASK_SUCCESS);

            ((NAL1MultistepTest) n).multistepSim4();
            n.end(null);
            return 1f / (1 + n.test.time());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
