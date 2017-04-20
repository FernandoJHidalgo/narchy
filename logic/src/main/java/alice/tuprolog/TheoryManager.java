/*
 * tuProlog - Copyright (C) 2001-2007  aliCE team at deis.unibo.it
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package alice.tuprolog;

import alice.util.Tools;
import org.jetbrains.annotations.NotNull;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.function.Predicate;

/**
 * This class defines the Theory Manager who manages the clauses/theory often referred to as the Prolog database.
 * The theory (as a set of clauses) are stored in the ClauseDatabase which in essence is a HashMap grouped by functor/arity.
 * <p/>
 * The TheoryManager functions logically, as prescribed by ISO Standard 7.5.4
 * section. The effects of assertions and retractions shall not be undone if the
 * program subsequently backtracks over the assert or retract call, as prescribed
 * by ISO Standard 7.7.9 section.
 * <p/>
 * To use the TheoryManager one should primarily use the methods assertA, assertZ, consult, retract, abolish and find.
 * <p/>
 * <p>
 * rewritten by:
 *
 * @author ivar.orstavik@hist.no
 * @see Theory
 */
public class TheoryManager {

    public static final Struct TRUE = new Struct("true");
    private final ClauseIndex dynamicDBase;
    private final MutableClauseIndex staticDBase;

    private final Prolog engine;
    private final PrimitiveManager primitiveManager;
    private final Deque<Term> startGoalStack;
    Theory lastConsultedTheory;

//	public TheoryManager(Prolog vm) {
//		this(vm, new MutableClauseIndex());
//	}

    public TheoryManager(Prolog vm, ClauseIndex dynamics) {
        engine = vm;
        dynamicDBase = dynamics;
        staticDBase = new MutableClauseIndex();

        lastConsultedTheory = new Theory();
        primitiveManager = engine.getPrimitiveManager();
        startGoalStack = new ArrayDeque<>(32);
    }

    /**
     * inserting of a clause at the head of the dbase
     */
    public /*synchronized*/ void assertA(Struct clause, boolean dyn, String libName, boolean backtrackable) {
        ClauseInfo d = new ClauseInfo(toClause(clause), libName);
        String key = d.getHead().getPredicateIndicator();
        if (dyn) {
            dynamicDBase.add(key, d, true);
            if (staticDBase.containsKey(key)) {
                Prolog.warn("A static predicate with signature " + key + " has been overriden.");
            }
        } else
            staticDBase.add(key, d, true);
        if (engine.isSpy())
            engine.spy("INSERTA: " + d.getClause() + '\n');
    }

    /**
     * inserting of a clause at the end of the dbase
     */
    public /*synchronized*/ void assertZ(Struct clause, boolean dyn, String libName, boolean backtrackable) {
        ClauseInfo d = new ClauseInfo(toClause(clause), libName);
        String key = d.getHead().getPredicateIndicator();
        if (dyn) {
            dynamicDBase.add(key, d, false);
            if (engine.isSpy() && staticDBase.containsKey(key)) {
                Prolog.warn("A static predicate with signature " + key + " has been overriden.");
            }
        } else
            staticDBase.add(key, d, false);
        if (engine.isSpy()) engine.spy("INSERTZ: " + d.getClause() + '\n');
    }

