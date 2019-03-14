/*
 * This file is part of lanterna (http://code.google.com/p/lanterna/).
 * 
 * lanterna is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Copyright (C) 2010-2016 Martin
 */
package com.googlecode.lanterna.gui2;

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The list box will display a number of items, of which one and only one can be marked as selected.
 * The user can select an item in the list box by pressing the return key or space bar key. If you
 * select one item when another item is already selected, the previously selected item will be
 * deselected and the highlighted item will be the selected one instead.
 * @author Martin
 */
public class RadioBoxList<V> extends AbstractListBox<V, RadioBoxList<V>> {
    /**
     * Listener interface that can be attached to the {@code RadioBoxList} in order to be notified on user actions
     */
    public interface Listener {
        /**
         * Called by the {@code RadioBoxList} when the user changes which item is selected
         * @param selectedIndex Index of the newly selected item, or -1 if the selection has been cleared (can only be
         *                      done programmatically)
         * @param previousSelection The index of the previously selected item which is now no longer selected, or -1 if
         *                          nothing was previously selected
         */
        void onSelectionChanged(int selectedIndex, int previousSelection);
    }

    private final List<Listener> listeners;
    private int checkedIndex;

    /**
     * Creates a new RadioCheckBoxList with no items. The size of the {@code RadioBoxList} will be as big as is required
     * to display all items.
     */
    public RadioBoxList() {
        this(null);
    }

    /**
     * Creates a new RadioCheckBoxList with a specified size. If the items in the {@code RadioBoxList} cannot fit in the
     * size specified, scrollbars will be used
     * @param preferredSize Size of the {@code RadioBoxList} or {@code null} to have it try to be as big as necessary to
     *                      be able to draw all items
     */
    public RadioBoxList(TerminalPosition preferredSize) {
        super(preferredSize);
        this.listeners = new CopyOnWriteArrayList<>();
        this.checkedIndex = -1;
    }

    @Override
    protected ListItemRenderer<V,RadioBoxList<V>> createDefaultListItemRenderer() {
        return new RadioBoxListItemRenderer<>();
    }

    @Override
    public synchronized Result handleKeyStroke(KeyStroke keyStroke) {
        if(keyStroke.getKeyType() == KeyType.Enter ||
                (keyStroke.getKeyType() == KeyType.Character && keyStroke.getCharacter() == ' ')) {
            setCheckedIndex( getSelectedIndex() );
            return Result.HANDLED;
        }
        return super.handleKeyStroke(keyStroke);
    }

    @Override
    public synchronized RadioBoxList<V> clearItems() {
        setCheckedIndex(-1);
        return super.clearItems();
    }

    /**
     * This method will see if an object is the currently selected item in this RadioCheckBoxList
     * @param object Object to test if it's the selected one
     * @return {@code true} if the supplied object is what's currently selected in the list box,
     * {@code false} otherwise. Returns null if the supplied object is not an item in the list box.
     */
    public synchronized Boolean isChecked(V object) {
        if(object == null)
            return null;

        if(indexOf(object) == -1)
            return null;

        return checkedIndex == indexOf(object);
    }

    /**
     * This method will see if an item, addressed by index, is the currently selected item in this
     * RadioCheckBoxList
     * @param index Index of the item to check if it's currently selected
     * @return {@code true} if the currently selected object is at the supplied index,
     * {@code false} otherwise. Returns false if the index is out of range.
     */
    @SuppressWarnings("SimplifiableIfStatement")
    public synchronized boolean isChecked(int index) {
        if(index < 0 || index >= getItemCount()) {
            return false;
        }

        return checkedIndex == index;
    }

    /**
     * Sets the currently checked item by the value itself. If null, the selection is cleared. When changing selection,
     * any previously selected item is deselected.
     * @param item Item to be checked
     */
    public synchronized void setCheckedItem(V item) {
        if(item == null) {
            setCheckedIndex(-1);
        }
        else {
            setCheckedItemIndex(indexOf(item));
        }
    }

    /**
     * Sets the currently selected item by index. If the index is out of range, it does nothing.
     * @param index Index of the item to be selected
     */
    public synchronized void setCheckedItemIndex(int index) {
        if(index < -1 || index >= getItemCount())
            return;

        setCheckedIndex(index);
    }

    /**
     * @return The index of the item which is currently selected, or -1 if there is no selection
     */
    public int getCheckedItemIndex() {
        return checkedIndex;
    }

    /**
     * @return The object currently selected, or null if there is no selection
     */
    public synchronized V getCheckedItem() {
        if(checkedIndex == -1 || checkedIndex >= getItemCount())
            return null;

        return getItemAt(checkedIndex);
    }

    /**
     * Un-checks the currently checked item (if any) and leaves the radio check box in a state where no item is checked.
     */
    public synchronized void clearSelection() {
        setCheckedIndex(-1);
    }

    /**
     * Adds a new listener to the {@code RadioBoxList} that will be called on certain user actions
     * @param listener Listener to attach to this {@code RadioBoxList}
     * @return Itself
     */
    public RadioBoxList<V> addListener(Listener listener) {
        if(listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
        return this;
    }

    /**
     * Removes a listener from this {@code RadioBoxList} so that if it had been added earlier, it will no longer be
     * called on user actions
     * @param listener Listener to remove from this {@code RadioBoxList}
     * @return Itself
     */
    public RadioBoxList<V> removeListener(Listener listener) {
        listeners.remove(listener);
        return this;
    }

    private void setCheckedIndex(int index) {
        final int previouslyChecked = checkedIndex;
        this.checkedIndex = index;
        invalidate();
        runOnGUIThreadIfExistsOtherwiseRunDirect(new Runnable() {
            @Override
            public void run() {
                for(Listener listener: listeners) {
                    listener.onSelectionChanged(checkedIndex, previouslyChecked);
                }
            }
        });
    }

    /**
     * Default renderer for this component which is used unless overridden. The selected state is drawn on the left side
     * of the item label using a "&lt; &gt;" block filled with an "o" if the item is the selected one
     * @param <V>
     */
    public static class RadioBoxListItemRenderer<V> extends ListItemRenderer<V,RadioBoxList<V>> {
        @Override
        public int getHotSpotPositionOnLine(int selectedIndex) {
            return 1;
        }

        @Override
        public String getLabel(RadioBoxList<V> listBox, int index, V item) {
            String check = " ";
            if(listBox.checkedIndex == index)
                check = "o";

            String text = (item != null ? item : "<null>").toString();
            return '<' + check + "> " + text;
        }
    }

}