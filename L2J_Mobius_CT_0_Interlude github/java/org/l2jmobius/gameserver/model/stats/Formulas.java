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
package org.l2jmobius.gameserver.model.stats;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.NpcConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.RatesConfig;
import org.l2jmobius.gameserver.config.custom.ChampionMonstersConfig;
import org.l2jmobius.gameserver.config.custom.ClassBalanceConfig;
import org.l2jmobius.gameserver.data.sql.ClanHallTable;
import org.l2jmobius.gameserver.data.xml.HitConditionBonusData;
import org.l2jmobius.gameserver.data.xml.KarmaLossData;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.SiegeManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.Cubic;
import org.l2jmobius.gameserver.model.actor.instance.SiegeFlag;
import org.l2jmobius.gameserver.model.actor.instance.StaticObject;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.effects.EffectType;
import org.l2jmobius.gameserver.model.item.Armor;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.Weapon;
import org.l2jmobius.gameserver.model.item.enums.ShotType;
import org.l2jmobius.gameserver.model.item.type.ArmorType;
import org.l2jmobius.gameserver.model.item.type.WeaponType;
import org.l2jmobius.gameserver.model.residences.ClanHall;
import org.l2jmobius.gameserver.model.sevensigns.SevenSigns;
import org.l2jmobius.gameserver.model.sevensigns.SevenSignsFestival;
import org.l2jmobius.gameserver.model.siege.Castle;
import org.l2jmobius.gameserver.model.siege.Siege;
import org.l2jmobius.gameserver.model.siege.SiegeClan;
import org.l2jmobius.gameserver.model.skill.BuffInfo;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.stats.functions.formulas.FuncArmorSet;
import org.l2jmobius.gameserver.model.stats.functions.formulas.FuncAtkAccuracy;
import org.l2jmobius.gameserver.model.stats.functions.formulas.FuncAtkCritical;
import org.l2jmobius.gameserver.model.stats.functions.formulas.FuncAtkEvasion;
import org.l2jmobius.gameserver.model.stats.functions.formulas.FuncGatesMDefMod;
import org.l2jmobius.gameserver.model.stats.functions.formulas.FuncGatesPDefMod;
import org.l2jmobius.gameserver.model.stats.functions.formulas.FuncHenna;
import org.l2jmobius.gameserver.model.stats.functions.formulas.FuncMAtkCritical;
import org.l2jmobius.gameserver.model.stats.functions.formulas.FuncMAtkMod;
import org.l2jmobius.gameserver.model.stats.functions.formulas.FuncMAtkSpeed;
import org.l2jmobius.gameserver.model.stats.functions.formulas.FuncMDefMod;
import org.l2jmobius.gameserver.model.stats.functions.formulas.FuncMaxCpMul;
import org.l2jmobius.gameserver.model.stats.functions.formulas.FuncMaxHpMul;
import org.l2jmobius.gameserver.model.stats.functions.formulas.FuncMaxMpMul;
import org.l2jmobius.gameserver.model.stats.functions.formulas.FuncMoveSpeed;
import org.l2jmobius.gameserver.model.stats.functions.formulas.FuncPAtkMod;
import org.l2jmobius.gameserver.model.stats.functions.formulas.FuncPAtkSpeed;
import org.l2jmobius.gameserver.model.stats.functions.formulas.FuncPDefMod;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.model.zone.type.CastleZone;
import org.l2jmobius.gameserver.model.zone.type.ClanHallZone;
import org.l2jmobius.gameserver.model.zone.type.MotherTreeZone;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.LocationUtil;
import org.l2jmobius.gameserver.util.MathUtil;

/**
 * Global calculations.
 */
public class Formulas
{
	/** Regeneration Task period. */
	private static final int HP_REGENERATE_PERIOD = 3000; // 3 secs
	
	public static final byte SHIELD_DEFENSE_FAILED = 0; // no shield defense
	public static final byte SHIELD_DEFENSE_SUCCEED = 1; // normal shield defense
	public static final byte SHIELD_DEFENSE_PERFECT_BLOCK = 2; // perfect block
	
	private static final byte MELEE_ATTACK_RANGE = 40;
	
	/**
	 * Return the period between 2 regeneration task (3s for Creature, 5 min for Door).
	 * @param creature
	 * @return
	 */
	public static int getRegeneratePeriod(Creature creature)
	{
		return creature.isDoor() ? HP_REGENERATE_PERIOD * 100 : HP_REGENERATE_PERIOD;
	}
	
	/**
	 * Return the standard NPC Calculator set containing ACCURACY_COMBAT and EVASION_RATE.<br>
	 * <br>
	 * <b><u>Concept</u>:</b><br>
	 * <br>
	 * A calculator is created to manage and dynamically calculate the effect of a character property (ex : MAX_HP, REGENERATE_HP_RATE...). In fact, each calculator is a table of Func object in which each Func represents a mathematic function :<br>
	 * FuncAtkAccuracy -> Math.sqrt(_player.getDEX())*6+_player.getLevel()<br>
	 * To reduce cache memory use, Npcs who don't have skills share the same Calculator set called <b>NPC_STD_CALCULATOR</b>.<br>
	 * @return
	 */
	public static Calculator[] getStdNPCCalculators()
	{
		final Calculator[] std = new Calculator[Stat.NUM_STATS];
		
		std[Stat.MAX_HP.ordinal()] = new Calculator();
		std[Stat.MAX_HP.ordinal()].addFunc(FuncMaxHpMul.getInstance());
		
		std[Stat.MAX_MP.ordinal()] = new Calculator();
		std[Stat.MAX_MP.ordinal()].addFunc(FuncMaxMpMul.getInstance());
		
		std[Stat.POWER_ATTACK.ordinal()] = new Calculator();
		std[Stat.POWER_ATTACK.ordinal()].addFunc(FuncPAtkMod.getInstance());
		
		std[Stat.MAGIC_ATTACK.ordinal()] = new Calculator();
		std[Stat.MAGIC_ATTACK.ordinal()].addFunc(FuncMAtkMod.getInstance());
		
		std[Stat.POWER_DEFENCE.ordinal()] = new Calculator();
		std[Stat.POWER_DEFENCE.ordinal()].addFunc(FuncPDefMod.getInstance());
		
		std[Stat.MAGIC_DEFENCE.ordinal()] = new Calculator();
		std[Stat.MAGIC_DEFENCE.ordinal()].addFunc(FuncMDefMod.getInstance());
		
		std[Stat.CRITICAL_RATE.ordinal()] = new Calculator();
		std[Stat.CRITICAL_RATE.ordinal()].addFunc(FuncAtkCritical.getInstance());
		
		std[Stat.MCRITICAL_RATE.ordinal()] = new Calculator();
		std[Stat.MCRITICAL_RATE.ordinal()].addFunc(FuncMAtkCritical.getInstance());
		
		std[Stat.ACCURACY_COMBAT.ordinal()] = new Calculator();
		std[Stat.ACCURACY_COMBAT.ordinal()].addFunc(FuncAtkAccuracy.getInstance());
		
		std[Stat.EVASION_RATE.ordinal()] = new Calculator();
		std[Stat.EVASION_RATE.ordinal()].addFunc(FuncAtkEvasion.getInstance());
		
		std[Stat.POWER_ATTACK_SPEED.ordinal()] = new Calculator();
		std[Stat.POWER_ATTACK_SPEED.ordinal()].addFunc(FuncPAtkSpeed.getInstance());
		
		std[Stat.MAGIC_ATTACK_SPEED.ordinal()] = new Calculator();
		std[Stat.MAGIC_ATTACK_SPEED.ordinal()].addFunc(FuncMAtkSpeed.getInstance());
		
		std[Stat.MOVE_SPEED.ordinal()] = new Calculator();
		std[Stat.MOVE_SPEED.ordinal()].addFunc(FuncMoveSpeed.getInstance());
		
		return std;
	}
	
	public static Calculator[] getStdDoorCalculators()
	{
		final Calculator[] std = new Calculator[Stat.NUM_STATS];
		
		// Add the FuncAtkAccuracy to the Standard Calculator of ACCURACY_COMBAT
		std[Stat.ACCURACY_COMBAT.ordinal()] = new Calculator();
		std[Stat.ACCURACY_COMBAT.ordinal()].addFunc(FuncAtkAccuracy.getInstance());
		
		// Add the FuncAtkEvasion to the Standard Calculator of EVASION_RATE
		std[Stat.EVASION_RATE.ordinal()] = new Calculator();
		std[Stat.EVASION_RATE.ordinal()].addFunc(FuncAtkEvasion.getInstance());
		
		// SevenSigns PDEF Modifier
		std[Stat.POWER_DEFENCE.ordinal()] = new Calculator();
		std[Stat.POWER_DEFENCE.ordinal()].addFunc(FuncGatesPDefMod.getInstance());
		
		// SevenSigns MDEF Modifier
		std[Stat.MAGIC_DEFENCE.ordinal()] = new Calculator();
		std[Stat.MAGIC_DEFENCE.ordinal()].addFunc(FuncGatesMDefMod.getInstance());
		
		return std;
	}
	
