package nars.experiment.minicraft;

import nars.$;
import nars.NAR;
import nars.NAgentX;
import nars.Narsese;
import nars.experiment.minicraft.top.InputHandler;
import nars.experiment.minicraft.top.TopDownMinicraft;
import nars.util.signal.Sensor2D;
import nars.op.video.PixelAutoClassifier;

//import org.jcodec.codecs.h264.H264Encoder;
//import org.jcodec.codecs.h264.H264Utils;
//import org.jcodec.codecs.h264.encode.H264FixedRateControl;
//import org.jcodec.common.NIOUtils;
//import org.jcodec.common.SeekableByteChannel;
//import org.jcodec.common.model.ColorSpace;
//import org.jcodec.common.model.Picture;
//import org.jcodec.containers.mp4.Brand;
//import org.jcodec.containers.mp4.MP4Packet;
//import org.jcodec.containers.mp4.TrackType;
//import org.jcodec.containers.mp4.muxer.FramesMP4MuxerTrack;
//import org.jcodec.containers.mp4.muxer.MP4Muxer;
//import org.jcodec.scale.ColorUtil;
//import org.jcodec.scale.Transform;

/**
 * Created by me on 9/19/16.
 */
public class TopCraft extends NAgentX {

    private final TopDownMinicraft craft;
    private Sensor2D pixels;
    private PixelAutoClassifier camAE;

    public static void main(String[] args) {
        runRT(nar1 -> {
            try {
                return new TopCraft(nar1);
            } catch (Narsese.NarseseException e) {
                e.printStackTrace();
                return null;
            }
        }, 25);
    }

    public TopCraft(NAR nar) throws Narsese.NarseseException {
        super("cra", nar);

        this.craft = new TopDownMinicraft();

//        {
//            SequenceEncoder enc = null;
//            try {
//
//                enc = new SequenceEncoder(new File("/tmp/x.mp4"));
//
//                // GOP size will be supported in 0.2
//                // enc.getEncoder().setKeyInterval(25);
//
//                //for(...) {
//                //BufferedImage image = null; // ... // Obtain an image to encode
//
//                int w = 16, h = 16;
//                Picture p = Picture.create(w, h, ColorSpace.RGB);
//                int[] i = p.getPlaneData(0);
//                for (int k = 0; k < 100; k++) {
//                    for (int j = 0; j < i.length; j++)
//                        i[j] = (int)(Math.random()*(200));
//                    enc.encodeNativeFrame(p);
//                }
//
//
//                enc.finish();
//
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
        pixels = senseCameraRetina("cra", ()->craft.image, 32,32);
        //pixels = addFreqCamera("see", ()->craft.image, 64,64, (v) -> $.t( v, alpha));

//        int nx = 8;
//        camAE = new PixelAutoClassifier("cra", pixels.src.pixels, nx, nx,   (subX, subY) -> {
//            //context metadata: camera zoom, to give a sense of scale
//            //return new float[]{subX / ((float) (nx - 1)), subY / ((float) (nx - 1)), pixels.src.Z};
//            return new float[]{ pixels.src.Z};
//        }, 24, this);
//        window(camAE.newChart(), 500, 500);


        senseSwitch("dir(cra)", ()->craft.player.dir, 0, 4);
        sense($.func("stamina", "cra"), ()->(craft.player.stamina)/((float)craft.player.maxStamina));
        sense($.func("health", "cra"), ()->(craft.player.health)/((float)craft.player.maxHealth));

        int tileMax = 13;
        senseSwitch("tile(cra,here)", ()->craft.player.tile().id, 0, tileMax);
        senseSwitch("tile(cra,up)", ()->craft.player.tile(0,1).id, 0, tileMax);
        senseSwitch("tile(cra,down)", ()->craft.player.tile(0,-1).id, 0, tileMax);
        senseSwitch("tile(cra,right)", ()->craft.player.tile(1,0).id, 0, tileMax);
        senseSwitch("tile(cra,left)", ()->craft.player.tile(-1,0).id, 0, tileMax);

        InputHandler input = craft.input;
        actionToggle($.func("fire", "cra"), input.attack::toggle/*, 16*/ );
        actionTriState($.func("move","cra", "X"), (i)->{
           boolean l = false, r = false;
           switch (i) {
               case -1: l = true;  break;
               case +1: r = true;  break;
           }
           input.left.toggle(l);
           input.right.toggle(r);
        });
        actionTriState($.func("move","cra", "Y"), (i)->{
            boolean u = false, d = false;
            switch (i) {
                case -1:  u = true;  break;
                case +1:  d = true;  break;
            }
            input.up.toggle(u);
            input.down.toggle(d);
        });
        actionToggle($.func("menu", "cra"), input.menu::toggle);

//        Param.DEBUG = true;
//        nar.onTask(t ->{
//            if (t.isEternal() && (!(t instanceof VarIntroduction.VarIntroducedTask)) && t.concept(nar).get(Abbreviation.class)==null) {
//                System.err.println(t.proof());
//                System.err.println();
//            }
//        });

        TopDownMinicraft.start(craft, false);
    }



