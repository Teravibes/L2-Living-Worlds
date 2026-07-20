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

import org.l2jmobius.gameserver.ai.CreatureAI;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.model.actor.stat.StaticObjectStat;
import org.l2jmobius.gameserver.model.actor.status.StaticObjectStatus;
import org.l2jmobius.gameserver.model.actor.templates.CreatureTemplate;
import org.l2jmobius.gameserver.model.item.Weapon;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.ShowTownMap;
import org.l2jmobius.gameserver.network.serverpackets.StaticObjectInfo;

/**
 * Static Object instance.
 * @author godson
 */
public class StaticObject extends Creature
{
	/** The interaction distance of the StaticObject */
	public static final int INTERACTION_DISTANCE = 150;
	
	private final int _staticObjectId;
	private int _meshIndex = 0; // 0 - static objects, alternate static objects
	private int _type = -1; // 0 - map signs, 1 - throne , 2 - arena signs
	private ShowTownMap _map;
	
	/**
	 * Creates a static object.
	 * @param template the static object
	 * @param staticId the static ID
	 */
	public StaticObject(CreatureTemplate template, int staticId)
	{
		super(template);
		setInstanceType(InstanceType.StaticObject);
		_staticObjectId = staticId;
	}
	
	@Override
	protected CreatureAI initAI()
	{
		return null;
	}
	
	/**
	 * Gets the static object ID.
	 * @return the static object ID
	 */
	@Override
	public int getId()
	{
		return _staticObjectId;
	}
	
	@Override
	public StaticObjectStat getStat()
	{
		return (StaticObjectStat) super.getStat();
	}
	
	@Override
	public void initCharStat()
	{
		setStat(new StaticObjectStat(this));
	}
	
	@Override
	public StaticObjectStatus getStatus()
	{
		return (StaticObjectStatus) super.getStatus();
	}
	
	@Override
	public void initCharStatus()
	{
		setStatus(new StaticObjectStatus(this));
	}
	
	public int getType()
	{
		return _type;
	}
	
	public void setType(int type)
	{
		_type = type;
	}
	
	public void setMap(String texture, int x, int y)
	{
		_map = new ShowTownMap("town_map." + texture, x, y);
	}
	
	public ShowTownMap getMap()
	{
		return _map;
	}
	
	@Override
	public int getLevel()
	{
		return 1;
	}
	
	@Override
	public Item getActiveWeaponInstance()
	{
		return null;
	}
	
	@Override
	public Weapon getActiveWeaponItem()
	{
		return null;
	}
	
	@Override
	public Item getSecondaryWeaponInstance()
	{
		return null;
	}
	
	@Override
	public Weapon getSecondaryWeaponItem()
	{
		return null;
	}
	
	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		return false;
	}
	
	/**
	 * Set the meshIndex of the object.<br>
	 * <br>
	 * <b><u>Values</u>:</b>
	 * <ul>
	 * <li>default textures : 0</li>
	 * <li>alternate textures : 1</li>
	 * </ul>
	 * @param meshIndex
	 */
	public void setMeshIndex(int meshIndex)
	{
		_meshIndex = meshIndex;
		broadcastPacket(new StaticObjectInfo(this));
	}
	
	/**
	 * <b><u>Values</u>:</b>
	 * <ul>
	 * <li>default textures : 0</li>
	 * <li>alternate textures : 1</li>
	 * </ul>
	 * @return the meshIndex of the object
	 */
	public int getMeshIndex()
	{
		return _meshIndex;
	}
	
	@Override
	public void updateAbnormalEffect()
	{
	}
	
	@Override
	public void sendInfo(Player player)
	{
		player.sendPacket(new StaticObjectInfo(this));
	}
	
	@Override
	public void moveToLocation(int x, int y, int z, int offset)
	{
	}
	
	@Override
	public void stopMove(Location loc)
	{
	}
	
	@Override
	public void doAttack(Creature target)
	{
	}
	
	@Override
	public void doCast(Skill skill)
	{
	}
}
