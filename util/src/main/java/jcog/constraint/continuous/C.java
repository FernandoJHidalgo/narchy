package jcog.constraint.continuous;

import jcog.constraint.continuous.exceptions.NonlinearExpressionException;
import jcog.list.FasterList;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by alex on 31/01/15.
 */
public class C {

    private C() {
    }

    // Variable multiply, divide, and unary invert
    public static DoubleTerm multiply(DoubleVar variable, double coefficient) {
        return new DoubleTerm(variable, coefficient);
    }

    public static DoubleTerm divide(DoubleVar variable, double denominator) {
        return multiply(variable, (1.0 / denominator));
    }

    public static DoubleTerm negate(DoubleVar variable) {
        return multiply(variable, -1.0);
    }

    // Term multiply, divide, and unary invert
    public static DoubleTerm multiply(DoubleTerm term, double coefficient) {
        return new DoubleTerm(term.var, term.coefficient * coefficient);
    }

    public static DoubleTerm divide(DoubleTerm term, double denominator) {
        return multiply(term, (1.0 / denominator));
    }

    public static DoubleTerm negate(DoubleTerm term) {
        return multiply(term, -1.0);
    }

    // Expression multiply, divide, and unary invert
    public static Expression multiply(Expression expression, double coefficient) {

        List<DoubleTerm> terms = new ArrayList<>();

        for (DoubleTerm term : expression.terms) {
            terms.add(multiply(term, coefficient));
        }

        // TODO Do we need to make a copy of the term objects in the array?
        return new Expression(terms, expression.getConstant() * coefficient);
    }

    public static Expression multiply(Expression expression1, Expression expression2) throws NonlinearExpressionException {
        if (expression1.isConstant()) {
            return multiply(expression1.getConstant(), expression2);
        } else if (expression2.isConstant()) {
            return multiply(expression2.getConstant(), expression1);
        } else {
            throw new NonlinearExpressionException();
        }
    }
    
    public static Expression divide(Expression expression, double denominator) {
        return multiply(expression, (1.0 / denominator));
    }

    public static Expression divide(Expression expression1, Expression expression2) throws NonlinearExpressionException {
        if (expression2.isConstant()) {
            return divide(expression1, expression2.getConstant());
        } else {
            throw new NonlinearExpressionException();
        }
    }

    public static Expression negate(Expression expression) {
        return multiply(expression, -1.0);
    }

    // Double multiply
    public static Expression multiply(double coefficient, Expression expression) {
        return multiply(expression, coefficient);
    }


    public static DoubleTerm multiply(double coefficient, DoubleTerm term) {
        return multiply(term, coefficient);
    }


    public static DoubleTerm multiply(double coefficient, DoubleVar variable) {
        return multiply(variable, coefficient);
    }

    // Expression add and subtract
    public static Expression add(Expression first, Expression second) {
        //TODO do we need to copy term objects?
        List<DoubleTerm> terms = new ArrayList<>(first.terms.size() + second.terms.size());

        terms.addAll(first.terms);
        terms.addAll(second.terms);

        return new Expression(terms, first.getConstant() + second.getConstant());
    }

    public static Expression add(Expression first, DoubleTerm second) {
        //TODO do we need to copy term objects?
        List<DoubleTerm> terms = new ArrayList<>(first.terms.size() + 1);

        terms.addAll(first.terms);
        terms.add(second);

        return new Expression(terms, first.getConstant());
    }

    public static Expression add(Expression expression, DoubleVar variable) {
        return add(expression, new DoubleTerm(variable));
    }

    public static Expression add(Expression expression, double constant) {
        return new Expression(expression.terms, expression.getConstant() + constant);
    }

    public static Expression subtract(Expression first, Expression second) {
        return add(first, negate(second));
    }

    public static Expression subtract(Expression expression, DoubleTerm term) {
        return add(expression, negate(term));
    }

    public static Expression subtract(Expression expression, DoubleVar variable) {
        return add(expression, negate(variable));
    }

    public static Expression subtract(Expression expression, double constant) {
        return add(expression, -constant);
    }

    // Term add and subtract
    public static Expression add(DoubleTerm term, Expression expression) {
        return add(expression, term);
    }


    public static Expression add(DoubleTerm... t) {
        return new Expression(new FasterList(t));
    }



//    public static Expression add(List<Variable> tt) {
//        return new Expression(new FasterList<>(tt.stream().map(Term::new).toArray(Term[]::new)));
//    }

    public static Expression add(List<DoubleTerm> tt) {
        return new Expression(tt);
    }

    public static Expression add(DoubleTerm term, DoubleVar variable) {
        return add(term, new DoubleTerm(variable));
    }

    public static Expression add(DoubleTerm term, double constant) {
        return new Expression(term, constant);
    }

