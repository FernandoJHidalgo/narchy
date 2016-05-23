package nars.learn.ql;

import nars.data.Range;
import nars.learn.Agent;
import nars.util.data.random.XorShift128PlusRandom;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

/**
 * q-learning + SOM agent, cognitive prosthetic. designed by patham9
 */
abstract public class HaiQ implements Agent {

	@NotNull
	public final Random rng;

	//@NotNull
	//final Hsom som;

	@NotNull
	public float[][] q; // state x action
	@NotNull
	public float[][] et;

	int actions;
	int lastState, lastDecidedAction;

	/*
	 * http://stackoverflow.com/questions/1854659/alpha-and-gamma-parameters-in-
	 * qlearning Alpha is the learning rate. If the reward or transition
	 * function is stochastic (random), then alpha should change over time,
	 * approaching zero at infinity. This has to do with approximating the
	 * expected outcome of a inner product (T(transition)*R(reward)), when one
	 * of the two, or both, have random behavior.
	 * 
	 * Gamma is the value of future reward. It can affect learning quite a bit,
	 * and can be a dynamic or static value. If it is equal to one, the agent
	 * values future reward JUST AS MUCH as current reward. This means, in ten
	 * actions, if an agent does something good this is JUST AS VALUABLE as
	 * doing this action directly. So learning doesn't work at that well at high
	 * gamma values. Conversely, a gamma of zero will cause the agent to only
	 * value immediate rewards, which only works with very detailed reward
	 * functions.
	 * 
	 * http://web.eecs.utk.edu/~asaites/project-pdfs/sarsalambda.pdf Lambda is
	 * the rate of decay (in conjunction with gamma) of the eligibility trace.
	 * This is the amount by which the eligibility of a state is reduced each
	 * time step that it is not being visited. A low lambda causes a lower
	 * reward to propagate back to states farther from the goal. While this can
	 * prevent the reinforcement of a path which is not optimal, it causes a
	 * state which is far from the goal to receive very little reward. This
	 * slows down convergence, because the agent spends more time searching for
	 * a path if it starts far from the goal. Conversely, a high lambda allows
	 * more of the path to be updated with higher rewards. This suited our
	 * implementation, because our high initial epsilon was able to correct any
	 * state values which might have been incorrectly reinforced and create a
	 * more defined path to the goal in fewer episodes. Because of this, we
	 * chose a final lambda of 0.9.
	 */
	public float Gamma, Lambda;

	@Range(min=0, max=1f)
	public final MutableFloat Alpha = new MutableFloat();

	private int inputs;
	public float Epsilon;

	public HaiQ() {
		rng = new XorShift128PlusRandom(1);
	}

	int learn(int state, float reward) {
		return learn(state, reward, 1f, true);
	}

	int learn(int state, float reward, float confidence, boolean decide) {

		// 1. decide next action
		int action = decide ? nextAction(state) : -1;

		// 2. learn
		int lastAction = lastAction();
		final int lastState = this.lastState;
		if (lastAction != -1) {

			float DeltaQ = (reward + (Gamma * q[state][action]))
					- q[lastState][lastAction];
			et[lastState][lastAction] += confidence;

			// 3. update
			update(DeltaQ);
		}

		if (decide) {
			this.lastState = state;
			this.lastDecidedAction = action;
		}

		return action;
	}

	protected int nextAction(int state) {
		return rng.nextFloat() < Epsilon ? randomAction() : choose(state);
	}

	private int randomAction() {
		return rng.nextInt(actions);
	}

	private void update(float deltaQ) {

		float[][] q = this.q;
		float[][] et = this.et;

		float alphaDelta = Alpha.floatValue() * deltaQ;
		float gammaLambda = Gamma * Lambda;


		for (int i = 0; i < inputs; i++) {
			float[] eti = et[i];
			float[] qi = q[i];

			for (int k = 0; k < actions; k++) {
				qi[k] += alphaDelta * eti[k];
				eti[k] *= gammaLambda;
			}
		}
	}

	protected int choose(int state) {
		int maxk = -1;
		float maxval = Float.NEGATIVE_INFINITY;

		float[] qs = q[state];
		for (int k = 0; k < actions; k++) {
			float qq = qs[k];
			// TODO determine if q is significant enough above threshold to
			// count
			if (qq > maxval) {
				maxk = k;
				maxval = qq;
			}
		}

		return maxk != -1 ? maxk : randomAction();
	}

