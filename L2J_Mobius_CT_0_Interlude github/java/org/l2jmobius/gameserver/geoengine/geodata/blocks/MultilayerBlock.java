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
public class MultilayerBlock implements IBlock
{
	private final byte[] _data;
	
	/**
	 * Initializes a new instance of this block reading the specified buffer.
	 * @param bb the buffer
	 */
	public MultilayerBlock(ByteBuffer bb)
	{
		final int start = bb.position();
		
		for (int blockCellOffset = 0; blockCellOffset < IBlock.BLOCK_CELLS; blockCellOffset++)
		{
			final byte nLayers = bb.get();
			if ((nLayers <= 0) || (nLayers > 125))
			{
				throw new RuntimeException("L2JGeoDriver: Geo file corrupted! Invalid layers count!");
			}
			
			bb.position(bb.position() + (nLayers * 2));
		}
		
		_data = new byte[bb.position() - start];
		bb.position(start);
		bb.get(_data);
	}
	
	private short getNearestLayer(int geoX, int geoY, int worldZ)
	{
		final int startOffset = getCellDataOffset(geoX, geoY);
		final byte nLayers = _data[startOffset];
		final int endOffset = startOffset + 1 + (nLayers * 2);
		
		// One layer at least was required on loading so this is set at least once on the loop below.
		int nearestDZ = 0;
		short nearestData = 0;
		for (int offset = startOffset + 1; offset < endOffset; offset += 2)
		{
			final short layerData = extractLayerData(offset);
			final int layerZ = extractLayerHeight(layerData);
			if (layerZ == worldZ)
			{
				// Exact z.
				return layerData;
			}
			
			final int layerDZ = Math.abs(layerZ - worldZ);
			if ((offset == (startOffset + 1)) || (layerDZ < nearestDZ))
			{
				nearestDZ = layerDZ;
				nearestData = layerData;
			}
		}
		
		return nearestData;
	}
	
	private int getCellDataOffset(int geoX, int geoY)
	{
		final int cellLocalOffset = ((geoX % IBlock.BLOCK_CELLS_X) * IBlock.BLOCK_CELLS_Y) + (geoY % IBlock.BLOCK_CELLS_Y);
		int cellDataOffset = 0;
		
		// Move index to cell, we need to parse on each request, OR we parse on creation and save indexes.
		for (int i = 0; i < cellLocalOffset; i++)
		{
			cellDataOffset += 1 + (_data[cellDataOffset] * 2);
		}
		
		// Now the index points to the cell we need.
		
		return cellDataOffset;
	}
	
	private short extractLayerData(int dataOffset)
	{
		return (short) ((_data[dataOffset] & 0xff) | (_data[dataOffset + 1] << 8));
	}
	
	private int getNearestNSWE(int geoX, int geoY, int worldZ)
	{
		return extractLayerNswe(getNearestLayer(geoX, geoY, worldZ));
	}
	
	@Override
	public boolean checkNearestNswe(int geoX, int geoY, int worldZ, int nswe)
	{
		return (getNearestNSWE(geoX, geoY, worldZ) & nswe) == nswe;
	}
	
	@Override
	public void setNearestNswe(int geoX, int geoY, int worldZ, byte nswe)
	{
		final int startOffset = getCellDataOffset(geoX, geoY);
		final byte nLayers = _data[startOffset];
		final int endOffset = startOffset + 1 + (nLayers * 2);
		
		int nearestDZ = 0;
		int nearestLayerZ = 0;
		int nearestOffset = 0;
		for (int offset = startOffset + 1; offset < endOffset; offset += 2)
		{
			final short layerData = extractLayerData(offset);
			final int layerZ = extractLayerHeight(layerData);
			if (layerZ == worldZ)
			{
				nearestLayerZ = layerZ;
				nearestOffset = offset;
				break;
			}
			
			final int layerDZ = Math.abs(layerZ - worldZ);
			if ((offset == (startOffset + 1)) || (layerDZ < nearestDZ))
			{
				nearestDZ = layerDZ;
				nearestLayerZ = layerZ;
				nearestOffset = offset;
			}
		}
		
		final short currentNswe = getNearestNswe(geoX, geoY, worldZ);
		if ((currentNswe & nswe) == 0)
		{
			final short encodedHeight = (short) (nearestLayerZ << 1); // Shift left by 1 bit.
			final short newNswe = (short) (currentNswe | nswe); // Combine NSWE.
			final short newCombinedData = (short) (encodedHeight | newNswe); // Combine height and NSWE.
			_data[nearestOffset] = (byte) (newCombinedData & 0xff); // Update the first byte at offset.
			_data[nearestOffset + 1] = (byte) ((newCombinedData >> 8) & 0xff); // Update the second byte at offset + 1.
		}
	}
	
