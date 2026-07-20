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
package org.l2jmobius.gameserver.network.serverpackets;

import static org.l2jmobius.gameserver.data.xml.MultisellData.PAGE_SIZE;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.model.multisell.Entry;
import org.l2jmobius.gameserver.model.multisell.Ingredient;
import org.l2jmobius.gameserver.model.multisell.ListContainer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class MultiSellList extends ServerPacket
{
	private int _size;
	private int _index;
	private final ListContainer _list;
	private final boolean _finished;
	
	public MultiSellList(ListContainer list, int index)
	{
		_list = list;
		_index = index;
		_size = list.getEntries().size() - index;
		if (_size > PAGE_SIZE)
		{
			_finished = false;
			_size = PAGE_SIZE;
		}
		else
		{
			_finished = true;
		}
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.MULTI_SELL_LIST.writeId(this, buffer);
		buffer.writeInt(_list.getListId()); // list id
		buffer.writeInt(1 + (_index / PAGE_SIZE)); // page started from 1
		buffer.writeInt(_finished); // finished
		buffer.writeInt(PAGE_SIZE); // size of pages
		buffer.writeInt(_size); // list length
		Entry ent;
		while (_size-- > 0)
		{
			ent = _list.getEntries().get(_index++);
			buffer.writeInt(ent.getEntryId());
			buffer.writeInt(0); // C6
			buffer.writeInt(0); // C6
			buffer.writeByte(1);
			buffer.writeShort(ent.getProducts().size());
			buffer.writeShort(ent.getIngredients().size());
			for (Ingredient ing : ent.getProducts())
			{
				if (ing.getTemplate() != null)
				{
					buffer.writeShort(ing.getTemplate().getDisplayId());
					buffer.writeInt(ing.getTemplate().getBodyPart().getMask());
					buffer.writeShort(ing.getTemplate().getType2());
				}
				else
				{
					buffer.writeShort(ing.getItemId());
					buffer.writeInt(0);
					buffer.writeShort(65535);
				}
				
				buffer.writeInt(ing.getItemCount());
				if (ing.getItemInfo() != null)
				{
					buffer.writeShort(ing.getItemInfo().getEnchantLevel()); // enchant level
					buffer.writeInt(ing.getItemInfo().getAugmentId()); // augment id
					buffer.writeInt(0); // mana
				}
				else
				{
					buffer.writeShort(ing.getEnchantLevel()); // enchant level
					buffer.writeInt(0); // augment id
					buffer.writeInt(0); // mana
				}
			}
			
			for (Ingredient ing : ent.getIngredients())
			{
				buffer.writeShort(ing.getTemplate() != null ? ing.getTemplate().getDisplayId() : ing.getItemId());
				buffer.writeShort(ing.getTemplate() != null ? ing.getTemplate().getType2() : 65535);
				buffer.writeInt(ing.getItemCount());
				if (ing.getItemInfo() != null)
				{
					buffer.writeShort(ing.getItemInfo().getEnchantLevel()); // enchant level
					buffer.writeInt(ing.getItemInfo().getAugmentId()); // augment id
					buffer.writeInt(0); // mana
				}
				else
				{
					buffer.writeShort(ing.getEnchantLevel()); // enchant level
					buffer.writeInt(0); // augment id
					buffer.writeInt(0); // mana
				}
			}
		}
	}
}
