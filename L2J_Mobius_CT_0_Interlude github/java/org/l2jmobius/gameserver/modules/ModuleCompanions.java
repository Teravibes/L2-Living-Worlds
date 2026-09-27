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
package org.l2jmobius.gameserver.modules;

import java.util.logging.Logger;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.managers.PhantomManager;
import org.l2jmobius.gameserver.managers.TownFakeInviteRules;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.groups.Party;
import org.l2jmobius.gameserver.taskmanagers.PlayerAutoSaveTaskManager;

/**
 * The party companion extension point, handed to a module through {@link ModuleContext#companions()}. A companion is a
 * saved character that is not logged in, brought into a player's party as a clientless member run by the party AI: it
 * follows, assists, and plays its class the way recruited party members do, and it gains experience like any party
 * member. It keeps its own level, skills, gear and consumables, and it is saved when it leaves, so everything it earns
 * stays on the character.
 * <p>
 * The platform owns the safety rules every companion feature needs: a bot character, a character that is already in
 * the world, and a dead character are refused, and a companion's row is never deleted. Which characters a feature
 * offers, and how the player asks for one, is the module's policy.
 * <p>
 * It is inert until a module calls it.
 */
public class ModuleCompanions
{
	private static final Logger LOGGER = Logger.getLogger(ModuleCompanions.class.getName());

	/** How far from the owner a companion appears, in game units. */
	private static final int SPAWN_OFFSET = 60;

	/** The outcome of a {@link #summon} request. */
	public enum Result
	{
		/** The companion is in the owner's party. */
		JOINED,
		/** No saved character has that id. */
		NOT_FOUND,
		/** The character belongs to a bot account. */
		BOT,
		/** The character is already in the world (logged in, or already a companion). */
		ALREADY_ONLINE,
		/** The character was saved dead; it must be revived by logging in to it. */
		DEAD,
		/** The owner is in a party they do not lead, or the party is full. */
		PARTY_CLOSED,
		/** The character could not be loaded or could not join. */
		FAILED
	}

	ModuleCompanions()
	{
	}

	/**
	 * Loads a saved character and brings it into the owner's party next to them.
	 * @param owner the player whose party it joins
	 * @param charId the character's objectId
	 * @param onLeave run once after the companion has left the party and been saved (may be {@code null})
	 * @return the outcome
	 */
	public Result summon(Player owner, int charId, Runnable onLeave)
	{
		if ((owner == null) || (charId <= 0))
		{
			return Result.NOT_FOUND;
		}
		if (World.getInstance().getPlayer(charId) != null)
		{
			return Result.ALREADY_ONLINE;
		}
		final Party party = owner.getParty();
		if (!TownFakeInviteRules.mayAddMember(party != null, (party != null) && party.isLeader(owner), (party != null) ? party.getMemberCount() : 0))
		{
			return Result.PARTY_CLOSED;
		}

		Player companion = null;
		try
		{
			companion = Player.load(charId);
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Failed to load character " + charId + ": " + e.getMessage());
		}
		if (companion == null)
		{
			return Result.NOT_FOUND;
		}
		// Player.load marks the character online and schedules its auto save. Every refusal below undoes that without
		// saving, so the untouched row stays exactly as it was.
		if (PhantomManager.isBotAccount(companion.getAccountName()))
		{
			discard(companion);
			return Result.BOT;
		}
		if (companion.isDead())
		{
			discard(companion);
			return Result.DEAD;
		}
		// Re-checked after the load, in case its own account logged in while it was loading.
		if (World.getInstance().getPlayer(charId) != null)
		{
			discard(companion);
			return Result.ALREADY_ONLINE;
		}

		return PhantomManager.getInstance().addCompanion(owner, companion, spawnLocationNear(owner), onLeave) ? Result.JOINED : Result.FAILED;
	}

	/**
	 * @param player a player
	 * @return {@code true} if the player is a live companion
	 */
	public boolean isCompanion(Player player)
	{
		return PhantomManager.getInstance().isCompanion(player);
	}

	private static void discard(Player loaded)
	{
		loaded.setOnlineStatus(false, false);
		PlayerAutoSaveTaskManager.getInstance().remove(loaded);
	}

	private static Location spawnLocationNear(Player owner)
	{
		final int x = owner.getX() + Rnd.get(-SPAWN_OFFSET, SPAWN_OFFSET);
		final int y = owner.getY() + Rnd.get(-SPAWN_OFFSET, SPAWN_OFFSET);
		if (!GeoEngine.getInstance().canMoveToTarget(owner.getX(), owner.getY(), owner.getZ(), x, y, owner.getZ(), owner.getInstanceId()))
		{
			return new Location(owner.getX(), owner.getY(), owner.getZ());
		}
		return new Location(x, y, GeoEngine.getInstance().getHeight(x, y, owner.getZ()));
	}
}
