package nars.web;

import jcog.RateIterator;
import jcog.data.random.XorShift128PlusRandom;
import nars.Control;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.conceptualize.DefaultConceptBuilder;
import nars.control.ChainedControl;
import nars.index.term.map.CaffeineIndex;
import nars.link.BLink;
import nars.nar.Default;
import nars.op.Command;
import nars.op.Leak;
import nars.op.mental.Abbreviation;
import nars.op.mental.Inperience;
import nars.op.stm.MySTMClustered;
import nars.rdfowl.NQuadsRDF;
import nars.time.RealTime;
import nars.util.exe.MultiThreadExecutioner;
import org.jetbrains.annotations.NotNull;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spacegraph.net.IRC;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Random;
import java.util.function.Consumer;

/**
 * $0.9;0.9;0.99$
 *
 * $0.9;0.9;0.99$ (hear(?someone, $something) ==>+1 hear(I,$something)).
 * $0.9;0.9;0.99$ (((hear(#someone,#someThing) &&+1 hear(#someone,$nextThing)) && hear(I, #someThing)) ==>+1 hear(I, $nextThing)).
 * $0.9;0.9;0.99$ (((hear($someone,$someThing) &&+1 hear($someone,$nextThing)) <=> hear($someone, ($someThing,$nextThing)))).
 * $0.9;0.9;0.99$ (((I<->#someone) && hear(#someone, $something)) ==>+1 hear(I, $something)).
 * $0.9;0.9;0.99$ hear(I, #something)!
 * hear(I,?x)?
 *
 * $0.9$ (($x,"the") <-> ($x,"a")).
 * ((($x --> (/,hear,#c,_)) &&+1 ($y --> (/,hear,#c,_))) ==> bigram($x,$y)).
 */
public class IRCAgent extends IRC {
    private static final Logger logger = LoggerFactory.getLogger(IRCAgent.class);

    //private final Talk talk;
    private final NAR nar;
    //private float ircMessagePri = 0.9f;

    private boolean hearTwenglish = true;

    final int wordDelayMS = 25; //for serializing tokens to events: the time in millisecond between each perceived (subvocalized) word, when the input is received simultaneously
    private final Leak<Task> out;

    boolean trace = false;

    public IRCAgent(NAR nar, String nick, String server, String... channels) throws Exception {
        super(nick, server, channels);

        this.nar = nar;


        out = new LeakOut(nar, 8, 0.02f) {
            @Override
            protected float send(Task task) {
                boolean cmd = task.isCommand();
                if (cmd || (trace && !task.isDeleted())) {
                    String s = (!cmd) ? task.toString() : task.term().toString();
                    Runnable r = IRCAgent.this.send(channels, s);
                    if (r!=null) {
                        nar.runLater(r);
                        if (Param.DEBUG && !task.isCommand())
                            logger.info("{}\n{}", task, task.proof());
                    } else {
                        //..?
                    }
                    return cmd ? 0 : 1; //no cost for command outputs
                }
                return 0;
            }

            @Override
            protected void in(@NotNull Task t, Consumer<BLink<Task>> each) {
                if (trace || t.isCommand())
                    super.in(t, each);
            }
        };

//        //SPEAK
//        nar.onTask(t -> {
//
//        });



//        nar.onExec(new IRCBotOperator("top") {
//
//            @Override
//            protected Object function(Compound arguments) {
//                return null;
//            }
//        });

        /*
        $0.9;0.9;0.99$ (hear(?someone, $something) ==>+1 hear(I,$something)).
 $0.9;0.9;0.99$ (((hear(#someone,#someThing) &&+1 hear(#someone,$nextThing)) && hear(I, #someThing)) ==>+1 hear(I, $nextThing)).
 $0.9;0.9;0.99$ (((hear($someone,$someThing) &&+1 hear($someone,$nextThing)) <=> hear($someone, ($someThing,$nextThing)))).
 $0.9;0.9;0.99$ (((I<->#someone) && hear(#someone, $something)) ==>+1 hear(I, $something)).
 $0.9;0.9;0.99$ hear(I, #something)!
 hear(I,?x)?

 $0.9$ (($x,"the") <-> ($x,"a")).
         */
//        nar.input(
//                "$0.9;0.9;0.99$ (hear(?someone, $something) ==>+0 hear(I,$something)).",
//                "$0.9;0.9;0.99$ (((hear(#someone,#someThing) &&+1 hear(#someone,$nextThing)) &&+0 hear(I, #someThing)) ==>+1 hear(I, $nextThing)).\n",
//                //"$0.9;0.9;0.99$ (((hear(#someone,$someThing) &&+1 hear(#someone,$nextThing)) <=> hear($someone, ($someThing,$nextThing)))).",
//                "$0.9;0.9;0.99$ hear(I, #something)!",
//                //"(((I<->#someone) && hear(#someone, $something)) ==>+1 hear(I, $something)).",
//                "$0.9;0.9;0.99$ hear(I,?x)?"
//        );
//        final Atomic HEAR = $.the("hear");
//        final Atomic I = $.the("I");
//        nar.onTask(tt->{
//            //HACK
//            if (tt.isBelief()) {
//                if (Math.abs(tt.occurrence()-nar.time()) < 100) {
//                    if (tt.op() == INH && tt.term(0).op() == PROD && tt.term(1).equals(HEAR)) {
//                        Compound arg = (Compound) tt.term(0);
//                        if (arg.term(0).equals(I)) {
//                            Term w = arg.term(1);
//                            if (!(w instanceof Variable)) {
//                                say(channels, w);
//                            }
//                        }
//                    }
//                }
//            }
//        });

    }

