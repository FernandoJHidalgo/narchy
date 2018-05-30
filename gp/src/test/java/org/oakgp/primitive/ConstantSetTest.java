/*
 * Copyright 2015 S. Webber
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http:
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.oakgp.primitive;

import org.junit.jupiter.api.Test;
import org.oakgp.node.ConstantNode;

import java.util.List;

import static java.lang.Boolean.TRUE;
import static org.junit.jupiter.api.Assertions.*;
import static org.oakgp.TestUtils.*;
import static org.oakgp.Type.*;

public class ConstantSetTest {
    @Test
    public void testGetByType() {
        ConstantNode c0 = integerConstant(7);
        ConstantNode c1 = booleanConstant(TRUE);
        ConstantNode c2 = integerConstant(5);

        ConstantSet s = new ConstantSet(c0, c1, c2);

        List<ConstantNode> integers = s.getByType(integerType());
        assertEquals(2, integers.size());
        assertSame(c0, integers.get(0));
        assertSame(c2, integers.get(1));

        List<ConstantNode> booleans = s.getByType(booleanType());
        assertEquals(1, booleans.size());
        assertSame(c1, booleans.get(0));

        assertNull(s.getByType(stringType()));
    }

    @Test
    public void assertGetByTypeUnmodifiable() {
        ConstantSet s = new ConstantSet(integerConstant(7), booleanConstant(TRUE), integerConstant(5));
        List<ConstantNode> integers = s.getByType(integerType());
        assertUnmodifiable(integers);
    }
}
