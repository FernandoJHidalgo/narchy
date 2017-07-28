package jcog.version;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.function.Supplier;

import static org.junit.Assert.*;

/**
 * TODO test capacity restriction
 */
public class VersioningTest {

    @NotNull
    Versioning v = new Versioning(10, 10);
    @NotNull
    Versioned a = new Versioned(v, 8);
    @NotNull
    Versioned b = new Versioned(v, 8);

    @Test
    public void test1() {
        Versioning w = new Versioning(10, 10);
        VersionMap<Object,Object> m = new VersionMap(w);
        m.tryPut("x", "a");
        assertEquals("{x=a}", m.toString());
        assertEquals(1, w.size());
        m.tryPut("x", "b");

        assertEquals("{x=b}", m.toString());

        Versioned mvx = m.map.get("x");

        assertEquals("(a, b)", mvx.toStackString());
        assertEquals(2, w.size());
        assertEquals(2, mvx.size());

        w.revert(2); //should have no effect:
        assertEquals(2, w.size());
        assertEquals("(a, b)", mvx.toStackString());
        assertEquals(2, mvx.size());
        assertEquals(2, w.size());

        w.revert(1);
        assertEquals(1, w.size());
        assertEquals("{x=a}", m.toString());
        assertEquals("(a)", mvx.toStackString());
        assertEquals(1, mvx.size());
        assertEquals(1, w.size());

        w.revert(0);
        assertEquals(0, w.size());
        assertEquals(0, w.size());
        assertEquals(0, mvx.size());
        assertEquals("{}", m.toString());

        assertNull(m.get("x")); //removed from map because it did not exist at version 0 which is effectively empty


    }

    public void initTestSequence1(boolean print) {

        if (print) System.out.println(v);
        Versioned z = a.set("a0");
        assertNotNull(z);
        if (print) System.out.println(v);      a.set("a1");
        if (print) System.out.println(v);      b.set("b0");
        if (print) System.out.println(v);      a.set("a2");
        if (print) System.out.println(v);      a.set("a3");
        if (print) System.out.println(v);      b.set("b1");

    }

    @Test
    public void test2() {
        initTestSequence1();

        Supplier<String> s = () -> a + " " + b;

        System.out.println(v);
        assertEquals(6, v.size()); assertEquals("a3 b1", s.get());

        v.revert(5); System.out.println(v);
        assertEquals(5, v.size()); assertEquals("a3 b0", s.get());

        v.revert(4); System.out.println(v);
        assertEquals(4, v.size()); assertEquals("a2 b0", s.get());

        v.revert(3); System.out.println(v);
        assertEquals(3, v.size()); assertEquals("a1 b0", s.get());

        v.revert(2); System.out.println(v);
        assertEquals(2, v.size());  assertEquals("a1 null", s.get());

        v.revert(1); System.out.println(v);
        assertEquals(1, v.size());  assertEquals("a0 null", s.get());

        v.revert(0); System.out.println(v);
        assertEquals(0, v.size()); assertEquals("null null", s.get());

    }

    public void initTestSequence1() {
        initTestSequence1(false);
    }


    @Test
    public void testRevert() {

        initTestSequence1();

        Supplier<String> s = () -> a + " " + b;

        System.out.println(v);
        assertEquals(6, v.size()); assertEquals("a3 b1", s.get());
        assertEquals(6, v.size());

        System.out.println("revert to 3");

        //skip behind to halfway
        v.revert(3); System.out.println(v);
        assertEquals(3, v.size());
        assertEquals(3, v.size()); assertEquals("a1 b0", s.get());

        v.revert(2);  System.out.println(v);
        assertEquals(2, v.size());
        assertEquals(2, v.size()); assertEquals("a1 null", s.get());

    }

}