	/**
	 * Add basics Func objects to Player and Summon.<br>
	 * <br>
	 * <b><u>Concept</u>:</b><br>
	 * <br>
	 * A calculator is created to manage and dynamically calculate the effect of a character property (ex : MAX_HP, REGENERATE_HP_RATE...). In fact, each calculator is a table of Func object in which each Func represents a mathematic function :<br>
	 * FuncAtkAccuracy -> Math.sqrt(_player.getDEX())*6+_player.getLevel()
	 * @param creature Player or Summon that must obtain basic Func objects
	 */
	public static void addFuncsToNewCharacter(Creature creature)
	{
		if (creature.isPlayer())
		{
			creature.addStatFunc(FuncMaxHpMul.getInstance());
			creature.addStatFunc(FuncMaxCpMul.getInstance());
			creature.addStatFunc(FuncMaxMpMul.getInstance());
			creature.addStatFunc(FuncPAtkMod.getInstance());
			creature.addStatFunc(FuncMAtkMod.getInstance());
			creature.addStatFunc(FuncPDefMod.getInstance());
			creature.addStatFunc(FuncMDefMod.getInstance());
			creature.addStatFunc(FuncAtkCritical.getInstance());
			creature.addStatFunc(FuncMAtkCritical.getInstance());
			creature.addStatFunc(FuncAtkAccuracy.getInstance());
			creature.addStatFunc(FuncAtkEvasion.getInstance());
			creature.addStatFunc(FuncPAtkSpeed.getInstance());
			creature.addStatFunc(FuncMAtkSpeed.getInstance());
			creature.addStatFunc(FuncMoveSpeed.getInstance());
			
			creature.addStatFunc(FuncHenna.getInstance(Stat.STAT_STR));
			creature.addStatFunc(FuncHenna.getInstance(Stat.STAT_DEX));
			creature.addStatFunc(FuncHenna.getInstance(Stat.STAT_INT));
			creature.addStatFunc(FuncHenna.getInstance(Stat.STAT_MEN));
			creature.addStatFunc(FuncHenna.getInstance(Stat.STAT_CON));
			creature.addStatFunc(FuncHenna.getInstance(Stat.STAT_WIT));
			
			creature.addStatFunc(FuncArmorSet.getInstance(Stat.STAT_STR));
			creature.addStatFunc(FuncArmorSet.getInstance(Stat.STAT_DEX));
			creature.addStatFunc(FuncArmorSet.getInstance(Stat.STAT_INT));
			creature.addStatFunc(FuncArmorSet.getInstance(Stat.STAT_MEN));
			creature.addStatFunc(FuncArmorSet.getInstance(Stat.STAT_CON));
			creature.addStatFunc(FuncArmorSet.getInstance(Stat.STAT_WIT));
		}
		else if (creature.isSummon())
		{
			creature.addStatFunc(FuncMaxHpMul.getInstance());
			creature.addStatFunc(FuncMaxMpMul.getInstance());
			creature.addStatFunc(FuncPAtkMod.getInstance());
			creature.addStatFunc(FuncMAtkMod.getInstance());
			creature.addStatFunc(FuncPDefMod.getInstance());
			creature.addStatFunc(FuncMDefMod.getInstance());
			creature.addStatFunc(FuncAtkCritical.getInstance());
			creature.addStatFunc(FuncMAtkCritical.getInstance());
			creature.addStatFunc(FuncAtkAccuracy.getInstance());
			creature.addStatFunc(FuncAtkEvasion.getInstance());
			creature.addStatFunc(FuncMoveSpeed.getInstance());
			creature.addStatFunc(FuncPAtkSpeed.getInstance());
			creature.addStatFunc(FuncMAtkSpeed.getInstance());
		}
	}
	
	/**
	 * Calculate the HP regen rate (base + modifiers).
	 * @param creature
	 * @return
	 */
	public static double calcHpRegen(Creature creature)
	{
		double init = creature.isPlayer() ? creature.asPlayer().getTemplate().getBaseHpRegen(creature.getLevel()) : creature.getTemplate().getBaseHpReg();
		double hpRegenMultiplier = creature.isRaid() ? NpcConfig.RAID_HP_REGEN_MULTIPLIER : PlayerConfig.HP_REGEN_MULTIPLIER;
		double hpRegenBonus = 0;
		
		if (ChampionMonstersConfig.CHAMPION_ENABLE && creature.isChampion())
		{
			hpRegenMultiplier *= ChampionMonstersConfig.CHAMPION_HP_REGEN;
		}
		
		if (creature.isPlayer())
		{
			final Player player = creature.asPlayer();
			
			// SevenSigns Festival modifier
			if (SevenSignsFestival.getInstance().isFestivalInProgress() && player.isFestivalParticipant())
			{
				hpRegenMultiplier *= calcFestivalRegenModifier(player);
			}
			else
			{
				final double siegeModifier = calcSiegeRegenModifier(player);
				if (siegeModifier > 0)
				{
					hpRegenMultiplier *= siegeModifier;
				}
			}
			
			final Clan clan = player.getClan();
			if (player.isInsideZone(ZoneId.CLAN_HALL) && (clan != null) && (clan.getHideoutId() > 0))
			{
				final ClanHallZone zone = ZoneManager.getInstance().getZone(player, ClanHallZone.class);
				final int posChIndex = zone == null ? -1 : zone.getResidenceId();
				final int clanHallIndex = clan.getHideoutId();
				if ((clanHallIndex > 0) && (clanHallIndex == posChIndex))
				{
					final ClanHall clansHall = ClanHallTable.getInstance().getClanHallById(clanHallIndex);
					if ((clansHall != null) && (clansHall.getFunction(ClanHall.FUNC_RESTORE_HP) != null))
					{
						hpRegenMultiplier *= 1 + ((double) clansHall.getFunction(ClanHall.FUNC_RESTORE_HP).getLevel() / 100);
					}
				}
			}
			
			if (player.isInsideZone(ZoneId.CASTLE) && (clan != null) && (clan.getCastleId() > 0))
			{
				final CastleZone zone = ZoneManager.getInstance().getZone(player, CastleZone.class);
				final int posCastleIndex = zone == null ? -1 : zone.getResidenceId();
				final int castleIndex = clan.getCastleId();
				if ((castleIndex > 0) && (castleIndex == posCastleIndex))
				{
					final Castle castle = CastleManager.getInstance().getCastleById(castleIndex);
					if ((castle != null) && (castle.getFunction(Castle.FUNC_RESTORE_HP) != null))
					{
						hpRegenMultiplier *= 1 + ((double) castle.getFunction(Castle.FUNC_RESTORE_HP).getLvl() / 100);
					}
				}
			}
			
			// Mother Tree effect is calculated at last
			if (player.isInsideZone(ZoneId.MOTHER_TREE))
			{
				final MotherTreeZone zone = ZoneManager.getInstance().getZone(player, MotherTreeZone.class);
				final int hpBonus = zone == null ? 0 : zone.getHpRegenBonus();
				hpRegenBonus += hpBonus;
			}
			
			// Calculate Movement bonus
			if (player.isSitting())
			{
				hpRegenMultiplier *= 1.5; // Sitting
			}
			else if (!player.isMoving())
			{
				hpRegenMultiplier *= 1.1; // Staying
			}
			else if (player.isRunning())
			{
				hpRegenMultiplier *= 0.7; // Running
			}
			
			// Add CON bonus
			init *= creature.getLevelMod() * BaseStat.CON.calcBonus(creature);
		}
		else if (creature.isPet())
		{
			init = creature.asPet().getPetLevelData().getPetRegenHP() * NpcConfig.PET_HP_REGEN_MULTIPLIER;
		}
		
		return (creature.calcStat(Stat.REGENERATE_HP_RATE, Math.max(1, init), null, null) * hpRegenMultiplier) + hpRegenBonus;
	}
	
	/**
	 * Calculate the MP regen rate (base + modifiers).
	 * @param creature
	 * @return
	 */
	public static double calcMpRegen(Creature creature)
	{
		double init = creature.isPlayer() ? creature.asPlayer().getTemplate().getBaseMpRegen(creature.getLevel()) : creature.getTemplate().getBaseMpReg();
		double mpRegenMultiplier = creature.isRaid() ? NpcConfig.RAID_MP_REGEN_MULTIPLIER : PlayerConfig.MP_REGEN_MULTIPLIER;
		double mpRegenBonus = 0;
		
		if (creature.isPlayer())
		{
			final Player player = creature.asPlayer();
			
			// SevenSigns Festival modifier
			if (SevenSignsFestival.getInstance().isFestivalInProgress() && player.isFestivalParticipant())
			{
				mpRegenMultiplier *= calcFestivalRegenModifier(player);
			}
			
			// Mother Tree effect is calculated at last'
			if (player.isInsideZone(ZoneId.MOTHER_TREE))
			{
				final MotherTreeZone zone = ZoneManager.getInstance().getZone(player, MotherTreeZone.class);
				final int mpBonus = zone == null ? 0 : zone.getMpRegenBonus();
				mpRegenBonus += mpBonus;
			}
			
			final Clan clan = player.getClan();
			if (player.isInsideZone(ZoneId.CLAN_HALL) && (clan != null) && (clan.getHideoutId() > 0))
			{
				final ClanHallZone zone = ZoneManager.getInstance().getZone(player, ClanHallZone.class);
				final int posChIndex = zone == null ? -1 : zone.getResidenceId();
				final int clanHallIndex = clan.getHideoutId();
				if ((clanHallIndex > 0) && (clanHallIndex == posChIndex))
				{
					final ClanHall clansHall = ClanHallTable.getInstance().getClanHallById(clanHallIndex);
					if ((clansHall != null) && (clansHall.getFunction(ClanHall.FUNC_RESTORE_MP) != null))
					{
						mpRegenMultiplier *= 1 + ((double) clansHall.getFunction(ClanHall.FUNC_RESTORE_MP).getLevel() / 100);
					}
				}
			}
			
			if (player.isInsideZone(ZoneId.CASTLE) && (clan != null) && (clan.getCastleId() > 0))
			{
				final CastleZone zone = ZoneManager.getInstance().getZone(player, CastleZone.class);
				final int posCastleIndex = zone == null ? -1 : zone.getResidenceId();
				final int castleIndex = clan.getCastleId();
				if ((castleIndex > 0) && (castleIndex == posCastleIndex))
				{
					final Castle castle = CastleManager.getInstance().getCastleById(castleIndex);
					if ((castle != null) && (castle.getFunction(Castle.FUNC_RESTORE_MP) != null))
					{
						mpRegenMultiplier *= 1 + ((double) castle.getFunction(Castle.FUNC_RESTORE_MP).getLvl() / 100);
					}
				}
			}
			
			// Calculate Movement bonus
			if (player.isSitting())
			{
				mpRegenMultiplier *= 1.5; // Sitting
			}
			else if (!player.isMoving())
			{
				mpRegenMultiplier *= 1.1; // Staying
			}
			else if (player.isRunning())
			{
				mpRegenMultiplier *= 0.7; // Running
			}
			
			// Add MEN bonus
			init *= creature.getLevelMod() * BaseStat.MEN.calcBonus(creature);
		}
		else if (creature.isPet())
		{
			init = creature.asPet().getPetLevelData().getPetRegenMP() * NpcConfig.PET_MP_REGEN_MULTIPLIER;
		}
		
		return (creature.calcStat(Stat.REGENERATE_MP_RATE, Math.max(1, init), null, null) * mpRegenMultiplier) + mpRegenBonus;
	}
	