    /**
     * removing from dbase the first clause with head unifying with clause
     */
    public synchronized int retract(final Struct cl, Predicate<ClauseInfo> each) {
        Struct clause = toClause(cl);
        Struct struct = ((Struct) clause.term(0));
        List<ClauseInfo> family = dynamicDBase.get(struct.getPredicateIndicator());
        //final ExecutionContext ctx = engine.getEngineManager().getCurrentContext();

		/*creo un nuovo clause database x memorizzare la teoria all'atto della retract 
		 * questo lo faccio solo al primo giro della stessa retract 
		 * (e riconosco questo in base all'id del contesto)
		 * sara' la retract da questo db a restituire il risultato
		 */
//		FamilyClausesList familyQuery = retractDBase.computeIfAbsent("ctxId "+ctx.getId(), (c) -> {
//			return new FamilyClausesList(family);
//		});

        if (family == null)
            return 0;

        final int[] removals = {0};
        family.removeIf(ci -> {
            if (clause.match(ci.getClause())) {
                if (each.test(ci)) {
                    removals[0]++;
                    return true;
                }
            }
            return false;
        });


//		//fa la retract dalla teoria base
//		if (family != null){
//			for (Iterator<ClauseInfo> it = family.iterator(); it.hasNext();) {
//				ClauseInfo d = it.next();
//				if (clause.match(d.getClause())) {
//					it.remove();
//					break;
//				}
//			}
//		}
//		//fa la retract dal retract db
//		for (Iterator<ClauseInfo> i = familyQuery.iterator(); i.hasNext();) {
//			ClauseInfo d = i.next();
//			if (clause.match(d.getClause())) {
//				i.remove();
//				if (engine.isSpy())
//					engine.spy("DELETE: " + d.getClause() + '\n');
//				return new ClauseInfo(d.getClause(), null);
//			}
//		}
        return removals[0];
    }

    /**
     * removing from dbase all the clauses corresponding to the
     * predicate indicator passed as a parameter
     */
    public /*synchronized*/ boolean abolish(Struct pi) {
        if (!(pi instanceof Struct) || !pi.isGround() || !(pi.getArity() == 2))
            throw new IllegalArgumentException(pi + " is not a valid Struct");
        if (!pi.name().equals("/"))
            throw new IllegalArgumentException(pi + " has not the valid predicate name. Espected '/' but was " + pi.name());

        String arg0 = Tools.removeApices(pi.term(0).toString());
        String arg1 = Tools.removeApices(pi.term(1).toString());
        String key = arg0 + '/' + arg1;
        List<ClauseInfo> abolished = dynamicDBase.remove(key); /* Reviewed by Paolo Contessi: LinkedList -> List */
        if (abolished != null)
            if (engine.isSpy())
                engine.spy("ABOLISHED: " + key + " number of clauses=" + abolished.size() + '\n');
        return true;
    }

    /**
     * Returns a family of clauses with functor and arity equals
     * to the functor and arity of the term passed as a parameter
     * <p>
     * Reviewed by Paolo Contessi: modified according to new ClauseDatabase
     * implementation
     */
    @NotNull
    public /*synchronized*/ List<ClauseInfo> find(Term headt) {

        if (headt instanceof Struct) {
            //String key = ((Struct) headt).getPredicateIndicator();
            Struct s = (Struct) headt;
            List<ClauseInfo> list = dynamicDBase.getPredicates(s);
            if (list == null) {
                list = staticDBase.getPredicates(s);
                if (list != null)
                    return list;
            } else {
                return list;
            }
        }

        if (headt instanceof Var) {
            //            List l = new LinkedList();
            //            for (Iterator iterator = clauseDBase.iterator(); iterator.hasNext();) {
            //                ClauseInfo ci =  (ClauseInfo) iterator.next();
            //                if(ci.dynamic)
            //                    l.add(ci);
            //            }
            //            return l;
            throw new RuntimeException();
        }
        return Collections.emptyList(); //new LinkedList<>();
    }

    /**
     * Consults a theory.
     *
     * @param theory        theory to add
     * @param dynamicTheory if it is true, then the clauses are marked as dynamic
     * @param libName       if it not null, then the clauses are marked to belong to the specified library
     */
    public /*synchronized*/ void consult(Theory theory, boolean dynamicTheory, String libName) throws InvalidTheoryException {
        startGoalStack.clear();

		/*Castagna 06/2011*/
        int clause = 1;
		/**/
        // iterate all clauses in theory and assert them
        try {
            for (Iterator<? extends Term> it = theory.iterator(engine); it.hasNext(); ) {
				/*Castagna 06/2011*/
                clause++;
				/**/
                Struct d = (Struct) it.next();
                if (!runDirective(d))
                    assertZ(d, dynamicTheory, libName, true);
            }
        } catch (InvalidTermException e) {
			/*Castagna 06/2011*/
            //throw new InvalidTheoryException(e.getMessage());
            throw new InvalidTheoryException(e.getMessage(), clause, e.line, e.pos);
			/**/
        }

        if (libName == null)
            //lastConsultedTheory.append(theory);
            lastConsultedTheory = theory;
    }

