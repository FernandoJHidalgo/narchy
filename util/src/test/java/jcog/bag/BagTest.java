package jcog.bag;

import jcog.Util;
import jcog.bag.impl.ArrayBag;
import jcog.bag.impl.CurveBag;
import jcog.bag.impl.PLinkArrayBag;
import jcog.bag.impl.hijack.DefaultHijackBag;
import jcog.list.FasterList;
import jcog.pri.PLink;
import jcog.pri.Pri;
import jcog.pri.PriReference;
import jcog.pri.Priority;
import jcog.pri.op.PriMerge;
import jcog.random.XorShift128PlusRandom;
import jcog.tensor.ArrayTensor;
import jcog.tensor.Tensor;
import org.apache.commons.math3.random.EmpiricalDistribution;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;

import static jcog.Texts.n4;
import static jcog.pri.op.PriMerge.plus;
import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @author me
 */
public class BagTest {


    @Test
    public void testBasicInsertionRemovalArray() {
        testBasicInsertionRemoval(new PLinkArrayBag<>(1, plus, new HashMap<>(1)));
    }


    static void testBasicInsertionRemoval(Bag<String, PriReference<String>> c) {


        assertEquals(1, c.capacity());
        if (!(c instanceof DefaultHijackBag)) {
            assertEquals(0, c.size());
            assertTrue(c.isEmpty());
        }

        //insert an item with (nearly) zero budget
        PLink x0 = new PLink("x", 2 * Pri.EPSILON);
        PriReference added = c.put(x0);
        assertSame(added, x0);
        c.commit();

        assertEquals(1, c.size());


        assertEquals(0, c.priMin(), Pri.EPSILON * 2);

        PriReference<String> x = c.get("x");
        assertNotNull(x);
        assertSame(x, x0);
        assertTrue(Priority.Zero.equalsBudget(x, 0.01f));

    }

    @Test
    public void testBudgetMerge() {
        PLinkArrayBag<String> a = new PLinkArrayBag<String>(4, plus, new HashMap<>(4));
        assertEquals(0, a.size());

        a.put(new PLink("x", 0.1f));
        a.put(new PLink("x", 0.1f));
        a.commit(null);
        assertEquals(1, a.size());


        PriReference<String> agx = a.get("x");
        Pri expect = new Pri(0.2f);
        assertTrue(agx + "==?==" + expect, expect.equalsBudget(
                agx, 0.01f));

    }

    @Test
    public void testSort() {
        PLinkArrayBag a = new PLinkArrayBag(4, plus, new HashMap<>(4));

        a.put(new PLink("x", 0.1f));
        a.put(new PLink("y", 0.2f));

        a.commit(null);

        Iterator<PriReference<String>> ii = a.iterator();
        assertEquals("y", ii.next().get());
        assertEquals("x", ii.next().get());


        assertEquals("[y=0.2, x=0.1]", a.listCopy().toString());

        a.put(new PLink("x", 0.2f));

        a.print();
        System.out.println(a.listCopy());

        a.commit();

        //x should now be ahead
        assertTrue(a.listCopy().toString().contains("[x="));
        assertTrue(a.listCopy().toString().contains(", y="));

        ii = a.iterator();
        assertEquals("x", ii.next().get());
        assertEquals("y", ii.next().get());

    }

    @Test
    public void testCapacity() {
        PLinkArrayBag a = new PLinkArrayBag(2, plus, new HashMap<>(2));

        a.put(new PLink("x", 0.1f));
        a.put(new PLink("y", 0.2f));
        a.commit(null);
        a.print();
        System.out.println();
        assertEquals(2, a.size());

        assertEquals(0.1f, a.priMin(), 0.01f);

        a.put(new PLink("z", 0.05f));
        a.commit();
        a.print();
        System.out.println();
        assertEquals(2, a.size());
        assertTrue(a.contains("x") && a.contains("y"));
        assertFalse(a.contains("z"));

    }

    @Test
    public void testRemoveByKey() {
        testRemoveByKey(new PLinkArrayBag(2, plus, new HashMap<>(2)));
    }

    public static void testRemoveByKey(Bag<String, PriReference<String>> a) {

        a.put(new PLink("x", 0.1f));
        a.commit();
        assertEquals(1, a.size());

        a.remove("x");
        a.commit();
        assertEquals(0, a.size());
        assertTrue(a.isEmpty());
        if (a instanceof ArrayBag) {
            assertTrue(((ArrayBag) a).listCopy().isEmpty());
            assertTrue(((ArrayBag) a).keySet().isEmpty());
        }

    }


