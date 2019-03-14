package spacegraph.test;

//=================================================================================
// Picking 0.2                                                       (Thomas Bladh)
//=================================================================================
// A simple picking example using java/jogl. This is far from a complete solution 
// but it should give you an idea of how to include picking in your assigment 
// solutions.
//
// Notes: * Based on example 13-3 (p 542) in the "OpenGL Programming Guide"
//        * This version should handle overlapping objects correctly.
//---------------------------------------------------------------------------------

import com.jogamp.common.nio.Buffers;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.glu.GLU;
import spacegraph.render.JoglSpace;

import java.nio.IntBuffer;


public class Picking extends JoglSpace implements MouseListener {
    public static void main(String[] args) {
        new Picking().show(800, 800);
    }

    Picking() {
        super();


        addMouseListener(this);
        //addMouseMotionListener(this);
    }

    static final int NOTHING = 0, UPDATE = 1, SELECT = 2;
    int cmd = UPDATE;
    int mouse_x, mouse_y;

    private final GLU glu = new GLU();

    @Override
    public void init(GL2 gl) {

        gl.glEnable(GL2.GL_CULL_FACE);
        gl.glEnable(GL2.GL_DEPTH_TEST);
        gl.glEnable(GL2.GL_NORMALIZE);
        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glViewport(0, 0, width, height);
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        glu.gluOrtho2D(0.0f, 1.0f, 0.0f, 1.0f);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        switch (cmd) {
            case UPDATE:
                drawScene(gl);
                break;
            case SELECT:
                int buffsize = 512;
                double x = mouse_x, y = mouse_y;
                int[] viewPort = new int[4];
                IntBuffer selectBuffer = Buffers.newDirectIntBuffer(buffsize);
                int hits = 0;
                gl.glGetIntegerv(GL2.GL_VIEWPORT, viewPort, 0);
                gl.glSelectBuffer(buffsize, selectBuffer);
                gl.glRenderMode(GL2.GL_SELECT);
                gl.glInitNames();
                gl.glMatrixMode(GL2.GL_PROJECTION);
                gl.glPushMatrix();
                gl.glLoadIdentity();
                glu.gluPickMatrix(x, viewPort[3] - y, 5.0d, 5.0d, viewPort, 0);
                glu.gluOrtho2D(0.0d, 1.0d, 0.0d, 1.0d);
                drawScene(gl);
                gl.glMatrixMode(GL2.GL_PROJECTION);
                gl.glPopMatrix();
                gl.glFlush();
                hits = gl.glRenderMode(GL2.GL_RENDER);
                processHits(hits, selectBuffer);
                cmd = UPDATE;
                break;
        }
    }

    public static void processHits(int hits, IntBuffer buffer) {
        System.out.println("---------------------------------");
        System.out.println(" HITS: " + hits);
        int offset = 0;
        int names;
        float z1, z2;
        for (int i = 0; i < hits; i++) {
            System.out.println("- - - - - - - - - - - -");
            System.out.println(" hit: " + (i + 1));
            names = buffer.get(offset);
            offset++;
            z1 = (float) (buffer.get(offset) & 0xffffffffL) / 0x7fffffff;
            offset++;
            z2 = (float) (buffer.get(offset) & 0xffffffffL) / 0x7fffffff;
            offset++;
            System.out.println(" number of names: " + names);
            System.out.println(" z1: " + z1);
            System.out.println(" z2: " + z2);
            System.out.println(" names: ");

            for (int j = 0; j < names; j++) {
                System.out.print("       " + buffer.get(offset));
                if (j == (names - 1))
                    System.out.println("<-");
                else
                    System.out.println();
                offset++;
            }
            System.out.println("- - - - - - - - - - - -");
        }
        System.out.println("---------------------------------");
    }

    public static int viewPortWidth(GL2 gl) {
        int[] viewPort = new int[4];
        gl.glGetIntegerv(GL2.GL_VIEWPORT, viewPort, 0);
        return viewPort[2];
    }

    public static int viewPortHeight(GL2 gl) {
        int[] viewPort = new int[4];
        gl.glGetIntegerv(GL2.GL_VIEWPORT, viewPort, 0);
        return viewPort[3];
    }

    public void drawScene(GL2 gl) {
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

        // Colors
        float red[] = {1.0f, 0.0f, 0.0f, 1.0f};
        float green[] = {0.0f, 1.0f, 0.0f, 1.0f};
        float blue[] = {0.0f, 0.0f, 1.0f, 1.0f};

        // Red rectangle
        GLRectangleEntity r1 = new GLRectangleEntity(gl, glu);
        r1.x = 0.15f;
        r1.y = 0.25f;
        r1.z = 0.75f;
        r1.w = 0.4f;
        r1.h = 0.4f;
        r1.c = red;
        r1.id = 10;
        r1.draw();

        // Green rectangle
        GLRectangleEntity r2 = new GLRectangleEntity(gl, glu);
        r2.x = 0.35f;
        r2.y = 0.45f;
        r2.z = 0.5f;
        r2.w = 0.4f;
        r2.h = 0.4f;
        r2.c = green;
        r2.id = 20;
        r2.draw();

        // Blue rectangle
        GLRectangleEntity r3 = new GLRectangleEntity(gl, glu);
        r3.x = 0.45f;
        r3.y = 0.15f;
        r3.z = 0.25f;
        r3.w = 0.4f;
        r3.h = 0.4f;
        r3.c = blue;
        r3.id = 30;
        r3.draw();

        gl.glFlush();
    }

    public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
    }

    @Override
    public void mouseWheelMoved(MouseEvent mouseEvent) {

    }

    @Override
    public void mousePressed(MouseEvent e) {
        cmd = SELECT;
        mouse_x = e.getX();
        mouse_y = e.getY();
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseDragged(MouseEvent e) {
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    public abstract static class GLEntity {
        float x, y, z;
        float[] c;
        int id;
        boolean outline;
        GL2 gl;
        GLU glu;

        public GLEntity(GL2 gl, GLU glu) {
            this.gl = gl;
            this.glu = glu;
        }

        public void draw() {
            gl.glPushName(id);
            _draw();
        }

        public abstract void _draw();
    }

    public static class GLRectangleEntity extends GLEntity {
        float w = 0.1f;
        float h = 0.1f;

        public GLRectangleEntity(GL2 gl, GLU glu) {
            super(gl, glu);
        }

        @Override
        public void _draw() {
            if (outline)
                gl.glPolygonMode(GL2.GL_FRONT, GL2.GL_LINE);
            else
                gl.glPolygonMode(GL2.GL_FRONT, GL2.GL_FILL);

            gl.glColor4fv(c, 0);
            gl.glBegin(GL2.GL_POLYGON);
            gl.glVertex3f(x, y, z);
            gl.glVertex3f(x + w, y, z);
            gl.glVertex3f(x + w, y + h, z);
            gl.glVertex3f(x, y + h, z);
            gl.glEnd();
        }
    }
}