    /**
     * Binds clauses in the database with the corresponding
     * primitive predicate, if any
     */
    public void rebindPrimitives() {
        for (ClauseInfo d : dynamicDBase) {
            for (AbstractSubGoalTree sge : d.getBody()) {
                Term t = (Term) sge;
                primitiveManager.identifyPredicate(t);
            }
        }
    }

    /**
     * Clears the clause dbase.
     */
    public /*synchronized*/ void clear() {
        dynamicDBase.clear();
    }

    /**
     * remove all the clauses of lib theory
     */
    public synchronized void removeLibraryTheory(String libName) {
        for (Iterator<ClauseInfo> allClauses = staticDBase.iterator(); allClauses.hasNext(); ) {
            ClauseInfo d = allClauses.next();
            if (d.libName != null && libName.equals(d.libName)) {
                try {
                    // Rimuovendolo da allClauses si elimina solo il valore e non la chiave
                    allClauses.remove();
                } catch (Exception e) {
                }
            }
        }
    }


    private boolean runDirective(Struct c) {
        if ("':-'".equals(c.name()) ||
                ((c.getArity() == 1) && ":-".equals(c.name()) && (c.getTerm(0) instanceof Struct))) {
            Struct dir = (Struct) c.getTerm(0);
            try {
                if (!primitiveManager.evalAsDirective(dir))
                    Prolog.warn("The directive " + dir.getPredicateIndicator() + " is unknown.");
            } catch (Throwable t) {
                Prolog.warn("An exception has been thrown during the execution of the " +
                        dir.getPredicateIndicator() + " directive.\n" + t.getMessage());
            }
            return true;
        }
        return false;
    }

    /**
     * Gets a clause from a generic Term
     */
    private Struct toClause(Struct t) {        //PRIMITIVE
        // TODO bad, slow way of cloning. requires approx twice the time necessary
        t = (Struct) Term.createTerm(t.toString(), this.engine.getOperatorManager());
        if (!t.isClause())
            t = new Struct(":-", t, TRUE);
        primitiveManager.identifyPredicate(t);
        return t;
    }

    public /*synchronized*/ void solveTheoryGoal() {
        Struct s = null;
        Deque<Term> goals = this.startGoalStack;

        while (!goals.isEmpty()) {
            Term g = goals.pop();
            s = (s == null) ?
                    (Struct) g :
                    new Struct(",", g, s);
        }
        if (s != null) {
            try {
                engine.solve(s);
            } catch (Exception ex) {
                Prolog.logger.error("solveTheoryGoal {}", ex);
                //ex.printStackTrace();
            }
        }
    }

    /**
     * add a goal eventually defined by last parsed theory.
     */
    public /*synchronized*/ void addStartGoal(Struct g) {
        startGoalStack.push(g);
    }

    /**
     * saves the dbase on a output stream.
     */
    synchronized boolean save(OutputStream os, boolean onlyDynamic) {
        try {
            new DataOutputStream(os).writeBytes(getTheory(onlyDynamic));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Gets current theory
     *
     * @param onlyDynamic if true, fetches only dynamic clauses
     */
    @Deprecated
    public /*synchronized*/ String getTheory(boolean onlyDynamic) {
        StringBuilder buffer = new StringBuilder();
        for (Iterator<ClauseInfo> dynamicClauses = dynamicDBase.iterator(); dynamicClauses.hasNext(); ) {
            ClauseInfo d = dynamicClauses.next();
            buffer.append(d.toString(engine.getOperatorManager())).append('\n');
        }
        if (!onlyDynamic)
            for (Iterator<ClauseInfo> staticClauses = staticDBase.iterator(); staticClauses.hasNext(); ) {
                ClauseInfo d = staticClauses.next();
                buffer.append(d.toString(engine.getOperatorManager())).append('\n');
            }
        return buffer.toString();
    }

    /**
     * Gets last consulted theory
     *
     * @return last theory
     */
    public /*synchronized*/ Theory getLastConsultedTheory() {
        return lastConsultedTheory;
    }


}