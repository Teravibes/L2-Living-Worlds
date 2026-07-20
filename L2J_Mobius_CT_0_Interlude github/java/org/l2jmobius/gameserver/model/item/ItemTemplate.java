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
package org.l2jmobius.gameserver.model.item;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.OlympiadConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.conditions.Condition;
import org.l2jmobius.gameserver.model.events.ListenersContainer;
import org.l2jmobius.gameserver.model.item.enums.BodyPart;
import org.l2jmobius.gameserver.model.item.enums.ItemGrade;
import org.l2jmobius.gameserver.model.item.holders.ExtractableProduct;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.item.type.ActionType;
import org.l2jmobius.gameserver.model.item.type.CrystalType;
import org.l2jmobius.gameserver.model.item.type.EtcItemType;
import org.l2jmobius.gameserver.model.item.type.ItemType;
import org.l2jmobius.gameserver.model.item.type.MaterialType;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.model.stats.functions.AbstractFunction;
import org.l2jmobius.gameserver.model.stats.functions.FuncTemplate;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * This class contains all informations concerning the item (weapon, armor, etc).<br>
 * Mother class of :
 * <ul>
 * <li>Armor</li>
 * <li>EtcItem</li>
 * <li>Weapon</li>
 * </ul>
 */
public abstract class ItemTemplate extends ListenersContainer
{
	protected static final Logger LOGGER = Logger.getLogger(ItemTemplate.class.getName());
	
	public static final int TYPE1_WEAPON_RING_EARRING_NECKLACE = 0;
	public static final int TYPE1_SHIELD_ARMOR = 1;
	public static final int TYPE1_ITEM_QUESTITEM_ADENA = 4;
	
	public static final int TYPE2_WEAPON = 0;
	public static final int TYPE2_SHIELD_ARMOR = 1;
	public static final int TYPE2_ACCESSORY = 2;
	public static final int TYPE2_QUEST = 3;
	public static final int TYPE2_MONEY = 4;
	public static final int TYPE2_OTHER = 5;
	
	private int _itemId;
	private int _displayId;
	private String _name;
	private String _icon;
	private int _weight;
	private boolean _stackable;
	private MaterialType _materialType;
	private CrystalType _crystalType;
	private int _equipReuseDelay;
	private int _duration;
	private long _time;
	private int _autoDestroyTime;
	private BodyPart _bodyPart;
	private int _referencePrice;
	private int _crystalCount;
	private boolean _sellable;
	private boolean _dropable;
	private boolean _destroyable;
	private boolean _tradeable;
	private boolean _depositable;
	private boolean _enchantable;
	private boolean _elementable;
	private boolean _questItem;
	private boolean _freightable;
	private boolean _allowSelfResurrection;
	private boolean _isOlyRestricted;
	private boolean _forNpc;
	private boolean _common;
	private boolean _heroItem;
	private boolean _pvpItem;
	private boolean _immediateEffect;
	private boolean _exImmediateEffect;
	private int _defaultEnchantLevel;
	private ActionType _defaultAction;
	
	protected int _type1; // needed for item list (inventory)
	protected int _type2; // different lists for armor, weapon, etc
	protected List<FuncTemplate> _funcTemplates;
	protected List<Condition> _preConditions;
	private SkillHolder[] _skillHolder;
	private SkillHolder _unequipSkill = null;
	
	private int _useSkillDisTime;
	protected int _reuseDelay;
	private int _sharedReuseGroup;
	
	/**
	 * Constructor of the Item that fill class variables.
	 * @param set : StatSet corresponding to a set of couples (key,value) for description of the item
	 */
	protected ItemTemplate(StatSet set)
	{
		set(set);
	}
	
