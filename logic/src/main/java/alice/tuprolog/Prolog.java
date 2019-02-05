/*
 * tuProlog - Copyright (C) 2001-2002  aliCE team at deis.unibo.it
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

import alice.tuprolog.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Predicate;


/**
 * The Prolog class represents a tuProlog engine.
 */
public class Prolog extends EngineManager {

    public static final ThreadLocal<EngineRunner> threads = new ThreadLocal<>();
    /*  manager of current theory */
    public final TheoryManager theories;
    /*  component managing primitive  */
    public final PrimitiveManager prims;
    /* component managing operators */
    public final OperatorManager ops;
    /* component managing flags */
    public final Flags flags;
    /* component managing libraries */
    public final LibraryManager libs;
    public final ConcurrentHashMap<String, TermQueue> queues = new ConcurrentHashMap<>();
    protected final EngineRunner root = new EngineRunner(0);
    protected final AtomicInteger id = new AtomicInteger();
    protected final ConcurrentHashMap<Integer, EngineRunner> runners = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    /*  spying activated ?  */
    private boolean spy;


    /* listeners registrated for virtual machine output events */
    /*Castagna 06/2011*/
    /* exception activated ? */
    private boolean exception;
    /**/
    final CopyOnWriteArrayList<OutputListener> outputListeners;
    /* listeners registrated for virtual machine internal events */
    private final CopyOnWriteArrayList<SpyListener> spyListeners;

    /* listeners registrated for virtual machine state change events */

    public final static Logger logger = LoggerFactory.getLogger(Prolog.class);

    /*Castagna 06/2011*/
    /* listeners registrated for virtual machine state exception events */
    private final List<ExceptionListener> exceptionListeners;
    /**/

    /* listeners to theory events */
    private final List<TheoryListener> theoryListeners;
    /* listeners to library events */
    private final List<LibraryListener> libraryListeners;
    /* listeners to query events */
    private final List<Consumer<QueryEvent>> queryListeners;

    /* path history for including documents */
    private List<String> absolutePathList;
    private boolean warning;


    public Prolog() {
        this(new MutableClauseIndex());
    }

