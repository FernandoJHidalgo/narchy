package no.birkett.kiwi;


import java.util.*;

/**
 * Created by alex on 30/01/15.
 */
public class Solver {

    protected static class Tag {
        Symbol marker;
        Symbol other;

        public Tag() {
            marker = new Symbol();
            other = new Symbol();
        }
    }

    protected static class EditInfo {
        final Tag tag;
        final Constraint constraint;
        double constant;

        public EditInfo(Constraint constraint, Tag tag, double constant) {
            this.constraint = constraint;
            this.tag = tag;
            this.constant = constant;
        }
    }

    protected final Map<Constraint, Tag> cns = new LinkedHashMap<>();
    protected final Map<Symbol, Row> rows = new LinkedHashMap<>();
    protected final Map<Variable, Symbol> vars = new LinkedHashMap<>();
    protected final List<Symbol> infeasibleRows = new ArrayList<>();
    protected final Row objective = new Row();
    private Row artificial;


    /**
     * Add a constraint to the solver.
     *
     * @param constraint
     * @throws DuplicateConstraintException     The given constraint has already been added to the solver.
     * @throws UnsatisfiableConstraintException The given constraint is required and cannot be satisfied.
     */
    public void add(Constraint constraint) throws DuplicateConstraintException, UnsatisfiableConstraintException {

        if (cns.containsKey(constraint)) {
            throw new DuplicateConstraintException(constraint);
        }

        Tag tag = new Tag();
        Row row = createRow(constraint, tag);
        Symbol subject = chooseSubject(row, tag);

        if (subject.type == Symbol.Type.INVALID && allDummies(row)) {
            if (!Util.nearZero(row.getConstant())) {
                throw new UnsatisfiableConstraintException(constraint);
            } else {
                subject = tag.marker;
            }
        }

        if (subject.type == Symbol.Type.INVALID) {
            if (!addWithArtificialVariable(row)) {
                throw new UnsatisfiableConstraintException(constraint);
            }
        } else {
            row.solveFor(subject);
            substitute(subject, row);
            this.rows.put(subject, row);
        }

        this.cns.put(constraint, tag);

        optimize(objective);
    }

    public void remove(Constraint constraint) throws UnknownConstraintException, InternalSolverError {
        Tag tag = cns.get(constraint);
        if (tag == null) {
            throw new UnknownConstraintException(constraint);
        }

        cns.remove(constraint);
        removeConstraintEffects(constraint, tag);

        Row row = rows.get(tag.marker);
        if (row != null) {
            rows.remove(tag.marker);
        } else {
            row = getMarkerLeavingRow(tag.marker);
            if (row == null) {
                throw new InternalSolverError("internal solver error");
            }

            //This looks wrong! changes made below
            //Symbol leaving = tag.marker;
            //rows.remove(tag.marker);

            Symbol leaving = null;
            for (Symbol s : rows.keySet()) {
                if (rows.get(s) == row) {
                    leaving = s;
                }
            }
            if (leaving == null) {
                throw new InternalSolverError("internal solver error");
            }

            rows.remove(leaving);
            row.solveFor(leaving, tag.marker);
            substitute(tag.marker, row);
        }
        optimize(objective);
    }

    void removeConstraintEffects(Constraint constraint, Tag tag) {
        if (tag.marker.type == Symbol.Type.ERROR) {
            removeMarkerEffects(tag.marker, constraint.getStrength());
        } else if (tag.other.type == Symbol.Type.ERROR) {
            removeMarkerEffects(tag.other, constraint.getStrength());
        }
    }

    void removeMarkerEffects(Symbol marker, double strength) {
        Row row = rows.get(marker);
        if (row != null) {
            objective.insert(row, -strength);
        } else {
            objective.insert(marker, -strength);
        }
    }

