package nars.io;

import nars.Op;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Terms;
import nars.term.atom.Atomic;

import java.io.IOException;
import java.util.function.Function;

import static nars.Op.*;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;

/** prints readable forms of terms */
public enum TermAppender { ;

    static void compoundAppend(Compound c, Appendable p, Op op) throws IOException {

        p.append(Op.COMPOUND_TERM_OPENER);

        op.append(c, p);

        Subterms cs = c.subterms();
        if (cs.subs() == 1)
            p.append(Op.ARGUMENT_SEPARATOR);

        appendArgs(cs, p);

        p.append(Op.COMPOUND_TERM_CLOSER);

    }

    static void compoundAppend(String o, Subterms c, Function<Term, Term> filter, Appendable p) throws IOException {

        p.append(Op.COMPOUND_TERM_OPENER);

        p.append(o);

        if (c.subs() == 1)
            p.append(Op.ARGUMENT_SEPARATOR);

        appendArgs(c, filter, p);

        p.append(Op.COMPOUND_TERM_CLOSER);

    }


    static void appendArgs(Subterms c, Appendable p) throws IOException {
        int nterms = c.subs();

        boolean bb = nterms > 1;
        for (int i = 0; i < nterms; i++) {
            if ((i != 0) || bb) {
                p.append(Op.ARGUMENT_SEPARATOR);
            }
            c.sub(i).appendTo(p);
        }
    }

    static void appendArgs(Subterms c, Function<Term, Term> filter, Appendable p) throws IOException {
        int nterms = c.subs();

        boolean bb = nterms > 1;
        for (int i = 0; i < nterms; i++) {
            if ((i != 0) || bb) {
                p.append(Op.ARGUMENT_SEPARATOR);
            }
            filter.apply(c.sub(i)).appendTo(p);
        }
    }

    public static void append(Compound c, Appendable p) throws IOException {
        final Op op = c.op();

        switch (op) {

            case SECTi:
            case SECTe:
                sectAppend(c, p);
                return;

            case SETi:
            case SETe:
                setAppend(c, p);
                return;
            case PROD:
                productAppend(c.subterms(), p);
                return;
            case NEG:
                negAppend(c, p);
                return;
        }

        if (op.statement || c.subs() == 2) {


            if (c.hasAll(Op.FuncBits)) {
                Term subj = c.sub(0);
                if (op == INH && subj.op() == Op.PROD) {
                    Term pred = c.sub(1);
                    Op pOp = pred.op();
                    if (pOp == ATOM) {
                        operationAppend((Compound) subj, (Atomic) pred, p);
                        return;
                    }
                }
            }

            statementAppend(c, p, op);

        } else {
            compoundAppend(c, p, op);
        }
    }

    static void sectAppend(Compound c, Appendable p) throws IOException {
        Op o = c.op();
        Subterms cs = c.subterms();
        if (cs.subs() == 2) {
            Term subracted = cs.sub(0), from;
            //negated subterm will be in the 0th position, if anywhere due to target sorting
            if (subracted.op() == NEG && (from=cs.sub(1)).op()!=NEG) {
                p.append('(');
                from.appendTo(p);
                p.append(o == SECTe ? DIFFi : DIFFe);
                subracted.unneg().appendTo(p);
                p.append(')');
                return;
            }

            statementAppend(c, p, o);
        } else {
            compoundAppend(c, p, o);
        }
    }

    static void negAppend(final Compound neg, Appendable p) throws IOException {
        /**
         * detect a negated conjunction of negated subterms:
         * (--, (&&, --A, --B, .., --Z) )
         */

        final Term sub = neg.unneg();

        if ((sub.op() == CONJ) && sub.hasAny(NEG.bit)) {
            int dt;
            if ((((dt = sub.dt()) == DTERNAL) || (dt == XTERNAL))) {
                Subterms cxx = sub.subterms();
                if (Terms.negatedNonConjCount(cxx) >= cxx.subs() / 2) {
                    String s;
                    switch (dt) {
                        case XTERNAL:
                            s = Op.DISJstr + "+- ";
                            break;
                        case DTERNAL:
                            s = Op.DISJstr;
                            break;
                        default:
                            throw new UnsupportedOperationException();
                    }

//                                    if (cxx.subs() == 2) {
//                                        statementAppend(cx, op, p); //infix
//                                    } else {
                    compoundAppend(s, cxx, Term::neg, p);
//                                    }
                    return;

                }
            }
        }

        p.append("(--,");
        sub.appendTo(p);
        p.append(')');
    }


    static void statementAppend(Term c, Appendable p, Op op  /*@NotNull*/) throws IOException {


        p.append(Op.COMPOUND_TERM_OPENER);

        int dt = c.dt();

        boolean reversedDT = dt != DTERNAL && /*dt != XTERNAL && */ dt < 0 && op.commutative;

        Subterms cs = c.subterms();
        cs.sub(reversedDT ? 1 : 0).appendTo(p);

        op.append(dt, p, reversedDT);

        cs.sub(reversedDT ? 0 : 1).appendTo(p);

        p.append(Op.COMPOUND_TERM_CLOSER);
    }


    static void productAppend(Subterms product, Appendable p) throws IOException {

        int s = product.subs();
        p.append(Op.COMPOUND_TERM_OPENER);
        for (int i = 0; i < s; i++) {
            product.sub(i).appendTo(p);
            if (i < s - 1) {
                p.append(',');
            }
        }
        p.append(Op.COMPOUND_TERM_CLOSER);
    }


    static void setAppend(Compound set, Appendable p) throws IOException {

        int len = set.subs();


        char opener, closer;
        if (set.op() == Op.SETe) {
            opener = Op.SETe.ch;
            closer = Op.SET_EXT_CLOSER;
        } else {
            opener = Op.SETi.ch;
            closer = Op.SET_INT_CLOSER;
        }

        p.append(opener);

        Subterms setsubs = set.subterms();
        for (int i = 0; i < len; i++) {
            if (i != 0) p.append(Op.ARGUMENT_SEPARATOR);
            setsubs.sub(i).appendTo(p);
        }
        p.append(closer);
    }

    static void operationAppend(Compound argsProduct, Atomic operator, Appendable w) throws IOException {

        operator.appendTo(w);

        w.append(Op.COMPOUND_TERM_OPENER);


        argsProduct.forEachWith((t, n) -> {
            try {
                if (n != 0)
                    w.append(Op.ARGUMENT_SEPARATOR);

                t.appendTo(w);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        w.append(Op.COMPOUND_TERM_CLOSER);

    }


}
