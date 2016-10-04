package nars.video;

import boofcv.abst.transform.wavelet.WaveletTransform;
import boofcv.alg.transform.wavelet.UtilWavelet;
import boofcv.alg.transform.wavelet.WaveletTransformOps;
import boofcv.factory.transform.wavelet.FactoryWaveletTransform;
import boofcv.factory.transform.wavelet.GFactoryWavelet;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.ConvertRaster;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;
import boofcv.struct.wavelet.WlCoef_F32;
import nars.NAgent;
import nars.util.Util;
import nars.util.data.random.XorShift128PlusRandom;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.Random;
import java.util.function.Supplier;

import static java.lang.Math.max;
import static nars.util.Util.lerp;

/**
 * 2D wavelet transform of input image
 * https://github.com/lessthanoptimal/BoofCV/blob/v0.23/demonstrations/src/boofcv/demonstrations/transform/wavelet/WaveletVisualizeApp.java
 */
public class WaveletBag implements Bitmap2D {

    public static final int NUM_LEVELS = 3;
    final Supplier<BufferedImage> source;
    private int px;
    private int py;


    GrayF32 image;

    public final float[][] pixels;

    WaveletTransform<GrayF32,GrayF32,WlCoef_F32> waveletTran =
            FactoryWaveletTransform.create(GrayF32.class, GFactoryWavelet.haar(GrayF32.class), NUM_LEVELS,0,255);
    //private BufferedImage output;
    private GrayF32 imageWavelet;


    public WaveletBag(BufferedImage b, int px, int py) {
        this(()->b, px, py);
    }

    public WaveletBag(Supplier<BufferedImage> b, int px, int py) {
        this.source = b;

        this.pixels = new float[px][py];
        this.px = px;
        this.py = py;
    }

    @Override
    public void update() {
        //image = ConvertBufferedImage.convertFrom(source.get(), image);
        BufferedImage src = source.get();
        if (image == null || image.getWidth()!=src.getWidth() || image.getHeight()!=src.getHeight())
            image = new GrayF32(src.getWidth(), src.getHeight());
        bufferedToGray(src, image.data, image.startIndex, image.stride);

        imageWavelet = waveletTran.transform(image, imageWavelet);
        UtilWavelet.adjustForDisplay(imageWavelet, waveletTran.getLevels(), 1f);

        //output = VisualizeImageData.grayMagnitude(imageWavelet, output,255);
//        px = imageWavelet.getWidth();
//        py = imageWavelet.getHeight();
    }

    public static void bufferedToGray(BufferedImage src, float[] data, int dstStartIndex, int dstStride) {
        int width = src.getWidth();
        int height = src.getHeight();
        int x;
        int argb;
        int r;
        if(src.getType() == 10) {
            WritableRaster y = src.getRaster();
            float[] index = new float[1];

            for(x = 0; x < height; ++x) {
                argb = dstStartIndex + x * dstStride;

                for(r = 0; r < width; ++r) {
                    y.getPixel(r, x, index);
                    data[argb++] = index[0];
                }
            }
        } else {
            for(int var14 = 0; var14 < height; ++var14) {
                int var15 = dstStartIndex + var14 * dstStride;

                for(x = 0; x < width; ++x) {
                    argb = src.getRGB(x, var14);
                    r = argb >>> 16 & 255;
                    int g = argb >>> 8 & 255;
                    int b = argb & 255;
                    float ave = (float)(r + g + b) / 3.0F;
                    data[var15++] = ave;
                }
            }
        }

    }

    @Override
    public void see(EachPixelRGB p) {
        throw new UnsupportedOperationException("yet");

    }

    @Override
    public int width() {
        return px;
    }

    @Override
    public int height() {
        return py;
    }

    @Override
    public float brightness(int xx, int yy) {
        return imageWavelet!=null ? imageWavelet.get(xx,yy) : 0.5f;
    }



}