    Row getMarkerLeavingRow(Symbol marker) {
        double dmax = Double.MAX_VALUE;
        double r1 = dmax;
        double r2 = dmax;

        Row first = null;
        Row second = null;
        Row third = null;

        for (Map.Entry<Symbol, Row> symbolRowEntry : rows.entrySet()) {
            Row candidateRow = symbolRowEntry.getValue();
            double c = candidateRow.coefficientFor(marker);
            if (c == 0.0) {
                continue;
            }
            if ((symbolRowEntry.getKey()).type == Symbol.Type.EXTERNAL) {
                third = candidateRow;
            } else if (c < 0.0) {
                double r = -candidateRow.getConstant() / c;
                if (r < r1) {
                    r1 = r;
                    first = candidateRow;
                }
            } else {
                double r = candidateRow.getConstant() / c;
                if (r < r2) {
                    r2 = r;
                    second = candidateRow;
                }
            }
        }

        if (first != null) {
            return first;
        }
        if (second != null) {
            return second;
        }
        return third;
    }

    public boolean hasConstraint(Constraint constraint) {
        return cns.containsKey(constraint);
    }

    /**
     * Update the values of the external solver variables.
     */
    public void update() {

        for (Map.Entry<Variable, Symbol> varEntry : vars.entrySet()) {
            Variable variable = varEntry.getKey();
            Row row = this.rows.get(varEntry.getValue());

            if (row == null) {
                variable.value(0);
            } else {
                variable.value(row.getConstant());
            }
        }
    }


    /**
     * Create a new Row object for the given constraint.
     * <p/>
     * The terms in the constraint will be converted to cells in the row.
     * Any term in the constraint with a coefficient of zero is ignored.
     * This method uses the `getVarSymbol` method to get the symbol for
     * the variables added to the row. If the symbol for a given cell
     * variable is basic, the cell variable will be substituted with the
     * basic row.
     * <p/>
     * The necessary slack and error variables will be added to the row.
     * If the constant for the row is negative, the sign for the row
     * will be inverted so the constant becomes positive.
     * <p/>
     * The tag will be updated with the marker and error symbols to use
     * for tracking the movement of the constraint in the tableau.
     */
    Row createRow(Constraint constraint, Tag tag) {
        Expression expression = constraint.expression;
        Row row = new Row(expression.getConstant());

        List<Term> terms = expression.terms;
        for (int i = 0, termsSize = terms.size(); i < termsSize; i++) {
            Term term = terms.get(i);
            double coefficient = term.coefficient;
            if (!Util.nearZero(coefficient)) {

                Symbol symbol = getVarSymbol(term.var);

                Row otherRow = rows.get(symbol);

                if (otherRow == null) {
                    row.insert(symbol, coefficient);
                } else {
                    row.insert(otherRow, coefficient);
                }
            }
        }

        double str = constraint.getStrength();

        switch (constraint.op) {
            case OP_LE:
            case OP_GE: {
                double coeff = constraint.op == RelationalOperator.OP_LE ? 1.0 : -1.0;
                Symbol slack = new Symbol(Symbol.Type.SLACK);
                tag.marker = slack;
                row.insert(slack, coeff);
                if (str < Strength.REQUIRED) {
                    Symbol error = new Symbol(Symbol.Type.ERROR);
                    tag.other = error;
                    row.insert(error, -coeff);
                    this.objective.insert(error, str);
                }
                break;
            }
            case OP_EQ: {
                if (str < Strength.REQUIRED) {
                    Symbol errplus = new Symbol(Symbol.Type.ERROR);
                    Symbol errminus = new Symbol(Symbol.Type.ERROR);
                    tag.marker = errplus;
                    tag.other = errminus;
                    row.insert(errplus, -1.0); // v = eplus - eminus
                    row.insert(errminus, 1.0); // v - eplus + eminus = 0
                    this.objective.insert(errplus, str);
                    this.objective.insert(errminus, str);
                } else {
                    Symbol dummy = new Symbol(Symbol.Type.DUMMY);
                    tag.marker = dummy;
                    row.insert(dummy);
                }
                break;
            }
        }

        // Ensure the row as a positive constant.
        if (row.getConstant() < 0.0) {
            row.reverseSign();
        }

        return row;
    }