	public void set(StatSet set)
	{
		_itemId = set.getInt("item_id");
		_displayId = set.getInt("displayId", _itemId);
		_name = set.getString("name");
		_icon = set.getString("icon", null);
		_weight = set.getInt("weight", 0);
		_materialType = set.getEnum("material", MaterialType.class, MaterialType.STEEL);
		_equipReuseDelay = set.getInt("equip_reuse_delay", 0) * 1000;
		_duration = set.getInt("duration", -1);
		_time = set.getInt("time", -1);
		_autoDestroyTime = set.getInt("auto_destroy_time", -1) * 1000;
		_bodyPart = BodyPart.fromName(set.getString("bodypart", "none"));
		_referencePrice = set.getInt("price", 0);
		_crystalType = set.getEnum("crystal_type", CrystalType.class, CrystalType.NONE);
		_crystalCount = set.getInt("crystal_count", 0);
		_stackable = set.getBoolean("is_stackable", false);
		_sellable = set.getBoolean("is_sellable", true);
		_dropable = set.getBoolean("is_dropable", true);
		_destroyable = set.getBoolean("is_destroyable", true);
		_tradeable = set.getBoolean("is_tradable", true);
		_questItem = set.getBoolean("is_questitem", false);
		_depositable = set.getBoolean("is_depositable", true);
		_elementable = set.getBoolean("element_enabled", false);
		_enchantable = set.getBoolean("enchant_enabled", false);
		_freightable = set.getBoolean("is_freightable", false);
		_allowSelfResurrection = set.getBoolean("allow_self_resurrection", false);
		_isOlyRestricted = set.getBoolean("is_oly_restricted", false);
		_forNpc = set.getBoolean("for_npc", false);
		_immediateEffect = set.getBoolean("immediate_effect", false);
		_exImmediateEffect = set.getBoolean("ex_immediate_effect", false);
		_defaultAction = set.getEnum("default_action", ActionType.class, ActionType.NONE);
		_useSkillDisTime = set.getInt("useSkillDisTime", 0);
		_defaultEnchantLevel = set.getInt("enchanted", 0);
		_reuseDelay = set.getInt("reuse_delay", 0);
		_sharedReuseGroup = set.getInt("shared_reuse_group", 0);
		
		String skills = set.getString("item_skill", null);
		if (skills != null)
		{
			final String[] skillsSplit = skills.split(";");
			_skillHolder = new SkillHolder[skillsSplit.length];
			int used = 0;
			for (String element : skillsSplit)
			{
				try
				{
					final String[] skillSplit = element.split("-");
					final int id = Integer.parseInt(skillSplit[0]);
					final int level = Integer.parseInt(skillSplit[1]);
					if (id == 0)
					{
						LOGGER.info("Ignoring item_skill(" + element + ") for item " + this + ". Skill id is 0!");
						continue;
					}
					
					if (level == 0)
					{
						LOGGER.info("Ignoring item_skill(" + element + ") for item " + this + ". Skill level is 0!");
						continue;
					}
					
					_skillHolder[used] = new SkillHolder(id, level);
					++used;
				}
				catch (Exception e)
				{
					LOGGER.warning("Failed to parse item_skill(" + element + ") for item " + this + "! Format: SkillId0-SkillLevel0[;SkillIdN-SkillLevelN]");
				}
			}
			
			// this is only loading? just don't leave a null or use a collection?
			if (used != _skillHolder.length)
			{
				final SkillHolder[] skillHolder = new SkillHolder[used];
				System.arraycopy(_skillHolder, 0, skillHolder, 0, used);
				_skillHolder = skillHolder;
			}
		}
		
		skills = set.getString("unequip_skill", null);
		if (skills != null)
		{
			final String[] info = skills.split("-");
			if ((info != null) && (info.length == 2))
			{
				int id = 0;
				int level = 0;
				try
				{
					id = Integer.parseInt(info[0]);
					level = Integer.parseInt(info[1]);
				}
				catch (Exception nfe)
				{
					// Incorrect syntax, don't add new skill
					LOGGER.info("Could not parse " + skills + " in weapon unequip skills! item " + this);
				}
				
				if ((id > 0) && (level > 0))
				{
					_unequipSkill = new SkillHolder(id, level);
				}
			}
		}
		
		_common = ((_itemId >= 11605) && (_itemId <= 12361));
		_heroItem = ((_itemId >= 6611) && (_itemId <= 6621)) || ((_itemId >= 9388) && (_itemId <= 9390)) || (_itemId == 6842);
		_pvpItem = ((_itemId >= 10667) && (_itemId <= 10835)) || ((_itemId >= 12852) && (_itemId <= 12977)) || ((_itemId >= 14363) && (_itemId <= 14525)) || (_itemId == 14528) || (_itemId == 14529) || (_itemId == 14558) || ((_itemId >= 15913) && (_itemId <= 16024)) || ((_itemId >= 16134) && (_itemId <= 16147)) || (_itemId == 16149) || (_itemId == 16151) || (_itemId == 16153) || (_itemId == 16155) || (_itemId == 16157) || (_itemId == 16159) || ((_itemId >= 16168) && (_itemId <= 16176)) || ((_itemId >= 16179) && (_itemId <= 16220));
	}
	
