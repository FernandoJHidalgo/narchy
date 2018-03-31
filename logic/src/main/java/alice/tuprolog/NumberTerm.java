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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * Number abstract class represents numbers prolog data type
 *
 * @see Int
 * @see Long
 * @see Float
 * @see Double
 *
 * Reviewed by Paolo Contessi: implements Comparable<Number>
 */
public abstract class NumberTerm extends Term implements Comparable<NumberTerm> {

    /**
     *  Returns the value of the number as int
     */
    public abstract int intValue();
    
    /**
     *  Returns the value of the number as float
     */
    public abstract float floatValue();
    
    /**
     *  Returns the value of the number as long
     */
    public abstract long longValue();
    
    /**
     *  Returns the value of the number as double
     */
    public abstract double doubleValue();
    
    
    /** is this term a prolog integer term? */
    public abstract boolean isInteger();
    
    /** is this term a prolog real term? */
    public abstract boolean isReal();
    
    //

//    public static Number createNumber(String s) {
//        Term t = Term.createTerm(s);
//        if (t instanceof Number)
//            return (Number) t;
//        throw new InvalidTermException("Term " + t + " is not a number.");
//    }
    
    /**
     * Gets the actual term referred by this Term.
     */
    @Override
    public final Term term() {
        return this;
    }
    
    // checking type and properties of the Term

    @Override
    final public boolean isEmptyList() {
        return false;
    }
    
    //
    
    /** is this term a constant prolog term? */
    @Override
    final public boolean isAtom() {
        return true;
    }
    
    /** is this term a prolog compound term? */
    @Override
    final public boolean isCompound() {
        return false;
    }
    
    /** is this term a prolog (alphanumeric) atom? */
    @Override
    final public boolean isAtomic() {
        return false;
    }
    
    /** is this term a prolog list? */
    @Override
    final public boolean isList() {
        return false;
    }
    
    /** is this term a ground term? */
    @Override
    final public boolean isGround() {
        return true;
    }
    
    
    //
    
    /**
     * gets a copy of this term.
     */
    public Term copy(int idExecCtx) {
        return this;
    }
    
    /**
     * gets a copy (with renamed variables) of the term.
     * <p>
     * the list argument passed contains the list of variables to be renamed
     * (if empty list then no renaming)
     */
    @Override
    final Term copy(Map<Var, Var> vMap, int idExecCtx) {
        return this;
    }
    
    /**
     * gets a copy of the term.
     * @param vMap
     * @param substMap
     */
    @Override
    final Term copy(Map<Var, Var> vMap, Map<Term, Var> substMap) {
        return this;
    }
    
    
    @Override
    void resolveTerm(long count) {

    }

    
    void restoreVariables() {}

    /**/

    /**
     *
     * Long class represents the long prolog data type
     *
     *
     *
     */
    public static class Long extends NumberTerm {
       private static final long serialVersionUID = 1L;
       private final long value;

        public Long(long v) {
            value = v;
        }

        /**
         *  Returns the value of the Integer as int
         *
         */
        @Override
        final public int intValue() {
            return (int) value;
        }

        /**
         *  Returns the value of the Integer as float
         *
         */
        @Override
        final public float floatValue() {
            return value;
        }

        /**
         *  Returns the value of the Integer as double
         *
         */
        @Override
        final public double doubleValue() {
            return value;
        }

        /**
         *  Returns the value of the Integer as long
         *
         */
        @Override
        final public long longValue() {
            return value;
        }


        /** is this term a prolog integer term? */
        @Override
        final public boolean isInteger() {
            return true;
        }

        /** is this term a prolog real term? */
        @Override
        final public boolean isReal() {
            return false;
        }

        /**
         * Returns true if this integer term is grater that the term provided.
         * For number term argument, the int value is considered.
         */
        @Override
        public boolean isGreater(Term t) {
            t = t.term();
            if (t instanceof NumberTerm) {
                return value > ( (NumberTerm) t ).longValue();
            } else if (t instanceof Struct) {
                return false;
            } else return t instanceof Var;
        }
        @Override
        public boolean isGreaterRelink(Term t, ArrayList<String> vorder) {
            return isGreater(t);
    //        t = t.getTerm();
    //        if (t instanceof Number) {
    //            return value > ( (Number) t ).longValue();
    //        } else if (t instanceof Struct) {
    //            return false;
    //        } else return t instanceof Var;
        }

