/*
 * Copyright (C) 2014 tc
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package nars.op.in;

import com.google.common.collect.Lists;
import nars.$;
import nars.NAR;
import nars.Narsese;
import nars.task.MutableTask;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.util.io.Twokenize;
import nars.util.io.Twokenize.Span;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;


/**
 * Twitter English - english with additional tags for twitter-like content 
 */
public class Twenglish {
    public static final Atom GOAL = $.the("exclaims");
    public static final Atom QUESTION = $.the("asks");
    //public static final Atom QUEST = $.the("quest");
    public static final Atom JUDGMENT = $.the("declares");
    public static final Atom FRAGMENT = $.the("says");

    //public final ArrayList<String> vocabulary = new ArrayList<>();
    
    /** substitutions */
    public final Map<String,String> sub = new HashMap();


    //boolean languageBooted = true; //set to false to initialize on first twenglish input
    boolean inputProduct = true;
    boolean inputConjSeq = true;
    
    
    public static final Map<String,String> POS = new HashMap(){{
        //https://www.englishclub.com/grammar/parts-of-speech-table.htm
        
        put("i", "pronoun");
        put("it", "pronoun");
        put("them", "pronoun");
        put("they", "pronoun");
        put("we", "pronoun");
        put("you", "pronoun");
        put("he", "pronoun");
        put("she", "pronoun");
        put("some", "pronoun");
        put("all", "pronoun");
        put("this", "pronoun");
        put("that", "pronoun");
        put("these", "pronoun");
        put("those", "pronoun");
        
        put("is", "verb");

        put("who", "qpronoun");
        put("what", "qpronoun");
        put("where", "qpronoun");
        put("when", "qpronoun");
        put("why", "qpronoun");
        put("which", "qpronoun");
        
        put("to", "prepos");
        put("at", "prepos");
        put("before", "prepos");
        put("after", "prepos");
        put("on", "prepos");
        put("but", "prepos");
        
        put("and", "conjunc");
        put("but", "conjunc");
        put("or", "conjunc");
        put("if", "conjunc");
        put("while", "conjunct");
                
    }};
    
    public Twenglish() {
        //TODO use word tokenization so that word substitutions dont get applied across words.
        sub.put("go to", "goto");
        //etc..
    }


    @NotNull
    protected Collection<MutableTask> parseSentence(String source, @NotNull NAR n, @NotNull List<Span> s) {

        LinkedList<Term> t = new LinkedList();
        Span last = null;
        for (Span c : s) {
            t.add( spanToTerm(c) );
            last = c;
        }
        if (t.isEmpty()) return Collections.emptyList();

        Atom sentenceType = FRAGMENT;
        if ((last!=null) && ("punct".equals(last.pattern))) {
            switch (last.content) {
                case ".": sentenceType = JUDGMENT; break;
                case "?": sentenceType = QUESTION; break;
                //case "@": sentenceType = QUEST; break;
                case "!": sentenceType = GOAL; break;
            }
        }
        if (!"words".equals(sentenceType.toString()))
            t.removeLast(); //remove the punctuation, it will be redundant


        if (t.isEmpty())
            return null;


        List<MutableTask> tt = new ArrayList();

        //1. add the logical structure of the sequence of terms
        if (inputProduct) {

            Term tokens =
                $.p(t.toArray(new Term[t.size()]));
//            Term q =
//                    $.image(2,
//                            $.the(source),
//                            sentenceType,
//                            tokens
//                    )

            Term q = $.image(2, sentenceType, $.the(source), $.sete(tokens));

            if (q != null) {
                MutableTask newtask = new MutableTask(q,'.', 1f, n).present(n); //n.task(q + ". %0.95|0.95%");
                if (newtask!=null)
                    tt.add(newtask); //TODO non-string construct
            }

        }

        //2. add the 'heard' sequence of just the terms
//        if (inputConjSeq) {
//            LinkedList<Term> cont = s.stream().map(cp -> lexToTerm(cp.content)).collect(Collectors.toCollection(LinkedList::new));
//            //separate each by a duration interval
////cont.add(Interval.interval(memory.duration(), memory));
//            cont.removeLast(); //remove trailnig interval term
//
//            Compound con = Sentence.termOrNull(Conjunction.make(cont.toArray(new Term[cont.size()]), Temporal.ORDER_FORWARD));
//            if (con!=null) {
//                throw new RuntimeException("API Upgrade not finished here:");
//                /*tt.add(
//                        memory.newTask(con, '.', 1.0f, Parameters.DEFAULT_JUDGMENT_CONFIDENCE, Parameters.DEFAULT_JUDGMENT_PRIORITY, Parameters.DEFAULT_JUDGMENT_DURABILITY)
//                );*/
//            }
//        }

        return tt;

    }


