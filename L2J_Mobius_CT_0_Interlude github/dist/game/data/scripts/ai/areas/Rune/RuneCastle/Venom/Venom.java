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
package ai.areas.Rune.RuneCastle.Venom;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.GlobalVariablesManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.model.events.holders.sieges.castle.OnCastleSiegeFinish;
import org.l2jmobius.gameserver.model.events.holders.sieges.castle.OnCastleSiegeStart;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.network.enums.ChatType;

/**
 * Venom AI on Rune Castle.
 * @author nonom, MELERIX
 */
public class Venom extends Script
{
	private static final int CASTLE = 8; // Rune
	
	private static final int VENOM = 29054;
	private static final int TELEPORT_CUBE = 29055;
	private static final int DUNGEON_KEEPER = 35506;
	
	private static final byte ALIVE = 0;
	private static final byte DEAD = 1;
	
	private static final int HOURS_BEFORE = 24;
	
	private static final Location[] TARGET_TELEPORTS =
	{
		new Location(12860, -49158, 976),
		new Location(14878, -51339, 1024),
		new Location(15674, -49970, 864),
		new Location(15696, -48326, 864),
		new Location(14873, -46956, 1024),
		new Location(12157, -49135, -1088),
		new Location(12875, -46392, -288),
		new Location(14087, -46706, -288),
		new Location(14086, -51593, -288),
		new Location(12864, -51898, -288),
		new Location(15538, -49153, -1056),
		new Location(17001, -49149, -1064)
	};
	
	private static final Location TRHONE = new Location(11025, -49152, -537);
	private static final Location DUNGEON = new Location(11882, -49216, -3008);
	private static final Location TELEPORT = new Location(12589, -49044, -3008);
	private static final Location CUBE = new Location(12047, -49211, -3009);
	
	private static final SkillHolder VENOM_STRIKE = new SkillHolder(4993, 1);
	private static final SkillHolder SONIC_STORM = new SkillHolder(4994, 1);
	private static final SkillHolder VENOM_TELEPORT = new SkillHolder(4995, 1);
	private static final SkillHolder RANGE_TELEPORT = new SkillHolder(4996, 1);
	
	private Npc _venom;
	private Npc _massymore;
	
	private Location _loc;
	
	private boolean _aggroMode = false;
	private boolean _prisonIsOpen = false;
	
	// @formatter:off
	private static final int[] TARGET_TELEPORTS_OFFSET =
	{
		650, 100, 100, 100, 100, 650, 200, 200, 200, 200, 200, 650
	};
	// @formatter:on
	
	private static List<Player> _targets = new ArrayList<>();
	
	private Venom()
	{
		addStartNpc(DUNGEON_KEEPER, TELEPORT_CUBE);
		addFirstTalkId(DUNGEON_KEEPER, TELEPORT_CUBE);
		addTalkId(DUNGEON_KEEPER, TELEPORT_CUBE);
		addSpawnId(VENOM, DUNGEON_KEEPER);
		addSpellFinishedId(VENOM);
		addAttackId(VENOM);
		addKillId(VENOM);
		addAggroRangeEnterId(VENOM);
		setCastleSiegeStartId(this::onSiegeStart, CASTLE);
		setCastleSiegeFinishId(this::onSiegeFinish, CASTLE);
		
		final long currentTime = System.currentTimeMillis();
		final long startSiegeDate = CastleManager.getInstance().getCastleById(CASTLE).getSiegeDate().getTimeInMillis();
		if ((currentTime > (startSiegeDate - (HOURS_BEFORE * 360000))) && (currentTime < startSiegeDate))
		{
			_prisonIsOpen = true;
		}
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + ".html";
	}
	