    /**
     * Builds a prolog engine with default libraries loaded.
     * <p>
     * The default libraries are BasicLibrary, ISOLibrary,
     * IOLibrary, and  JavaLibrary
     */
    public Prolog(ClauseIndex dynamics) {
        this(false, dynamics);
        try {
            addLibrary("alice.tuprolog.lib.BasicLibrary");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        try {
            addLibrary("alice.tuprolog.lib.ISOLibrary");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        try {
            addLibrary("alice.tuprolog.lib.IOLibrary");
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }


    /**
     * Builds a tuProlog engine with loaded
     * the specified libraries
     *
     * @param libs the (class) name of the libraries to be loaded
     */
    public Prolog(String... libs) {
        this(false, new MutableClauseIndex());
        if (libs != null) {
            for (String lib : libs) {
                try {
                    addLibrary(lib);
                } catch (InvalidLibraryException e) {
                    logger.error("loading {}: {}", lib, e);
                }
            }
        }
    }


    /**
     * Initialize basic engine structures.
     *
     * @param spy spying activated
     */
    private Prolog(boolean spy, ClauseIndex dynamics) {
        super();

        outputListeners = new CopyOnWriteArrayList<>();
        spyListeners = new CopyOnWriteArrayList<>();
        /*Castagna 06/2011*/
        exceptionListeners = new CopyOnWriteArrayList<>();
        /**/
        this.spy = spy;

        /*Castagna 06/2011*/
        exception = true;
        /**/
        theoryListeners = new CopyOnWriteArrayList<>();
        queryListeners = new CopyOnWriteArrayList<>();
        libraryListeners = new CopyOnWriteArrayList<>();
        absolutePathList = new CopyOnWriteArrayList<>();
        flags = new Flags();
        libs = new LibraryManager();
        ops = new OperatorManager();
        prims = new PrimitiveManager();

        theories = new TheoryManager(this, dynamics);
        libs.start(this);
        prims.start(this);

        root.initialize(this);
        threads.set(root);

    }


    /**
     * Gets the last Element of the path list
     */
    public String getCurrentDirectory() {
        String directory = "";
        if (absolutePathList.isEmpty()) {
            directory = /*this.lastPath != null ? this.lastPath : */System.getProperty("user.dir");
        } else {
            directory = absolutePathList.get(absolutePathList.size() - 1);
        }

        return directory;
    }


    /**
     * Sets a new theory
     *
     * @param th is the new theory
     * @throws InvalidTheoryException if the new theory is not valid
     * @see Theory
     */
    public Prolog setTheory(Theory th) throws InvalidTheoryException {
        theories.clear();
        input(th);
        return this;
    }


    /**
     * Adds (appends) a theory
     *
     * @param th is the theory to be added
     * @throws InvalidTheoryException if the new theory is not valid
     * @see Theory
     */
    public Prolog input(Theory th) throws InvalidTheoryException {

        Consumer<Theory> ifSuccessful;
        if (!theoryListeners.isEmpty()) {
            Theory oldTh = getTheory();
            ifSuccessful = (newTheory) -> {
                for (TheoryListener tl : theoryListeners) {
                    tl.theoryChanged(new TheoryEvent(this, oldTh, newTheory));
                }
            };
        } else {
            ifSuccessful = null;
        }

        theories.consult(th, true, null);
        theories.solveTheoryGoal();
        Theory newTh = getTheory();

        if (ifSuccessful != null)
            ifSuccessful.accept(newTh);

        return this;
    }

    /**
     * Gets current theory
     *
     * @return current(dynamic) theory
     */
    public Theory getTheory() {
        try {
            return new Theory(theories.getTheory(true));
        } catch (Exception ex) {
            return null;
        }
    }


    /**
     * Clears current theory
     */
    public void clearTheory() {
        try {
            setTheory(Theory.Null);
        } catch (InvalidTheoryException e) {

        }
    }


    /**
     * Loads a library.
     * <p>
     * If a library with the same name is already present,
     * a warning event is notified and the request is ignored.
     *
     * @param className name of the Java class containing the library to be loaded
     * @return the reference to the Library just loaded
     * @throws InvalidLibraryException if name is not a valid library
     */
    public Library addLibrary(String className) throws InvalidLibraryException {
        return libs.loadClass(className);
    }

    /**
     * Loads a library.
     * <p>
     * If a library with the same name is already present,
     * a warning event is notified and the request is ignored.
     *
     * @param className name of the Java class containing the library to be loaded
     * @param paths     The path where is contained the library.
     * @return the reference to the Library just loaded
     * @throws InvalidLibraryException if name is not a valid library
     */
    public Library addLibrary(String className, String... paths) throws InvalidLibraryException {
        return libs.loadClass(className, paths);
    }


    /**
     * Loads a specific instance of a library
     * <p>
     * If a library with the same name is already present,
     * a warning event is notified
     *
     * @param lib the (Java class) name of the library to be loaded
     * @throws InvalidLibraryException if name is not a valid library
     */
    public void addLibrary(Library lib) throws InvalidLibraryException {
        libs.load(lib);
    }

    public void addLibrary(Class<? extends Library> lib) throws InvalidLibraryException {
        try {
            addLibrary(lib.getConstructor().newInstance());
        } catch (InvocationTargetException | NoSuchMethodException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Unloads a previously loaded library
     *
     * @param name of the library to be unloaded
     * @throws InvalidLibraryException if name is not a valid loaded library
     */
    public void removeLibrary(String name) throws InvalidLibraryException {
        libs.unload(name);
    }


    /**
     * Gets the reference to a loaded library
     *
     * @param name the name of the library already loaded
     * @return the reference to the library loaded, null if the library is
     * not found
     */
    public Library library(String name) {
        return libs.getLibrary(name);
    }


    /**
     * Gets the list of the operators currently defined
     *
     * @return the list of the operators
     */
    public Iterable<Operator> operators() {
        return ops.operators();
    }


    /**
     * Solves a query
     *
     * @param g the term representing the goal to be demonstrated
     * @return the result of the demonstration
     * @see Solution
     **/
    public Solution solve(Term g) {

        this.clearSinfoSetOf();

        Solution sinfo = root.solve(g);

        solution(this, sinfo);

        return sinfo;
    }

    public final Prolog solve(String g, Consumer<Solution> eachSolution) {
        return solve(term(g), eachSolution);
    }
    public final Prolog solveWhile(String g, Predicate<Solution> eachSolution) {
        return solveWhile(term(g), eachSolution);
    }

    public Prolog solve(Term g, Consumer<Solution> eachSolution) {
        return solveWhile(g, (x) -> {
            eachSolution.accept(x);
            return true;
        });
    }

    public Prolog solveWhile(Term g, Predicate<Solution> eachSolution) {
        return solveWhile(g, eachSolution, -1);
    }

    public Prolog solveWhile(Term g, Predicate<Solution> eachSolution, long timeoutMS) {

        Solution next = null;
        do {
            if (next == null) {
                next = solve(g);
                if (next == null)
                    break;
            } else {
                try {
                    next = solveNext( /* TODO subdivide input time */);
                } catch (NoMoreSolutionException e) {
                    e.printStackTrace();
                }
            }

            solution(this, next);

        } while (eachSolution.test(next) && hasOpenAlternatives());


        return this;
    }

    /**
     * Solves a query
     *
     * @param st the string representing the goal to be demonstrated
     * @return the result of the demonstration
     * @see Solution
     **/
    public Solution solve(String st) throws MalformedGoalException {
        try {
            return solve(term(st));
        } catch (InvalidTermException ex) {
            throw new MalformedGoalException();
        }
    }

    public Term term(String toParse) throws InvalidTermException {
        return new Parser(toParse, ops).nextTerm(true);
    }

    /**
     * Gets next solution
     *
     * @return the result of the demonstration
     * @throws NoMoreSolutionException if no more solutions are present
     * @see Solution
     **/
    public Solution solveNext() throws NoMoreSolutionException {
        if (hasOpenAlternatives()) {
            Solution result;
            synchronized (root) {
                result = root.solveNext();
            }
            Solution sinfo = result;
            solution(this, sinfo);
            return sinfo;
        } else
            throw new NoMoreSolutionException();
    }

    /**
     * Halts current solve computation
     */
    public void solveHalt() {
        solveHalt();
    }

    /**
     * Accepts current solution
     */
    public void solveEnd() {
        solveEnd();
    }




    /**
     * Checks if the demonstration process was stopped by an halt command.
     *
     * @return true if the demonstration was stopped
     */
    public boolean isHalted() {
        return runner().isHalted();
    }


    /**
     * Gets a term from a string, using the operators currently
     * defined by the engine
     *
     * @param st the string representing a term
     * @return the term parsed from the string
     * @throws InvalidTermException if the string does not represent a valid term
     */
    public Term toTerm(String st) throws InvalidTermException {
        return Parser.parseSingleTerm(st, ops);
    }

    /**
     * Gets the string representation of a term, using operators
     * currently defined by engine
     *
     * @param term the term to be represented as a string
     * @return the string representing the term
     */
    public String toString(Term term) {
        return (term.toStringAsArgY(ops, OperatorManager.OP_HIGH));
    }


    /**
     * Switches on/off the notification of spy information events
     *
     * @param state - true for enabling the notification of spy event
     */
    public void setSpy(boolean state) {
        spy = state;
    }

    /**
     * Checks the spy state of the engine
     *
     * @return true if the engine emits spy information
     */
    public boolean isSpy() {
        return spy;
    }


    /**
     * Notifies a spy information event
     */
    protected void spy(String s) {
        if (spy) {
            notifySpy(new SpyEvent(this, s));
        }
    }

    /**
     * Notifies a spy information event
     *
     * @param s TODO
     */
    protected void spy(State s, Engine e) {

        if (spy) {
            ExecutionContext ctx = e.currentContext;
            if (ctx != null) {
                int i = 0;
                String g = "-";
                if (ctx.fatherCtx != null) {
                    i = ctx.depth - 1;
                    g = ctx.fatherCtx.currentGoal.toString();
                }
                notifySpy(new SpyEvent(this, e, "spy: " + i + "  " + s + "  " + g));
            }
        }
    }
















    /*Castagna 06/2011*/

    /**
     * Notifies a exception information event
     *
     * @param m the exception message
     */
    public void exception(Throwable e) {
        if (exception && !exceptionListeners.isEmpty()) {
            ExceptionEvent e1 = new ExceptionEvent(this, e);

            for (ExceptionListener exceptionListener : exceptionListeners)
                exceptionListener.onException(e1);

            logger.error("{} {}", e1.getSource(), e1.getException());
        }
    }


    /**
     * Produces an output information event
     *
     * @param m the output string
     */
    public void output(String m) {

        int outputListenersSize = outputListeners.size();
        if (outputListenersSize > 0) {
            OutputEvent e = new OutputEvent(this, m);
            for (OutputListener outputListener : outputListeners) {
                outputListener.onOutput(e);
            }
        }

    }


    /**
     * Adds a listener to ouput events
     *
     * @param l the listener
     */
    public void addOutputListener(OutputListener l) {
        outputListeners.add(l);
    }


    /**
     * Adds a listener to theory events
     *
     * @param l the listener
     */
    public void addTheoryListener(TheoryListener l) {
        theoryListeners.add(l);
    }

    /**
     * Adds a listener to library events
     *
     * @param l the listener
     */
    public void addLibraryListener(LibraryListener l) {
        libraryListeners.add(l);
    }

    /**
     * Adds a listener to theory events
     *
     * @param l the listener
     */
    public void addQueryListener(Consumer<QueryEvent> l) {
        queryListeners.add(l);
    }

    /**
     * Adds a listener to spy events
     *
     * @param l the listener
     */
    public void addSpyListener(SpyListener l) {
        spy = true;
        spyListeners.add(l);
    }










    /*Castagna 06/2011*/

    /**
     * Adds a listener to exception events
     *
     * @param l the listener
     */
    public void addExceptionListener(ExceptionListener l) {
        exceptionListeners.add(l);
    }
    /**/

    /**
     * Removes a listener to ouput events
     *
     * @param l the listener
     */
    public void removeOutputListener(OutputListener l) {
        outputListeners.remove(l);
    }

    /**
     * Removes all output event listeners
     */
    public void removeAllOutputListeners() {
        outputListeners.clear();
    }

    /**
     * Removes a listener to theory events
     *
     * @param l the listener
     */
    public void removeTheoryListener(TheoryListener l) {
        theoryListeners.remove(l);
    }

    /**
     * Removes a listener to library events
     *
     * @param l the listener
     */
    public void removeLibraryListener(LibraryListener l) {
        libraryListeners.remove(l);
    }

    /**
     * Removes a listener to query events
     *
     * @param l the listener
     */
    public void removeQueryListener(Consumer<QueryEvent> l) {
        queryListeners.remove(l);

    }


    /**
     * Removes a listener to spy events
     *
     * @param l the listener
     */
    public synchronized void removeSpyListener(SpyListener l) {
        spyListeners.remove(l);
        spy = !(spyListeners.isEmpty());
    }

    /**
     * Removes all spy event listeners
     */
    public synchronized void removeAllSpyListeners() {
        spy = false;
        spyListeners.clear();
    }

















    /* Castagna 06/2011*/

    /**
     * Removes a listener to exception events
     *
     * @param l the listener
     */
    public void removeExceptionListener(ExceptionListener l) {
        exceptionListeners.remove(l);
    }
    /**/

    /*Castagna 06/2011*/

    /**
     * Removes all exception event listeners
     */
    public void removeAllExceptionListeners() {
        exceptionListeners.clear();
    }
    /**/


    /**
     * Notifies a spy information event
     *
     * @param e the event
     */
    private void notifySpy(SpyEvent e) {
        for (SpyListener spyListener : spyListeners) {
            spyListener.onSpy(e);
        }
    }

    /*Castagna 06/2011*/

    /**/


    /**
     * Notifies a library loaded event
     *
     * @param e the event
     */
    protected void notifyLoadedLibrary(/* TODO Supplier< */ LibraryEvent e) {
        for (LibraryListener ll : libraryListeners) {
            ll.libraryLoaded(e);
        }
    }

    /**
     * Notifies a library unloaded event
     *
     * @param e the event
     */
    protected void notifyUnloadedLibrary(LibraryEvent e) {
        for (LibraryListener ll : libraryListeners) {
            ll.libraryUnloaded(e);
        }
    }

    /**
     * Notifies a library loaded event
     *
     * @param e the event
     */
    protected void solution(Prolog source, Solution info) {

        int qls = queryListeners.size();
        if (qls > 0) {
            QueryEvent e = new QueryEvent(source, info);
            for (int i = 0, queryListenersSize = qls; i < queryListenersSize; i++) {
                queryListeners.get(i).accept(e);
            }
        }


    }


    /**
     * Append a new path to directory list
     */
    public void pushDirectoryToList(String path) {
        absolutePathList.add(path);
    }

    /**
     * Retract an element from directory list
     */
    public void popDirectoryFromList() {
        if (!absolutePathList.isEmpty()) {
            absolutePathList.remove(absolutePathList.size() - 1);
        }
    }

    /**
     * Reset directory list
     */
    public void resetDirectoryList(String path) {
        absolutePathList = new ArrayList<>();
        absolutePathList.add(path);
    }

//    public Term termSolve(String st) {
//        try {
//            Parser p = new Parser(st, ops);
//            return p.nextTerm(true);
//        } catch (InvalidTermException e) {
//
//            return Term.term("null");
//        }
//    }

    public boolean isTrue(String s) {
        return isTrue(term(s));
    }

    public boolean isTrue(Term s) {
        Solution r = solve(s);
        return r.isSuccess();
    }

    public static void warn(String s) {
        logger.warn(s);
    }

    public boolean isWarning() {
        return warning;
    }

    public void notifyWarning(WarningEvent warningEvent) {
        if (warning)
            logger.warn("warning {}", warningEvent);
    }

    public void setWarning(boolean b) {
        this.warning = b;
    }

    public boolean threadCreate(Term threadID, Term goal) {

        if (goal == null)
            return false;

        int id = this.id.incrementAndGet();

        if (goal instanceof Var)
            goal = goal.term();

        Prolog vm = this;

        EngineRunner er = new EngineRunner(id);
        er.initialize(vm);

        if (!threadID.unify(vm, new NumberTerm.Int(id)))
            return false;

        er.setGoal(goal);


        runners.put(id, er);


        Thread t = new Thread(er);


        t.start();
        return true;
    }


    public Solution join(int id) {
        EngineRunner er = runner(id);
        if (er == null || er.isDetached()) return null;
        /*toSPY
         * System.out.println("Thread id "+runnerId()+" - prelevo la soluzione (join)");*/
        Solution solution = er.read();
        /*toSPY
         * System.out.println("Soluzione: "+solution);*/
        removeRunner(id);
        return solution;
    }

    public Solution read(int id) {
        EngineRunner er = runner(id);
        if (er == null || er.isDetached()) return null;
        /*toSPY
         * System.out.println("Thread id "+runnerId()+" - prelevo la soluzione (read) del thread di id: "+er.getId());
         */
        /*toSPY
         * System.out.println("Soluzione: "+solution);
         */
        return er.read();
    }

    public boolean hasNext(int id) {
        EngineRunner er = runner(id);
        return !(er == null || er.isDetached()) && er.hasOpenAlternatives();
    }

    public boolean nextSolution(int id) {
        EngineRunner er = runner(id);
        /*toSPY
         * System.out.println("Thread id "+runnerId()+" - next_solution: risveglio il thread di id: "+er.getId());
         */
        return !(er == null || er.isDetached()) && er.nextSolution();
    }

    public void detach(int id) {
        EngineRunner er = runner(id);
        if (er != null)
            er.detach();
    }

    private void removeRunner(int id) {
        runners.remove(id);
    }

    void cut() {
        runner().cut();
    }

    ExecutionContext getCurrentContext() {
        return runner().getCurrentContext();
    }

    public boolean hasOpenAlternatives() {
        return runner().hasOpenAlternatives();
    }

    void pushSubGoal(SubGoalTree goals) {
        runner().pushSubGoal(goals);
    }

    /**
     * @return L'EngineRunner associato al thread di id specificato.
     */

    public EngineRunner runner(int id) {


        return runners.get(id);


    }

    public final EngineRunner runner() {

        return threads.get();

//        Integer id = threads.get();
//
//        return id != null ? runner(id) : root;

    }

    public boolean createQueue(String name) {

        queues.computeIfAbsent(name, (n) -> new TermQueue());

        return true;
    }

    public void destroyQueue(String name) {

        queues.remove(name);

    }

    public int queueSize(int id) {
        return runner(id).msgQSize();
    }

    public int queueSize(String name) {
        TermQueue q = queues.get(name);
        return q == null ? -1 : q.size();
    }

    public ReentrantLock createLock(String name) {
        return locks.computeIfAbsent(name, (n) -> new ReentrantLock());
    }

    public boolean destroyLock(String name) {
        return locks.remove(name)!=null;
    }

    public void mutexLock(String name) {
        //while (true) {
        ReentrantLock mutex = createLock(name);

        mutex.lock();
        /*toSPY
         * System.out.println("Thread id "+runnerId()+ " - mi sono impossessato del lock");
         */
    }

    public boolean mutexTryLock(String name) {
        ReentrantLock mutex = locks.get(name);
        return mutex != null && mutex.tryLock();
        /*toSPY
         * System.out.println("Thread id "+runnerId()+ " - provo ad impossessarmi del lock");
         */
    }

    public boolean mutexUnlock(String name) {
        ReentrantLock mutex = locks.get(name);
        if (mutex == null) return false;
        try {
            mutex.unlock();
            /*toSPY
             * System.out.println("Thread id "+runnerId()+ " - Ho liberato il lock");
             */
            return true;
        } catch (IllegalMonitorStateException e) {
            return false;
        }
    }

    public boolean isLocked(String name) {
        ReentrantLock mutex = locks.get(name);
        return mutex != null && mutex.isLocked();
    }

    public void unlockAll() {

        locks.forEach((k, mutex) -> {
            boolean unlocked = false;
            while (!unlocked) {
                try {
                    mutex.unlock();
                } catch (IllegalMonitorStateException e) {
                    unlocked = true;
                }
            }
        });
    }

    Engine getEnv() {
        return runner().env;
    }

    public void identify(Term t) {
        runner().identify(t);
    }

    public boolean relinkVar() {
        return this.runner().getRelinkVar();
    }

    public void relinkVar(boolean b) {
        this.runner().setRelinkVar(b);
    }

    public List<Term> getBagOFres() {
        return this.runner().getBagOFres();
    }

    public void setBagOFres(List<Term> l) {
        this.runner().setBagOFres(l);
    }

    public List<String> getBagOFresString() {
        return this.runner().getBagOFresString();
    }

    public void setBagOFresString(List<String> l) {
        this.runner().setBagOFresString(l);
    }

    public Term getBagOFvarSet() {
        return this.runner().getBagOFvarSet();
    }

    public void setBagOFvarSet(Term l) {
        this.runner().setBagOFvarSet(l);
    }

    public Term getBagOFgoal() {
        return this.runner().getBagOFgoal();
    }

    public void setBagOFgoal(Term l) {
        this.runner().setBagOFgoal(l);
    }

    public Term getBagOFbag() {
        return this.runner().getBagOFBag();
    }

    public void setBagOFbag(Term l) {
        this.runner().setBagOFBag(l);
    }

    public void setSetOfSolution(String s) {
        this.runner().setSetOfSolution(s);
    }

    public void clearSinfoSetOf() {
        this.runner().clearSinfoSetOf();
    }

    public void endFalse(String s) {
        setSetOfSolution(s);
        relinkVar(false);
        setBagOFres(null);
        setBagOFgoal(null);
        setBagOFvarSet(null);
        setBagOFbag(null);
    }
}