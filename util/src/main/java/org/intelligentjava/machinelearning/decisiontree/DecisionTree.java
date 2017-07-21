package org.intelligentjava.machinelearning.decisiontree;

import com.google.common.collect.Streams;
import com.google.common.graph.*;
import jcog.list.FasterList;
import org.eclipse.collections.api.block.function.primitive.IntToFloatFunction;
import org.intelligentjava.machinelearning.decisiontree.impurity.GiniIndexImpurityCalculation;
import org.intelligentjava.machinelearning.decisiontree.impurity.ImpurityCalculator;
import org.jetbrains.annotations.NotNull;

import java.io.PrintStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.*;
import static org.intelligentjava.machinelearning.decisiontree.DecisionTree.Node.leaf;

/**
 * Decision tree implementation.
 *
 * @author Ignas
 */
public class DecisionTree<K, V> {

    /**
     * When data is considered homogeneous and node becomes leaf and is labeled. If it is equal 1.0 then absolutely all
     * data must be of the same label that node would be considered a leaf.
     */
    public static final float DEFAULT_PRECISION = 0.90f;

    /**
     * Logger.
     */
    //private static final Logger log = LoggerFactory.getLogger(DecisionTree.class);

    /**
     * Impurity calculation method.
     */
    private final ImpurityCalculator impurityCalculator = new GiniIndexImpurityCalculation();
    /**
     * Max depth parameter. Growth of the tree is stopped once this depth is reached. Limiting depth of the tree can
     * help with overfitting, however if depth will be set too low tree will not be acurate.
     */
    private int maxDepth = 15;
    /**
     * Root node.
     */
    private Node<V> root;

    //    protected static <K,V> V label(K value, Collection<Function<K, V>> data) {
//        return DecisionTree.label(value, data, DEFAULT_PRECISION);
//    }
    public DecisionTree maxDepth(int d) {
        this.maxDepth = d;
        return this;
    }

    /**
     * Returns Label if data is homogeneous.
     */
    protected static <K, V> V label(K value, Collection<Function<K, V>> data, float homogenityPercentage) {
        // group by to map <Label, count>
        Map<V, Long> labelCount = data.stream().collect(groupingBy((x) -> x.apply(value), counting()));
        long totalCount = data.size();
        for (Map.Entry<V, Long> e : labelCount.entrySet()) {
            long nbOfLabels = e.getValue();
            if ((nbOfLabels / (double) totalCount) >= homogenityPercentage) {
                return e.getKey();
            }
        }
        return null;
    }

    /**
     * Get root.
     */
    public Node<V> root() {
        return root;
    }

    public Stream<Node.LeafNode<V>> leaves() {
        return root != null ? root.recurse().filter(Node::isLeaf).map(n -> (Node.LeafNode<V>) n).distinct() : Stream.empty();
    }

    /**
     * Trains tree on training data for provided features.
     *
     * @param value        The value column being learned
     * @param trainingData List of training data samples.
     * @param features     List of possible features.
     */
    public Node<V> put(K value, Collection<Function<K, V>> trainingData, List<Predicate<Function<K, V>>> features, IntToFloatFunction precision) {
        root = put(value, trainingData, features, 1, precision);
        return root;
    }

    /**
     * constant precision
     */
    public Node put(K value, Collection<Function<K, V>> data, List<Predicate<Function<K, V>>> features, float precision) {
        return put(value, data, features, (depth) -> precision);
    }

    /**
     * default constant precision
     */
    public Node put(K value, Collection<Function<K, V>> data, List<Predicate<Function<K, V>>> features) {
        return put(value, data, features, DEFAULT_PRECISION);
    }

    /**
     * Split data according to if it has this feature.
     *
     * @param data Data to by split by this feature.
     * @return Sublists of split data samples.
     */
    static <K, V> Stream<List<Function<K, V>>> split(Predicate<Function<K, V>> p, Collection<Function<K, V>> data) {
        // TODO:  maybe use sublist streams instead of creating new list just track indexes
        //  TODO maybe with bitset, Pair<sublist stream, bitset>
        // http://stackoverflow.com/questions/22917270/how-to-get-a-range-of-items-from-stream-using-java-8-lambda
        Map<Boolean, List<Function<K, V>>> split = data.stream().collect(partitioningBy(p::test));

        return Stream.of(split.get(true), split.get(false));
    }

