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
package org.oakgp.examples.tictactoe;

import org.oakgp.Arguments;
import org.oakgp.Assignments;
import org.oakgp.function.Fn;
import org.oakgp.util.Signature;

import static org.oakgp.NodeType.nullableType;
import static org.oakgp.NodeType.type;

final class GetPossibleMove implements Fn {
    private static final Signature SIGNATURE = new Signature(nullableType(type("move")), type("board"));

    private final String displayName;
    private final java.util.function.Function<Board, Move> f;

    public GetPossibleMove(String displayName, java.util.function.Function<Board, Move> f) {
        this.displayName = displayName;
        this.f = f;
    }

    @Override
    public Object evaluate(Arguments arguments, Assignments assignments) {
        Board board = arguments.firstArg().eval(assignments);
        return f.apply(board);
    }

    @Override
    public Signature sig() {
        return SIGNATURE;
    }

    @Override
    public String name() {
        return displayName;
    }
}
