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
package handlers.chat.channels;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.custom.FactionSystemConfig;
import org.l2jmobius.gameserver.config.custom.FakePlayersConfig;
import org.l2jmobius.gameserver.data.xml.FakePlayerData;
import org.l2jmobius.gameserver.handler.IChatHandler;
import org.l2jmobius.gameserver.managers.FakePlayerChatManager;
import org.l2jmobius.gameserver.managers.PhantomBuddyManager;
import org.l2jmobius.gameserver.managers.PhantomPartyManager;
import org.l2jmobius.gameserver.managers.PhantomManager;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.holders.player.BlockList;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;

/**
 * Tell Chat Handler.
 * @author durgus
 */
public class ChatWhisper implements IChatHandler
{
	private static final ChatType[] CHAT_TYPES =
	{
		ChatType.WHISPER
	};
	
	@Override
	public void onChat(ChatType type, Player activeChar, String target, String text)
	{
		if (activeChar.isChatBanned() && GeneralConfig.BAN_CHAT_CHANNELS.contains(type))
		{
			activeChar.sendPacket(SystemMessageId.CHATTING_IS_CURRENTLY_PROHIBITED_IF_YOU_TRY_TO_CHAT_BEFORE_THE_PROHIBITION_IS_REMOVED_THE_PROHIBITION_TIME_WILL_BECOME_EVEN_LONGER);
			return;
		}
		
		if (GeneralConfig.JAIL_DISABLE_CHAT && activeChar.isJailed() && !activeChar.isGM())
		{
			activeChar.sendPacket(SystemMessageId.CHATTING_IS_CURRENTLY_PROHIBITED);
			return;
		}
		
		// Return if no target is set
		if (target == null)
		{
			return;
		}
		
		if (FakePlayersConfig.FAKE_PLAYERS_ENABLED && (FakePlayerData.getInstance().getProperName(target) != null))
		{
			if (FakePlayerData.getInstance().isTalkable(target))
			{
				if (FakePlayersConfig.FAKE_PLAYER_CHAT)
				{
					final String name = FakePlayerData.getInstance().getProperName(target);
					activeChar.sendPacket(new CreatureSay(activeChar, type, "->" + name, text));
					FakePlayerChatManager.getInstance().manageChat(activeChar, name, text);
				}
				else
				{
					activeChar.sendPacket(SystemMessageId.THAT_PERSON_IS_IN_MESSAGE_REFUSAL_MODE);
				}
			}
			else
			{
				activeChar.sendPacket(SystemMessageId.THAT_PLAYER_IS_NOT_ONLINE);
			}
			return;
		}

		// Procedurally generated fake players are not in FakePlayerData; resolve them from the live world
		// so you can whisper the roaming bots you see (AFK store vendors stay "offline" and don't reply).
		if (FakePlayersConfig.FAKE_PLAYERS_ENABLED && FakePlayersConfig.FAKE_PLAYER_CHAT)
		{
			final String botName = FakePlayerChatManager.getInstance().talkableBotName(target);
			if (botName != null)
			{
				activeChar.sendPacket(new CreatureSay(activeChar, type, "->" + botName, text));
				FakePlayerChatManager.getInstance().manageChat(activeChar, botName, text);
				return;
			}
		}

		final Player receiver = World.getInstance().getPlayer(target);

		// Support buddy: a clientless phantom you can whisper to give orders (party / follow / tp / brb / heal).
		// Routed before the offline-mode guard since a buddy has no client. The buddy answers with a whisper.
		if ((receiver != null) && PhantomManager.getInstance().isBuddy(receiver))
		{
			activeChar.sendPacket(new CreatureSay(activeChar, type, "->" + receiver.getName(), text));
			final String reply = PhantomBuddyManager.getInstance().handleWhisper(activeChar, receiver, text);
			if ((reply != null) && !reply.isEmpty())
			{
				activeChar.sendPacket(new CreatureSay(receiver, ChatType.WHISPER, receiver.getName(), reply));
			}
			return;
		}

		// Recruited combat party member: whisper it orders (assist / attack freely / follow / hold / brb / bye).
		if ((receiver != null) && PhantomManager.getInstance().isRecruit(receiver))
		{
			activeChar.sendPacket(new CreatureSay(activeChar, type, "->" + receiver.getName(), text));
			final String reply = PhantomPartyManager.getInstance().handleWhisper(activeChar, receiver, text);
			if ((reply != null) && !reply.isEmpty())
			{
				activeChar.sendPacket(new CreatureSay(receiver, ChatType.WHISPER, receiver.getName(), reply));
			}
			return;
		}

		// Befriended regular phantom (friend tier): PM it like a person - the brain answers in FRIEND mode
		// over this same whisper channel. Without this hook the clientless-target guard below reported a
		// friend as "in offline mode". Gated on the sender's friend list, so strangers still see it offline.
		if ((receiver != null) && PhantomManager.getInstance().isPhantom(receiver) && activeChar.getFriendList().contains(receiver.getObjectId()))
		{
			activeChar.sendPacket(new CreatureSay(activeChar, type, "->" + receiver.getName(), text));
			FakePlayerChatManager.getInstance().handleFriendWhisper(activeChar, receiver, text);
			return;
		}

		if ((receiver != null) && !receiver.isSilenceMode(activeChar.getObjectId()))
		{
			if (GeneralConfig.JAIL_DISABLE_CHAT && receiver.isJailed() && !activeChar.isGM())
			{
				activeChar.sendMessage("Player is in jail.");
				return;
			}
			
			if (receiver.isChatBanned())
			{
				activeChar.sendPacket(SystemMessageId.THAT_PERSON_IS_IN_MESSAGE_REFUSAL_MODE);
				return;
			}
			
			if ((receiver.getClient() == null) || receiver.getClient().isDetached())
			{
				activeChar.sendMessage("Player is in offline mode.");
				return;
			}
			
			if (FactionSystemConfig.FACTION_SYSTEM_ENABLED && FactionSystemConfig.FACTION_SPECIFIC_CHAT && ((activeChar.isGood() && receiver.isEvil()) || (activeChar.isEvil() && receiver.isGood())))
			{
				activeChar.sendMessage("Player belongs to the opposing faction.");
				return;
			}
			
			if (!BlockList.isBlocked(receiver, activeChar))
			{
				// Allow reciever to send PMs to this char, which is in silence mode.
				if (PlayerConfig.SILENCE_MODE_EXCLUDE && activeChar.isSilenceMode())
				{
					activeChar.addSilenceModeExcluded(receiver.getObjectId());
				}
				
				receiver.sendPacket(new CreatureSay(activeChar, type, activeChar.getName(), text));
				activeChar.sendPacket(new CreatureSay(activeChar, type, "->" + receiver.getName(), text));
			}
			else
			{
				activeChar.sendPacket(SystemMessageId.THAT_PERSON_IS_IN_MESSAGE_REFUSAL_MODE);
			}
		}
		else
		{
			activeChar.sendPacket(SystemMessageId.THAT_PLAYER_IS_NOT_ONLINE);
		}
	}
	
	@Override
	public ChatType[] getChatTypeList()
	{
		return CHAT_TYPES;
	}
}
