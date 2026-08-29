package org.l2jmobius.gameserver.managers;

import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.holders.TradeItem;
import org.l2jmobius.gameserver.network.holders.TradeList;
import org.l2jmobius.gameserver.network.serverpackets.TradeOtherAdd;
import org.l2jmobius.gameserver.network.serverpackets.TradeOwnAdd;
import org.l2jmobius.gameserver.network.serverpackets.TradeUpdate;

import java.util.Map;

/**
 * Handles item exchange via /trade between two different Player entities, which includes phantoms and the actual human player
 *
 * Why this class exists:
 * Let's take a look at this code snippet
 * { @snippet :
 *   npc.startTrade(owner);
 * 	 items.forEach((itemId, itemCount) -> npc.getActiveTradeList().addItemByItemId(itemId, itemCount, 0));
 * 	 npc.getActiveTradeList().confirm()
 * }
 *
 * This snippet will create a valid exchange between two players, BUT the receiving party won't be able to see a single item in the trade window,
 * which makes the whole process completely unimmersive. To fix this, clients have to send corresponding packets to the server, which, in turn,
 * sends its own packets to clients.
 */
public class PhantomExchangeManager
{

    protected PhantomExchangeManager() {
    }

    void runTrade(Player sender, Player receiver, Map<Integer, Integer> itemObjIdsToCounts)
    {
        sender.startTrade(receiver);
        itemObjIdsToCounts.forEach((itemObjectId, itemCount) -> addTradeItem(sender, itemObjectId, itemCount));
        sender.getActiveTradeList().confirm();
    }

    private void addTradeItem(
            Player sender,
            int itemObjectId,
            int count
    )
    {
        final TradeList trade = sender.getActiveTradeList();
        if (trade == null)
        {
            PacketLogger.warning("Character: " + sender.getName() + " requested item:" + itemObjectId + " add without active tradelist:" + sender.getActiveTradeList().getTitle());
            return;
        }

        final Player partner = trade.getPartner();
        if ((partner == null) || (World.getInstance().getPlayer(partner.getObjectId()) == null) || (partner.getActiveTradeList() == null))
        {
            // Trade partner not found, cancel trade
            if (partner != null)
            {
                PacketLogger.warning("Character:" + sender.getName() + " requested invalid trade object: " + itemObjectId);
            }

            sender.sendPacket(SystemMessageId.THAT_PLAYER_IS_NOT_ONLINE);
            sender.cancelActiveTrade();
            return;
        }

        if (trade.isConfirmed() || partner.getActiveTradeList().isConfirmed())
        {
            sender.sendPacket(SystemMessageId.YOU_MAY_NO_LONGER_ADJUST_ITEMS_IN_THE_TRADE_BECAUSE_THE_TRADE_HAS_BEEN_CONFIRMED);
            return;
        }

        if (!sender.getAccessLevel().allowTransaction())
        {
            sender.sendMessage("Transactions are disabled for your Access Level.");
            sender.cancelActiveTrade();
            return;
        }

        if (!sender.validateItemManipulation(itemObjectId, ItemProcessType.TRANSFER))
        {
            sender.sendPacket(SystemMessageId.NOTHING_HAPPENED);
            return;
        }

        final TradeItem item = trade.addItem(itemObjectId, count);
        if (item != null)
        {
            sender.sendPacket(new TradeOwnAdd(item));
            sender.sendPacket(new TradeUpdate(trade, sender));
            trade.getPartner().sendPacket(new TradeOtherAdd(item));
        }
    }

    public static PhantomExchangeManager newInstance()
    {
        return new PhantomExchangeManager();
    }
}