    /**
     * Choose the subject for solving for the row
     * <p/>
     * This method will choose the best subject for using as the solve
     * target for the row. An invalid symbol will be returned if there
     * is no valid target.
     * The symbols are chosen according to the following precedence:
     * 1) The first symbol representing an external variable.
     * 2) A negative slack or error tag variable.
     * If a subject cannot be found, an invalid symbol will be returned.
     */
    private static Symbol chooseSubject(Row row, Tag tag) {

        for (Map.Entry<Symbol, Double> cell : row.cells.entrySet()) {
            if (cell.getKey().type == Symbol.Type.EXTERNAL) {
                return cell.getKey();
            }
        }
        if (tag.marker.type == Symbol.Type.SLACK || tag.marker.type == Symbol.Type.ERROR) {
            if (row.coefficientFor(tag.marker) < 0.0)
                return tag.marker;
        }
        if (tag.other != null && (tag.other.type == Symbol.Type.SLACK || tag.other.type == Symbol.Type.ERROR)) {
            if (row.coefficientFor(tag.other) < 0.0)
                return tag.other;
        }
        return new Symbol();
    }

    /**
     * Add the row to the tableau using an artificial variable.
     * <p/>
     * This will return false if the constraint cannot be satisfied.
     */
    private boolean addWithArtificialVariable(Row row) {
        //TODO check this

        // Create and add the artificial variable to the tableau

        Symbol art = new Symbol(Symbol.Type.SLACK);
        rows.put(art, new Row(row));

        this.artificial = new Row(row);

        // Optimize the artificial objective. This is successful
        // only if the artificial objective is optimized to zero.
        optimize(this.artificial);
        boolean success = Util.nearZero(artificial.getConstant());
        artificial = null;

        // If the artificial variable is basic, pivot the row so that
        // it becomes basic. If the row is constant, exit early.

        Row rowptr = this.rows.get(art);

        if (rowptr != null) {

            /**this looks wrong!!!*/
            //rows.remove(rowptr);

            LinkedList<Symbol> deleteQueue = new LinkedList<>();
            for (Map.Entry<Symbol, Row> symbolRowEntry : rows.entrySet()) {
                if (symbolRowEntry.getValue() == rowptr) {
                    deleteQueue.add(symbolRowEntry.getKey());
                }
            }
            while (!deleteQueue.isEmpty()) {
                rows.remove(deleteQueue.pop());
            }

            if (rowptr.cells.isEmpty()) {
                return success;
            }

            deleteQueue.clear();

            Symbol entering = anyPivotableSymbol(rowptr);
            if (entering.type == Symbol.Type.INVALID) {
                return false; // unsatisfiable (will this ever happen?)
            }
            rowptr.solveFor(art, entering);
            substitute(entering, rowptr);
            this.rows.put(entering, rowptr);
        }

        // Remove the artificial variable from the tableau.
        rows.values().forEach(r -> r.remove(art));

        objective.remove(art);

        return success;
    }

    /**
     * Substitute the parametric symbol with the given row.
     * <p/>
     * This method will substitute all instances of the parametric symbol
     * in the tableau and the objective function with the given row.
     */
    void substitute(Symbol symbol, Row row) {
        for (Map.Entry<Symbol, Row> rowEntry : rows.entrySet()) {
            Row v = rowEntry.getValue();
            v.substitute(symbol, row);
            Symbol k = rowEntry.getKey();
            if (k.type != Symbol.Type.EXTERNAL && v.getConstant() < 0.0) {
                infeasibleRows.add(k);
            }
        }

        objective.substitute(symbol, row);

        if (artificial != null) {
            artificial.substitute(symbol, row);
        }
    }

