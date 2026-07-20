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
package org.l2jmobius.gameserver.model.script;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.managers.ScriptManager;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.serverpackets.ExShowQuestMark;
import org.l2jmobius.gameserver.network.serverpackets.PlaySound;
import org.l2jmobius.gameserver.network.serverpackets.QuestList;
import org.l2jmobius.gameserver.network.serverpackets.TutorialCloseHtml;
import org.l2jmobius.gameserver.network.serverpackets.TutorialEnableClientEvent;
import org.l2jmobius.gameserver.network.serverpackets.TutorialShowHtml;
import org.l2jmobius.gameserver.network.serverpackets.TutorialShowQuestionMark;

/**
 * Quest state class.
 * @author Luis Arias, Mobius
 */
public class QuestState
{
	protected static final Logger LOGGER = Logger.getLogger(QuestState.class.getName());
	
	// Constants
	private static final String COND_VAR = "cond";
	private static final String RESTART_VAR = "restartTime";
	private static final String MEMO_VAR = "memoState";
	private static final String MEMO_EX_VAR = "memoStateEx";
	private static final int RESET_HOUR = 6;
	private static final int RESET_MINUTES = 30;
	
	/** The name of the quest of this QuestState */
	private final String _questName;
	
	/** The "owner" of this QuestState object */
	private final Player _player;
	
	/** The current state of the quest */
	private byte _state;
	
	/** The current condition of the quest */
	private int _cond = 0;
	
	/** A map of key->value pairs containing the quest state variables and their values */
	private Map<String, String> _vars;
	
	/**
	 * boolean flag letting QuestStateManager know to exit quest when cleaning up
	 */
	private boolean _isExitQuestOnCleanUp = false;
	
	/**
	 * Constructor of the QuestState. Creates the QuestState object and sets the player's progress of the quest to this QuestState.
	 * @param quest the {@link Quest} object associated with the QuestState
	 * @param player the owner of this {@link QuestState} object
	 * @param state the initial state of the quest
	 */
	public QuestState(Quest quest, Player player, byte state)
	{
		_questName = quest.getName();
		_player = player;
		_state = state;
		player.setQuestState(this);
	}
	
	/**
	 * @return the name of the quest of this QuestState
	 */
	public String getQuestName()
	{
		return _questName;
	}
	
	/**
	 * @return the {@link Quest} object of this QuestState
	 */
	public Quest getQuest()
	{
		return ScriptManager.getInstance().getScript(_questName);
	}
	
	/**
	 * @return the {@link Player} object of the owner of this QuestState
	 */
	public Player getPlayer()
	{
		return _player;
	}
	
	/**
	 * @return the current State of this QuestState
	 * @see org.l2jmobius.gameserver.model.script.State
	 */
	public byte getState()
	{
		return _state;
	}
	
	/**
	 * @return {@code true} if the State of this QuestState is CREATED, {@code false} otherwise
	 * @see org.l2jmobius.gameserver.model.script.State
	 */
	public boolean isCreated()
	{
		return _state == State.CREATED;
	}
	
	/**
	 * @return {@code true} if the State of this QuestState is STARTED, {@code false} otherwise
	 * @see org.l2jmobius.gameserver.model.script.State
	 */
	public boolean isStarted()
	{
		return _state == State.STARTED;
	}
	
	/**
	 * @return {@code true} if the State of this QuestState is COMPLETED, {@code false} otherwise
	 * @see org.l2jmobius.gameserver.model.script.State
	 */
	public boolean isCompleted()
	{
		return _state == State.COMPLETED;
	}
	
	/**
	 * @param state the new state of the quest to set
	 * @see #setState(byte state, boolean saveInDb)
	 * @see org.l2jmobius.gameserver.model.script.State
	 */
	public void setState(byte state)
	{
		setState(state, true);
	}
	
	/**
	 * Change the state of this quest to the specified value.
	 * @param state the new state of the quest to set
	 * @param saveInDb if {@code true}, will save the state change in the database
	 * @see org.l2jmobius.gameserver.model.script.State
	 */
	public void setState(byte state, boolean saveInDb)
	{
		if (_state == state)
		{
			return;
		}
		
		final boolean newQuest = isCreated();
		_state = state;
		if (saveInDb)
		{
			if (newQuest)
			{
				Quest.createQuestInDb(this);
			}
			else
			{
				Quest.updateQuestInDb(this);
			}
		}
		
		_player.sendPacket(new QuestList(_player));
	}
	
