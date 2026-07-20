/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.network.serverpackets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.config.custom.MultilingualSupportConfig;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.SystemMessageId.SMLocalisation;

/**
 * @author Kerberos
 */
public class ExShowScreenMessage extends ServerPacket
{
	// Positions
	public static final byte TOP_LEFT = 1;
	public static final byte TOP_CENTER = 2;
	public static final byte TOP_RIGHT = 3;
	public static final byte MIDDLE_LEFT = 4;
	public static final byte MIDDLE_CENTER = 5;
	public static final byte MIDDLE_RIGHT = 6;
	public static final byte BOTTOM_CENTER = 7;
	public static final byte BOTTOM_RIGHT = 8;
	
	private final int _type;
	private final int _sysMessageId;
	private final int _unk1;
	private final int _unk2;
	private final int _unk3;
	private final boolean _fade;
	private final int _size;
	private final int _position;
	private final boolean _effect;
	private final String _text;
	private final int _time;
	private List<String> _parameters;
	
	/**
	 * Display a String on the screen for a given time.
	 * @param text the text to display
	 * @param time the display time
	 */
	public ExShowScreenMessage(String text, int time)
	{
		_type = 1;
		_sysMessageId = -1;
		_unk1 = 0;
		_unk2 = 0;
		_unk3 = 0;
		_fade = false;
		_position = TOP_CENTER;
		_text = text;
		_time = time;
		_size = 0;
		_effect = false;
	}
	
	/**
	 * Display a String on the screen for a given time.
	 * @param text the text to display
	 * @param position the position on the screen
	 * @param time the display time
	 */
	public ExShowScreenMessage(String text, int position, int time)
	{
		_type = 1;
		_sysMessageId = -1;
		_unk1 = 0;
		_unk2 = 0;
		_unk3 = 0;
		_fade = false;
		_position = position;
		_text = text;
		_time = time;
		_size = 0;
		_effect = false;
	}
	
	/**
	 * Display a String on the screen for a given time.
	 * @param text the text to display
	 * @param position the position on the screen
	 * @param time the display time
	 * @param size the font size 0 - normal, 1 - small
	 * @param fade the fade effect
	 * @param showEffect upper effect
	 */
	public ExShowScreenMessage(String text, int position, int time, int size, boolean fade, boolean showEffect)
	{
		_type = 1;
		_sysMessageId = -1;
		_unk1 = 0;
		_unk2 = 0;
		_unk3 = 0;
		_fade = fade;
		_position = position;
		_text = text;
		_time = time;
		_size = size;
		_effect = showEffect;
	}
	
	/**
	 * Display a System Message on the screen for a given position and time.
	 * @param systemMsg the System Message Id
	 * @param position the position on the screen
	 * @param time the display time
	 * @param params the String parameters
	 */
	public ExShowScreenMessage(SystemMessageId systemMsg, int position, int time, String... params)
	{
		_type = 2;
		_sysMessageId = systemMsg.getId();
		_unk1 = 0x00;
		_unk2 = 0x00;
		_unk3 = 0x00;
		_fade = false;
		_position = position;
		_text = null;
		_time = time;
		_size = 0x00;
		_effect = false;
		if (params != null)
		{
			addStringParameter(params);
		}
	}
	
	/**
	 * Display a Text, System Message or a NPC String on the screen for the given parameters.
	 * @param type 0 - System Message, 1 - Text, 2 - NPC String
	 * @param messageId the System Message Id
	 * @param position the position on the screen
	 * @param unk1
	 * @param size the font size 0 - normal, 1 - small
	 * @param unk2
	 * @param unk3
	 * @param showEffect upper effect (0 - disabled, 1 enabled) - _position must be 2 (center) otherwise no effect
	 * @param time the display time
	 * @param fade the fade effect (0 - disabled, 1 enabled)
	 * @param text the text to display
	 * @param params the String parameters
	 */
	public ExShowScreenMessage(int type, int messageId, int position, int unk1, int size, int unk2, int unk3, boolean showEffect, int time, boolean fade, String text, String params)
	{
		_type = type;
		_sysMessageId = messageId;
		_unk1 = unk1;
		_unk2 = unk2;
		_unk3 = unk3;
		_fade = fade;
		_position = position;
		_text = text;
		_time = time;
		_size = size;
		_effect = showEffect;
	}
	
	/**
	 * String parameter for argument S1,S2,.. in npcstring-e.dat
	 * @param params the parameter
	 */
	public void addStringParameter(String... params)
	{
		if (_parameters == null)
		{
			_parameters = new ArrayList<>();
		}
		
		for (String param : params)
		{
			_parameters.add(param);
		}
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_SHOW_SCREEN_MESSAGE.writeId(this, buffer);
		
		// Localisation related.
		if (MultilingualSupportConfig.MULTILANG_ENABLE)
		{
			final Player player = client.getPlayer();
			if (player != null)
			{
				final String lang = player.getLang();
				if ((lang != null) && !lang.equals("en"))
				{
					if (_sysMessageId > -1)
					{
						final SystemMessageId sm = SystemMessageId.getSystemMessageId(_sysMessageId);
						if (sm != null)
						{
							final SMLocalisation sml = sm.getLocalisation(lang);
							if (sml != null)
							{
								buffer.writeInt(_type);
								buffer.writeInt(-1);
								buffer.writeInt(_position);
								buffer.writeInt(_unk1);
								buffer.writeInt(_size);
								buffer.writeInt(_unk2);
								buffer.writeInt(_unk3);
								buffer.writeInt(_effect);
								buffer.writeInt(_time);
								buffer.writeInt(_fade);
								buffer.writeInt(-1);
								buffer.writeString(sml.getLocalisation(_parameters != null ? _parameters : Collections.emptyList()));
								return;
							}
						}
					}
				}
			}
		}
		
		buffer.writeInt(_type);
		buffer.writeInt(_sysMessageId);
		buffer.writeInt(_position);
		buffer.writeInt(_unk1);
		buffer.writeInt(_size);
		buffer.writeInt(_unk2);
		buffer.writeInt(_unk3);
		buffer.writeInt(_effect);
		buffer.writeInt(_time);
		buffer.writeInt(_fade);
		buffer.writeString(_text);
		if (_sysMessageId == -1)
		{
			buffer.writeString(_text);
		}
		else if (_parameters != null)
		{
			for (String s : _parameters)
			{
				buffer.writeString(s);
			}
		}
	}
}
