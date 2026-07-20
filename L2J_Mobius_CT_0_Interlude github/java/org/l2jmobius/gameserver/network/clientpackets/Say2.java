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

import java.util.logging.Logger;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.config.custom.WalkerBotProtectionConfig;
import org.l2jmobius.gameserver.handler.ChatHandler;
import org.l2jmobius.gameserver.handler.IChatHandler;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.effects.EffectType;
import org.l2jmobius.gameserver.model.events.EventDispatcher;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerChat;
import org.l2jmobius.gameserver.model.events.returns.ChatFilterReturn;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.network.Disconnection;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.LeaveWorld;

/**
 * @version $Revision: 1.16.2.12.2.7 $ $Date: 2005/04/11 10:06:11 $
 */
public class Say2 extends ClientPacket
{
	private static Logger LOGGER_CHAT = Logger.getLogger("chat");
	
	private static final String[] WALKER_COMMAND_LIST =
	{
		"USESKILL",
		"USEITEM",
		"BUYITEM",
		"SELLITEM",
		"SAVEITEM",
		"LOADITEM",
		"MSG",
		"DELAY",
		"LABEL",
		"JMP",
		"CALL",
		"RETURN",
		"MOVETO",
		"NPCSEL",
		"NPCDLG",
		"DLGSEL",
		"CHARSTATUS",
		"POSOUTRANGE",
		"POSINRANGE",
		"GOHOME",
		"SAY",
		"EXIT",
		"PAUSE",
		"STRINDLG",
		"STRNOTINDLG",
		"CHANGEWAITTYPE",
		"FORCEATTACK",
		"ISMEMBER",
		"REQUESTJOINPARTY",
		"REQUESTOUTPARTY",
		"QUITPARTY",
		"MEMBERSTATUS",
		"CHARBUFFS",
		"ITEMCOUNT",
		"FOLLOWTELEPORT"
	};
	
	private String _text;
	private int _type;
	private String _target;
	