    public void setTrace(boolean trace) {
        this.trace = trace;
    }

    //    abstract class IRCBotOperator extends TermProcedure {
//
//
//        public IRCBotOperator(String id) {
//            super(id);
//        }
//
//
//        @Nullable
//        @Override
//        public final Object function(Compound arguments, TermIndex i) {
//            Object y = function(arguments);
//
//            if (y!=null)
//                send( y.toString().replace("\n"," ") );
//
//            //loop back as hearing
//            //say($.quote(y.toString()), $.p(nar.self, $.the("controller")));
//
//            return y;
//        }
//
//        protected abstract Object function(Compound arguments);
//
//
//    }

//    abstract class IRCBotOperator implements Operator.CommandOperator {
//
//
//        @Nullable
//        @Override
//        public final synchronized void run(Atom op, Term[] args, NAR nar) {
//
//            Object y = function(args);
//
//            if (y != null)
//                broadcast(y.toString().replace("\n", " "));
//
//            //loop back as hearing
//            //say($.quote(y.toString()), $.p(nar.self, $.the("controller")));
//
//            return null;
//        }
//
//        protected abstract Object function(Term[] arguments);
//
//    }

    void hear(String text, String src) {
        Hear.hear(nar, text, src, hearTwenglish ? wordDelayMS : -1);
    }

    @Override
    public void onPrivateMessage(PrivateMessageEvent event) throws Exception {
        //hear(event.getMessage(), event.getUser().toString());
    }

    @Override
    public void onGenericMessage(GenericMessageEvent event) throws Exception {

        if (event instanceof MessageEvent) {
            MessageEvent pevent = (MessageEvent) event;

            if (pevent.getUser().equals(irc.getUserBot())) {
                return; //ignore own messages (echo)
            }

            String msg = pevent.getMessage().trim();

            //        if (channel.equals("unknown")) return;
            if (msg.startsWith("//"))
                return; //comment or previous output


            String src = pevent.getUser().getNick(); //TODO use hostmask ?
            String channel = pevent.getChannel().getName();

            try {

                hear(msg, src);

            } catch (Exception e) {
                pevent.respond(e.toString());
            }


            //logger.info("hear({},{}): {}", channel, nick, msg);
            //talk.hear(msg, context(channel, nick), ircMessagePri);
        }


    }





