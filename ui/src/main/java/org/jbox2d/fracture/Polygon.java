package org.jbox2d.fracture;

import org.jbox2d.collision.AABB;
import org.jbox2d.common.Settings;
import org.jbox2d.fracture.fragmentation.Arithmetic;
import org.jbox2d.fracture.hertelmehlhorn.SingletonHM;
import org.jbox2d.fracture.poly2Tri.Triangulation;
import spacegraph.math.Tuple2f;
import spacegraph.math.v2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * Polygon - je reprezentovany postupnostou vrcholov
 *
 * @author Marek Benovic
 */
public class Polygon implements Iterable<Tuple2f>, Cloneable {
    private static final float AABBConst = 1;
    private static final SingletonHM HM = new SingletonHM();

    /**
     * Pole vrcholov.
     */
    protected Tuple2f[] array;

    /**
     * Pocet vrcholov.
     */
    protected int count;

    /**
     * Vytvori prazdny polygon bez vrcholov. Polygon moze byt konvexny,
     * konkavny aj nonsimple.
     */
    public Polygon() {
        array = new Tuple2f[8];
        count = 0;
    }

    /**
     * Vytvori polygon z danych vrcholov. Vlozi dane body do polygonu. Pole
     * z parametra sa preda referenciou (nedochadza ku klonovaniu).
     *
     * @param va Vstupne vrcholy.
     */
    public Polygon(Tuple2f[] va) {
        array = va;
        count = va.length;
    }

    /**
     * Vytvori polygon z danych vrcholov. Vlozi dane body do polygonu. Pole
     * z parametra sa preda referenciou (nedochadza ku klonovaniu).
     *
     * @param va Vstupne vrcholy.
     * @param n  Pocet aktivnych vrcholov
     */
    public Polygon(Tuple2f[] va, int n) {
        array = va;
        count = n;
    }

    /**
     * Vlozi do Polygonu prvky z kolekcie.
     *
     * @param c Kolekcia s vrcholmi.
     */
    public void add(Collection<? extends Tuple2f> c) {
        for (Tuple2f v : c) {
            add(v);
        }
    }

    /**
     * Prida vrchol do polygonu
     *
     * @param v Pridavany vrchol
     */
    public void add(Tuple2f v) {
        if (array.length == count) {
            Tuple2f[] newArray = new Tuple2f[count * 2];
            System.arraycopy(array, 0, newArray, 0, count);
            array = newArray;
        }
        array[count++] = v;
    }

    /**
     * @param index
     * @return Vrati prvok na danom mieste
     */
    public Tuple2f get(int index) {
        return array[index];
    }

    /**
     * @return Vrati pocet prvkov
     */
    public int size() {
        return count;
    }

    /**
     * @param index Index bodu
     * @return Vrati vrchol podla poradia s osetrenim pretecenia.
     */
    public Tuple2f cycleGet(int index) {
        return get(index % count);
    }

    /**
     * @return Vrati v poli vrcholy polygonu - vrati referenciu na interne pole,
     * preto pri iterovani treba brat pocet cez funkciu size a nie
     * cez array.length.
     */
    public Tuple2f[] getArray() {
        return array;
    }

    /**
     * Existuje efektivnejsia implementacia v pripade, ze bodov je viacej.
     * http://alienryderflex.com/polygon/
     * Este upravena by bola vziat vsetky hrany
     *
     * @param p
     * @return Vrati true.
     */
    public boolean inside(Tuple2f p) {
        int i, j;
        boolean c = false;
        Tuple2f v = new v2();
        for (i = 0, j = count - 1; i < count; j = i++) {
            Tuple2f a = get(i);
            Tuple2f b = get(j);
            v.set(b);
            v.subbed(a);
            if (((a.y >= p.y) != (b.y >= p.y)) && (p.x <= v.x * (p.y - a.y) / v.y + a.x)) {
                c = !c;
            }
        }
        return c;
    }

    /**
     * @return Vrati hmotnost telesa.
     */
    public double mass() {
        double m = 0;
        for (int i = 0, j = 1; i != count; i = j, j++) {
            Tuple2f b1 = get(i);
            Tuple2f b2 = get(j == count ? 0 : j);
            m += Tuple2f.cross(b1, b2);
        }
        m = Math.abs(m / 2);
        return m;
    }

    /**
     * @return Vrati tazisko polygonu.
     */
    public Tuple2f centroid() {
        Tuple2f C = new v2(); //centroid
        double m = 0;
        Tuple2f g = new v2(); //pomocne vektor pre medzivypocet
        for (int i = 0, j = 1; i != count; i = j, j++) {
            Tuple2f b1 = get(i);
            Tuple2f b2 = get(j == count ? 0 : j);
            float s = Tuple2f.cross(b1, b2);
            m += s;
            g.set(b1);
            g.added(b2);
            g.scaled(s);
            C.added(g);
        }
        C.scaled((float) (1 / (3 * m)));
        return C;
    }