	public void start(int inputs, int outputs) {

		this.actions = outputs;
		this.inputs = inputs;

		//som = new Hsom(inputs, states);

		q = new float[inputs][outputs];
		et = new float[inputs][outputs];

		setQ(0.05f, 0.5f, 0.9f, 0.02f); // 0.1 0.5 0.9
	}

	public void setQ(float alpha, float gamma, float lambda, float epsilon) {
		Alpha.setValue( alpha );
		Gamma = gamma;
		Lambda = lambda;
		Epsilon = epsilon;
	}

	/**
	 * main control function
	 */
	public final int act(float reward, float[] input) {
		return learn(perceive(input), reward);
	}

	abstract protected int perceive(float[] input);

	/**
	 * TODO make abstract
	 */
	/*protected int perceive(float[] input) {
		som.learn(input);
		return som.winnerx + (som.winnery * som.SomSize);
	}*/

	protected int lastAction() {
		return lastDecidedAction;
	}

	public int actions() {
		return actions;
	}

	public int inputs() {
		return inputs;
	}

	// void hsom_DrawSOM(Hsom somobj,int RenderSize,int x,int y,boolean
	// bSpecial,int specialIndex)
	// {
	// fill(0);
	// pushMatrix();
	// translate(x,y);
	// float[][] input = new float[somobj.SomSize][somobj.SomSize];
	// somobj.GetActivationForRendering(input,bSpecial,specialIndex);
	// hamlib.Draw2DPlane(input,RenderSize);
	// fill(255);
	// rect(somobj.winnerx*RenderSize,somobj.winnery*RenderSize,RenderSize,RenderSize);
	// popMatrix();
	// }

