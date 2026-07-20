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
 * Enum representing possible actions that can occur for an AI character.
 */
public enum Action
{
	/** AI must decide the next action after a change. */
	THINK,
	
	/** Actor was attacked, may trigger a response. */
	ATTACKED,
	
	/** Increase/decrease aggression towards a target or reduce global aggression. */
	AGGRESSION,
	
	/** Actor is stunned and cannot act. */
	STUNNED,
	
	/** Actor is paralyzed or petrified and cannot move or act. */
	PARALYZED,
	
	/** Actor starts or stops sleeping. */
	SLEEPING,
	
	/** Actor is rooted and cannot move. */
	ROOTED,
	
	/** Actor evaded an attack. */
	EVADED,
	
	/** Previous action was completed, ready for the next. */
	READY_TO_ACT,
	
	/** User's command, such as using combat magic or changing weapons. */
	USER_CMD,
	
	/** Actor arrived at the assigned location. */
	ARRIVED,
	
	/** Actor arrived at an intermediate point and needs to revalidate destination. */
	ARRIVED_REVALIDATE,
	
	/** Actor cannot move further. */
	ARRIVED_BLOCKED,
	
	/** Actor forgets a specific object. */
	FORGET_OBJECT,
	
	/** Attempt to cancel the current step without changing intention. */
	CANCEL,
	
	/** Actor is dead. */
	DEATH,
	
	/** Actor appears to be dead but isn't. */
	FAKE_DEATH,
	
	/** Actor attacks randomly. */
	CONFUSED,
	
	/** Actor cannot cast spells. */
	MUTED,
	
	/** Actor flees in random directions. */
	AFRAID,
	
	/** Actor finishes casting a spell. */
	FINISH_CASTING,
	
	/** Actor betrays its master. */
	BETRAYED
}
