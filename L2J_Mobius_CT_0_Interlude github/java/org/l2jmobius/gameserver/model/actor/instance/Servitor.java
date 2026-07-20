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
package org.l2jmobius.gameserver.model.actor.instance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.sql.CharSummonTable;
import org.l2jmobius.gameserver.data.sql.SummonEffectTable;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.skill.AbnormalType;
import org.l2jmobius.gameserver.model.skill.BuffInfo;
import org.l2jmobius.gameserver.model.skill.EffectScope;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.model.stats.Stat;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SetSummonRemainTime;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * @author UnAfraid
 */
public class Servitor extends Summon implements Runnable
{
	protected static final Logger log = Logger.getLogger(Servitor.class.getName());
	
	private static final String ADD_SKILL_SAVE = "REPLACE INTO character_summon_skills_save (ownerId,ownerClassIndex,summonSkillId,skill_id,skill_level,remaining_time,buff_index) VALUES (?,?,?,?,?,?,?)";
	private static final String RESTORE_SKILL_SAVE = "SELECT skill_id,skill_level,remaining_time,buff_index FROM character_summon_skills_save WHERE ownerId=? AND ownerClassIndex=? AND summonSkillId=? ORDER BY buff_index ASC";
	private static final String DELETE_SKILL_SAVE = "DELETE FROM character_summon_skills_save WHERE ownerId=? AND ownerClassIndex=? AND summonSkillId=?";
	
	private float _expMultiplier = 0;
	private ItemHolder _itemConsume;
	private int _lifeTime;
	private int _lifeTimeRemaining;
	private int _consumeItemInterval;
	private int _consumeItemIntervalRemaining;
	protected Future<?> _summonLifeTask;
	
	private int _referenceSkill;
	
