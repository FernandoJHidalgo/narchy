package spacegraph.input;


import com.google.common.base.Splitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * this reads joystick events using the jstest command, which is available in most linux distributions
 *
 * TODO add calibration as a set of limits cached in GamePad, which it can resolve the raw data to
 * using a method like:
 *      gamePad1.calibrate(event1) -> CalibratedGameInputEvent
 *
 * http://www.java-gaming.org/index.php?PHPSESSID=ecau62mklg3us0870iq44ule60&topic=16866.0
 */
public class GamePad implements Function<String, GamePad.GameInputEvent>, Runnable {

    public final Logger logger;
    private final String device;

    final AtomicBoolean running = new AtomicBoolean();
    private final Consumer<GameInputEvent> each;


    public static void main(String[] args) {
        new GamePad("js0", (e) -> {
            System.out.println(e + " @ " + System.currentTimeMillis());
        }).start();
    }


    public GamePad(String device /* ex: js0 */, Consumer<GameInputEvent> each) {
        super();

        this.logger = LoggerFactory.getLogger(GamePad.class + ":" + device);
        this.device = device;
        this.each = each;

    }

    public synchronized Thread start() {
        if (running.get()) {
            logger.warn("already started {}", this);
            return null;
        }

        Thread t = new Thread(this);
        t.start();
        return t;
    }

    public static class GameInputEvent {
        final int axis; //"number"
        final long when; //"time"
        final int value;

        public GameInputEvent(int axis, int value, long when) {
            this.axis = axis;
            this.when = when;
            this.value = value;
        }

        @Override
        public String toString() {
            return "GameInputEvent{" +
                    "axis=" + axis +
                    ", value=" + value +
                    ", @=" + when +
                    '}';
        }
    }

    @Override public GameInputEvent apply(String l) {

        String prefix = "Event: ";
        if (!l.startsWith(prefix)) {
            logger.warn("unknown: {}", l);
            return null; //??
        }

        String remaining = l.substring(prefix.length());
        Map<String, String> fields = Splitter.on(", ").withKeyValueSeparator(' ').split(remaining);

        if (fields.size() != 4) {
            logger.warn("unknown format: {}", fields);
        } else {
            //ignor the "type" field for now

            long when =
                    //TODO for more accuracy, use the included time stamp, but it must be shifted to absolute unixtime
                    //Long.parseLong(fields.get("time"));
                    System.currentTimeMillis();

            int axis = Integer.parseInt(fields.get("number"));
            int value = Integer.parseInt(fields.get("value"));

            GameInputEvent e = new GameInputEvent(axis, value, when);
            each.accept(e);
            return e;
        }

        return null;
    }

    public synchronized void stop() {
        if (!running.get()) {
            logger.warn("already stopped {}", this);
        } else {
            running.set(false);
        }
    }

    @Override
    public void run() {

        if (!running.compareAndSet(false, true))
            return;

        ProcessBuilder pb = new ProcessBuilder().command("jstest", "--event", "/dev/input/" + device);

        try {
            Process proc = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));

            logger.info("start {}", proc.info());

            while (running.get()) {
                String l = reader.readLine();
                if (!l.isEmpty())
                    apply(l);
            }

            proc.destroyForcibly();
            logger.info("stop {}", proc.info());

        } catch (IOException e) {
            logger.error("{} error {}", pb, e);
        }

        stop();
    }
}

