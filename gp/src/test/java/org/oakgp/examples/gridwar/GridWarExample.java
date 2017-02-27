/*
 * Copyright 2015 S. Webber
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.oakgp.examples.gridwar;

import org.oakgp.Type;
import org.oakgp.function.Function;
import org.oakgp.function.choice.If;
import org.oakgp.function.compare.*;
import org.oakgp.function.math.IntegerUtils;
import org.oakgp.node.ConstantNode;
import org.oakgp.node.Node;
import org.oakgp.rank.RankedCandidates;
import org.oakgp.rank.tournament.FirstPlayerAdvantageGame;
import org.oakgp.rank.tournament.TwoPlayerGame;
import org.oakgp.util.Random;
import org.oakgp.Evolution;
import org.oakgp.util.StdRandom;
import org.oakgp.util.Utils;

import static org.oakgp.Type.integerType;
import static org.oakgp.util.Utils.createIntegerTypeArray;

public class GridWarExample {
    private static final int NUM_VARIABLES = 5;
    private static final int NUM_GENERATIONS = 10;
    private static final int INITIAL_POPULATION_SIZE = 50;
    private static final int INITIAL_POPULATION_MAX_DEPTH = 4;

    public static void main(String[] args) {
        Function[] functions = {IntegerUtils.the.getAdd(), IntegerUtils.the.getSubtract(), IntegerUtils.the.getMultiply(),
                LessThan.create(integerType()), LessThanOrEqual.create(integerType()), new GreaterThan(integerType()), new GreaterThanOrEqual(integerType()),
                new Equal(integerType()), new NotEqual(integerType()), new If(integerType())};
        ConstantNode[] constants = Utils.createIntegerConstants(0, 4);
        Type[] variables = createIntegerTypeArray(NUM_VARIABLES);
        Random random = new StdRandom();
        // wrap a GridWar object in a FirstPlayerAdvantageGame to avoid bias
        TwoPlayerGame game = new FirstPlayerAdvantageGame(new GridWar(random));

        RankedCandidates output = new Evolution().returning(integerType()).setConstants(constants).setVariables(variables).setFunctions(functions)
                .setTwoPlayerGame(game).setInitialPopulationSize(INITIAL_POPULATION_SIZE).setTreeDepth(INITIAL_POPULATION_MAX_DEPTH)
                .setMaxGenerations(NUM_GENERATIONS).get();
        Node best = output.best().getNode();
        System.out.println(best);
    }
}
