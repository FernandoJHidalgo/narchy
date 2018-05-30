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
package org.oakgp;

import org.oakgp.function.Function;
import org.oakgp.function.choice.If;
import org.oakgp.function.choice.OrElse;
import org.oakgp.function.classify.IsNegative;
import org.oakgp.function.classify.IsPositive;
import org.oakgp.function.classify.IsZero;
import org.oakgp.function.coll.Count;
import org.oakgp.function.compare.*;
import org.oakgp.function.hof.Filter;
import org.oakgp.function.hof.Reduce;
import org.oakgp.function.math.IntFunc;
import org.oakgp.node.ConstantNode;
import org.oakgp.node.FunctionNode;
import org.oakgp.node.Node;
import org.oakgp.node.VariableNode;
import org.oakgp.primitive.VariableSet;
import org.oakgp.rank.Candidates;
import org.oakgp.rank.RankedCandidate;
import org.oakgp.serialize.NodeReader;
import org.oakgp.serialize.NodeWriter;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.oakgp.Type.*;
import static org.oakgp.util.Utils.intArrayType;

public class TestUtils {
    public static final VariableSet VARIABLE_SET = VariableSet.createVariableSet(intArrayType(100));
    private static final Function[] FUNCTIONS = createDefaultFunctions();

    public static void assertVariable(int expectedId, Node node) {
        assertTrue(node instanceof VariableNode);
        assertEquals(expectedId, ((VariableNode) node).getId());
    }

    public static void assertConstant(Object expectedValue, Node node) {
        assertTrue(node instanceof ConstantNode);
        assertEquals(expectedValue, node.eval(null));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void assertUnmodifiable(List list) {
        assertEquals("java.util.Collections$UnmodifiableRandomAccessList", list.getClass().getName());
        try {
            list.add(new Object());
            fail("");
        } catch (UnsupportedOperationException e) {
            
        }
    }

    public static String writeNode(Node input) {
        return new NodeWriter().writeNode(input);
    }

    public static FunctionNode readFunctionNode(String input) {
        return (FunctionNode) readNode(input);
    }

    public static Node readNode(String input) {
        List<Node> outputs = readNodes(input);
        assertEquals(1, outputs.size());
        return outputs.get(0);
    }

    public static List<Node> readNodes(String input) {
        return readNodes(input, FUNCTIONS, VARIABLE_SET);
    }

    private static List<Node> readNodes(String input, Function[] functions, VariableSet variableSet) {
        List<Node> outputs = new ArrayList<>();
        try (NodeReader nr = new NodeReader(input, functions, new ConstantNode[0], variableSet)) {
            while (!nr.isEndOfStream()) {
                outputs.add(nr.readNode());
            }
        } catch (IOException e) {
            throw new RuntimeException("IOException caught reading: " + input, e);
        }
        return outputs;
    }

    private static Function[] createDefaultFunctions() {
        List<Function> functions = new ArrayList<>();

        functions.add(IntFunc.the.getAdd());
        functions.add(IntFunc.the.getSubtract());
        functions.add(IntFunc.the.getMultiply());
        functions.add(IntFunc.the.getDivide());

        functions.add(LessThan.create(integerType()));
        functions.add(LessThanOrEqual.create(integerType()));
        functions.add(new GreaterThan(integerType()));
        functions.add(new GreaterThanOrEqual(integerType()));
        functions.add(new Equal(integerType()));
        functions.add(new NotEqual(integerType()));

        functions.add(new If(integerType()));
        functions.add(new OrElse(stringType()));
        functions.add(new OrElse(integerType()));

        functions.add(new Reduce(integerType()));
        functions.add(new Filter(integerType()));
        functions.add(new org.oakgp.function.hof.Map(integerType(), booleanType()));

        functions.add(new IsPositive());
        functions.add(new IsNegative());
        functions.add(new IsZero());

        functions.add(new Count(integerType()));
        functions.add(new Count(booleanType()));

        return functions.toArray(new Function[functions.size()]);
    }

    public static Arguments createArguments(String... expressions) {
        Node[] args = new Node[expressions.length];
        for (int i = 0; i < expressions.length; i++) {
            args[i] = readNode(expressions[i]);
        }
        return new Arguments(args);
    }

    public static ConstantNode integerConstant(int value) {
        return new ConstantNode(value, integerType());
    }

    public static ConstantNode longConstant(long value) {
        return new ConstantNode(value, longType());
    }

    public static ConstantNode doubleConstant(double value) {
        return new ConstantNode(value, doubleType());
    }

    public static ConstantNode bigIntegerConstant(String value) {
        return new ConstantNode(new BigInteger(value), bigIntegerType());
    }

    public static ConstantNode bigDecimalConstant(String value) {
        return new ConstantNode(new BigDecimal(value), bigDecimalType());
    }

    public static ConstantNode booleanConstant(Boolean value) {
        return new ConstantNode(value, Type.booleanType());
    }

    public static ConstantNode stringConstant(String value) {
        return new ConstantNode(value, Type.stringType());
    }

    public static VariableNode createVariable(int id) {
        return VARIABLE_SET.getById(id);
    }

    public static void assertRankedCandidate(RankedCandidate actual, Node expectedNode, double expectedFitness) {
        assertSame(expectedNode, actual.node);
        assertEquals(expectedFitness, actual.fitness, 0.001f);
    }

    public static void assertNodeEquals(String expected, Node actual) {
        assertEquals(expected, writeNode(actual));
    }

    public static Candidates singletonRankedCandidates() {
        return singletonRankedCandidates(1);
    }

    public static Candidates singletonRankedCandidates(double fitness) {
        return new Candidates(new RankedCandidate[]{new RankedCandidate(mockNode(), fitness)});
    }

    public static Node mockNode() {
        return mockNode(integerType());
    }

    public static Node mockNode(Type type) {
        Node mockNode = mock(Node.class);
        given(mockNode.returnType()).willReturn(type);
        return mockNode;
    }
}