	/**
	 * Returns the itemType.
	 * @return Enum
	 */
	public abstract ItemType getItemType();
	
	/**
	 * Verifies if the item is an etc item.
	 * @return {@code true} if the item is an etc item, {@code false} otherwise.
	 */
	public boolean isEtcItem()
	{
		return false;
	}
	
	/**
	 * Verifies if the item is an armor.
	 * @return {@code true} if the item is an armor, {@code false} otherwise.
	 */
	public boolean isArmor()
	{
		return false;
	}
	
	/**
	 * Verifies if the item is a weapon.
	 * @return {@code true} if the item is a weapon, {@code false} otherwise.
	 */
	public boolean isWeapon()
	{
		return false;
	}
	
	/**
	 * Verifies if the item is a magic weapon.
	 * @return {@code true} if the weapon is magic, {@code false} otherwise.
	 */
	public boolean isMagicWeapon()
	{
		return false;
	}
	
	/**
	 * @return the _equipReuseDelay
	 */
	public int getEquipReuseDelay()
	{
		return _equipReuseDelay;
	}
	
	/**
	 * Returns the duration of the item
	 * @return int
	 */
	public int getDuration()
	{
		return _duration;
	}
	
	/**
	 * Returns the time of the item
	 * @return long
	 */
	public long getTime()
	{
		return _time;
	}
	
	/**
	 * @return the auto destroy time of the item in seconds: 0 or less - default
	 */
	public int getAutoDestroyTime()
	{
		return _autoDestroyTime;
	}
	
	/**
	 * Returns the ID of the item
	 * @return int
	 */
	public int getId()
	{
		return _itemId;
	}
	
	/**
	 * Returns the ID of the item
	 * @return int
	 */
	public int getDisplayId()
	{
		return _displayId;
	}
	
	public abstract int getItemMask();
	
	/**
	 * Return the type of material of the item
	 * @return MaterialType
	 */
	public MaterialType getMaterialType()
	{
		return _materialType;
	}
	
	/**
	 * Returns the type 2 of the item
	 * @return int
	 */
	public int getType2()
	{
		return _type2;
	}
	
	/**
	 * Returns the weight of the item
	 * @return int
	 */
	public int getWeight()
	{
		return _weight;
	}
	
	/**
	 * Returns if the item is crystallizable
	 * @return boolean
	 */
	public boolean isCrystallizable()
	{
		return (_crystalType != CrystalType.NONE) && (_crystalCount > 0);
	}
	
	/**
	 * @return return General item grade
	 */
	public ItemGrade getItemGrade()
	{
		return ItemGrade.valueOf(_crystalType);
	}
	
	/**
	 * Return the type of crystal if item is crystallizable
	 * @return CrystalType
	 */
	public CrystalType getCrystalType()
	{
		return _crystalType;
	}
	
