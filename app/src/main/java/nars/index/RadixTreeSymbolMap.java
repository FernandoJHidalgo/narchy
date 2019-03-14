package nars.index;

import com.googlecode.concurrenttrees.common.CharSequences;
import com.googlecode.concurrenttrees.common.PrettyPrinter;
import com.googlecode.concurrenttrees.radix.node.Node;
import com.googlecode.concurrenttrees.radix.node.NodeFactory;
import com.googlecode.concurrenttrees.radix.node.concrete.bytearray.*;
import com.googlecode.concurrenttrees.radix.node.concrete.chararray.*;
import com.googlecode.concurrenttrees.radix.node.concrete.voidvalue.VoidValue;
import com.googlecode.concurrenttrees.radix.node.util.NodeUtil;
import nars.$;
import nars.index.symbol.SymbolMap;
import nars.term.Term;
import nars.term.Termed;
import nars.util.MyConcurrentRadixTree;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * String interner that maps strings to integers and resolves them
 * bidirectionally with a globally shared Atomic concept
 */
public class RadixTreeSymbolMap extends MyConcurrentRadixTree<Termed> implements SymbolMap {


    public RadixTreeSymbolMap() {
        super(new AtomNodeFactory());
    }


    @Override
    public final Termed resolve(String id) {
        return getValueForExactKey(id);
    }



    @Override
    public final Termed resolveOrAdd(String s, Function<Term, ? extends Termed> conceptBuilder) {
        return putIfAbsent(s, () -> conceptBuilder.apply($.the(s)) );
    }


    /**
     * // PrettyPrintable is a non-public API for testing, prints semi-graphical representations of trees...
     */
    @Override public void print(Appendable out) {
        PrettyPrinter.prettyPrint(this, out);
    }

    @Override
    public void forEach(Consumer<? super Termed> c) {
        throw new UnsupportedOperationException(); //TODO
    }


    private static final class AtomNodeFactory implements NodeFactory {

        public static final boolean DEBUG = false;

        @Override
        public Node createNode(CharSequence edgeCharacters, Object value, List<Node> childNodes, boolean isRoot) {
            if (DEBUG) {
                assert edgeCharacters != null : "The edgeCharacters argument was null";
                assert !(!isRoot && edgeCharacters.length() == 0) : "Invalid edge characters for non-root node: " + CharSequences.toString(edgeCharacters);
                assert childNodes != null : "The childNodes argument was null";
                NodeUtil.ensureNoDuplicateEdges(childNodes);
            }

            try {

                if (childNodes.isEmpty()) {
                    // Leaf node...
                    if (value instanceof VoidValue) {
                        return new ByteArrayNodeLeafVoidValue(edgeCharacters);
                    } else if (value != null) {
                        return new ByteArrayNodeLeafWithValue(edgeCharacters, value);
                    } else {
                        return new ByteArrayNodeLeafNullValue(edgeCharacters);
                    }
                } else {
                    // Non-leaf node...
                    if (value instanceof VoidValue) {
                        return new ByteArrayNodeNonLeafVoidValue(edgeCharacters, childNodes);
                    } else if (value == null) {
                        return new ByteArrayNodeNonLeafNullValue(edgeCharacters, childNodes);
                    } else {
                        return new ByteArrayNodeDefault(edgeCharacters, value, childNodes);
                    }
                }
            } catch (ByteArrayCharSequence.IncompatibleCharacterException e) {

                if (childNodes.isEmpty()) {
                    // Leaf node...
                    if (value instanceof VoidValue) {
                        return new CharArrayNodeLeafVoidValue(edgeCharacters);
                    } else if (value != null) {
                        return new CharArrayNodeLeafWithValue(edgeCharacters, value);
                    } else {
                        return new CharArrayNodeLeafNullValue(edgeCharacters);
                    }
                } else {
                    // Non-leaf node...
                    if (value instanceof VoidValue) {
                        return new CharArrayNodeNonLeafVoidValue(edgeCharacters, childNodes);
                    } else if (value == null) {
                        return new CharArrayNodeNonLeafNullValue(edgeCharacters, childNodes);
                    } else {
                        return new CharArrayNodeDefault(edgeCharacters, value, childNodes);
                    }
                }
            }
        }
    }

}

//    final class InternedAtom extends Atomic implements Node /* implements Concept */ {
//
//        private final int id;
//
//        InternedAtom(int id) {
//            this.id = id;
//
//        }
//
//        @Override
//        public Character getIncomingEdgeFirstCharacter() {
//            return null;
//        }
//
//        @Override
//        public CharSequence getIncomingEdge() {
//            return null;
//        }
//
//        @Override
//        public Object getValue() {
//            return this;
//        }
//
//        @Override
//        public Node getOutgoingEdge(Character edgeFirstCharacter) {
//            return null;
//        }
//
//        @Override
//        public void updateOutgoingEdge(Node childNode) {
//
//        }
//
//        @Override
//        public List<Node> getOutgoingEdges() {
//            return null;
//        }
//
//        @Override
//        public
//        @Nullable
//        String toString() {
//            return Integer.toString(id);
//        }
//
//        @Override
//        public
//        @Nullable
//        Op op() {
//            return null;
//        }
//
//        @Override
//        public int complexity() {
//            return 0;
//        }
//
//        @Override
//        public int varIndep() {
//            return 0;
//        }
//
//        @Override
//        public int varDep() {
//            return 0;
//        }
//
//        @Override
//        public int varQuery() {
//            return 0;
//        }
//
//        @Override
//        public int varPattern() {
//            return 0;
//        }
//
//        @Override
//        public int vars() {
//            return 0;
//        }
//
//        @Override
//        public int compareTo(Object o) {
//            return 0;
//        }
//    }