    float prevScore;
    @Override protected float act() {

        float nextScore = craft.frameImmediate();
        float ds = nextScore - prevScore;
        this.prevScore = nextScore;
        float r = ((ds/2f)) + 2f * (craft.player.health/((float)craft.player.maxHealth)-0.5f);// + 0.25f * (craft.player.stamina*((float)craft.player.maxStamina))-0.5f);
        return r;
    }


//    /** customized copy of JEncode's */
//    public class SequenceEncoder {
//        private final SeekableByteChannel ch;
//        private Picture toEncode;
//        private final Transform transform;
//        private final H264Encoder encoder;
//        private final ArrayList<ByteBuffer> spsList;
//        private final ArrayList<ByteBuffer> ppsList;
//        private final FramesMP4MuxerTrack outTrack;
//        private final ByteBuffer _out;
//        private int frameNo;
//        private final MP4Muxer muxer;
//
//        int timescale = 25;
//        int duration = 1;
//
//        public SequenceEncoder(File out) throws IOException {
//            this.ch = NIOUtils.writableFileChannel(out);
//
//            // Muxer that will store the encoded frames
//            muxer = new MP4Muxer(ch, Brand.MP4);
//
//            // Add video track to muxer
//            outTrack = muxer.addTrack(TrackType.VIDEO, timescale);
//
//            // Allocate a buffer big enough to hold output frames
//            _out = ByteBuffer.allocate(1920 * 1080 * 6);
//
//            // Create an instance of encoder
//            encoder = new H264Encoder(new H264FixedRateControl(64));
//
//            // Transform to convert between RGB and YUV
//            transform = ColorUtil.getTransform(ColorSpace.RGB, encoder.getSupportedColorSpaces()[0]);
//
//            // Encoder extra data ( SPS, PPS ) to be stored in a special place of
//            // MP4
//            spsList = new ArrayList<>();
//            ppsList = new ArrayList<>();
//
//        }
//
//        public void encodeNativeFrame(Picture pic) throws IOException {
//            if (toEncode == null) {
//                toEncode = Picture.create(pic.getWidth(), pic.getHeight(), encoder.getSupportedColorSpaces()[0]);
//            }
//
//            // Perform conversion
//            transform.transform(pic, toEncode);
//
//            // Encode image into H.264 frame, the result is stored in '_out' buffer
//            _out.clear();
//            ByteBuffer result = encoder.encodeFrame(toEncode, _out);
//
//            // Based on the frame above form correct MP4 packet
//            spsList.clear();
//            ppsList.clear();
//            H264Utils.wipePS(result, spsList, ppsList);
//            H264Utils.encodeMOVPacket(result);
//
//            // Add packet to video track
//            outTrack.addFrame(new MP4Packet(result, frameNo, timescale, duration, frameNo, true, null, frameNo, 0));
//
//            frameNo++;
//        }
//
//        public void finish() throws IOException {
//            // Push saved SPS/PPS to a special storage in MP4
//            outTrack.addSampleEntry(H264Utils.createMOVSampleEntry(spsList, ppsList, 4));
//
//            // Write MP4 header and finalize recording
//            muxer.writeHeader();
//            NIOUtils.closeQuietly(ch);
//        }
//    }
}
