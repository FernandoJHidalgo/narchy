package jcog;

import org.junit.jupiter.api.Test;

public class ServicesTest {

    @Test
    public void testServices1() {

        Services<?, String> s = new Services("");
        StringBuilder sb = new StringBuilder();

        s.add("x", new DummyService(sb), true);
        s.add("y", new DummyService(sb), true);

        s.print(System.out);

        

        s.stop();

        s.print(System.out);

        
    }

    private static class DummyService extends Service {
        private final StringBuilder sb;

        public DummyService(StringBuilder sb) {
            this.sb = sb;
        }

        @Override
        public void off() {

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