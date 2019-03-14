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
package com.googlecode.lanterna.graphics;

import com.googlecode.lanterna.*;
import com.googlecode.lanterna.screen.TabBehaviour;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;

/**
 * This class hold the default logic for drawing the basic text graphic as exposed by TextGraphic. All implementations
 * rely on a setCharacter method being implemented in subclasses.
 * @author Martin
 */
public abstract class AbstractTextGraphics implements TextGraphics {
    protected TextColor foregroundColor;
    protected TextColor backgroundColor;
    protected TabBehaviour tabBehaviour;
    protected final EnumSet<SGR> activeModifiers;
    private final ShapeRenderer shapeRenderer;

    protected AbstractTextGraphics() {
        this.activeModifiers = EnumSet.noneOf(SGR.class);
        this.tabBehaviour = TabBehaviour.ALIGN_TO_COLUMN_4;
        this.foregroundColor = TextColor.ANSI.DEFAULT;
        this.backgroundColor = TextColor.ANSI.DEFAULT;
        this.shapeRenderer = new DefaultShapeRenderer(this::set);
    }

    @Override
    public TextColor getBackgroundColor() {
        return backgroundColor;
    }

    @Override
    public TextGraphics setBackgroundColor(final TextColor backgroundColor) {
        this.backgroundColor = backgroundColor;
        return this;
    }

    @Override
    public TextColor getForegroundColor() {
        return foregroundColor;
    }

    @Override
    public TextGraphics setForegroundColor(final TextColor foregroundColor) {
        this.foregroundColor = foregroundColor;
        return this;
    }

    @Override
    public TextGraphics enableModifiers(SGR... modifiers) {
        enableModifiers(Arrays.asList(modifiers));
        return this;
    }

    private void enableModifiers(Collection<SGR> modifiers) {
        this.activeModifiers.addAll(modifiers);
    }

    @Override
    public TextGraphics disableModifiers(SGR... modifiers) {
        disableModifiers(Arrays.asList(modifiers));
        return this;
    }

    private void disableModifiers(Collection<SGR> modifiers) {
        this.activeModifiers.removeAll(modifiers);
    }

    @Override
    public synchronized TextGraphics setModifiers(EnumSet<SGR> modifiers) {
        activeModifiers.clear();
        activeModifiers.addAll(modifiers);
        return this;
    }

    @Override
    public TextGraphics clearModifiers() {
        this.activeModifiers.clear();
        return this;
    }

    @Override
    public EnumSet<SGR> getActiveModifiers() {
        return activeModifiers;
    }

    @Override
    public TabBehaviour getTabBehaviour() {
        return tabBehaviour;
    }

    @Override
    public TextGraphics setTabBehaviour(TabBehaviour tabBehaviour) {
        if(tabBehaviour != null) {
            this.tabBehaviour = tabBehaviour;
        }
        return this;
    }

    @Override
    public TextGraphics fill(char c) {
        fillRectangle(TerminalPosition.TOP_LEFT_CORNER, getSize(), c);
        return this;
    }

    @Override
    public TextGraphics set(int column, int row, char character) {
        return set(column, row, newTextCharacter(character));
    }

    @Override
    public TextGraphics set(TerminalPosition position, TextCharacter textCharacter) {
        set(position.col, position.row, textCharacter);
        return this;
    }

    @Override
    public TextGraphics set(TerminalPosition position, char character) {
        return set(position.col, position.row, character);
    }

    @Override
    public TextGraphics drawLine(TerminalPosition fromPosition, TerminalPosition toPoint, char character) {
        return drawLine(fromPosition, toPoint, newTextCharacter(character));
    }

    @Override
    public TextGraphics drawLine(TerminalPosition fromPoint, TerminalPosition toPoint, TextCharacter character) {
        shapeRenderer.drawLine(fromPoint, toPoint, character);
        return this;
    }

    @Override
    public TextGraphics drawLine(int fromX, int fromY, int toX, int toY, char character) {
        return drawLine(fromX, fromY, toX, toY, newTextCharacter(character));
    }

    @Override
    public TextGraphics drawLine(int fromX, int fromY, int toX, int toY, TextCharacter character) {
        return drawLine(new TerminalPosition(fromX, fromY), new TerminalPosition(toX, toY), character);
    }

    @Override
    public TextGraphics drawTriangle(TerminalPosition p1, TerminalPosition p2, TerminalPosition p3, char character) {
        return drawTriangle(p1, p2, p3, newTextCharacter(character));
    }

    @Override
    public TextGraphics drawTriangle(TerminalPosition p1, TerminalPosition p2, TerminalPosition p3, TextCharacter character) {
        shapeRenderer.drawTriangle(p1, p2, p3, character);
        return this;
    }

    @Override
    public TextGraphics fillTriangle(TerminalPosition p1, TerminalPosition p2, TerminalPosition p3, char character) {
        return fillTriangle(p1, p2, p3, newTextCharacter(character));
    }

    @Override
    public TextGraphics fillTriangle(TerminalPosition p1, TerminalPosition p2, TerminalPosition p3, TextCharacter character) {
        shapeRenderer.fillTriangle(p1, p2, p3, character);
        return this;
    }

    @Override
    public TextGraphics drawRectangle(TerminalPosition topLeft, TerminalPosition size, char character) {
        return drawRectangle(topLeft, size, newTextCharacter(character));
    }

    @Override
    public TextGraphics drawRectangle(TerminalPosition topLeft, TerminalPosition size, TextCharacter character) {
        shapeRenderer.drawRectangle(topLeft, size, character);
        return this;
    }