	@Override
	public void unsetNearestNswe(int geoX, int geoY, int worldZ, byte nswe)
	{
		final int startOffset = getCellDataOffset(geoX, geoY);
		final byte nLayers = _data[startOffset];
		final int endOffset = startOffset + 1 + (nLayers * 2);
		
		int nearestDZ = 0;
		int nearestLayerZ = 0;
		int nearestOffset = 0;
		for (int offset = startOffset + 1; offset < endOffset; offset += 2)
		{
			final short layerData = extractLayerData(offset);
			final int layerZ = extractLayerHeight(layerData);
			if (layerZ == worldZ)
			{
				nearestLayerZ = layerZ;
				nearestOffset = offset;
				break;
			}
			
			final int layerDZ = Math.abs(layerZ - worldZ);
			if ((offset == (startOffset + 1)) || (layerDZ < nearestDZ))
			{
				nearestDZ = layerDZ;
				nearestLayerZ = layerZ;
				nearestOffset = offset;
			}
		}
		
		final short currentNswe = getNearestNswe(geoX, geoY, worldZ);
		if ((currentNswe & nswe) != 0)
		{
			final short encodedHeight = (short) (nearestLayerZ << 1); // Shift left by 1 bit.
			final short newNswe = (short) (currentNswe & ~nswe); // Subtract NSWE.
			final short newCombinedData = (short) (encodedHeight | newNswe); // Combine height and NSWE.
			_data[nearestOffset] = (byte) (newCombinedData & 0xff); // Update the first byte at offset.
			_data[nearestOffset + 1] = (byte) ((newCombinedData >> 8) & 0xff); // Update the second byte at offset + 1.
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
	
	private int extractLayerNswe(short layer)
	{
		return (byte) (layer & 0x000f);
	}
	
	private int extractLayerHeight(short layer)
	{
		return (short) (layer & 0x0fff0) >> 1;
	}
	
	@Override
	public int getNearestZ(int geoX, int geoY, int worldZ)
	{
		// Get the height of the nearest terrain layer.
		final int nearestZ = extractLayerHeight(getNearestLayer(geoX, geoY, worldZ));
		
		// If the nearest layer is more than 1000 units above current position (likely a different floor/level).
		if ((nearestZ - worldZ) > 1000)
		{
			// Find the next lower valid height instead to prevent unreachable positions.
			return getNextLowerZ(geoX, geoY, worldZ);
		}
		
		return nearestZ;
	}
	
	@Override
	public int getNextLowerZ(int geoX, int geoY, int worldZ)
	{
		final int startOffset = getCellDataOffset(geoX, geoY);
		final byte nLayers = _data[startOffset];
		final int endOffset = startOffset + 1 + (nLayers * 2);
		
		int lowerZ = Integer.MIN_VALUE;
		for (int offset = startOffset + 1; offset < endOffset; offset += 2)
		{
			final short layerData = extractLayerData(offset);
			
			final int layerZ = extractLayerHeight(layerData);
			if (layerZ == worldZ)
			{
				// Exact z.
				return layerZ;
			}
			
			if ((layerZ < worldZ) && (layerZ > lowerZ))
			{
				lowerZ = layerZ;
			}
		}
		
		return lowerZ == Integer.MIN_VALUE ? worldZ : lowerZ;
	}
	
	@Override
	public int getNextHigherZ(int geoX, int geoY, int worldZ)
	{
		final int startOffset = getCellDataOffset(geoX, geoY);
		final byte nLayers = _data[startOffset];
		final int endOffset = startOffset + 1 + (nLayers * 2);
		
		int higherZ = Integer.MAX_VALUE;
		for (int offset = startOffset + 1; offset < endOffset; offset += 2)
		{
			final short layerData = extractLayerData(offset);
			
			final int layerZ = extractLayerHeight(layerData);
			if (layerZ == worldZ)
			{
				// Exact z.
				return layerZ;
			}
			
			if ((layerZ > worldZ) && (layerZ < higherZ))
			{
				higherZ = layerZ;
			}
		}
		
		return higherZ == Integer.MAX_VALUE ? worldZ : higherZ;
	}
	
	public byte[] getData()
	{
		return _data;
	}
}
