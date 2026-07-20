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
package org.l2jmobius.commons.ui;

import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;

/**
 * A {@link DocumentListener} to limit the maximum number of lines in a Document.<br>
 * If the number of lines exceeds the specified limit, excess lines will be removed either from the start or the end of the Document, depending on the specified configuration:<br>
 * a) Removing from the start is typically used when appending text.<br>
 * b) Removing from the end is used when inserting text at the beginning.
 * @author Mobius
 */
public class LineLimitListener implements DocumentListener
{
	private final boolean _removeFromStart;
	private final int _maxLines;
	
	/**
	 * Constructs a LineLimitListener with a specified maximum line count.<br>
	 * By default, this configuration removes excess lines from the start of the Document.
	 * @param maxLines the maximum number of lines to retain in the Document
	 */
	public LineLimitListener(int maxLines)
	{
		this(maxLines, true);
	}
	
	/**
	 * Constructs a LineLimitListener with a specified maximum line count<br>
	 * and a setting to control where excess lines are removed from.
	 * @param maxLines the maximum number of lines to retain in the Document
	 * @param removeFromStart if true, excess lines are removed from the start; if false, from the end
	 */
	public LineLimitListener(int maxLines, boolean removeFromStart)
	{
		_removeFromStart = removeFromStart;
		_maxLines = maxLines;
	}
	
	/**
	 * Returns the maximum number of lines that this listener will retain in the Document.
	 * @return the maximum line count allowed in the Document
	 */
	public int getLimitLines()
	{
		return _maxLines;
	}
	
	/**
	 * Removes excess lines from the Document when the line count exceeds the maximum limit.<br>
	 * This method determines whether to remove lines from the start or end based on the configured setting.
	 * @param event the DocumentEvent that triggered this method call
	 */
	private void removeLines(DocumentEvent event)
	{
		// The root Element of the Document will tell us the total number of line in the Document.
		final Document document = event.getDocument();
		final Element root = document.getDefaultRootElement();
		
		while (root.getElementCount() > _maxLines)
		{
			if (_removeFromStart)
			{
				removeFromStart(document, root);
			}
			else
			{
				removeFromEnd(document, root);
			}
		}
	}
	
	/**
	 * Removes lines from the start of the Document until the line count is within the limit.
	 * @param document the Document to be modified
	 * @param root the root Element representing all lines in the Document
	 */
	private void removeFromStart(Document document, Element root)
	{
		final Element line = root.getElement(0);
		final int end = line.getEndOffset();
		
		try
		{
			document.remove(0, end);
		}
		catch (BadLocationException ble)
		{
			System.out.println(ble);
		}
	}
	
	/**
	 * Removes lines from the end of the Document until the line count is within the limit.<br>
	 * The newline character preceding the last line is also removed to maintain line integrity.
	 * @param document the Document to be modified
	 * @param root the root Element representing all lines in the Document
	 */
	private void removeFromEnd(Document document, Element root)
	{
		// We use start minus 1 to make sure we remove the newline character of the previous line.
		final Element line = root.getElement(root.getElementCount() - 1);
		final int start = line.getStartOffset();
		final int end = line.getEndOffset();
		
		try
		{
			document.remove(start - 1, end - start);
		}
		catch (BadLocationException e)
		{
			System.out.println(e);
		}
	}
	
	/**
	 * Handles the insertion of new text into the Document. After text is inserted,<br>
	 * this method schedules a check (on the EDT) to remove lines if the maximum line count is exceeded.
	 * @param event the DocumentEvent representing the text insertion
	 */
	@Override
	public void insertUpdate(DocumentEvent event)
	{
		SwingUtilities.invokeLater(() -> removeLines(event));
	}
	
	/**
	 * No action taken on text removal, as this event does not affect line limit enforcement.
	 * @param event the DocumentEvent representing the text removal
	 */
	@Override
	public void removeUpdate(DocumentEvent event)
	{
		// No action required.
	}
	
	/**
	 * No action taken on attribute changes, as this event does not affect line limit enforcement.
	 * @param event the DocumentEvent representing the attribute change
	 */
	@Override
	public void changedUpdate(DocumentEvent event)
	{
		// No action required.
	}
}