	public Servitor(NpcTemplate template, Player owner)
	{
		super(template, owner);
		setInstanceType(InstanceType.Servitor);
		setShowSummonAnimation(true);
	}
	
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		if (_summonLifeTask == null)
		{
			_summonLifeTask = ThreadPool.scheduleAtFixedRate(this, 0, 5000);
		}
	}
	
	@Override
	public int getLevel()
	{
		return getTemplate() != null ? getTemplate().getLevel() : 0;
	}
	
	@Override
	public int getSummonType()
	{
		return 1;
	}
	
	public void setExpMultiplier(float expMultiplier)
	{
		_expMultiplier = expMultiplier;
	}
	
	public float getExpMultiplier()
	{
		return _expMultiplier;
	}
	
	public void setItemConsume(ItemHolder item)
	{
		_itemConsume = item;
	}
	
	public ItemHolder getItemConsume()
	{
		return _itemConsume;
	}
	
	public void setItemConsumeInterval(int interval)
	{
		_consumeItemInterval = interval;
		_consumeItemIntervalRemaining = interval;
	}
	
	public int getItemConsumeInterval()
	{
		return _consumeItemInterval;
	}
	
	public void setLifeTime(int lifeTime)
	{
		_lifeTime = lifeTime;
		_lifeTimeRemaining = lifeTime;
	}
	
	public int getLifeTime()
	{
		return _lifeTime;
	}
	
	public void setLifeTimeRemaining(int time)
	{
		_lifeTimeRemaining = time;
	}
	
	public int getLifeTimeRemaining()
	{
		return _lifeTimeRemaining;
	}
	
	public void setReferenceSkill(int skillId)
	{
		_referenceSkill = skillId;
	}
	
	public int getReferenceSkill()
	{
		return _referenceSkill;
	}
	
	@Override
	public boolean doDie(Creature killer)
	{
		if (!super.doDie(killer))
		{
			return false;
		}
		
		if (_summonLifeTask != null)
		{
			_summonLifeTask.cancel(false);
		}
		
		CharSummonTable.getInstance().removeServitor(getOwner());
		return true;
	}
	
	@Override
	public void doPickupItem(WorldObject object)
	{
	}
	
	@Override
	public void setRestoreSummon(boolean value)
	{
		_restoreSummon = value;
	}
	
	@Override
	public void stopSkillEffects(SkillFinishType type, int skillId)
	{
		super.stopSkillEffects(type, skillId);
		SummonEffectTable.getInstance().removeServitorEffects(getOwner(), getReferenceSkill(), skillId);
	}
	
	@Override
	public void storeMe()
	{
		if ((_referenceSkill == 0) || isDead())
		{
			return;
		}
		
		if (PlayerConfig.RESTORE_SERVITOR_ON_RECONNECT)
		{
			CharSummonTable.getInstance().saveSummon(this);
		}
	}
	
	@Override
	public void storeEffect(boolean storeEffects)
	{
		if (!PlayerConfig.SUMMON_STORE_SKILL_COOLTIME || (getOwner() == null) || getOwner().isInOlympiadMode())
		{
			return;
		}
		
		// Clear list for overwrite
		SummonEffectTable.getInstance().clearServitorEffects(getOwner(), getReferenceSkill());
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(DELETE_SKILL_SAVE))
		{
			// Delete all current stored effects for summon to avoid dupe
			ps.setInt(1, getOwner().getObjectId());
			ps.setInt(2, getOwner().getClassIndex());
			ps.setInt(3, _referenceSkill);
			ps.execute();
			
			int buffIndex = 0;
			
			final List<Integer> storedSkills = new LinkedList<>();
			
			// Store all effect data along with calculated remaining
			if (storeEffects)
			{
				try (PreparedStatement ps2 = con.prepareStatement(ADD_SKILL_SAVE))
				{
					for (BuffInfo info : getEffectList().getEffects())
					{
						if (info == null)
						{
							continue;
						}
						
						final Skill skill = info.getSkill();
						
						// Do not save heals.
						if (skill.getAbnormalType() == AbnormalType.LIFE_FORCE_OTHERS)
						{
							continue;
						}
						
						if (skill.isToggle())
						{
							continue;
						}
						
						// Dances and songs are not kept in retail.
						if (skill.isDance() && !PlayerConfig.ALT_STORE_DANCES)
						{
							continue;
						}
						
						if (storedSkills.contains(skill.getReuseHashCode()))
						{
							continue;
						}
						
						storedSkills.add(skill.getReuseHashCode());
						
						ps2.setInt(1, getOwner().getObjectId());
						ps2.setInt(2, getOwner().getClassIndex());
						ps2.setInt(3, _referenceSkill);
						ps2.setInt(4, skill.getId());
						ps2.setInt(5, skill.getLevel());
						ps2.setInt(6, info.getTime());
						ps2.setInt(7, ++buffIndex);
						ps2.addBatch();
						
						SummonEffectTable.getInstance().addServitorEffect(getOwner(), getReferenceSkill(), skill, info.getTime());
					}
					
					ps2.executeBatch();
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not store summon effect data: ", e);
		}
	}
	
	@Override
	public void restoreEffects()
	{
		if (getOwner().isInOlympiadMode())
		{
			return;
		}
		
		try (Connection con = DatabaseFactory.getConnection())
		{
			if (!SummonEffectTable.getInstance().containsSkill(getOwner(), getReferenceSkill()))
			{
				try (PreparedStatement ps = con.prepareStatement(RESTORE_SKILL_SAVE))
				{
					ps.setInt(1, getOwner().getObjectId());
					ps.setInt(2, getOwner().getClassIndex());
					ps.setInt(3, _referenceSkill);
					try (ResultSet rs = ps.executeQuery())
					{
						while (rs.next())
						{
							final int effectCurTime = rs.getInt("remaining_time");
							final Skill skill = SkillData.getInstance().getSkill(rs.getInt("skill_id"), rs.getInt("skill_level"));
							if (skill == null)
							{
								continue;
							}
							
							if (skill.hasEffects(EffectScope.GENERAL))
							{
								SummonEffectTable.getInstance().addServitorEffect(getOwner(), getReferenceSkill(), skill, effectCurTime);
							}
						}
					}
				}
			}
			
			try (PreparedStatement statement = con.prepareStatement(DELETE_SKILL_SAVE))
			{
				statement.setInt(1, getOwner().getObjectId());
				statement.setInt(2, getOwner().getClassIndex());
				statement.setInt(3, _referenceSkill);
				statement.executeUpdate();
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not restore " + this + " active effect data: " + e.getMessage(), e);
		}
		finally
		{
			SummonEffectTable.getInstance().applyServitorEffects(this, getOwner(), getReferenceSkill());
		}
	}
	
	@Override
	public void unSummon(Player owner)
	{
		if (_summonLifeTask != null)
		{
			_summonLifeTask.cancel(false);
		}
		
		super.unSummon(owner);
		
		if (!_restoreSummon)
		{
			CharSummonTable.getInstance().removeServitor(owner);
		}
	}
	
	@Override
	public boolean destroyItem(ItemProcessType process, int objectId, int count, WorldObject reference, boolean sendMessage)
	{
		return getOwner().destroyItem(process, objectId, count, reference, sendMessage);
	}
	
	@Override
	public boolean destroyItemByItemId(ItemProcessType process, int itemId, int count, WorldObject reference, boolean sendMessage)
	{
		return getOwner().destroyItemByItemId(process, itemId, count, reference, sendMessage);
	}
	
	@Override
	public boolean isServitor()
	{
		return true;
	}
	
	@Override
	public Servitor asServitor()
	{
		return this;
	}
	
	@Override
	public void run()
	{
		final int usedtime = 5000;
		_lifeTimeRemaining -= usedtime;
		if (isDead() || !isSpawned())
		{
			if (_summonLifeTask != null)
			{
				_summonLifeTask.cancel(false);
			}
			return;
		}
		
		// check if the summon's lifetime has ran out
		if (_lifeTimeRemaining < 0)
		{
			sendPacket(SystemMessageId.SERVITOR_PASSED_AWAY);
			unSummon(getOwner());
			return;
		}
		
		if (_consumeItemInterval > 0)
		{
			_consumeItemIntervalRemaining -= usedtime;
			
			// check if it is time to consume another item
			if ((_consumeItemIntervalRemaining <= 0) && (_itemConsume.getCount() > 0) && (_itemConsume.getId() > 0) && !isDead())
			{
				if (destroyItemByItemId(null, _itemConsume.getId(), _itemConsume.getCount(), this, false))
				{
					final SystemMessage msg = new SystemMessage(SystemMessageId.A_SUMMONED_MONSTER_USES_S1);
					msg.addItemName(_itemConsume.getId());
					sendPacket(msg);
					
					// Reset
					_consumeItemIntervalRemaining = _consumeItemInterval;
				}
				else
				{
					sendPacket(SystemMessageId.SINCE_YOU_DO_NOT_HAVE_ENOUGH_ITEMS_TO_MAINTAIN_THE_SERVITOR_S_STAY_THE_SERVITOR_WILL_DISAPPEAR);
					unSummon(getOwner());
				}
			}
		}
		
		sendPacket(new SetSummonRemainTime(_lifeTime, _lifeTimeRemaining));
		updateEffectIcons();
	}
	
	@Override
	public double getMAtk(Creature target, Skill skill)
	{
		final Player player = asPlayer();
		if (player == null)
		{
			return super.getMAtk(target, skill);
		}
		
		return super.getMAtk(target, skill) + (player.getMAtk(target, skill) * (player.getServitorShareBonus(Stat.MAGIC_ATTACK) - 1.0));
	}
	
	@Override
	public double getMDef(Creature target, Skill skill)
	{
		final Player player = asPlayer();
		if (player == null)
		{
			return super.getMDef(target, skill);
		}
		
		return super.getMDef(target, skill) + (player.getMDef(target, skill) * (player.getServitorShareBonus(Stat.MAGIC_DEFENCE) - 1.0));
	}
	
	@Override
	public double getPAtk(Creature target)
	{
		final Player player = asPlayer();
		if (player == null)
		{
			return super.getPAtk(target);
		}
		
		return super.getPAtk(target) + (player.getPAtk(target) * (player.getServitorShareBonus(Stat.POWER_ATTACK) - 1.0));
	}
	
	@Override
	public double getPDef(Creature target)
	{
		final Player player = asPlayer();
		if (player == null)
		{
			return super.getPDef(target);
		}
		
		return super.getPDef(target) + (player.getPDef(target) * (player.getServitorShareBonus(Stat.POWER_DEFENCE) - 1.0));
	}
	
	@Override
	public int getMAtkSpd()
	{
		final Player player = asPlayer();
		if (player == null)
		{
			return super.getMAtkSpd();
		}
		
		return (int) (super.getMAtkSpd() + (player.getMAtkSpd() * (player.getServitorShareBonus(Stat.MAGIC_ATTACK_SPEED) - 1.0)));
	}
	
	@Override
	public int getMaxHp()
	{
		final Player player = asPlayer();
		if (player == null)
		{
			return super.getMaxHp();
		}
		
		return (int) (super.getMaxHp() + (player.getMaxHp() * (player.getServitorShareBonus(Stat.MAX_HP) - 1.0)));
	}
	
	@Override
	public int getMaxMp()
	{
		final Player player = asPlayer();
		if (player == null)
		{
			return super.getMaxMp();
		}
		
		return (int) (super.getMaxMp() + (player.getMaxMp() * (player.getServitorShareBonus(Stat.MAX_MP) - 1.0)));
	}
	
	@Override
	public int getCriticalHit(Creature target, Skill skill)
	{
		final Player player = asPlayer();
		if (player == null)
		{
			return super.getCriticalHit(target, skill);
		}
		
		return (int) (super.getCriticalHit(target, skill) + ((player.getCriticalHit(target, skill)) * (player.getServitorShareBonus(Stat.CRITICAL_RATE) - 1.0)));
	}
	
	@Override
	public double getPAtkSpd()
	{
		final Player player = asPlayer();
		if (player == null)
		{
			return super.getPAtkSpd();
		}
		
		return super.getPAtkSpd() + (player.getPAtkSpd() * (player.getServitorShareBonus(Stat.POWER_ATTACK_SPEED) - 1.0));
	}
}
