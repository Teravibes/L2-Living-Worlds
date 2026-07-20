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
package org.l2jmobius.gameserver.managers;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.data.holders.NpcRoutesHolder;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.holders.npc.WalkInfo;
import org.l2jmobius.gameserver.model.actor.holders.npc.WalkNode;
import org.l2jmobius.gameserver.model.actor.holders.npc.WalkRoute;
import org.l2jmobius.gameserver.model.actor.instance.Monster;
import org.l2jmobius.gameserver.model.actor.tasks.npc.walker.ArrivedTask;
import org.l2jmobius.gameserver.model.events.EventDispatcher;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.actor.npc.OnNpcMoveNodeArrived;
import org.l2jmobius.gameserver.network.enums.ChatType;

/**
 * This class manages walking monsters.
 * @author GKR
 */
public class WalkingManager implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(WalkingManager.class.getName());
	
	// Repeat style:
	// -1 - no repeat
	// 0 - go back
	// 1 - go to first point (circle style)
	// 2 - teleport to first point (conveyor style)
	// 3 - random walking between points.
	public static final byte NO_REPEAT = -1;
	public static final byte REPEAT_GO_BACK = 0;
	public static final byte REPEAT_GO_FIRST = 1;
	public static final byte REPEAT_TELE_FIRST = 2;
	public static final byte REPEAT_RANDOM = 3;
	
	private final Set<Integer> _targetedNpcIds = new HashSet<>();
	private final Map<String, WalkRoute> _routes = new HashMap<>(); // all available routes
	private final Map<Integer, WalkInfo> _activeRoutes = new HashMap<>(); // each record represents NPC, moving by predefined route from _routes, and moving progress
	private final Map<Integer, NpcRoutesHolder> _routesToAttach = new HashMap<>(); // each record represents NPC and all available routes for it
	private final Map<Npc, ScheduledFuture<?>> _startMoveTasks = new ConcurrentHashMap<>();
	private final Map<Npc, ScheduledFuture<?>> _repeatMoveTasks = new ConcurrentHashMap<>();
	private final Map<Npc, ScheduledFuture<?>> _arriveTasks = new ConcurrentHashMap<>();
	
	protected WalkingManager()
	{
		load();
	}
	
	@Override
	public void load()
	{
		parseDatapackFile("data/Routes.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _routes.size() + " walking routes.");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		for (Node d = document.getFirstChild().getFirstChild(); d != null; d = d.getNextSibling())
		{
			if (d.getNodeName().equals("route"))
			{
				final String routeName = parseString(d.getAttributes(), "name");
				final boolean repeat = parseBoolean(d.getAttributes(), "repeat");
				final String repeatStyle = d.getAttributes().getNamedItem("repeatStyle").getNodeValue().toLowerCase();
				final byte repeatType;
				switch (repeatStyle)
				{
					case "back":
					{
						repeatType = REPEAT_GO_BACK;
						break;
					}
					case "cycle":
					{
						repeatType = REPEAT_GO_FIRST;
						break;
					}
					case "conveyor":
					{
						repeatType = REPEAT_TELE_FIRST;
						break;
					}
					case "random":
					{
						repeatType = REPEAT_RANDOM;
						break;
					}
					default:
					{
						repeatType = NO_REPEAT;
						break;
					}
				}
				
				final List<WalkNode> list = new ArrayList<>();
				for (Node r = d.getFirstChild(); r != null; r = r.getNextSibling())
				{
					if (r.getNodeName().equals("point"))
					{
						final NamedNodeMap attrs = r.getAttributes();
						final int x = parseInteger(attrs, "X");
						final int y = parseInteger(attrs, "Y");
						final int z = parseInteger(attrs, "Z");
						final int delay = parseInteger(attrs, "delay");
						final boolean run = parseBoolean(attrs, "run");
						String chatString = null;
						final Node node = attrs.getNamedItem("string");
						if (node != null)
						{
							chatString = node.getNodeValue();
						}
						
						list.add(new WalkNode(x, y, z, delay, run, chatString));
					}
					
					else if (r.getNodeName().equals("target"))
					{
						final NamedNodeMap attrs = r.getAttributes();
						try
						{
							final int npcId = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
							final int x = Integer.parseInt(attrs.getNamedItem("spawnX").getNodeValue());
							final int y = Integer.parseInt(attrs.getNamedItem("spawnY").getNodeValue());
							final int z = Integer.parseInt(attrs.getNamedItem("spawnZ").getNodeValue());
							if (NpcData.getInstance().getTemplate(npcId) != null)
							{
								final NpcRoutesHolder holder = _routesToAttach.containsKey(npcId) ? _routesToAttach.get(npcId) : new NpcRoutesHolder();
								holder.addRoute(routeName, new Location(x, y, z));
								_routesToAttach.put(npcId, holder);
								
								if (!_targetedNpcIds.contains(npcId))
								{
									_targetedNpcIds.add(npcId);
								}
							}
							// else
							// {
							// LOGGER.warning(getClass().getSimpleName() + ": NPC with id " + npcId + " for route '" + routeName + "' does not exist.");
							// }
						}
						catch (Exception e)
						{
							LOGGER.warning(getClass().getSimpleName() + ": Error in target definition for route '" + routeName + "'");
						}
					}
				}
				
				_routes.put(routeName, new WalkRoute(routeName, list, repeat, repeatType));
			}
		}
	}
	
	/**
	 * Checks if the given NPC or its leader is controlled by the Walking Manager and is currently moving.
	 * @param npc the {@link Npc} to check
	 * @return {@code true} if the NPC or its leader is on a registered route and actively moving, {@code false} otherwise
	 */
	public boolean isOnWalk(Npc npc)
	{
		final Monster monster = npc.isMonster() ? npc.asMonster().getLeader() == null ? npc.asMonster() : npc.asMonster().getLeader() : null;
		if (((monster != null) && !isRegistered(monster)) || !isRegistered(npc))
		{
			return false;
		}
		
		final WalkInfo walk = monster != null ? _activeRoutes.get(monster.getObjectId()) : _activeRoutes.get(npc.getObjectId());
		return !walk.isStoppedByAttack() && !walk.isSuspended();
	}
	
	/**
	 * Retrieves a walk route by its name.
	 * @param route the name of the route
	 * @return the {@link WalkRoute} associated with the specified name, or {@code null} if not found
	 */
	public WalkRoute getRoute(String route)
	{
		return _routes.get(route);
	}
	
	/**
	 * Checks if the specified NPC is a target in any registered route.
	 * @param npc the {@link Npc} to check
	 * @return {@code true} if the NPC is targeted in a route, {@code false} otherwise
	 */
	public boolean isTargeted(Npc npc)
	{
		return _targetedNpcIds.contains(npc.getId());
	}
	
	/**
	 * Checks if the specified NPC is registered with the Walking Manager.
	 * @param npc the {@link Npc} to check
	 * @return {@code true} if the NPC is registered, {@code false} otherwise
	 */
	private boolean isRegistered(Npc npc)
	{
		return _activeRoutes.containsKey(npc.getObjectId());
	}
	
	/**
	 * Retrieves the name of the route assigned to the specified NPC.
	 * @param npc the {@link Npc} whose route name is requested
	 * @return the name of the route, or an empty string if the NPC is not on a route
	 */
	public String getRouteName(Npc npc)
	{
		return _activeRoutes.containsKey(npc.getObjectId()) ? _activeRoutes.get(npc.getObjectId()).getRoute().getName() : "";
	}
	
	/**
	 * Starts moving the specified NPC along a given route.
	 * @param npc the {@link Npc} to move
	 * @param routeName the name of the route to follow
	 */
	public void startMoving(Npc npc, String routeName)
	{
		if (_routes.containsKey(routeName) && (npc != null) && !npc.isDead()) // check, if these route and NPC present
		{
			if (!_activeRoutes.containsKey(npc.getObjectId())) // new walk task
			{
				// only if not already moved / not engaged in battle... should not happens if called on spawn
				if ((npc.getAI().getIntention() == Intention.ACTIVE) || (npc.getAI().getIntention() == Intention.IDLE))
				{
					final WalkInfo walk = new WalkInfo(routeName);
					WalkNode node = walk.getCurrentNode();
					
					// adjust next waypoint, if NPC spawns at first waypoint
					if ((npc.getX() == node.getX()) && (npc.getY() == node.getY()))
					{
						walk.calculateNextNode(npc);
						node = walk.getCurrentNode();
					}
					
					if (!npc.isInsideRadius3D(node, 3000))
					{
						LOGGER.warning(getClass().getSimpleName() + ": " + "Route '" + routeName + "': NPC (id=" + npc.getId() + ", x=" + npc.getX() + ", y=" + npc.getY() + ", z=" + npc.getZ() + ") is too far from starting point (node x=" + node.getX() + ", y=" + node.getY() + ", z=" + node.getZ() + ", range=" + npc.calculateDistance3D(node) + "). Teleporting to proper location.");
						npc.teleToLocation(node);
					}
					
					if (node.runToLocation())
					{
						npc.setRunning();
					}
					else
					{
						npc.setWalking();
					}
					
					npc.getAI().setIntention(Intention.MOVE_TO, node);
					
					final ScheduledFuture<?> task = _repeatMoveTasks.get(npc);
					if ((task == null) || task.isCancelled() || task.isDone())
					{
						final ScheduledFuture<?> newTask = ThreadPool.scheduleAtFixedRate(() -> startMoving(npc, routeName), 10000, 10000);
						_repeatMoveTasks.put(npc, newTask);
						walk.setWalkCheckTask(newTask); // start walk check task, for resuming walk after fight
					}
					
					npc.setWalker();
					_activeRoutes.put(npc.getObjectId(), walk); // register route
				}
				else
				{
					final ScheduledFuture<?> task = _startMoveTasks.get(npc);
					if ((task == null) || task.isCancelled() || task.isDone())
					{
						_startMoveTasks.put(npc, ThreadPool.schedule(() -> startMoving(npc, routeName), 10000));
					}
				}
			}
			else // walk was stopped due to some reason (arrived to node, script action, fight or something else), resume it
			{
				if (_activeRoutes.containsKey(npc.getObjectId()) && ((npc.getAI().getIntention() == Intention.ACTIVE) || (npc.getAI().getIntention() == Intention.IDLE)))
				{
					final WalkInfo walk = _activeRoutes.get(npc.getObjectId());
					if (walk == null)
					{
						return;
					}
					
					// Prevent call simultaneously from scheduled task and onArrived() or temporarily stop walking for resuming in future
					if (walk.isBlocked() || walk.isSuspended())
					{
						return;
					}
					
					walk.setBlocked(true);
					final WalkNode node = walk.getCurrentNode();
					if (node.runToLocation())
					{
						npc.setRunning();
					}
					else
					{
						npc.setWalking();
					}
					
					npc.getAI().setIntention(Intention.MOVE_TO, node);
					walk.setBlocked(false);
					walk.setStoppedByAttack(false);
				}
			}
		}
	}
	
	/**
	 * Permanently cancels any active movement for the specified NPC.
	 * @param npc the {@link Npc} whose movement is to be canceled
	 */
	public synchronized void cancelMoving(Npc npc)
	{
		final WalkInfo walk = _activeRoutes.remove(npc.getObjectId());
		if (walk != null)
		{
			final ScheduledFuture<?> task = walk.getWalkCheckTask();
			if (task != null)
			{
				task.cancel(true);
			}
		}
	}
	
	/**
	 * Resumes movement for an NPC that was previously stopped.
	 * @param npc the {@link Npc} to resume moving
	 */
	public void resumeMoving(Npc npc)
	{
		final WalkInfo walk = _activeRoutes.get(npc.getObjectId());
		if (walk != null)
		{
			walk.setSuspended(false);
			walk.setStoppedByAttack(false);
			startMoving(npc, walk.getRoute().getName());
		}
	}
	
	/**
	 * Temporarily stops the movement of the specified NPC until resumed.
	 * @param npc the {@link Npc} to pause
	 * @param suspend {@code true} if the stop is temporary and movement will be resumed later
	 * @param stoppedByAttack {@code true} if the stop was due to the NPC being attacked or attacking
	 */
	public void stopMoving(Npc npc, boolean suspend, boolean stoppedByAttack)
	{
		final Monster monster = npc.isMonster() ? npc.asMonster().getLeader() == null ? npc.asMonster() : npc.asMonster().getLeader() : null;
		if (((monster != null) && !isRegistered(monster)) || !isRegistered(npc))
		{
			return;
		}
		
		final WalkInfo walk = monster != null ? _activeRoutes.get(monster.getObjectId()) : _activeRoutes.get(npc.getObjectId());
		walk.setSuspended(suspend);
		walk.setStoppedByAttack(stoppedByAttack);
		
		if (monster != null)
		{
			monster.stopMove(null);
			monster.getAI().setIntention(Intention.ACTIVE);
		}
		else
		{
			npc.stopMove(null);
			npc.getAI().setIntention(Intention.ACTIVE);
		}
	}
	
	/**
	 * Manages tasks related to an NPC arriving at a designated node in its route.
	 * <p>
	 * This method schedules the next move, handles events, and optionally triggers scripted actions upon arrival.
	 * </p>
	 * @param npc the {@link Npc} that has arrived at a node
	 */
	public void onArrived(Npc npc)
	{
		if (!_activeRoutes.containsKey(npc.getObjectId()))
		{
			return;
		}
		
		// Notify quest
		if (EventDispatcher.getInstance().hasListener(EventType.ON_NPC_MOVE_NODE_ARRIVED, npc))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnNpcMoveNodeArrived(npc), npc);
		}
		
		final WalkInfo walk = _activeRoutes.get(npc.getObjectId());
		
		// Opposite should not happen... but happens sometime
		if ((walk.getCurrentNodeId() < 0) || (walk.getCurrentNodeId() >= walk.getRoute().getNodesCount()))
		{
			return;
		}
		
		final List<WalkNode> nodelist = walk.getRoute().getNodeList();
		final WalkNode node = nodelist.get(Math.min(walk.getCurrentNodeId(), nodelist.size() - 1));
		if (!npc.isInsideRadius2D(node, 10))
		{
			return;
		}
		
		walk.calculateNextNode(npc);
		walk.setBlocked(true); // prevents to be ran from walk check task, if there is delay in this node.
		if (!node.getChatText().isEmpty())
		{
			npc.broadcastSay(ChatType.NPC_GENERAL, node.getChatText());
		}
		
		final ScheduledFuture<?> task = _arriveTasks.get(npc);
		if ((task == null) || task.isCancelled() || task.isDone())
		{
			_arriveTasks.put(npc, ThreadPool.schedule(new ArrivedTask(npc, walk), 100 + (node.getDelay() * 1000)));
		}
	}
	
	/**
	 * Handles tasks related to an NPC's death, including permanently canceling any movement.
	 * @param npc the {@link Npc} that has died
	 */
	public void onDeath(Npc npc)
	{
		cancelMoving(npc);
	}
	
	/**
	 * Handles tasks related to an NPC's spawn, such as starting its movement if a route is attached to its spawn point.
	 * @param npc the {@link Npc} that has spawned
	 */
	public void onSpawn(Npc npc)
	{
		if (_routesToAttach.containsKey(npc.getId()))
		{
			final String routeName = _routesToAttach.get(npc.getId()).getRouteName(npc);
			if (!routeName.isEmpty())
			{
				startMoving(npc, routeName);
			}
		}
	}
	
	public static WalkingManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final WalkingManager INSTANCE = new WalkingManager();
	}
}
