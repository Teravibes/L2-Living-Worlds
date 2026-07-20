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
package org.l2jmobius.gameserver.geoengine.geodata.regions;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

import org.l2jmobius.gameserver.config.GeoEngineConfig;
import org.l2jmobius.gameserver.geoengine.geodata.Cell;
import org.l2jmobius.gameserver.geoengine.geodata.IBlock;
import org.l2jmobius.gameserver.geoengine.geodata.IRegion;
import org.l2jmobius.gameserver.geoengine.geodata.blocks.ComplexBlock;
import org.l2jmobius.gameserver.geoengine.geodata.blocks.FlatBlock;
import org.l2jmobius.gameserver.geoengine.geodata.blocks.MultilayerBlock;

/**
 * @author HorridoJoho, Mobius
 */
public class Region implements IRegion
{
	private final IBlock[] _blocks = new IBlock[IRegion.REGION_BLOCKS];
	
	public Region(ByteBuffer bb)
	{
		load(bb);
	}
	
	public void load(ByteBuffer bb)
	{
		for (int blockOffset = 0; blockOffset < IRegion.REGION_BLOCKS; blockOffset++)
		{
			final int blockType = bb.get();
			switch (blockType)
			{
				case IBlock.TYPE_FLAT:
				{
					_blocks[blockOffset] = new FlatBlock(bb);
					break;
				}
				case IBlock.TYPE_COMPLEX:
				{
					_blocks[blockOffset] = new ComplexBlock(bb);
					break;
				}
				case IBlock.TYPE_MULTILAYER:
				{
					_blocks[blockOffset] = new MultilayerBlock(bb);
					break;
				}
				default:
				{
					throw new RuntimeException("Invalid block type " + blockType + "!");
				}
			}
		}
	}
	
	private IBlock getBlock(int geoX, int geoY)
	{
		return _blocks[(((geoX / IBlock.BLOCK_CELLS_X) % IRegion.REGION_BLOCKS_X) * IRegion.REGION_BLOCKS_Y) + ((geoY / IBlock.BLOCK_CELLS_Y) % IRegion.REGION_BLOCKS_Y)];
	}
	
	@Override
	public boolean checkNearestNswe(int geoX, int geoY, int worldZ, int nswe)
	{
		return getBlock(geoX, geoY).checkNearestNswe(geoX, geoY, worldZ, nswe);
	}
	
	@Override
	public void setNearestNswe(int geoX, int geoY, int worldZ, byte nswe)
	{
		final IBlock block = getBlock(geoX, geoY);
		
		// Flat block cells are enabled by default on all directions.
		if (block instanceof FlatBlock)
		{
			// convertFlatToComplex(block, geoX, geoY);
			return;
		}
		
		getBlock(geoX, geoY).setNearestNswe(geoX, geoY, worldZ, nswe);
	}
	
	@Override
	public void unsetNearestNswe(int geoX, int geoY, int worldZ, byte nswe)
	{
		final IBlock block = getBlock(geoX, geoY);
		
		// Flat blocks are by default enabled on all locations.
		if (block instanceof FlatBlock)
		{
			convertFlatToComplex(block, geoX, geoY);
		}
		
		getBlock(geoX, geoY).unsetNearestNswe(geoX, geoY, worldZ, nswe);
	}
	
	private void convertFlatToComplex(IBlock block, int geoX, int geoY)
	{
		final short currentHeight = ((FlatBlock) block).getHeight();
		final short encodedHeight = (short) ((currentHeight << 1) & 0xffff);
		final short combinedData = (short) (encodedHeight | Cell.NSWE_ALL);
		final ByteBuffer buffer = ByteBuffer.allocate(IBlock.BLOCK_CELLS * 2);
		for (int i = 0; i < IBlock.BLOCK_CELLS; i++)
		{
			buffer.putShort(combinedData);
		}
		
		buffer.rewind();
		_blocks[(((geoX / IBlock.BLOCK_CELLS_X) % IRegion.REGION_BLOCKS_X) * IRegion.REGION_BLOCKS_Y) + ((geoY / IBlock.BLOCK_CELLS_Y) % IRegion.REGION_BLOCKS_Y)] = new ComplexBlock(buffer);
	}
	
	@Override
	public int getNearestZ(int geoX, int geoY, int worldZ)
	{
		return getBlock(geoX, geoY).getNearestZ(geoX, geoY, worldZ);
	}
	
	@Override
	public int getNextLowerZ(int geoX, int geoY, int worldZ)
	{
		return getBlock(geoX, geoY).getNextLowerZ(geoX, geoY, worldZ);
	}
	
	@Override
	public int getNextHigherZ(int geoX, int geoY, int worldZ)
	{
		return getBlock(geoX, geoY).getNextHigherZ(geoX, geoY, worldZ);
	}
	
	@Override
	public boolean hasGeo()
	{
		return true;
	}
	
	/**
	 * Saves this region to a file.
	 * @param fileName the target file name.
	 * @return true if the file was saved successfully, false otherwise.
	 */
	@Override
	public boolean saveToFile(String fileName)
	{
		final Path filePath = new File(GeoEngineConfig.GEOEDIT_PATH + File.separator + fileName).toPath();
		if (Files.exists(filePath))
		{
			try
			{
				Files.delete(filePath);
			}
			catch (IOException e)
			{
				return false;
			}
		}
		
		try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath.toFile())))
		{
			for (IBlock block : _blocks)
			{
				if (block instanceof FlatBlock)
				{
					final ByteBuffer buffer = ByteBuffer.allocate(3);
					buffer.put((byte) IBlock.TYPE_FLAT);
					buffer.putShort(Short.reverseBytes(((FlatBlock) block).getHeight()));
					bos.write(buffer.array());
				}
				else if (block instanceof ComplexBlock)
				{
					final short[] data = ((ComplexBlock) block).getData();
					final ByteBuffer buffer = ByteBuffer.allocate(1 + (data.length * 2));
					buffer.put((byte) IBlock.TYPE_COMPLEX);
					for (short info : data)
					{
						buffer.putShort(Short.reverseBytes(info));
					}
					
					bos.write(buffer.array());
				}
				else if (block instanceof MultilayerBlock)
				{
					final byte[] data = ((MultilayerBlock) block).getData();
					final ByteBuffer buffer = ByteBuffer.allocate(1 + (data.length));
					buffer.put((byte) IBlock.TYPE_MULTILAYER);
					for (byte info : data)
					{
						buffer.put(info);
					}
					
					bos.write(buffer.array());
				}
			}
		}
		catch (IOException e)
		{
			return false;
		}
		
		return true;
	}
}
