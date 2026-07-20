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
package org.l2jmobius.gameserver.data.holders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Mobius
 */
public class FakePlayerChatHolder
{
	private final String _fpcName;
	private final String _searchMethod;
	private final List<String> _searchText;
	private final List<String> _answers;
	
	public FakePlayerChatHolder(String fpcName, String searchMethod, String searchText, String answers)
	{
		_fpcName = fpcName;
		_searchMethod = searchMethod;
		_searchText = new ArrayList<>(Arrays.asList(searchText.split(";")));
		_answers = new ArrayList<>(Arrays.asList(answers.split(";")));
	}
	
	public String getFpcName()
	{
		return _fpcName;
	}
	
	public String getSearchMethod()
	{
		return _searchMethod;
	}
	
	public List<String> getSearchText()
	{
		return _searchText;
	}
	
	public List<String> getAnswers()
	{
		return _answers;
	}
}
