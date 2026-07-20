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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DropMode;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.l2jmobius.commons.config.InterfaceConfig;
import org.l2jmobius.commons.ui.DarkTheme;
import org.l2jmobius.commons.ui.LineLimitListener;
import org.l2jmobius.commons.ui.SplashScreen;
import org.l2jmobius.loginserver.GameServerTable;
import org.l2jmobius.loginserver.GameServerTable.GameServerInfo;
import org.l2jmobius.loginserver.LoginController;
import org.l2jmobius.loginserver.LoginServer;
import org.l2jmobius.loginserver.network.gameserverpackets.ServerStatus;

/**
 * @author Mobius
 */
public class Gui
{
	private static final String[] SHUTDOWN_OPTIONS =
	{
		"Shutdown",
		"Cancel"
	};
	private static final String[] RESTART_OPTIONS =
	{
		"Restart",
		"Cancel"
	};
	
	private final JTextArea _txtrConsole;
	private final JCheckBoxMenuItem _chckbxmntmEnabled;
	private JCheckBoxMenuItem _chckbxmntmDisabled;
	private JCheckBoxMenuItem _chckbxmntmGmOnly;
	
	public Gui()
	{
		// Disable hardware acceleration.
		System.setProperty("sun.java2d.opengl", "false");
		System.setProperty("sun.java2d.d3d", "false");
		System.setProperty("sun.java2d.noddraw", "true");
		
		if (InterfaceConfig.DARK_THEME)
		{
			DarkTheme.activate();
		}
		
		// Initialize console.
		_txtrConsole = new JTextArea();
		_txtrConsole.setEditable(false);
		_txtrConsole.setLineWrap(true);
		_txtrConsole.setWrapStyleWord(true);
		_txtrConsole.setDropMode(DropMode.INSERT);
		_txtrConsole.setFont(new Font("Monospaced", Font.PLAIN, 16));
		_txtrConsole.getDocument().addDocumentListener(new LineLimitListener(500));
		
		// Initialize menu items.
		final JMenuBar menuBar = new JMenuBar();
		menuBar.setFont(new Font("Segoe UI", Font.PLAIN, 14));
		
		final JMenu mnActions = new JMenu("Actions");
		mnActions.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		menuBar.add(mnActions);
		
		final JMenuItem mntmShutdown = new JMenuItem("Shutdown");
		mntmShutdown.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		mntmShutdown.addActionListener(_ ->
		{
			if (JOptionPane.showOptionDialog(null, "Shutdown LoginServer?", "Select an option", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, SHUTDOWN_OPTIONS, SHUTDOWN_OPTIONS[1]) == 0)
			{
				LoginServer.getInstance().shutdown(false);
			}
		});
		mnActions.add(mntmShutdown);
		
		final JMenuItem mntmRestart = new JMenuItem("Restart");
		mntmRestart.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		mntmRestart.addActionListener(_ ->
		{
			if (JOptionPane.showOptionDialog(null, "Restart LoginServer?", "Select an option", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, RESTART_OPTIONS, RESTART_OPTIONS[1]) == 0)
			{
				LoginServer.getInstance().shutdown(true);
			}
		});
		mnActions.add(mntmRestart);
		
		final JMenu mnReload = new JMenu("Reload");
		mnReload.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		menuBar.add(mnReload);
		
		final JMenuItem mntmBannedIps = new JMenuItem("Banned IPs");
		mntmBannedIps.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		mntmBannedIps.addActionListener(_ ->
		{
			LoginController.getInstance().getBannedIps().clear();
			LoginServer.getInstance().loadBanFile();
		});
		mnReload.add(mntmBannedIps);
		
		final JMenu mnStatus = new JMenu("Status");
		mnStatus.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		menuBar.add(mnStatus);
		
		_chckbxmntmEnabled = new JCheckBoxMenuItem("Enabled");
		_chckbxmntmEnabled.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		_chckbxmntmEnabled.addActionListener(_ ->
		{
			_chckbxmntmEnabled.setSelected(true);
			_chckbxmntmDisabled.setSelected(false);
			_chckbxmntmGmOnly.setSelected(false);
			LoginServer.getInstance().setStatus(ServerStatus.STATUS_NORMAL);
			for (GameServerInfo gsi : GameServerTable.getInstance().getRegisteredGameServers().values())
			{
				gsi.setStatus(ServerStatus.STATUS_NORMAL);
			}
			
			LoginServer.LOGGER.info("Status changed to enabled.");
		});
		_chckbxmntmEnabled.setSelected(true);
		mnStatus.add(_chckbxmntmEnabled);
		
		_chckbxmntmDisabled = new JCheckBoxMenuItem("Disabled");
		_chckbxmntmDisabled.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		_chckbxmntmDisabled.addActionListener(_ ->
		{
			_chckbxmntmEnabled.setSelected(false);
			_chckbxmntmDisabled.setSelected(true);
			_chckbxmntmGmOnly.setSelected(false);
			LoginServer.getInstance().setStatus(ServerStatus.STATUS_DOWN);
			for (GameServerInfo gsi : GameServerTable.getInstance().getRegisteredGameServers().values())
			{
				gsi.setStatus(ServerStatus.STATUS_DOWN);
			}
			
			LoginServer.LOGGER.info("Status changed to disabled.");
		});
		mnStatus.add(_chckbxmntmDisabled);
		
		_chckbxmntmGmOnly = new JCheckBoxMenuItem("GM only");
		_chckbxmntmGmOnly.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		_chckbxmntmGmOnly.addActionListener(_ ->
		{
			_chckbxmntmEnabled.setSelected(false);
			_chckbxmntmDisabled.setSelected(false);
			_chckbxmntmGmOnly.setSelected(true);
			LoginServer.getInstance().setStatus(ServerStatus.STATUS_GM_ONLY);
			for (GameServerInfo gsi : GameServerTable.getInstance().getRegisteredGameServers().values())
			{
				gsi.setStatus(ServerStatus.STATUS_GM_ONLY);
			}
			
			LoginServer.LOGGER.info("Status changed to GM only.");
		});
		mnStatus.add(_chckbxmntmGmOnly);
		
		final JMenu mnFont = new JMenu("Font");
		mnFont.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		menuBar.add(mnFont);
		
		final String[] fonts =
		{
			"16",
			"21",
			"27",
			"33"
		};
		for (String font : fonts)
		{
			final JMenuItem mntmFont = new JMenuItem(font);
			mntmFont.setFont(new Font("Segoe UI", Font.PLAIN, 13));
			mntmFont.addActionListener(_ -> _txtrConsole.setFont(new Font("Monospaced", Font.PLAIN, Integer.parseInt(font))));
			mnFont.add(mntmFont);
		}
		
		final JMenu mnHelp = new JMenu("Help");
		mnHelp.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		menuBar.add(mnHelp);
		
		final JMenuItem mntmAbout = new JMenuItem("About");
		mntmAbout.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		mntmAbout.addActionListener(_ -> new AboutFrame());
		mnHelp.add(mntmAbout);
		
		// Set icons.
		final List<Image> icons = new ArrayList<>();
		icons.add(new ImageIcon(".." + File.separator + "images" + File.separator + "l2jmobius_16x16.png").getImage());
		icons.add(new ImageIcon(".." + File.separator + "images" + File.separator + "l2jmobius_32x32.png").getImage());
		icons.add(new ImageIcon(".." + File.separator + "images" + File.separator + "l2jmobius_64x64.png").getImage());
		icons.add(new ImageIcon(".." + File.separator + "images" + File.separator + "l2jmobius_128x128.png").getImage());
		
		final JScrollPane scrollPanel = new JScrollPane(_txtrConsole);
		scrollPanel.setBounds(0, 0, 800, 550);
		
		// Set frame.
		final JFrame frame = new JFrame("Mobius - LoginServer");
		frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent ev)
			{
				if (JOptionPane.showOptionDialog(null, "Shutdown LoginServer?", "Select an option", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, SHUTDOWN_OPTIONS, SHUTDOWN_OPTIONS[1]) == 0)
				{
					LoginServer.getInstance().shutdown(false);
				}
			}
		});
		frame.addComponentListener(new ComponentAdapter()
		{
			@Override
			public void componentResized(ComponentEvent ev)
			{
				scrollPanel.setSize(frame.getContentPane().getSize());
			}
		});
		frame.setJMenuBar(menuBar);
		frame.setIconImages(icons);
		frame.add(scrollPanel, BorderLayout.CENTER);
		frame.getContentPane().setPreferredSize(new Dimension(InterfaceConfig.DARK_THEME ? 815 : 800, 550));
		frame.pack();
		frame.setLocationRelativeTo(null);
		
		// Redirect output to text area.
		redirectSystemStreams();
		
		// Show SplashScreen.
		new SplashScreen(".." + File.separator + "images" + File.separator + "splash.png", 5000, frame);
	}
	
	// Set where the text is redirected. In this case, txtrConsole.
	void updateTextArea(String text)
	{
		SwingUtilities.invokeLater(() ->
		{
			_txtrConsole.append(text);
			_txtrConsole.setCaretPosition(_txtrConsole.getText().length());
		});
	}
	
	// Method that manages the redirect.
	private void redirectSystemStreams()
	{
		final OutputStream out = new OutputStream()
		{
			@Override
			public void write(int b)
			{
				updateTextArea(String.valueOf((char) b));
			}
			
			@Override
			public void write(byte[] b, int off, int len)
			{
				updateTextArea(new String(b, off, len));
			}
			
			@Override
			public void write(byte[] b)
			{
				write(b, 0, b.length);
			}
		};
		
		System.setOut(new PrintStream(out, true));
		System.setErr(new PrintStream(out, true));
	}
}
