package spacegraph.input;

/** exclusive finger control state which a surface can activate in reaction to input events.
 * it has a specified termination condition (ex: button release) and
 * while active, locks any other surface from receiving interaction events.
 *
 * it can be used to drag move/resize with additional functionality or constraints
 * applied in real-time.
 * */
abstract public class Fingering {

    abstract public void start(Finger f);

    /** return false to finish */
    abstract public boolean update(Finger f);

}
