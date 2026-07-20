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
package org.l2jmobius.gameserver.model.actor;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.data.xml.MapRegionData;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.model.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.model.actor.holders.creature.VehiclePathPoint;
import org.l2jmobius.gameserver.model.actor.stat.VehicleStat;
import org.l2jmobius.gameserver.model.actor.templates.CreatureTemplate;
import org.l2jmobius.gameserver.model.interfaces.ILocational;
import org.l2jmobius.gameserver.model.item.Weapon;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.zone.ZoneRegion;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager;
import org.l2jmobius.gameserver.taskmanagers.MovementTaskManager;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * @author DS
 */
public abstract class Vehicle extends Creature
{
	protected int _dockId = 0;
	protected final Collection<Player> _passengers = ConcurrentHashMap.newKeySet();
	protected Location _oustLoc = null;
	private Runnable _engine = null;
	
	protected VehiclePathPoint[] _currentPath = null;
	protected int _runState = 0;
	private ScheduledFuture<?> _monitorTask = null;
	private final Location _monitorLocation = new Location(this);
	
	/**
	 * Creates an abstract vehicle.
	 * @param template the vehicle template
	 */
	public Vehicle(CreatureTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.Vehicle);
		setFlying(true);
	}
	
	public boolean isBoat()
	{
		return false;
	}
	
	public boolean isAirShip()
	{
		return false;
	}
	
	public boolean canBeControlled()
	{
		return _engine == null;
	}
	
	public void registerEngine(Runnable r)
	{
		_engine = r;
	}
	
	public void runEngine(int delay)
	{
		if (_engine != null)
		{
			ThreadPool.schedule(_engine, delay);
		}
	}
	
	public void executePath(VehiclePathPoint[] path)
	{
		_runState = 0;
		_currentPath = path;
		if ((_currentPath != null) && (_currentPath.length > 0))
		{
			final VehiclePathPoint point = _currentPath[0];
			if (point.getMoveSpeed() > 0)
			{
				getStat().setMoveSpeed(point.getMoveSpeed());
			}
			
			if (point.getRotationSpeed() > 0)
			{
				getStat().setRotationSpeed(point.getRotationSpeed());
			}
			
			getAI().setIntention(Intention.MOVE_TO, new Location(point.getX(), point.getY(), point.getZ(), 0));
			return;
		}
		
		getAI().setIntention(Intention.ACTIVE);
	}
	
	@Override
	public boolean moveToNextRoutePoint()
	{
		_move = null;
		if (_currentPath != null)
		{
			_runState++;
			if (_runState < _currentPath.length)
			{
				final VehiclePathPoint point = _currentPath[_runState];
				if (!isMovementDisabled())
				{
					if (point.getMoveSpeed() == 0)
					{
						point.setHeading(point.getRotationSpeed());
						teleToLocation(point, false);
						if (_monitorTask != null)
						{
							_monitorTask.cancel(true);
							_monitorTask = null;
						}
						
						_currentPath = null;
					}
					else
					{
						if (point.getMoveSpeed() > 0)
						{
							getStat().setMoveSpeed(point.getMoveSpeed());
						}
						
						if (point.getRotationSpeed() > 0)
						{
							getStat().setRotationSpeed(point.getRotationSpeed());
						}
						
						final MoveData m = new MoveData();
						m.disregardingGeodata = false;
						m.onGeodataPathIndex = -1;
						m.xDestination = point.getX();
						m.yDestination = point.getY();
						m.zDestination = point.getZ();
						m.heading = 0;
						
						final double distance = Math.hypot(point.getX() - getX(), point.getY() - getY());
						if (distance > 1)
						{
							setHeading(LocationUtil.calculateHeadingFrom(getX(), getY(), point.getX(), point.getY()));
						}
						
						m.moveStartTime = GameTimeTaskManager.getInstance().getGameTicks();
						_move = m;
						MovementTaskManager.getInstance().registerMovingObject(this);
						
						// Make sure vehicle is not stuck.
						if (_monitorTask == null)
						{
							_monitorTask = ThreadPool.scheduleAtFixedRate(() ->
							{
								if (!isInDock() && (calculateDistance3D(_monitorLocation) == 0))
								{
									if (_currentPath != null)
									{
										if (_runState < _currentPath.length)
										{
											_runState = Math.max(0, _runState - 1);
											moveToNextRoutePoint();
										}
										else
										{
											broadcastInfo();
										}
									}
								}
								else
								{
									_monitorLocation.setXYZ(this);
								}
							}, 1000, 1000);
						}
						
						return true;
					}
				}
			}
			else
			{
				if (_monitorTask != null)
				{
					_monitorTask.cancel(true);
					_monitorTask = null;
				}
				
				_currentPath = null;
			}
		}
		
		runEngine(10);
		return false;
	}
	
	@Override
	public VehicleStat getStat()
	{
		return (VehicleStat) super.getStat();
	}
	
	@Override
	public void initCharStat()
	{
		setStat(new VehicleStat(this));
	}
	
	public boolean isInDock()
	{
		return _dockId > 0;
	}
	
	public int getDockId()
	{
		return _dockId;
	}
	
	public void setInDock(int d)
	{
		_dockId = d;
	}
	
	public void setOustLoc(Location loc)
	{
		_oustLoc = loc;
	}
	
	public Location getOustLoc()
	{
		return _oustLoc != null ? _oustLoc : MapRegionData.getInstance().getTeleToLocation(this, TeleportWhereType.TOWN);
	}
	
	public void oustPlayers()
	{
		_passengers.forEach(this::oustPlayer);
		_passengers.clear();
	}
	
	public void oustPlayer(Player player)
	{
		player.setVehicle(null);
		player.setInVehiclePosition(null);
		removePassenger(player);
	}
	
	public boolean addPassenger(Player player)
	{
		if ((player == null) || _passengers.contains(player) || ((player.getVehicle() != null) && (player.getVehicle() != this)))
		{
			return false;
		}
		
		_passengers.add(player);
		return true;
	}
	
	public void removePassenger(Player player)
	{
		try
		{
			_passengers.remove(player);
		}
		catch (Exception e)
		{
			// Ignore.
		}
	}
	
	public boolean isEmpty()
	{
		return _passengers.isEmpty();
	}
	
	public Collection<Player> getPassengers()
	{
		return _passengers;
	}
	
	public void broadcastToPassengers(ServerPacket packet)
	{
		for (Player player : _passengers)
		{
			if (player != null)
			{
				player.sendPacket(packet);
			}
		}
	}
	
	/**
	 * Consume ticket(s) and teleport player from boat if no correct ticket
	 * @param itemId Ticket itemId
	 * @param count Ticket count
	 * @param oustX
	 * @param oustY
	 * @param oustZ
	 */
	public void payForRide(int itemId, int count, int oustX, int oustY, int oustZ)
	{
		World.getInstance().forEachVisibleObjectInRange(this, Player.class, 1000, player ->
		{
			if (player.isInBoat() && (player.getBoat() == this))
			{
				if (itemId > 0)
				{
					final Item ticket = player.getInventory().getItemByItemId(itemId);
					if ((ticket == null) || (player.getInventory().destroyItem(ItemProcessType.FEE, ticket, count, player, this) == null))
					{
						player.sendPacket(SystemMessageId.YOU_DO_NOT_POSSESS_THE_CORRECT_TICKET_TO_BOARD_THE_BOAT);
						player.teleToLocation(new Location(oustX, oustY, oustZ), true);
						return;
					}
					
					final InventoryUpdate iu = new InventoryUpdate();
					iu.addModifiedItem(ticket);
					player.sendInventoryUpdate(iu);
				}
				
				addPassenger(player);
			}
		});
	}
	
	@Override
	public boolean updatePosition()
	{
		final boolean result = super.updatePosition();
		for (Player player : _passengers)
		{
			if ((player != null) && (player.getVehicle() == this))
			{
				player.setXYZ(getX(), getY(), getZ());
				player.revalidateZone(false);
			}
		}
		
		return result;
	}
	
	@Override
	public void teleToLocation(ILocational loc, boolean allowRandomOffset)
	{
		if (isMoving())
		{
			stopMove(null);
		}
		
		setTeleporting(true);
		
		getAI().setIntention(Intention.ACTIVE);
		
		for (Player player : _passengers)
		{
			if (player != null)
			{
				player.teleToLocation(loc, false);
			}
		}
		
		decayMe();
		setXYZ(loc);
		
		// temporary fix for heading on teleports
		if (loc.getHeading() != 0)
		{
			setHeading(loc.getHeading());
		}
		
		onTeleported();
		revalidateZone(true);
	}
	
	@Override
	public void stopMove(Location loc)
	{
		_move = null;
		if (loc != null)
		{
			setXYZ(loc);
			setHeading(loc.getHeading());
			revalidateZone(true);
		}
	}
	
	@Override
	public boolean deleteMe()
	{
		_engine = null;
		
		try
		{
			if (isMoving())
			{
				stopMove(null);
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Failed stopMove().", e);
		}
		
		try
		{
			oustPlayers();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Failed oustPlayers().", e);
		}
		
		final ZoneRegion oldZoneRegion = ZoneManager.getInstance().getRegion(this);
		
		try
		{
			decayMe();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Failed decayMe().", e);
		}
		
		oldZoneRegion.removeFromZones(this);
		
		return super.deleteMe();
	}
	
	@Override
	public void updateAbnormalEffect()
	{
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
	public int getLevel()
	{
		return 0;
	}
	
	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		return false;
	}
	
	@Override
	public void detachAI()
	{
	}
	
	@Override
	public boolean isVehicle()
	{
		return true;
	}
}