        /**
         * Returns true if this integer term is equal that the term provided.
         * For number term argument, the int value is considered.
         */
        @Override
        public boolean isEqual(Term t) {
            t = t.term();
            return t instanceof NumberTerm && value == ((NumberTerm) t).longValue();
        }

        /**
         * Tries to unify a term with the provided term argument.
         * This service is to be used in demonstration context.
         */
        @Override
        boolean unify(List<Var> vl1, List<Var> vl2, Term t) {
            t = t.term();
            if (t instanceof Var) {
                return t.unify(vl1, vl2, this);
            } else if (t instanceof NumberTerm && ((NumberTerm) t).isInteger()) {
                return value == ((NumberTerm) t).longValue();
            } else {
                return false;
            }
        }

        public String toString() {
            return java.lang.Long.toString(value);
        }

        /**
         * @author Paolo Contessi
         */
        @Override
        public int compareTo(NumberTerm o) {
            return java.lang.Long.compare(value, o.longValue());
        }

    }

    /**
     *
     * Int class represents the integer prolog data type
     *
     */
    public static class Int extends NumberTerm {
       private final int      value;

        public Int(int v) {
            value = v;
        }

        /**
         *  Returns the value of the Integer as int
         *
         */
        @Override
        final public int intValue() {
            return value;
        }

        /**
         *  Returns the value of the Integer as float
         *
         */
        @Override
        final public float floatValue() {
            return value;
        }

        /**
         *  Returns the value of the Integer as double
         *
         */
        @Override
        final public double doubleValue() {
            return value;
        }

        /**
         *  Returns the value of the Integer as long
         *
         */
        @Override
        final public long longValue() {
            return value;
        }


        /** is this term a prolog integer term? */
        @Override
        final public boolean isInteger() {
            return true;
        }

        /** is this term a prolog real term? */
        @Override
        final public boolean isReal() {
            return false;
        }



        /**
         * Returns true if this integer term is grater that the term provided.
         * For number term argument, the int value is considered.
         */
        @Override
        public boolean isGreater(Term t) {
            t = t.term();
            if (t instanceof NumberTerm) {
                return value>((NumberTerm)t).intValue();
            } else if (t instanceof Struct) {
                return false;
            } else return t instanceof Var;
        }
        @Override
        public boolean isGreaterRelink(Term t, ArrayList<String> vorder) {
            return isGreater(t);
            /*t = t.getTerm();
            if (t instanceof Number) {
                return value>((Number)t).intValue();
            } else if (t instanceof Struct) {
                return false;
            } else return t instanceof Var;*/
        }

        /**
         * Returns true if this integer term is equal to the term provided.
         */
        @Override
        public boolean isEqual(Term t) {
            if (this == t) return true;
            t = t.term();
            if (this == t) return true;
            if (t instanceof NumberTerm) {
                NumberTerm n = (NumberTerm) t;
                if (!n.isInteger())
                    return false;
                return value == n.longValue();
            } else
                return false;
        }

        /**
         * Tries to unify a term with the provided term argument.
         * This service is to be used in demonstration context.
         */
        @Override
        boolean unify(List<Var> vl1, List<Var> vl2, Term t) {
            t = t.term();
            if (t instanceof Var) {
                return t.unify(vl2, vl1, this);
            } else if (t instanceof NumberTerm && ((NumberTerm) t).isInteger()) {
                return value == ((NumberTerm) t).intValue();
            } else {
                return false;
            }
        }

        public String toString() {
            return Integer.toString(value);
        }

        /**
         * @author Paolo Contessi
         */
        @Override
        public int compareTo(NumberTerm o) {
            return Integer.compare(value, o.intValue());
        }

    }

    /**
     *
     * Float class represents the float prolog data type
     *
     *
     *
     */
    public static class Float extends NumberTerm {
        private static final long serialVersionUID = 1L;
        private final float value;

        public Float(float v) {
            value=v;
        }

        /**
         *  Returns the value of the Float as int
         *
         */
        @Override
        final public int intValue() {
            return (int) value;
        }

        /**
         *  Returns the value of the Float as float
         *
         */
        @Override
        final public float floatValue() {
            return value;
        }

        /**
         *  Returns the value of the Float as double
         *
         */
        @Override
        final public double doubleValue() {
            return value;
        }

        /**
         *  Returns the value of the Float as long
         *
         */
        @Override
        final public long longValue() {
            return (long) value;
        }


        /** is this term a prolog integer term? */
        @Override
        final public boolean isInteger() {
            return false;
        }

        /** is this term a prolog real term? */
        @Override
        final public boolean isReal() {
            return true;
        }

