/*
 * Copyright 2015 S. Webber
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http:
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.oakgp.evolve;

import org.oakgp.node.Node;
import org.oakgp.rank.Candidates;
import org.oakgp.select.NodeSelector;
import org.oakgp.select.NodeSelectorFactory;
import org.oakgp.util.NodeSimplifier;

import java.util.Map;
import java.util.stream.Stream;

import static java.lang.Math.min;

/**
 * Uses a user-defined collection of {@code GeneticOperator} instances to evolve new generations from existing generations.
 */
public final class GenerationEvolverImpl implements GenerationEvolver {
    private final int elitismSize;
    private final NodeSelectorFactory selectorFactory;
    private final Map<GeneticOperator, Integer> operators;

    /**
     * Creates a {@code GenerationEvolverImpl} that uses the given values to evolve new generations.
     *
     * @param elitismSize     the number of best candidates from an existing generation to automatically include "as-is" in the next generation
     * @param selectorFactory used to select candidates from an existing generation to be used as a basis for evolving candidates for the next generation
     * @param operators       the genetic operators to be used to evolve new candidates where the key = a genetic operator and the value = the number of times that genetic
     *                        operator should be applied during each single invocation of {@link #apply(Candidates)}
     */
    public GenerationEvolverImpl(int elitismSize, NodeSelectorFactory selectorFactory, Map<GeneticOperator, Integer> operators) {
        this.elitismSize = elitismSize;
        this.selectorFactory = selectorFactory;
        this.operators = operators;
    }

    /**
     * Returns a new generation of {@code Node} instances evolved from the specified existing generation.
     *
     * @param oldGeneration the existing generation to use as a basis for evolving a new generation
     * @return a new generation of {@code Node} instances evolved from the existing generation specified by {@code oldGeneration}
     */
    @Override
    public Stream<Node> apply(Candidates oldGeneration) {
        NodeSelector selector = selectorFactory.getSelector(oldGeneration);

        final int elitismSizeForGeneration = min(elitismSize, oldGeneration.size());
        return Stream.concat(

            oldGeneration.stream().limit(elitismSizeForGeneration).map(oldGenerationIndividual -> oldGenerationIndividual.node),

            operators.entrySet().stream().flatMap(e->{
                GeneticOperator operator = e.getKey();
                int count = e.getValue();
                return Stream.generate(()->operator.apply(selector)).limit(count);
            }).map( NodeSimplifier::simplify )
        ).distinct();
    }
}
