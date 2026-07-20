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

import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JWindow;

/**
 * @author Mobius
 */
public class SplashScreen extends JWindow
{
	private final Image _image;
	
	/**
	 * @param path of image file
	 * @param time in milliseconds
	 * @param parent frame to set visible after time ends
	 */
	public SplashScreen(String path, long time, JFrame parent)
	{
		setBackground(new Color(0, 255, 0, 0)); // Transparency.
		_image = Toolkit.getDefaultToolkit().getImage(path);
		final ImageIcon imageIcon = new ImageIcon(_image);
		setSize(imageIcon.getIconWidth(), imageIcon.getIconHeight());
		setLocationRelativeTo(null);
		setAlwaysOnTop(true);
		setVisible(true);
		
		new Timer().schedule(new TimerTask()
		{
			@Override
			public void run()
			{
				setVisible(false);
				if (parent != null)
				{
					// Make parent visible.
					parent.setVisible(true);
					
					// Focus parent window.
					parent.toFront();
					parent.setState(Frame.ICONIFIED);
					parent.setState(Frame.NORMAL);
				}
				
				dispose();
			}
		}, imageIcon.getIconWidth() > 0 ? time : 100);
	}
	
	@Override
	public void paint(Graphics g)
	{
		g.drawImage(_image, 0, 0, null);
	}
}
