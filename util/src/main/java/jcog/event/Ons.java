package jcog.event;

import java.util.Collections;

/**
 * essentially holds a list of registrations but forms an activity context
 * from the dynamics of its event reactivity
 */
public class Ons extends jcog.list.FastCoWList<Off> {

    Ons(int capacity) {
        super(capacity, Off[]::new);
    }

    Ons() {
        this(1);
    }

    public Ons(Off... r) {
        this(r.length);
        Collections.addAll(this, r);
    }


    public void off() {
        for (int i = 0; i < size(); i++) {
            get(i).off();
        }
        clear();
    }







}