    /**
     * Optimize the system for the given objective function.
     * <p/>
     * This method performs iterations of Phase 2 of the simplex method
     * until the objective function reaches a minimum.
     *
     * @throws InternalSolverError The value of the objective function is unbounded.
     */
    void optimize(Row objective) {
        while (true) {
            Symbol entering = getEnteringSymbol(objective);
            if (entering.type == Symbol.Type.INVALID) {
                return;
            }

            Row entry = getLeavingRow(entering);
            if (entry == null) {
                throw new InternalSolverError("The objective is unbounded.");
            }

            Symbol leaving = null;
            for (Map.Entry<Symbol, Row> key : rows.entrySet()) {
                if (key.getValue() == entry) {
                    leaving = key.getKey();
                }
            }

            rows.remove(leaving);
            entry.solveFor(leaving, entering);
            substitute(entering, entry);
            rows.put(entering, entry);
        }
    }


    /**
     * Compute the entering variable for a pivot operation.
     * <p/>
     * This method will return first symbol in the objective function which
     * is non-dummy and has a coefficient less than zero. If no symbol meets
     * the criteria, it means the objective function is at a minimum, and an
     * invalid symbol is returned.
     */
    private static Symbol getEnteringSymbol(Row objective) {

        for (Map.Entry<Symbol, Double> cell : objective.cells.entrySet()) {

            Symbol k = cell.getKey();
            if (k.type != Symbol.Type.DUMMY && cell.getValue() < 0.0) {
                return k;
            }
        }
        return new Symbol();

    }


    /**
     * Get the first Slack or Error symbol in the row.
     * <p/>
     * If no such symbol is present, and Invalid symbol will be returned.
     */
    private static Symbol anyPivotableSymbol(Row row) {
        Symbol symbol =
                row.cells.keySet().stream()
                        .filter(k -> k.type == Symbol.Type.SLACK || k.type == Symbol.Type.ERROR)
                        .findFirst().orElseGet(Symbol::new);

//        for (Map.Entry<Symbol, Double> entry : row.cells.entrySet()) {
//            Symbol k = entry.getKey();
//            if (k.type == Symbol.Type.SLACK || k.type == Symbol.Type.ERROR) {
//                symbol = k;
//            }
//        }
//        if (symbol == null) {
//            symbol = new Symbol();
//        }
        return symbol;
    }

    /**
     * Compute the row which holds the exit symbol for a pivot.
     * <p/>
     * This documentation is copied from the C++ version and is outdated
     * <p/>
     * <p/>
     * This method will return an iterator to the row in the row map
     * which holds the exit symbol. If no appropriate exit symbol is
     * found, the end() iterator will be returned. This indicates that
     * the objective function is unbounded.
     */
    private Row getLeavingRow(Symbol entering) {
        double ratio = Double.MAX_VALUE;
        Row row = null;

        for (Map.Entry<Symbol, Row> symbolRowEntry : rows.entrySet()) {
            if ((symbolRowEntry.getKey()).type != Symbol.Type.EXTERNAL) {
                Row candidateRow = symbolRowEntry.getValue();
                double temp = candidateRow.coefficientFor(entering);
                if (temp < 0) {
                    double temp_ratio = (-candidateRow.getConstant() / temp);
                    if (temp_ratio < ratio) {
                        ratio = temp_ratio;
                        row = candidateRow;
                    }
                }
            }
        }
        return row;
    }

    /**
     * Get the symbol for the given variable.
     * <p/>
     * If a symbol does not exist for the variable, one will be created.
     */
    private Symbol getVarSymbol(Variable variable) {
        return vars.computeIfAbsent(variable, (v) -> new Symbol(Symbol.Type.EXTERNAL));
    }

    /**
     * Test whether a row is composed of all dummy variables.
     */
    private static boolean allDummies(Row row) {
        return row.cells.keySet().stream().allMatch(x -> x.type == Symbol.Type.DUMMY);
    }

}
