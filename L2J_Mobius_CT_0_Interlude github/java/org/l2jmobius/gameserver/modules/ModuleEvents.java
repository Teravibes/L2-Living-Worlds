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

import java.util.function.Consumer;

import org.l2jmobius.gameserver.model.events.Containers;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.ListenersContainer;
import org.l2jmobius.gameserver.model.events.holders.IBaseEvent;
import org.l2jmobius.gameserver.model.events.listeners.ConsumerEventListener;

/**
 * The event-listener surface handed to a module through {@link ModuleContext#events()}. It lets a module react to a
 * game moment without touching the class that fires it, which is the first tool a module should reach for before asking
 * for a new platform hook.
 * <p>
 * A module registers a callback on one of the four global scopes. The scope is which population the moment is watched
 * for: {@link #onGlobal} hears every occurrence, {@link #onPlayers} only those on players, {@link #onNpcs} on any npc,
 * and {@link #onMonsters} on monsters. The {@link EventType} and the event object handed to the callback must match, the
 * same contract the core event system already uses; an event object is one of the {@code On*} holders.
 * <p>
 * Listeners are owned by the module (recorded in its {@link ModuleHandles}), so the platform can report what a module
 * contributed and a later hot-unload path can remove them. Id-specific moments, such as talking to one npc, stay the
 * domain of a quest or a handler; this surface is for population-wide reactions.
 */
public class ModuleEvents
{
	private final ModuleHandles _handles;

	ModuleEvents(ModuleHandles handles)
	{
		_handles = handles;
	}

	/**
	 * Registers a callback for an event on the global scope (every occurrence).
	 * @param <T> the event holder type
	 * @param type the event to listen for
	 * @param callback what to run when it fires
	 */
	public <T extends IBaseEvent> void onGlobal(EventType type, Consumer<T> callback)
	{
		register(Containers.Global(), type, callback, "global");
	}

	/**
	 * Registers a callback for an event on the players scope.
	 * @param <T> the event holder type
	 * @param type the event to listen for
	 * @param callback what to run when it fires
	 */
	public <T extends IBaseEvent> void onPlayers(EventType type, Consumer<T> callback)
	{
		register(Containers.Players(), type, callback, "players");
	}

	/**
	 * Registers a callback for an event on the npcs scope.
	 * @param <T> the event holder type
	 * @param type the event to listen for
	 * @param callback what to run when it fires
	 */
	public <T extends IBaseEvent> void onNpcs(EventType type, Consumer<T> callback)
	{
		register(Containers.Npcs(), type, callback, "npcs");
	}

	/**
	 * Registers a callback for an event on the monsters scope.
	 * @param <T> the event holder type
	 * @param type the event to listen for
	 * @param callback what to run when it fires
	 */
	public <T extends IBaseEvent> void onMonsters(EventType type, Consumer<T> callback)
	{
		register(Containers.Monsters(), type, callback, "monsters");
	}

	private void register(ListenersContainer container, EventType type, Consumer<? extends IBaseEvent> callback, String scope)
	{
		container.addListener(new ConsumerEventListener(container, type, callback, _handles));
		_handles.record("event " + type + " on " + scope);
	}
}