    public static Expression subtract(DoubleTerm term, Expression expression) {
        return add(negate(expression), term);
    }

    public static Expression subtract(DoubleTerm first, DoubleTerm second) {
        return add(first, negate(second));
    }

    public static Expression subtract(DoubleTerm term, DoubleVar variable) {
        return add(term, negate(variable));
    }

    public static Expression subtract(DoubleTerm term, double constant) {
        return add(term, -constant);
    }

    // Variable add and subtract
    public static Expression add(DoubleVar variable, Expression expression) {
        return add(expression, variable);
    }

    public static Expression add(DoubleVar variable, DoubleTerm term) {
        return add(term, variable);
    }

    public static Expression add(DoubleVar first, DoubleVar second) {
        return add(new DoubleTerm(first), second);
    }

    public static Expression add(DoubleVar variable, double constant) {
        return add(new DoubleTerm(variable), constant);
    }

    public static Expression subtract(DoubleVar variable, Expression expression) {
        return add(variable, negate(expression));
    }

    public static Expression subtract(DoubleVar variable, DoubleTerm term) {
        return add(variable, negate(term));
    }

    public static Expression subtract(DoubleVar first, DoubleVar second) {
        return add(first, negate(second));
    }

    public static Expression subtract(DoubleVar variable, double constant) {
        return add(variable, -constant);
    }

    // Double add and subtract

    public static Expression add(double constant, Expression expression) {
        return add(expression, constant);
    }

    public static Expression add(double constant, DoubleTerm term) {
        return add(term, constant);
    }

    public static Expression add(double constant, DoubleVar variable) {
        return add(variable, constant);
    }

    public static Expression subtract(double constant, Expression expression) {
        return add(negate(expression), constant);
    }

    public static Expression subtract(double constant, DoubleTerm term) {
        return add(negate(term), constant);
    }

    public static Expression subtract(double constant, DoubleVar variable) {
        return add(negate(variable), constant);
    }

    // Expression relations
    public static ContinuousConstraint equals(Expression first, Expression second) {
        return new ContinuousConstraint(subtract(first, second), RelationalOperator.OP_EQ);
    }

    public static ContinuousConstraint equals(Expression expression, DoubleTerm term) {
        return equals(expression, new Expression(term));
    }

    public static ContinuousConstraint equals(Expression expression, DoubleVar variable) {
        return equals(expression, new DoubleTerm(variable));
    }

    public static ContinuousConstraint equals(Expression expression, double constant) {
        return equals(expression, new Expression(constant));
    }

    public static ContinuousConstraint lessThanOrEqualTo(Expression first, Expression second) {
        return new ContinuousConstraint(subtract(first, second), RelationalOperator.OP_LE);
    }

    public static ContinuousConstraint lessThanOrEqualTo(Expression expression, DoubleTerm term) {
        return lessThanOrEqualTo(expression, new Expression(term));
    }

    public static ContinuousConstraint lessThanOrEqualTo(Expression expression, DoubleVar variable) {
        return lessThanOrEqualTo(expression, new DoubleTerm(variable));
    }

    public static ContinuousConstraint lessThanOrEqualTo(Expression expression, double constant) {
        return lessThanOrEqualTo(expression, new Expression(constant));
    }

    public static ContinuousConstraint greaterThanOrEqualTo(Expression first, Expression second) {
        return new ContinuousConstraint(subtract(first, second), RelationalOperator.OP_GE);
    }

    public static ContinuousConstraint greaterThanOrEqualTo(Expression expression, DoubleTerm term) {
        return greaterThanOrEqualTo(expression, new Expression(term));
    }

    public static ContinuousConstraint greaterThanOrEqualTo(Expression expression, DoubleVar variable) {
        return greaterThanOrEqualTo(expression, new DoubleTerm(variable));
    }

    public static ContinuousConstraint greaterThanOrEqualTo(Expression expression, double constant) {
        return greaterThanOrEqualTo(expression, new Expression(constant));
    }

    // Term relations
    public static ContinuousConstraint equals(DoubleTerm term, Expression expression) {
        return equals(expression, term);
    }

    public static ContinuousConstraint equals(DoubleTerm first, DoubleTerm second) {
        return equals(new Expression(first), second);
    }

    public static ContinuousConstraint equals(DoubleTerm term, DoubleVar variable) {
        return equals(new Expression(term), variable);
    }

    public static ContinuousConstraint equals(DoubleTerm term, double constant) {
        return equals(new Expression(term), constant);
    }

    public static ContinuousConstraint lessThanOrEqualTo(DoubleTerm term, Expression expression) {
        return lessThanOrEqualTo(new Expression(term), expression);
    }