    /**
     * Grow tree during training by splitting data recusively on best feature.
     *
     * @param data     List of training data samples.
     * @param features List of possible features.
     * @return Node after split. For a first invocation it returns tree root node.
     */
    protected Node<V> put(K key, Collection<Function<K, V>> data, List<Predicate<Function<K, V>>> features, int currentDepth, IntToFloatFunction depthToPrecision) {

        // if dataset already homogeneous enough (has label assigned) make this node a leaf
        V currentNodeLabel;
        if ((currentNodeLabel = label(key, data, depthToPrecision.valueOf(currentDepth))) != null) {
            return leaf(currentNodeLabel); //log.debug("New leaf is created because data is homogeneous: {}", currentNodeLabel.name());
        }

        int fs = features.size();
        boolean stoppingCriteriaReached = (fs == 0) || currentDepth >= maxDepth;
        if (stoppingCriteriaReached) {
            return leaf(majority(key, data)); //log.debug("New leaf is created because stopping criteria reached: {}", majorityLabel.name());
        }

        Predicate<Function<K, V>> split = bestSplit(key, data, features); // get best set of literals
        //log.debug("Best split found: {}", bestSplit.toString());

        // add children to current node according to split
        // if subset data is empty add a leaf with label calculated from initial data
        // else grow tree further recursively

        //log.debug("Data is split into sublists of sizes: {}", splitData.stream().map(List::size).collect(Collectors.toList()));
        Node<V> branch = split(split, data).map(

                subsetTrainingData -> subsetTrainingData.isEmpty() ?

                        leaf(majority(key, data))

                        :

                        put(key, subsetTrainingData,
                                new FasterList<>(() ->
                                        features.stream().filter(p -> !p.equals(split)).iterator(), fs - 1),
                                currentDepth + 1,
                                depthToPrecision
                        ))

                .collect(Collectors.toCollection(() -> Node.feature(split)));

        return (branch.size() == 1) ? branch.get(0) : branch;

    }

    /**
     * Classify a sample.
     *
     * @param value Data sample
     * @return Return label of class.
     */
    public V get(Function<K, V> value) {
        Node<V> node = root;
        while (!node.isLeaf()) { // go through tree until leaf is reached
            // only binary splits for now - has feature first child node(left branch), does not have feature second child node(right branch).
            node = node.get(node.feature.test(value) ? 0 : 1);
        }
        return (V) node.label;
    }

    /**
     * Finds best feature to split on which is the one whose split results in lowest impurity measure.
     */
    protected Predicate<Function<K, V>> bestSplit(K value, Collection<Function<K, V>> data, Iterable<Predicate<Function<K, V>>> features) {
        double currentImpurity = 1;
        Predicate<Function<K, V>> bestSplitFeature = null; // rename split to feature

        for (Predicate<Function<K, V>> feature : features) {

            // totalSplitImpurity = sum(singleLeafImpurities) / nbOfLeafs
            // in other words splitImpurity is average of leaf impurities
            double calculatedSplitImpurity =
                    split(feature, data).filter(list -> !list.isEmpty()).mapToDouble(splitData -> impurityCalculator.impurity(value, splitData)).average().orElse(Double.POSITIVE_INFINITY);
            if (calculatedSplitImpurity < currentImpurity) {
                currentImpurity = calculatedSplitImpurity;
                bestSplitFeature = feature;
            }
        }

        return bestSplitFeature;
    }

    /**
     * Differs from getLabel() that it always return some label and does not look at homogenityPercentage parameter. It
     * is used when tree growth is stopped and everything what is left must be classified so it returns majority label for the data.
     */
    static <K, V> V majority(K value, Collection<Function<K, V>> data) {
        // group by to map <Label, count> like in getLabels() but return Label with most counts
        return data.stream().collect(groupingBy((x) -> x.apply(value), counting())).entrySet().stream().max(Map.Entry.comparingByValue()).get().getKey();
    }

    // -------------------------------- TREE PRINTING ------------------------------------