	// public void keyPressed()
	// {
	// if(key=='t')
	// {
	// test.v=1.0f;
	// }
	// if(key=='g')
	// {
	// test.v=-1.0f;
	// }
	// if(key=='f')
	// {
	// test.a+=0.2f;
	// }
	// if(key=='h')
	// {
	// test.a-=0.2;
	// }
	// hamlib.keyPressed();
	// }
	// public void mouseMoved()
	// {
	// hamlib.mouseMoved();
	// }
	// public void mouseReleased()
	// {
	// hamlib.mouseReleased();
	// }
	// public void mouseDragged()
	// {
	// hamlib.mouseDragged();
	// }
	// public void mousePressed()
	// {
	// hamlib.mousePressed();
	// }
	// void hsim_Draw(Obj o)
	// {
	// image(im[o.type],-o.s/2,-o.s/2,o.s,o.s);
	// }
	// int goods=1,bads=1;
	// void hsim_Interact(Obj i,Obj j)
	// {
	// if(i.type==0 && j.type==1)
	// {
	// i.acc=1.0f;
	// goods++;
	// }
	// if(i.type==0 && j.type==2)
	// {
	// i.acc=-1.0f;
	// bads++;
	// }
	// if(j.type==1 || j.type==2)
	// {
	// j.x=random(1)*width;
	// j.y=random(1)*height;
	// }
	// }
	// int Hsim_eyesize=3; //9
	// float[] viewField=new float[Hsim_eyesize*2];
	// void hsim_ObjectTask(Obj oi)
	// {
	// oi.v=0;
	// if(oi.type==2)
	// {
	// if(random(1)>0.5)
	// {
	// // mem.ProcessingInteract(oi.x,oi.y,1.0,2.0);
	// }
	// oi.a+=0.05f;
	// }
	// if(oi.hai!=null)
	// {
	// for(int i=0;i<viewField.length;i++)
	// {
	// viewField[i]=0;
	// }
	// int maxIndex=hamlib.MinMaxFrom(oi.visarea).MaxIndex;
	// boolean Had=false;
	// for(int i=0;i<oi.visarea.length;i++)
	// {
	// if(i==maxIndex)
	// {
	// if(oi.visareatype[i]==2)
	// {
	// viewField[i]=oi.visarea[i];
	// Had=true;
	// }
	// if(oi.visareatype[i]==3)
	// {
	// viewField[i+Hsim_eyesize]=oi.visarea[i];
	// Had=true;
	// }
	// }
	// }
	// int action=oi.hai.UpdateSOM(viewField,oi.acc);
	// if(!Had)
	// {
	// action=0;
	// }
	// if(action==2)
	// {
	// oi.a+=0.5f;
	// oi.v=10.0f;
	// //mem.ProcessingInteract(oi.x,oi.y,1.0,10.0);
	// }
	// else
	// if(action==1)
	// {
	// oi.a-=0.5f;
	// oi.v=10.0f;
	// // mem.ProcessingInteract(oi.x,oi.y,1.0,10.0);
	// }
	// else
	// if(action==0)
	// {
	// oi.v=10.0f;
	// // mem.ProcessingInteract(oi.x,oi.y,1.0,10.0);
	// }
	// if(oi.x>width)
	// {
	// oi.x=0;
	// }
	// if(oi.x<0)
	// {
	// oi.x=width;
	// }
	// if(oi.y>height)
	// {
	// oi.y=0;
	// }
	// if(oi.y<0)
	// {
	// oi.y=height;
	// }
	// oi.acc=0.0f;
	// }
	// }
	// Obj lastclicked=null;
	// void hsim_ElemClicked(Obj i)
	// {
	// if(lastclicked!=null)
	// {
	// lastclicked.DrawField=false;
	// }
	// lastclicked=i;
	// }
	// void hsim_ElemDragged(Obj i)
	// {
	// // mem.ProcessingInteract(i.x,i.y,1.0,3.0);
	// }
	// void hrend_DrawGUI()
	// {
	// fill(0);
	// text("viewfield and RF-Rewards:",20,20);
	// //test.DrawViewFields(20,30,10);
	// test.hai.Draw(20,30,2);
	// }
	// void hrend_DrawBegin()
	// {
	// label1.text="opti index:"+((float)goods)/((float)bads)+ "FPS:"+frameRate;
	// fill(138,138,128);
	// pushMatrix();
	// if(hamlib.Mode==hamlib.Hamlib3DMode)
	// {
	// translate(0,0,-2);
	// }
	// rect(0,0,width,height);
	// popMatrix();
	// //mem.DrawForProcessing(0.0f,0.0f,0.0f,0.01f,true);
	// }
	//
	// void hrend_DrawEnd()
	// {
	// fill(0);
	// text("Hamlib simulation system demonstration",0,-5);
	// stroke(255,255,255);
	// line(0,0,width,0);
	// line(width,height,width,0);
	// line(width,height,0,height);
	// line(0,0,0,height);
	// noStroke();
	// if(lastclicked!=null)
	// {
	// fill(255,0,0);
	// rect(lastclicked.x,-20,5,20);
	// rect(-20,lastclicked.y,20,5);
	// rect(lastclicked.x,height+20,5,-20);
	// rect(width+20,lastclicked.y,-20,5);
	// lastclicked.DrawField=true;
	// pushMatrix();
	// if(hamlib.Mode==hamlib.Hamlib3DMode)
	// {
	// translate(0,0,1);
	// }
	// ellipse(lastclicked.x,lastclicked.y,10,10);
	// popMatrix();
	// }
	// }
	// void hgui_ElemEvent(Gui i)
	// {
	// }
	// class Hsim
	// {
	// Hsim(){}
	// ArrayList obj=new ArrayList();
	// void Init()
	// {
	// smooth();
	// hcam.zpos=100;
	// }
	// void mousePressed()
	// {
	// if(mouseButton==LEFT)
	// {
	// checkSelect();
	// }
	// }
	// boolean dragged=false;
	// void mouseDragged()
	// {
	// if(mouseButton==LEFT)
	// {
	// dragged=true;
	// dragElems();
	// }
	// }
	// void mouseReleased()
	// {
	// dragged=false;
	// selected=null;
	// }
	// Obj selected=null;
	// void dragElems()
	// {
	// if(dragged && selected!=null)
	// {
	// selected.x=hnav.MouseToWorldCoordX(mouseX);
	// selected.y=hnav.MouseToWorldCoordY(mouseY);
	// hsim_ElemDragged(selected);
	// }
	// }
	// void checkSelect()
	// {
	// if(selected==null)
	// {
	// for(int i=0;i<obj.size();i++)
	// {
	// Obj oi=(Obj)obj.get(i);
	// float dx=oi.x-hnav.MouseToWorldCoordX(mouseX);
	// float dy=oi.y-hnav.MouseToWorldCoordY(mouseY);
	// float distance=sqrt(dx*dx+dy*dy);
	// if(distance<oi.s)
	// {
	// selected=oi;
	// hsim_ElemClicked(oi);
	// return;
	// }
	// }
	// }
	// }
	// float Cursor3DWidth=20;
	// void DrawCursor(float x, float y)
	// {
	// fill(0);
	// stroke(255);
	// ellipse(x,y,Cursor3DWidth,Cursor3DWidth);
	// noStroke();
	// }
	// float visarea=PI/3;
	// float viewdist=100.0f;
	//
	// void Simulate()
	// {
	// for(int i=0;i<obj.size();i++)
	// {
	// Obj oi=((Obj)obj.get(i));
	// oi.a=hamlib.RadAngleRange(oi.a);
	// int Hsim_eyesize=oi.visarea.length;
	// for(int k=0;k<Hsim_eyesize;k++)
	// {
	// oi.visarea[k]=0;
	// oi.visareatype[k]=0;
	// }
	// for(int j=0;j<obj.size();j++)
	// {
	// Obj oj=((Obj)obj.get(j));
	// if(i!=j)
	// {
	// float dx=oi.x-oj.x;
	// float dy=oi.y-oj.y;
	// float d=sqrt(dx*dx+dy*dy);
	// if(oi.type==0)
	// {
	// float ati=atan2(dy,dx)+PI;
	// float diffi=hamlib.angleDiff(ati,oi.a);
	// float diffi2=hamlib.angleDiff(ati,oi.a-visarea);
	// float part=diffi/visarea;
	// float part2=diffi2/(visarea*2);
	// if(part<1.0 && d<viewdist)
	// {
	// int index=min(Hsim_eyesize-1,max(0,(int)(part2*((float)Hsim_eyesize))));
	// oi.visarea[index]=1.0f-d/viewdist;
	// oi.visareatype[index]=oj.type+1;
	// }
	// }
	// if(d<(oi.s+oj.s)/2.0)
	// {
	// hsim_Interact(oi,oj);
	// }
	// }
	// }
	// hsim_ObjectTask(oi);
	// float a=oi.a;
	// float cosa=cos(a);
	// float sina=sin(a);
	// oi.x+=cosa*oi.v;
	// oi.y+=sina*oi.v;
	// oi.x+=oi.vx;
	// oi.y+=oi.vy;
	//
	// if(oi.DrawField==true && oi.type==0)
	// {
	// stroke(255,0,0);
	// pushMatrix();
	// if(hamlib.Mode==hamlib.Hamlib3DMode)
	// {
	// translate(0,0, (float) 1.5);
	// }
	// line(oi.x,oi.y,oi.x+viewdist*cos(a+visarea),oi.y+viewdist*sin(a+visarea));
	// line(oi.x,oi.y,oi.x+viewdist*cos(a-visarea),oi.y+viewdist*sin(a-visarea));
	// popMatrix();
	// noStroke();
	// }
	// fill(255,0,0);
	// pushMatrix();
	// translate(oi.x,oi.y);
	// rotate(a+PI);
	// hsim_Draw(oi);
	// popMatrix();
	// }
	// if(hamlib.Mode)
	// {
	// pushMatrix();
	// translate(0,0,-1);
	// DrawCursor(hnav.MouseToWorldCoordX(mouseX),hnav.MouseToWorldCoordY(mouseY));
	// popMatrix();
	// }
	// }
	// void DrawViewField(Obj o,int x,int y)
	// {
	// if(o.type==0)
	// {
	// for(int i=0;i<o.visarea.length;i++)
	// {
	// fill(o.visarea[i]*255);
	// rect(10*i+x,10+y,10,10);
	// fill(o.visareatype[i]/hamlib.MinMaxFrom(o.visarea).MaxValue*255.0f);
	// rect(10*i+x,20+y,10,10);
	// }
	// }
	// }
	// }
	// Hsim hsim=new Hsim();
	//
	// class Hsim_Custom
	// {
	// Hsim_Custom(){}
	// }
	//
	// int nactions=3;
	// int worldSize=800;
	// PImage[] im=new PImage[10];
	// Gui label1;
	// Obj test;
	// //WaveMembran mem=new WaveMembran(100);
	//
	//
	//
	// class Hamlib
	// {
	// Hamlib(){}
	// void FillDependendOnVal(float Val)
	// {
	// fill(128,0,128+Val*128);
	// }
	// void Draw2DPlane(float[][] input,int RenderSize)
	// {
	// for(int i=0;i<input.length;i++)
	// {
	// for(int j=0;j<input[i].length;j++)
	// {
	// FillDependendOnVal(input[i][j]);
	// rect(i*RenderSize,j*RenderSize,RenderSize,RenderSize);
	// }
	// }
	// }
	// void Draw1DLine(float[] input,int RenderSize)
	// {
	// for(int i=0;i<input.length;i++)
	// {
	// FillDependendOnVal(input[i]);
	// rect(i*RenderSize,0,RenderSize,RenderSize);
	// }
	// }
	// void farrcpy(float[] a,float[] b,int sz)
	// {
	// for(int i=0;i<sz;i++)
	// {
	// a[i]=b[i];
	// }
	// }
	// float angleDiff(float a,float b)
	// {
	// return min(abs(a-b),2*PI-abs((a-b)));
	// }
	// float deg(float radval)
	// {
	// return radval/(2*PI)*360;
	// }
	// float rad(float degval)
	// {
	// return degval/360*2*PI;
	// }
	// float RadAngleRange(float ret)
	// { //spuckt zwischen 0 und 2*PI aus
	// if(ret>2*PI)
	// {
	// ret-=2*PI;
	// }
	// if(ret<0)
	// {
	// ret+=2*PI;
	// }
	// return ret;
	// }
	//
	// MinMaxClass MinMaxFrom(float[] arr)
	// {
	// MinMaxClass ret=new MinMaxClass();
	// for(int i=0;i<arr.length;i++)
	// {
	// if(arr[i]>ret.MaxValue)
	// {
	// ret.MaxValue=arr[i];
	// ret.MaxIndex=i;
	// }
	// if(arr[i]<ret.MinValue)
	// {
	// ret.MinValue=arr[i];
	// ret.MinIndex=i;
	// }
	// }
	// return ret;
	// }
	// float Integrate(float[] arr)
	// {
	// float ret=0;
	// for(int i=0;i<arr.length;i++)
	// {
	// ret+=arr[i];
	// }
	// return ret;
	// }
	// boolean Mode; //2d or 3d
	// boolean Hamlib3DMode=true;
	// boolean Hamlib2DMode=false;
	// void Init(boolean Mode3D)
	// {
	// noStroke();
	// Mode=Mode3D;
	// hnav.Init();
	// if(Mode3D)
	// {
	// hcam_Init();
	// // noCursor();
	// }
	// hsim.Init();
	// }
	// void mousePressed()
	// {
	// hnav.mousePressed();
	// hsim.mousePressed();
	// hgui.mousePressed();
	// }
	// void mouseDragged()
	// {
	// hnav.mouseDragged();
	// hcam_mouseDragged();
	// hsim.mouseDragged();
	// }
	// void mouseReleased()
	// {
	// hnav.mouseReleased();
	// hsim.mouseReleased();
	// }
	// void mouseMoved()
	// {
	// hcam_mouseMoved();
	// }
	// void keyPressed()
	// {
	// hnav.keyPressed();
	// hcam_keyPressed();
	// hgui.keyPressed();
	// }
	// void mouseScrolled()
	// {
	// hnav.mouseScrolled();
	// hcam_mouseScrolled();
	// }
	// void Camera()
	// {
	// if(Mode==true)
	// {
	// hcam_Transform();
	// }
	// else
	// {
	// hnav.Transform();
	// }
	// }
	// void Update(int r,int g,int b)
	// {
	// background(r,g,b);
	// pushMatrix();
	// Camera();
	// hrend_DrawBegin();
	// Simulate();
	// hrend_DrawEnd();
	// popMatrix();
	// Gui();
	// hrend_DrawGUI();
	// }
	// void Gui()
	// {
	// hgui.Draw();
	// }
	// void Simulate()
	// {
	// hsim.Simulate();
	// }
	// }
	// Hamlib hamlib=new Hamlib();
	//
	//
	// float max_distance;
	//
	//
	//
	// class Hcam
	// {
	// Hcam(){}
	// int mouse_x;
	// int mouse_y;
	// float xpos;
	// float ypos;
	// float zpos;
	// float xrot;
	// float yrot;
	// float angle;
	// float camzahigkeit;
	// float speed=10.0f;
	// }
	// Hcam hcam=new Hcam();
	// void hcam_mouseScrolled()
	// {
	// float mul=-1;
	// if(mouseScroll>0)
	// {
	// mul=1;
	// }
	// float xrotrad, yrotrad;
	// yrotrad=(float)(hcam.yrot/180*PI);
	// xrotrad=(float)(hcam.xrot/180*PI);
	// hcam.xpos+=mul*(float)(hcam.speed*2*sin(yrotrad));
	// hcam.zpos-=mul*(float)(hcam.speed*2*cos(yrotrad));
	// hcam.ypos-=mul*(float)(hcam.speed*2*sin(xrotrad));
	// }
	// void hcam_keyPressed()
	// {
	// if(key=='q')
	// {
	// hcam.xrot+=1;
	// if(hcam.xrot>360)
	// {
	// hcam.xrot-=360;
	// }
	// }
	// if(key=='e')
	// {
	// hcam.xrot-=1;
	// if(hcam.xrot<-360)
	// {
	// hcam.xrot+=360;
	// }
	// }
	// if(key=='s')
	// {
	// float xrotrad,yrotrad;
	// yrotrad=(float)(hcam.yrot/180*PI);
	// xrotrad=(float)(hcam.xrot/180*PI);
	// hcam.xpos-=(float)(hcam.speed*sin(yrotrad));
	// hcam.zpos+=(float)(hcam.speed*cos(yrotrad)) ;
	// hcam.ypos+=(float)(hcam.speed*sin(xrotrad));
	// }
	// if(key=='w')
	// {
	// float xrotrad, yrotrad;
	// yrotrad=(float)(hcam.yrot/180*PI);
	// xrotrad=(float)(hcam.xrot/180*PI);
	// hcam.xpos+=(float)(hcam.speed*sin(yrotrad));
	// hcam.zpos-=(float)(hcam.speed*cos(yrotrad));
	// hcam.ypos-=(float)(hcam.speed*sin(xrotrad));
	// }
	// if(key=='d')
	// {
	// hcam.yrot+=1;
	// if(hcam.yrot>360)
	// {
	// hcam.yrot-=360;
	// }
	// }
	// if(key=='a')
	// {
	// hcam.yrot-=1;
	// if(hcam.yrot<-360)
	// {
	// hcam.yrot += 360;
	// }
	// }
	// }
	// int lastx=512,lasty=384;
	// void hcam_mouseDragged()
	// {
	// if(mouseButton==CENTER)
	// {
	// float difx=(float)lastx-mouseX;
	// float dify=(float)lasty-mouseY;
	//
	// hcam.xrot+=dify*hcam.camzahigkeit;
	// if(hcam.xrot<-360)
	// {
	// hcam.xrot+=360;
	// }
	// hcam.yrot-=difx*hcam.camzahigkeit;
	// if(hcam.yrot<-360)
	// {
	// hcam.yrot += 360;
	// }
	// lastx=mouseX;
	// lasty=mouseY;
	// }
	// }
	// void hcam_mouseMoved()
	// {
	// lastx=mouseX;
	// lasty=mouseY;
	// }
	//
	// float
	// hcam_saved_xpos,hcam_saved_ypos,hcam_saved_zpos,hcam_saved_xrot,hcam_saved_yrot;
	// void hcam_SaveCamPos()
	// {
	// hcam_saved_xpos=hcam.xpos;
	// hcam_saved_ypos=hcam.ypos;
	// hcam_saved_zpos=hcam.zpos;
	// hcam_saved_xrot=hcam.xrot;
	// hcam_saved_yrot=hcam.yrot;
	// }
	// void hcam_SetCamPos(float x,float y,float z,float xrot,float yrot)
	// {
	// hcam.xpos=x;
	// hcam.ypos=y;
	// hcam.zpos=z;
	// hcam.xrot=xrot;
	// hcam.yrot=yrot;
	// }
	// void hcam_LoadCamPos()
	// {
	// hcam.xpos=hcam_saved_xpos;
	// hcam.ypos=hcam_saved_ypos;
	// hcam.zpos=hcam_saved_zpos;
	// hcam.xrot=hcam_saved_xrot;
	// hcam.yrot=hcam_saved_yrot;
	// }
	// void hcam_Init()
	// {
	// hcam.xpos=width/2;
	// hcam.ypos=height/2;
	// hcam.zpos=0.0f;
	// hcam.xrot=0.0f;
	// hcam.yrot=0.0f;
	// hcam.angle=0.0f;
	// hcam.camzahigkeit=0.3f;
	// hnav.difx=0;
	// hnav.dify=0;
	// }
	// void hcam_Transform()
	// {
	// translate(width/2,height/2);
	// translate(hnav.difx,hnav.dify);
	// hnav.EnableZooming=false;
	// rotateX(hcam.xrot/360*2*PI);
	// rotateY(hcam.yrot/360*2*PI);
	// translate(-hcam.xpos,-hcam.ypos,-hcam.zpos);
	// }
	//
	// class Gui
	// {
	// float px;
	// float py;
	// float sx;
	// float sy;
	// boolean bTextBox;
	// String text;
	// String name;
	// Gui(float Px,float Py,float Sx,float Sy, String Name, String Text,
	// boolean TextBox)
	// {
	// px=Px;
	// py=Py;
	// sx=Sx;
	// sy=Sy;
	// name=Name;
	// text=Text;
	// bTextBox=TextBox;
	// }
	// }
	//
	// class Hgui
	// {
	// Hgui(){}
	//
	// ArrayList gui=new ArrayList();
	// Gui selected=null;
	// void keyPressed()
	// {
	// if(selected!=null && selected.bTextBox)
	// {
	// if(keyCode==BACKSPACE)
	// {
	// int len=selected.text.length();
	// if(len-1>=0)
	// {
	// selected.text=selected.text.substring(0,len-1);
	// }
	// }
	// else
	// if(keyCode==ENTER)
	// {
	// hgui_ElemEvent(selected);
	// }
	// else
	// if(key>='a' && key<'z' || key>='A' && key<'Z')
	// {
	// selected.text+=key;
	// }
	// }
	// }
	// void mousePressed()
	// {
	// for(int i=0;i<gui.size();i++)
	// {
	// Gui g=((Gui)gui.get(i));
	// if(mouseX>g.px && mouseX<g.px+g.sx && mouseY>g.py && mouseY<g.py+g.sy)
	// {
	// if(!g.bTextBox)
	// {
	// hgui_ElemEvent(g);
	// }
	// selected=g;
	// }
	// }
	// }
	// void Draw()
	// {
	// for(int i=0;i<gui.size();i++)
	// {
	// Gui g=((Gui)gui.get(i));
	// fill(0,0,0);
	// rect(g.px,g.py,g.sx,g.sy);
	// fill(255,255,255);
	// text(g.text,g.px+g.sx/2,g.py+g.sy/2);
	// }
	// }
	// }
	// Hgui hgui=new Hgui();
	//
	//
	//
	// class Hnav
	// {
	// Hnav(){ }
	// float savepx=0;
	// float savepy=0;
	// int selID=0;
	// float zoom=1.0f;
	// float difx=0;
	// float dify=0;
	// int lastscr=0;
	// boolean EnableZooming=true;
	// float scrollcamspeed=1.1f;
	//
	// float MouseToWorldCoordX(int x)
	// {
	// if(hamlib.Mode)
	// {
	// return mouseX;
	// }
	// return 1/zoom*(x-difx-width/2);
	// }
	// float MouseToWorldCoordY(int y)
	// {
	// if(hamlib.Mode)
	// {
	// return mouseY;
	// }
	// return 1/zoom*(y-dify-height/2);
	// }
	// boolean md=false;
	// void mousePressed()
	// {
	// md=true;
	// if(mouseButton==RIGHT)
	// {
	// savepx=mouseX;
	// savepy=mouseY;
	// }
	// }
	// void mouseReleased()
	// {
	// md=false;
	// }
	// void mouseDragged()
	// {
	// if(mouseButton==RIGHT)
	// {
	// difx+=(mouseX-savepx);
	// dify+=(mouseY-savepy);
	// savepx=mouseX;
	// savepy=mouseY;
	// }
	// }
	// float camspeed=20.0f;
	// float scrollcammult=0.92f;
	// boolean keyToo=true;
	// void keyPressed()
	// {
	// if((keyToo && key=='w') || keyCode==UP)
	// {
	// dify+=(camspeed);
	// }
	// if((keyToo && key=='s') || keyCode==DOWN)
	// {
	// dify+=(-camspeed);
	// }
	// if((keyToo && key=='a') || keyCode==LEFT)
	// {
	// difx+=(camspeed);
	// }
	// if((keyToo && key=='d') || keyCode==RIGHT)
	// {
	// difx+=(-camspeed);
	// }
	// if(!EnableZooming)
	// {
	// return;
	// }
	// if(key=='-' || key=='#')
	// {
	// float zoomBefore=zoom;
	// zoom*=scrollcammult;
	// difx=(difx)*(zoom/zoomBefore);
	// dify=(dify)*(zoom/zoomBefore);
	// }
	// if(key=='+')
	// {
	// float zoomBefore=zoom;
	// zoom/=scrollcammult;
	// difx=(difx)*(zoom/zoomBefore);
	// dify=(dify)*(zoom/zoomBefore);
	// }
	// }
	// void Init()
	// {
	// difx=-width/2;
	// dify=-height/2;
	// }
	// void mouseScrolled()
	// {
	// if(!EnableZooming)
	// {
	// return;
	// }
	// float zoomBefore=zoom;
	// if(mouseScroll>0)
	// {
	// zoom*=scrollcamspeed;
	// }
	// else
	// {
	// zoom/=scrollcamspeed;
	// }
	// difx=(difx)*(zoom/zoomBefore);
	// dify=(dify)*(zoom/zoomBefore);
	// }
	// void Transform()
	// {
	// translate(difx+0.5f*width,dify+0.5f*height);
	// scale(zoom,zoom);
	// }
	// }
	// Hnav hnav=new Hnav();
	//
	// class MinMaxClass
	// {
	// float MaxValue;
	// float MinValue;
	// int MaxIndex;
	// int MinIndex;
	// MinMaxClass()
	// {
	// MaxValue=-999999;
	// MinValue=999999;
	// MaxIndex=0;
	// MinIndex=0;
	// }
	// }
	//
	//
	// class Obj
	// {
	// Obj(){}
	// float[] visarea;
	// float[] visareatype;
	// int type;
	// float x;
	// float y;
	// float a;
	// float v;
	// float vx;
	// float vy;
	// float s;
	// float acc;
	// boolean DrawField=false;
	// Hai hai=null;
	// Hsim_Custom custom=null;
	// Obj(Hsim_Custom customobj,Hai haiobj,int X,int Y,float A,float V,float
	// Vx,float Vy,float S,int Type,int Hsim_eyesize)
	// {
	// hai=haiobj;
	// custom=customobj;
	// x=X;
	// y=Y;
	// a=A;
	// v=V;
	// s=S;
	// vx=Vx;
	// vy=Vy;
	// type=Type;
	// if(Hsim_eyesize>0)
	// {
	// visarea=new float[Hsim_eyesize];
	// visareatype=new float[Hsim_eyesize];
	// }
	// }
	// void DrawViewFields(int x,int y,int RenderSize)
	// {
	// if(Hsim_eyesize>0)
	// {
	// pushMatrix();
	// translate(x,y);
	// hamlib.Draw1DLine(visarea,10);
	// translate(0,10);
	// hamlib.Draw1DLine(visareatype,10);
	// popMatrix();
	// }
	// }
	// }
	//
	// public void settings() {
	// size(600,600);
	// }
	//
	// public void setup()
	// {
	// //mem.simulate_consistency=0.05;
	// //mem.simulate_damping=0.90;
	// //size(worldSize-200,worldSize-200);
	// hamlib.Init(false);
	// im[0]=loadImage("."+File.separator+"nars_lab"+File.separator+"src"+File.separator+"main"+File.separator+"java"+File.separator+"nars"+File.separator+"microworld"+File.separator+"agent.png");
	// im[1]=loadImage("."+File.separator+"nars_lab"+File.separator+"src"+File.separator+"main"+File.separator+"java"+File.separator+"nars"+File.separator+"microworld"+File.separator+"food.png");
	// im[2]=loadImage("."+File.separator+"nars_lab"+File.separator+"src"+File.separator+"main"+File.separator+"java"+File.separator+"nars"+File.separator+"microworld"+File.separator+"fire.png");
	// for(int i=0;i<1;i++)
	// {
	// int SomSize=10;
	// Hai h=new Hai(nactions,SomSize);
	// h.som=new Hsom(SomSize,Hsim_eyesize*2);
	// h.som.Leaky=false;
	// test=new Obj(new
	// Hsim_Custom(),h,(int)(random(1)*(double)width),(int)(random(1)*(double)height),random(1)*2*PI-PI,random(1),0,0,random(1)*5+20,0,Hsim_eyesize);
	// hsim.obj.add(test);
	// }
	// lastclicked=((Obj)hsim.obj.get(0));
	// for(int i=0;i<5;i++)
	// {
	// hsim.obj.add(new
	// Obj(null,null,(int)(random(1)*(double)width),(int)(random(1)*(double)height),random(1)*2*PI,0,0,0,random(1)*5+20,1,10));
	// }
	// for(int i=0;i<5;i++)
	// {
	// hsim.obj.add(new
	// Obj(null,null,(int)(random(1)*(double)width),(int)(random(1)*(double)height),random(1)*2*PI,0,0,0,random(1)*5+20,2,10));
	// }
	// hsim.viewdist=width/5; //4
	// label1=new Gui(0,height-25,width,25, "label1", "", false);
	// hgui.gui.add(label1);
	// }
	//
	// public void draw()
	// {
	// hamlib.Update(128,138,128);
	// //mem.simulate(0);
	// }
	//
	//
	//
	// }
}