    @NotNull
    public static Default newRealtimeNAR(int activeConcepts, int framesPerSecond, int conceptsPerFrame) throws FileNotFoundException {

        Random random = new XorShift128PlusRandom(System.currentTimeMillis());

        MultiThreadExecutioner exe = new MultiThreadExecutioner(2, 1024 * 32);
        exe.sync(true);

        Default nar = new Default(activeConcepts, conceptsPerFrame, 1, 3, random,

                new CaffeineIndex(new DefaultConceptBuilder(), 600 * 1024, false, exe),
                //new TreeTermIndex.L1TreeIndex(new DefaultConceptBuilder(), 400000, 64 * 1024, 3),

                new RealTime.DS(true).dur(0.25f),
                exe
        );


        int volMax = 32;

//        //Multi nar = new Multi(3,512,
//        Default nar = new Default(2048,
//                conceptsPerCycle, 2, 2, rng,
//                //new CaffeineIndex(new DefaultConceptBuilder(rng), 1024*1024, volMax/2, false, exe)
//                new TreeIndex.L1TreeIndex(new DefaultConceptBuilder(new XORShiftRandom(3)), 400000, 64*1024, 3)
//
//                , new FrameClock(), exe);


        nar.beliefConfidence(0.9f);
        nar.goalConfidence(0.9f);

        float p = 1f;
        nar.DEFAULT_BELIEF_PRIORITY = 0.8f * p;
        nar.DEFAULT_QUESTION_PRIORITY = 0.9f * p;
        nar.DEFAULT_QUESTION_QUALITY = 0.9f * p;
        nar.DEFAULT_GOAL_PRIORITY = 0.4f * p;
        nar.DEFAULT_QUEST_PRIORITY = 0.5f * p;

        nar.confMin.setValue(0.01f);
        nar.termVolumeMax.setValue(volMax);
        //nar.linkFeedbackRate.setValue(0.005f);




        MySTMClustered stm = new MySTMClustered(nar, 64, '.', 8, false, 3);
        //MySTMClustered stm2 = new MySTMClustered(nar, 32, '.', 2, true, 2);

        //new Abbreviation(nar, "_", 3, 12, 0.001f, 8);
        //new Inperience(nar, 0.003f, 8);


        nar.loop(framesPerSecond);

        return nar;
    }

    public static void main(String[] args) throws Exception {

        //Param.DEBUG = true;

        @NotNull Default n = newRealtimeNAR(1024, 35, 200);


        Control c = n.getControl();
        n.setControl(new ChainedControl(c) {
//            @Override
//            public void activate(Termed term, float priToAdd) {
//
//                synchronized(this) {
//                    System.out.print(term + " " + priToAdd + "\t===");
//                    super.activate(term, priToAdd);
//                    System.out.println(pri(term));
//                }
//            }
        });



        Hear.wiki(n);


        IRCAgent bot = new IRCAgent(n,
                "experiment1", "irc.freenode.net",
                "#123xyz"
                //"#netention"
                //"#nars"
        );

        n.on("trace", (Command) (a, t, nn) -> {
            if (t.length > 0) {
                switch (t[0].toString()) {
                    case "on": bot.setTrace(true); break;
                    case "off": bot.setTrace(false);  bot.out.clear(); break;
                }
            }
        });


        /*
        n.on("readToUs", (Command) (a, t, nn) -> {
            if (t.length > 0) {
                String url = $.unquote(t[0]);
                if (canReadURL(url)) {
                    try {

                        Term[] targets;
                        if (t.length > 1 && t[1] instanceof Compound) {
                            targets = ((Compound)t[1]).terms();
                        } else {
                            targets = null;
                        }

                        Collection<String> lines = IOUtil.readLines(new URL(url).openStream());

                        new RateIterator<String>(lines.iterator(), 2)
                                .threadEachRemaining(l -> {

                                    bot.hear(l, nn.self().toString());

                                    if (targets == null) {
                                        bot.broadcast(l);
                                    } else {
                                        for (Term u : targets)
                                            bot.send($.unquote(u), l);
                                    }

                                }).start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        */


        /*

        try {
            new RateIterator<Task>(
                NQuadsRDF.stream(n,
                    new File("/home/me/Downloads/nquad")), 500)
                        .threadEachRemaining(n::inputLater).start();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        */



        //new NARWeb(n, 8080);

        bot.start();


//        nar.run(1);

    }

    public void send(@NotNull String target, String l) {
        irc.send().message(target, l);
    }

