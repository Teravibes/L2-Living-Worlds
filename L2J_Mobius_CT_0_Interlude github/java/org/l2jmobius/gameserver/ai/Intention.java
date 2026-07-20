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
package org.l2jmobius.gameserver.ai;

/**
 * Enumeration of generic intentions of an NPC/PC, an intention may require several steps to be completed.
 */
public enum Intention
{
	/** Do nothing; disconnect AI if no players are around. */
	IDLE,
	
	/** Alerted state without a goal: scan targets, random walk, etc. */
	ACTIVE,
	
	/** Rest (sit until attacked). */
	REST,
	
	/** Attack target (cast combat magic, go to target, combat). */
	ATTACK,
	
	/** Cast a spell; may start or stop attacking depending on the spell. */
	CAST,
	
	/** Move to another location. */
	MOVE_TO,
	
	/** Follow a target, adjusting movement based on the target's actions. */
	FOLLOW,
	
	/** Pick up an item (go to item, pick it up, then become idle). */
	PICK_UP,
	
	/** Move to target and then interact with it. */
	INTERACT;
}
