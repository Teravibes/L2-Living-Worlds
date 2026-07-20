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
package org.l2jmobius.gameserver.model.actor.status;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.custom.MultilingualSupportConfig;
import org.l2jmobius.gameserver.config.custom.OfflineTradeConfig;
import org.l2jmobius.gameserver.data.xml.NpcNameLocalisationData;
import org.l2jmobius.gameserver.managers.DuelManager;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.actor.enums.player.PrivateStoreType;
import org.l2jmobius.gameserver.model.actor.holders.player.Duel;
import org.l2jmobius.gameserver.model.actor.stat.PlayerStat;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.model.stats.Formulas;
import org.l2jmobius.gameserver.model.stats.Stat;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.LocationUtil;

public class PlayerStatus extends PlayableStatus
{
	private double _currentCp = 0; // Current CP of the Player
	
	public PlayerStatus(Player player)
	{
		super(player);
	}
	
	@Override
	public void reduceCp(int value)
	{
		setCurrentCp(_currentCp > value ? _currentCp - value : 0);
	}
	
	@Override
	public void reduceHp(double value, Creature attacker)
	{
		reduceHp(value, attacker, true, false, false, false);
	}
	
	@Override
	public void reduceHp(double value, Creature attacker, boolean awake, boolean isDOT, boolean isHPConsumption)
	{
		reduceHp(value, attacker, awake, isDOT, isHPConsumption, false);
	}
	