        /**
         * Returns true if this Float term is grater that the term provided.
         * For number term argument, the int value is considered.
         */
        @Override
        public boolean isGreater(Term t) {
            t = t.term();
            if (t instanceof NumberTerm) {
                return value>((NumberTerm)t).floatValue();
            } else if (t instanceof Struct) {
                return false;
            } else return t instanceof Var;
        }
        @Override
        public boolean isGreaterRelink(Term t, ArrayList<String> vorder) {
            t = t.term();
            if (t instanceof NumberTerm) {
                return value>((NumberTerm)t).floatValue();
            } else if (t instanceof Struct) {
                return false;
            } else return t instanceof Var;
        }

        /**
         * Returns true if this Float term is equal that the term provided.
         * For number term argument, the int value is considered.
         */
        @Override
        public boolean isEqual(Term t) {
            t = t.term();
            return t instanceof NumberTerm && value == ((NumberTerm) t).floatValue();
        }

        /**
         * Tries to unify a term with the provided term argument.
         * This service is to be used in demonstration context.
         */
        @Override
        boolean unify(List<Var> vl1, List<Var> vl2, Term t) {
            t = t.term();
            if (t instanceof Var) {
                return t.unify(vl2, vl1, this);
            } else if (t instanceof NumberTerm && ((NumberTerm) t).isReal()) {
                return value == ((NumberTerm) t).floatValue();
            } else {
                return false;
            }
        }

        public String toString() {
            return java.lang.Float.toString(value);
        }

        /**
         * @author Paolo Contessi
         */
        @Override
        public int compareTo(NumberTerm o) {
            return java.lang.Float.compare(value, o.floatValue());
        }

    }

    /**
     *
     * Double class represents the double prolog data type
     *
     */
    public static class Double extends NumberTerm {
        private static final long serialVersionUID = 1L;
        private final double value;

        public Double(double v) {
            value = v;
        }

        /**
         *  Returns the value of the Double as int
         */
        @Override
        final public int intValue() {
            return (int) value;
        }

        /**
         *  Returns the value of the Double as float
         *
         */
        @Override
        final public float floatValue() {
            return (float) value;
        }

        /**
         *  Returns the value of the Double as double
         *
         */
        @Override
        final public double doubleValue() {
            return value;
        }

        /**
         *  Returns the value of the Double as long
         */
        @Override
        final public long longValue() {
            return (long) value;
        }


        /** is this term a prolog integer term? */
        @Override
        final public boolean isInteger() {
            return false;
        }

        /** is this term a prolog real term? */
        @Override
        final public boolean isReal() {
            return true;
        }

        /**
         * Returns true if this Double term is grater that the term provided.
         * For number term argument, the int value is considered.
         */
        @Override
        public boolean isGreater(Term t) {
            t = t.term();
            if (t instanceof NumberTerm) {
                return value>((NumberTerm)t).doubleValue();
            } else if (t instanceof Struct) {
                return false;
            } else return t instanceof Var;
        }

        @Override
        public boolean isGreaterRelink(Term t, ArrayList<String> vorder) {
            t = t.term();
            if (t instanceof NumberTerm) {
                return value>((NumberTerm)t).doubleValue();
            } else if (t instanceof Struct) {
                return false;
            } else return t instanceof Var;
        }

        /**
         * Returns true if this Double term is equal to the term provided.
         */
        @Override
        public boolean isEqual(Term t) {
            t = t.term();
            if (t instanceof NumberTerm) {
                NumberTerm n = (NumberTerm) t;
                if (!n.isReal())
                    return false;
                return value == n.doubleValue();
            } else {
                return false;
            }
        }

        /**
         * Tries to unify a term with the provided term argument.
         * This service is to be used in demonstration context.
         */
        @Override
        boolean unify(List<Var> vl1, List<Var> vl2, Term t) {
            t = t.term();
            if (t instanceof Var) {
                return t.unify(vl2, vl1, this);
            } else if (t instanceof NumberTerm && ((NumberTerm) t).isReal()) {
                return value == ((NumberTerm) t).doubleValue();
            } else {
                return false;
            }
        }

        public String toString() {
            return java.lang.Double.toString(value);
        }

    //    public int resolveVariables(int count) {
    //        return count;
    //    }

        /**
         * @author Paolo Contessi
         */
        @Override
        public final int compareTo(NumberTerm o) {
            return java.lang.Double.compare(value, o.doubleValue()); //(new java.lang.Double(value)).compareTo(o.doubleValue());
        }

    }
}