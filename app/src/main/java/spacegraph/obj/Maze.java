package spacegraph.obj;

import nars.$;
import spacegraph.SimpleSpatial;
import spacegraph.math.v3;
import spacegraph.phys.Collidable;
import spacegraph.phys.Dynamic;
import spacegraph.phys.Dynamics;
import spacegraph.phys.constraint.TypedConstraint;
import spacegraph.phys.math.Transform;
import spacegraph.phys.shape.*;
import spacegraph.phys.util.Motion;

import java.nio.ByteBuffer;
import java.util.List;

import static spacegraph.math.v3.v;

/**
 * TODO extend CompoundSpatial
 */
public class Maze extends CompoundSpatial {


    boolean[][] cells;

    public Maze(String id, int x, int y) {
        super(id);
        cells = new boolean[x][y];
        build(0,0,x-1,y-1);
    }

    @Override
    protected void create(Dynamics world) {

        float dx = 1, dy = 1;
        float y = 0;
        for (boolean[] c : cells) {
            float x = 0;
            for (boolean cc : c) {


                if (cc) {


                    Dynamic b;
                    b = Dynamics.newBody(
                            1f, //mass
                            new BoxShape(0.9f, 0.9f, 0.9f), new Motion(),
                            +1, //group
                            -1//collidesWithOthersLikeThis ? -1 : -1 & ~(+1) //exclude collisions with self
                    );
                    b.setCenterOfMassTransform(new Transform(x, y, 0));
                    b.setRenderer(this);
                    b.setData(this);

                    //b.setLinearFactor(1,1,0); //restricts movement to a 2D plane


                    b.setDamping(0.9f, 0.5f);
                    b.setFriction(0.9f);

                    add(b);
                }

                x+=dx;

            }
            y+=dy;
        }



        CollisionShape groundShape = new BoxShape(v(20f, 20f, 10f));
        Dynamic ground = Dynamics.newBody(0f, groundShape, new Motion(), +1, -1);
        ground.setCenterOfMassTransform(new Transform(0, 0, -15f));
        ground.setRenderer(this);
        ground.setData(this);
        add(ground);


        /*ConvexShape blobShape = new BvhTriangleMeshShape(
                new TriangleIndexVertexArray(), true
        );*/
        //ConvexShape blobShape =new TetrahedronShapeEx(v(-10,0,0), v(10, 0, 10), v(10, -10, 10), v(-10, -10, 10));
        //CollisionShape blobShape = terrain(5, 0.25f, 1, v(5,5,5));


//        ConvexHullShape blobShape = hull();
//        Dynamic blob = Dynamics.newBody(4f, blobShape, new Motion(), +1, -1);
//        blob.setCenterOfMassTransform(new Transform(0, 0, 15f));
//        l.add(blob);


//
//        Collections.addAll( l,  new RagDoll().build(world, v(0,0,20), 3f) );
    }


    public ConvexHullShape hull() {
        ConvexHullShape c = new ConvexHullShape().add(
            v(-3,0,0), v(-3, 0, 3), v(-3, -3, 3), v(-3, 3, 3),
            v(0, 3, 1.5f)
        );

        return c;
    }

    protected CollisionShape terrain(int tesselation, float height, int seed, v3 scale) {


        int count, countsq, countsqp, indCount = 0, nsscroll;

        count = tesselation;
        countsq = count * count;
        countsqp = countsq + (count * 2 + 1);

        nsscroll = seed;
        v3[] pvert = new v3[countsqp];

        ByteBuffer ind, vert;

        float hdim =  count * .5f;
        float nscl = .09f;
        float zscl = 7.5f;
        ind = ByteBuffer.allocateDirect(countsq * 24);
        vert = ByteBuffer.allocateDirect(countsqp * 12);
        for (int a = 0; a < countsqp; a++) {
            int xi = a % (count + 1);
            int yi = (int) Math.floor(a / (count + 1));
            float xx = xi - hdim;
            float zz = yi  - hdim;
            float yy = 0;
            yy += noise((nsscroll + xx + 1) * nscl, zz * nscl);
            yy += noise((nsscroll + xx + 1) * nscl, (zz + 1) * nscl);
            yy += noise((nsscroll + xx - 1) * nscl, zz * nscl);
            yy += noise((nsscroll + xx) * nscl, (zz - 1) * nscl);
            yy *= height * zscl;
            if (xi == 0 || xi == count || yi == 0 || yi == count)
                yy -= zscl * .2;
            pvert[a] = new v3(xx, yy, zz);

            vert.putFloat(xx);
            vert.putFloat(yy);
            vert.putFloat(zz);
            if (a != 0 && (xi == count || a > countsq + (count - 2))) {
                continue;
            }
            indCount += 2;
            ind.putInt(a);
            ind.putInt(a + 1);
            ind.putInt(a + count + 1);
            ind.putInt(a + 1);
            ind.putInt(a + count + 2);
            ind.putInt(a + count + 1);
        }
        TriangleIndexVertexArray tiva = new TriangleIndexVertexArray(indCount, ind, 12, countsq, vert, 12);

        BvhTriangleMeshShape cs = new BvhTriangleMeshShape(tiva, true);
        cs.setLocalScaling(scale);
        return cs;
    }

    private float noise(float a, float b) {
        //TODO perlin noise
        return (float)Math.random();
    }

    public static int irand(int x) {
        return (int)(Math.random() * x);
    }

        public void build(int x1, int y1, int x2, int y2) {

            for (int x = x1; x < x2; x++)
                for (int y = y1; y < y2; y++)
                    cells[x][y] = true;

            int w = x2 - x1 + 1;
            int rw = (w + 1) / 2;
            int h = y2 - y1 + 1;
            int rh = (h + 1) / 2;

            int sx = x1 + 2 * irand(rw);
            int sy = y1 + 2 * irand(rh);
            cells[sx][sy] = false; //start point

            int finishedCount = 0;
            for (int i = 1; (i < (rw * rh * 1000)) && (finishedCount < (rw * rh)); i++) {
                int x = x1 + 2 * irand(rw);
                int y = y1 + 2 * irand(rh);
                if (cells[x][y] != true)
                    continue;

                int dx = (irand(2) == 1) ? (irand(2) * 2 - 1) : 0;
                int dy = (dx == 0) ? (irand(2) * 2 - 1) : 0;
                int lx = x + dx * 2;
                int ly = y + dy * 2;
                if ((lx >= x1) && (lx <= x2) && (ly >= y1) && (ly <= y2)) {
                    if (!cells[lx][ly]) {
                        cells[x][y] = false;
                        cells[x+dx][y+dy] = false;
                        finishedCount++;
                    }
                }
            }
        }


}