    static boolean canReadURL(String url) {
        return url.startsWith("https://gist.githubusercontent");
    }

//    final StringBuilder b = new StringBuilder();
//
//    public synchronized void say(String[] channels, Term w) {
//        logger.info("say {}", w);
//        b.append(w.toString()).append(' ');
//        if (b.length() > 40) {
//            send(channels, b.toString());
//            b.setLength(0);
//        }
//
//    }


//    public static void main(String[] args) throws Exception {
//        //while (running) {
//            try {
//                new IRCAgent(
//                        "irc.freenode.net",
//                        //"localhost",
//                        "NARchy", "#netention");
//            } catch (Exception e) {
//                e.printStackTrace();
//
//            }
//        //}
//    }


}
//package nars.irc;
//
//import com.google.common.base.Function;
//import com.google.common.base.Predicate;
//import com.google.common.collect.Iterables;
//import nars.NAR;
//import spacegraph.net.IRC;
//
//import java.io.File;
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Paths;
//import java.util.List;
//
///**
// * Created by me on 6/20/15.
// */
//public class NLPIRCBot extends IRC {
//
//
//    private final NAR nar;
//
//    public NLPIRCBot(NAR n) throws Exception {
//        super("irc.freenode.net", "NARchy", "#nars");
//
//        this.nar = n;
//
//        new NARReaction(nar, say.class) {
//
//            public String last = "";
//
//
//            int minSpokenWords = 2;
//
//            @Override
//            public void event(Class event, Object[] args) {
//                if (event == say.class) {
//                    //Operator.ExecutionResult er = (Operator.ExecutionResult)args[0];
//
//                    Term a = (Term)args[0]; //er.getOperation().getArguments().term;
//
//                    String s = a.toString();
//                    s = s.replace("{\"", "");
//                    s = s.replace("\"}", "");
//                    s = s.trim();
//                    if (s.length() == 1) {
//                        if (s.equals("¸")) s = "."; //hotfix for the period
//                        if (s.equals(last))
//                            return; //dont repeat punctuation
//                    }
//                    else {
//
//                    }
//
//                    if (!s.isEmpty()) {
//                        say(s);
//                        last = s;
//                    }
//
////                    if (a.length >= minSpokenWords)  {
////                        String m = "";
////                        int n = 0;
////                        for (int i = 0; i < a.length; i++) {
////                            Term x = a[i];
////                            if (x.equals(nar.memory.getSelf()))
////                                continue;
////                            m += x.toString().replace("\"", "").trim() + " ";
////                        }
////                        m = m.trim();
////
////                        if (!m.equals(lastMessage))
////                            say(m);
////
////                        lastMessage = m;
////                    }
////                    else {
////                        //System.out.println("not SAY: " + Arrays.toString(a));
////
////                    }
//                }
//            }
//        };
//
//    }
//
//    public static void main(String[] args) throws Exception {
//        Global.DEBUG = false;
//
//
//        Default d = new Default();
//        //Default d = new Solid(4, 64, 0,5, 0,3);
//        d.setActiveConcepts(768);
//
//        d.executionThreshold.set(0.5);
//
//        d.temporalRelationsMax.set(4);
//
//        d.shortTermMemoryHistory.set(4);
//
//        d.conceptTaskTermProcessPerCycle.set(4);
//
//        d.conceptsFiredPerCycle.set(64);
//
//        d.duration.set(100 /* ms */);
//        d.setClock(new RealtimeMSClock(false));
//
//
//        //d.temporalPlanner(16f,8,8,2);
//
//        NAR n = new NAR( d );
//
//        TextOutput.out(n);
//
//        File corpus = new File("/tmp/h.nal");
//        n.input(corpus);
//
//        System.out.print("initializing...");
//        for (int i = 0; i < 10; i++) {
//            System.out.print(i + " ");
//            n.frame(10);
//        }
//        System.out.println("ok");
//
//
//
//
//        Video.themeInvert();
//        new NARSwing(n).setSpeed(0.04f);
//
//
//        NLPIRCBot i = new NLPIRCBot(n);
//
//        i.loop(corpus, 200);
//
//        /*String[] book = String.join(" ", Files.readAllLines(Paths.get("/home/me/battle.txt"))).split("\\. ");
//        i.read(book, 1200, 0.5f);*/
//        String[] book2 = String.join(" ", Files.readAllLines(Paths.get("/home/me/meta.txt"))).split("\\. ");
//        i.read(book2, 1300, 0.25f);
//
//    }
//
//    public void loop(File corpus, int lineDelay) {
//        new Thread(new Runnable() {
//
//            @Override
//            public void run() {
//                List<String> lines = null;
//                try {
//                    lines = Files.readAllLines(Paths.get(corpus.toURI()));
//                } catch (IOException e) {
//                    e.printStackTrace();
//                    return;
//                }
//
//                while (true)  {
//
//                    for (String s : lines) {
//                        s = s.trim();
//                        if (s.isEmpty())continue;
//
//
//                        nar.input(s);
//
//                        try {
//                            Thread.sleep(lineDelay);
//                        } catch (InterruptedException e) {
//                        }
//                    }
//
//                }
//            }
//        }).start();
//
//    }
//
//    public void read(String[] sentences, int delayMS, float priority) {
//        new Thread(new Runnable() {
//
//            @Override
//            public void run() {
//                for (String s : sentences) {
//
//                    s = s.trim();
//                    if (s.length() < 2) continue;
//
//                    if (!s.endsWith(".")  && !s.endsWith("?") && !s.endsWith("!")) s=s+'.';
//                    if (hear("book", s, priority, delayMS) == 0) continue;
//
//                }
//            }
//        }).start();
//    }
//
//
//    public int hear(String channel, String m, float priority, long wordDelay) {
//        final int delay = 25 /*cycles */, endDelay = 1000, tokenMax = 16, tokenMin = 1;
//        List<Twokenize.Span> tokens = Twokenize.twokenize(m);
//        int nonPunc = Iterables.size(Iterables.filter(tokens, new Predicate<Twokenize.Span>() {
//
//            @Override
//            public boolean apply(Twokenize.Span input) {
//                return !input.pattern.equals("punct");
//            }
//        }));
//
//        if (nonPunc > tokenMax) return 0;
//        if (nonPunc < tokenMin) return 0;
//
//
//
//        //String i = "<language --> hear>. :|: \n " + delay + "\n";
//
//        Iterable<String> s = Iterables.transform(tokens, new Function<Twokenize.Span, String>() {
//
//            @Override
//            public String apply(Twokenize.Span input) {
//                String a = "";
//                String pattern = "";
//                Term wordTerm;
//                if (input.pattern.equals("word")) {
//                    a = input.content.toLowerCase().toString();
//                    wordTerm = Atom.the(a);
//                    pattern = "word";
//                }
//                //TODO apostrophe words
//                else if (input.pattern.equals("punct")) {
//                    String b = input.content;
//                    wordTerm = Atom.quote(b);
//
//                    a = input.content;
//                    pattern = "word";
//                }
//                else {
//                    return "";
//                }
//                //else
//                //  a = "\"" + input.content.toLowerCase() + "\"";
//                //String r = "<" + a + " --> " + pattern + ">. :|:\n";
//
//                //Term tt = Inheritance.make(wordTerm, Term.get(pattern));
//                //char punc = '.';
//
//                //Term tt = Operation.make(nar.memory.operate("^say"), new Term[] {wordTerm});
//                //char punc = '!';
//
//                //nar.input(new Sentence(tt, punc, new TruthValue(1.0f, 0.9f), new Stamp(nar.memory, Tense.Present)));
//                //nar.think(delay);
//                //r += "say(" + a + ")!\n";
//                //r += delay + "\n";
////                try {
////                    Thread.sleep(delay);
////                } catch (InterruptedException e) {
////                    e.printStackTrace();
////                }
//                if (a.isEmpty()) return "";
//                //return "<{\"" + a + "\"}-->WORD>.";
//                return "(say, \"" + a + "\", " + channel + "). :|:";
//            }
//        });
//        //String xs = "say()!\n" + delay + "\n"; //clear the buffer before
//        for (String w : s) {
//            String xs = "$" + Texts.n2(priority) + "$ " + w + "\n";
//
//            System.err.println(xs);
//            nar.input(xs);
//
//            try {
//                Thread.sleep(wordDelay);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//
//        }
////
////        System.out.println(nar.time() + " HEAR: " + tokens);
////        //System.out.println("HEAR: " + i);
////
////        String i = "<(*," + c + ") --> PHRASE>.";
////        nar.input(i);
////        String j = "<(&/," + c + ") --> PHRASE>. :|:";
////        nar.input(j);
////
////        try {
////            Thread.sleep(endDelay);
////        } catch (InterruptedException e) {
////            e.printStackTrace();
////        }
//
//        return tokens.size();
//    }
//
//
//    String buffer = "";
//    int outputBufferLength = 100;
//
//    public synchronized void say(String s) {
//
//        System.out.println("say: " + s);
//        buffer += " " + s;
//
//        if (buffer.length() > outputBufferLength) {
//
//
//            buffer = buffer.trim();
//            buffer = buffer.replace(" .", ". ");
//
//            System.out.println("SAY: " + buffer);
//            if ((writer!=null) && (outputting)) {
//                send(channel, buffer);
//            }
//            buffer = "";
//        }
//
//    }
//
//    @Override
//    protected void onMessage(IRCBot bot, String channel, String nick, String msg) {
//        new Thread( () -> { hear(channel, msg, 0.7f, 100); } ).start();
//    }
// }
