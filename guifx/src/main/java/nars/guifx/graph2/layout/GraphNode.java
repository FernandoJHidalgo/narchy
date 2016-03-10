package nars.guifx.graph2.layout;

import javafx.scene.Group;
import nars.guifx.graph2.TermEdge;
import nars.util.data.Util;


public class GraphNode extends Group {

	/**
	 * cached from last set
	 */

	final static double SCALE_EPSILON = 0.05f;

	private double scaled = 0.0;
	private double tx = 0.0;
	private double ty = 0.0;
	private double px = Double.NaN, py = Double.NaN;

	public GraphNode() {
		setManaged(false);
		setPickOnBounds(true);
		//setVisible(false);
	}

	public boolean visible() {

		return /*isVisible() &&*/ getParent()!=null;
	}

	public void scale(double scale) {
		if (!Util.equals(this.scaled, scale, SCALE_EPSILON)) {
			scaled = scale;

			setScaleX(scale);
			setScaleY(scale);
		}

		//float conf = c != null ? c.getBeliefs().getConfidenceMax(0, 1) : 0;
            /*base.setFill(NARGraph.vis.get().getVertexColor(priNorm, conf));*/

		//setOpacity(0.75f + 0.25f * vertexScale);

		//System.out.println(scale + " " + vertexScale + " " + (int)(priorityDisplayedResolution * vertexScale));
	}


	public final void getPosition(double[] v) {
		v[0] = tx;
		v[1] = ty;
	}

	//Point2D sceneCoord;// = new Point2D(0,0);

	public void commit() {
		if (this.px!=tx)
			setTranslateX(this.px = tx);
		if (this.py!=ty)
			setTranslateY(this.py = ty);
	}

	public GraphNode move(double x, double y) {
		tx = x;
		ty = y;

		//sceneCoord = null;
		return this;
	}

	public void moveDelta(float dx, float dy) {
		move(x() + dx, y() + dy);
	}


	public final void move(double[] v, double speed, double threshold) {
		move(v[0], v[1], speed, threshold);
	}

	public final void move(double v0, double v1, double speed) {
		double px = tx;
		double py = ty;
		double momentum = 1.0f - speed;
		double nx = v0 * speed + px * momentum;
		double ny = v1 * speed + py * momentum;
		move(nx, ny);
	}

	public final void move(double v0, double v1, double speed, double threshold) {
		double px = tx;
		double py = ty;
		double momentum = 1.0f - speed;
		double nx = v0 * speed + px * momentum;
		double ny = v1 * speed + py * momentum;
		double dx = Math.abs(px - nx);
		double dy = Math.abs(py - ny);
		if ((dx > threshold) || (dy > threshold)) {
			move(nx, ny);
		}
	}

	public boolean move(double[] v, double threshold) {
		double x = tx;
		double y = ty;
		double nx = v[0];
		double ny = v[1];
		if (Math.abs(x - nx) < threshold && Math.abs(y - ny) < threshold) {
			return false;
		} else {
			move(nx, ny);
			return true;
		}
	}

	public final double width() {
		return scaled; //getScaleX();
	}

	public final double height() {
		return scaled; //getScaleY();
	}

//    public double sx() {
//        if (sceneCoord == null) sceneCoord = localToParent(0, 0);
//        return sceneCoord.getX();
//    }
//
//    public double sy() {
//        if (sceneCoord == null) sceneCoord = localToParent(0, 0);
//        return sceneCoord.getY();
//    }

	public double x() {
		return tx;
	}

	public double y() {
		return ty;
	}

	/** TODO generalize to EdgeNode's  */
	@Deprecated public TermEdge[] getEdges() {
		return TermEdge.empty;
	}
}
