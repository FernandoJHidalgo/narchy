/*
 * Concept.java
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
package nars.concept;

import jcog.bag.Bag;
import jcog.pri.PriReference;
import nars.NAR;
import nars.Task;
import nars.concept.state.ConceptState;
import nars.control.Activate;
import nars.table.BeliefTable;
import nars.table.QuestionTable;
import nars.term.Term;
import nars.term.Termed;
import nars.term.container.TermContainer;
import nars.util.SoftException;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

public interface Concept extends Termed, ConcurrentMap {


    @NotNull Bag<Task,PriReference<Task>> tasklinks();

    @NotNull Bag<Term,PriReference<Term>> termlinks();


    @NotNull BeliefTable beliefs();

    @NotNull BeliefTable goals();

    @NotNull QuestionTable questions();

    @Nullable QuestionTable quests();

    Activate activate(float pri, NAR n);


    void delete(@NotNull NAR nar);

    default boolean isDeleted() {
        return state() == ConceptState.Deleted;
    }


    default void print() {
        print(System.out);
    }

    default <A extends Appendable> A print(@NotNull A out) {
        print(out, true, true, true, true);
        return out;
    }


    String printIndent = "  \t";

    /**
     * prints a summary of all termlink, tasklink, etc..
     */
    default void print(@NotNull Appendable out, boolean showbeliefs, boolean showgoals, boolean showtermlinks, boolean showtasklinks) {

        try {
            out.append("concept: ").append(toString()).append('\n');

            Consumer<PriReference> printBagItem = b -> {
                try {
                    out.append(printIndent);
                    out.append(String.valueOf(b.get())).append(' ').append(b.toBudgetString());
                    out.append(" ");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            };

            if (showtermlinks) {
                //out.append("TermLinkTemplates: ");
                //out.appendln(termlinkTemplates());

                out.append("\n TermLinks: ").append(String.valueOf(termlinks().size())).append(String.valueOf('/')).append(String.valueOf(termlinks().capacity())).append('\n');

                termlinks().forEach(printBagItem);
            }

            if (showtasklinks) {
                out.append("\n TaskLinks: ").append(String.valueOf(tasklinks().size())).append(String.valueOf('/')).append(String.valueOf(tasklinks().capacity())).append('\n');

                tasklinks().forEach(printBagItem);
            }

            out.append('\n');
        } catch (IOException e) {
            e.printStackTrace();
        }

                Consumer<Task> printTask = s -> {
            try {
                out.append(printIndent);
                out.append(s.toString());
                out.append(" ");
                Object ll = s.lastLogged();
                if (ll != null)
                    out.append(ll.toString());
                out.append('\n');
            } catch (IOException e) {
                e.printStackTrace();
            }
        };

        try {
            if (showbeliefs) {
                out.append(" Beliefs:");
                if (beliefs().isEmpty()) out.append(" none").append('\n');
                else {
                    out.append('\n');
                    beliefs().forEachTask(printTask);
                }
                out.append(" Questions:");
                if (questions().isEmpty()) out.append(" none").append('\n');
                else {
                    out.append('\n');
                    questions().forEachTask(printTask);
                }
            }

            if (showgoals) {
                out.append(" Goals:");
                if (goals().isEmpty()) out.append(" none").append('\n');
                else {
                    out.append('\n');
                    goals().forEachTask(printTask);
                }
                out.append(" Quests:");
                if (questions().isEmpty()) out.append(" none").append('\n');
                else {
                    out.append('\n');
                    quests().forEachTask(printTask);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    @NotNull ConceptState state();

    /**
     * returns the previous state
     */
    ConceptState state(@NotNull ConceptState c);

    /** should not include itself, although this will be included with these templates on activation */
    @NotNull Collection<Termed> templates(NAR nar);


    void process(Task task, @NotNull NAR n);

    float value(@NotNull Task t, float activation, long when, NAR n);

    Stream<Task> tasks(boolean includeBeliefs, boolean includeQuestions, boolean includeGoals, boolean includeQuests);

    default Stream<Task> tasks() {
        return tasks(true,true,true,true);
    }


    /**
     * Created by me on 9/13/16.
     */
    final class InvalidConceptException extends SoftException {

        @NotNull
        public final Termed term;
        @NotNull
        public final String reason;

        public InvalidConceptException(@NotNull Termed term, @NotNull String reason) {
            this.term = term;
            this.reason = reason;
        }

        @NotNull
        @Override
        public String getMessage() {
            return "InvalidConceptTerm: " + term + " (" + term.getClass() + "): " + reason;
        }

    }


}