	/**
	 * Return the ID of crystal if item is crystallizable
	 * @return int
	 */
	public int getCrystalItemId()
	{
		return _crystalType.getCrystalId();
	}
	
	/**
	 * Returns the crystal type.
	 * @return the grade of the item.
	 */
	public CrystalType getCrystalTypePlus()
	{
		return _crystalType;
	}
	
	/**
	 * @return the quantity of crystals for crystallization.
	 */
	public int getCrystalCount()
	{
		return _crystalCount;
	}
	
	/**
	 * @param enchantLevel
	 * @return the quantity of crystals for crystallization on specific enchant level
	 */
	public int getCrystalCount(int enchantLevel)
	{
		if (enchantLevel > 3)
		{
			switch (_type2)
			{
				case TYPE2_SHIELD_ARMOR:
				case TYPE2_ACCESSORY:
				{
					return _crystalCount + (_crystalType.getCrystalEnchantBonusArmor() * ((3 * enchantLevel) - 6));
				}
				case TYPE2_WEAPON:
				{
					return _crystalCount + (_crystalType.getCrystalEnchantBonusWeapon() * ((2 * enchantLevel) - 3));
				}
				default:
				{
					return _crystalCount;
				}
			}
		}
		
		if (enchantLevel <= 0)
		{
			return _crystalCount;
		}
		
		switch (_type2)
		{
			case TYPE2_SHIELD_ARMOR:
			case TYPE2_ACCESSORY:
			{
				return _crystalCount + (_crystalType.getCrystalEnchantBonusArmor() * enchantLevel);
			}
			case TYPE2_WEAPON:
			{
				return _crystalCount + (_crystalType.getCrystalEnchantBonusWeapon() * enchantLevel);
			}
			default:
			{
				return _crystalCount;
			}
		}
	}
	
	/**
	 * @return the name of the item.
	 */
	public String getName()
	{
		return _name;
	}
	
	/**
	 * @return the part of the body used with the item.
	 */
	public BodyPart getBodyPart()
	{
		return _bodyPart;
	}
	
	/**
	 * @return the type 1 of the item.
	 */
	public int getType1()
	{
		return _type1;
	}
	
	/**
	 * @return {@code true} if the item is stackable, {@code false} otherwise.
	 */
	public boolean isStackable()
	{
		return _stackable;
	}
	
	/**
	 * @return {@code true} if the item can be equipped, {@code false} otherwise.
	 */
	public boolean isEquipable()
	{
		return (_bodyPart != BodyPart.NONE) && !(getItemType() instanceof EtcItemType);
	}
	
	/**
	 * @return the price of reference of the item.
	 */
	public int getReferencePrice()
	{
		return _referencePrice;
	}
	
	/**
	 * @return {@code true} if the item can be sold, {@code false} otherwise.
	 */
	public boolean isSellable()
	{
		return _sellable;
	}
	
	/**
	 * @return {@code true} if the item can be dropped, {@code false} otherwise.
	 */
	public boolean isDropable()
	{
		return _dropable;
	}
	
	/**
	 * @return {@code true} if the item can be destroyed, {@code false} otherwise.
	 */
	public boolean isDestroyable()
	{
		return _destroyable;
	}
	
	/**
	 * @return {@code true} if the item can be traded, {@code false} otherwise.
	 */
	public boolean isTradeable()
	{
		return _tradeable;
	}
	
	/**
	 * @return {@code true} if the item can be put into warehouse, {@code false} otherwise.
	 */
	public boolean isDepositable()
	{
		return _depositable;
	}
	
	/**
	 * This method also check the enchant blacklist.
	 * @return {@code true} if the item can be enchanted, {@code false} otherwise.
	 */
	public boolean isEnchantable()
	{
		return (Arrays.binarySearch(PlayerConfig.ENCHANT_BLACKLIST, _itemId) < 0) && _enchantable;
	}
	
