package org.intelligentjava.machinelearning.decisiontree;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;

import static org.intelligentjava.machinelearning.decisiontree.label.BooleanLabel.FALSE_LABEL;
import static org.intelligentjava.machinelearning.decisiontree.label.BooleanLabel.TRUE_LABEL;
import static org.junit.jupiter.api.Assertions.*;

public class DecisionTreeGetLabelTest {

    final static Object it = Boolean.TRUE;

    @Test
    public void testGetLabelOnEmptyList() {
        DecisionTree tree = new DecisionTree();
        List<Function<Object,Object>> data = Lists.newArrayList();
        assertNull(DecisionTree.label(it, data, 0.9f));
    }

    @Test
    public void testGetLabelOnSingleElement() {
        DecisionTree tree = new DecisionTree();
        List<Function<Object,Object>> data = Lists.newArrayList();
        data.add(new TestValue(TRUE_LABEL));
        assertEquals("true", DecisionTree.label(it, data,0.9f).toString());
    }

    @Test
    public void testGetLabelOnTwoDifferent() {
        DecisionTree tree = new DecisionTree();
        List<Function<Object,Object>> data = Lists.newArrayList();
        data.add(new TestValue(TRUE_LABEL));
        data.add(new TestValue(FALSE_LABEL));
        assertNull(DecisionTree.label(it, data,0.9f));
    }

    @Test
    public void testGetLabelOn95vs5() {
        DecisionTree tree = new DecisionTree();
        List<Function<Object,Object>> data = Lists.newArrayList();
        for (int i = 0; i < 95; i++) {
            data.add(new TestValue(TRUE_LABEL));
        }
        for (int i = 0; i < 5; i++) {
            data.add(new TestValue(FALSE_LABEL));
        }
        assertEquals("true", DecisionTree.label(it, data,0.9f).toString());
    }

    @Test
    public void testGetLabelOn94vs6() {
        DecisionTree tree = new DecisionTree();

        List<Function<Object,Object>> homogenous = buildSample(96, 4);
        assertNotNull(DecisionTree.label(it, homogenous, 0.9f));


        List<Function<Object,Object>> nonhomogenous = buildSample(50, 50);
        assertNull(DecisionTree.label(it, nonhomogenous, 0.9f));
    }

    static List<Function<Object,Object>> buildSample(int a, int b) {
        List<Function<Object,Object>> homogenous = Lists.newArrayList();
        for (int i = 0; i < a; i++)
            homogenous.add(new TestValue(TRUE_LABEL));
        for (int i = 0; i < b; i++)
            homogenous.add(new TestValue(FALSE_LABEL));
        return homogenous;
    }

}
