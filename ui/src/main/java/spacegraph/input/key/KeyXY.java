package spacegraph.input.key;

import com.jogamp.newt.event.KeyEvent;
import spacegraph.video.JoglSpace;

/**
 * Created by me on 11/20/16.
 */
public class KeyXY extends SpaceKeys {
    protected float speed = 64f;

    public KeyXY(JoglSpace g) {
        super(g);


        watch(KeyEvent.VK_NUMPAD4, (dt)-> {
            moveX(speed);
        }, null);
        watch(KeyEvent.VK_NUMPAD6, (dt)-> {
            moveX(-speed);
        }, null);

        watch(KeyEvent.VK_NUMPAD8, (dt)-> {
            moveY(speed);
        }, null);
        watch(KeyEvent.VK_NUMPAD2, (dt)-> {
            moveY(-speed);
        }, null);


        watch(KeyEvent.VK_NUMPAD5, (dt)-> {
            moveZ(speed);
        }, null);
        watch(KeyEvent.VK_NUMPAD0, (dt)-> {
            moveZ(-speed);
        }, null);

    }

    void moveX(float speed) {
        space.camPos.add(speed, 0, 0);
    }

    void moveY(float speed) {
        space.camPos.add(0, speed, 0);
    }

    void moveZ(float speed) {
        space.camPos.add(0, 0, speed);
    }

}