	/**
	 * Calculates the CP regeneration rate (base + modifiers).
	 * @param player the player
	 * @return the CP regeneration rate
	 */
	public static double calcCpRegen(Player player)
	{
		// With CON bonus
		final double init = player.asPlayer().getTemplate().getBaseCpRegen(player.getLevel()) * player.getLevelMod() * BaseStat.CON.calcBonus(player);
		double cpRegenMultiplier = PlayerConfig.CP_REGEN_MULTIPLIER;
		if (player.isSitting())
		{
			cpRegenMultiplier *= 1.5; // Sitting
		}
		else if (!player.isMoving())
		{
			cpRegenMultiplier *= 1.1; // Staying
		}
		else if (player.isRunning())
		{
			cpRegenMultiplier *= 0.7; // Running
		}
		
		return player.calcStat(Stat.REGENERATE_CP_RATE, Math.max(1, init), null, null) * cpRegenMultiplier;
	}
	
	public static double calcFestivalRegenModifier(Player player)
	{
		final int[] festivalInfo = SevenSignsFestival.getInstance().getFestivalForPlayer(player);
		final int oracle = festivalInfo[0];
		final int festivalId = festivalInfo[1];
		int[] festivalCenter;
		
		// If the player isn't found in the festival, leave the regen rate as it is.
		if (festivalId < 0)
		{
			return 0;
		}
		
		// Retrieve the X and Y coords for the center of the festival arena the player is in.
		if (oracle == SevenSigns.CABAL_DAWN)
		{
			festivalCenter = SevenSignsFestival.FESTIVAL_DAWN_PLAYER_SPAWNS[festivalId];
		}
		else
		{
			festivalCenter = SevenSignsFestival.FESTIVAL_DUSK_PLAYER_SPAWNS[festivalId];
		}
		
		// Check the distance between the player and the player spawn point, in the center of the arena.
		final double distToCenter = player.calculateDistance2D(festivalCenter[0], festivalCenter[1], 0);
		
		return 1.0 - (distToCenter * 0.0005); // Maximum Decreased Regen of ~ -65%;
	}
	
	public static double calcSiegeRegenModifier(Player player)
	{
		if (player == null)
		{
			return 0;
		}
		
		final Clan clan = player.getClan();
		if (clan == null)
		{
			return 0;
		}
		
		final Siege siege = SiegeManager.getInstance().getSiege(player.getX(), player.getY(), player.getZ());
		if ((siege == null) || !siege.isInProgress())
		{
			return 0;
		}
		
		final SiegeClan siegeClan = siege.getAttackerClan(clan.getId());
		if ((siegeClan == null) || siegeClan.getFlag().isEmpty() || !LocationUtil.checkIfInRange(200, player, siegeClan.getFlag().stream().findFirst().get(), true))
		{
			return 0;
		}
		
		return 1.5; // If all is true, then modifier will be 50% more
	}
	
	public static double calcBlowDamage(Creature attacker, Creature target, Skill skill, byte shld, boolean ss)
	{
		double defence = target.getPDef(attacker);
		
		switch (shld)
		{
			case SHIELD_DEFENSE_SUCCEED:
			{
				defence += target.getShldDef();
				break;
			}
			case SHIELD_DEFENSE_PERFECT_BLOCK: // perfect block
			{
				return 1;
			}
		}
		
		final boolean isPvP = attacker.isPlayable() && target.isPlayable();
		final boolean isPvE = attacker.isPlayable() && target.isAttackable();
		final double power = skill.getPower(isPvP, isPvE);
		double damage = 0;
		final double proximityBonus = attacker.isBehind(target) ? 1.2 : attacker.isInFrontOf(target) ? 1 : 1.1; // Behind: +20% - Side: +10% (TODO: values are unconfirmed, possibly custom, remove or update when confirmed);
		final double ssboost = ss ? 1.458 : 1;
		double pvpBonus = 1;
		
		if (isPvP)
		{
			// Damage bonuses in PvP fight
			pvpBonus = attacker.calcStat(Stat.PVP_PHYS_SKILL_DMG, 1, null, null);
			pvpBonus *= ClassBalanceConfig.PVP_BLOW_SKILL_DAMAGE_MULTIPLIERS[attacker.asPlayer().getPlayerClass().getId()];
			
			// Defense bonuses in PvP fight
			defence *= target.calcStat(Stat.PVP_PHYS_SKILL_DEF, 1, null, null);
			defence *= ClassBalanceConfig.PVP_BLOW_SKILL_DEFENCE_MULTIPLIERS[target.asPlayer().getPlayerClass().getId()];
		}
		
		if (isPvE)
		{
			pvpBonus *= ClassBalanceConfig.PVE_BLOW_SKILL_DAMAGE_MULTIPLIERS[attacker.asPlayer().getPlayerClass().getId()];
		}
		
		if (attacker.isAttackable() && target.isPlayable())
		{
			defence *= ClassBalanceConfig.PVE_BLOW_SKILL_DEFENCE_MULTIPLIERS[target.asPlayer().getPlayerClass().getId()];
		}
		
		// Initial damage
		final double baseMod = ((77 * (power + (attacker.getPAtk(target) * ssboost))) / defence);
		
		// Critical
		final double criticalMod = (attacker.calcStat(Stat.CRITICAL_DAMAGE, 1, target, skill));
		final double criticalModPos = (((attacker.calcStat(Stat.CRITICAL_DAMAGE_POS, 1, target, skill) - 1) / 2) + 1);
		final double criticalVulnMod = (target.calcStat(Stat.DEFENCE_CRITICAL_DAMAGE, 1, target, skill));
		final double criticalAddMod = ((attacker.getStat().calcStat(Stat.CRITICAL_DAMAGE_ADD, 0) * 6.1 * 77) / defence);
		final double criticalAddVuln = target.calcStat(Stat.DEFENCE_CRITICAL_DAMAGE_ADD, 0, target, skill);
		
		// Trait, elements
		final double weaponTraitMod = calcWeaponTraitBonus(attacker, target);
		final double generalTraitMod = calcGeneralTraitBonus(attacker, target, skill.getTraitType(), false);
		final double attributeMod = calcAttributeBonus(attacker, target, skill);
		
		// Random weapon damage for physical skills (blow).
		final double weaponMod = PlayerConfig.RANDOMIZE_PHYSICAL_SKILL_DAMAGE ? attacker.getRandomDamageMultiplier() : 1.0;
		
		damage = (baseMod * criticalMod * criticalModPos * criticalVulnMod * proximityBonus * pvpBonus) + criticalAddMod + criticalAddVuln;
		damage *= weaponTraitMod;
		damage *= generalTraitMod;
		damage *= attributeMod;
		damage *= weaponMod;
		
		return Math.max(damage, 1);
	}
	
	public static double calcBackstabDamage(Creature attacker, Creature target, Skill skill, byte shld, boolean ss)
	{
		double defence = target.getPDef(attacker);
		
		switch (shld)
		{
			case SHIELD_DEFENSE_SUCCEED:
			{
				defence += target.getShldDef();
				break;
			}
			case SHIELD_DEFENSE_PERFECT_BLOCK: // perfect block
			{
				return 1;
			}
		}
		
		final boolean isPvP = attacker.isPlayable() && target.isPlayer();
		final boolean isPvE = attacker.isPlayable() && target.isAttackable();
		double damage = 0;
		final double proximityBonus = attacker.isBehind(target) ? 1.2 : attacker.isInFrontOf(target) ? 1 : 1.1; // Behind: +20% - Side: +10%
		final double ssboost = ss ? 1.458 : 1;
		double pvpBonus = 1;
		
		if (isPvP)
		{
			// Damage bonuses in PvP fight
			pvpBonus = attacker.calcStat(Stat.PVP_PHYS_SKILL_DMG, 1, null, null);
			pvpBonus *= ClassBalanceConfig.PVP_PHYSICAL_SKILL_DAMAGE_MULTIPLIERS[attacker.asPlayer().getPlayerClass().getId()];
			
			// Defense bonuses in PvP fight
			defence *= target.calcStat(Stat.PVP_PHYS_SKILL_DEF, 1, null, null);
			defence *= ClassBalanceConfig.PVP_PHYSICAL_SKILL_DEFENCE_MULTIPLIERS[target.asPlayer().getPlayerClass().getId()];
		}
		
		// Initial damage
		final double baseMod = ((77 * (skill.getPower(isPvP, isPvE) + attacker.getPAtk(target))) / defence) * ssboost;
		
		// Critical
		final double criticalMod = (attacker.calcStat(Stat.CRITICAL_DAMAGE, 1, target, skill));
		final double criticalModPos = (((attacker.calcStat(Stat.CRITICAL_DAMAGE_POS, 1, target, skill) - 1) / 2) + 1);
		final double criticalVulnMod = (target.calcStat(Stat.DEFENCE_CRITICAL_DAMAGE, 1, target, skill));
		final double criticalAddMod = ((attacker.calcStat(Stat.CRITICAL_DAMAGE_ADD, 0, target, skill) * 6.1 * 77) / defence);
		final double criticalAddVuln = target.calcStat(Stat.DEFENCE_CRITICAL_DAMAGE_ADD, 0, target, skill);
		
		// Trait, elements
		final double generalTraitMod = calcGeneralTraitBonus(attacker, target, skill.getTraitType(), false);
		final double attributeMod = calcAttributeBonus(attacker, target, skill);
		
		// Random weapon damage for physical skills (backstab).
		final double weaponMod = PlayerConfig.RANDOMIZE_PHYSICAL_SKILL_DAMAGE ? attacker.getRandomDamageMultiplier() : 1.0;
		
		damage = (baseMod * criticalMod * criticalModPos * criticalVulnMod * proximityBonus * pvpBonus) + criticalAddMod + criticalAddVuln;
		damage *= generalTraitMod;
		damage *= attributeMod;
		damage *= weaponMod;
		
		return Math.max(damage, 1);
	}
	
