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

import com.googlecode.lanterna.Symbols;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.graphics.ThemeDefinition;

/**
 * Classic scrollbar that can be used to display where inside a larger component a view is showing. This implementation
 * is not interactable and needs to be driven externally, meaning you can't focus on the scrollbar itself, you have to
 * update its state as part of another component being modified. {@code ScrollBar}s are either horizontal or vertical,
 * which affects the way they appear and how they are drawn.
 * <p>
 * This class works on two concepts, the min-position-max values and the view size. The minimum value is always 0 and
 * cannot be changed. The maximum value is 100 and can be adjusted programmatically. Position value is whever along the
 * axis of 0 to max the scrollbar's tracker currently is placed. The view size is an important concept, it determines
 * how big the tracker should be and limits the position so that it can only reach {@code maximum value - view size}.
 * <p>
 * The regular way to use the {@code ScrollBar} class is to tie it to the model-view of another component and set the
 * scrollbar's maximum to the total height (or width, if the scrollbar is horizontal) of the model-view. View size
 * should then be assigned based on the current size of the view, meaning as the terminal and/or the GUI changes and the
 * components visible space changes, the scrollbar's view size is updated along with it. Finally the position of the
 * scrollbar should be equal to the scroll offset in the component.
 *
 * @author Martin
 */
public class ScrollBar extends AbstractComponent<ScrollBar> {

    private final Direction direction;
    private int maximum;
    private int position;
    private int viewSize;

    /**
     * Creates a new {@code ScrollBar} with a specified direction
     * @param direction Direction of the scrollbar
     */
    public ScrollBar(Direction direction) {
        this.direction = direction;
        this.maximum = 100;
        this.position = 0;
        this.viewSize = 0;
    }

    /**
     * Returns the direction of this {@code ScrollBar}
     * @return Direction of this {@code ScrollBar}
     */
    public Direction getDirection() {
        return direction;
    }

    /**
     * Sets the maximum value the scrollbar's position (minus the view size) can have
     * @param maximum Maximum value
     * @return Itself
     */
    public ScrollBar setScrollMaximum(int maximum) {
        if(maximum < 0) {
            throw new IllegalArgumentException("Cannot set ScrollBar maximum to " + maximum);
        }
        this.maximum = maximum;
        invalidate();
        return this;
    }

    /**
     * Returns the maximum scroll value
     * @return Maximum scroll value
     */
    public int getScrollMaximum() {
        return maximum;
    }


    /**
     * Sets the scrollbar's position, should be a value between 0 and {@code maximum - view size}
     * @param position Scrollbar's tracker's position
     * @return Itself
     */
    public ScrollBar setScrollPosition(int position) {
        this.position = Math.min(position, this.maximum);
        invalidate();
        return this;
    }

    /**
     * Returns the position of the {@code ScrollBar}'s tracker
     * @return Position of the {@code ScrollBar}'s tracker
     */
    public int getScrollPosition() {
        return position;
    }

    /**
     * Sets the view size of the scrollbar, determining how big the scrollbar's tracker should be and also affecting the
     * maximum value of tracker's position
     * @param viewSize View size of the scrollbar
     * @return Itself
     */
    public ScrollBar setViewSize(int viewSize) {
        this.viewSize = viewSize;
        return this;
    }

    /**
     * Returns the view size of the scrollbar
     * @return View size of the scrollbar
     */
    public int getViewSize() {
        if(viewSize > 0) {
            return viewSize;
        }
        if(direction == Direction.HORIZONTAL) {
            return getSize().col;
        }
        else {
            return getSize().row;
        }
    }

    @Override
    protected ComponentRenderer<ScrollBar> createDefaultRenderer() {
        return new DefaultScrollBarRenderer();
    }

    /**
     * Helper class for making new {@code ScrollBar} renderers a little bit cleaner
     */
    public static abstract class ScrollBarRenderer implements ComponentRenderer<ScrollBar> {
        @Override
        public TerminalPosition getPreferredSize(ScrollBar component) {
            return TerminalPosition.ONE;
        }
    }

    /**
     * Default renderer for {@code ScrollBar} which will be used unless overridden. This will draw a scrollbar using
     * arrows at each extreme end, a background color for spaces between those arrows and the tracker and then the
     * tracker itself in three different styles depending on the size of the tracker. All characters and colors are
     * customizable through whatever theme is currently in use.
     */
    public static class DefaultScrollBarRenderer extends ScrollBarRenderer {

        private boolean growScrollTracker;

        /**
         * Default constructor
         */
        public DefaultScrollBarRenderer() {
            this.growScrollTracker = true;
        }

        /**
         * Should tracker automatically grow in size along with the {@code ScrollBar} (default: {@code true})
         * @param growScrollTracker Automatically grow tracker
         */
        public void setGrowScrollTracker(boolean growScrollTracker) {
            this.growScrollTracker = growScrollTracker;
        }