	@Override
	protected void readImpl()
	{
		_text = readString();
		_type = readInt();
		_target = (_type == ChatType.WHISPER.getClientId()) ? readString() : null;
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		ChatType chatType = ChatType.findByClientId(_type);
		if (chatType == null)
		{
			PacketLogger.warning("Say2: Invalid type: " + _type + " Player : " + player.getName() + " text: " + _text);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			Disconnection.of(player).storeAndDeleteWith(LeaveWorld.STATIC_PACKET);
			return;
		}
		
		if (_text.isEmpty())
		{
			PacketLogger.warning(player.getName() + ": sending empty text. Possible packet hack!");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			Disconnection.of(player).storeAndDeleteWith(LeaveWorld.STATIC_PACKET);
			return;
		}
		
		// Even though the client can handle more characters than it's current limit allows, an overflow (critical error) happens if you pass a huge (1000+) message.
		// July 11, 2011 - Verified on High Five 4 official client as 105.
		// Allow higher limit if player shift some item (text is longer then).
		if (!player.isGM() && (((_text.indexOf(8) >= 0) && (_text.length() > 500)) || ((_text.indexOf(8) < 0) && (_text.length() > 105))))
		{
			player.sendPacket(SystemMessageId.WHEN_A_USER_S_KEYBOARD_INPUT_EXCEEDS_A_CERTAIN_CUMULATIVE_SCORE_A_CHAT_BAN_WILL_BE_APPLIED_THIS_IS_DONE_TO_DISCOURAGE_SPAMMING_PLEASE_AVOID_POSTING_THE_SAME_MESSAGE_MULTIPLE_TIMES_DURING_A_SHORT_PERIOD);
			return;
		}
		
		if (WalkerBotProtectionConfig.L2WALKER_PROTECTION && (chatType == ChatType.WHISPER) && checkBot(_text))
		{
			PunishmentManager.handleIllegalPlayerAction(player, "Client Emulator Detect: " + player + " using L2Walker.", GeneralConfig.DEFAULT_PUNISH);
			return;
		}
		
		if (player.isCursedWeaponEquipped() && ((chatType == ChatType.TRADE) || (chatType == ChatType.SHOUT)))
		{
			player.sendMessage("Shout and trade chatting cannot be used while possessing a cursed weapon.");
			return;
		}
		
		if (player.isChatBanned() && (_text.charAt(0) != '.'))
		{
			if (player.getEffectList().getFirstEffect(EffectType.CHAT_BLOCK) != null)
			{
				player.sendMessage("You have been reported as an illegal program user, so chatting is not allowed.");
			}
			else if (GeneralConfig.BAN_CHAT_CHANNELS.contains(chatType))
			{
				player.sendPacket(SystemMessageId.CHATTING_IS_CURRENTLY_PROHIBITED);
			}
			return;
		}
		
		if (player.isJailed() && GeneralConfig.JAIL_DISABLE_CHAT && ((chatType == ChatType.WHISPER) || (chatType == ChatType.SHOUT) || (chatType == ChatType.TRADE) || (chatType == ChatType.HERO_VOICE)))
		{
			player.sendMessage("You can not chat with players outside of the jail.");
			return;
		}
		
		if ((chatType == ChatType.PETITION_PLAYER) && player.isGM())
		{
			chatType = ChatType.PETITION_GM;
		}
		
		if (GeneralConfig.LOG_CHAT)
		{
			final StringBuilder sb = new StringBuilder();
			sb.append(chatType.name());
			sb.append(" [");
			sb.append(player);
			if (chatType == ChatType.WHISPER)
			{
				sb.append(" to ");
				sb.append(_target);
				sb.append("] ");
				sb.append(_text);
				LOGGER_CHAT.info(sb.toString());
			}
			else
			{
				sb.append("] ");
				sb.append(_text);
				LOGGER_CHAT.info(sb.toString());
			}
		}
		
		if ((_text.indexOf(8) >= 0) && !parseAndPublishItem(player))
		{
			return;
		}
		
		if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_CHAT))
		{
			final ChatFilterReturn filter = EventDispatcher.getInstance().notifyEvent(new OnPlayerChat(player, World.getInstance().getPlayer(_target), _text, chatType), ChatFilterReturn.class);
			if (filter != null)
			{
				_text = filter.getFilteredText();
			}
		}
		
		// Say Filter implementation
		if (GeneralConfig.USE_SAY_FILTER)
		{
			checkText();
		}
		
		final IChatHandler handler = ChatHandler.getInstance().getHandler(chatType);
		if (handler != null)
		{
			handler.onChat(chatType, player, _target, _text);
		}
		else
		{
			PacketLogger.info("No handler registered for ChatType: " + _type + " Player: " + player);
		}
	}
	
	private boolean checkBot(String text)
	{
		for (String botCommand : WALKER_COMMAND_LIST)
		{
			if (text.startsWith(botCommand))
			{
				return true;
			}
		}
		
		return false;
	}
	
	private void checkText()
	{
		String filteredText = _text;
		for (String pattern : ServerConfig.FILTER_LIST)
		{
			filteredText = filteredText.replaceAll("(?i)" + pattern, GeneralConfig.CHAT_FILTER_CHARS);
		}
		
		_text = filteredText;
	}
	
	private boolean parseAndPublishItem(Player owner)
	{
		int pos1 = -1;
		while ((pos1 = _text.indexOf(8, pos1)) > -1)
		{
			int pos = _text.indexOf("ID=", pos1);
			if (pos == -1)
			{
				return false;
			}
			
			final StringBuilder result = new StringBuilder(9);
			pos += 3;
			while (Character.isDigit(_text.charAt(pos)))
			{
				result.append(_text.charAt(pos++));
			}
			
			final int id = Integer.parseInt(result.toString());
			final WorldObject item = World.getInstance().findObject(id);
			if (item instanceof Item)
			{
				if (owner.getInventory().getItemByObjectId(id) == null)
				{
					PacketLogger.info(owner.getClient() + " trying publish item which does not own! ID:" + id);
					return false;
				}
				
				((Item) item).publish();
			}
			else
			{
				return false;
			}
			
			pos1 = _text.indexOf(8, pos) + 1;
			if (pos1 == 0) // missing ending tag
			{
				PacketLogger.info(owner.getClient() + " sent invalid publish item msg! ID:" + id);
				return false;
			}
		}
		
		return true;
	}
}
