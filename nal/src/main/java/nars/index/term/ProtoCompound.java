package nars.index.term;

import nars.Op;
import nars.term.Term;
import nars.term.Termlike;
import nars.term.container.TermContainer;
import org.jetbrains.annotations.Nullable;

/**
 * a lightweight Compound builder
 *      - fast-write
 *      - hashed (for use as a key, etc)
 *
 * used for tentative construction of terms during critical
 * derivation and other purposes.
 *
 */
public interface ProtoCompound extends TermContainer {

    @Nullable
    Op op();

    /** subterms as an array for construction */
    @Override /*@NotNull*/ Term[] theArray();

    /** number subterms */
    @Override
    int subs();

    @Override
    boolean equals(Object obj);

    @Override
    int hashCode();

    @Override
    Term sub(int i);

    /** since it has potentially any structure... */
    @Override default int structure() {
        return ~0;
    }

    @Override
    default boolean hasAll(int structuralVector) {
        return true;
    }

    @Override
    default boolean hasAny(/*@NotNull*/ Op op) {
        return true;
    }

    @Override
    default boolean impossibleSubTerm( /*@NotNull*/ Termlike target) {
        return false;
    }

    @Override
    default boolean impossibleSubTermOrEquality(/*@NotNull*/ Term target) {
        return false;
    }

    @Override
    default boolean impossibleSubTermOrEqualityVolume(int otherTermsVolume) {
        return false;
    }

    @Override
    default boolean impossibleSubTermVolume(int otherTermVolume) {
        return false;
    }

    ProtoCompound commit();

}
