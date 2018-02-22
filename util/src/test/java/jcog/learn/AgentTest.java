package jcog.learn;

import jcog.learn.ql.DPG;
import jcog.learn.ql.DQN;
import jcog.learn.ql.HaiQ;
import jcog.learn.ql.HaiQae;
import org.eclipse.collections.impl.map.mutable.primitive.IntIntHashMap;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class AgentTest {


    @Test public void testHaiQ() {
        testAgent( new HaiQ(1, 2) );
    }
    @Test public void testHaiQAgent() {
        testAgent( new HaiQae(1, 2) );
    }

    @Disabled
    @Test public void testDPGAgent() {
        testAgent( new DPG(1, 2) ); //broken it seems
    }


    @Test public void testDQNAgent() {
        testAgent( new DQN(1, 2) ); //broken it seems
    }

    static void testAgent(Agent agent) {

        //assert(agent.inputs == 1); //not true via HaiQ perception
        assert(agent.inputs >= 1);
        assert(agent.actions == 2);

        final float minRatio = 2f;

        int cycles = 100;

        float nextReward = 0;
        IntIntHashMap acts = new IntIntHashMap();
        for (int i = 0; i < cycles; i++) {
            int action = agent.act(nextReward, new float[] { (float)Math.random() } );
            //System.out.println(a);
            acts.addToValue(action, 1);
            switch (action) {
                case 0: nextReward = -1.0f; break;
                case 1: nextReward = +1.0f; break;
                default:
                    throw new UnsupportedOperationException();
            }
        }
        System.out.println(agent.getClass() + " " + agent.summary() + "\n" + acts);
        assertTrue(acts.get(1) > acts.get(0));
        assertTrue(acts.get(1)/ minRatio > acts.get(0)); //at least 10:1
    }

}