    public static Random rng() {
        return new XorShift128PlusRandom(1);
    }

//
//    public static void testScalePutHalfs(float expect, Bag<String,PLink<String>> a, float... scales) {
//        for (float s : scales) {
//            a.put(new RawPLink("x", 0.5f), s, null);
//            Assert.assertNotNull(a.get("x"));
//        }
//        a.commit(null);
//
//
//        assertEquals(expect, a.get("x").pri(), 0.01f);
//        assertEquals(expect, a.pri("x", -1), 0.01f);
//    }
//
//    public static void testScalePut2(Bag<String,PLink<String>> a) {
//
//        a.put(new RawPLink("y",0.1f));
//        assertEquals(1, a.size());
//        a.put(new RawPLink("y",0.1f), 0.5f, null);
//        assertEquals(1, a.size());
//        a.put(new RawPLink("y",0.1f), 0.25f, null);
//        assertEquals(1, a.size());
//
//        a.commit(null);
//
//        assertTrue(a.contains("y"));
//
//        assertEquals(0.1 + 0.05 + 0.025, a.pri("y", -1), 0.001f);
//
//    }

    public static void printDist(@NotNull EmpiricalDistribution f) {
        System.out.println(f.getSampleStats().toString().replace("\n", " "));
        f.getBinStats().forEach(
                s -> {
                    /*if (s.getN() > 0)*/
                    System.out.println(
                            n4(s.getMin()) + ".." + n4(s.getMax()) + ":\t" + s.getN());
                }
        );
    }


    public static Tensor samplingPriDist(@NotNull Bag b, int batches, int batchSize, int bins) {

        assert(bins > 1);

        ArrayTensor f = new ArrayTensor(bins);
        //DoubleHistogram d = new DoubleHistogram(5);
        assertFalse(b.isEmpty());
        for (int i = 0; i < batches; i++) {
            b.sample(batchSize, (Consumer) x -> f.data[Util.bin(b.pri(x), bins)]++ /*d.recordValue(b.pri(x)*/);
        }
        int total = batches * batchSize;
        assertEquals(total, Util.sum(f.data), 0.001f);
        return f.scale(1f / total);
    }


//    @Test
//    public void testNormalization() {
//        int n = 64;
//        int bins = 5;
//        int samples = n * 32;
//
//        CurveBag fullDynamicRange = populated(n, Math::random);
//        EmpiricalDistribution unifDistr = getSamplingPriorityDistribution(fullDynamicRange, samples, bins);
//        printDist(unifDistr);
//
//        float ratioUniform = maxMinRatio(unifDistr);
//
//        //smaller dynamic range should be lesser probabailty difference from low to high
//        CurveBag smallDynamicRange = populated(n, () -> 0.1f * Math.random());
//        EmpiricalDistribution flatDistr = getSamplingPriorityDistribution(smallDynamicRange, samples, bins);
//        printDist(flatDistr);
//
//        float ratioFlat = maxMinRatio(flatDistr);
//
//        System.out.println(ratioUniform + " " + ratioFlat);
//
//        Assert.assertTrue(ratioUniform > 7f); //should be ideally ~10
//        Assert.assertTrue(ratioFlat < 7f); //should be ideally ~1
//
//    }

    private float maxMinRatio(@NotNull EmpiricalDistribution d) {
        List<SummaryStatistics> bins = d.getBinStats();
        return ((float) bins.get(bins.size() - 1).getN() / (bins.get(0).getN()));
    }

    @NotNull
    private CurveBag<PLink<String>> populated(int n, @NotNull DoubleSupplier random) {


        CurveBag<PLink<String>> a = curveBag(n, plus);


        //fill with uniform randomness
        for (int i = 0; i < n; i++) {
            a.put(new PLink("x" + i, (float) random.getAsDouble()));
        }

        a.commit();
        //a.printAll();

        return a;

    }

