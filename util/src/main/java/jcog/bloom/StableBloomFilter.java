package jcog.bloom;

import jcog.bloom.hash.HashProvider;

import java.util.Random;

/**
 * Stable Bloom Filters continuously "reset" random fields in the filter.
 * Deng and Rafiei have shown that by doing this, the FPR can be stabilised [1]. The disadvantage of this approach is that it introduces false negatives.
 * <p>
 * Created by jeff on 14/05/16.
 */
public class StableBloomFilter<E> implements CountingLeakySet<E> {

    private final HashProvider<E> hashProvider;
    private final byte[] cells;
    private final int numberOfCells;
    private final int numberOfHashes;
    private final Random rng;

    /**
     * Set the unlearning rate to make the {@link LeakySet} stable. The unlearning rate represents
     * a percentage of filter cells that will be "unlearned" with each write operation.
     *
     * @param unlearningRate Must be between 0.0 and 1.0.
     * @return {@link BloomFilterBuilder} For chaining.
     */
    private final int forget;

    public StableBloomFilter(int numberOfCells,
                             int numberOfHashes,
                             float forget,
                             Random rng,
                             HashProvider<E> hashProvider) {
        this.numberOfCells = numberOfCells;
        this.numberOfHashes = numberOfHashes;
        this.cells = new byte[numberOfCells];
        this.hashProvider = hashProvider;
        this.forget = (int) Math.ceil(numberOfCells * forget);
        this.rng = rng;
    }

    /**
     * if the element isnt contained, add it. return true if added, false if already present.
     */
    public boolean addIfMissing(E element) {
        return addIfMissing(element, 0, null);
    }

    public boolean addIfMissing(E element, float unlearnIfNew, Random rng) {
        int[] hash = hash(element);
        boolean c = contains(hash);
        if (!c) {
            if (unlearnIfNew > 0)
                unlearn(unlearnIfNew, rng);
            add(hash);
            return true;
        }
        return false;
    }

    @Override
    public void add(E element) {
        add(hash(element));
    }

    @Override
    public boolean contains(E element) {
        return contains(hash(element));
    }

    public void add(int[] indices) {
        for (int i = 0; i < numberOfHashes; i++) {
            increment(indices[i]);
        }
    }


    public boolean contains(int[] indices) {
        boolean mightContain = true;
        for (int i = 0; i < numberOfHashes; i++) {
            mightContain &= cells[indices[i]] > 0;
        }

        return mightContain;
    }


    @Override
    public void remove(E element) {
        int[] indices = hash(element);

        remove(indices);
    }

    public void remove(int[] indices) {
        for (int i = 0; i < numberOfHashes; i++) {
            decrement(indices[i]);
        }
    }


    public void unlearn(float rate, Random rng) {
        for (int i = 0; i < forget; i++) {
            int index = rng.nextInt(numberOfCells);
            decrement(index);
        }
    }


    public int[] hash(E element) {
        int[] hashes = new int[numberOfHashes];

        long h1 = hashProvider.hash1(element);
        long h2 = hashProvider.hash2(element);
        for (int i = 0; i < numberOfHashes; i++) {
            hashes[i] = Math.abs((int) ((h1 + i * h2) % numberOfCells));
        }

        return hashes;
    }

    private void decrement(int idx) {
        if (cells[idx] > 0) {
            cells[idx] -= 1;
        }
    }

    private void increment(int idx) {
        if (cells[idx] < Byte.MAX_VALUE) {
            cells[idx] += 1;
        }
    }

}
