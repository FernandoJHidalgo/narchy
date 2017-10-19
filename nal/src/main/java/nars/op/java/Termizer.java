package nars.op.java;

import jcog.Util;
import nars.term.Term;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.Nullable;

/**
 * Converts POJOs to NAL Term's, and vice-versa
 */
public interface Termizer {

	Term TRUE = Atomic.the("true");
	Term FALSE = Atomic.the("false");
	Term VOID = Atomic.the("void");
	Term EMPTY = Atomic.the("empty");
	Term NULL = Atomic.the("null");

	@Nullable
	Term term(Object o);

	@Nullable
	Object object(Term t);

	default Object[] object(Term[] t) {
		return Util.map(this::object, new Object[t.length], t);
	}
}
