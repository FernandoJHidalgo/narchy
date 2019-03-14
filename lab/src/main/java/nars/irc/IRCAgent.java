package nars.irc;

import nars.experiment.Talk;
import nars.index.TermIndex;
import nars.inter.InterNAR;
import nars.nal.Tense;
import nars.nar.Terminal;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.time.RealtimeMSClock;
import nars.util.data.random.XorShift128PlusRandom;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static nars.experiment.Talk.context;

/**
 * Created by me on 7/10/16.
 */
public class IRCAgent extends IRCBot {
    private static final Logger logger = LoggerFactory.getLogger(IRCAgent.class);

    final Terminal nar = new Terminal(16, new XorShift128PlusRandom(1), new RealtimeMSClock());

    static boolean running = true;

    private final InterNAR inter;
    private final Talk talk;
    private float ircMessagePri = 0.9f;

    public IRCAgent(String server, String nick, String channel, int udpPort) throws Exception {
        super(server, nick, channel);

        talk = new Talk(nar);

        nar.log();

        nar.onExec(new TermProcedure("say") {
            @Override public @Nullable Object function(Compound arguments, TermIndex i) {
                Term content = arguments.term(0);
                String context = arguments.size() > 1 ? arguments.term(1).toString() : "";
                if (context.equals("I"))
                    send(channel, content.toString());

                return null;
            }
        });

        inter = new InterNAR(nar, (short)udpPort );
        inter.broadcastPriorityThreshold = 0.25f; //lower threshold

        nar.believe("connect(\"" + server + "\").", Tense.Present, 1f, 0.9f);
    }


    @Override
    protected void onMessage(String channel, String nick, String msg) {

//        nar.goal(
//                "say(\"" + msg + "\",(\"" + channel+ "\",\"" + nick + "\"))",
//                Tense.Present, 1f, 0.9f );
//
//    }
//
//    protected void onMessage(String channel, String nick, String msg) {
        if (channel.equals("unknown")) return;
        if (msg.startsWith("//")) return; //comment or previous output

//        if (msg.equals("RESET")) {
//            restart();
//        }
//        else if (msg.equals("WTF")) {
//            flush();
//        }
//        else {

        try {
            @NotNull List<Task> parsed = nar.tasks(msg.replace("http://","") /* HACK */, o -> {
                //logger.error("unparsed narsese {} in: {}", o, msg);
            });


            int narsese = parsed.size();
            if (narsese > 0) {
                for (Task t : parsed) {
                    logger.info("narsese({},{}): {}", channel, nick, t);
                }
                parsed.forEach(nar::input);
                return;
            }
        } catch (Exception e) { }

        logger.info("hear({},{}): {}", channel, nick, msg);
        talk.hear(msg, context(channel, nick), ircMessagePri);


    }
    public static void main(String[] args) throws Exception {
        //while (running) {
            try {
                new IRCAgent(
                        "irc.freenode.net",
                        //"localhost",
                        "NARchy", "#netention", 11001);
            } catch (Exception e) {
                e.printStackTrace();

            }
        //}
    }
}