	/**
	 * Calculated damage caused by ATTACK of attacker on target.
	 * @param attacker player or NPC that makes ATTACK
	 * @param target player or NPC, target of ATTACK
	 * @param skill
	 * @param shld
	 * @param crit if the ATTACK have critical success
	 * @param ss if weapon item was charged by soulshot
	 * @return
	 */
	public static double calcPhysDam(Creature attacker, Creature target, Skill skill, byte shld, boolean crit, boolean ss)
	{
		final boolean isPvP = attacker.isPlayable() && target.isPlayable();
		final boolean isPvE = attacker.isPlayable() && target.isAttackable();
		final double proximityBonus = attacker.isBehind(target) ? 1.2 : attacker.isInFrontOf(target) ? 1 : 1.1; // Behind: +20% - Side: +10%
		double damage = attacker.getPAtk(target);
		double defence = target.getPDef(attacker);
		
		// Defense bonuses in PvP fight
		if (isPvP)
		{
			defence *= (skill == null) ? target.calcStat(Stat.PVP_PHYSICAL_DEF, 1, null, null) : target.calcStat(Stat.PVP_PHYS_SKILL_DEF, 1, null, null);
			defence *= ClassBalanceConfig.PVP_PHYSICAL_SKILL_DEFENCE_MULTIPLIERS[target.asPlayer().getPlayerClass().getId()];
		}
		
		switch (shld)
		{
			case SHIELD_DEFENSE_SUCCEED:
			{
				if (!PlayerConfig.ALT_GAME_SHIELD_BLOCKS)
				{
					defence += target.getShldDef();
				}
				break;
			}
			case SHIELD_DEFENSE_PERFECT_BLOCK: // perfect block
			{
				return 1;
			}
		}
		
		// Add soulshot boost.
		final int ssBoost = ss ? 2 : 1;
		damage = (skill != null) ? ((damage * ssBoost) + skill.getPower(attacker, target, isPvP, isPvE)) : (damage * ssBoost);
		if (crit)
		{
			// H5 Damage Formula
			damage = 2 * attacker.calcStat(Stat.CRITICAL_DAMAGE, 1, target, skill) * attacker.calcStat(Stat.CRITICAL_DAMAGE_POS, 1, target, skill) * target.calcStat(Stat.DEFENCE_CRITICAL_DAMAGE, 1, target, null) * ((76 * damage * proximityBonus) / defence);
			damage += ((attacker.calcStat(Stat.CRITICAL_DAMAGE_ADD, 0, target, skill) * 77) / defence);
			damage += target.calcStat(Stat.DEFENCE_CRITICAL_DAMAGE_ADD, 0, target, skill);
			
			if (skill == null)
			{
				if (isPvP)
				{
					damage *= ClassBalanceConfig.PVP_PHYSICAL_ATTACK_CRITICAL_DAMAGE_MULTIPLIERS[attacker.asPlayer().getPlayerClass().getId()];
				}
				else if (isPvE)
				{
					damage *= ClassBalanceConfig.PVE_PHYSICAL_ATTACK_CRITICAL_DAMAGE_MULTIPLIERS[attacker.asPlayer().getPlayerClass().getId()];
				}
			}
			else
			{
				if (isPvP)
				{
					damage *= ClassBalanceConfig.PVP_PHYSICAL_SKILL_CRITICAL_DAMAGE_MULTIPLIERS[attacker.asPlayer().getPlayerClass().getId()];
				}
				else if (isPvE)
				{
					damage *= ClassBalanceConfig.PVE_PHYSICAL_SKILL_CRITICAL_DAMAGE_MULTIPLIERS[attacker.asPlayer().getPlayerClass().getId()];
				}
			}
		}
		else
		{
			damage = (76 * damage * proximityBonus) / defence;
		}
		
		damage *= calcAttackTraitBonus(attacker, target);
		
		// Weapon random damage.
		if (((skill == null) && PlayerConfig.RANDOMIZE_AUTO_ATTACK_DAMAGE) || ((skill != null) && PlayerConfig.RANDOMIZE_PHYSICAL_SKILL_DAMAGE))
		{
			damage *= attacker.getRandomDamageMultiplier();
		}
		
		damage *= attacker.getRandomDamageMultiplier();
		if ((shld > 0) && PlayerConfig.ALT_GAME_SHIELD_BLOCKS)
		{
			damage -= target.getShldDef();
			if (damage < 0)
			{
				damage = 0;
			}
		}
		
		if ((damage > 0) && (damage < 1))
		{
			damage = 1;
		}
		else if (damage < 0)
		{
			damage = 0;
		}
		
		// Dmg bonuses in PvP fight
		if (isPvP)
		{
			if (skill == null)
			{
				damage *= attacker.calcStat(Stat.PVP_PHYSICAL_DMG, 1, null, null);
				damage *= ClassBalanceConfig.PVP_PHYSICAL_ATTACK_DAMAGE_MULTIPLIERS[attacker.asPlayer().getPlayerClass().getId()];
			}
			else
			{
				damage *= attacker.calcStat(Stat.PVP_PHYS_SKILL_DMG, 1, null, null);
				damage *= ClassBalanceConfig.PVP_PHYSICAL_SKILL_DAMAGE_MULTIPLIERS[attacker.asPlayer().getPlayerClass().getId()];
			}
		}
		
		// Physical skill dmg boost
		if (skill != null)
		{
			damage = attacker.calcStat(Stat.PHYSICAL_SKILL_POWER, damage, null, null);
		}
		
		damage *= calcAttributeBonus(attacker, target, skill);
		if (target.isAttackable())
		{
			final Weapon weapon = attacker.getActiveWeaponItem();
			if ((weapon != null) && (weapon.getItemType() == WeaponType.BOW))
			{
				if (skill != null)
				{
					damage *= attacker.calcStat(Stat.PVE_BOW_SKILL_DMG, 1, null, null);
				}
				else
				{
					damage *= attacker.calcStat(Stat.PVE_BOW_DMG, 1, null, null);
				}
			}
			else
			{
				damage *= attacker.calcStat(Stat.PVE_PHYSICAL_DMG, 1, null, null);
				
				if (attacker.isPlayable())
				{
					if (skill == null)
					{
						damage *= ClassBalanceConfig.PVE_PHYSICAL_ATTACK_DAMAGE_MULTIPLIERS[attacker.asPlayer().getPlayerClass().getId()];
					}
					else
					{
						damage *= ClassBalanceConfig.PVE_PHYSICAL_SKILL_DAMAGE_MULTIPLIERS[attacker.asPlayer().getPlayerClass().getId()];
					}
				}
			}
		}
		
		return damage;
	}
	
	public static double calcMagicDam(Creature attacker, Creature target, Skill skill, byte shld, boolean sps, boolean bss, boolean mcrit)
	{
		double mDef = target.getMDef(attacker, skill);
		switch (shld)
		{
			case SHIELD_DEFENSE_SUCCEED:
			{
				mDef += target.getShldDef();
				break;
			}
			case SHIELD_DEFENSE_PERFECT_BLOCK:
			{
				return 1;
			}
		}
		
		double mAtk = attacker.getMAtk(target, skill);
		final boolean isPvP = attacker.isPlayable() && target.isPlayable();
		final boolean isPvE = attacker.isPlayable() && target.isAttackable();
		
		// PvP bonuses for defense
		if (isPvP)
		{
			if (skill.isMagic())
			{
				mDef *= target.calcStat(Stat.PVP_MAGICAL_DEF, 1, null, null);
				mDef *= ClassBalanceConfig.PVP_MAGICAL_SKILL_DEFENCE_MULTIPLIERS[target.asPlayer().getPlayerClass().getId()];
			}
			else
			{
				mDef *= target.calcStat(Stat.PVP_PHYS_SKILL_DEF, 1, null, null);
				mDef *= ClassBalanceConfig.PVP_PHYSICAL_SKILL_DEFENCE_MULTIPLIERS[target.asPlayer().getPlayerClass().getId()];
			}
		}
		
		// Bonus Spirit shot
		mAtk *= bss ? 4 : sps ? 2 : 1;
		
		// MDAM Formula.
		double damage = ((91 * Math.sqrt(mAtk)) / mDef) * skill.getPower(attacker, target, isPvP, isPvE);
		
		// Failure calculation
		if (PlayerConfig.ALT_GAME_MAGICFAILURES && !calcMagicSuccess(attacker, target, skill))
		{
			if (attacker.isPlayer())
			{
				if (calcMagicSuccess(attacker, target, skill) && ((target.getLevel() - attacker.getLevel()) <= 9))
				{
					if (skill.hasEffectType(EffectType.HP_DRAIN))
					{
						attacker.sendPacket(SystemMessageId.DRAIN_WAS_ONLY_50_PERCENT_SUCCESSFUL);
					}
					else
					{
						attacker.sendPacket(SystemMessageId.YOUR_ATTACK_HAS_FAILED);
					}
					
					damage /= 2;
				}
				else
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_RESISTED_YOUR_S2);
					sm.addString(target.getName());
					sm.addSkillName(skill);
					attacker.sendPacket(sm);
					damage = 1;
				}
			}
			
