package jcog.tree.rtree.split;

/*
 * #%L
 * Conversant RTree
 * ~~
 * Conversantmedia.com © 2016, Conversant, Inc. Conversant® is a trademark of Conversant, Inc.
 * ~~
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import jcog.tree.rtree.*;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Fast RTree split suggested by Yufei Tao taoyf@cse.cuhk.edu.hk
 * <p>
 * Perform an axial split
 * <p>
 * Created by jcairns on 5/5/15.
 */
public final class AxialSplitLeaf<T> implements Split<T> {


    @Override
    public Node<T, ?> split(T t, Leaf<T> leaf, Spatialization<T> model) {


        HyperRegion r = leaf.region;

        final int nD = r.dim();

        // choose axis to split
        int axis = 0;
        double rangeD = r.range(0);
        for (int d = 1; d < nD; d++) {
            // split along the greatest range extent
            final double dr = r.rangeIfFinite(d, 0);
            if (dr > rangeD) {
                axis = d;
                rangeD = dr;
            }
        }

        // sort along split dimension
        final int splitDimension = axis;

        short size = leaf.size;
        final HyperRegion[] sortedMbr = HyperRegion.toArray(leaf.data, size, model.region);
        Arrays.sort(sortedMbr, Comparator.comparingDouble(o -> o.center(splitDimension)));

        // divide sorted leafs
        final Leaf<T> l1Node = model.transfer(leaf, sortedMbr, 0, size/2);
        final Leaf<T> l2Node = model.transfer(leaf, sortedMbr, size / 2, size);

        leaf.classify(l1Node, l2Node, t, model, new boolean[1] /* dummy */);

        return model.newBranch(l1Node, l2Node);
    }




}
