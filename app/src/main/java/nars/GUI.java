package nars;

import jcog.User;
import jcog.exe.Loop;
import jcog.list.FasterList;
import nars.gui.Vis;
import org.apache.lucene.document.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spacegraph.SpaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Container;
import spacegraph.space2d.container.MutableContainer;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.meta.OmniBox;
import spacegraph.space2d.widget.meta.ServicesTable;
import spacegraph.space2d.widget.windo.Dyn2DSurface;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

/**
 * main UI entry point
 */
public class GUI {

    static final Logger logger = LoggerFactory.getLogger(GUI.class);


    public static void main(String[] args) {

        Dyn2DSurface w = SpaceGraph.wall(800, 600);

        NAR nar = NARchy.ui();

        Loop loop = nar.startFPS(10f); //10hz alpha
        //((NARLoop) loop).throttle.set(0.1f);


        //1. try to open a Spacegraph openGL window
        logger.info("start SpaceGraph UI");

        //            window(new ConsoleTerminal(new TextUI(nar).session(8f)) {
        //                {
        //                    Util.pause(50); term.addInput(KeyStroke.fromString("<pageup>")); //HACK trigger redraw
        //                }
        //            }, 800, 600);


        //Loop.invokeLater(()->{
        //((ZoomOrtho) w.root()).scaleMin = 100f;


        w.frame(new ServicesTable(nar.services), 5, 4);
        w.frame(new OmniBox(new LuceneQueryModel()), 6, 1);
        w.frame(Vis.top(nar), 4, 4);



        //nar.inputNarsese(new FileInputStream("/home/me/d/sumo_merge.nal"));

//        try {
//            Thread.currentThread().join();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
    }

    /** TODO further abstract this as the prototype for other async models */
    static class LuceneQueryModel extends OmniBox.Model {

        private final User user;

        public LuceneQueryModel() {
            this(User.the());
        }
        public LuceneQueryModel(User u) {
            super();
            this.user = u;
        }

        private final AtomicReference<Querying> query = new AtomicReference<>(null);

        final class Querying implements Predicate<User.DocObj>, Runnable {


            public final String q;
            final List<Result> results = new FasterList();
            private final MutableContainer target;

            Querying(String text, MutableContainer target) {
                this.q = text;
                this.target = target;
            }

            public Querying start() {
                if (query.get() == this) {
                    //System.out.println("query start: " + q);
                    user.run(this);
                }
                return this;
            }

            @Override
            public boolean test(User.DocObj docObj) {
                //System.out.println(q + ": " + docObj);
                if (query.get()!=this)
                    return false;
                else {
                    Document d = docObj.doc();
                    Result r = new Result(d);
                    Surface s = result(r);
                    if (query.get()==this) {
                        results.add(r);
                        target.add(s);
                        return true;
                    } else {
                        return false;
                    }
                }
            }

            @Override
            public void run() {
                if (query.get() != this)
                    return;

                target.clear();
                user.get(q, 16, this);
            }



            private Surface result(Result r) {
                return new PushButton(r.id);
            }

            void clear() {
                results.clear();
            }
        }

        class Result {
            public final String id;
            public final String type;
            final Document doc;
            //icon

            Result(Document doc) {
                this.doc = doc;
                this.id = doc.get("i");
                switch (this.type = doc.get("c")) {
                    case "blob":
                        //
                        break;
                }
//            System.out.println(id);
//            d.forEach(f -> {
//                System.out.println(f.name() + " " + f.fieldType());
//            });
            }

            Object get() {
                return user.undocument(doc);
            }

        }

        @Override
        public void onTextChange(String next, MutableContainer target) {
            Querying prev = null;
            if (next.isEmpty()) {
                prev = query.getAndSet(null);
            } else {

                Querying q = query.get();
                if (q == null || !q.q.equals(next)) {
                    Querying qq = new Querying(next, target);
                    prev = query.getAndSet(qq);
                    qq.start();
                }
            }
            if (prev!=null)
                prev.clear();

        }
    }
}
