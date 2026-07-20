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
package org.l2jmobius.gameserver.network;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import org.l2jmobius.gameserver.config.DevelopmentConfig;
import org.l2jmobius.gameserver.network.clientpackets.*;

/**
 * @author Mobius
 */
public enum ClientPackets
{
	PROTOCOL_VERSION(0x00, ProtocolVersion::new, ConnectionState.CONNECTED),
	AUTH_LOGIN(0x08, AuthLogin::new, ConnectionState.CONNECTED),
	LOGOUT(0x09, Logout::new, ConnectionState.AUTHENTICATED, ConnectionState.IN_GAME),
	CHARACTER_CREATE(0x0B, CharacterCreate::new, ConnectionState.AUTHENTICATED),
	CHARACTER_DELETE(0x0C, CharacterDelete::new, ConnectionState.AUTHENTICATED),
	CHARACTER_SELECT(0x0D, CharacterSelect::new, ConnectionState.AUTHENTICATED),
	NEW_CHARACTER(0x0E, NewCharacter::new, ConnectionState.AUTHENTICATED),
	CHARACTER_RESTORE(0x62, CharacterRestore::new, ConnectionState.AUTHENTICATED),
	REQUEST_PLEDGE_CREST(0x68, RequestPledgeCrest::new, ConnectionState.AUTHENTICATED, ConnectionState.IN_GAME),
	ENTER_WORLD(0x03, EnterWorld::new, ConnectionState.ENTERING),
	MOVE_TO_LOCATION(0x01, MoveToLocation::new, ConnectionState.IN_GAME),
	ACTION(0x04, Action::new, ConnectionState.IN_GAME),
	ATTACK_REQUEST(0x0A, AttackRequest::new, ConnectionState.IN_GAME),
	REQUEST_ITEM_LIST(0x0F, RequestItemList::new, ConnectionState.IN_GAME),
	REQUEST_UN_EQUIP_ITEM(0x11, RequestUnEquipItem::new, ConnectionState.IN_GAME),
	REQUEST_DROP_ITEM(0x12, RequestDropItem::new, ConnectionState.IN_GAME),
	USE_ITEM(0x14, UseItem::new, ConnectionState.IN_GAME),
	TRADE_REQUEST(0x15, TradeRequest::new, ConnectionState.IN_GAME),
	ADD_TRADE_ITEM(0x16, AddTradeItem::new, ConnectionState.IN_GAME),
	TRADE_DONE(0x17, TradeDone::new, ConnectionState.IN_GAME),
	REQUEST_SOCIAL_ACTION(0x1B, RequestSocialAction::new, ConnectionState.IN_GAME),
	CHANGE_MOVE_TYPE2(0x1C, ChangeMoveType2::new, ConnectionState.IN_GAME),
	CHANGE_WAIT_TYPE2(0x1D, ChangeWaitType2::new, ConnectionState.IN_GAME),
	REQUEST_SELL_ITEM(0x1E, RequestSellItem::new, ConnectionState.IN_GAME),
	REQUEST_BUY_ITEM(0x1F, RequestBuyItem::new, ConnectionState.IN_GAME),
	REQUEST_LINK_HTML(0x20, RequestLinkHtml::new, ConnectionState.IN_GAME),
	REQUEST_BYPASS_TO_SERVER(0x21, RequestBypassToServer::new, ConnectionState.IN_GAME),
	REQUEST_B_B_SWRITE(0x22, RequestBBSwrite::new, ConnectionState.IN_GAME),
	REQUEST_JOIN_PLEDGE(0x24, RequestJoinPledge::new, ConnectionState.IN_GAME),
	REQUEST_ANSWER_JOIN_PLEDGE(0x25, RequestAnswerJoinPledge::new, ConnectionState.IN_GAME),
	REQUEST_WITHDRAWAL_PLEDGE(0x26, RequestWithdrawalPledge::new, ConnectionState.IN_GAME),
	REQUEST_OUST_PLEDGE_MEMBER(0x27, RequestOustPledgeMember::new, ConnectionState.IN_GAME),
	REQUEST_JOIN_PARTY(0x29, RequestJoinParty::new, ConnectionState.IN_GAME),
	REQUEST_ANSWER_JOIN_PARTY(0x2A, RequestAnswerJoinParty::new, ConnectionState.IN_GAME),
	REQUEST_WITH_DRAWAL_PARTY(0x2B, RequestWithDrawalParty::new, ConnectionState.IN_GAME),
	REQUEST_OUST_PARTY_MEMBER(0x2C, RequestOustPartyMember::new, ConnectionState.IN_GAME),
	REQUEST_MAGIC_SKILL_USE(0x2F, RequestMagicSkillUse::new, ConnectionState.IN_GAME),
	APPEARING(0x30, Appearing::new, ConnectionState.IN_GAME),
	SEND_WARE_HOUSE_DEPOSIT_LIST(0x31, SendWareHouseDepositList::new, ConnectionState.IN_GAME),
	SEND_WARE_HOUSE_WITH_DRAW_LIST(0x32, SendWareHouseWithDrawList::new, ConnectionState.IN_GAME),
	REQUEST_SHORT_CUT_REG(0x33, RequestShortcutReg::new, ConnectionState.IN_GAME),
	REQUEST_SHORT_CUT_DEL(0x35, RequestShortcutDel::new, ConnectionState.IN_GAME),
	CANNOT_MOVE_ANYMORE(0x36, CannotMoveAnymore::new, ConnectionState.IN_GAME),
	REQUEST_TARGET_CANCELD(0x37, RequestTargetCanceld::new, ConnectionState.IN_GAME),
	SAY2(0x38, Say2::new, ConnectionState.IN_GAME),
	REQUEST_PLEDGE_MEMBER_LIST(0x3C, RequestPledgeMemberList::new, ConnectionState.IN_GAME),
	REQUEST_SKILL_LIST(0x3F, RequestSkillList::new, ConnectionState.IN_GAME),
	MOVE_WITH_DELTA(0x41, MoveWithDelta::new, ConnectionState.IN_GAME),
	REQUEST_GET_ON_VEHICLE(0x42, RequestGetOnVehicle::new, ConnectionState.IN_GAME),
	REQUEST_GET_OFF_VEHICLE(0x43, RequestGetOffVehicle::new, ConnectionState.IN_GAME),
	ANSWER_TRADE_REQUEST(0x44, AnswerTradeRequest::new, ConnectionState.IN_GAME),
	REQUEST_ACTION_USE(0x45, RequestActionUse::new, ConnectionState.IN_GAME),
	REQUEST_RESTART(0x46, RequestRestart::new, ConnectionState.IN_GAME),
	REQUEST_SIEGE_INFO(0x47, RequestSiegeInfo::new, ConnectionState.IN_GAME),
	VALIDATE_POSITION(0x48, ValidatePosition::new, ConnectionState.IN_GAME),
	START_ROTATING(0x4A, StartRotating::new, ConnectionState.IN_GAME),
	FINISH_ROTATING(0x4B, FinishRotating::new, ConnectionState.IN_GAME),
	REQUEST_START_PLEDGE_WAR(0x4D, RequestStartPledgeWar::new, ConnectionState.IN_GAME),
	REQUEST_REPLY_START_PLEDGE_WAR(0x4E, RequestReplyStartPledgeWar::new, ConnectionState.IN_GAME),
	REQUEST_STOP_PLEDGE_WAR(0x4F, RequestStopPledgeWar::new, ConnectionState.IN_GAME),
	REQUEST_REPLY_STOP_PLEDGE_WAR(0x50, RequestReplyStopPledgeWar::new, ConnectionState.IN_GAME),
	REQUEST_SURRENDER_PLEDGE_WAR(0x51, RequestSurrenderPledgeWar::new, ConnectionState.IN_GAME),
	REQUEST_REPLY_SURRENDER_PLEDGE_WAR(0x52, RequestReplySurrenderPledgeWar::new, ConnectionState.IN_GAME),
	REQUEST_SET_PLEDGE_CREST(0x53, RequestSetPledgeCrest::new, ConnectionState.IN_GAME),
	REQUEST_GIVE_NICK_NAME(0x55, RequestGiveNickName::new, ConnectionState.IN_GAME),
	REQUEST_SHOW_BOARD(0x57, RequestShowBoard::new, ConnectionState.IN_GAME),
	REQUEST_ENCHANT_ITEM(0x58, RequestEnchantItem::new, ConnectionState.IN_GAME),
	REQUEST_DESTROY_ITEM(0x59, RequestDestroyItem::new, ConnectionState.IN_GAME),
	SEND_BYPASS_BUILD_CMD(0x5B, SendBypassBuildCmd::new, ConnectionState.IN_GAME),
	REQUEST_MOVE_TO_LOCATION_IN_VEHICLE(0x5C, RequestMoveToLocationInVehicle::new, ConnectionState.IN_GAME),
	CANNOT_MOVE_ANYMORE_IN_VEHICLE(0x5D, CannotMoveAnymoreInVehicle::new, ConnectionState.IN_GAME),
	REQUEST_FRIEND_INVITE(0x5E, RequestFriendInvite::new, ConnectionState.IN_GAME),
	REQUEST_ANSWER_FRIEND_INVITE(0x5F, RequestAnswerFriendInvite::new, ConnectionState.IN_GAME),
	REQUEST_FRIEND_LIST(0x60, RequestFriendList::new, ConnectionState.IN_GAME),
	REQUEST_FRIEND_DEL(0x61, RequestFriendDel::new, ConnectionState.IN_GAME),
	REQUEST_QUEST_LIST(0x63, RequestQuestList::new, ConnectionState.IN_GAME),
	REQUEST_QUEST_ABORT(0x64, RequestQuestAbort::new, ConnectionState.IN_GAME),
	REQUEST_PLEDGE_INFO(0x66, RequestPledgeInfo::new, ConnectionState.IN_GAME),
	REQUEST_PLEDGE_EXTENDED_INFO(0x67, RequestPledgeExtendedInfo::new, ConnectionState.IN_GAME),
	REQUEST_SURRENDER_PERSONALLY(0x69, null, ConnectionState.IN_GAME), // TODO: Compare to C6 branch.
	REQUEST_ACQUIRE_SKILL_INFO(0x6B, RequestAcquireSkillInfo::new, ConnectionState.IN_GAME),
	REQUEST_ACQUIRE_SKILL(0x6C, RequestAcquireSkill::new, ConnectionState.IN_GAME),
	REQUEST_RESTART_POINT(0x6D, RequestRestartPoint::new, ConnectionState.IN_GAME),
	REQUEST_G_M_COMMAND(0x6E, RequestGMCommand::new, ConnectionState.IN_GAME),
	REQUEST_PARTY_MATCH_CONFIG(0x6F, RequestPartyMatchConfig::new, ConnectionState.IN_GAME),
	REQUEST_PARTY_MATCH_LIST(0x70, RequestPartyMatchList::new, ConnectionState.IN_GAME),
	REQUEST_PARTY_MATCH_DETAIL(0x71, RequestPartyMatchDetail::new, ConnectionState.IN_GAME),
	REQUEST_CRYSTALLIZE_ITEM(0x72, RequestCrystallizeItem::new, ConnectionState.IN_GAME),
	REQUEST_PRIVATE_STORE_MANAGE_SELL(0x73, RequestPrivateStoreManageSell::new, ConnectionState.IN_GAME),
	SET_PRIVATE_STORE_LIST_SELL(0x74, SetPrivateStoreListSell::new, ConnectionState.IN_GAME),
	REQUEST_PRIVATE_STORE_QUIT_SELL(0x76, RequestPrivateStoreQuitSell::new, ConnectionState.IN_GAME),
	SET_PRIVATE_STORE_MSG_SELL(0x77, SetPrivateStoreMsgSell::new, ConnectionState.IN_GAME),
	REQUEST_PRIVATE_STORE_BUY(0x79, RequestPrivateStoreBuy::new, ConnectionState.IN_GAME),
	REQUEST_TUTORIAL_LINK_HTML(0x7B, RequestTutorialLinkHtml::new, ConnectionState.IN_GAME),
	REQUEST_TUTORIAL_PASS_CMD_TO_SERVER(0x7C, RequestTutorialPassCmdToServer::new, ConnectionState.IN_GAME),
	REQUEST_TUTORIAL_QUESTION_MARK(0x7D, RequestTutorialQuestionMark::new, ConnectionState.IN_GAME),
	REQUEST_TUTORIAL_CLIENT_EVENT(0x7E, RequestTutorialClientEvent::new, ConnectionState.IN_GAME),
	REQUEST_PETITION(0x7F, RequestPetition::new, ConnectionState.IN_GAME),
	REQUEST_PETITION_CANCEL(0x80, RequestPetitionCancel::new, ConnectionState.IN_GAME),
	REQUEST_GM_LIST(0x81, RequestGmList::new, ConnectionState.IN_GAME),
	REQUEST_JOIN_ALLY(0x82, RequestJoinAlly::new, ConnectionState.IN_GAME),
	REQUEST_ANSWER_JOIN_ALLY(0x83, RequestAnswerJoinAlly::new, ConnectionState.IN_GAME),
	ALLY_LEAVE(0x84, AllyLeave::new, ConnectionState.IN_GAME),
	ALLY_DISMISS(0x85, AllyDismiss::new, ConnectionState.IN_GAME),
	REQUEST_DISMISS_ALLY(0x86, RequestDismissAlly::new, ConnectionState.IN_GAME),
	REQUEST_SET_ALLY_CREST(0x87, RequestSetAllyCrest::new, ConnectionState.IN_GAME),
	REQUEST_ALLY_CREST(0x88, RequestAllyCrest::new, ConnectionState.IN_GAME),
	REQUEST_CHANGE_PET_NAME(0x89, RequestChangePetName::new, ConnectionState.IN_GAME),
	REQUEST_PET_USE_ITEM(0x8A, RequestPetUseItem::new, ConnectionState.IN_GAME),
	REQUEST_GIVE_ITEM_TO_PET(0x8B, RequestGiveItemToPet::new, ConnectionState.IN_GAME),
	REQUEST_GET_ITEM_FROM_PET(0x8C, RequestGetItemFromPet::new, ConnectionState.IN_GAME),
	REQUEST_ALLY_INFO(0x8E, RequestAllyInfo::new, ConnectionState.IN_GAME),
	REQUEST_PET_GET_ITEM(0x8F, RequestPetGetItem::new, ConnectionState.IN_GAME),
	REQUEST_PRIVATE_STORE_MANAGE_BUY(0x90, RequestPrivateStoreManageBuy::new, ConnectionState.IN_GAME),
	SET_PRIVATE_STORE_LIST_BUY(0x91, SetPrivateStoreListBuy::new, ConnectionState.IN_GAME),
	REQUEST_PRIVATE_STORE_QUIT_BUY(0x93, RequestPrivateStoreQuitBuy::new, ConnectionState.IN_GAME),
	SET_PRIVATE_STORE_MSG_BUY(0x94, SetPrivateStoreMsgBuy::new, ConnectionState.IN_GAME),
	REQUEST_PRIVATE_STORE_SELL(0x96, RequestPrivateStoreSell::new, ConnectionState.IN_GAME),
	REQUEST_SKILL_COOL_TIME(0x9D, RequestSkillCoolTime::new, ConnectionState.IN_GAME),
	REQUEST_PACKAGE_SENDABLE_ITEM_LIST(0x9E, RequestPackageSendableItemList::new, ConnectionState.IN_GAME),
	REQUEST_PACKAGE_SEND(0x9F, RequestPackageSend::new, ConnectionState.IN_GAME),
	REQUEST_BLOCK(0xA0, RequestBlock::new, ConnectionState.IN_GAME),
	REQUEST_SIEGE_ATTACKER_LIST(0xA2, RequestSiegeAttackerList::new, ConnectionState.IN_GAME),
	REQUEST_SIEGE_DEFENDER_LIST(0xA3, RequestSiegeDefenderList::new, ConnectionState.IN_GAME),
	REQUEST_JOIN_SIEGE(0xA4, RequestJoinSiege::new, ConnectionState.IN_GAME),
	REQUEST_CONFIRM_SIEGE_WAITING_LIST(0xA5, RequestConfirmSiegeWaitingList::new, ConnectionState.IN_GAME),
	MULTI_SELL_CHOOSE(0xA7, MultiSellChoose::new, ConnectionState.IN_GAME),
	NET_PING(0xA8, RequestNetPing::new, ConnectionState.IN_GAME),
	BYPASS_USER_CMD(0xAA, BypassUserCmd::new, ConnectionState.IN_GAME),
	SNOOP_QUIT(0xAB, SnoopQuit::new, ConnectionState.IN_GAME),
	REQUEST_RECIPE_BOOK_OPEN(0xAC, RequestRecipeBookOpen::new, ConnectionState.IN_GAME),
	REQUEST_RECIPE_BOOK_DESTROY(0xAD, RequestRecipeBookDestroy::new, ConnectionState.IN_GAME),
	REQUEST_RECIPE_ITEM_MAKE_INFO(0xAE, RequestRecipeItemMakeInfo::new, ConnectionState.IN_GAME),
	REQUEST_RECIPE_ITEM_MAKE_SELF(0xAF, RequestRecipeItemMakeSelf::new, ConnectionState.IN_GAME),
	REQUEST_RECIPE_SHOP_MESSAGE_SET(0xB1, RequestRecipeShopMessageSet::new, ConnectionState.IN_GAME),
	REQUEST_RECIPE_SHOP_LIST_SET(0xB2, RequestRecipeShopListSet::new, ConnectionState.IN_GAME),
	REQUEST_RECIPE_SHOP_MANAGE_QUIT(0xB3, RequestRecipeShopManageQuit::new, ConnectionState.IN_GAME),
	REQUEST_RECIPE_SHOP_MAKE_INFO(0xB5, RequestRecipeShopMakeInfo::new, ConnectionState.IN_GAME),
	REQUEST_RECIPE_SHOP_MAKE_ITEM(0xB6, RequestRecipeShopMakeItem::new, ConnectionState.IN_GAME),
	REQUEST_RECIPE_SHOP_MANAGE_PREV(0xB7, RequestRecipeShopManagePrev::new, ConnectionState.IN_GAME),
	OBSERVER_RETURN(0xB8, ObserverReturn::new, ConnectionState.IN_GAME),
	REQUEST_EVALUATE(0xB9, RequestEvaluate::new, ConnectionState.IN_GAME),
	REQUEST_HENNA_ITEM_LIST(0xBA, RequestHennaItemList::new, ConnectionState.IN_GAME),
	REQUEST_HENNA_ITEM_INFO(0xBB, RequestHennaItemInfo::new, ConnectionState.IN_GAME),
	REQUEST_HENNA_EQUIP(0xBC, RequestHennaEquip::new, ConnectionState.IN_GAME),
	REQUEST_HENNA_REMOVE_LIST(0xBD, RequestHennaRemoveList::new, ConnectionState.IN_GAME),
	REQUEST_HENNA_ITEM_REMOVE_INFO(0xBE, RequestHennaItemRemoveInfo::new, ConnectionState.IN_GAME),
	REQUEST_HENNA_REMOVE(0xBF, RequestHennaRemove::new, ConnectionState.IN_GAME),
	REQUEST_PLEDGE_POWER(0xC0, RequestPledgePower::new, ConnectionState.IN_GAME),
	REQUEST_MAKE_MACRO(0xC1, RequestMakeMacro::new, ConnectionState.IN_GAME),
	REQUEST_DELETE_MACRO(0xC2, RequestDeleteMacro::new, ConnectionState.IN_GAME),
	REQUEST_BUY_PROCURE(0xC3, null, ConnectionState.IN_GAME), // TODO: Compare to C6 branch.
	REQUEST_BUY_SEED(0xC4, RequestBuySeed::new, ConnectionState.IN_GAME),
	DLG_ANSWER(0xC5, DlgAnswer::new, ConnectionState.IN_GAME),
	REQUEST_PREVIEW_ITEM(0xC6, RequestPreviewItem::new, ConnectionState.IN_GAME),
	REQUEST_S_S_Q_STATUS(0xC7, RequestSSQStatus::new, ConnectionState.IN_GAME),
	GAME_GUARD_REPLY(0xCA, GameGuardReply::new, ConnectionState.IN_GAME),
	REQUEST_SEND_FRIEND_MSG(0xCC, RequestSendFriendMsg::new, ConnectionState.IN_GAME),
	REQUEST_SHOW_MINI_MAP(0xCD, RequestShowMiniMap::new, ConnectionState.IN_GAME),
	REQUEST_RECORD_INFO(0xCF, RequestRecordInfo::new, ConnectionState.IN_GAME),
	EX_PACKET(0xD0, null, ConnectionState.values()); // This packet has its own connection state checking so we allow all of them.
	
