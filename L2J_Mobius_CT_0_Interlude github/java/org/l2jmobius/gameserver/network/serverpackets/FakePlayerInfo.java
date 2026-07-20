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

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.enums.player.Sex;
import org.l2jmobius.gameserver.model.actor.holders.npc.FakePlayerAppearance;
import org.l2jmobius.gameserver.model.actor.holders.npc.FakePlayerHolder;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * @author Mobius
 */
public class FakePlayerInfo extends ServerPacket
{
	private final Npc _npc;
	private final int _objId;
	private final int _x;
	private final int _y;
	private final int _z;
	private final int _heading;
	private final int _mAtkSpd;
	private final int _pAtkSpd;
	private final int _runSpd;
	private final int _walkSpd;
	private final int _swimRunSpd;
	private final int _swimWalkSpd;
	private final int _flyRunSpd;
	private final int _flyWalkSpd;
	private final double _moveMultiplier;
	private final float _attackSpeedMultiplier;
	private final FakePlayerHolder _fpcHolder;
	private final FakePlayerAppearance _look; // per-instance override; null = use template
	private final Clan _clan;

	public FakePlayerInfo(Npc npc)
	{
		_npc = npc;
		_look = npc.getFakePlayerAppearance();
		_objId = npc.getObjectId();
		_x = npc.getX();
		_y = npc.getY();
		_z = npc.getZ();
		_heading = npc.getHeading();
		_mAtkSpd = npc.getMAtkSpd();
		_pAtkSpd = (int) npc.getPAtkSpd();
		_attackSpeedMultiplier = npc.getAttackSpeedMultiplier();
		_moveMultiplier = npc.getMovementSpeedMultiplier();
		_runSpd = (int) Math.round(npc.getRunSpeed() / _moveMultiplier);
		_walkSpd = (int) Math.round(npc.getWalkSpeed() / _moveMultiplier);
		_swimRunSpd = (int) Math.round(npc.getSwimRunSpeed() / _moveMultiplier);
		_swimWalkSpd = (int) Math.round(npc.getSwimWalkSpeed() / _moveMultiplier);
		_flyRunSpd = npc.isFlying() ? _runSpd : 0;
		_flyWalkSpd = npc.isFlying() ? _walkSpd : 0;
		_fpcHolder = npc.getTemplate().getFakePlayerInfo();
		_clan = _look != null ? null : ClanTable.getInstance().getClan(_fpcHolder.getClanId());
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.CHAR_INFO.writeId(this, buffer);
		buffer.writeInt(_x);
		buffer.writeInt(_y);
		buffer.writeInt(_z);
		buffer.writeInt(0); // vehicleId
		buffer.writeInt(_objId);
		buffer.writeString(_npc.getName());
		buffer.writeInt(_look != null ? _look.getRace().ordinal() : _npc.getRace().ordinal());
		buffer.writeInt(_look != null ? (_look.isFemale() ? 1 : 0) : (_npc.getTemplate().getSex() == Sex.FEMALE ? 1 : 0));
		buffer.writeInt(_look != null ? _look.getPlayerClass().getId() : _fpcHolder.getPlayerClass().getId());
		buffer.writeInt(0); // Inventory.PAPERDOLL_UNDER
		buffer.writeInt(_look != null ? _look.getEquipHead() : _fpcHolder.getEquipHead());
		buffer.writeInt(_look != null ? _look.getEquipRHand() : _fpcHolder.getEquipRHand());
		buffer.writeInt(_look != null ? _look.getEquipLHand() : _fpcHolder.getEquipLHand());
		buffer.writeInt(_look != null ? _look.getEquipGloves() : _fpcHolder.getEquipGloves());
		buffer.writeInt(_look != null ? _look.getEquipChest() : _fpcHolder.getEquipChest());
		buffer.writeInt(_look != null ? _look.getEquipLegs() : _fpcHolder.getEquipLegs());
		buffer.writeInt(_look != null ? _look.getEquipFeet() : _fpcHolder.getEquipFeet());
		buffer.writeInt(_look != null ? _look.getEquipCloak() : _fpcHolder.getEquipCloak());
		buffer.writeInt(_look != null ? _look.getEquipRHand() : _fpcHolder.getEquipRHand()); // dual hand
		buffer.writeInt(_look != null ? _look.getEquipHair() : _fpcHolder.getEquipHair());
		buffer.writeInt(_look != null ? _look.getEquipHair2() : _fpcHolder.getEquipHair2());
		
		// c6 new h's
		buffer.writeShort(0);
		buffer.writeShort(0);
		buffer.writeShort(0);
		buffer.writeShort(0);
		buffer.writeInt(0); // _player.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_RHAND)
		buffer.writeShort(0);
		buffer.writeShort(0);
		buffer.writeShort(0);
		buffer.writeShort(0);
		buffer.writeShort(0);
		buffer.writeShort(0);
		buffer.writeShort(0);
		buffer.writeShort(0);
		buffer.writeShort(0);
		buffer.writeShort(0);
		buffer.writeShort(0);
		buffer.writeShort(0);
		buffer.writeInt(0); // _player.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_RHAND)
		buffer.writeShort(0);
		buffer.writeShort(0);
		buffer.writeShort(0);
		buffer.writeShort(0);
		
		buffer.writeInt(_npc.getScriptValue()); // getPvpFlag()
		buffer.writeInt(_npc.getKarma());
		buffer.writeInt(_mAtkSpd);
		buffer.writeInt(_pAtkSpd);
		buffer.writeInt(_npc.getScriptValue()); // getPvpFlag()
		buffer.writeInt(_npc.getKarma());
		buffer.writeInt(_runSpd);
		buffer.writeInt(_walkSpd);
		buffer.writeInt(_swimRunSpd);
		buffer.writeInt(_swimWalkSpd);
		buffer.writeInt(_flyRunSpd);
		buffer.writeInt(_flyWalkSpd);
		buffer.writeInt(_flyRunSpd);
		buffer.writeInt(_flyWalkSpd);
		buffer.writeDouble(_moveMultiplier);
		buffer.writeDouble(_attackSpeedMultiplier);
		buffer.writeDouble(_npc.getCollisionRadius());
		buffer.writeDouble(_npc.getCollisionHeight());
		buffer.writeInt(_look != null ? _look.getHairStyle() : _fpcHolder.getHair());
		buffer.writeInt(_look != null ? _look.getHairColor() : _fpcHolder.getHairColor());
		buffer.writeInt(_look != null ? _look.getFace() : _fpcHolder.getFace());
		buffer.writeString(_look != null ? _look.getTitle() : _npc.getTemplate().getTitle());
		if (_clan != null)
		{
			buffer.writeInt(_clan.getId());
			buffer.writeInt(_clan.getCrestId());
			buffer.writeInt(_clan.getAllyId());
			buffer.writeInt(_clan.getAllyCrestId());
		}
		else
		{
			buffer.writeInt(0);
			buffer.writeInt(0);
			buffer.writeInt(0);
			buffer.writeInt(0);
		}
		
		// In UserInfo leader rights and siege flags, but here found nothing??
		// Therefore RelationChanged packet with that info is required
		buffer.writeInt(0);
		buffer.writeByte(_look != null ? !_look.isSitting() : !_fpcHolder.isSitting());
		buffer.writeByte(_look != null ? (!_look.isSitting() && _npc.isRunning()) : _npc.isRunning());
		buffer.writeByte(_npc.isInCombat());
		buffer.writeByte(_npc.isAlikeDead());
		buffer.writeByte(_npc.isInvisible());
		buffer.writeByte(0); // 1-on Strider, 2-on Wyvern, 3-on Great Wolf, 0-no mount
		buffer.writeByte(_look != null ? _look.getPrivateStoreType() : _fpcHolder.getPrivateStoreType());
		buffer.writeShort(0); // getCubics().size()
		
		// getCubics().keySet().forEach(packet::writeH);
		buffer.writeByte(0); // isInPartyMatchRoom
		buffer.writeInt(_npc.getAbnormalVisualEffects());
		buffer.writeByte(0); // _player.getRecomLeft()
		buffer.writeShort(_fpcHolder.getRecommends()); // Blue value for name (0 = white, 255 = pure blue)
		buffer.writeInt(_fpcHolder.getPlayerClass().getId());
		buffer.writeInt(0); // ?
		buffer.writeInt(0); // _player.getCurrentCp()
		buffer.writeByte(_look != null ? _look.getWeaponEnchantLevel() : _fpcHolder.getWeaponEnchantLevel()); // isMounted() ? 0 : _enchantLevel
		buffer.writeByte(_npc.getTeam().getId());
		buffer.writeInt(_clan != null ? _clan.getCrestLargeId() : 0);
		buffer.writeByte(_fpcHolder.getNobleLevel());
		buffer.writeByte(_fpcHolder.isHero());
		buffer.writeByte(_fpcHolder.isFishing());
		buffer.writeInt(_fpcHolder.getBaitLocationX());
		buffer.writeInt(_fpcHolder.getBaitLocationY());
		buffer.writeInt(_fpcHolder.getBaitLocationZ());
		buffer.writeInt(_look != null ? _look.getNameColor() : _fpcHolder.getNameColor());
		buffer.writeInt(_heading);
		buffer.writeInt(_fpcHolder.getPledgeStatus());
		buffer.writeInt(0); // getPledgeType()
		buffer.writeInt(_look != null ? _look.getTitleColor() : _fpcHolder.getTitleColor());
		buffer.writeInt(0); // isCursedWeaponEquipped
	}
}