    public static void testPutMinMaxAndUniqueness(Bag<Integer, PriReference<Integer>> a) {
        float pri = 0.5f;
        int n = a.capacity() * 16; //insert enough to fully cover all slots. strings have bad hashcode when input iteratively so this may need to be a high multiple


        for (int i = 0; i < n; i++) {
            a.put(new PLink((i), pri));
        }

        a.commit(null); //commit but dont forget
        assertEquals(a.capacity(), a.size());

        //a.print();

        //System.out.println(n + " " + a.size());

        List<Integer> keys = new FasterList(a.capacity());
        a.forEachKey(keys::add);
        assertEquals(a.size(), keys.size());
        assertEquals(new HashSet(keys).size(), keys.size());

        assertEquals(pri, a.priMin(), 0.01f);
        assertEquals(a.priMin(), a.priMax(), 0.08f);

    }

    @NotNull
    public CurveBag<PLink<String>> curveBag(int n, PriMerge mergeFunction) {
        return new CurveBag(mergeFunction, new HashMap<>(n), new XorShift128PlusRandom(1), n);
    }

    public static void populate(Bag<String, PriReference<String>> b, Random rng, int count, int dimensionality, float minPri, float maxPri, float qua) {
        populate(b, rng, count, dimensionality, minPri, maxPri, qua, qua);
    }

    public static void populate(Bag<String, PriReference<String>> b, Random rng, int count, int dimensionality, float minPri, float maxPri, float minQua, float maxQua) {
        float dPri = maxPri - minPri;
        for (int i = 0; i < count; i++) {
            b.put(new PLink(
                    "x" + rng.nextInt(dimensionality),
                    rng.nextFloat() * dPri + minPri)
            );
        }
        b.commit(null);

    }


    @Test
    public void testCurveBagDistribution() {

        int cap = 64;

        CurveBag<PLink<String>> bag = curveBag(cap, PriMerge.plus);

        fillLinear(bag);

        //bag.forEach(System.out::println);

        Tensor f3 = samplingPriDist(bag, cap / 2, 4, 5);

        Tensor f2 = samplingPriDist(bag, cap * 2, 4, 5);


        int batches = cap * 100;
        int batchSize = 4;
        Tensor f1 = samplingPriDist(bag, batches, batchSize, 10);
        String h = "cap=" + cap + " samples=" + (batches * batchSize);
        System.out.println(h + ":\n\t" + f1.tsv2());
        System.out.println();

        float[] ff = f1.get();

        //monotonically increasing (test only the upper half because it flattens out at the bottom)
        int half = ff.length / 2;
        for (int j = half+1; j < ff.length; j++) {
            for (int i = half; i < j - 1; i++) {
                assertTrue(ff[j] > ff[i]);
            }
        }


        //TODO verify the histogram resulting from the above execution is relatively flat:
        //ex: [0.21649484536082475, 0.2268041237113402, 0.28865979381443296, 0.26804123711340205]
        //the tests below assume that it begins with a relatively flat distribution
//        System.out.println(Arrays.toString(bag.priHistogram(4)));
//        System.out.println(Arrays.toString(bag.priHistogram(8)));


//        System.out.print("Sampling: " );
//        printDist(samplingPriDistribution((CurveBag) n.core.concepts, 1000));
//        System.out.print("Priority: " );
//        EmpiricalDistribution pri;
//        printDist(pri = getSamplingPriorityDistribution(n.core.concepts, 1000));
//
//        List<SummaryStatistics> l = pri.getBinStats();
//        assertTrue(l.get(0).getN() < l.get(l.size() - 1).getN());

    }