    public ValueGraph<Node<V>,Boolean> graph() {

        MutableValueGraph<Node<V>,Boolean> graph = ValueGraphBuilder
                .directed()
                .nodeOrder(ElementOrder.unordered())
                .allowsSelfLoops(false)
                .build();

        return root.graph(graph);
    }

    public void print() {
        print(System.out);
    }

    public void print(PrintStream o) {
        printSubtree(root, o);
    }

    private void printSubtree(Node<?> node, PrintStream o) {
        if (!node.isEmpty() && node.get(0) != null) {
            print(node.get(0), true, "", o);
        }
        print(node, o);
        if (node.size() > 1 && node.get(1) != null) {
            print(node.get(1), false, "", o);
        }
    }

    private static void print(Node node, PrintStream o) {
        o.print(node);
        o.println();
    }

    private static <K> void print(Node<?> node, boolean isRight, K indent, PrintStream o) {
        if (!node.isEmpty() && node.get(0) != null) {
            print(node.get(0), true, indent + (isRight ? "        " : " |      "), o);
        }
        o.print(indent);
        if (isRight) {
            o.print(" /");
        } else {
            o.print(" \\");
        }
        o.print("----- ");
        print(node, o);
        if (node.size() > 1 && node.get(1) != null) {
            print(node.get(1), false, indent + (isRight ? " |      " : "        "), o);
        }
    }

    public static class Node<V> extends FasterList<Node<V>> implements Comparable<V> {


        /**
         * Node's feature used to split it further.
         */
        public final Predicate feature;

        public final V label;
        private final int hash;

        Node(Predicate feature) {
            this(feature, null);
        }

        private Node(Predicate feature, V label) {
            super(0);
            this.label = label;
            this.feature = feature;
            this.hash = Objects.hash(label, feature);
        }


        @Override
        public boolean add(Node<V> newItem) {
            if (contains(newItem))
                return false; //err
            return super.add(newItem);
        }

        @Override
        public void add(int index, Node element) {
            super.add(index, element);
        }

        public Stream<Node<V>> recurse() {
            return Stream.concat(
                    Stream.of(this),
                    !isEmpty() ?
                            Streams.concat(stream().map(Node::recurse).toArray(Stream[]::new))
                            :
                            Stream.empty());
        }

        public static <V> Node<V> feature(Predicate feature) {
            return new Node<V>(feature);
        }

        public static <V> Node<V> leaf(V label) {
            return new LeafNode<>(label);
        }

        public boolean isLeaf() {
            return feature == null || isEmpty();
        }

        public String toString() {
            return feature != null ? feature.toString() : label.toString();
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object that) {
            if (this == that) return true;
            else {
                if (feature != null)
                    if (!feature.equals(((Node) that).feature)) //branch
                        return false;
                return Objects.equals(label, (((Node) that).label)); //leaf
            }
        }

        @Override
        public int compareTo(@NotNull Object o) {
            if (o == this) return 0;
            Node n = (Node) o;
            if (feature != null) {
                int f = Integer.compare(feature.hashCode(), n.feature.hashCode()); //branch
                if (f != 0)
                    return f;
            }
            return ((Comparable) label).compareTo(n.label); //leaf
        }

        public MutableValueGraph<Node<V>,Boolean> graph(MutableValueGraph<Node<V>,Boolean> graph) {
            graph.addNode(this);

            int s = size();
            if (s ==2) {
                Node<V> ifTrue = get(0);
                ifTrue.graph(graph);
                graph.putEdgeValue(this, ifTrue, true);

                Node<V> ifFalse = get(1);
                ifFalse.graph(graph);
                graph.putEdgeValue(this, ifFalse, false);
            } else if (s == 0) {
                //nothing
            } else {
                throw new UnsupportedOperationException("predicate?");
            }

            return graph;
        }

        private static class LeafNode<V> extends Node<V> {
            public LeafNode(V label) {
                super(null, label);
            }


            @Override
            public String toString() {
                return label.toString();
            }

            //override other modifying methods

            @Override
            public boolean add(Node newItem) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void add(int index, Node element) {
                throw new UnsupportedOperationException();
            }
        }
    }
}
