/*
 * Copyright 2013-2014 Richard M. Hightower
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * __________                              _____          __   .__
 * \______   \ ____   ____   ____   /\    /     \ _____  |  | _|__| ____    ____
 *  |    |  _//  _ \ /  _ \ /    \  \/   /  \ /  \\__  \ |  |/ /  |/    \  / ___\
 *  |    |   (  <_> |  <_> )   |  \ /\  /    Y    \/ __ \|    <|  |   |  \/ /_/  >
 *  |______  /\____/ \____/|___|  / \/  \____|__  (____  /__|_ \__|___|  /\___  /
 *         \/                   \/              \/     \/     \/       \//_____/
 *      ____.                     ___________   _____    ______________.___.
 *     |    |____ ___  _______    \_   _____/  /  _  \  /   _____/\__  |   |
 *     |    \__  \\  \/ /\__  \    |    __)_  /  /_\  \ \_____  \  /   |   |
 * /\__|    |/ __ \\   /  / __ \_  |        \/    |    \/        \ \____   |
 * \________(____  /\_/  (____  / /_______  /\____|__  /_______  / / ______|
 *               \/           \/          \/         \/        \/  \/
 */
package org.boon.tests;

//import junit.framework.Assert;

import org.boon.Lists;
import org.boon.core.reflection.BeanUtils;
import org.boon.core.reflection.fields.FieldAccess;
import org.boon.criteria.ObjectFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.boon.criteria.internal.QueryFactory.filter;
import static org.boon.criteria.internal.QueryFactory.test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CriteriaTest {

    private List<TestClass> items;

    Map<String, FieldAccess> fields;

    class TestClass {

        int i;

        float f;

        String s;

        short st;

        List items;

        TestClass(int i, float f, String s, short st, String... items) {
            this.i = i;
            this.f = f;
            this.s = s;
            this.st = st;
            this.items = Lists.list(items);
        }
    }

    @BeforeEach
    public void setUp() throws Exception {
        fields = BeanUtils.getFieldsFromObject(TestClass.class);
        items = Lists.list(new TestClass(0, 0.1f, "a", (short) 1, "dog", "cat", "girl"), new TestClass(1, 1.1f, "a dog chased a cat today", (short) 2), new TestClass(2, 2.1f, null, (short) 3), new TestClass(3, 3.1f, "a", (short) 4), new TestClass(4, 4.1f, "a", (short) 5), new TestClass(5, 5.1f, "a", (short) 6), new TestClass(6, 6.1f, "a", (short) 7), new TestClass(7, 7.1f, "a", (short) 8), new TestClass(8, 8.1f, "a", (short) 9), new TestClass(9, 9.1f, "a", (short) 10));
    }

    @Test
    public void testIsNull() throws Exception {
        TestClass tc = items.get(2);
        assertTrue(test(tc, ObjectFilter.isNull("s")));
        Assertions.assertFalse(test(tc, ObjectFilter.notNull("s")));
    }

    @Test
    public void tesNotNull() throws Exception {
        TestClass tc = items.get(3);
        assertTrue(test(tc, ObjectFilter.notNull("s")));
        Assertions.assertFalse(test(tc, ObjectFilter.isNull("s")));
    }

    @Test
    public void testContains() throws Exception {
        TestClass tc = items.get(0);
        assertTrue(test(tc, ObjectFilter.contains("items", "cat")));
        Assertions.assertFalse(test(tc, ObjectFilter.contains("items", "Mountain Lion")));
    }

    @Test
    public void testContainsStr() throws Exception {
        TestClass tc = items.get(1);
        assertTrue(test(tc, ObjectFilter.contains("s", "cat")));
        Assertions.assertFalse(test(tc, ObjectFilter.contains("s", "Mountain Lion")));
    }

    @Test
    public void testNotContains() throws Exception {
        TestClass tc = items.get(0);
        Assertions.assertFalse(test(tc, ObjectFilter.notContains("items", "cat")));
        assertTrue(test(tc, ObjectFilter.notContains("items", "Mountain Lion")));
    }

    @Test
    public void testNotEmpty() throws Exception {
        TestClass tc = items.get(0);
        assertTrue(test(tc, ObjectFilter.notEmpty("items")));
    }

    @Test
    public void testEmpty() throws Exception {
        TestClass tc = items.get(9);
        assertTrue(test(tc, ObjectFilter.empty("items")));
    }

    @Test
    public void testNotContainsStr() throws Exception {
        TestClass tc = items.get(1);
        Assertions.assertFalse(test(tc, ObjectFilter.notContains("s", "cat")));
        assertTrue(test(tc, ObjectFilter.notContains("s", "Mountain Lion")));
    }

    @Test
    public void testIn() throws Exception {
        List<TestClass> results = filter(items, ObjectFilter.in("i", 5, 6, 7));
        assertEquals(3, results.size());
    }

    @Test
    public void testNotIn() throws Exception {
        List<TestClass> results = filter(items, ObjectFilter.notIn("i", 5, 6, 7));
        assertEquals(7, results.size());
    }

    @Test
    public void testNotInUsingNot() throws Exception {
        List<TestClass> results = filter(items, ObjectFilter.not(ObjectFilter.in("i", 5, 6, 7)));
        assertEquals(7, results.size());
    }

    @Test
    public void testBetween() throws Exception {
        List<TestClass> results = filter(items, ObjectFilter.between("i", 5, 10));
        assertEquals(5, results.size());
    }

    @Test
    public void testNotEqual() throws Exception {
        List<TestClass> results = filter(items, ObjectFilter.notEq("i", 5));
        assertEquals(9, results.size());
    }

    @Test
    public void testEqual() throws Exception {
        List<TestClass> results = filter(items, ObjectFilter.eq("i", 5));
        assertEquals(1, results.size());
        assertEquals(5, results.get(0).i);
    }

    @Test
    public void testGTE() throws Exception {
        List<TestClass> results = filter(items, ObjectFilter.gte("i", 5));
        assertEquals(5, results.size());
        assertEquals(5, results.get(0).i);
        assertEquals(6, results.get(1).i);
        assertEquals(7, results.get(2).i);
        assertEquals(8, results.get(3).i);
        assertEquals(9, results.get(4).i);
    }

    @Test
    public void testGT() throws Exception {
        List<TestClass> results = filter(items, ObjectFilter.gt("i", 5));
        assertEquals(4, results.size());
        assertEquals(6, results.get(0).i);
        assertEquals(7, results.get(1).i);
        assertEquals(8, results.get(2).i);
        assertEquals(9, results.get(3).i);
    }

    @Test
    public void testLT() throws Exception {
        List<TestClass> results = filter(items, ObjectFilter.lt("i", 5));
        assertEquals(5, results.size());
        assertEquals(0, results.get(0).i);
        assertEquals(4, results.get(4).i);
    }

    @Test
    public void testLTE() throws Exception {
        List<TestClass> results = filter(items, ObjectFilter.lte("i", 5));
        assertEquals(6, results.size());
        assertEquals(0, results.get(0).i);
        assertEquals(5, results.get(5).i);
    }
}