    public static ContinuousConstraint lessThanOrEqualTo(DoubleTerm first, DoubleTerm second) {
        return lessThanOrEqualTo(new Expression(first), second);
    }

    public static ContinuousConstraint lessThanOrEqualTo(DoubleTerm term, DoubleVar variable) {
        return lessThanOrEqualTo(new Expression(term), variable);
    }

    public static ContinuousConstraint lessThanOrEqualTo(DoubleTerm term, double constant) {
        return lessThanOrEqualTo(new Expression(term), constant);
    }

    public static ContinuousConstraint greaterThanOrEqualTo(DoubleTerm term, Expression expression) {
        return greaterThanOrEqualTo(new Expression(term), expression);
    }

    public static ContinuousConstraint greaterThanOrEqualTo(DoubleTerm first, DoubleTerm second) {
        return greaterThanOrEqualTo(new Expression(first), second);
    }

    public static ContinuousConstraint greaterThanOrEqualTo(DoubleTerm term, DoubleVar variable) {
        return greaterThanOrEqualTo(new Expression(term), variable);
    }

    public static ContinuousConstraint greaterThanOrEqualTo(DoubleTerm term, double constant) {
        return greaterThanOrEqualTo(new Expression(term), constant);
    }

    // Variable relations
    public static ContinuousConstraint equals(DoubleVar variable, Expression expression) {
        return equals(expression, variable);
    }

    public static ContinuousConstraint equals(DoubleVar variable, DoubleTerm term) {
        return equals(term, variable);
    }

    public static ContinuousConstraint equals(DoubleVar first, DoubleVar second) {
        return equals(new DoubleTerm(first), second);
    }

    public static ContinuousConstraint equals(DoubleVar variable, double constant) {
        return equals(new DoubleTerm(variable), constant);
    }

    public static ContinuousConstraint lessThanOrEqualTo(DoubleVar variable, Expression expression) {
        return lessThanOrEqualTo(new DoubleTerm(variable), expression);
    }

    public static ContinuousConstraint lessThanOrEqualTo(DoubleVar variable, DoubleTerm term) {
        return lessThanOrEqualTo(new DoubleTerm(variable), term);
    }

    public static ContinuousConstraint lessThanOrEqualTo(DoubleVar first, DoubleVar second) {
        return lessThanOrEqualTo(new DoubleTerm(first), second);
    }

    public static ContinuousConstraint lessThanOrEqualTo(DoubleVar variable, double constant) {
        return lessThanOrEqualTo(new DoubleTerm(variable), constant);
    }

    public static ContinuousConstraint greaterThanOrEqualTo(DoubleVar variable, Expression expression) {
        return greaterThanOrEqualTo(new DoubleTerm(variable), expression);
    }

    public static ContinuousConstraint greaterThanOrEqualTo(DoubleVar variable, DoubleTerm term) {
        return greaterThanOrEqualTo(term, variable);
    }

    public static ContinuousConstraint greaterThanOrEqualTo(DoubleVar first, DoubleVar second) {
        return greaterThanOrEqualTo(new DoubleTerm(first), second);
    }

    public static ContinuousConstraint greaterThanOrEqualTo(DoubleVar variable, double constant) {
        return greaterThanOrEqualTo(new DoubleTerm(variable), constant);
    }

    // Double relations
    public static ContinuousConstraint equals(double constant, Expression expression) {
        return equals(expression, constant);
    }

    public static ContinuousConstraint equals(double constant, DoubleTerm term) {
        return equals(term, constant);
    }

    public static ContinuousConstraint equals(double constant, DoubleVar variable) {
        return equals(variable, constant);
    }

    public static ContinuousConstraint lessThanOrEqualTo(double constant, Expression expression) {
        return lessThanOrEqualTo(new Expression(constant), expression);
    }

    public static ContinuousConstraint lessThanOrEqualTo(double constant, DoubleTerm term) {
        return lessThanOrEqualTo(constant, new Expression(term));
    }

    public static ContinuousConstraint lessThanOrEqualTo(double constant, DoubleVar variable) {
        return lessThanOrEqualTo(constant, new DoubleTerm(variable));
    }

    public static ContinuousConstraint greaterThanOrEqualTo(double constant, DoubleTerm term) {
        return greaterThanOrEqualTo(new Expression(constant), term);
    }

    public static ContinuousConstraint greaterThanOrEqualTo(double constant, DoubleVar variable) {
        return greaterThanOrEqualTo(constant, new DoubleTerm(variable));
    }

    // Constraint strength modifier
    public static ContinuousConstraint modifyStrength(ContinuousConstraint constraint, double strength) {
        return new ContinuousConstraint(constraint, strength);
    }

//    public static Constraint modifyStrength(double strength, Constraint constraint) {
//        return modifyStrength(strength, constraint);
//    }

}