        @Override
        public void drawComponent(TextGUIGraphics graphics, ScrollBar component) {
            TerminalPosition size = graphics.getSize();
            Direction direction = component.getDirection();
            int position = component.getScrollPosition();
            int maximum = component.getScrollMaximum();
            int viewSize = component.getViewSize();

            if(size.row == 0 || size.col == 0) {
                return;
            }

            //Adjust position if necessary
            if(position + viewSize >= maximum) {
                position = Math.max(0, maximum - viewSize);
                component.setScrollPosition(position);
            }

            ThemeDefinition themeDefinition = graphics.getThemeDefinition(ScrollBar.class);
            graphics.applyThemeStyle(themeDefinition.getNormal());

            if(direction == Direction.VERTICAL) {
                if(size.row == 1) {
                    graphics.set(0, 0, themeDefinition.getCharacter("VERTICAL_BACKGROUND", Symbols.BLOCK_MIDDLE));
                }
                else if(size.row == 2) {
                    graphics.set(0, 0, themeDefinition.getCharacter("UP_ARROW", Symbols.ARROW_UP));
                    graphics.set(0, 1, themeDefinition.getCharacter("DOWN_ARROW", Symbols.ARROW_DOWN));
                }
                else {
                    int scrollableArea = size.row - 2;
                    int scrollTrackerSize = 1;
                    if(growScrollTracker) {
                        float ratio = clampRatio((float) viewSize / (float) maximum);
                        scrollTrackerSize = Math.max(1, (int) (ratio * (float) scrollableArea));
                    }

                    float ratio = clampRatio((float)position / (float)(maximum - viewSize));
                    int scrollTrackerPosition = (int)(ratio * (float)(scrollableArea - scrollTrackerSize)) + 1;

                    graphics.set(0, 0, themeDefinition.getCharacter("UP_ARROW", Symbols.ARROW_UP));
                    graphics.drawLine(0, 1, 0, size.row - 2, themeDefinition.getCharacter("VERTICAL_BACKGROUND", Symbols.BLOCK_MIDDLE));
                    graphics.set(0, size.row - 1, themeDefinition.getCharacter("DOWN_ARROW", Symbols.ARROW_DOWN));
                    if(scrollTrackerSize == 1) {
                        graphics.set(0, scrollTrackerPosition, themeDefinition.getCharacter("VERTICAL_SMALL_TRACKER", Symbols.SOLID_SQUARE_SMALL));
                    }
                    else if(scrollTrackerSize == 2) {
                        graphics.set(0, scrollTrackerPosition, themeDefinition.getCharacter("VERTICAL_TRACKER_TOP", (char)0x28c));
                        graphics.set(0, scrollTrackerPosition + 1, themeDefinition.getCharacter("VERTICAL_TRACKER_BOTTOM", 'v'));
                    }
                    else {
                        graphics.set(0, scrollTrackerPosition, themeDefinition.getCharacter("VERTICAL_TRACKER_TOP", (char)0x28c));
                        graphics.drawLine(0, scrollTrackerPosition + 1, 0, scrollTrackerPosition + scrollTrackerSize - 2, themeDefinition.getCharacter("VERTICAL_TRACKER_BACKGROUND", ' '));
                        graphics.set(0, scrollTrackerPosition + (scrollTrackerSize / 2), themeDefinition.getCharacter("VERTICAL_SMALL_TRACKER", Symbols.SOLID_SQUARE_SMALL));
                        graphics.set(0, scrollTrackerPosition + scrollTrackerSize - 1, themeDefinition.getCharacter("VERTICAL_TRACKER_BOTTOM", 'v'));
                    }
                }
            }
            else {
                if(size.col == 1) {
                    graphics.set(0, 0, themeDefinition.getCharacter("HORIZONTAL_BACKGROUND", Symbols.BLOCK_MIDDLE));
                }
                else if(size.col == 2) {
                    graphics.set(0, 0, Symbols.ARROW_LEFT);
                    graphics.set(1, 0, Symbols.ARROW_RIGHT);
                }
                else {
                    int scrollableArea = size.col - 2;
                    int scrollTrackerSize = 1;
                    if(growScrollTracker) {
                        float ratio = clampRatio((float) viewSize / (float) maximum);
                        scrollTrackerSize = Math.max(1, (int) (ratio * (float) scrollableArea));
                    }

                    float ratio = clampRatio((float)position / (float)(maximum - viewSize));
                    int scrollTrackerPosition = (int)(ratio * (float)(scrollableArea - scrollTrackerSize)) + 1;

                    graphics.set(0, 0, themeDefinition.getCharacter("LEFT_ARROW", Symbols.ARROW_LEFT));
                    graphics.drawLine(1, 0, size.col - 2, 0, themeDefinition.getCharacter("HORIZONTAL_BACKGROUND", Symbols.BLOCK_MIDDLE));
                    graphics.set(size.col - 1, 0, themeDefinition.getCharacter("RIGHT_ARROW", Symbols.ARROW_RIGHT));
                    if(scrollTrackerSize == 1) {
                        graphics.set(scrollTrackerPosition, 0, themeDefinition.getCharacter("HORIZONTAL_SMALL_TRACKER", Symbols.SOLID_SQUARE_SMALL));
                    }
                    else if(scrollTrackerSize == 2) {
                        graphics.set(scrollTrackerPosition, 0, themeDefinition.getCharacter("HORIZONTAL_TRACKER_LEFT", '<'));
                        graphics.set(scrollTrackerPosition + 1, 0, themeDefinition.getCharacter("HORIZONTAL_TRACKER_RIGHT", '>'));
                    }
                    else {
                        graphics.set(scrollTrackerPosition, 0, themeDefinition.getCharacter("HORIZONTAL_TRACKER_LEFT", '<'));
                        graphics.drawLine(scrollTrackerPosition + 1, 0, scrollTrackerPosition + scrollTrackerSize - 2, 0, themeDefinition.getCharacter("HORIZONTAL_TRACKER_BACKGROUND", ' '));
                        graphics.set(scrollTrackerPosition + (scrollTrackerSize / 2), 0, themeDefinition.getCharacter("HORIZONTAL_SMALL_TRACKER", Symbols.SOLID_SQUARE_SMALL));
                        graphics.set(scrollTrackerPosition + scrollTrackerSize - 1, 0, themeDefinition.getCharacter("HORIZONTAL_TRACKER_RIGHT", '>'));
                    }
                }
            }
        }

        private static float clampRatio(float value) {
            if(value < 0.0f) {
                return 0.0f;
            }
            else if(value > 1.0f) {
                return 1.0f;
            }
            else {
                return value;
            }
        }
    }
}
