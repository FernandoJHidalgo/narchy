package jcog;

import org.junit.Test;

import java.util.concurrent.ForkJoinPool;

public class ServicesTest {

    @Test
    public void testServices1() {

        Services<?, String> s = new Services("");
        StringBuilder sb = new StringBuilder();

        s.add("x", new DummyService(sb), true);
        s.add("y", new DummyService(sb), true);

        s.printServices(System.out);

        //ForkJoinPool.commonPool().awaitQuiescence()

        s.stop();

        s.printServices(System.out);

        System.out.println(sb);
    }

    private static class DummyService extends Services.AbstractService {
        private final StringBuilder sb;

        public DummyService(StringBuilder sb) {
            this.sb = sb;
        }

        @Override
        protected void start(Object x) {
            sb.append(this).append(" start\n");
        }

        @Override
        protected void stop(Object x){
            sb.append(this).append(" stop\n");
        }
    }
}