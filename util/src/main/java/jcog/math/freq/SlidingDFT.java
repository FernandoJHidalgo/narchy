/*
 *  SlidingDFT.java
 *  Eisenkraut
 *
 *  Copyright (c) 2004-2017 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */
package jcog.math.freq;

import org.jetbrains.annotations.Nullable;

/**
 * For the sliding DFT algorithm, see for example
 * Bradford/Dobson/ffitch "Sliding is smoother than jumping".
 *
 *
 * The FFT provides you with amplitude and phase. The amplitude is encoded as the magnitude of the complex number (sqrt(x^2+y^2)) while the phase is encoded as the angle (atan2(y,x)). To have a strictly real result from the FFT, the incoming signal must have even symmetry (i.e. x[n]=conj(x[N-n])).
 *  If all you care about is intensity, the magnitude of the complex number is sufficient for analysis.
 * https://stackoverflow.com/questions/10304532/why-does-fft-produce-complex-numbers-instead-of-real-numbers#10304604
 */
public class SlidingDFT {
    private final int fftSize;
    private final int fftSizeP2;
    private final int bins;


    private final float[] cos;
    private final float[] sin;
    private final float[][] timeBuf;
    private final float[][] fftBufD;
    private final int[] timeBufIdx;


    public SlidingDFT(int fftSize, int numChannels) {
        this.fftSize = fftSize;

        bins = fftSize / 2;
        fftSizeP2 = fftSize + 2;
        fftBufD = new float[numChannels][fftSizeP2];

        cos = new float[bins + 1];
        sin = new float[bins + 1];
        timeBuf = new float[numChannels][fftSize];
        timeBufIdx = new int[numChannels];


        float d1 = (float) (Math.PI * 2 / fftSize);
        final int binsH = bins / 2;
        for (int i = 0, j = bins, k = binsH, m = binsH; i < binsH; i++, j--, k--, m++) {
            float d2 = (float) Math.cos(d1 * i);
            cos[i] = d2;
            cos[j] = -d2;
            sin[k] = d2;
            sin[m] = d2;
        }
    }

    public void next(float[] inBuf, int chan, float[] fftBuf) {
        next(inBuf, 0, inBuf.length, chan, fftBuf);
    }

    public void nextFreq(float[] inBuf, int chan, float[] fftBuf) {
        next(inBuf, 0, inBuf.length, chan, null);

        float[] f = fftBufD[chan];
        int n = (f.length-2);
        assert(fftBuf.length >= n/2);
        int k = 0;
        for (int i = 2; i < f.length; ) {
            float real = f[i++];
            float imag = f[i++];
            float amp = real*real + imag*imag;
            fftBuf[k++] = amp;
        }

    }

    private void next(float[] inBuf, int inOff, int inLen, int chan, @Nullable float[] fftBuf) {

        if (inLen == 0 || inBuf.length == 0)
            return;

        final float[] fftBufDC = fftBufD[chan];
        final float[] timeBufC = timeBuf[chan];
        int timeBufIdxC = timeBufIdx[chan];
        float delta, re1, im1, re2, im2;
        float f1;


        for (int i = 0, j = inOff; i < inLen; i++, j++) {
            f1 = inBuf[j];
            delta = f1 - timeBufC[timeBufIdxC];

            timeBufC[timeBufIdxC] = f1;
            for (int k = 0, m = 0; m < fftSizeP2; k++) {

                re1 = fftBufDC[m] + ((k & 1) == 0 ? +1 : -1) * delta;
                im1 = fftBufDC[m + 1];

                re2 = cos[k];
                im2 = sin[k];

                fftBufDC[m++] = re1 * re2 - im1 * im2;
                fftBufDC[m++] = re1 * im2 + re2 * im1;
            }
            if (++timeBufIdxC == fftSize) timeBufIdxC = 0;
        }
        timeBufIdx[chan] = timeBufIdxC;

        if (fftBuf!=null)
            System.arraycopy(fftBufDC, 0, fftBuf, 0, Math.min(fftBuf.length, fftSize));

    }
}