    /**
     * @return Vrati najvacsiu vzdialenost 2 bodov.
     */
    private double radius() {
        double ln = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < count; ++i) {
            ln = Math.max(Arithmetic.distanceSq(get(i), cycleGet(i + 1)), ln);
        }
        return Math.sqrt(ln);
    }

    /**
     * @return Ak je polygon priliz maly, alebo tenky (nieje dobre ho zobrazovat), vrati false.
     */
    public boolean isCorrect() {
        double r = radius();
        double mass = mass();
        return (r > Material.MINFRAGMENTSIZE && mass > Material.MINFRAGMENTSIZE && mass / r > Material.MINFRAGMENTSIZE);
    }

    /**
     * @return Vrati AABB pre Polygon sluziaci na rozsah generovanych ohnisk pre
     * fraktury. Preto je to umelo nafunknute o konstantu 1.
     */
    public AABB getAABB() {

        if (count == 0) {
            return null;
        } else {
            float minX = Float.POSITIVE_INFINITY;
            float minY = Float.POSITIVE_INFINITY;
            float maxX = Float.NEGATIVE_INFINITY;
            float maxY = Float.NEGATIVE_INFINITY;
            for (int i = 0; i < count; ++i) {
                Tuple2f v = get(i);
                minX = Math.min(v.x, minX);
                maxX = Math.max(v.x, maxX);
                minY = Math.min(v.y, minY);
                maxY = Math.max(v.y, maxY);
            }
            return new AABB(
                    new v2(minX - AABBConst, minY - AABBConst),
                    new v2(maxX + AABBConst, maxY + AABBConst), false);
        }
    }

    /**
     * @return Vezme dany polygon a urobi konvexnu dekompoziciu - rozpad na konvexne polygony
     * s referencnou zavislostou (spolocne vrcholy polygonov su rovnake instancie).
     */
    public Polygon[] convexDecomposition() {
        if (isSystemPolygon()) { //optimalizacia - je zbytocne spustat algoritmus, ked to nieje zapotreby
            return new Polygon[]{this};
        }

        //tu pustim triangulacu z poly2tri
        Tuple2f[] reverseArray = new Tuple2f[count];
        for (int i = 0; i < count; ++i) {
            reverseArray[i] = get(count - i - 1);
        }

        ArrayList<int[]> triangles = Triangulation.triangulate(reverseArray, count);

        int c = triangles.size();

        int[][] list = new int[c][3];
        for (int i = 0; i < c; i++) {
            int[] t = triangles.get(i);
            list[i][0] = t[0];
            list[i][1] = t[1];
            list[i][2] = t[2];
        }

        HM.calculate(list, reverseArray, Settings.maxPolygonVertices);
        return HM.dekomposition;
    }

    /**
     * Otoci poradie prvkov v poli.
     */
    public void flip() {
        Tuple2f temp;
        int size = size();
        int n = size() >> 1;
        for (int i = 0; i < n; i++) {
            temp = array[i];
            int j = size - 1 - i;
            array[i] = array[j];
            array[j] = temp;
        }
    }

    /**
     * @return Vrati true, pokial je polygon konvexny a pocet vrcholov je maxPolygonVertices
     */
    private boolean isSystemPolygon() {
        return isConvex() && count <= Settings.maxPolygonVertices;
    }

    /**
     * @return Vrati true, pokial je polygon konvexny.
     */
    private boolean isConvex() {
        for (int i = 0; i < count; i++) {
            Tuple2f a = get(i);
            Tuple2f b = cycleGet(i + 1);
            Tuple2f c = cycleGet(i + 2);
            if (Arithmetic.siteDef(a, b, c) == 1) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return Vrati true, pokial je postupnost vrcholov v smere hodinovych ruciciek
     */
    public boolean isClockwise() {
        double signedArea = 0;
        for (int i = 0; i < size(); ++i) {
            Tuple2f v1 = get(i);
            Tuple2f v2 = cycleGet(i + 1);
            double v1x = v1.x;
            double v1y = v1.y;
            double v2x = v2.x;
            double v2y = v2.y;
            signedArea += v1x * v2y - v2x * v1y;
        }
        return signedArea < 0;
    }


    /**
     * @return Vrati novy polygon. Pole je realokovane, ale referencie na
     * body (Tuple2f) su povodne.
     */
    @Override
    public Polygon clone() {
        Tuple2f[] newArray = new Tuple2f[count];
        int newCount = count;
        System.arraycopy(array, 0, newArray, 0, count);
        return new Polygon(newArray, newCount);
    }

    /**
     * @return Vrati iterator na vrcholy polygonu
     */
    @Override
    public Iterator<Tuple2f> iterator() {
        return new MyIterator();
    }

    private class MyIterator implements Iterator<Tuple2f> {
        private int index;

        public MyIterator() {
            index = 0;
        }

        public MyIterator(int index) {
            this.index = index;
        }

        @Override
        public boolean hasNext() {
            return index < count;
        }

        @Override
        public Tuple2f next() {
            return get(index++);
        }
    }
}
