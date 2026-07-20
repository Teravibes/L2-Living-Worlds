/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.l2jmobius.gameserver.geoengine.geodata.blocks;

import java.nio.ByteBuffer;

import org.l2jmobius.gameserver.geoengine.geodata.Cell;
import org.l2jmobius.gameserver.geoengine.geodata.IBlock;

/**
 * @author HorridoJoho, Mobius
 */
public class ComplexBlock implements IBlock
{
	private final short[] _data;
	
	public ComplexBlock(ByteBuffer bb)
	{
		_data = new short[IBlock.BLOCK_CELLS];
		for (int cellOffset = 0; cellOffset < IBlock.BLOCK_CELLS; cellOffset++)
		{
			_data[cellOffset] = bb.getShort();
		}
	}
	
	private short getCellData(int geoX, int geoY)
	{
		return _data[((geoX % IBlock.BLOCK_CELLS_X) * IBlock.BLOCK_CELLS_Y) + (geoY % IBlock.BLOCK_CELLS_Y)];
	}
	
	private byte getCellNSWE(int geoX, int geoY)
	{
		return (byte) (getCellData(geoX, geoY) & 0x000F);
	}
	
	private int getCellHeight(int geoX, int geoY)
	{
		return (short) (getCellData(geoX, geoY) & 0x0FFF0) >> 1;
	}
	
	@Override
	public boolean checkNearestNswe(int geoX, int geoY, int worldZ, int nswe)
	{
		return (getCellNSWE(geoX, geoY) & nswe) == nswe;
	}
	
	@Override
	public void setNearestNswe(int geoX, int geoY, int worldZ, byte nswe)
	{
		final short currentNswe = getNearestNswe(geoX, geoY, worldZ);
		if ((currentNswe & nswe) == 0)
		{
			final short currentHeight = (short) getCellHeight(geoX, geoY);
			final short encodedHeight = (short) (currentHeight << 1); // Shift left by 1 bit.
			final short newNswe = (short) (currentNswe | nswe); // Add NSWE.
			final short newCombinedData = (short) (encodedHeight | newNswe); // Combine height and NSWE.
			_data[((geoX % IBlock.BLOCK_CELLS_X) * IBlock.BLOCK_CELLS_Y) + (geoY % IBlock.BLOCK_CELLS_Y)] = (short) (newCombinedData & 0xffff);
		}
	}
	
	@Override
	public void unsetNearestNswe(int geoX, int geoY, int worldZ, byte nswe)
	{
		final short currentNswe = getNearestNswe(geoX, geoY, worldZ);
		if ((currentNswe & nswe) != 0)
		{
			final short currentHeight = (short) getCellHeight(geoX, geoY);
			final short encodedHeight = (short) (currentHeight << 1); // Shift left by 1 bit.
			final short newNswe = (short) (currentNswe & ~nswe); // Subtract NSWE.
			final short newCombinedData = (short) (encodedHeight | newNswe); // Combine height and NSWE.
			_data[((geoX % IBlock.BLOCK_CELLS_X) * IBlock.BLOCK_CELLS_Y) + (geoY % IBlock.BLOCK_CELLS_Y)] = (short) (newCombinedData & 0xffff);
		}
	}
	
	@Override
	public short getNearestNswe(int geoX, int geoY, int worldZ)
	{
		short nswe = 0;
		if (checkNearestNswe(geoX, geoY, worldZ, Cell.NSWE_NORTH))
		{
			nswe = (short) (nswe | Cell.NSWE_NORTH);
		}
		
		if (checkNearestNswe(geoX, geoY, worldZ, Cell.NSWE_EAST))
		{
			nswe = (short) (nswe | Cell.NSWE_EAST);
		}
		
		if (checkNearestNswe(geoX, geoY, worldZ, Cell.NSWE_SOUTH))
		{
			nswe = (short) (nswe | Cell.NSWE_SOUTH);
		}
		
		if (checkNearestNswe(geoX, geoY, worldZ, Cell.NSWE_WEST))
		{
			nswe = (short) (nswe | Cell.NSWE_WEST);
		}
		
		return nswe;
	}
	
	@Override
	public int getNearestZ(int geoX, int geoY, int worldZ)
	{
		return getCellHeight(geoX, geoY);
	}
	
	@Override
	public int getNextLowerZ(int geoX, int geoY, int worldZ)
	{
		final int cellHeight = getCellHeight(geoX, geoY);
		return cellHeight <= worldZ ? cellHeight : worldZ;
	}
	
	@Override
	public int getNextHigherZ(int geoX, int geoY, int worldZ)
	{
		final int cellHeight = getCellHeight(geoX, geoY);
		return cellHeight >= worldZ ? cellHeight : worldZ;
	}
	
	public short[] getData()
	{
		return _data;
	}
}