	public void reduceHp(double value, Creature attacker, boolean awake, boolean isDOT, boolean isHPConsumption, boolean ignoreCP)
	{
		final Player player = getActiveChar();
		if (player.isDead())
		{
			return;
		}
		
		// If OFFLINE_MODE_NO_DAMAGE is enabled and player is offline and he is in store/craft mode, no damage is taken.
		if (OfflineTradeConfig.OFFLINE_MODE_NO_DAMAGE && (player.getClient() != null) && player.getClient().isDetached() && ((OfflineTradeConfig.OFFLINE_TRADE_ENABLE && ((player.getPrivateStoreType() == PrivateStoreType.SELL) || (player.getPrivateStoreType() == PrivateStoreType.BUY))) || (OfflineTradeConfig.OFFLINE_CRAFT_ENABLE && (player.isCrafting() || (player.getPrivateStoreType() == PrivateStoreType.MANUFACTURE)))))
		{
			return;
		}
		
		if (player.isInvul() && !(isDOT || isHPConsumption))
		{
			return;
		}
		
		if (!isHPConsumption)
		{
			player.stopEffectsOnDamage(awake);
			
			// Attacked players in craft/shops stand up.
			if (player.isCrafting() || player.isInStoreMode())
			{
				player.setPrivateStoreType(PrivateStoreType.NONE);
				player.standUp();
				player.broadcastUserInfo();
			}
			else if (player.isSitting())
			{
				player.standUp();
			}
			
			if (!isDOT && player.isStunned() && (Rnd.get(10) == 0))
			{
				player.stopStunning(true);
			}
		}
		
		double amount = value;
		int fullValue = (int) amount;
		int tDmg = 0;
		int mpDam = 0;
		if ((attacker != null) && (attacker != player))
		{
			final Player attackerPlayer = attacker.asPlayer();
			if (attackerPlayer != null)
			{
				if (attackerPlayer.isGM() && !attackerPlayer.getAccessLevel().canGiveDamage())
				{
					return;
				}
				
				if (player.isInDuel())
				{
					if (player.getDuelState() == Duel.DUELSTATE_DEAD)
					{
						return;
					}
					else if (player.getDuelState() == Duel.DUELSTATE_WINNER)
					{
						return;
					}
					
					// cancel duel if player got hit by another player, that is not part of the duel
					if (attackerPlayer.getDuelId() != player.getDuelId())
					{
						player.setDuelState(Duel.DUELSTATE_INTERRUPTED);
					}
				}
			}
			
			// Check and calculate transfered damage
			final PlayerStat stat = player.getStat();
			final Summon summon = player.getSummon();
			if (player.hasServitor() && LocationUtil.checkIfInRange(1000, player, summon, true))
			{
				tDmg = ((int) amount * (int) stat.calcStat(Stat.TRANSFER_DAMAGE_PERCENT, 0, null, null)) / 100;
				
				// Only transfer dmg up to current HP, it should not be killed
				tDmg = Math.min((int) summon.getCurrentHp() - 1, tDmg);
				if (tDmg > 0)
				{
					summon.reduceCurrentHp(tDmg, attacker, null);
					amount -= tDmg;
					fullValue = (int) amount; // reduce the announced value here as player will get a message about summon damage
				}
			}
			
			mpDam = ((int) amount * (int) stat.calcStat(Stat.MANA_SHIELD_PERCENT, 0, null, null)) / 100;
			if (mpDam > 0)
			{
				mpDam = (int) (amount - mpDam);
				if (mpDam > player.getCurrentMp())
				{
					player.sendMessage("MP became 0 and the Arcane Shield is disappearing.");
					player.stopSkillEffects(SkillFinishType.REMOVED, 1556);
					amount = mpDam - player.getCurrentMp();
					player.setCurrentMp(0);
				}
				else
				{
					player.reduceCurrentMp(mpDam);
					player.sendMessage("Arcane Shield decreased your MP by " + mpDam + " instead of HP.");
					return;
				}
			}
			
			final Player caster = player.getTransferingDamageTo();
			if ((caster != null) && (player.getParty() != null) && LocationUtil.checkIfInRange(1000, player, caster, true) && !caster.isDead() && (player != caster) && player.getParty().getMembers().contains(caster))
			{
				int transferDmg = Math.min((int) caster.getCurrentHp() - 1, ((int) amount * (int) stat.calcStat(Stat.TRANSFER_DAMAGE_TO_PLAYER, 0, null, null)) / 100);
				if (transferDmg > 0)
				{
					int membersInRange = 0;
					for (Player member : caster.getParty().getMembers())
					{
						if (LocationUtil.checkIfInRange(1000, member, caster, false) && (member != caster))
						{
							membersInRange++;
						}
					}
					
					if ((attacker.isPlayable() || attacker.isFakePlayer()) && (caster.getCurrentCp() > 0))
					{
						if (caster.getCurrentCp() > transferDmg)
						{
							caster.getStatus().reduceCp(transferDmg);
						}
						else
						{
							transferDmg = (int) (transferDmg - caster.getCurrentCp());
							caster.getStatus().reduceCp((int) caster.getCurrentCp());
						}
					}
					
					if (membersInRange > 0)
					{
						caster.reduceCurrentHp(transferDmg / membersInRange, attacker, null);
						amount -= transferDmg;
						fullValue = (int) amount;
					}
				}
			}
			
			if (!ignoreCP && (attacker.isPlayable() || attacker.isFakePlayer()))
			{
				if (_currentCp >= amount)
				{
					setCurrentCp(_currentCp - amount); // Set Cp to diff of Cp vs value
					amount = 0; // No need to subtract anything from Hp
				}
				else
				{
					amount -= _currentCp; // Get diff from value vs Cp; will apply diff to Hp
					setCurrentCp(0, false); // Set Cp to 0
				}
			}
			
			if ((fullValue > 0) && !isDOT)
			{
				// Send a System Message to the Player
				SystemMessage smsg = new SystemMessage(SystemMessageId.S1_HIT_YOU_FOR_S2_DAMAGE);
				
				// Localisation related.
				String targetName = attacker.getName();
				if (MultilingualSupportConfig.MULTILANG_ENABLE && attacker.isNpc())
				{
					final String[] localisation = NpcNameLocalisationData.getInstance().getLocalisation(player.getLang(), attacker.getId());
					if (localisation != null)
					{
						targetName = localisation[0];
					}
				}
				
				smsg.addString(targetName);
				smsg.addInt(fullValue);
				player.sendPacket(smsg);
				
				if ((tDmg > 0) && (attackerPlayer != null))
				{
					smsg = new SystemMessage(SystemMessageId.YOU_HAVE_GIVEN_S1_DAMAGE_TO_YOUR_TARGET_AND_S2_DAMAGE_TO_THE_SERVITOR);
					smsg.addInt(fullValue);
					smsg.addInt(tDmg);
					attackerPlayer.sendPacket(smsg);
				}
			}
		}
		
		if (amount > 0)
		{
			amount = getCurrentHp() - amount;
			if (amount <= 0)
			{
				if (player.isInDuel())
				{
					player.disableAllSkills();
					stopHpMpRegeneration();
					if (attacker != null)
					{
						attacker.getAI().setIntention(Intention.ACTIVE);
						attacker.sendPacket(ActionFailed.STATIC_PACKET);
						attacker.setTarget(null);
						attacker.abortAttack();
					}
					
					// let the DuelManager know of his defeat
					DuelManager.getInstance().onPlayerDefeat(player);
					amount = 1;
				}
				else
				{
					amount = 0;
				}
			}
			
			setCurrentHp(amount);
		}
		
		if ((player.getCurrentHp() < 0.5) && !isHPConsumption)
		{
			player.abortAttack();
			player.abortCast();
			
			if (player.isInOlympiadMode())
			{
				stopHpMpRegeneration();
				player.setDead(true);
				player.setIsPendingRevive(true);
				if (player.hasSummon())
				{
					player.getSummon().getAI().setIntention(Intention.IDLE, null);
				}
				return;
			}
			
			player.doDie(attacker);
			if (!PlayerConfig.DISABLE_TUTORIAL)
			{
				final QuestState qs = player.getQuestState("Q00255_Tutorial");
				if (qs != null)
				{
					qs.getQuest().notifyEvent("CE30", null, player);
				}
			}
		}
	}
	
