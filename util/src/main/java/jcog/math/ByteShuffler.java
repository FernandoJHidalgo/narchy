package jcog.math;

import org.apache.commons.lang3.ArrayUtils;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

public class ByteShuffler {

    public final byte[] order;

    public ByteShuffler(int capacity) {
        this.order = new byte[capacity];
    }

    public byte[] shuffle(Random rng, int len) {
        assert(len >= 2 && len < 127);
        for (byte i = 0; i < ((byte)len); i++)
            order[i] = i; //caution: there could be numbers remaining beyond the set range here

        int rndInt = 0; //generate one 32 bit random integer every 4 cycles
        int generate = 0;
        for (int i=0; i < len; i++) {
            if ((generate++ & 4) == 0)
                rndInt = rng.nextInt();
            else
                rndInt >>= 8;
            int j = (rndInt & 0xff) % len;
            if (i!=j) {
                byte x = order[i];
                order[i] = order[j];
                order[j] = x;
            }
        }

        return order;
    }
}
