package spacegraph.space2d.dyn2d.fracture;

import spacegraph.space2d.dyn2d.ICase;
import spacegraph.space2d.phys.collision.shapes.PolygonShape;
import spacegraph.space2d.phys.dynamics.*;
import spacegraph.space2d.phys.fracture.materials.Diffusion;
import spacegraph.util.math.v2;

/**
 * Testovaci scenar
 *
 * @author Marek Benovic
 */
public class RotatedBody implements ICase {
    @Override
    public void init(Dynamics2D w) {
        {
            BodyDef bodyDef2 = new BodyDef();
            bodyDef2.type = BodyType.DYNAMIC;
            bodyDef2.position.set(10.0f, 20.0f); //pozicia
            bodyDef2.linearVelocity = new v2(0.0f, 0.0f); // smer pohybu
            bodyDef2.angularVelocity = 10.0f; //rotacia (rychlost rotacie)
            Body2D newBody = w.addBody(bodyDef2);
            PolygonShape shape3 = new PolygonShape();
            shape3.setAsBox(1.0f, 10.0f);

            Fixture f = newBody.addFixture(shape3, 1.0f);
            f.friction = 0.2f; // trenie
            f.restitution = 0.0f; //odrazivost
            f.material = new Diffusion();
            f.material.m_rigidity = 32.0f;
        }
    }

    @Override
    public String toString() {
        return "Rotated body";
    }
}