	@Override
	public boolean setCurrentHp(double newHp, boolean broadcastPacket)
	{
		final boolean result = super.setCurrentHp(newHp, broadcastPacket);
		final Player player = getActiveChar();
		if (!PlayerConfig.DISABLE_TUTORIAL && (getCurrentHp() <= (player.getStat().getMaxHp() * .3)))
		{
			final QuestState qs = player.getQuestState("Q00255_Tutorial");
			if (qs != null)
			{
				qs.getQuest().notifyEvent("CE45", null, player);
			}
		}
		
		return result;
	}
	
	@Override
	public double getCurrentCp()
	{
		return _currentCp;
	}
	
	@Override
	public void setCurrentCp(double newCp)
	{
		setCurrentCp(newCp, true);
	}
	
	public void setCurrentCp(double value, boolean broadcastPacket)
	{
		// Get the Max CP of the Creature
		final int currentCp = (int) _currentCp;
		final Player player = getActiveChar();
		final int maxCp = player.getStat().getMaxCp();
		
		synchronized (this)
		{
			if (player.isDead())
			{
				return;
			}
			
			final double newCp = Math.max(0, value);
			if (newCp >= maxCp)
			{
				// Set the RegenActive flag to false
				_currentCp = maxCp;
				_flagsRegenActive &= ~REGEN_FLAG_CP;
				
				// Stop the HP/MP/CP Regeneration task
				if (_flagsRegenActive == 0)
				{
					stopHpMpRegeneration();
				}
			}
			else
			{
				// Set the RegenActive flag to true
				_currentCp = newCp;
				_flagsRegenActive |= REGEN_FLAG_CP;
				
				// Start the HP/MP/CP Regeneration task with Medium priority
				startHpMpRegeneration();
			}
		}
		
		// Send the Server->Client packet StatusUpdate with current HP and MP to all other Player to inform
		if ((currentCp != _currentCp) && broadcastPacket)
		{
			player.broadcastStatusUpdate();
		}
	}
	
	@Override
	protected void doRegeneration()
	{
		final Player player = getActiveChar();
		final PlayerStat stat = player.getStat();
		
		// Modify the current CP of the Creature and broadcast Server->Client packet StatusUpdate
		if (_currentCp < stat.getMaxRecoverableCp())
		{
			setCurrentCp(_currentCp + Formulas.calcCpRegen(player), false);
		}
		
		// Modify the current HP of the Creature and broadcast Server->Client packet StatusUpdate
		if (getCurrentHp() < stat.getMaxRecoverableHp())
		{
			setCurrentHp(getCurrentHp() + Formulas.calcHpRegen(player), false);
		}
		
		// Modify the current MP of the Creature and broadcast Server->Client packet StatusUpdate
		if (getCurrentMp() < stat.getMaxRecoverableMp())
		{
			setCurrentMp(getCurrentMp() + Formulas.calcMpRegen(player), false);
		}
		
		player.broadcastStatusUpdate(); // send the StatusUpdate packet
	}
	
	@Override
	public Player getActiveChar()
	{
		return super.getActiveChar().asPlayer();
	}
}