    @Override
    public TextGraphics fillRectangle(TerminalPosition topLeft, TerminalPosition size, char character) {
        return fillRectangle(topLeft, size, newTextCharacter(character));
    }

    @Override
    public TextGraphics fillRectangle(TerminalPosition topLeft, TerminalPosition size, TextCharacter character) {
        shapeRenderer.fillRectangle(topLeft, size, character);
        return this;
    }

    @Override
    public TextGraphics drawImage(TerminalPosition topLeft, TextImage image) {
        return drawImage(topLeft, image, TerminalPosition.TOP_LEFT_CORNER, image.getSize());
    }

    @Override
    public TextGraphics drawImage(
            TerminalPosition topLeft,
            TextImage image,
            TerminalPosition sourceImageTopLeft,
            TerminalPosition sourceImageSize) {

        // If the source image position is negative, offset the whole image
        if(sourceImageTopLeft.col < 0) {
            topLeft = topLeft.withRelativeColumn(-sourceImageTopLeft.col);
            sourceImageSize = sourceImageSize.withRelativeColumn(sourceImageTopLeft.col);
            sourceImageTopLeft = sourceImageTopLeft.withColumn(0);
        }
        if(sourceImageTopLeft.row < 0) {
            topLeft = topLeft.withRelativeRow(-sourceImageTopLeft.row);
            sourceImageSize = sourceImageSize.withRelativeRow(sourceImageTopLeft.row);
            sourceImageTopLeft = sourceImageTopLeft.withRow(0);
        }

        // cropping specified image-subrectangle to the image itself:
        int fromRow = Math.max(sourceImageTopLeft.row, 0);
        int untilRow = Math.min(sourceImageTopLeft.row + sourceImageSize.row, image.getSize().row);
        int fromColumn = Math.max(sourceImageTopLeft.col, 0);
        int untilColumn = Math.min(sourceImageTopLeft.col + sourceImageSize.col, image.getSize().col);

        // difference between position in image and position on target:
        int diffRow = topLeft.row - sourceImageTopLeft.row;
        int diffColumn = topLeft.col - sourceImageTopLeft.col;

        // top/left-crop at target(TextGraphics) rectangle: (only matters, if topLeft has a negative coordinate)
        fromRow = Math.max(fromRow, -diffRow);
        fromColumn = Math.max(fromColumn, -diffColumn);

        // bot/right-crop at target(TextGraphics) rectangle: (only matters, if topLeft has a negative coordinate)
        untilRow = Math.min(untilRow, getSize().row - diffRow);
        untilColumn = Math.min(untilColumn, getSize().col - diffColumn);

        if (fromRow >= untilRow || fromColumn >= untilColumn) {
            return this;
        }
        for (int row = fromRow; row < untilRow; row++) {
            for (int column = fromColumn; column < untilColumn; column++) {
                set(column + diffColumn, row + diffRow, image.get(column, row));
            }
        }
        return this;
    }

    @Override
    public TextGraphics putString(int column, int row, String string) {
        if(string.contains("\n")) {
            string = string.substring(0, string.indexOf('\n'));
        }
        if(string.contains("\r")) {
            string = string.substring(0, string.indexOf('\r'));
        }
        string = tabBehaviour.replaceTabs(string, column);
        int offset = 0;
        for(int i = 0; i < string.length(); i++) {
            char character = string.charAt(i);
            set(
                    column + offset,
                    row,
                    new TextCharacter(
                            character,
                            foregroundColor,
                            backgroundColor,
                            activeModifiers.clone()));
            
            if(TerminalTextUtils.isCharCJK(character)) {
                //CJK characters are twice the normal characters in width, so next character position is two columns forward
                offset += 2;
            }
            else {
                //For "normal" characters we advance to the next column
                offset += 1;
            }
        }
        return this;
    }

    @Override
    public TextGraphics putString(TerminalPosition position, String string) {
        putString(position.col, position.row, string);
        return this;
    }

    @Override
    public TextGraphics putString(int column, int row, String string, SGR extraModifier, SGR... optionalExtraModifiers) {clearModifiers();
        return putString(column, row, string, EnumSet.of(extraModifier, optionalExtraModifiers));
    }

    @Override
    public TextGraphics putString(int column, int row, String string, Collection<SGR> extraModifiers) {
        extraModifiers.removeAll(activeModifiers);
        enableModifiers(extraModifiers);
        putString(column, row, string);
        disableModifiers(extraModifiers);
        return this;
    }

    @Override
    public TextGraphics putString(TerminalPosition position, String string, SGR extraModifier, SGR... optionalExtraModifiers) {
        putString(position.col, position.row, string, extraModifier, optionalExtraModifiers);
        return this;
    }

    @Override
    public TextCharacter get(TerminalPosition position) {
        return get(position.col, position.row);
    }

    @Override
    public TextGraphics newTextGraphics(TerminalPosition topLeftCorner, TerminalPosition size) throws IllegalArgumentException {
        TerminalPosition writableArea = getSize();
        if(topLeftCorner.col + size.col <= 0 ||
                topLeftCorner.col >= writableArea.col ||
                topLeftCorner.row + size.row <= 0 ||
                topLeftCorner.row >= writableArea.row) {
            //The area selected is completely outside of this TextGraphics, so we can return a "null" object that doesn't
            //do anything because it is impossible to change anything anyway
            return new NullTextGraphics(size);
        }
        return new SubTextGraphics(this, topLeftCorner, size);
    }

    private TextCharacter newTextCharacter(char character) {
        return new TextCharacter(character, foregroundColor, backgroundColor, activeModifiers);
    }
}