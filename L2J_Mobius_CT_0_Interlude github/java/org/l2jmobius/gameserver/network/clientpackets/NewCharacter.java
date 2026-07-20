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
package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.data.xml.PlayerTemplateData;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.network.serverpackets.NewCharacterSuccess;

/**
 * @version $Revision: 1.3.4.5 $ $Date: 2005/03/27 15:29:30 $
 */
public class NewCharacter extends ClientPacket
{
	@Override
	protected void readImpl()
	{
	}
	
	@Override
	protected void runImpl()
	{
		final NewCharacterSuccess ct = new NewCharacterSuccess();
		ct.addChar(PlayerTemplateData.getInstance().getTemplate(PlayerClass.FIGHTER)); // Human Figther
		ct.addChar(PlayerTemplateData.getInstance().getTemplate(PlayerClass.MAGE)); // Human Mystic
		ct.addChar(PlayerTemplateData.getInstance().getTemplate(PlayerClass.ELVEN_FIGHTER)); // Elven Fighter
		ct.addChar(PlayerTemplateData.getInstance().getTemplate(PlayerClass.ELVEN_MAGE)); // Elven Mystic
		ct.addChar(PlayerTemplateData.getInstance().getTemplate(PlayerClass.DARK_FIGHTER)); // Dark Fighter
		ct.addChar(PlayerTemplateData.getInstance().getTemplate(PlayerClass.DARK_MAGE)); // Dark Mystic
		ct.addChar(PlayerTemplateData.getInstance().getTemplate(PlayerClass.ORC_FIGHTER)); // Orc Fighter
		ct.addChar(PlayerTemplateData.getInstance().getTemplate(PlayerClass.ORC_MAGE)); // Orc Mystic
		ct.addChar(PlayerTemplateData.getInstance().getTemplate(PlayerClass.DWARVEN_FIGHTER)); // Dwarf Fighter
		getClient().sendPacket(ct);
	}
}
