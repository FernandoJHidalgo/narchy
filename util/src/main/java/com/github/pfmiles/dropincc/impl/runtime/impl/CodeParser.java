/*******************************************************************************
 * Copyright (c) 2012 pf_miles.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     pf_miles - initial API and implementation
 ******************************************************************************/
package com.github.pfmiles.dropincc.impl.runtime.impl;

import java.util.HashMap;
import java.util.Map;

import com.github.pfmiles.dropincc.DropinccException;
import com.github.pfmiles.dropincc.impl.TokenType;
import com.github.pfmiles.dropincc.impl.runtime.Token;
import com.github.pfmiles.dropincc.impl.util.Pair;

/**
 * The parer super class for non-stream (that means, input is finite) parsing.
 * 
 * @author pf-miles
 * 
 */
public abstract class CodeParser extends Parser implements Cloneable {

    protected CodeLexer lexer;

    // key[ruleNum, startPosition] -> Pair[cachedParsingResult, forwardPosition]
    private Map<ParseCacheKey, Pair<DelayedAction, Integer>> parseCache = new HashMap<>();

    private int lastForwardPosition = -1;

    // put the current parsing result at the specific position into cache, to
    // ensure a linear parsing time when backtracking occured
    protected void putInCache(int ruleNum, DelayedAction node, int startPoint) {
        int forwardPosition = lexer.getCurrentPosition();
        this.parseCache.put(new ParseCacheKey(ruleNum, startPoint), new Pair<>(node, forwardPosition));
    }

    // try to get the cached result from the parsing cache
    protected DelayedAction tryResolveFromCache(int ruleNum) {
        ParseCacheKey k = new ParseCacheKey(ruleNum, lexer.getCurrentPosition());
        if (parseCache.containsKey(k)) {
            Pair<DelayedAction, Integer> p = parseCache.get(k);
            this.lastForwardPosition = p.getRight();
            return p.getLeft();
        } else {
            return null;
        }
    }

    // to match a specific token type, returned the matched lexeme
    protected Object match(TokenType type) {
        Token ret = lexer.nextElement();
        if (!ret.getType().equals(type))
            throw new DropinccException("Unexpected token encountered: " + ret + ", expected type: " + type + ", at position: "
                    + lexer.getCurrentPosition());
        return ret.getLexeme();
    }

    // fast forward to go by pass the cached parsing element which is hit
    protected void fastForward() {
        lexer.fastForward(this.lastForwardPosition);
    }

    @Override
    public void setLexer(Lexer lexer) {
        this.lexer = (CodeLexer) lexer;
    }

    // clean all cached parsing element
    protected void cleanCache() {
        if (!this.parseCache.isEmpty())
            this.parseCache.clear();
    }

    // creates delayedAction, avoid creating needless DelayedAcion objects
    protected static DelayedAction newDelayedAction(Object action, Object matched) {
        if (matched instanceof DelayedAction) {
            DelayedAction da = (DelayedAction) matched;
            if (action == null) {
                return da;
            } else if (da.action == null) {
                da.action = action;
                return da;
            }
        }
        return new DelayedAction(action, matched);
    }

    @Override
    protected CodeParser clone() {
        CodeParser ret = null;
        try {
            ret = (CodeParser) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new DropinccException(e);
        }
        // reset all states
        ret.parseCache = new HashMap<>();
        return ret;
    }
}