	/**
	 * @return {@code true} if the item can be elemented, {@code false} otherwise.
	 */
	public boolean isElementable()
	{
		return _elementable;
	}
	
	/**
	 * Returns if item is common
	 * @return boolean
	 */
	public boolean isCommon()
	{
		return _common;
	}
	
	/**
	 * Returns if item is hero-only
	 * @return
	 */
	public boolean isHeroItem()
	{
		return _heroItem;
	}
	
	/**
	 * Returns if item is pvp
	 * @return
	 */
	public boolean isPvpItem()
	{
		return _pvpItem;
	}
	
	public boolean isPotion()
	{
		return getItemType() == EtcItemType.POTION;
	}
	
	public boolean isElixir()
	{
		return getItemType() == EtcItemType.ELIXIR;
	}
	
	public boolean isScroll()
	{
		return getItemType() == EtcItemType.SCROLL;
	}
	
	/**
	 * Get the functions used by this item.
	 * @param item : Item pointing out the item
	 * @param creature : Creature pointing out the player
	 * @return the list of functions
	 */
	public List<AbstractFunction> getStatFuncs(Item item, Creature creature)
	{
		if ((_funcTemplates == null) || _funcTemplates.isEmpty())
		{
			return Collections.<AbstractFunction> emptyList();
		}
		
		final List<AbstractFunction> funcs = new ArrayList<>(_funcTemplates.size());
		for (FuncTemplate t : _funcTemplates)
		{
			final AbstractFunction f = t.getFunc(creature, creature, item, item);
			if (f != null)
			{
				funcs.add(f);
			}
		}
		
		return funcs;
	}
	