	/**
	 * Add parameter used in quests.
	 * @param variable String pointing out the name of the variable for quest
	 * @param value String pointing out the value of the variable for quest
	 */
	public void setInternal(String variable, String value)
	{
		if (_vars == null)
		{
			_vars = new HashMap<>();
		}
		
		if (value == null)
		{
			_vars.put(variable, "");
			return;
		}
		
		if (COND_VAR.equals(variable))
		{
			try
			{
				_cond = Integer.parseInt(value);
			}
			catch (Exception ignored)
			{
			}
		}
		
		_vars.put(variable, value);
	}
	
	public void set(String variable, int value)
	{
		set(variable, Integer.toString(value));
	}
	
	/**
	 * Return value of parameter "value" after adding the couple (var,value) in class variable "vars".<br>
	 * Actions:<br>
	 * <ul>
	 * <li>Initialize class variable "vars" if is null.</li>
	 * <li>Initialize parameter "value" if is null</li>
	 * <li>Add/Update couple (var,value) in class variable Map "vars"</li>
	 * <li>If the key represented by "var" exists in Map "vars", the couple (var,value) is updated in the database.<br>
	 * The key is known as existing if the preceding value of the key (given as result of function put()) is not null.<br>
	 * If the key doesn't exist, the couple is added/created in the database</li>
	 * <ul>
	 * @param variable String indicating the name of the variable for quest
	 * @param value String indicating the value of the variable for quest
	 */
	public void set(String variable, String value)
	{
		if (_vars == null)
		{
			_vars = new HashMap<>();
		}
		
		String newValue = value;
		if (newValue == null)
		{
			newValue = "";
		}
		
		final String old = _vars.put(variable, newValue);
		if (old != null)
		{
			Quest.updateQuestVarInDb(this, variable, newValue);
		}
		else
		{
			Quest.createQuestVarInDb(this, variable, newValue);
		}
		
		if (COND_VAR.equals(variable))
		{
			try
			{
				int previousVal = 0;
				try
				{
					previousVal = Integer.parseInt(old);
				}
				catch (Exception ignored)
				{
				}
				
				int newCond = 0;
				try
				{
					newCond = Integer.parseInt(newValue);
				}
				catch (Exception ignored)
				{
				}
				
				_cond = newCond;
				setCond(newCond, previousVal);
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, _player.getName() + ", " + _questName + " cond [" + newValue + "] is not an integer.  Value stored, but no packet was sent: " + e.getMessage(), e);
			}
		}
	}
	
	/**
	 * Internally handles the progression of the quest so that it is ready for sending appropriate packets to the client.<br>
	 * <u><i>Actions :</i></u><br>
	 * <ul>
	 * <li>Check if the new progress number resets the quest to a previous (smaller) step.</li>
	 * <li>If not, check if quest progress steps have been skipped.</li>
	 * <li>If skipped, prepare the variable completedStateFlags appropriately to be ready for sending to clients.</li>
	 * <li>If no steps were skipped, flags do not need to be prepared...</li>
	 * <li>If the passed step resets the quest to a previous step, reset such that steps after the parameter are not considered, while skipped steps before the parameter, if any, maintain their info.</li>
	 * </ul>
	 * @param cond the current quest progress condition (0 - 31 including)
	 * @param old the previous quest progress condition to check against
	 */
	private void setCond(int cond, int old)
	{
		if (cond == old)
		{
			return;
		}
		
		int completedStateFlags = 0;
		
		// cond 0 and 1 do not need completedStateFlags. Also, if cond > 1, the 1st step must
		// always exist (i.e. it can never be skipped). So if cond is 2, we can still safely
		// assume no steps have been skipped.
		// Finally, more than 31 steps CANNOT be supported in any way with skipping.
		if ((cond < 3) || (cond > 31))
		{
			unset("__compltdStateFlags");
		}
		else
		{
			completedStateFlags = getInt("__compltdStateFlags");
		}
		
		// case 1: No steps have been skipped so far...
		if (completedStateFlags == 0)
		{
			// Check if this step also doesn't skip anything. If so, no further work is needed also, in this case, no work is needed if the state is being reset to a smaller value in those cases, skip forward to informing the client about the change...
			// ELSE, if we just now skipped for the first time...prepare the flags!!!
			if (cond > (old + 1))
			{
				// set the most significant bit to 1 (indicates that there exist skipped states)
				// also, ensure that the least significant bit is an 1 (the first step is never skipped, no matter what the cond says)
				completedStateFlags = 0x80000001;
				
				// since no flag had been skipped until now, the least significant bits must all be set to 1, up until "old" number of bits.
				completedStateFlags |= (1 << old) - 1;
				
				// now, just set the bit corresponding to the passed cond to 1 (current step)
				completedStateFlags |= 1 << (cond - 1);
				set("__compltdStateFlags", String.valueOf(completedStateFlags));
			}
		}
		// case 2: There were exist previously skipped steps
		else if (cond < old) // if this is a push back to a previous step, clear all completion flags ahead
		{
			completedStateFlags &= (1 << cond) - 1; // note, this also unsets the flag indicating that there exist skips
			
			// now, check if this resulted in no steps being skipped any more
			if (completedStateFlags == ((1 << cond) - 1))
			{
				unset("__compltdStateFlags");
			}
			else
			{
				// set the most significant bit back to 1 again, to correctly indicate that this skips states.
				// also, ensure that the least significant bit is an 1 (the first step is never skipped, no matter what the cond says)
				completedStateFlags |= 0x80000001;
				set("__compltdStateFlags", String.valueOf(completedStateFlags));
			}
		}
		
		// If this moves forward, it changes nothing on previously skipped steps.
		// Just mark this state and we are done.
		else
		{
			completedStateFlags |= 1 << (cond - 1);
			set("__compltdStateFlags", String.valueOf(completedStateFlags));
		}
		
		// send a packet to the client to inform it of the quest progress (step change)
		_player.sendPacket(new QuestList(_player));
		
		final Quest q = getQuest();
		if (!q.isCustomQuest() && (cond > 0))
		{
			_player.sendPacket(new ExShowQuestMark(q.getId()));
		}
	}
	
	/**
	 * Removes a quest variable from the list of existing quest variables.
	 * @param variable the name of the variable to remove
	 */
	public void unset(String variable)
	{
		if (_vars == null)
		{
			return;
		}
		
		final String old = _vars.remove(variable);
		if (old != null)
		{
			if (COND_VAR.equals(variable))
			{
				_cond = 0;
			}
			
			Quest.deleteQuestVarInDb(this, variable);
		}
	}
	
	/**
	 * @param variable the name of the variable to get
	 * @return the value of the variable from the list of quest variables
	 */
	public String get(String variable)
	{
		if (_vars == null)
		{
			return null;
		}
		
		return _vars.get(variable);
	}
	
	/**
	 * @param variable the name of the variable to get
	 * @return the integer value of the variable or 0 if the variable does not exist or its value is not an integer
	 */
	public int getInt(String variable)
	{
		if (_vars == null)
		{
			return 0;
		}
		
		final String varStr = _vars.get(variable);
		if ((varStr == null) || varStr.isEmpty())
		{
			return 0;
		}
		
		int varInt = 0;
		try
		{
			varInt = Integer.parseInt(varStr);
		}
		catch (NumberFormatException nfe)
		{
			LOGGER.log(Level.INFO, "Quest " + _questName + ", method getInt(" + variable + "), tried to parse a non-integer value (" + varStr + "). Char Id: " + _player.getObjectId(), nfe);
		}
		
		return varInt;
	}
	
	/**
	 * Checks if the quest state progress ({@code cond}) is at the specified step.
	 * @param condition the condition to check against
	 * @return {@code true} if the quest condition is equal to {@code condition}, {@code false} otherwise
	 * @see #getInt(String var)
	 */
	public boolean isCond(int condition)
	{
		return _cond == condition;
	}
	
	/**
	 * Sets the quest state progress ({@code cond}) to the specified step.
	 * @param value the new value of the quest state progress
	 * @see #set(String var, String value)
	 * @see #setCond(int, boolean)
	 */
	public void setCond(int value)
	{
		if (isStarted())
		{
			set(COND_VAR, Integer.toString(value));
		}
	}
	
	/**
	 * @return the current quest progress ({@code cond})
	 */
	public int getCond()
	{
		if (isStarted())
		{
			return _cond;
		}
		
		return 0;
	}
	
	/**
	 * Check if a given variable is set for this quest.
	 * @param variable the variable to check
	 * @return {@code true} if the variable is set, {@code false} otherwise
	 * @see #get(String)
	 * @see #getInt(String)
	 * @see #getCond()
	 */
	public boolean isSet(String variable)
	{
		return get(variable) != null;
	}
	
	/**
	 * Sets the quest state progress ({@code cond}) to the specified step.
	 * @param value the new value of the quest state progress
	 * @param playQuestMiddle if {@code true}, plays "ItemSound.quest_middle"
	 * @see #setCond(int value)
	 * @see #set(String var, String value)
	 */
	public void setCond(int value, boolean playQuestMiddle)
	{
		if (!isStarted())
		{
			return;
		}
		
		set(COND_VAR, String.valueOf(value));
		if (playQuestMiddle)
		{
			_player.sendPacket(QuestSound.ITEMSOUND_QUEST_MIDDLE.getPacket());
		}
	}
	
	public void setMemoState(int value)
	{
		set(MEMO_VAR, String.valueOf(value));
	}
	
	/**
	 * @return the current Memo State
	 */
	public int getMemoState()
	{
		if (isStarted())
		{
			return getInt(MEMO_VAR);
		}
		
		return 0;
	}
	
	public boolean isMemoState(int memoState)
	{
		return getInt(MEMO_VAR) == memoState;
	}
	
	/**
	 * Gets the memo state ex.
	 * @param slot the slot where the value was saved
	 * @return the memo state ex
	 */
	public int getMemoStateEx(int slot)
	{
		if (isStarted())
		{
			return getInt(MEMO_EX_VAR + slot);
		}
		
		return 0;
	}
	
	/**
	 * Sets the memo state ex.
	 * @param slot the slot where the value will be saved
	 * @param value the value
	 */
	public void setMemoStateEx(int slot, int value)
	{
		set(MEMO_EX_VAR + slot, String.valueOf(value));
	}
	
	/**
	 * Verifies if the given value is equal to the current memos state ex.
	 * @param slot the slot where the value was saved
	 * @param memoStateEx the value to verify
	 * @return {@code true} if the values are equal, {@code false} otherwise
	 */
	public boolean isMemoStateEx(int slot, int memoStateEx)
	{
		return getMemoStateEx(slot) == memoStateEx;
	}
	
	/**
	 * Add player to get notification of characters death
	 * @param creature the {@link Creature} object of the character to get notification of death
	 */
	public void addNotifyOfDeath(Creature creature)
	{
		if (!creature.isPlayer())
		{
			return;
		}
		
		creature.asPlayer().addNotifyQuestOfDeath(this);
	}
	
	/**
	 * @return {@code true} if quest is to be exited on clean up by QuestStateManager, {@code false} otherwise
	 */
	public boolean isExitQuestOnCleanUp()
	{
		return _isExitQuestOnCleanUp;
	}
	
	/**
	 * @param isExitQuestOnCleanUp {@code true} if quest is to be exited on clean up by QuestStateManager, {@code false} otherwise
	 */
	public void setIsExitQuestOnCleanUp(boolean isExitQuestOnCleanUp)
	{
		_isExitQuestOnCleanUp = isExitQuestOnCleanUp;
	}
	
	/**
	 * Set condition to 1, state to STARTED and play the "ItemSound.quest_accept".<br>
	 * Works only if state is CREATED and the quest is not a custom quest.
	 */
	public void startQuest()
	{
		if (isCreated() && !getQuest().isCustomQuest())
		{
			set(COND_VAR, "1");
			setState(State.STARTED);
			_player.sendPacket(QuestSound.ITEMSOUND_QUEST_ACCEPT.getPacket());
		}
	}
	
	/**
	 * Finishes the quest and removes all quest items associated with this quest from the player's inventory.<br>
	 * If {@code type} is {@code QuestType.ONE_TIME}, also removes all other quest data associated with this quest.
	 * @param type the {@link QuestType} of the quest
	 * @see #exitQuest(QuestType type, boolean playExitQuest)
	 * @see #exitQuest(boolean repeatable)
	 * @see #exitQuest(boolean repeatable, boolean playExitQuest)
	 */
	public void exitQuest(QuestType type)
	{
		switch (type)
		{
			case DAILY:
			{
				exitQuest(false);
				setRestartTime();
				break;
			}
			
			// case ONE_TIME:
			// case REPEATABLE:
			default:
			{
				exitQuest(type == QuestType.REPEATABLE);
				break;
			}
		}
	}
	
	/**
	 * Finishes the quest and removes all quest items associated with this quest from the player's inventory.<br>
	 * If {@code type} is {@code QuestType.ONE_TIME}, also removes all other quest data associated with this quest.
	 * @param type the {@link QuestType} of the quest
	 * @param playExitQuest if {@code true}, plays "ItemSound.quest_finish"
	 * @see #exitQuest(QuestType type)
	 * @see #exitQuest(boolean repeatable)
	 * @see #exitQuest(boolean repeatable, boolean playExitQuest)
	 */
	public void exitQuest(QuestType type, boolean playExitQuest)
	{
		exitQuest(type);
		if (playExitQuest)
		{
			_player.sendPacket(QuestSound.ITEMSOUND_QUEST_FINISH.getPacket());
		}
	}
	
	/**
	 * Finishes the quest and removes all quest items associated with this quest from the player's inventory.<br>
	 * If {@code repeatable} is set to {@code false}, also removes all other quest data associated with this quest.
	 * @param repeatable if {@code true}, deletes all data and variables of this quest, otherwise keeps them
	 * @see #exitQuest(QuestType type)
	 * @see #exitQuest(QuestType type, boolean playExitQuest)
	 * @see #exitQuest(boolean repeatable, boolean playExitQuest)
	 */
	public void exitQuest(boolean repeatable)
	{
		_player.removeNotifyQuestOfDeath(this);
		
		if (!isStarted())
		{
			return;
		}
		
		// Clean registered quest items
		getQuest().removeRegisteredQuestItems(_player);
		
		Quest.deleteQuestInDb(this, repeatable);
		if (repeatable)
		{
			_player.delQuestState(_questName);
			_player.sendPacket(new QuestList(_player));
		}
		else
		{
			setState(State.COMPLETED);
		}
		
		_vars = null;
	}
	
	/**
	 * Finishes the quest and removes all quest items associated with this quest from the player's inventory.<br>
	 * If {@code repeatable} is set to {@code false}, also removes all other quest data associated with this quest.
	 * @param repeatable if {@code true}, deletes all data and variables of this quest, otherwise keeps them
	 * @param playExitQuest if {@code true}, plays "ItemSound.quest_finish"
	 * @see #exitQuest(QuestType type)
	 * @see #exitQuest(QuestType type, boolean playExitQuest)
	 * @see #exitQuest(boolean repeatable)
	 */
	public void exitQuest(boolean repeatable, boolean playExitQuest)
	{
		exitQuest(repeatable);
		if (playExitQuest)
		{
			_player.sendPacket(QuestSound.ITEMSOUND_QUEST_FINISH.getPacket());
		}
	}
	
	public void showQuestionMark(int number)
	{
		_player.sendPacket(new TutorialShowQuestionMark(number));
	}
	
	// TODO: make tutorial voices the same as quest sounds
	public void playTutorialVoice(String voice)
	{
		_player.sendPacket(new PlaySound(2, voice, 0, 0, _player.getX(), _player.getY(), _player.getZ()));
	}
	
	/**
	 * Set the restart time for the daily quests.
	 */
	public void setRestartTime()
	{
		final Calendar reDo = Calendar.getInstance();
		if (reDo.get(Calendar.HOUR_OF_DAY) >= RESET_HOUR)
		{
			reDo.add(Calendar.DATE, 1);
		}
		
		reDo.set(Calendar.HOUR_OF_DAY, RESET_HOUR);
		reDo.set(Calendar.MINUTE, RESET_MINUTES);
		set(RESTART_VAR, String.valueOf(reDo.getTimeInMillis()));
	}
	
	/**
	 * Check if a daily quest is available to be started over.
	 * @return {@code true} if the quest is available, {@code false} otherwise.
	 */
	public boolean isNowAvailable()
	{
		final String val = get(RESTART_VAR);
		return (val != null) && (Long.parseLong(val) <= System.currentTimeMillis());
	}
	
	/**
	 * Used only in 255_Tutorial
	 * @param html
	 */
	public void showTutorialHTML(String html)
	{
		String text = HtmCache.getInstance().getHtm(_player, "data/scripts/quests/Q00255_Tutorial/" + html);
		if (text == null)
		{
			LOGGER.warning("missing html page data/scripts/quests/Q00255_Tutorial/" + html);
			text = "<html><body>File data/scripts/quests/Q00255_Tutorial/" + html + " not found or file is empty.</body></html>";
		}
		
		_player.sendPacket(new TutorialShowHtml(text));
	}
	
	/**
	 * Used only in 255_Tutorial
	 */
	public void closeTutorialHtml()
	{
		_player.sendPacket(TutorialCloseHtml.STATIC_PACKET);
	}
	
	/**
	 * Used only in 255_Tutorial
	 * @param number
	 */
	public void onTutorialClientEvent(int number)
	{
		_player.sendPacket(new TutorialEnableClientEvent(number));
	}
}