	@Override
	public String onTalk(Npc npc, Player talker)
	{
		switch (npc.getId())
		{
			case TELEPORT_CUBE:
			{
				talker.teleToLocation(TeleportWhereType.TOWN);
				break;
			}
			case DUNGEON_KEEPER:
			{
				if (_prisonIsOpen)
				{
					talker.teleToLocation(TELEPORT);
				}
				else
				{
					return "35506-02.html";
				}
				break;
			}
		}
		
		return super.onTalk(npc, talker);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "tower_check":
			{
				if (CastleManager.getInstance().getCastleById(CASTLE).getSiege().getControlTowerCount() <= 1)
				{
					changeLocation(MoveTo.THRONE);
					_massymore.broadcastSay(ChatType.NPC_SHOUT, "Oh no! The defenses have failed. It is too dangerous to remain inside the castle. Flee! Every man for himself!");
					cancelQuestTimer("tower_check", npc, null);
					startQuestTimer("raid_check", 10000, npc, null, true);
				}
				break;
			}
			case "raid_check":
			{
				if (!npc.isInsideZone(ZoneId.SIEGE) && !npc.isTeleporting())
				{
					npc.teleToLocation(_loc);
				}
				break;
			}
			case "cube_despawn":
			{
				if (npc != null)
				{
					npc.deleteMe();
				}
				break;
			}
		}
		
