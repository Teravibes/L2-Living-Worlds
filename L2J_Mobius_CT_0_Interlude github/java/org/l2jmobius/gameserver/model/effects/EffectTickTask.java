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
package org.l2jmobius.gameserver.model.effects;

import org.l2jmobius.gameserver.model.skill.BuffInfo;

/**
 * Effect tick task.
 * @author Zoey76
 */
public class EffectTickTask implements Runnable
{
	private final BuffInfo _info;
	private final AbstractEffect _effect;
	
	/**
	 * EffectTickTask constructor.
	 * @param info the buff info
	 * @param effect the effect
	 */
	public EffectTickTask(BuffInfo info, AbstractEffect effect)
	{
		_info = info;
		_effect = effect;
	}
	
	/**
	 * Gets the buff info.
	 * @return the buff info
	 */
	public BuffInfo getBuffInfo()
	{
		return _info;
	}
	
	/**
	 * Gets the effect.
	 * @return the effect
	 */
	public AbstractEffect getEffect()
	{
		return _effect;
	}
	
	@Override
	public void run()
	{
		_info.onTick(_effect);
	}
}