	public static final ClientPackets[] PACKET_ARRAY;
	static
	{
		final int maxPacketId = Arrays.stream(values()).mapToInt(ClientPackets::getPacketId).max().orElse(0);
		PACKET_ARRAY = new ClientPackets[maxPacketId + 1];
		for (ClientPackets packet : values())
		{
			PACKET_ARRAY[packet.getPacketId()] = packet;
		}
	}
	
	private final int _packetId;
	private final Supplier<ClientPacket> _packetSupplier;
	private final Set<ConnectionState> _connectionStates;
	
	ClientPackets(int packetId, Supplier<ClientPacket> packetSupplier, ConnectionState... connectionStates)
	{
		// Packet id is an unsigned byte.
		if (packetId > 0xFF)
		{
			throw new IllegalArgumentException("Packet id must not be bigger than 0xFF");
		}
		
		_packetId = packetId;
		_packetSupplier = packetSupplier != null ? packetSupplier : () -> null;
		_connectionStates = new HashSet<>(Arrays.asList(connectionStates));
	}
	
	public int getPacketId()
	{
		return _packetId;
	}
	
	public ClientPacket newPacket()
	{
		final ClientPacket packet = _packetSupplier.get();
		if (DevelopmentConfig.DEBUG_CLIENT_PACKETS)
		{
			if (packet != null)
			{
				final String name = packet.getClass().getSimpleName();
				if (!DevelopmentConfig.EXCLUDED_DEBUG_PACKETS.contains(name))
				{
					PacketLogger.info("[C] " + name);
				}
			}
			else if (DevelopmentConfig.DEBUG_UNKNOWN_PACKETS)
			{
				PacketLogger.info("[C] 0x" + Integer.toHexString(_packetId).toUpperCase());
			}
		}
		
		return packet;
	}
	
	public Set<ConnectionState> getConnectionStates()
	{
		return _connectionStates;
	}
}