			if (target.isPlayer())
			{
				final SystemMessage sm = (skill.hasEffectType(EffectType.HP_DRAIN)) ? new SystemMessage(SystemMessageId.YOU_RESISTED_S1_S_DRAIN) : new SystemMessage(SystemMessageId.YOU_HAVE_RESISTED_S1_S_MAGIC);
				sm.addString(attacker.getName());
				target.sendPacket(sm);
			}
		}
		else if (mcrit)
		{
			damage *= attacker.isPlayer() && target.isPlayer() ? 2.5 : 3;
			damage *= attacker.calcStat(Stat.MAGIC_CRIT_DMG, 1, null, null);
			
			if (attacker.isPlayable())
			{
				damage *= target.isPlayable() ? ClassBalanceConfig.PVP_MAGICAL_SKILL_CRITICAL_DAMAGE_MULTIPLIERS[attacker.asPlayer().getPlayerClass().getId()] : ClassBalanceConfig.PVE_MAGICAL_SKILL_CRITICAL_DAMAGE_MULTIPLIERS[attacker.asPlayer().getPlayerClass().getId()];
			}
		}
		
		// Weapon random damage.
		if (PlayerConfig.RANDOMIZE_MAGICAL_SKILL_DAMAGE)
		{
			damage *= attacker.getRandomDamageMultiplier();
		}
		
		// PvP bonuses for damage
		if (isPvP)
		{
			final Stat stat;
			if (skill.isMagic())
			{
				stat = Stat.PVP_MAGICAL_DMG;
				damage *= ClassBalanceConfig.PVP_MAGICAL_SKILL_DAMAGE_MULTIPLIERS[attacker.asPlayer().getPlayerClass().getId()];
			}
			else
			{
				stat = Stat.PVP_PHYS_SKILL_DMG;
				damage *= ClassBalanceConfig.PVP_PHYSICAL_SKILL_DAMAGE_MULTIPLIERS[attacker.asPlayer().getPlayerClass().getId()];
			}
			damage *= attacker.calcStat(stat, 1, null, null);
		}
		
		damage *= calcAttributeBonus(attacker, target, skill);
		
		if (target.isAttackable())
		{
			damage *= attacker.calcStat(Stat.PVE_MAGICAL_DMG, 1, null, null);
			
			final Player attackerPlayer = attacker.asPlayer();
			if (attackerPlayer != null)
			{
				damage *= ClassBalanceConfig.PVE_MAGICAL_SKILL_DAMAGE_MULTIPLIERS[attackerPlayer.getPlayerClass().getId()];
			}
		}
		
		return damage;
	}
	
	public static double calcMagicDam(Cubic attacker, Creature target, Skill skill, boolean mcrit, byte shld)
	{
		double mDef = target.getMDef(attacker.getOwner(), skill);
		switch (shld)
		{
			case SHIELD_DEFENSE_SUCCEED:
			{
				mDef += target.getShldDef(); // kamael
				break;
			}
			case SHIELD_DEFENSE_PERFECT_BLOCK: // perfect block
			{
				return 1;
			}
		}
		
		final int mAtk = attacker.getCubicPower();
		final boolean isPvP = target.isPlayable();
		final boolean isPvE = target.isAttackable();
		
		// Cubics MDAM Formula (similar to PDAM formula, but using 91 instead of 70, also resisted by mDef).
		double damage = 91 * ((mAtk + skill.getPower(isPvP, isPvE)) / mDef);
		
		// Failure calculation
		final Player owner = attacker.getOwner();
		if (PlayerConfig.ALT_GAME_MAGICFAILURES && !calcMagicSuccess(owner, target, skill))
		{
			if (calcMagicSuccess(owner, target, skill) && ((target.getLevel() - skill.getMagicLevel()) <= 9))
			{
				if (skill.hasEffectType(EffectType.HP_DRAIN))
				{
					owner.sendPacket(SystemMessageId.DRAIN_WAS_ONLY_50_PERCENT_SUCCESSFUL);
				}
				else
				{
					owner.sendPacket(SystemMessageId.YOUR_ATTACK_HAS_FAILED);
				}
				
				damage /= 2;
			}
			else
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_RESISTED_YOUR_S2);
				sm.addString(target.getName());
				sm.addSkillName(skill);
				owner.sendPacket(sm);
				damage = 1;
			}
			
			if (target.isPlayer())
			{
				if (skill.hasEffectType(EffectType.HP_DRAIN))
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_RESISTED_S1_S_DRAIN);
					sm.addString(owner.getName());
					target.sendPacket(sm);
				}
				else
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_RESISTED_S1_S_MAGIC);
					sm.addString(owner.getName());
					target.sendPacket(sm);
				}
			}
		}
		else if (mcrit)
		{
			damage *= 3;
		}
		
		damage *= calcAttributeBonus(owner, target, skill);
		if (target.isAttackable())
		{
			damage *= owner.calcStat(Stat.PVE_MAGICAL_DMG, 1, null, null);
			damage *= ClassBalanceConfig.PVE_MAGICAL_SKILL_DAMAGE_MULTIPLIERS[owner.getPlayerClass().getId()];
		}
		
		return damage;
	}
	
	public static boolean calcCrit(Creature attacker, Creature target)
	{
		return calcCrit(attacker, target, null);
	}
	
	/**
	 * Returns true in case of critical hit
	 * @param attacker
	 * @param target
	 * @param skill
	 * @return
	 */
	public static boolean calcCrit(Creature attacker, Creature target, Skill skill)
	{
		double rate;
		if (skill != null)
		{
			rate = skill.getBaseCritRate() * 10 * BaseStat.STR.calcBonus(attacker);
		}
		else
		{
			rate = attacker.getStat().calcStat(Stat.CRITICAL_RATE_POS, attacker.getStat().getCriticalHit(target, null), target, skill);
		}
		
		final boolean isPvP = attacker.isPlayable() && target.isPlayable();
		if (skill == null)
		{
			if (isPvP)
			{
				rate *= ClassBalanceConfig.PVP_PHYSICAL_ATTACK_CRITICAL_CHANCE_MULTIPLIERS[attacker.asPlayer().getPlayerClass().getId()];
			}
			else if (attacker.isPlayable() && target.isAttackable()) // isPvE
			{
				rate *= ClassBalanceConfig.PVE_PHYSICAL_ATTACK_CRITICAL_CHANCE_MULTIPLIERS[attacker.asPlayer().getPlayerClass().getId()];
			}
		}
		else
		{
			if (isPvP)
			{
				rate *= ClassBalanceConfig.PVP_PHYSICAL_SKILL_CRITICAL_CHANCE_MULTIPLIERS[attacker.asPlayer().getPlayerClass().getId()];
			}
			else if (attacker.isPlayable() && target.isAttackable()) // isPvE
			{
				rate *= ClassBalanceConfig.PVE_PHYSICAL_SKILL_CRITICAL_CHANCE_MULTIPLIERS[attacker.asPlayer().getPlayerClass().getId()];
			}
		}
		
		return (target.getStat().calcStat(Stat.DEFENCE_CRITICAL_RATE, rate, null, null) + target.getStat().calcStat(Stat.DEFENCE_CRITICAL_RATE_ADD, 0, null, null)) > Rnd.get(1000);
	}
	
	public static boolean calcMCrit(double mRate)
	{
		return mRate > Rnd.get(1000);
	}
	
	/**
	 * @param target
	 * @param dmg
	 * @return true in case when ATTACK is canceled due to hit
	 */
	public static boolean calcAtkBreak(Creature target, double dmg)
	{
		if (target.isChanneling())
		{
			return false;
		}
		
		double init = 0;
		
		if (PlayerConfig.ALT_GAME_CANCEL_CAST && target.isCastingNow())
		{
			init = 15;
		}
		
		if (PlayerConfig.ALT_GAME_CANCEL_BOW && target.isAttackingNow())
		{
			final Weapon wpn = target.getActiveWeaponItem();
			if ((wpn != null) && (wpn.getItemType() == WeaponType.BOW))
			{
				init = 15;
			}
		}
		
		if (target.isRaid() || target.isInvul() || (init <= 0))
		{
			return false; // No attack break
		}
		
		// Chance of break is higher with higher dmg
		init += Math.sqrt(13 * dmg);
		
		// Chance is affected by target MEN
		init -= ((BaseStat.MEN.calcBonus(target) * 100) - 100);
		
		// Calculate all modifiers for ATTACK_CANCEL
		double rate = target.calcStat(Stat.ATTACK_CANCEL, init, null, null);
		
		// Adjust the rate to be between 1 and 99
		rate = Math.max(Math.min(rate, 99), 1);
		
		return Rnd.get(100) < rate;
	}
	
	/**
	 * Calculate delay (in milliseconds) before next ATTACK
	 * @param attacker
	 * @param target
	 * @param rate
	 * @return
	 */
	public static int calcPAtkSpd(Creature attacker, Creature target, double rate)
	{
		// measured Oct 2006 by Tank6585, formula by Sami
		// attack speed 312 equals 1500 ms delay... (or 300 + 40 ms delay?)
		if (rate < 2)
		{
			return 2700;
		}
		
		return (int) (470000 / rate);
	}
	
	/**
	 * Calculate delay (in milliseconds) for skills cast
	 * @param attacker
	 * @param skill
	 * @param skillTime
	 * @return
	 */
	public static int calcAtkSpd(Creature attacker, Skill skill, double skillTime)
	{
		if (skill.isMagic())
		{
			return (int) ((skillTime / attacker.getMAtkSpd()) * 333);
		}
		
		return (int) ((skillTime / attacker.getPAtkSpd()) * 300);
	}
	
	/**
	 * Formula based on http://l2p.l2wh.com/nonskillattacks.html
	 * @param attacker
	 * @param target
	 * @return {@code true} if hit missed (target evaded), {@code false} otherwise.
	 */
	public static boolean calcHitMiss(Creature attacker, Creature target)
	{
		int chance = (80 + (2 * (attacker.getAccuracy() - target.getEvasionRate(attacker)))) * 10;
		
		// Get additional bonus from the conditions when you are attacking
		chance *= HitConditionBonusData.getInstance().getConditionBonus(attacker, target);
		
		chance = Math.max(chance, 200);
		chance = Math.min(chance, 980);
		
		return chance < Rnd.get(1000);
	}
	
	/**
	 * Returns:<br>
	 * 0 = shield defense doesn't succeed<br>
	 * 1 = shield defense succeed<br>
	 * 2 = perfect block
	 * @param attacker
	 * @param target
	 * @param skill
	 * @param sendSysMsg
	 * @return
	 */
	public static byte calcShldUse(Creature attacker, Creature target, Skill skill, boolean sendSysMsg)
	{
		if ((skill != null) && skill.ignoreShield())
		{
			return 0;
		}
		
		final ItemTemplate item = target.getSecondaryWeaponItem();
		if ((item == null) || !(item instanceof Armor) || (((Armor) item).getItemType() == ArmorType.SIGIL))
		{
			return 0;
		}
		
		double shldRate = target.calcStat(Stat.SHIELD_RATE, 0, attacker, null) * BaseStat.DEX.calcBonus(target);
		if (shldRate <= 1e-6)
		{
			return 0;
		}
		
		final int degreeside = (int) target.calcStat(Stat.SHIELD_DEFENCE_ANGLE, 0, null, null) + 120;
		if ((degreeside < 360) && (Math.abs(target.calculateDirectionTo(attacker) - LocationUtil.convertHeadingToDegree(target.getHeading())) > (degreeside / 2)))
		{
			return 0;
		}
		
		byte shldSuccess = SHIELD_DEFENSE_FAILED;
		
		// if attacker use bow and target wear shield, shield block rate is multiplied by 1.3 (30%)
		final Weapon atWeapon = attacker.getActiveWeaponItem();
		if ((atWeapon != null) && (atWeapon.getItemType() == WeaponType.BOW))
		{
			shldRate *= 1.3;
		}
		
		if ((shldRate > 0) && ((100 - PlayerConfig.ALT_PERFECT_SHLD_BLOCK) < Rnd.get(100)))
		{
			shldSuccess = SHIELD_DEFENSE_PERFECT_BLOCK;
		}
		else if (shldRate > Rnd.get(100))
		{
			shldSuccess = SHIELD_DEFENSE_SUCCEED;
		}
		
		if (sendSysMsg && target.isPlayer())
		{
			final Player enemy = target.asPlayer();
			
			switch (shldSuccess)
			{
				case SHIELD_DEFENSE_SUCCEED:
				{
					enemy.sendPacket(SystemMessageId.YOUR_SHIELD_DEFENSE_HAS_SUCCEEDED);
					break;
				}
				case SHIELD_DEFENSE_PERFECT_BLOCK:
				{
					enemy.sendPacket(SystemMessageId.YOUR_EXCELLENT_SHIELD_DEFENSE_WAS_A_SUCCESS);
					break;
				}
			}
		}
		
		return shldSuccess;
	}
	
	public static byte calcShldUse(Creature attacker, Creature target, Skill skill)
	{
		return calcShldUse(attacker, target, skill, true);
	}
	
	public static byte calcShldUse(Creature attacker, Creature target)
	{
		return calcShldUse(attacker, target, null, true);
	}
	
	public static boolean calcMagicAffected(Creature actor, Creature target, Skill skill)
	{
		// TODO: CHECK/FIX THIS FORMULA UP!!
		double defence = 0;
		if (skill.isActive() && skill.hasNegativeEffect())
		{
			defence = target.getMDef(actor, skill);
		}
		
		final double attack = 2 * actor.getMAtk(target, skill) * calcGeneralTraitBonus(actor, target, skill.getTraitType(), false);
		double d = (attack - defence) / (attack + defence);
		
		if (skill.isDebuff())
		{
			if (target.calcStat(Stat.DEBUFF_IMMUNITY, 0, null, skill) > 0)
			{
				return false;
			}
		}
		
		d += 0.5 * Rnd.nextGaussian();
		return d > 0;
	}
	
	public static double calcLvlBonusMod(Creature attacker, Creature target, Skill skill)
	{
		final int attackerLvl = skill.getMagicLevel() > 0 ? skill.getMagicLevel() : attacker.getLevel();
		final double skillLevelBonusRateMod = 1 + (skill.getLvlBonusRate() / 100.);
		final double lvlMod = 1 + ((attackerLvl - target.getLevel()) / 100.);
		return skillLevelBonusRateMod * lvlMod;
	}
	
	/**
	 * Calculates the effect landing success.
	 * @param attacker the attacker
	 * @param target the target
	 * @param skill the skill
	 * @return {@code true} if the effect lands
	 */
	public static boolean calcEffectSuccess(Creature attacker, Creature target, Skill skill)
	{
		// StaticObjects can not receive continuous effects.
		if (target.isDoor() || (target instanceof SiegeFlag) || (target instanceof StaticObject))
		{
			return false;
		}
		
		if (skill.isDebuff() && (target.calcStat(Stat.DEBUFF_IMMUNITY, 0, attacker, skill) > 0))
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_RESISTED_YOUR_S2);
			sm.addString(target.getName());
			sm.addSkillName(skill);
			attacker.sendPacket(sm);
			return false;
		}
		
		final int activateRate = skill.getActivateRate();
		if ((activateRate == -1) || (activateRate > 99) || (skill.getBasicProperty() == BaseStat.NONE))
		{
			return true;
		}
		
		int magicLevel = skill.getMagicLevel();
		if (magicLevel <= -1)
		{
			magicLevel = target.getLevel() + 3;
		}
		
		int targetBaseStat = 0;
		switch (skill.getBasicProperty())
		{
			case STR:
			{
				targetBaseStat = target.getSTR();
				break;
			}
			case DEX:
			{
				targetBaseStat = target.getDEX();
				break;
			}
			case CON:
			{
				targetBaseStat = target.getCON();
				break;
			}
			case INT:
			{
				targetBaseStat = target.getINT();
				break;
			}
			case MEN:
			{
				targetBaseStat = target.getMEN();
				break;
			}
			case WIT:
			{
				targetBaseStat = target.getWIT();
				break;
			}
		}
		
		final double baseMod = ((((((magicLevel - target.getLevel()) + 3) * skill.getLvlBonusRate()) + activateRate) + 30.0) - targetBaseStat);
		final double elementMod = calcAttributeBonus(attacker, target, skill);
		final double traitMod = calcGeneralTraitBonus(attacker, target, skill.getTraitType(), false);
		final double buffDebuffMod = 1 + (target.calcStat(skill.isDebuff() ? Stat.DEBUFF_VULN : Stat.BUFF_VULN, 1, null, null) / 100);
		double mAtkMod = 1;
		
		if (skill.isMagic())
		{
			final double mAtk = attacker.getMAtk(null, null);
			double val = 0;
			if (attacker.isChargedShot(ShotType.BLESSED_SPIRITSHOTS))
			{
				val = mAtk * 3.0; // 3.0 is the blessed spiritshot multiplier
			}
			
			val += mAtk;
			val = (Math.sqrt(val) / target.getMDef(null, null)) * 11.0;
			mAtkMod = val;
		}
		
		final double rate = baseMod * elementMod * traitMod * mAtkMod * buffDebuffMod;
		final double finalRate = traitMod > 0 ? MathUtil.clamp(rate, skill.getMinChance(), skill.getMaxChance()) : 0;
		
		if (finalRate <= Rnd.get(100))
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_RESISTED_YOUR_S2);
			sm.addString(target.getName());
			sm.addSkillName(skill);
			attacker.sendPacket(sm);
			return false;
		}
		
		return true;
	}
	
	public static boolean calcCubicSkillSuccess(Cubic attacker, Creature target, Skill skill, byte shld)
	{
		if (skill.isDebuff())
		{
			if (skill.getPower() == -1)
			{
				return true;
			}
			else if (target.calcStat(Stat.DEBUFF_IMMUNITY, 0, null, skill) > 0)
			{
				return false;
			}
		}
		
		// Perfect Shield Block.
		if (shld == SHIELD_DEFENSE_PERFECT_BLOCK)
		{
			return false;
		}
		
		// if target reflect this skill then the effect will fail
		if (calcBuffDebuffReflection(target, skill))
		{
			return false;
		}
		
		// Calculate BaseRate.
		final double baseRate = skill.getPower();
		final double statMod = skill.getBasicProperty().calcBonus(target);
		double rate = (baseRate / statMod);
		
		// Resist Modifier.
		final double resMod = calcGeneralTraitBonus(attacker.getOwner(), target, skill.getTraitType(), false);
		rate *= resMod;
		
		// Lvl Bonus Modifier.
		final double lvlBonusMod = calcLvlBonusMod(attacker.getOwner(), target, skill);
		rate *= lvlBonusMod;
		
		// Element Modifier.
		final double elementMod = calcAttributeBonus(attacker.getOwner(), target, skill);
		rate *= elementMod;
		
		// Add Matk/Mdef Bonus (TODO: Pending)
		
		// Check the Rate Limits.
		final double finalRate = MathUtil.clamp(rate, skill.getMinChance(), skill.getMaxChance());
		
		return (Rnd.get(100) < finalRate);
	}
	
	public static boolean calcMagicSuccess(Creature attacker, Creature target, Skill skill)
	{
		if (skill.getPower() == -1)
		{
			return true;
		}
		
		final double lvlModifier = Math.pow(1.3, target.getLevel() - (PlayerConfig.CALCULATE_MAGIC_SUCCESS_BY_SKILL_MAGIC_LEVEL && (skill.getMagicLevel() > 0) ? skill.getMagicLevel() : attacker.getLevel()));
		
		// general magic resist
		final double resModifier = target.calcStat(Stat.MAGIC_SUCCESS_RES, 1, null, skill);
		final int rate = 100 - Math.round((float) (lvlModifier * resModifier));
		
		return (Rnd.get(100) < rate);
	}
	
	public static double calcManaDam(Creature attacker, Creature target, Skill skill, byte shld, boolean sps, boolean bss, boolean mcrit)
	{
		// Formula: (SQR(M.Atk)*Power*(Target Max MP/97))/M.Def
		double mAtk = attacker.getMAtk(target, skill);
		double mDef = target.getMDef(attacker, skill);
		final boolean isPvP = attacker.isPlayable() && target.isPlayable();
		final boolean isPvE = attacker.isPlayable() && target.isAttackable();
		final double mp = target.getMaxMp();
		
		switch (shld)
		{
			case SHIELD_DEFENSE_SUCCEED:
			{
				mDef += target.getShldDef();
				break;
			}
			case SHIELD_DEFENSE_PERFECT_BLOCK: // perfect block
			{
				return 1;
			}
		}
		
		// Bonus Spiritshot
		mAtk *= bss ? 4 : sps ? 2 : 1;
		
		double damage = (Math.sqrt(mAtk) * skill.getPower(attacker, target, isPvP, isPvE) * (mp / 97)) / mDef;
		damage *= calcGeneralTraitBonus(attacker, target, skill.getTraitType(), false);
		
		if (target.isAttackable())
		{
			damage *= attacker.calcStat(Stat.PVE_MAGICAL_DMG, 1, null, null);
			
			final Player attackerPlayer = attacker.asPlayer();
			if (attackerPlayer != null)
			{
				damage *= ClassBalanceConfig.PVE_MAGICAL_SKILL_DAMAGE_MULTIPLIERS[attackerPlayer.getPlayerClass().getId()];
			}
		}
		
		// Failure calculation
		if (PlayerConfig.ALT_GAME_MAGICFAILURES && !calcMagicSuccess(attacker, target, skill))
		{
			if (attacker.isPlayer())
			{
				attacker.sendMessage("Damage is decreased because " + target.getName() + " resisted " + attacker.getName() + "'s magic.");
				damage /= 2;
			}
			
			if (target.isPlayer())
			{
				target.sendMessage(target.getName() + " weakly resisted " + attacker.getName() + "'s magic.");
			}
		}
		
		if (mcrit)
		{
			damage *= 3;
			attacker.sendPacket(SystemMessageId.MAGIC_CRITICAL_HIT);
		}
		
		return damage;
	}
	
	public static double calculateSkillResurrectRestorePercent(double baseRestorePercent, Creature caster)
	{
		if ((baseRestorePercent == 0) || (baseRestorePercent == 100))
		{
			return baseRestorePercent;
		}
		
		double restorePercent = baseRestorePercent * BaseStat.WIT.calcBonus(caster);
		if ((restorePercent - baseRestorePercent) > 20.0)
		{
			restorePercent += 20.0;
		}
		
		restorePercent = Math.max(restorePercent, baseRestorePercent);
		restorePercent = Math.min(restorePercent, 90.0);
		
		return restorePercent;
	}
	
	public static boolean calcPhysicalSkillEvasion(Creature creature, Creature target, Skill skill)
	{
		if (skill.isMagic() || skill.isDebuff())
		{
			return false;
		}
		
		if (Rnd.get(100) < target.calcStat(Stat.P_SKILL_EVASION, 0, null, skill))
		{
			if (creature.isPlayer())
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.S1_DODGES_THE_ATTACK);
				sm.addString(target.getName());
				creature.asPlayer().sendPacket(sm);
			}
			
			if (target.isPlayer())
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_DODGE_S1_S_ATTACK);
				sm.addString(creature.getName());
				target.asPlayer().sendPacket(sm);
			}
			
			return true;
		}
		
		return false;
	}
	
	public static boolean calcSkillMastery(Creature actor, Skill sk)
	{
		// Non players are not affected by Skill Mastery.
		if (!actor.isPlayer())
		{
			return false;
		}
		
		// Static Skills are not affected by Skill Mastery.
		if (sk.isStatic())
		{
			return false;
		}
		
		final int val = (int) actor.getStat().calcStat(Stat.SKILL_CRITICAL, 0, null, null);
		if (val == 0)
		{
			return false;
		}
		
		double chance = 0;
		switch (val)
		{
			case 1:
			{
				chance = (BaseStat.STR).calcBonus(actor);
				break;
			}
			case 4:
			{
				chance = (BaseStat.INT).calcBonus(actor);
				break;
			}
		}
		
		chance *= actor.getStat().calcStat(Stat.SKILL_CRITICAL_PROBABILITY, 1, null, null);
		return (Rnd.get(100) < (chance * ClassBalanceConfig.SKILL_MASTERY_CHANCE_MULTIPLIERS[actor.asPlayer().getPlayerClass().getId()]));
	}
	
	/**
	 * Calculates the attribute bonus with the following formula:<br>
	 * diff > 0, so AttBonus = 1,025 + sqrt[(diff^3) / 2] * 0,0001, cannot be above 1,25!<br>
	 * diff < 0, so AttBonus = 0,975 - sqrt[(diff^3) / 2] * 0,0001, cannot be below 0,75!<br>
	 * diff == 0, so AttBonus = 1<br>
	 * It has been tested that physical skills do get affected by attack attribute even<br>
	 * if they don't have any attribute. In that case only the biggest attack attribute is taken.
	 * @param attacker
	 * @param target
	 * @param skill Can be {@code null} if there is no skill used for the attack.
	 * @return The attribute bonus
	 */
	public static double calcAttributeBonus(Creature attacker, Creature target, Skill skill)
	{
		int attackAttribute;
		int defenceAttribute;
		
		if ((skill != null) && (skill.getElement() != -1))
		{
			attackAttribute = attacker.getAttackElementValue(skill.getElement()) + skill.getElementPower();
			defenceAttribute = target.getDefenseElementValue(skill.getElement());
		}
		else
		{
			attackAttribute = attacker.getAttackElementValue(attacker.getAttackElement());
			defenceAttribute = target.getDefenseElementValue(attacker.getAttackElement());
		}
		
		final int diff = attackAttribute - defenceAttribute;
		if (diff > 0)
		{
			return Math.min(1.025 + (Math.sqrt(Math.pow(diff, 3) / 2) * 0.0001), 1.25);
		}
		else if (diff < 0)
		{
			return Math.max(0.975 - (Math.sqrt(Math.pow(-diff, 3) / 2) * 0.0001), 0.75);
		}
		
		return 1;
	}
	
	public static void calcDamageReflected(Creature attacker, Creature target, Skill skill, boolean crit)
	{
		// Only melee skills can be reflected
		if (skill.isMagic() || (skill.getCastRange() > MELEE_ATTACK_RANGE))
		{
			return;
		}
		
		final double chance = target.calcStat(Stat.VENGEANCE_SKILL_PHYSICAL_DAMAGE, 0, target, skill);
		if (Rnd.get(100) < chance)
		{
			if (target.isPlayer())
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_COUNTER_ATTACK_S1_S_ATTACK);
				sm.addString(attacker.getName());
				target.sendPacket(sm);
			}
			
			if (attacker.isPlayer())
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.S1_IS_PERFORMING_A_COUNTER_ATTACK);
				sm.addString(target.getName());
				attacker.sendPacket(sm);
			}
			
			double counterdmg = (((target.getPAtk(attacker) * 10.0) * 70.0) / attacker.getPDef(target));
			counterdmg *= calcWeaponTraitBonus(attacker, target);
			counterdmg *= calcGeneralTraitBonus(attacker, target, skill.getTraitType(), false);
			counterdmg *= calcAttributeBonus(attacker, target, skill);
			attacker.reduceCurrentHp(counterdmg, target, skill);
			if (crit) // TODO: It counters multiple times depending on how much effects skill has not on critical, but gotta be verified first!
			{
				attacker.reduceCurrentHp(counterdmg, target, skill);
			}
		}
	}
	
	/**
	 * Calculate buff/debuff reflection.
	 * @param target
	 * @param skill
	 * @return {@code true} if reflect, {@code false} otherwise.
	 */
	public static boolean calcBuffDebuffReflection(Creature target, Skill skill)
	{
		if (!skill.isDebuff() || (skill.getActivateRate() == -1))
		{
			return false;
		}
		
		final double reflectChance = target.calcStat(skill.isMagic() ? Stat.REFLECT_SKILL_MAGIC : Stat.REFLECT_SKILL_PHYSIC, 0, null, skill);
		return reflectChance > Rnd.get(100);
	}
	
	/**
	 * Calculate damage caused by falling
	 * @param creature
	 * @param fallHeight
	 * @return damage
	 */
	public static double calcFallDam(Creature creature, int fallHeight)
	{
		if (!GeneralConfig.ENABLE_FALLING_DAMAGE || (fallHeight < 0))
		{
			return 0;
		}
		
		return creature.calcStat(Stat.FALL, (fallHeight * creature.getMaxHp()) / 1000, null, null);
	}
	
	public static boolean calcBlowSuccess(Creature creature, Creature target, Skill skill)
	{
		final double dexMod = BaseStat.DEX.calcBonus(creature);
		
		// Apply DEX Mod.
		final double blowChance = skill.getBlowChance();
		
		// Apply Position Bonus (TODO: values are unconfirmed, possibly custom, remove or update when confirmed).
		final double sideMod = (creature.isInFrontOf(target)) ? 1 : (creature.isBehind(target)) ? 2 : 1.5;
		
		// Apply all mods.
		final double baseRate = blowChance * dexMod * sideMod;
		
		// Apply blow rates
		final double rate = creature.calcStat(Stat.BLOW_RATE, baseRate, target, null);
		return Rnd.get(100) < rate;
	}
	
	public static List<BuffInfo> calcStealEffects(Creature target, int amount)
	{
		final List<BuffInfo> canceled = new ArrayList<>(amount);
		
		// Prevent initialization.
		final List<BuffInfo> buffs = target.getEffectList().hasBuffs() ? new ArrayList<>(target.getEffectList().getBuffs()) : new ArrayList<>(1);
		if (target.getEffectList().hasDances())
		{
			buffs.addAll(target.getEffectList().getDances());
		}
		
		for (int i = buffs.size() - 1; (i >= 0) && (canceled.size() < amount); i--) // reverse order
		{
			final BuffInfo info = buffs.get(i);
			if (!info.getSkill().canBeStolen())
			{
				continue;
			}
			
			canceled.add(info);
		}
		
		return canceled;
	}
	
	public static List<BuffInfo> calcCancelEffects(Creature creature, Creature target, Skill skill, String slot, int rate, int max)
	{
		final List<BuffInfo> canceled = new ArrayList<>(max);
		switch (slot)
		{
			case "buff":
			{
				// Resist Modifier.
				final int cancelMagicLvl = skill == null ? creature.getLevel() : skill.getMagicLevel();
				final double vuln = target.calcStat(Stat.CANCEL_VULN, 0, target, null);
				final double prof = creature.calcStat(Stat.CANCEL_PROF, 0, target, null);
				final double resMod = 1 + (((vuln + prof) * -1) / 100);
				final double finalRate = rate / resMod;
				
				// Prevent initialization.
				final List<BuffInfo> buffs = target.getEffectList().hasBuffs() ? new ArrayList<>(target.getEffectList().getBuffs()) : new ArrayList<>(1);
				if (target.getEffectList().hasDances())
				{
					buffs.addAll(target.getEffectList().getDances());
				}
				
				for (int i = buffs.size() - 1; i >= 0; i--) // reverse order
				{
					final BuffInfo info = buffs.get(i);
					if (!info.getSkill().canBeStolen() || (!calcCancelSuccess(info, cancelMagicLvl, (int) finalRate, skill)))
					{
						continue;
					}
					
					canceled.add(info);
					if (canceled.size() >= max)
					{
						break;
					}
				}
				break;
			}
			case "debuff":
			{
				final List<BuffInfo> debuffs = new ArrayList<>(target.getEffectList().getDebuffs());
				for (int i = debuffs.size() - 1; i >= 0; i--)
				{
					final BuffInfo info = debuffs.get(i);
					if (info.getSkill().isDebuff() && info.getSkill().canBeDispeled() && (Rnd.get(100) <= rate))
					{
						canceled.add(info);
						if (canceled.size() >= max)
						{
							break;
						}
					}
				}
				break;
			}
		}
		
		return canceled;
	}
	
	public static boolean calcCancelSuccess(BuffInfo info, int cancelMagicLvl, int rate, Skill skill)
	{
		// Lvl Bonus Modifier.
		final int chance = (int) (rate * (info.getSkill().getMagicLevel() > 0 ? 1 + ((cancelMagicLvl - info.getSkill().getMagicLevel()) / 100.) : 1));
		return Rnd.get(100) < MathUtil.clamp(chance, skill.getMinChance(), skill.getMaxChance());
	}
	
	/**
	 * Calculates the abnormal time for an effect.<br>
	 * The abnormal time is taken from the skill definition, and it's global for all effects present in the skills.
	 * @param caster the caster
	 * @param target the target
	 * @param skill the skill
	 * @return the time that the effect will last
	 */
	public static int calcEffectAbnormalTime(Creature caster, Creature target, Skill skill)
	{
		int time = skill.isPassive() || skill.isToggle() ? -1 : skill.getAbnormalTime();
		
		// An herb buff will affect both master and servitor, but the buff duration will be half of the normal duration.
		// If a servitor is not summoned, the master will receive the full buff duration.
		if ((target != null) && target.isServitor() && skill.isAbnormalInstant())
		{
			time /= 2;
		}
		
		// If the skill is a mastery skill, the effect will last twice the default time.
		if (calcSkillMastery(caster, skill))
		{
			time *= 2;
		}
		
		// Debuffs Duration Affected by Resistances.
		if ((caster != null) && (target != null) && skill.isDebuff() && GeneralConfig.DEBUFF_DURATION_USES_RESISTS)
		{
			final double statMod = skill.getBasicProperty().calcBonus(target);
			final double resMod = calcGeneralTraitBonus(caster, target, skill.getTraitType(), false);
			final double lvlBonusMod = calcLvlBonusMod(caster, target, skill);
			final double elementMod = calcAttributeBonus(caster, target, skill);
			time = (int) Math.ceil(MathUtil.clamp(((time * resMod * lvlBonusMod * elementMod) / statMod), (time * 0.5), time));
		}
		
		return time;
	}
	
	/**
	 * Calculate Probability in following effects:<br>
	 * TargetCancel,<br>
	 * TargetMeProbability,<br>
	 * SkillTurning,<br>
	 * Betray,<br>
	 * Bluff,<br>
	 * DeleteHate,<br>
	 * RandomizeHate,<br>
	 * DeleteHateOfMe,<br>
	 * TransferHate,<br>
	 * Confuse
	 * @param baseChance chance from effect parameter
	 * @param attacker
	 * @param target
	 * @param skill
	 * @return chance for effect to succeed
	 */
	public static boolean calcProbability(double baseChance, Creature attacker, Creature target, Skill skill)
	{
		return Rnd.get(100) < ((((((skill.getMagicLevel() + baseChance) - target.getLevel()) + 30) - target.getINT()) * calcAttributeBonus(attacker, target, skill)) * calcGeneralTraitBonus(attacker, target, skill.getTraitType(), false));
	}
	
	/**
	 * Calculates karma lost upon death.
	 * @param player
	 * @param finalExp
	 * @return the amount of karma player has lost.
	 */
	public static int calculateKarmaLost(Player player, long finalExp)
	{
		final double karmaLossMod = KarmaLossData.getInstance().getModifier(player.getLevel());
		if (finalExp > 0) // Received experience.
		{
			return (int) ((Math.abs(finalExp / RatesConfig.RATE_KARMA_LOST) / karmaLossMod) / 30);
		}
		
		return (int) ((Math.abs(finalExp) / karmaLossMod) / 30);
	}
	
	/**
	 * Calculates karma gain upon playable kill.</br>
	 * Updated to High Five on 10.09.2014 by Zealar tested in retail.
	 * @param pkCount
	 * @param isSummon
	 * @return karma points that will be added to the player.
	 */
	public static int calculateKarmaGain(int pkCount, boolean isSummon)
	{
		int result = 43200;
		if (isSummon)
		{
			result = (int) ((((pkCount * 0.375) + 1) * 60) * 4) - 150;
			if (result > 10800)
			{
				return 10800;
			}
		}
		
		if (pkCount < 99)
		{
			result = (int) ((((pkCount * 0.5) + 1) * 60) * 12);
		}
		else if (pkCount < 180)
		{
			result = (int) ((((pkCount * 0.125) + 37.75) * 60) * 12);
		}
		
		return result;
	}
	
	public static double calcGeneralTraitBonus(Creature attacker, Creature target, TraitType traitType, boolean ignoreResistance)
	{
		if (traitType == TraitType.NONE)
		{
			return 1.0;
		}
		
		if (target.getStat().isTraitInvul(traitType))
		{
			return 0;
		}
		
		switch (traitType.getType())
		{
			case 2:
			{
				if (!attacker.getStat().hasAttackTrait(traitType) || !target.getStat().hasDefenceTrait(traitType))
				{
					return 1.0;
				}
				break;
			}
			case 3:
			{
				if (ignoreResistance)
				{
					return 1.0;
				}
				break;
			}
			default:
			{
				return 1.0;
			}
		}
		
		final double result = (attacker.getStat().getAttackTrait(traitType) - target.getStat().getDefenceTrait(traitType)) + 1.0;
		return MathUtil.clamp(result, 0.05, 2.0);
	}
	
	public static double calcWeaponTraitBonus(Creature attacker, Creature target)
	{
		final TraitType type = attacker.getAttackType().getTraitType();
		final double result = target.getStat().getDefenceTraits()[type.ordinal()] - 1.0;
		return 1.0 - result;
	}
	
	public static double calcAttackTraitBonus(Creature attacker, Creature target)
	{
		final double weaponTraitBonus = calcWeaponTraitBonus(attacker, target);
		if (weaponTraitBonus == 0)
		{
			return 0;
		}
		
		double weaknessBonus = 1.0;
		for (TraitType traitType : TraitType.values())
		{
			if (traitType.getType() == 2)
			{
				weaknessBonus *= calcGeneralTraitBonus(attacker, target, traitType, true);
				if (weaknessBonus == 0)
				{
					return 0;
				}
			}
		}
		
		return MathUtil.clamp((weaponTraitBonus * weaknessBonus), 0.05, 2.0);
	}
}
