package nars.term.visit;

import nars.term.Term;

import java.util.function.Consumer;

/**
 * TODO make a lighter-weight version which supplies only the 't' argument
 */
@FunctionalInterface
public interface SubtermVisitor extends Consumer<Term>
{

}
