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
package org.l2jmobius.gameserver.model.stats.functions;

import org.l2jmobius.gameserver.config.OlympiadConfig;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.conditions.Condition;
import org.l2jmobius.gameserver.model.item.enums.BodyPart;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.item.type.WeaponType;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.stats.Stat;

public class FuncEnchant extends AbstractFunction
{
	public FuncEnchant(Stat stat, int order, Object owner, double value, Condition applyCond)
	{
		super(stat, order, owner, value, applyCond);
	}
	
	@Override
	public double calc(Creature effector, Creature effected, Skill skill, double initVal)
	{
		double value = initVal;
		if ((getApplyCond() != null) && !getApplyCond().test(effector, effected, skill))
		{
			return value;
		}
		
		final Item item = (Item) getFuncOwner();
		int enchant = item.getEnchantLevel();
		if (enchant <= 0)
		{
			return value;
		}
		
		int overenchant = 0;
		if (enchant > 3)
		{
			overenchant = enchant - 3;
			enchant = 3;
		}
		
		if (effector.isPlayer() && effector.asPlayer().isInOlympiadMode() && (OlympiadConfig.OLYMPIAD_ENCHANT_LIMIT >= 0) && ((enchant + overenchant) > OlympiadConfig.OLYMPIAD_ENCHANT_LIMIT))
		{
			if (OlympiadConfig.OLYMPIAD_ENCHANT_LIMIT > 3)
			{
				overenchant = OlympiadConfig.OLYMPIAD_ENCHANT_LIMIT - 3;
			}
			else
			{
				overenchant = 0;
				enchant = OlympiadConfig.OLYMPIAD_ENCHANT_LIMIT;
			}
		}
		
		if ((getStat() == Stat.MAGIC_DEFENCE) || (getStat() == Stat.POWER_DEFENCE))
		{
			return value + enchant + (3 * overenchant);
		}
		
		if (getStat() == Stat.MAGIC_ATTACK)
		{
			switch (item.getTemplate().getCrystalTypePlus())
			{
				case S:
				{
					// M. Atk. increases by 4 for all weapons.
					// Starting at +4, M. Atk. bonus double.
					value += (4 * enchant) + (8 * overenchant);
					break;
				}
				case A:
				case B:
				case C:
				{
					// M. Atk. increases by 3 for all weapons.
					// Starting at +4, M. Atk. bonus double.
					value += (3 * enchant) + (6 * overenchant);
					break;
				}
				case D:
				case NONE:
				{
					// M. Atk. increases by 2 for all weapons. Starting at +4, M. Atk. bonus double.
					// Starting at +4, M. Atk. bonus double.
					value += (2 * enchant) + (4 * overenchant);
					break;
				}
			}
			
			return value;
		}
		
		if (item.isWeapon())
		{
			final WeaponType type = (WeaponType) item.getItemType();
			switch (item.getTemplate().getCrystalTypePlus())
			{
				case S:
				{
					if (item.getWeaponItem().getBodyPart() == BodyPart.LR_HAND)
					{
						if (type == WeaponType.BOW)
						{
							// P. Atk. increases by 10 for bows.
							// Starting at +4, P. Atk. bonus double.
							value += (10 * enchant) + (20 * overenchant);
						}
						else
						{
							// P. Atk. increases by 6 for two-handed swords, two-handed blunts, dualswords, and two-handed combat weapons.
							// Starting at +4, P. Atk. bonus double.
							value += (6 * enchant) + (12 * overenchant);
						}
					}
					else
					{
						// P. Atk. increases by 5 for one-handed swords, one-handed blunts, daggers, spears, and other weapons.
						// Starting at +4, P. Atk. bonus double.
						value += (5 * enchant) + (10 * overenchant);
					}
					break;
				}
				case A:
				{
					if (item.getWeaponItem().getBodyPart() == BodyPart.LR_HAND)
					{
						if (type == WeaponType.BOW)
						{
							// P. Atk. increases by 8 for bows.
							// Starting at +4, P. Atk. bonus double.
							value += (8 * enchant) + (16 * overenchant);
						}
						else
						{
							// P. Atk. increases by 5 for two-handed swords, two-handed blunts, dualswords, and two-handed combat weapons.
							// Starting at +4, P. Atk. bonus double.
							value += (5 * enchant) + (10 * overenchant);
						}
					}
					else
					{
						// P. Atk. increases by 4 for one-handed swords, one-handed blunts, daggers, spears, and other weapons.
						// Starting at +4, P. Atk. bonus double.
						value += (4 * enchant) + (8 * overenchant);
					}
					break;
				}
				case B:
				case C:
				{
					if (item.getWeaponItem().getBodyPart() == BodyPart.LR_HAND)
					{
						if (type == WeaponType.BOW)
						{
							// P. Atk. increases by 6 for bows.
							// Starting at +4, P. Atk. bonus double.
							value += (6 * enchant) + (12 * overenchant);
						}
						else
						{
							// P. Atk. increases by 4 for two-handed swords, two-handed blunts, dualswords, and two-handed combat weapons.
							// Starting at +4, P. Atk. bonus double.
							value += (4 * enchant) + (8 * overenchant);
						}
					}
					else
					{
						// P. Atk. increases by 3 for one-handed swords, one-handed blunts, daggers, spears, and other weapons.
						// Starting at +4, P. Atk. bonus double.
						value += (3 * enchant) + (6 * overenchant);
					}
					break;
				}
				case D:
				case NONE:
				{
					switch (type)
					{
						case BOW:
						{
							// Bows increase by 4.
							// Starting at +4, P. Atk. bonus double.
							value += (4 * enchant) + (8 * overenchant);
							break;
						}
						default:
						{
							// P. Atk. increases by 2 for all weapons with the exception of bows.
							// Starting at +4, P. Atk. bonus double.
							value += (2 * enchant) + (4 * overenchant);
							break;
						}
					}
					break;
				}
			}
		}
		
		return value;
	}
}
