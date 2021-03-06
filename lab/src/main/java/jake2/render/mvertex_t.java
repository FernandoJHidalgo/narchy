/*
 * Copyright (C) 1997-2001 Id Software, Inc.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * 
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 *  
 */



package jake2.render;

import jake2.Defines;

import java.nio.ByteBuffer;

public class mvertex_t {
    public static final int DISK_SIZE = 3 * Defines.SIZE_OF_FLOAT;

    public static final int MEM_SIZE = 3 * Defines.SIZE_OF_FLOAT;

    public final float[] position = { 0, 0, 0 };

    public mvertex_t(ByteBuffer b) {
        float[] position = this.position;
        position[0] = b.getFloat();
        position[1] = b.getFloat();
        position[2] = b.getFloat();
    }
}