    /**
     * fill it exactly to capacity
     */
    static void fillLinear(Bag<PLink<String>, PLink<String>> bag) {
        assertTrue(bag.isEmpty());
        int c = bag.capacity();
        for (int i = 0; i < c; i++) {
            float a = (float) (i) / c;
            float b = (float) (i + 1) / c;
            bag.put(new PLink("x" + i, (a+b)/2f)); //midpoint
        }
        bag.commit();
        assertEquals(c, bag.size());
        assertEquals(0.5f / c, bag.priMin(), 0.001f);
        assertEquals(1 - 1f/(c*2f), bag.priMax(), 0.001f); //no pressure should have been applied because capacity was only reached after the last put
    }


//    /** maybe should be in ArrayBagTest */
//    @Test public void inQueueTest() {
//
//        CurveBag<String> s = newBag(2);
//
//        //1. fill bag
//        s.put("a0", new RawBudget(0.25f, 0, 0));
//        s.put("a1", new RawBudget(0.25f, 0, 0));
//
//        //2. attempt to insert new under-budgeted item while bag is full
//        s.put("b", new RawBudget(0.2f, 0, 0));
//        assertEquals(2, s.size());
//
//        assertEquals(1, s.sizeQueue()); //b should be in the queue
//
//        s.commit(); //apply pending changes. try inserting items in the queue. if an item is not able to be inserted it remains buffering
//
//        //3. insert again, bringing "b" effective budget to 0.4
//        s.put("b", new RawBudget(0.2f, 0, 0));
//        assertEquals(2, s.size()); //still not actually in the bag
//        assertEquals("a0=$0.2500;0.0000;0.0000$,a1=$0.2500;0.0000;0.0000$", s.toStringDetailed());
//
//        assertEquals(1, s.sizeQueue()); //b should remain in the queue, accumulating the 2nd dose
//
//        s.commit(); //apply pending changes, while sorting. then try inserting items in the queue. if an item is
//
//        //4. if "b" has been successfully backlogged in the time prior to the commit,
//        //its accumulated budget is enough to displace a previous entry
//
//        assertEquals(2, s.size()); //still not actually in the bag
//        assertEquals(0, s.sizeQueue()); //commit has cleared queue
//        assertEquals("b=$0.4000;0.0000;0.0000$,a1=$0.2500;0.0000;0.0000$", s.toStringDetailed());
//
//        //TODO test queue finite size and
//
//    }

//    @NotNull
//    public static CurveBag<String> newBag(int n) {
//        Random rng = new XorShift128PlusRandom(1);
//        return new CurveBag(n, rng);
//    }


//
//    static final BagCurve curve = new CurveBag.FairPriorityProbabilityCurve();
//
//    @Test
//    public void testBagSampling() {
//
//        /** for testing normalization:
//         * these should produce similar results regardless of the input ranges */
//        testBags(0.25f, 0.75f,
//                1, 2, 3, 7, 12);
//        testBags(0.5f, 0.6f,
//                1, 2, 3, 7, 12);
//
//        testBags(0, 1.0f,
//                1, 2, 3, 6, 13, 27, 32, 64, 100);
//    }
//
//    public void testBags(float pMin, float pMax, int... capacities) {
//
//
//        //FractalSortedItemList<NullItem> f1 = new FractalSortedItemList<>();
//        //int[] d2 = testCurveBag(f1);
//        //int[] d3 = testCurveBag(new RedBlackSortedIndex<>());
//        //int[] d1 = testCurveBag(new ArraySortedIndex<>(40));
//
//
//        //use the final distribution to compare that each implementation generates exact same results
//        //assertTrue(Arrays.equals(d1, d2));
//        //assertTrue(Arrays.equals(d2, d3));
//
//        int repeats = 2;
//
//        System.out.println("Bag sampling distributions, inputs priority range=" + pMin + " .. " + pMax);
//        for (int capacity : capacities ) {
//
//
//            double[] total = new double[capacity];
//
//            for (int i = 0; i < repeats; i++) {
//                double[] count = testRemovalDistribution(pMin, pMax, capacity);
//                total = MathArrays.ebeAdd(total, count);
//            }
//
//            System.out.println("  " + capacity + ',' + " = " + Arrays.toString(total));
//
//        }
//
//    }
//
//    @Test  public void testCurveBag() {
//
//        ArraySortedIndex items = new ArraySortedIndex(1024);
//
//        testCurveBag(items);
//        testCurveBag(items);
//        testCapacityLimit(new CurveBag(curve, 4, rng));
//
//
//
//        testAveragePriority(4, items);
//        testAveragePriority(8, items);
//
//        int[] d = null;
//        for (int capacity : new int[] { 10, 51, 100, 256 } ) {
//            d = AbstractBagTest.testRemovalPriorityDistribution(items);
//        }
//
//
//    }
//
//    public void testCurveBag(SortedIndex<NullItem> items) {
//        CurveBag<CharSequence, NullItem> f = new CurveBag<>(items, curve, rng);
//        assertEquals(0, f.getPrioritySum(), 0.001);
//
//
//        NullItem ni;
//        f.put(ni = new NullItem(0.25f));
//        assertEquals(1, f.size());
//        assertEquals(ni.getPriority(), f.getPrioritySum(), 0.001);
//
//        f.put(new NullItem(0.9f));
//        f.put(new NullItem(0.75f));
//
//        //System.out.println(f);
//
//        //sorted
//        assertEquals(3, f.size());
//        assertTrue(f.getItems().toString(),
//                f.get(0).getPriority() > f.get(1).getPriority());
//
//        f.pop();
//
//        assertEquals(2, f.size());
//        f.pop();
//        assert(f.size() == 1);
//        f.pop();
//        assert(f.isEmpty());
//
//        assertEquals(0, f.getPrioritySum(), 0.01);
//    }
//
//    public void testCapacityLimit(Bag<CharSequence,NullItem> f) {
//
//        NullItem four = new NullItem(0.4f);
//        NullItem five = new NullItem(0.5f);
//
//        f.put(four); testOrder(f);
//
//
//
//        f.put(five); testOrder(f);
//
//        f.put(new NullItem(0.6f)); testOrder(f);
//
//
//        Item a = f.put(new NullItem(0.7f)); assertNull(a); testOrder(f);
//
//        assertEquals(4, f.size());
//        assertEquals(f.size(), f.keySet().size());
//        assertTrue(f.contains(five));    //5 should be in lowest position
//
//        System.out.println("x\n"); f.printAll();
//
//        f.put(new NullItem(0.8f)); //limit
//
//        System.out.println("x\n"); f.printAll(); testOrder(f);
//
//        assertEquals(4, f.size());
//    }
//
//    private void testOrder(Bag<CharSequence, NullItem> f) {
//        float max = f.getPriorityMax();
//        float min = f.getPriorityMin();
//
//        Iterator<NullItem> ii = f.iterator();
//
//        NullItem last = null;
//        do {
//            NullItem n = ii.next();
//            if (last == null)
//                assertEquals(max, n.getPriority(), 0.001);
//            else {
//                assertTrue(n.getPriority() <= last.getPriority() );
//            }
//
//            last = n;
//
//        } while (ii.hasNext());
//
//        assertEquals(min, last.getPriority(), 0.001);
//
//    }
//
//
//
//    public static double[] testRemovalDistribution(float priMin, float priMax, int capacity) {
//        int samples = 512 * capacity;
//
//        double[] count = new double[capacity];
//
//
//
//        CurveBag<CharSequence, NullItem> f = new CurveBag(curve, capacity, rng);
//
//        //fill
//        for (int i= 0; i < capacity; i++) {
//            f.put(new NullItem(priMin, priMax));
//            assertTrue(f.isSorted());
//        }
//
//        assertEquals(f.size(), f.capacity());
//
//
//        for (int i= 0; i < samples; i++) {
//            count[f.sample()]++;
//        }
//
//        assert(Util.isSemiMonotonicallyDec(count));
//
//        //System.out.println(random + " " + Arrays.toString(count));
//        //System.out.println(count[0] + " " + count[1] + " " + count[2] + " " + count[3]);
//
//        return count;
//    }
//
//    public void testAveragePriority(int capacity, SortedIndex<NullItem> items) {
//
//
//        float priorityEpsilon = 0.01f;
//
//        CurveBag<CharSequence, NullItem> c = new CurveBag<>(items, curve, rng);
//        LevelBag<CharSequence, NullItem> d = new LevelBag<>(capacity, 10);
//
//        assertEquals(c.getPrioritySum(), d.getPrioritySum(), 0);
//        assertEquals(c.getPriorityMean(), d.getPriorityMean(), 0);
//
//        c.printAll(System.out);
//
//        c.put(new NullItem(0.25f));
//        d.put(new NullItem(0.25f));
//
//        c.printAll(System.out);
//
//        //check that continuousbag and discretebag calculate the same average priority value
//        assertEquals(0.25f, c.getPriorityMean(), priorityEpsilon);
//        assertEquals(0.25f, d.getPriorityMean(), priorityEpsilon);
//
//        c.clear();
//        d.clear();
//
//        assertEquals(0, c.size());
//        assertEquals(0, d.size());
//        assertEquals(0, c.getPrioritySum(), 0.001);
//        assertEquals(0, d.getPrioritySum(), 0.001);
//        assertEquals(0, c.getPriorityMean(), 0.001);
//        assertEquals(0, d.getPriorityMean(), 0.001);
//
//        c.put(new NullItem(0.30f));
//        d.put(new NullItem(0.30f));
//        c.put(new NullItem(0.50f));
//        d.put(new NullItem(0.50f));
//
//        assertEquals(0.4, c.getPriorityMean(), priorityEpsilon);
//        assertEquals(0.4, d.getPriorityMean(), priorityEpsilon);
//
//    }
//
//
////    public void testCurveBag2(boolean random) {
////        ContinuousBag2<NullItem,CharSequence> f = new ContinuousBag2(4, new ContinuousBag2.PriorityProbabilityApproximateCurve(), random);
////
////        f.putIn(new NullItem(.25f));
////        assert(f.size() == 1);
////        assert(f.getMass() > 0);
////
////        f.putIn(new NullItem(.9f));
////        f.putIn(new NullItem(.75f));
////        assert(f.size() == 3);
////
////        //System.out.println(f);
////
////        //sorted
////        assert(f.items.first().getPriority() < f.items.last().getPriority());
////        assert(f.items.first().getPriority() < f.items.exact(1).getPriority());
////
////        assert(f.items.size() == f.nameTable.size());
////
////        assert(f.size() == 3);
////
////        f.takeOut();
////        assert(f.size() == 2);
////        assert(f.items.size() == f.nameTable.size());
////
////        f.takeOut();
////        assert(f.size() == 1);
////        assert(f.items.size() == f.nameTable.size());
////        assert(f.getMass() > 0);
////
////        f.takeOut();
////        assert(f.size() == 0);
////        assert(f.getMass() == 0);
////        assert(f.items.size() == f.nameTable.size());
////
////    }
//
//
//    @Test public void testEqualBudgetedItems() {
//        int capacity = 4;
//
//        CurveBag<CharSequence, NullItem> c = new CurveBag(curve, capacity, rng);
//        c.mergeAverage();
//
//        NullItem a, b;
//        c.put(a = new NullItem(0.5f));
//        c.put(b = new NullItem(0.5f));
//
//        assertEquals(2, c.size());
//
//        NullItem aRemoved = c.remove(a.name());
//
//        assertEquals(aRemoved, a);
//        assertNotEquals(aRemoved, b);
//        assertEquals(1, c.size());
//
//        c.put(a);
//        assertEquals(2, c.size());
//
//        NullItem x = c.peekNext();
//        assertNotNull(x);
//
//        assertEquals(2, c.size());
//
//        x = c.pop();
//
//        assertTrue(x.equals(a) || x.equals(b));
//        assertEquals(1, c.size());
//
//    }
//
//
//    @Test public void testMerge() {
//        int capacity = 3;
//
//        //final AtomicInteger putKey = new AtomicInteger(0);
//        //final AtomicInteger removeKey = new AtomicInteger(0);
//
//
//        CurveBag<CharSequence, NullItem> c = new CurveBag<>(curve, capacity, rng);
////
////
////            protected ArrayMapping<CharSequence, NullItem> newIndex(int capacity) {
////                return new ArrayMapping<CharSequence, NullItem>(
////                        //new HashMap(capacity)
////                        Global.newHashMap(capacity),
////                        items
////                ) {
////                    @Override
////                    public NullItem put(NullItem value) {
////                        return super.put(value);
////                    }
////
////                    @Override
////                    public NullItem putKey(CharSequence key, NullItem value) {
////                        putKey.incrementAndGet();
////                        return super.putKey(key, value);
////                    }
////
////                    @Override
////                    public NullItem remove(CharSequence key) {
////                        removeKey.incrementAndGet();
////                        return super.remove(key);
////                    }
////                };
////            }
////        };
//        c.mergePlus();
//
//        NullItem a = new NullItem(0.5f, "a");
//
//        c.put(a);
//
//        //assertEquals(1, putKey.get()); assertEquals(0, removeKey.get());
//        assertEquals(1, c.size());
//
//        c.put(a);
//
//        //assertEquals(2, putKey.get()); assertEquals(0, removeKey.get());
//        assertEquals(1, c.size());
//
//
//
//        //merged with plus, 0.5 + 0.5 = 1.0
//        assertEquals(1.0f, c.iterator().next().getPriority(), 0.001);
//
//
//        c.validate();
//
//        c.mergeAverage();
//
//        //for average merge, we need a new instance of same key
//        //but with different priority so that it doesnt modify itself (having no effect)
//        NullItem a2 = new NullItem(0.5f, "a");
//
//        c.put(a2);
//
//        //still only one element
//        assertEquals(1, c.size());
//
//        //but the merge should have decreased the priority from 1.0
//        assertEquals(0.833f, c.iterator().next().getPriority(), 0.001);
//
//
//        //finally, remove everything
//
//        c.remove(a.name());
//
//        //assertEquals(3, putKey.get()); assertEquals(1, removeKey.get());
//        assertEquals(0, c.size());
//
//        c.validate();
//
//    }
//

}
