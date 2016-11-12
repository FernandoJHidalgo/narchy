/*
Copyright 2007 Brian Tanner
http://rl-library.googlecode.com/
brian@tannerpages.com
http://brian.tannerpages.com

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package nars.experiment.tetris.impl;

import com.jogamp.opengl.GL2;
import spacegraph.Facial;
import spacegraph.SpaceGraph;
import spacegraph.Surface;

import javax.swing.*;

public class TetrisVisualizer extends Surface {

    private final int blockSize;
    private final int lastUpdateTimeStep = -1;
    JCheckBox printGridCheckBox;
    public final TetrisState tetris;
    private final TetrisBlocksComponent blocks;

    public boolean printGrid() {
        if (printGridCheckBox != null) {
            return printGridCheckBox.isSelected();
        }
        return false;
    }

    public TetrisVisualizer(TetrisState t, int blockSize) {
        this(t, blockSize, true);
    }

    public TetrisVisualizer(TetrisState t, int blockSize, boolean newWindow) {
        super();


        if (newWindow) {
            SpaceGraph s = new SpaceGraph();
            s.show(t.getWidth() * blockSize, t.getHeight() * blockSize);
            s.add(new Facial(this).maximize());
        }

        tetris = t;
        this.blockSize = blockSize;

        blocks = new TetrisBlocksComponent(this);

        // this.theGlueState = theGlueState;
        // this.theControlTarget = theControlTarget;
        // SelfUpdatingVizComponent theBlockVisualizer = new
        // TetrisBlocksComponent(this);
        // SelfUpdatingVizComponent theTetrlaisScoreViz = new
        // TetrisScoreComponent(this);
        //
        // addVizComponentAtPositionWithSize(theBlockVisualizer, 0, .1, 1.0,
        // .9);
        // addVizComponentAtPositionWithSize(theTetrlaisScoreViz, 0, 0, 1.0,
        // 0.3);
        //
        // addDesiredExtras();

        //render();
    }


    /*public void render() {
        blocks.render(g(), blockSize);
	}*/

    @Override
    public void paint(GL2 g) {
        //g.setPaintMode();
        if (blocks != null && tetris != null)
            blocks.render(g, tetris.seen);
        //g.setColor(Color.BLUE);
        //g.setXORMode(Color.GREEN);
        //g.drawString("Score: " + Texts.n4(tetris.reward()), 0, 400);
    }

    // //Override this if you don't want some extras (like check boxes)
    // protected void addDesiredExtras() {
    // addPreferenceComponents();
    // }

    // public void checkCoreData() {
    // if (currentState == null) {
    // currentState = TetrisStateRequest.Execute();
    // }
    // }
    //
    // protected void addPreferenceComponents() {
    // //Setup the slider
    // printGridCheckBox = new JCheckBox();
    // if (theControlTarget != null) {
    // Vector<Component> newComponents = new Vector<Component>();
    // JLabel tetrisPrefsLabel = new JLabel("Tetris Visualizer Preferences: ");
    // JLabel printGridLabel = new JLabel("Draw Grid");
    // tetrisPrefsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
    // printGridLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
    //
    // JPanel gridPrefPanel = new JPanel();
    // gridPrefPanel.add(printGridLabel);
    // gridPrefPanel.add(printGridCheckBox);
    //
    //
    // newComponents.add(tetrisPrefsLabel);
    // newComponents.add(gridPrefPanel);
    //
    // theControlTarget.addControls(newComponents);
    // }
    //
    // }
    //
    // public void updateAgentState(boolean force) {
    // // //Only do this if we're on a new time step
    // int currentTimeStep = theGlueState.getTotalSteps();
    //
    //
    // // if (currentState == null || currentTimeStep != lastUpdateTimeStep ||
    // force) {
    // System.out.println("\t\tRequesting tetris state");
    // currentState = TetrisStateRequest.Execute();
    // System.out.println("\t\tGot tetris state");
    // lastUpdateTimeStep = currentTimeStep;
    // // }
    // }

    public int getWorldWidth() {
        return tetris.getWidth();
    }

    //
    public int getWorldHeight() {
        return tetris.getHeight();
    }
    //
    // public double getScore() {
    // return theGlueState.getReturnThisEpisode();
    // }
    //

    //
    // public int getEpisodeNumber() {
    // return theGlueState.getEpisodeNumber();
    // }
    //
    // public int getTimeStep() {
    // return theGlueState.getTimeStep();
    // }
    //
    // public int getTotalSteps() {
    // return theGlueState.getTotalSteps();
    // }
    //
    // public int getCurrentPiece() {
    // checkCoreData();
    // return currentState.getCurrentPiece();
    // }
    //
    // public TinyGlue getGlueState() {
    // return theGlueState;
    // }
    //
    // public void drawObs(Observation tetrisObs) {
    // System.out.println("STEP: " + theGlueState.getTotalSteps());
    // int index = 0;
    // for (int i = 0; i < currentState.getHeight(); i++) {
    // for (int j = 0; j < currentState.getWidth(); j++) {
    // index = i * currentState.getWidth() + j;
    // System.out.print(tetrisObs.intArray[index]);
    // }
    // System.out.print("\n");
    // }
    //
    // }
    //
    // public String getName() {
    // return "Tetris 1.1 (DEV)";
    // }
}
