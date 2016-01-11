/*
 * Parameters.java
 *
 * Copyright (C) 2008  Pei Wang
 *
 * This file is part of Open-NARS.
 *
 * Open-NARS is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Open-NARS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Open-NARS.  If not, see <http://www.gnu.org/licenses/>.
 */
package nars;


import com.gs.collections.impl.set.mutable.UnifiedSet;
import nars.term.atom.Atom;
import nars.util.data.list.FasterList;
import nars.util.data.map.UnifriedMap;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Global NAR operating parameters (static scope)
 * Contains many static values which will eventually be migrated to
 * dynamic parameters specific to a component (allowing independent control).
 * (They began here for developmentconvenience)
 *
 */
public enum Global {
    ;



    public static final float TESTS_TRUTH_ERROR_TOLERANCE = 0.01f;

    //TODO use 'I' for SELf, it is 3 characters shorter
    public static final Atom DEFAULT_SELF = Atom.the("I");

    public static float EXECUTION_SATISFACTION_TRESHOLD = 0.0f; //decision threshold is enough for now


//    public static int UNIFICATION_POWER = 400;
//    public static int UNIFICATION_POWERmin = UNIFICATION_POWER;

    public static int DEFAULT_NAL_LEVEL = 8;


    public static boolean EXIT_ON_EXCEPTION = true;


    /** use this for advanced error checking, at the expense of lower performance.
        it is enabled for unit tests automatically regardless of the value here.    */
    public static boolean DEBUG = false;
    //public static final boolean DEBUG_BAG_MASS = false;
    //public static boolean DEBUG_TRACE_EVENTS = false; //shows all emitted events
    public static boolean DEBUG_DERIVATION_STACKTRACES = false; //includes stack trace in task's derivation rule string
    //public static boolean DEBUG_INVALID_SENTENCES = true;
    //public static boolean DEBUG_NONETERNAL_QUESTIONS = false;
    public static boolean DEBUG_TASK_LOG = true; //false disables task history completely
    //public static boolean PRINT_DUPLICATE_DERIVATIONS = false;
    //public static final boolean DEBUG_DERIVATION_GRAPH = false;
    public static final boolean DEBUG_REMOVED_CYCLIC_DERIVATIONS = false;
    public static final boolean DEBUG_REMOVED_INSUFFICIENT_BUDGET_DERIVATIONS = false;
    public static boolean DEBUG_LOG_DERIVING_RULE = true;
    public static boolean DEBUG_DETECT_DUPLICATE_DERIVATIONS = false;
    public static boolean DEBUG_DETECT_DUPLICATE_RULES = false;

    public static final float EXECUTION_DESIRE_EXPECTATION_THRESHOLD = 0.6f;


    //FIELDS BELOW ARE BEING CONVERTED TO DYNAMIC, NO MORE STATIC: ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //
    //Pei comments: parameters will be separated into a dynamic group and a static group
    //              and the latter contains "personality parameters" that cannot be changed
    //              in the lifetime of the system, though different systems may take different
    //              values. For example, to change HORIZON dynamically will cause inconsistency 
    //              in evidence evaluation.



//    public static final int METRICS_HISTORY_LENGTH = 256;

    /* ---------- logical parameters ---------- */
    /** Evidential Horizon, the amount of future evidence to be considered (during revision).
     * Must be >=1.0, usually 1 .. 2
     */
    public static float HORIZON = 1;



    /** minimum durability and quality necessary for a derivation to form */
    public static float BUDGET_DERIVATION_DURABILITY_THRESHOLD = 0.01f;

    /** minimum difference necessary to indicate a significant modification in budget float number components */
    public static final float BUDGET_PROPAGATION_EPSILON = 0.0001f;


//    /* ---------- default input values ---------- */
//    /** Default expectation for confirmation. */
//    public static final float DEFAULT_CONFIRMATION_EXPECTATION = 0.8f;



    
    
    /* ---------- avoiding repeated reasoning ---------- */
        /** Maximum length of the evidental base of the Stamp, a power of 2 */
    public static final int MAXIMUM_EVIDENTAL_BASE_LENGTH = 10;


    /**
     * The rate of confidence decrease in mental operations Doubt and Hesitate
     * set to zero to disable this feature.
     */
    public static float DISCOUNT_RATE = 0.5f;




    @Deprecated public static final int THREADS = 1; //temporary parameter for setting #threads to use, globally




//    /** how many maximum cycles difference in ocurrence time
//     * are two non-eternal sentences considered equal, if all
//     * other features (term, punctuation, truth, ..) are equal.
//     * this is similar to Duration parameter
//     */
//    public static final int SentenceOcurrenceTimeCyclesEqualityThreshold = 1;




