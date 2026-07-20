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
package org.l2jmobius.loginserver.ui;

import java.awt.Desktop;
import java.awt.Font;
import java.awt.Window.Type;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

/**
 * @author Mobius
 */
public class AboutFrame
{
	private static final String URL = "www.l2jmobius.org";
	
	private final URI _uri;
	private JFrame _frmAbout;
	
	public AboutFrame()
	{
		initialize();
		_uri = createURI(URL);
		_frmAbout.setVisible(true);
	}
	
	private void initialize()
	{
		_frmAbout = new JFrame();
		_frmAbout.setResizable(false);
		_frmAbout.setTitle("About");
		_frmAbout.setBounds(100, 100, 297, 197);
		_frmAbout.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		_frmAbout.setType(Type.UTILITY);
		_frmAbout.getContentPane().setLayout(null);
		
		final JLabel lblLjmobius = new JLabel("L2jMobius");
		lblLjmobius.setFont(new Font("Tahoma", Font.PLAIN, 32));
		lblLjmobius.setHorizontalAlignment(SwingConstants.CENTER);
		lblLjmobius.setBounds(10, 11, 271, 39);
		_frmAbout.getContentPane().add(lblLjmobius);
		
		final JLabel lblData = new JLabel("2013-" + Calendar.getInstance().get(Calendar.YEAR));
		lblData.setHorizontalAlignment(SwingConstants.CENTER);
		lblData.setBounds(10, 44, 271, 14);
		_frmAbout.getContentPane().add(lblData);
		
		final JLabel lblLoginServer = new JLabel("Login Server");
		lblLoginServer.setHorizontalAlignment(SwingConstants.CENTER);
		lblLoginServer.setFont(new Font("Tahoma", Font.PLAIN, 14));
		lblLoginServer.setBounds(10, 86, 271, 23);
		_frmAbout.getContentPane().add(lblLoginServer);
		
		final JLabel site = new JLabel(URL);
		site.setText("<html><font color=\"#000099\"><u>" + URL + "</u></font></html>");
		site.setHorizontalAlignment(SwingConstants.CENTER);
		site.setBounds(76, 128, 140, 14);
		site.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent arg0)
			{
				if (Desktop.isDesktopSupported())
				{
					try
					{
						Desktop.getDesktop().browse(_uri);
					}
					catch (IOException e)
					{
						// Ignore.
					}
				}
			}
		});
		_frmAbout.getContentPane().add(site);
		
		// Center frame to screen.
		_frmAbout.setLocationRelativeTo(null);
	}
	
	private static URI createURI(String str)
	{
		try
		{
			return new URI(str);
		}
		catch (URISyntaxException x)
		{
			throw new IllegalArgumentException(x.getMessage(), x);
		}
	}
}
