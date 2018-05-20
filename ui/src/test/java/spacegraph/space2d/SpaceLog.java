package spacegraph.space2d;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.io.FSWatch;
import jcog.net.UDPeer;
import jcog.util.Grok;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spacegraph.SpaceGraph;
import spacegraph.space2d.container.Bordering;
import spacegraph.space2d.container.Scale;
import spacegraph.space2d.widget.Timeline2D;
import spacegraph.space2d.widget.button.CheckBox;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.video.Draw;

import java.io.IOException;
import java.util.Date;

/** stand-alone local/remote log consumer and visualization
 *
 * see:
 *  https://www.elastic.co/guide/en/logstash/current/input-plugins.html
 * */
public class SpaceLog {

    /** aux logger, for console or another downstream target */
    final Logger logger;

    final UDPeer udp;

    final Grok grok = Grok.all();

    /** time buffer
     * TODO make fixed size, like ring buffer
     * */
    final Timeline2D.SimpleTimelineModel time = new Timeline2D.SimpleTimelineModel();

    public SpaceLog() throws IOException {
        this(0);
    }

    public SpaceLog(int port) throws IOException {
        this.udp = new UDPeer(port);
        this.udp.receive.on(this::input);
        this.udp.runFPS(10f);

        logger = LoggerFactory.getLogger(SpaceLog.class.getSimpleName() + "@" + udp.name());


    }

    protected void input(UDPeer.MsgReceived m) {
        //1. try default json/msgpack decode:
        byte[] data = m.data();
        input(m.from, data);
    }

    public void input(Object origin, byte[] data) {
        try {
            JsonNode x = Util.fromBytes(data, JsonNode.class);
            input(origin, x);
        } catch (IOException j) {
            try {
                Object x = Util.fromBytes(data, Object.class);
                //JsonNode x = Util.msgPackMapper.readTree(data);
                input(origin, x);
            } catch (IOException e) {
                //try to interpret it via UTF-8 String
                String s = new String(data);
                Grok.Match ms = grok.capture(s);
                if (!ms.isNull()) {
                    logger.info("recv: {}\n{}", origin, ms.toMap());
                }
            }
        }
    }

    public void input(Object origin, Object x) {
        if (x instanceof JsonNode) {
            if (input(origin, ((JsonNode)x)))
                return;
        }
        long now = System.nanoTime();
        time.add(new Timeline2D.SimpleEvent(x.toString(), now, now+1_000_000_000));
        logger.info("recv: {}\n\t{}", origin, x);
    }
    public boolean input(Object origin, JsonNode x) {
        JsonNode id = x.get("_");
        if (id!=null) {
            long s = x.get("t").get(0).asLong();
            long e = x.get("t").get(1).asLong();
            Timeline2D.SimpleEvent event = new Timeline2D.SimpleEvent(id.asText(), s, e);
            time.add(event);
            logger.info("recv: {}\n\t{}", origin, event);
            return true;
        }

        return false;
    }

    protected void gui() {
//
//        dummyModel.add(new Timeline2D.SimpleEvent("x", 0, 1));
//        dummyModel.add(new Timeline2D.SimpleEvent("y", 1, 3));
//        dummyModel.add(new Timeline2D.SimpleEvent("z", 2, 5));
//        dummyModel.add(new Timeline2D.SimpleEvent("w", 3, 3)); //point

        SpaceGraph.window(new Timeline2D<>(time,
                e->e.set(new Scale(
                        new PushButton(e.id.toString()) {
                            @Override
                            protected void paintBelow(GL2 gl) {
                                int eHash = e.id.hashCode();
                                Draw.colorHash(gl, eHash);
                                Draw.rect(gl, bounds);
                            }
                        }, 0.8f))){

            boolean autoNow = true;

            @Override
            protected void paintBelow(GL2 gl) {
                gl.glColor3f(0, 0, 0.1f);
                Draw.rect(gl, bounds);
            }

            @Override
            public Bordering controls() {
                Bordering b = super.controls();
                b.west(new CheckBox("Auto").set(autoNow).on(x->autoNow = x));
                return b;
            }

            @Override
            protected boolean prePaint(SurfaceRender r) {
                if (autoNow) {
                    view(System.nanoTime());
                }
                return super.prePaint(r);
            }
        }.view(0, 15_000_000_000L /* ns */).withControls(), 800, 600);

    }

    public static void main(String[] args) throws IOException {
        SpaceLog s = new SpaceLog();

//        Loop.of(new DummyLogGenerator(new UDPeer())).runFPS(0.75f);
//        Loop.of(new DummyLogGenerator(new UDPeer())).runFPS(0.2f);

        new FSWatch("/tmp", (p)-> {
            s.input("/tmp", p);
        }).runFPS(1);

        s.gui();
    }



    private static class DummyLogGenerator implements Runnable {

        private final UDPeer out;

        public DummyLogGenerator(UDPeer udPeer) {
            this.out = udPeer;
            out.runFPS(10f);
        }

        @Override
        public void run() {
            //echo -n "hello" >/dev/udp/localhost/44416

            try {
                out.tellSome("my time is " + new Date(), 3, false);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
    }
}
