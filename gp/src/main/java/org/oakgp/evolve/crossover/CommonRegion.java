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
package org.oakgp.evolve.crossover;

import org.oakgp.Arguments;
import org.oakgp.node.FunctionNode;
import org.oakgp.node.Node;

import static org.oakgp.node.NodeType.areFunctions;
import static org.oakgp.node.NodeType.areTerminals;

final class CommonRegion {
    /**
     * Private constructor as all methods are static.
     */
    private CommonRegion() {
        // do nothing
    }

    static Node crossoverAt(Node n1, Node n2, int crossOverPoint) {
        if (areFunctions(n1, n2)) {
            FunctionNode f1 = (FunctionNode) n1;
            FunctionNode f2 = (FunctionNode) n2;
            Arguments arguments = f1.args();
            int argCount = arguments.args();
            if (argCount == f2.args().args()) {
                int total = 0;
                for (int i = 0; i < argCount; i++) {
                    Node a1 = arguments.arg(i);
                    Node a2 = f2.args().arg(i);
                    int c = getNodeCount(a1, a2);
                    if (total + c > crossOverPoint) {
                        return new FunctionNode(f1.func(), arguments.replaceAt(i, crossoverAt(a1, a2, crossOverPoint - total)));
                    } else {
                        total += c;
                    }
                }
            }
        }

        return sameType(n1, n2) ? n2 : n1;
    }

    static int getNodeCount(Node n1, Node n2) {
        if (areFunctions(n1, n2)) {
            int total = sameType(n1, n2) ? 1 : 0;
            FunctionNode f1 = (FunctionNode) n1;
            FunctionNode f2 = (FunctionNode) n2;
            int argCount = f1.args().args();
            if (argCount == f2.args().args()) {
                for (int i = 0; i < argCount; i++) {
                    total += getNodeCount(f1.args().arg(i), f2.args().arg(i));
                }
            }
            return total;
        } else if (areTerminals(n1, n2)) {
            return sameType(n1, n2) ? 1 : 0;
        } else {
            // terminal node does not match with a function node
            return 0;
        }
    }

    private static boolean sameType(Node n1, Node n2) {
        return n1.returnType() == n2.returnType();
    }
}
