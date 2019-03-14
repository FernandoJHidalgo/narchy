/*
 * Copyright 2011 Mark McKay
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * MinimizeTitlePanel.java
 *
 * Created on Jan 13, 2011, 12:39:43 AM
 */
package automenta.vivisect.swing.dock;

import automenta.vivisect.swing.dock.DockingRegionContainer.MinMaxRecord;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;
import java.awt.event.ActionEvent;

import static javax.swing.BorderFactory.createBevelBorder;
import static javax.swing.BorderFactory.createEmptyBorder;

/**
 *
 * @author kitfox
 */
@Deprecated public class MinimizeTitlePanel extends JPanel {

    final MinMaxRecord record;


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JButton bn_close;
    private JButton bn_maximize;
    private JLabel label_title;
    // End of variables declaration//GEN-END:variables

    /**
     * Creates new form MinimizeTitlePanel
     */
    public MinimizeTitlePanel(MinMaxRecord record) {
        initComponents();
        this.record = record;

        label_title.setText(record.getContent().getTitle());
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        label_title = new JLabel();
        bn_maximize = new JButton();
        bn_close = new JButton();
        setBorder(createBevelBorder(BevelBorder.RAISED));
        setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        label_title.setHorizontalAlignment(SwingConstants.LEFT);
        label_title.setText("jLabel1");
        label_title.setBorder(createEmptyBorder(0, 0, 0, 4));
        add(label_title);
        bn_maximize.setIcon(new ImageIcon(getClass().getResource("/icons/maximize.png"))); // NOI18N
        bn_maximize.setBorderPainted(false);
        bn_maximize.setContentAreaFilled(false);
        bn_maximize.setMargin(new Insets(0, 0, 0, 0));
        bn_maximize.addActionListener(this::bn_maximizeActionPerformed);
        add(bn_maximize);
        bn_close.setIcon(new ImageIcon(getClass().getResource("/icons/close.png"))); // NOI18N
        bn_close.setBorderPainted(false);
        bn_close.setContentAreaFilled(false);
        bn_close.setMargin(new Insets(0, 0, 0, 0));
        bn_close.addActionListener(this::bn_closeActionPerformed);
        add(bn_close);
    } // </editor-fold>//GEN-END:initComponents

    private void bn_closeActionPerformed(ActionEvent evt) //GEN-FIRST:event_bn_closeActionPerformed
    {
        //GEN-HEADEREND:event_bn_closeActionPerformed
        record.getContainer().closeMinimized(record);
    } //GEN-LAST:event_bn_closeActionPerformed

    private void bn_maximizeActionPerformed(ActionEvent evt) //GEN-FIRST:event_bn_maximizeActionPerformed
    {
        //GEN-HEADEREND:event_bn_maximizeActionPerformed
        record.getContainer().restoreMinimized(record);
    } //GEN-LAST:event_bn_maximizeActionPerformed

}