    /** hard upper-bound limit on Compound term complexity;
     * if this is exceeded it may indicate a recursively
     * malformed term due to a serious inference bug */
    public static final int COMPOUND_VOLUME_MAX = 384;

    /** extra debugging checks */
    public static boolean DEBUG_PARANOID = false;

    public static float MAX_TERMUTATIONS_PER_MATCH = 8;
    public static float MIN_TERMUTATIONS_PER_MATCH = 1;



//    public static float TEMPORAL_INDUCTION_CHAIN_SAMPLE_DEPTH(float taskPriority) {
//        return 0.02f + taskPriority * 0.02f; //search up to 4% of concepts
//    }
//
//    public static int TEMPORAL_INDUCTION_CHAIN_SAMPLES = 1; //normal inference rule , this should be 10 to restore 1.6.1 behavior
//


    public static <K,V> Map<K, V> newHashMap() {
        return newHashMap(0);
    }

    public static <K, V> Map<K,V> newHashMap(int capacity) {
        //return new UnifiedMap(capacity);
        return new UnifriedMap(capacity);

        //return new FasterHashMap(capacity);
        //return new FastMap<>(); //javolution http://javolution.org/apidocs/javolution/util/FastMap.html
        //return new HashMap<>(capacity);
        //return new LinkedHashMap(capacity);
    }

    /** copy */
    public static <X,Y> Map<X, Y> newHashMap(Map<X, Y> xy) {
        return new UnifriedMap(xy);
    }

    public static <X> List<X> newArrayList() {
        return new FasterList<>(); //GS
        //return new ArrayList();
    }

    public static <X> List<X> newArrayList(int capacity) {

        return new FasterList(capacity);
        //return new ArrayList(capacity);
    }

    public static <X> Set<X> newHashSet(int capacity) {
        return new UnifiedSet(capacity);
        //return new SimpleHashSet(capacity);
        //return new HashSet(capacity);
        //return new LinkedHashSet(capacity);
    }

    public static <X> Set<X> newHashSet(Collection<X> values) {
        Set<X> s = newHashSet(values.size());
        s.addAll(values);
        return s;
    }


    public static <C> Reference<C> reference(C s) {
        if (s == null) return null;
        return new SoftReference(s);
    }
    public static <C> C dereference(Reference<C> s) {
        if (s == null) return null;
        return s.get();
    }





    //TODO eventually sort out in case that a parameter is not needed anymore
//
//    public static float CURIOSITY_BUSINESS_THRESHOLD=0.15f; //dont be curious if business is above
//    public static float CURIOSITY_PRIORITY_THRESHOLD=0.3f; //0.3f in 1.6.3
//    public static float CURIOSITY_CONFIDENCE_THRESHOLD=0.8f;
//    public static float CURIOSITY_DESIRE_CONFIDENCE_MUL=0.1f; //how much risk is the system allowed to take just to fullfill its hunger for knowledge?
//    public static float CURIOSITY_DESIRE_PRIORITY_MUL=0.1f; //how much priority should curiosity have?
//    public static float CURIOSITY_DESIRE_DURABILITY_MUL=0.3f; //how much durability should curiosity have?
//    public static boolean CURIOSITY_FOR_OPERATOR_ONLY=false; //for Peis concern that it may be overkill to allow it for all <a =/> b> statement, so that a has to be an operator
//    public static boolean CURIOSITY_ALSO_ON_LOW_CONFIDENT_HIGH_PRIORITY_BELIEF=true;
//
//    //public static float HAPPY_EVENT_HIGHER_THRESHOLD=0.75f;
//    public static float HAPPY_EVENT_CHANGE_THRESHOLD =0.01f;
//    //public static float BUSY_EVENT_HIGHER_THRESHOLD=0.9f; //1.6.4, step by step^, there is already enough new things ^^
    public static float BUSY_EVENT_CHANGE_THRESHOLD =0.5f;
//    public static boolean REFLECT_META_HAPPY_GOAL = false;
//    public static boolean REFLECT_META_BUSY_BELIEF = false;
//    public static boolean CONSIDER_REMIND=true;

//
//    public static boolean QUESTION_GENERATION_ON_DECISION_MAKING=true;
//    public static boolean HOW_QUESTION_GENERATION_ON_DECISION_MAKING=true;
//
//    public static float ANTICIPATION_CONFIDENCE=0.95f;
    




}

