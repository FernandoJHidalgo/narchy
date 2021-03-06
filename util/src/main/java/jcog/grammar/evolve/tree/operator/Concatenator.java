/*
 * Copyright (C) 2015 Machine Learning Lab - University of Trieste, 
 * Italy (http:
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http:
 */
package jcog.grammar.evolve.tree.operator;

import jcog.grammar.evolve.tree.DescriptionContext;
import jcog.grammar.evolve.tree.Node;

/**
 *
 * @author MaleLabTs
 */
public class Concatenator extends BinaryOperator {

    public Concatenator(Node a, Node b) {
        super(a, b);
    }
    public Concatenator() {
        super();
    }

    @Override
    protected BinaryOperator buildCopy() {
        return new Concatenator();
    }

    @Override
    public void describe(StringBuilder builder, DescriptionContext context, RegexFlavour flavour) {
        getLeft().describe(builder, context, flavour);
        getRight().describe(builder, context, flavour);
    }

    @Override
    public boolean isValid() {
        if(getLeft() instanceof Or || getRight() instanceof Or)
            return false;
        return getLeft().isValid() && getRight().isValid();
    }

}