	/**
	 * Checks if a function of the specified class is already attached to this item.
	 * @param functionClass the class of the function to search for
	 * @return {@code true} if a function of the specified class exists, {@code false} otherwise
	 */
	public boolean hasFunction(Class<? extends AbstractFunction> functionClass)
	{
		if ((_funcTemplates == null) || _funcTemplates.isEmpty())
		{
			return false;
		}
		
		for (FuncTemplate template : _funcTemplates)
		{
			if (template.getClass().equals(functionClass))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Add the FuncTemplate f to the list of functions used with the item
	 * @param f : FuncTemplate to add
	 */
	public void attach(FuncTemplate f)
	{
		if (_funcTemplates == null)
		{
			_funcTemplates = new ArrayList<>(1);
		}
		
		_funcTemplates.add(f);
	}
	
	public void attach(Condition c)
	{
		if (_preConditions == null)
		{
			_preConditions = new ArrayList<>(1);
		}
		
		if (!_preConditions.contains(c))
		{
			_preConditions.add(c);
		}
	}
	
	public boolean hasSkills()
	{
		return _skillHolder != null;
	}
	
	/**
	 * Method to retrieve skills linked to this item armor and weapon: passive skills etcitem: skills used on item use <-- ???
	 * @return Skills linked to this item as SkillHolder[]
	 */
	public SkillHolder[] getSkills()
	{
		return _skillHolder;
	}
	
	/**
	 * @return skill that activates, when player unequip this weapon or armor
	 */
	public Skill getUnequipSkill()
	{
		return _unequipSkill == null ? null : _unequipSkill.getSkill();
	}
	
	public boolean checkCondition(Creature creature, WorldObject object, boolean sendMessage)
	{
		if (creature.isGM() && !GeneralConfig.GM_ITEM_RESTRICTION)
		{
			return true;
		}
		
		// Don't allow hero equipment and restricted items during Olympiad
		if ((isOlyRestrictedItem() || _heroItem) && creature.isPlayer() && creature.asPlayer().isInOlympiadMode())
		{
			if (isEquipable())
			{
				creature.sendPacket(SystemMessageId.YOU_CANNOT_EQUIP_THAT_ITEM_IN_A_GRAND_OLYMPIAD_GAMES_MATCH);
			}
			else
			{
				creature.sendPacket(SystemMessageId.YOU_CANNOT_USE_THAT_ITEM_IN_A_GRAND_OLYMPIAD_GAMES_MATCH);
			}
			
			return false;
		}
		
		if (!isConditionAttached())
		{
			return true;
		}
		
		final Creature target = object.isCreature() ? object.asCreature() : null;
		for (Condition preCondition : _preConditions)
		{
			if (preCondition == null)
			{
				continue;
			}
			
			if (!preCondition.test(creature, target, null, null))
			{
				if (creature.isSummon())
				{
					creature.sendPacket(SystemMessageId.THIS_PET_CANNOT_USE_THIS_ITEM);
					return false;
				}
				
				if (sendMessage)
				{
					final String msg = preCondition.getMessage();
					final int msgId = preCondition.getMessageId();
					if (msg != null)
					{
						creature.sendMessage(msg);
					}
					else if (msgId != 0)
					{
						final SystemMessage sm = new SystemMessage(msgId);
						if (preCondition.isAddName())
						{
							sm.addItemName(_itemId);
						}
						
						creature.sendPacket(sm);
					}
				}
				
				return false;
			}
		}
		
		return true;
	}
	
	public boolean isConditionAttached()
	{
		return (_preConditions != null) && !_preConditions.isEmpty();
	}
	
	public boolean isQuestItem()
	{
		return _questItem;
	}
	
	public boolean isFreightable()
	{
		return _freightable;
	}
	
	public boolean isAllowSelfResurrection()
	{
		return _allowSelfResurrection;
	}
	
	public boolean isOlyRestrictedItem()
	{
		return _isOlyRestricted || OlympiadConfig.LIST_OLY_RESTRICTED_ITEMS.contains(_itemId);
	}
	
	public boolean isForNpc()
	{
		return _forNpc;
	}
	
	/**
	 * Verifies if the item has effects immediately.<br>
	 * <i>Used for herbs mostly.</i>
	 * @return {@code true} if the item applies effects immediately, {@code false} otherwise
	 */
	public boolean hasExImmediateEffect()
	{
		return _exImmediateEffect;
	}
	
	/**
	 * Verifies if the item has effects immediately.
	 * @return {@code true} if the item applies effects immediately, {@code false} otherwise
	 */
	public boolean hasImmediateEffect()
	{
		return _immediateEffect;
	}
	
	/**
	 * @return the _default_action
	 */
	public ActionType getDefaultAction()
	{
		return _defaultAction;
	}
	
	public int useSkillDisTime()
	{
		return _useSkillDisTime;
	}
	
	/**
	 * Gets the item reuse delay time in seconds.
	 * @return the reuse delay time
	 */
	public int getReuseDelay()
	{
		return _reuseDelay;
	}
	
	/**
	 * Gets the shared reuse group.<br>
	 * Items with the same reuse group will render reuse delay upon those items when used.
	 * @return the shared reuse group
	 */
	public int getSharedReuseGroup()
	{
		return _sharedReuseGroup;
	}
	
	/**
	 * Usable in HTML windows.
	 * @return the icon link in client files
	 */
	public String getIcon()
	{
		return _icon;
	}
	
	public int getDefaultEnchantLevel()
	{
		return _defaultEnchantLevel;
	}
	
	public boolean isPetItem()
	{
		return getItemType() == EtcItemType.PET_COLLAR;
	}
	
	/**
	 * @param extractableProduct
	 */
	public void addCapsuledItem(ExtractableProduct extractableProduct)
	{
	}
	
	public Skill getEnchant4Skill()
	{
		return null;
	}
	
	/**
	 * Returns the name of the item followed by the item ID.
	 * @return the name and the ID of the item
	 */
	@Override
	public String toString()
	{
		return StringUtil.concat(_name, "(", String.valueOf(_itemId), ")");
	}
}