    @Nullable
    public static Term spanToTerm(@NotNull Span c) {
        return spanToTerm(c, false);
    }

    //shorthand punctuations
    public static final Atom EXCLAMATION = $.quote("!");
    public static final Atom PERIOD = $.quote(".");
    public static final Atom QUESTION_MARK = $.quote("?");
    public static final Atom COMMA = $.quote(",");

    @Nullable
    public static Term spanToTerm(@NotNull Span c, boolean includeWordPOS) {
        switch (c.pattern) {
            case "word":
                //TODO support >1 and probabalistic POS
                if (!includeWordPOS) {
                    return lexToTerm(c.content);
                }
                else {
                    String pos = POS.get(c.content.toLowerCase());
                    if (pos != null) {
                        return $.prop(lexToTerm(c.content), tagToTerm(pos));
                    }
                }
                break;
            case "punct":
                switch (c.content) {
                    case "!": return EXCLAMATION;
                    case ".": return PERIOD;
                    case "?": return QUESTION_MARK;
                    case ",": return COMMA;
                }
                break;
        }

        return $.prop( lexToTerm(c.content), tagToTerm(c.pattern) );
    }
    
    public static Term lexToTerm(String c) {
        //return Atom.the(c, true);
        return $.quote(c);
        //return Atom.the(Utf8.toUtf8(name));

        //return $.the('"' + t + '"');

//        int olen = name.length();
//        switch (olen) {
//            case 0:
//                throw new RuntimeException("empty atom name: " + name);
//
////            //re-use short term names
////            case 1:
////            case 2:
////                return theCached(name);
//
//            default:
//                if (olen > Short.MAX_VALUE/2)
//                    throw new RuntimeException("atom name too long");

        //  }
    }
    @NotNull
    public static Term tagToTerm(String c) {
        c = c.toLowerCase();
        if ("word".equals(c)) return $.quote(" ");
        return $.the(c, true);
    }
    
    
    /** returns a list of all tasks that it was able to parse for the input */
    @NotNull
    public List<MutableTask> parse(String source, @NotNull NAR n, String s) throws Narsese.NarseseException {

        
        List<MutableTask> results = $.newArrayList();

        List<Span> tokens = Twokenize.twokenize(s);
        
        List<List<Span>> sentences = $.newArrayList();
        
        List<Span> currentSentence = $.newArrayList(tokens.size());
        for (Span p : tokens) {
            
            currentSentence.add(p);
            
            if ("punct".equals(p.pattern)) {
                switch (p.content) {
                    case ".":
                    case "?":
                    case "!":
                        if (!currentSentence.isEmpty()) {
                            sentences.add(currentSentence);
                            currentSentence = $.newArrayList();
                            break;
                        }
                }
            }
        }
                
        if (!currentSentence.isEmpty())
            sentences.add(currentSentence);
        
        for (List<Span> x : sentences) {
            Collection<MutableTask> ss = parseSentence(source, n, x);
            if (ss!=null)
                results.addAll(ss);
        }
                
        if (!results.isEmpty()) {
//            if (!languageBooted) {
//
//
//                results.add(0, n.task(new StringBuilder(
//                        "<{word,pronoun,qpronoun,prepos,conjunc} -]- symbol>.").toString()));
//                results.add(0, n.task(new StringBuilder(
//                        "$0.90;0.90$ <(*,<$a-->[$d]>,<is-->[verb]>,<$b-->[$d]>) =/> <$a <-> $b>>.").toString()));
//
//                languageBooted = true;
//            }
                
        }
        
        return results;
    }

    public static List<Term> tokenize(String msg) {
        return Lists.transform(Twokenize.tokenize(msg),
                Twenglish::spanToTerm);
    }

}