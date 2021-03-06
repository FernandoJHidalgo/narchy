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
 *      http:
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import jcog.tree.rtree.*;
import jcog.util.ArrayUtils;
import org.eclipse.collections.api.block.function.primitive.DoubleFunction;
import org.eclipse.collections.api.tuple.primitive.ObjectDoublePair;

import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;

/**
 * Fast RTree split suggested by Yufei Tao taoyf@cse.cuhk.edu.hk
 * <p>
 * Perform an axial split
 * <p>
 * Created by jcairns on 5/5/15.
 */
public final class AxialSplitLeaf<X> implements Split<X> {

    /** default stateless instance which can be re-used */
    public static final Split<?> the = new AxialSplitLeaf<>();

    public AxialSplitLeaf() { }

    @Override
    public Node<X> split(X x, Leaf<X> leaf, Spatialization<X> model) {


        HyperRegion rCombined = leaf.bounds.mbr(model.bounds(x));

        final int nD = rCombined.dim();

        
        int axis = 0;
        double mostCost = Double.NEGATIVE_INFINITY;
        for (int d = 0; d < nD; d++) {
            
            final double axisCost = rCombined.cost(d);
            if (axisCost > mostCost) {
                axis = d;
                mostCost = axisCost;
            }
        }

        
        final int splitDimension = axis;

        short size = (short) (leaf.size+1);
        ObjectDoublePair<X>[] sorted = new ObjectDoublePair[size];
        X[] ld = leaf.data;
        for (int i = 0; i < size; i++) {
            X li = i < size-1 ? ld[i] : x;
            double c = model.bounds(li).center(splitDimension); //TODO secondary sort by range
            sorted[i] = pair(li, -c /* negative since the ArrayUtils.sort below is descending */);
        }

        if (size > 1)
            ArrayUtils.sort(sorted, (DoubleFunction<ObjectDoublePair>) ObjectDoublePair::getTwo);





        
        final Leaf<X> l1Node = model.transfer(sorted, 0, size/2);
        final Leaf<X> l2Node = model.transfer(sorted, size / 2, size);



        //assert (l1Node.size()+l2Node.size() == size);

        //leaf.transfer(l1Node, l2Node, x, model);

        //assert (l1Node.size()+l2Node.size() == size);

        return model.newBranch(l1Node, l2Node);
    }




}