		return event;
	}
	
	@Override
	public void onAggroRangeEnter(Npc npc, Player player, boolean isSummon)
	{
		if (isSummon)
		{
			return;
		}
		
		if (_aggroMode && (_targets.size() < 10) && (getRandom(3) < 1) && !player.isDead())
		{
			_targets.add(player);
		}
	}
	
	public void onSiegeStart(OnCastleSiegeStart event)
	{
		_aggroMode = true;
		_prisonIsOpen = false;
		if ((_venom != null) && !_venom.isDead())
		{
			_venom.setCurrentHp(_venom.getMaxHp());
			_venom.setCurrentMp(_venom.getMaxMp());
			_venom.enableSkill(VENOM_TELEPORT.getSkill());
			_venom.enableSkill(RANGE_TELEPORT.getSkill());
			startQuestTimer("tower_check", 30000, _venom, null, true);
		}
	}
	
	public void onSiegeFinish(OnCastleSiegeFinish event)
	{
		_aggroMode = false;
		if ((_venom != null) && !_venom.isDead())
		{
			changeLocation(MoveTo.PRISON);
			_venom.disableSkill(VENOM_TELEPORT.getSkill(), -1);
			_venom.disableSkill(RANGE_TELEPORT.getSkill(), -1);
		}
		
		updateStatus(ALIVE);
		cancelQuestTimer("tower_check", _venom, null);
		cancelQuestTimer("raid_check", _venom, null);
	}
	
	@Override
	public void onSpellFinished(Npc npc, Player player, Skill skill)
	{
		switch (skill.getId())
		{
			case 4222:
			{
				npc.teleToLocation(_loc);
				break;
			}
			case 4995:
			{
				teleportTarget(player);
				npc.asAttackable().stopHating(player);
				break;
			}
			case 4996:
			{
				teleportTarget(player);
				npc.asAttackable().stopHating(player);
				if ((_targets != null) && !_targets.isEmpty())
				{
					for (Player target : _targets)
					{
						final long x = player.getX() - target.getX();
						final long y = player.getY() - target.getY();
						final long z = player.getZ() - target.getZ();
						final long range = 250;
						if (((x * x) + (y * y) + (z * z)) <= (range * range))
						{
							teleportTarget(target);
							npc.asAttackable().stopHating(target);
						}
					}
					
					_targets.clear();
				}
				break;
			}
		}
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		switch (npc.getId())
		{
			case DUNGEON_KEEPER:
			{
				_massymore = npc;
				break;
			}
			case VENOM:
			{
				_venom = npc;
				_loc = _venom.getLocation();
				_venom.disableSkill(VENOM_TELEPORT.getSkill(), -1);
				_venom.disableSkill(RANGE_TELEPORT.getSkill(), -1);
				_venom.doRevive();
				npc.broadcastSay(ChatType.NPC_SHOUT, "Who dares to covet the throne of our castle! Leave immediately or you will pay the price of your audacity with your very own blood!");
				_venom.asAttackable().setCanReturnToSpawnPoint(false);
				if (checkStatus() == DEAD)
				{
					_venom.deleteMe();
				}
				break;
			}
		}
		
		if (checkStatus() == DEAD)
		{
			npc.deleteMe();
		}
		else
		{
			npc.doRevive();
		}
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		final double distance = npc.calculateDistance2D(attacker);
		if (_aggroMode && (getRandom(100) < 25))
		{
			npc.setTarget(attacker);
			npc.doCast(VENOM_TELEPORT.getSkill());
		}
		else if (_aggroMode && (npc.getCurrentHp() < (npc.getMaxHp() / 3)) && (getRandom(100) < 25) && !npc.isCastingNow())
		{
			npc.setTarget(attacker);
			npc.doCast(RANGE_TELEPORT.getSkill());
		}
		else if ((distance > 300) && (getRandom(100) < 10) && !npc.isCastingNow())
		{
			npc.setTarget(attacker);
			npc.doCast(VENOM_STRIKE.getSkill());
		}
		else if ((getRandom(100) < 10) && !npc.isCastingNow())
		{
			npc.setTarget(attacker);
			npc.doCast(SONIC_STORM.getSkill());
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		updateStatus(DEAD);
		npc.broadcastSay(ChatType.NPC_SHOUT, "It's not over yet... It won't be... over... like this... Never...");
		if (!CastleManager.getInstance().getCastleById(CASTLE).getSiege().isInProgress())
		{
			final Npc cube = addSpawn(TELEPORT_CUBE, CUBE, false, 0);
			startQuestTimer("cube_despawn", 120000, cube, null);
		}
		
		cancelQuestTimer("raid_check", npc, null);
	}
	
	/**
	 * Alters the Venom location
	 * @param loc enum
	 */
	private void changeLocation(MoveTo loc)
	{
		switch (loc)
		{
			case THRONE:
			{
				_venom.teleToLocation(TRHONE, false);
				break;
			}
			case PRISON:
			{
				if ((_venom == null) || _venom.isDead() || _venom.isDecayed())
				{
					_venom = addSpawn(VENOM, DUNGEON, false, 0);
				}
				else
				{
					_venom.teleToLocation(DUNGEON, false);
				}
				
				cancelQuestTimer("raid_check", _venom, null);
				cancelQuestTimer("tower_check", _venom, null);
				break;
			}
		}
		
		_loc.setLocation(_venom.getLocation());
	}
	
	private void teleportTarget(Player player)
	{
		if ((player != null) && !player.isDead())
		{
			final int rnd = getRandom(11);
			player.teleToLocation(TARGET_TELEPORTS[rnd], TARGET_TELEPORTS_OFFSET[rnd]);
			player.getAI().setIntention(Intention.IDLE);
		}
	}
	
	/**
	 * Checks if Venom is Alive or Dead
	 * @return status
	 */
	private int checkStatus()
	{
		int checkStatus = ALIVE;
		if (GlobalVariablesManager.getInstance().hasVariable("VenomStatus"))
		{
			checkStatus = GlobalVariablesManager.getInstance().getInt("VenomStatus");
		}
		else
		{
			GlobalVariablesManager.getInstance().set("VenomStatus", 0);
		}
		
		return checkStatus;
	}
	
	/**
	 * Update the Venom status
	 * @param status the new status. 0 = ALIVE, 1 = DEAD.
	 */
	private void updateStatus(int status)
	{
		GlobalVariablesManager.getInstance().set("VenomStatus", Integer.toString(status));
	}
	
	private enum MoveTo
	{
		THRONE,
		PRISON
	}
	
	public static void main(String[] args)
	{
		new Venom();
	}
}
